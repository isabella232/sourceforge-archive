/*
 * parseargs.c    1.1 (5 May 2000) 1.2 (25 Aug 2000)
 *
 * Copyright 1998 by Bill Giel/KC Multimedia and Design Group, Inc.,
 * All rights reserved.
 *
 * Disclaimer of Warranty. Software is provided "AS IS,"
 * without a warranty of any kind. ALL EXPRESS OR IMPLIED
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED
 * WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. IN NO
 * EVENT WILL THE DEVELOPER OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * RELATING TO THE USE, DOWNLOAD, DISTRIBUTION OF OR INABILITY
 * TO USE SOFTWARE, EVEN IF THE DEVELOPER OR ITS LICENSORS HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

#include<stdio.h>
#include<stdlib.h>
#include<string.h>

#include "parseargs.h"

#define MAX_ARGLEN 2048

#define PREFIX1 "-D"
#define PREFIX2 "/D"
#define PREFIX3 "-X"
#define PREFIX4 "/X"
#define WRKDIR  "wrkdir="

LPTSTR getWorkingDirectory(DWORD dwArgc, LPTSTR *lpszArgv)
{
    UINT    i,
            uCount;

    TCHAR   szArg[MAX_ARGLEN];


    for(i=0,uCount=0; i < dwArgc; i++){
        if(strlen(lpszArgv[i]) > 7 && !_strnicmp(lpszArgv[i],WRKDIR,7)){
			return lpszArgv[i]+7;
        }
    }

	return NULL;
}


LPTSTR *getJavaArgs(LPTSTR *lpszArgs, PDWORD pdwLen, DWORD dwArgc, LPTSTR *lpszArgv)
{
    UINT    i,
            uCount;

    TCHAR   szArg[MAX_ARGLEN];


    for(i=0,uCount=0; i < dwArgc; i++){
        if(!strncmp(lpszArgv[i],PREFIX1,2) || !strncmp(lpszArgv[i],PREFIX2,2)
			|| !strncmp(lpszArgv[i],PREFIX3,2) || !strncmp(lpszArgv[i],PREFIX4,2)){
            uCount++;
        }
    }

    if(uCount == 0)
        return NULL;

    lpszArgs = (LPTSTR *)GlobalAlloc(GMEM_FIXED, uCount * sizeof(LPTSTR));
    *pdwLen = uCount;

    for(i=0,uCount=0; i < dwArgc; i++){
        if(!strncmp(lpszArgv[i],PREFIX1,2) || !strncmp(lpszArgv[i],PREFIX2,2)
			|| !strncmp(lpszArgv[i],PREFIX3,2) || !strncmp(lpszArgv[i],PREFIX4,2)){
            strcpy(szArg, lpszArgv[i]);
            lpszArgs[uCount] = (LPTSTR)GlobalAlloc(GMEM_FIXED,strlen(szArg)+1);
            strcpy(lpszArgs[uCount],szArg);
            uCount++;
        }
    }

    return lpszArgs;
}

LPTSTR *getAppArgs(LPTSTR *lpszArgs, PDWORD pdwLen, DWORD dwArgc, LPTSTR *lpszArgv)
{
    UINT    i,
            uCount;

    TCHAR   szArg[MAX_ARGLEN];


    for(i=0,uCount=0; i < dwArgc; i++){
        if(strncmp(lpszArgv[i],PREFIX1,2) && strncmp(lpszArgv[i],PREFIX2,2) && _strnicmp(lpszArgv[i],WRKDIR,7)
			&& strncmp(lpszArgv[i],PREFIX3,2) && strncmp(lpszArgv[i],PREFIX4,2)){
            uCount++;
        }
    }

    if(uCount == 0)
        return NULL;

    lpszArgs = (LPTSTR *)GlobalAlloc(GMEM_FIXED, uCount * sizeof(LPTSTR));
    *pdwLen = uCount;

    for(i=0,uCount=0; i < dwArgc; i++){
        if(strncmp(lpszArgv[i],PREFIX1,2) && strncmp(lpszArgv[i],PREFIX2,2) && _strnicmp(lpszArgv[i],WRKDIR,7)
			&& strncmp(lpszArgv[i],PREFIX3,2) && strncmp(lpszArgv[i],PREFIX4,2)){
            strcpy(szArg, lpszArgv[i]);
            lpszArgs[uCount] = (LPTSTR)GlobalAlloc(GMEM_FIXED,strlen(szArg)+1);
            strcpy(lpszArgs[uCount],szArg);
            uCount++;
        }
    }

    return lpszArgs;
}

LPTSTR *convertArgStringToArgList(LPTSTR *lpszArgs, PDWORD pdwLen, LPTSTR lpszArgstring)
{
    UINT uCount;
    LPTSTR lpszArg, lpszToken;


    if(strlen(lpszArgstring) == 0){
        *pdwLen = 0;
        lpszArgs = NULL;
        return NULL;
    }

    if(NULL == (lpszArg = (LPTSTR)GlobalAlloc(GMEM_FIXED,strlen(lpszArgstring)+1))){
        *pdwLen = 0;
        lpszArgs = NULL;
        return NULL;
    }

    strcpy(lpszArg, lpszArgstring);

    lpszToken = strtok( lpszArg, "|" ); 
    uCount = 0;
    while( lpszToken != NULL ){
        uCount++;
        lpszToken = strtok( NULL, "|");   
    }

    GlobalFree((HGLOBAL)lpszArg);

    lpszArgs = (LPTSTR *)GlobalAlloc(GMEM_FIXED,uCount * sizeof(LPTSTR));
    *pdwLen = uCount;


    lpszToken = strtok(lpszArgstring,"|");
    uCount = 0;
    while(lpszToken != NULL){
        lpszArgs[uCount] = (LPTSTR)GlobalAlloc(GMEM_FIXED,strlen(lpszToken)+1);
        strcpy(lpszArgs[uCount],lpszToken);
        uCount++;
        lpszToken = strtok( NULL, "|"); 
    }


    return lpszArgs;

}

LPTSTR convertArgListToArgString(LPTSTR lpszTarget, DWORD dwStart, DWORD dwArgc, LPTSTR *lpszArgv)
{
    UINT uTotalsize = 0;
    UINT i;

    if(dwStart >= dwArgc){
        return NULL;
    }

    *lpszTarget = 0;

    for(i=dwStart; i<dwArgc; i++){

        if(i != dwStart){
            strcat(lpszTarget,"|");
        }
        strcat(lpszTarget,lpszArgv[i]);
    }

    return lpszTarget;
}

