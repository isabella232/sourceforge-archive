/*
 * parseargs.h    1.1 (5 May 2000) 1.2 (25 Aug 2000)
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

#ifndef _PARSEARGS_H
#define _PARSEARGS_H

#include<windows.h>


#ifdef __cplusplus
extern "C" {
#endif

//
//  FUNCTION: getWorkingDirectory()
//
//  PURPOSE: Get the working directory for the application based
//           on the wrkdir= command line value.
//
//  PARAMETERS:
//		dwArgc - The length of the argument list
//		lpszArgv - The argument list
//
//  RETURN VALUE:
//    String containing the name of the assigned working directory or
//    NULL on failure
//
LPTSTR getWorkingDirectory(DWORD dwArgc, LPTSTR *lpszArgv);

//
//  FUNCTION: getJavaArgs()
//
//  PURPOSE: Return an array of strings containing all arguments that
//           begin with -D or /D (case-sensitive).
//
//  PARAMETERS:
//    lpszArgs  - The string array address to be allocated and receive the data
//    pdwLen - pointer to an int that will contain the returned array length
//    dwArgc - length of the argument list
//    lpszArgv - array of strings, the argument list.  
//
//  RETURN VALUE:
//    String array address containing the filtered arguments
//    NULL on failure
//
LPTSTR *getJavaArgs(LPTSTR *lpszArgs, PDWORD pdwLen, DWORD dwArgc, LPTSTR *lpszArgv);

//
//  FUNCTION: getAppArgs()
//
//  PURPOSE: Return an array of strings containing all arguments that
//           do not begin with -D or /D (case-sensitive) or wrkdir=
//
//  PARAMETERS:
//    lpszArgs  - The string array address to be allocated and receive the data
//    pdwLen - pointer to an int that will contain the returned array length
//    dwArgc - length of the argument list
//    lpszArgv - array of strings, the argument list.  
//
//  RETURN VALUE:
//    String array address containing the filtered arguments
//    NULL on failure
//
LPTSTR *getAppArgs(LPTSTR *lpszArgs, PDWORD pdwLen, DWORD dwArgc, LPTSTR *lpszArgv);

//
//  FUNCTION: convertArgStringToArgList()
//
//  PURPOSE: Return an array of strings containing all arguments that
//           are parsed from an argument string, space delimited
//
//  PARAMETERS:
//    args  - The string array address to be allocated and receive the data
//    len - pointer to an int that will contain the returned array length
//    argstring - string containing arguments to be parsed.  
//
//  RETURN VALUE:
//    String array address containing the filtered arguments
//    NULL on failure
//
LPTSTR* convertArgStringToArgList(LPTSTR *args, PDWORD pdwLen, LPTSTR lpszArgstring);

//
//  FUNCTION: convertArgListToArgString()
//
//  PURPOSE: Create a single space=delimited string of arguments from
//           an argument list
//
//  PARAMETERS:
//    target - pointer to the string to be allocated and created
//    start  - zero-based offest into the list to the first arg value used to
//             build the list.
//    argc - length of the argument list
//    argv - array of strings, the argument list.  
//
//  RETURN VALUE:
//    Character pointer to the target string.
//    NULL on failure
//
LPTSTR convertArgListToArgString(LPTSTR lpszTarget, DWORD dwStart, DWORD dwArgc, LPTSTR *lpszArgv);

#ifdef __cplusplus
}
#endif

#endif
