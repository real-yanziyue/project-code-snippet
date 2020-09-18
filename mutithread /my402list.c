/*
 * Author:      William Chia-Wei Cheng (bill.cheng@usc.edu)
 *
 * @(#)$Id: listtest.c,v 1.2 2020/05/18 05:09:12 william Exp $
 */


#include <stdlib.h>
#include "cs402.h"
#include "my402list.h"

int  My402ListLength(My402List* listPtr){
    if(listPtr == NULL) return 0;
    return listPtr->num_members;

}
int  My402ListEmpty(My402List* listPtr){
    if(listPtr == NULL) return TRUE;
    if(listPtr->num_members == 0) return TRUE;
    return FALSE;

}
int  My402ListAppend(My402List* listPtr, void* obj){
    My402ListElem * newElem = (My402ListElem*) malloc(sizeof(My402ListElem));
    if(newElem == NULL){
        return FALSE;
    }
    newElem->obj = obj;
    My402ListElem * lastElem = listPtr->anchor.prev;
    lastElem->next = newElem;
    listPtr->anchor.prev = newElem;
    newElem->next = &(listPtr->anchor);
    newElem->prev = lastElem;
    listPtr->num_members += 1;
    return TRUE;
}
int  My402ListPrepend(My402List* listPtr, void* obj){
     My402ListElem * newElem = (My402ListElem*) malloc(sizeof(My402ListElem));
    if(newElem == NULL){
        return FALSE;
    }
    newElem->obj = obj;
    My402ListElem* firstElem = listPtr->anchor.next;
    newElem->prev = &(listPtr->anchor);
    listPtr->anchor.next = newElem;
    firstElem->prev = newElem;
    newElem->next = firstElem;
    listPtr->num_members +=1;
    return TRUE;
}

void My402ListUnlink(My402List* listPtr, My402ListElem* elemPtr){
    My402ListElem *prevElem = elemPtr->prev;
    My402ListElem *nextElem = elemPtr->next;
    prevElem->next = nextElem;
    nextElem->prev = prevElem;
    free(elemPtr);
    listPtr->num_members -= 1;
}
void My402ListUnlinkAll(My402List* listPtr){
    My402ListElem *firstElem = My402ListFirst(listPtr);
    while(firstElem != NULL){
        My402ListUnlink(listPtr, firstElem);
        firstElem = My402ListFirst(listPtr);
    }
}
int  My402ListInsertAfter(My402List* listPtr, void* obj, My402ListElem* elemPtr){
    if(elemPtr == NULL){
        return My402ListAppend(listPtr, obj);
    }
    My402ListElem * nextElemPtr = elemPtr->next;
    My402ListElem * newElem = (My402ListElem*) malloc(sizeof(My402ListElem));
    if(newElem == NULL){
        return FALSE;
    }
    newElem->obj = obj;
    elemPtr->next = newElem;
    newElem->prev = elemPtr;
    nextElemPtr->prev = newElem;
    newElem->next = nextElemPtr;
    listPtr->num_members +=1;
    return TRUE;

}
int  My402ListInsertBefore(My402List* listPtr, void* obj, My402ListElem* elemPtr){
    if(elemPtr == NULL) return My402ListPrepend(listPtr, obj);
    My402ListElem * prevElemPtr = elemPtr->prev;
    My402ListElem * newElem = (My402ListElem*)malloc(sizeof(My402ListElem));
    if(newElem == NULL) return FALSE;
    newElem->obj = obj;
    listPtr->num_members += 1;
    prevElemPtr->next = newElem;
    elemPtr->prev = newElem;
    newElem->prev = prevElemPtr;
    newElem->next = elemPtr;
    return TRUE;
}

My402ListElem *My402ListFirst(My402List*listPtr){
    if(listPtr == NULL || listPtr->num_members == 0) return NULL;
    return listPtr->anchor.next;
}
My402ListElem *My402ListLast(My402List*listPtr){
     if(listPtr == NULL || listPtr->num_members == 0) return NULL;
    return listPtr->anchor.prev;
}
My402ListElem *My402ListNext(My402List* listPtr, My402ListElem* elemPtr){
    if(elemPtr->next == &(listPtr->anchor)) return NULL;
    return elemPtr->next;

}
My402ListElem *My402ListPrev(My402List* listPtr, My402ListElem* elemPtr){
    if(elemPtr->prev == &(listPtr->anchor)) return NULL;
    return elemPtr->prev;

}
My402ListElem *My402ListFind(My402List*listPtr, void*obj){
    My402ListElem *elem = NULL;
    elem = My402ListFirst(listPtr);
    while(elem != NULL){
        if(elem->obj == obj){
            return elem;
        }
        elem = My402ListNext(listPtr, elem);
    }
    return NULL;
}

int My402ListInit(My402List* listPtr){
    listPtr->anchor.next = &(listPtr->anchor);
    listPtr->anchor.prev = &(listPtr->anchor);
    listPtr->anchor.obj = NULL;
    listPtr->num_members = 0;
    return TRUE; 
}