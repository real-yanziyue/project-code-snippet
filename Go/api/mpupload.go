package api

import (
	"encoding/json"
	"fmt"
	"log"
	"math"
	"net/http"
	"os"
	"path"
	"strconv"
	"strings"
	"time"

	"github.com/garyburd/redigo/redis"
	"github.com/gin-gonic/gin"

	rPool "filestore-server/cache/redis"
	cmn "filestore-server/common"
	cmnCfg "filestore-server/config"
	"filestore-server/mq"
	dbcli "filestore-server/service/dbproxy/client"
	"filestore-server/util"
)

const (
	// ChunkKeyPrefix : redis key prefix
	ChunkKeyPrefix = "MP_"
	// HashUpIDKeyPrefix : hash to uploadID redis prefix
	HashUpIDKeyPrefix = "HASH_UPID_"
)

// MultipartUploadInfo : initialization
type MultipartUploadInfo struct {
	FileHash   string
	FileSize   int
	UploadID   string
	ChunkSize  int
	ChunkCount int
	// chucks which have been uploaded
	ChunkExists []int
}

func init() {
	if err := os.MkdirAll(cmnCfg.ChunckLocalRootDir, 0744); err != nil {
		fmt.Println("can't find directory to store chunks: " + cmnCfg.ChunckLocalRootDir)
		os.Exit(1)
	}

	if err := os.MkdirAll(cmnCfg.MergeLocalRootDir, 0744); err != nil {
		fmt.Println("can't find directory to store merged file: " + cmnCfg.MergeLocalRootDir)
		os.Exit(1)
	}
}

// InitialMultipartUploadHandler : initialize mutipart upload
func InitialMultipartUploadHandler(c *gin.Context) {
	// 1. get users request params
	username := c.Request.FormValue("username")
	filehash := c.Request.FormValue("filehash")
	filesize, err := strconv.Atoi(c.Request.FormValue("filesize"))
	if err != nil {
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -1,
				"msg":  "params invalid",
			})
		return
	}

	// check if file exsits
	if exists, _ := dbcli.IsUserFileUploaded(username, filehash); exists {
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": int(cmn.FileAlreadExists),
				"msg":  "file exists",
			})
		return
	}

	// 2. get a redis connection
	rConn := rPool.RedisPool().Get()
	defer rConn.Close()

	// 3. check if need to resume upload from breakpoint，and get uploadID
	uploadID := ""
	keyExists, _ := redis.Bool(rConn.Do("EXISTS", HashUpIDKeyPrefix+filehash))
	if keyExists {
		uploadID, err = redis.String(rConn.Do("GET", HashUpIDKeyPrefix+filehash))
		if err != nil {
			c.JSON(
				http.StatusOK,
				gin.H{
					"code": -2,
					"msg":  err.Error(),
				})
			return
		}
	}

	// 4.1 build new uploadID on first upload
	// 4.2 get the list of chuncks that have already been uploaded
	chunksExist := []int{}
	if uploadID == "" {
		uploadID = username + fmt.Sprintf("%x", time.Now().UnixNano())
	} else {
		chunks, err := redis.Values(rConn.Do("HGETALL", ChunkKeyPrefix+uploadID))
		if err != nil {
			c.JSON(
				http.StatusOK,
				gin.H{
					"code": -3,
					"msg":  err.Error(),
				})
			return
		}
		for i := 0; i < len(chunks); i += 2 {
			k := string(chunks[i].([]byte))
			v := string(chunks[i+1].([]byte))
			if strings.HasPrefix(k, "chkidx_") && v == "1" {
				// chkidx_6 -> 6
				chunkIdx, _ := strconv.Atoi(k[7:])
				chunksExist = append(chunksExist, chunkIdx)
			}
		}
	}

	// 5. initialize message for multipart upload
	upInfo := MultipartUploadInfo{
		FileHash:    filehash,
		FileSize:    filesize,
		UploadID:    uploadID,
		ChunkSize:   5 * 1024 * 1024, // 5MB
		ChunkCount:  int(math.Ceil(float64(filesize) / (5 * 1024 * 1024))),
		ChunkExists: chunksExist,
	}

	// 6. write into redis cache
	if len(upInfo.ChunkExists) <= 0 {
		hkey := ChunkKeyPrefix + upInfo.UploadID
		rConn.Do("HSET", hkey, "chunkcount", upInfo.ChunkCount)
		rConn.Do("HSET", hkey, "filehash", upInfo.FileHash)
		rConn.Do("HSET", hkey, "filesize", upInfo.FileSize)
		rConn.Do("EXPIRE", hkey, 43200)
		rConn.Do("SET", HashUpIDKeyPrefix+filehash, upInfo.UploadID, "EX", 43200)
	}

	// 7. return message to client
	c.JSON(
		http.StatusOK,
		gin.H{
			"code": 0,
			"msg":  "OK",
			"data": upInfo,
		})
}

// UploadPartHandler : upload file section
func UploadPartHandler(c *gin.Context) {
	// 1. get user request info
	//	username := c.Request.FormValue("username")
	uploadID := c.Request.FormValue("uploadid")
	chunkSha1 := c.Request.FormValue("chkhash")
	chunkIndex := c.Request.FormValue("index")

	// 2. get a redis connection
	rConn := rPool.RedisPool().Get()
	defer rConn.Close()

	// 3. 获得文件句柄，用于存储分块内容
	fpath := cmnCfg.ChunckLocalRootDir + uploadID + "/" + chunkIndex
	os.MkdirAll(path.Dir(fpath), 0744)
	fd, err := os.Create(fpath)
	if err != nil {
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": 0,
				"msg":  "Upload part failed",
				"data": nil,
			})
		return
	}
	defer fd.Close()

	buf := make([]byte, 1024*1024)
	for {
		n, err := c.Request.Body.Read(buf)
		fd.Write(buf[:n])
		if err != nil {
			break
		}
	}

	//  check hash 
	cmpSha1, err := util.ComputeSha1ByShell(fpath)
	if err != nil || cmpSha1 != chunkSha1 {
		log.Printf("Verify chunk sha1 failed, compare OK: %t, err:%+v\n",
			cmpSha1 == chunkSha1, err)
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -2,
				"msg":  "Verify hash failed, chkIdx:" + chunkIndex,
				"data": nil,
			})
		return
	}

	// 4. update redis
	rConn.Do("HSET", "MP_"+uploadID, "chkidx_"+chunkIndex, 1)

	// 5. return message to client
	c.JSON(
		http.StatusOK,
		gin.H{
			"code": 0,
			"msg":  "OK",
			"data": nil,
		})
}

// CompleteUploadHandler : complete upload
func CompleteUploadHandler(c *gin.Context) {
	// 1. get user request info
	upid := c.Request.FormValue("uploadid")
	username := c.Request.FormValue("username")
	filehash := c.Request.FormValue("filehash")
	filesize := c.Request.FormValue("filesize")
	filename := c.Request.FormValue("filename")

	// 2. get a redis con
	rConn := rPool.RedisPool().Get()
	defer rConn.Close()

	// 3. chceck if completed
	data, err := redis.Values(rConn.Do("HGETALL", "MP_"+upid))
	if err != nil {
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -1,
				"msg":  "服务错误",
				"data": nil,
			})
		return
	}
	totalCount := 0
	chunkCount := 0
	for i := 0; i < len(data); i += 2 {
		k := string(data[i].([]byte))
		v := string(data[i+1].([]byte))
		if k == "chunkcount" {
			totalCount, _ = strconv.Atoi(v)
		} else if strings.HasPrefix(k, "chkidx_") && v == "1" {
			chunkCount++
		}
	}
	if totalCount != chunkCount {
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -2,
				"msg":  "分块不完整",
				"data": nil,
			})
		return
	}

	
	srcPath := cmnCfg.ChunckLocalRootDir + upid + "/"
	destPath := cmnCfg.MergeLocalRootDir + filehash
	cmd := fmt.Sprintf("cd %s && ls | sort -n | xargs cat > %s", srcPath, destPath)
	mergeRes, err := util.ExecLinuxShell(cmd)
	if err != nil {
		log.Println(err)
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -3,
				"msg":  "合并失败",
				"data": nil,
			})
		return
	}
	log.Println(mergeRes)

	// 5. update file in db
	fsize, _ := strconv.Atoi(filesize)

	fileMeta := dbcli.FileMeta{
		FileSha1: filehash,
		FileName: filename,
		FileSize: int64(fsize),
		Location: destPath,
	}
	_, ferr := dbcli.OnFileUploadFinished(fileMeta)
	_, uferr := dbcli.OnUserFileUploadFinished(username, fileMeta)
	if ferr != nil || uferr != nil {
		errMsg := ""
		if ferr != nil {
			errMsg = ferr.Error()
		} else {
			errMsg = uferr.Error()
		}
		log.Println(errMsg)
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -4,
				"msg":  "数据更新失败",
				"data": errMsg,
			})
		return
	}

	// delete info for uploaded file
	_, delHashErr := rConn.Do("DEL", HashUpIDKeyPrefix+filehash)
	delUploadID, delUploadInfoErr := redis.Int64(rConn.Do("DEL", ChunkKeyPrefix+upid))
	if delUploadID != 1 || delUploadInfoErr != nil || delHashErr != nil {
		c.JSON(
			http.StatusOK,
			gin.H{
				"code": -5,
				"msg":  "数据更新失败",
				"data": nil,
			})
		return
	}

	// 6. asynchronous file transfer
	ossPath := cmnCfg.OSSRootDir + fileMeta.FileSha1
	transMsg := mq.TransferData{
		FileHash:      fileMeta.FileSha1,
		CurLocation:   fileMeta.Location,
		DestLocation:  ossPath,
		DestStoreType: cmn.StoreOSS,
	}
	pubData, _ := json.Marshal(transMsg)
	pubSuc := mq.Publish(
		cmnCfg.TransExchangeName,
		cmnCfg.TransOSSRoutingKey,
		pubData,
	)
	if !pubSuc {
		fmt.Println("publish transfer data failed, sha1: " + fileMeta.FileSha1)
	}

	// 7. handle response
	c.JSON(
		http.StatusOK,
		gin.H{
			"code": 0,
			"msg":  "OK",
			"data": nil,
		})
}
