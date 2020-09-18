// #define _POSIX_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <math.h>
#include <pthread.h>
#include <sys/time.h>
#include "my402list.h"
#include "cs402.h"

typedef struct tagPacket{
    int id;
    int tokenRequired;
    // double S_time_read;
    double serviceTimeRequired;
    
    double q1Time;
    double q2Time;
    double serviceTime;
    double timeEnterSystem;
}packet;


pthread_t packetProcessThread, tokenProcessThread, server1Thread, server2Thread, ctrcCatchingThread;
pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cv = PTHREAD_COND_INITIALIZER;
sigset_t set;
int controlCPressed = 0;
My402List Q1;
My402List Q2;
FILE *fp = NULL;
// lambda: packet arrival rate  mu : sever transmission rate r: token arrival rate
double lambda = 1, mu = 0.35, r = 1.5;
// B : bucket depth  p : pakect required tokens
int B = 10, P = 3;
// command line params
int numOfLambda = 0, numOfP = 0, numOfNum = 0, numOfB = 0, numOfMu = 0, numOfR = 0;
char filename[20];

int numOfTotalPackets = 20, numOfPacketArrived = 0;
int numOfTotalPacketToServe =20, numOfPacketServed = 0;;
struct timeval startTime;
double startTimeDdouble;
struct timeval endTime;
double endTimeDouble;
struct timeval currTime;
struct timeval diffFromNowToStart;
double enterQ1Time;
double leaveQ1Time;
double  inQ1Time;
double enterQ2Time;
double leaveQ2Time;
double  inQ2Time;

int tokens = 0;
int totalNumOfTokens = 0;

//statstics
double sumInterArrivalTime = 0;
double sumServiceTime = 0;
double sumTimeInSystem = 0;
// runnig avg here
double avgTimeInSystem = 0;
double avgTImeInSystemSqure = 0;

double avgNumInQ1 = 0;
double avgNumInQ2 = 0;
double avgNumAtS1 = 0;
double avgNumAtS2 = 0;
int numofPacketsDropped = 0;
int numofTokenDropped = 0;


double getDoubleTimeStamp( struct timeval *time){
    return (double)time->tv_sec * 1000  + (double)time->tv_usec * 0.001;
}

void printTimeStamp(struct timeval* t){
    if(t->tv_sec > 100000){
        fprintf(stdout, "????????.???ms ");
        return ;
    }
    char strTimeStamp[13];
    int secs = t->tv_sec * 1000 + t->tv_usec / 1000;
    int milsecs = t->tv_usec % 1000;
    memset(strTimeStamp, '0', sizeof(strTimeStamp));
    strTimeStamp[12] = '\0';
    strTimeStamp[8] ='.';
    int idx1 = 7;
    while(secs != 0){
        strTimeStamp[idx1] = secs%10 + '0';
        secs /= 10;
        idx1 --;
    }
    int idx2 = 11;
    while(milsecs != 0){
        strTimeStamp[idx2] = milsecs%10 + '0';
        milsecs /= 10;
        idx2 --;
    }
    fprintf(stdout, "%sms: ", strTimeStamp);
}


void* packetProcess(){
    // fprintf(stdout, "packet thread is running!\n");
    pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, 0);
    struct timeval prevPackageArriveTime = startTime;
    struct timeval interArrivalTime;
    while(1){
        pthread_mutex_lock(&m);
        if(numOfPacketArrived >= numOfTotalPackets){
            pthread_mutex_unlock(&m);
            break;
        }
        if(fp != NULL){
            char buffer[1024];
            fgets(buffer, sizeof(buffer), fp);
            char* slow = buffer;
            char* fast = buffer;
            fast = strchr(slow, ' ');
            if(fast != NULL){
              *fast++ = '\0';  
            }
            else{
                fprintf(stderr, "malformed trace file");
                exit(1);
            }
            // lose some accurracy here when.
            lambda = (double)1000 / atoi(slow);
            while(*fast == ' '){
                fast++;
            }
            slow = fast;
            fast = strchr(slow, ' ');
            if(fast != NULL){
              *fast++ = '\0';  
            }
            else{
                fprintf(stderr, "malformed trace file");
                exit(1);
            }
            P = atoi(slow);
            while(*fast == ' '){
                fast++;
            }
            slow = fast;
            mu = (double) 1000/atoi(slow);
        }
        //get the curr time before sleep
        gettimeofday(&currTime, NULL); 
        struct timeval diff;
        timersub(&currTime, &prevPackageArriveTime, &diff);
        unsigned int exactTimeInterval = (unsigned int)(1/lambda *1000000);
        unsigned int actualLag = (unsigned int)getDoubleTimeStamp(&diff) * 1000;
        pthread_mutex_unlock(&m);
        if(actualLag < exactTimeInterval){   
            unsigned int sleepTimeinMs = exactTimeInterval- actualLag;
            pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, 0);
            usleep(sleepTimeinMs);
            pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, 0);
        }
          
        pthread_mutex_lock(&m);
        if(controlCPressed){
            // fprintf(stdout,"packet thread detects control c");
            pthread_mutex_unlock(&m);
            break;
        }
        if(numOfPacketArrived >= numOfTotalPackets){
            pthread_mutex_unlock(&m);
            break;
        }
        numOfPacketArrived++;
        packet *aPacket = (packet*)malloc(sizeof(packet));
        aPacket->id = numOfPacketArrived;
        aPacket->tokenRequired = P;
        aPacket->serviceTimeRequired = (1/mu)*1000;
        gettimeofday(&currTime, NULL); 
        timersub(&currTime, &startTime, &diffFromNowToStart);
        aPacket->timeEnterSystem = getDoubleTimeStamp(&diffFromNowToStart);
        printTimeStamp(&diffFromNowToStart);
        timersub(&currTime, &prevPackageArriveTime, &interArrivalTime);
        double timeDouble = getDoubleTimeStamp(&interArrivalTime);
        sumInterArrivalTime += timeDouble;
        fprintf(stdout, "p%d arrives, needs %d tokens, inter-arrival time = %.3fms", aPacket->id, P,timeDouble);
        prevPackageArriveTime = currTime;
        if(P>B){
            numofPacketsDropped ++;
            fprintf(stdout, ", dropped\n");
            numOfTotalPacketToServe--;
            pthread_cond_broadcast(&cv);
            free(aPacket);
            pthread_mutex_unlock(&m);
            continue;
        }
        else{ 
            fprintf(stdout, "\n");
            My402ListPrepend(&Q1, aPacket);
            gettimeofday(&currTime, NULL);
            timersub(&currTime, &startTime, &diffFromNowToStart);
            enterQ1Time = getDoubleTimeStamp(&diffFromNowToStart);
            aPacket->q1Time = enterQ1Time;
            printTimeStamp(&diffFromNowToStart);
            fprintf(stdout, "p%d enters Q1\n", aPacket->id);

        }
        if(My402ListLength(&Q1) == 1 && tokens >= aPacket->tokenRequired){
            // packet leaves Q1
            My402ListElem *temp = My402ListLast(&Q1);
            My402ListUnlink(&Q1, temp);
            tokens -= aPacket->tokenRequired;
            gettimeofday(&currTime, NULL);
            timersub(&currTime, &startTime, &diffFromNowToStart);
            leaveQ1Time = getDoubleTimeStamp(&diffFromNowToStart);
            inQ1Time = leaveQ1Time - (aPacket->q1Time);
            // aPacket->q1Time = inQ1Time;
            avgNumInQ1 += inQ1Time;
            printTimeStamp(&diffFromNowToStart);
            fprintf(stdout, "p%d leaves Q1, time in Q1 = %.3fms, token bucket now has %d token\n", aPacket->id, inQ1Time, tokens);
            // packet enters Q2
            My402ListPrepend(&Q2, aPacket);
            gettimeofday(&currTime, NULL);
            timersub(&currTime, &startTime, &diffFromNowToStart);
            printTimeStamp(&diffFromNowToStart);
            enterQ2Time = getDoubleTimeStamp(&diffFromNowToStart);
            aPacket->q2Time = enterQ2Time;
            fprintf(stdout, "p%d enters Q2\n", aPacket->id);
            pthread_cond_broadcast(&cv); 
        }
        
        pthread_mutex_unlock(&m);
    }
    // fprintf(stdout, "packet thread is terminating!\n");
    pthread_exit(0);

}

void* tokenProcess(){
    int notStop = 1;
    struct timeval prevTokenArriveTime = startTime;
    // fprintf(stdout,"token thread is running!\n");
    pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, 0);
    while(notStop){
        pthread_mutex_lock(&m);
        //get the curr time before sleep
        gettimeofday(&currTime, NULL); 
        struct timeval diff;
        timersub(&currTime, &prevTokenArriveTime, &diff);
        unsigned int exactTimeInterval = (unsigned int)(1/r *1000000);
        unsigned int actualLag = (unsigned int)getDoubleTimeStamp(&diff) * 1000;
        
        pthread_mutex_unlock(&m);
        if(actualLag < exactTimeInterval){
            unsigned int sleepTimeinMs = exactTimeInterval -actualLag;    
            pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, 0);
            usleep(sleepTimeinMs); 
            pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, 0);
        }
    
   
        pthread_mutex_lock(&m);
        if(controlCPressed){
            notStop = 0;
            // fprintf(stdout,"token thread detects control c");
            pthread_mutex_unlock(&m);
            continue;  
        }
        if((numOfPacketArrived == numOfTotalPackets) && My402ListEmpty(&Q1)){
            notStop = 0;
            pthread_mutex_unlock(&m);
            continue;  
        }
        gettimeofday(&currTime, NULL);
        timersub(&currTime, &startTime, &diffFromNowToStart);
        printTimeStamp(&diffFromNowToStart);
        totalNumOfTokens++;
        prevTokenArriveTime = currTime;
        if(tokens < B){
            tokens++;
            fprintf(stdout, "token t%d arrives, token bucket now has %d token\n", totalNumOfTokens, tokens);
        }
        else{
            numofTokenDropped++;
            fprintf(stdout, "token t%d arrives, dropped\n", totalNumOfTokens);
            pthread_mutex_unlock(&m);
            continue;  
        }
        if(!My402ListEmpty(&Q1)){
            My402ListElem *temp = My402ListLast(&Q1);
            packet * aPacket = (packet*) temp->obj;
            // move packet from Q1 to Q2
            if(aPacket->tokenRequired <= tokens){
                 // packet leaves Q1
                My402ListUnlink(&Q1, temp);
                tokens -= aPacket->tokenRequired;
                gettimeofday(&currTime, NULL);
                timersub(&currTime, &startTime, &diffFromNowToStart);
                leaveQ1Time = getDoubleTimeStamp(&diffFromNowToStart);
                inQ1Time = leaveQ1Time - (aPacket->q1Time);
                // aPacket->q1Time = inQ1Time;
                avgNumInQ1+=inQ1Time;
                printTimeStamp(&diffFromNowToStart);
                fprintf(stdout, "p%d leaves Q1, time in Q1 = %.3fms, token bucket now has %d token\n", aPacket->id, inQ1Time, tokens);
                // packet enters Q2
                My402ListPrepend(&Q2, aPacket);
                gettimeofday(&currTime, NULL);
                timersub(&currTime, &startTime, &diffFromNowToStart);
                printTimeStamp(&diffFromNowToStart);
                enterQ2Time = getDoubleTimeStamp(&diffFromNowToStart);
                aPacket->q2Time = enterQ2Time;
                fprintf(stdout, "p%d enters Q2\n", aPacket->id);
                pthread_cond_broadcast(&cv);         
            }
        }
        pthread_mutex_unlock(&m);
    }
    // fprintf(stdout, "token thread is terminating!\n");
    pthread_exit(0);

}

void* server(void* serNum){
    int notStop = 1;
    // fprintf(stdout, "server%d thread is running!\n", (int)serNum);
    while(notStop){
        pthread_mutex_lock(&m);
        while(My402ListEmpty(&Q2) && (numOfTotalPacketToServe!= numOfPacketServed) && controlCPressed == 0){
            // fprintf(stdout, "server%d goes into condition wait\n", (int) serNum);
            pthread_cond_wait(&cv, &m);
        }
        if(controlCPressed){
            // fprintf(stdout,"sever%d thread detects control c", (int)serNum);
            notStop = 0; 
            pthread_mutex_unlock(&m);
            continue;
        }
        if( numOfTotalPacketToServe == numOfPacketServed){
            notStop = 0; 
            pthread_mutex_unlock(&m);
            continue;
        }
        // REMOVE PACKET FROM Q2
        My402ListElem *temp = My402ListLast(&Q2);
        packet * aPacket = (packet*)(temp->obj);
        My402ListUnlink(&Q2, temp);
        gettimeofday(&currTime, NULL);
        timersub(&currTime, &startTime, &diffFromNowToStart);
        leaveQ2Time = getDoubleTimeStamp(&diffFromNowToStart);
        inQ2Time = leaveQ2Time - (aPacket->q2Time);
        printTimeStamp(&diffFromNowToStart);
        avgNumInQ2+=inQ2Time;
        fprintf(stdout, "p%d leaves Q2, time in Q2 = %.3fms\n", aPacket->id, inQ2Time);
        // s1 s2
        gettimeofday(&currTime, NULL);
        timersub(&currTime, &startTime, &diffFromNowToStart);
        printTimeStamp(&diffFromNowToStart);
        aPacket->serviceTime = getDoubleTimeStamp(&diffFromNowToStart);
        fprintf(stdout,"p%d begins service at S%d, requesting %dms of service\n", aPacket->id, (int)serNum, round(aPacket->serviceTimeRequired));
        pthread_mutex_unlock(&m);
        usleep(aPacket->serviceTimeRequired * 1000);
        pthread_mutex_lock(&m);
        gettimeofday(&currTime, NULL);
        timersub(&currTime, &startTime, &diffFromNowToStart);
        printTimeStamp(&diffFromNowToStart);
        double timeInSys = getDoubleTimeStamp(&diffFromNowToStart) - (aPacket->timeEnterSystem);
        double timeinService = getDoubleTimeStamp(&diffFromNowToStart) - (aPacket->serviceTime);
        sumServiceTime+=timeinService;
        if((int)serNum == 1){
            avgNumAtS1 += timeinService;
        }
        else{
            avgNumAtS2 += timeinService;
        }
        sumTimeInSystem += timeInSys;
        avgTimeInSystem = ((numOfPacketServed*avgTimeInSystem) + timeInSys) / (numOfPacketServed + 1);
        avgTImeInSystemSqure =(numOfPacketServed *avgTImeInSystemSqure + pow(timeInSys,2)) /(numOfPacketServed + 1);
        numOfPacketServed++; 
        pthread_cond_broadcast(&cv);
        fprintf(stdout, "p%d departs from S%d, service time = %.3fms, time in system = %.3fms\n", aPacket->id, (int)serNum, timeinService, timeInSys);
        pthread_mutex_unlock(&m);
    }
    // fprintf(stdout, "server%d thread is terminating!\n", (int)serNum);
    pthread_exit(0);
}

void* sigHandler(){
    int sig;
    while(1){
        sigwait(&set, &sig);
        if(sig == 2){
            pthread_mutex_lock(&m);
            pthread_cancel(packetProcessThread);
            pthread_cancel(tokenProcessThread);
            controlCPressed = 1;
            pthread_cond_broadcast(&cv);
            fprintf(stdout,"SIGINT caught, no new packets or tokens will be allowed\n");
            pthread_mutex_unlock(&m);         
        }
    }
    pthread_exit(0);
}

void printStatistics(){
    fprintf(stdout, "\nStatistics:\n\n");
    if(numOfPacketArrived == 0){
        fprintf(stdout,"    average packet inter-arrival time = N/A, no packet arrived at this facility\n");
    }
    else{
        fprintf(stdout,"    average packet inter-arrival time = %.6g\n", ((sumInterArrivalTime / 1000) / numOfPacketArrived));
    }
    if (numOfPacketServed == 0) {
        fprintf(stdout,"    average packet service time = N/A, no packet is served at this facility\n\n");
    }
    else{
        fprintf(stdout,"    average packet service time = %.6g\n\n", ((sumServiceTime / 1000) / numOfPacketServed));
    }
    fprintf(stdout,"    average number of packets in Q1 = %.6g\n", (avgNumInQ1 / endTimeDouble));
    fprintf(stdout,"    average number of packets in Q2 = %.6g\n", (avgNumInQ2 / endTimeDouble));
    fprintf(stdout,"    average number of packets at S1 = %.6g\n", (avgNumAtS1  / endTimeDouble));
    fprintf(stdout,"    average number of packets at S2 = %.6g\n\n", (avgNumAtS2 / endTimeDouble));
    if (numOfPacketServed == 0) {
        fprintf(stdout,"    average time a packet spent in system = N/A, no packet spent time in the system\n");
        fprintf(stdout,"    standard deviation for time spent in system = N/A, no packet spent time at this facility\n\n");
    }
    else{
        fprintf(stdout,"    average time a packet spent in system = %.6g\n",(sumTimeInSystem / 1000) / numOfPacketServed ); 
        double variance = avgTImeInSystemSqure/1000000 - pow(avgTimeInSystem/1000, 2);
        fprintf(stdout,"    standard deviation for time spent in system = %.6g\n\n", sqrt(variance));
    }
    if (totalNumOfTokens == 0) {
        fprintf(stdout,"    token drop probability = N/A, no token arrived at this facility\n");
    }
    else{
        fprintf(stdout, "    token drop probability = %.6g\n", ((double)numofTokenDropped / (double)totalNumOfTokens));
    }  
    if (numOfPacketArrived  == 0) {
        fprintf(stdout,"    packet drop probability = N/A, no packet arrived at this facility\n");
    }
    else{
        fprintf(stdout,"    packet drop probability = %.6g\n", ((double)numofPacketsDropped / (double)numOfPacketArrived));
    }
}

void usage()
{
    fprintf(stderr, "Usage: warmup2 [-lambda lambda] [-mu mu] [-r r] [-B B] [-P P] [-n n] [-t tsfile]\n");
    exit(1);
}

void readCommandParam(int argc, char*argv[]){
     //recognize parameters
    for (argc--, argv++; argc > 0; argc--,argc--, argv++) {
        //check if file name specified
        if (!strcmp(*argv, "-t")) {
            argv++;
            fp = fopen(*argv, "r");
            if (fp == NULL) {
                fprintf(stderr, "input file %s does not exist\n", *argv);
                exit(1);
            }
            strncpy(filename, *argv, strlen(*argv));
            filename[strlen(*argv)] = '\0';
        }
        //check if lambda is input
        else if (!strcmp(*argv, "-lambda")) {
            numOfLambda = 1;
            argv++;
            if(*argv == NULL){
                fprintf(stderr, "malformed command\n");
                exit(1);
            }
            if (atof(*argv) < 0 ) {
                usage();
                
            }
            else if(atof(*argv) < 0.1){
                lambda = 0.1;
            }
            else{
                lambda = atof(*argv);
            }
        }
        //chech if mu is input
        else if(!strcmp(*argv, "-mu")){
            numOfMu = 1;
            argv++;
            if(*argv == NULL){
                fprintf(stderr, "malformed command\n");
                exit(1);
            }
            if (atof(*argv) < 0) {
                usage();
            }else if(atof(*argv) < 0.1){
                mu = 0.1;
            }else{
                mu = atof(*argv);
            }
        }
        //check if r is input
        else if(!strcmp(*argv, "-r")){
            numOfR = 1;
            argv++;
            if(*argv == NULL){
                fprintf(stderr, "malformed command\n");
                exit(1);
            }
            if (atof(*argv) <0) {
                usage();
            }
            else if(atof(*argv) >10){
                r = 10;
            }
            else{
                r = atof(*argv);
            }
        }
        //check if P is input
        else if(!strcmp(*argv, "-P")){
            numOfP = 1;
            argv++;
            if(*argv == NULL){
                fprintf(stderr, "malformed command\n");
                exit(1);
            }
            if (atoi(*argv) > 2147483647) {
                P = 2147483647;
            }
            else if(atoi(*argv) < 0){
                usage();
            }
            else{
                P = atoi(*argv);
            }
        }
        //check if num is input
        else if(!strcmp(*argv, "-n")){
            numOfNum = 1;
            argv++;
            if(*argv == NULL){
                fprintf(stderr, "malformed command\n");
                exit(1);
            }
            if (atoi(*argv) > 2147483647) {
                numOfTotalPackets = 2147483647;
            }
            else if(atoi(*argv) < 0){
                usage();
            }
            else{
                numOfTotalPackets = atoi(*argv);
            }
        }
        //check if B is input
        else if(strcmp(*argv, "-B") == 0){
            numOfB = 1;
            argv++;
            if(*argv == NULL){
                fprintf(stderr, "malformed command\n");
                exit(1);
            }
            if (atoi(*argv) > 2147483647) {
                B = 2147483647;
            }
            else if(atoi(*argv) < 0){
                usage();
            }
            else{
                B = atoi(*argv);
            }
        }
        else{
            fprintf(stderr, "malformed command\n");
            exit(1);
        }
    }

}

void printEmulationParam(){
     //default value of parameters
    if (fp == NULL) {
        fprintf(stdout,"Emulation Parameters:\n");
        fprintf(stdout,"    number to arrive = %d\n", numOfTotalPackets);
        fprintf(stdout,"    lambda = %.2f\n", lambda);
        fprintf(stdout,"    mu = %.2f\n", mu);
        fprintf(stdout,"    r = %.2f\n", r);
        fprintf(stdout,"    B = %d\n", B);
        fprintf(stdout,"    P = %d\n", P);
        numOfTotalPacketToServe = numOfTotalPackets;
    }
    else{
        //read file
        char buffer[1024];
        fgets(buffer, sizeof(buffer), fp);
        numOfTotalPackets = atoi(buffer);
        if (numOfTotalPackets == 0) {
            fprintf(stderr, "malformed input - line 1 is not just a number\n");
            exit(-1);
        }
        numOfTotalPacketToServe = numOfTotalPackets;
        fprintf(stdout, "Emulation Parameters:\n");
        fprintf(stdout, "    number to arrive = %d\n", numOfTotalPackets);
        fprintf(stdout,"    r = %.2f\n", r);
        fprintf(stdout,"    B = %d\n", B);
        fprintf(stdout,"    tsfile = %s\n", filename);
    }
    fprintf(stdout, "\n");

}


int main(int argc, char*argv[]){
    readCommandParam(argc, argv);
    printEmulationParam();
    pthread_mutex_lock(&m); 
    My402ListInit(&Q1);
    My402ListInit(&Q2);
    gettimeofday(&startTime, NULL);
    startTimeDdouble = getDoubleTimeStamp(&startTime);
    timersub(&startTime, &startTime, &diffFromNowToStart);
    printTimeStamp(&diffFromNowToStart);
    fprintf(stdout, "emulation begins\n");
    pthread_mutex_unlock(&m);
    sigemptyset(&set);
    sigaddset(&set, SIGINT);
    sigprocmask(SIG_BLOCK, &set, 0);
    pthread_create(&packetProcessThread, NULL, packetProcess, NULL);
    pthread_create(&tokenProcessThread, NULL, tokenProcess, NULL);
    pthread_create(&server1Thread, NULL, server, (void *)1);
    pthread_create(&server2Thread, NULL, server, (void *)2);
    pthread_create(&ctrcCatchingThread, NULL, sigHandler, NULL); 
    pthread_join(packetProcessThread, NULL);
    pthread_join(tokenProcessThread, NULL);
    pthread_join(server1Thread, NULL);
    pthread_join(server2Thread, NULL);
    pthread_mutex_lock(&m); 
    while(!My402ListEmpty(&Q1)){
        My402ListElem *temp = My402ListLast(&Q1);
        gettimeofday(&currTime, NULL);
        timersub(&currTime, &startTime, &diffFromNowToStart);
        printTimeStamp(&diffFromNowToStart);
        fprintf(stdout, "p%d removed from Q1\n", ((packet*)(temp->obj))->id);
        My402ListUnlink(&Q1, temp);
    }
    while(!My402ListEmpty(&Q2)){
        My402ListElem *temp = My402ListLast(&Q2);
        gettimeofday(&currTime, NULL);
        timersub(&currTime, &startTime, &diffFromNowToStart);
        printTimeStamp(&diffFromNowToStart);
        fprintf(stdout, "p%d removed from Q2\n", ((packet*)(temp->obj))->id);
        My402ListUnlink(&Q2, temp);
    }
    gettimeofday(&endTime, NULL);
    timersub(&endTime, &startTime, &diffFromNowToStart);
    // end time here is whole simulation time
    endTimeDouble = getDoubleTimeStamp(&diffFromNowToStart);
    printTimeStamp(&diffFromNowToStart);
    fprintf(stdout, "emulation ends\n");
    printStatistics();
    pthread_mutex_unlock(&m); 
    return 0;
}