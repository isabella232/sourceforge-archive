/*
 * service.h    1.1 (5 May 2000) 1.2 (25 Aug 2000)
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
 *
 * Portions of this source code were adapted from sample code
 * provided with Microsoft Visual C++ 4.2 
 */

#ifndef _SERVICE_H
#define _SERVICE_H


#ifdef __cplusplus
extern "C" {
#endif

// =========================================================
// TO DO: change as needed for specific Java app and service
// =========================================================
// internal name of the service
#define SZSERVICENAME        "JettyHttpServer"
// displayed name of the service
#define SZSERVICEDISPLAYNAME "Jetty HTTP Server"
// list of service dependencies - "dep1\0dep2\0\0"
#define SZDEPENDENCIES       "\0\0"
// Main java class
#define SZMAINCLASS "org/mortbay/jetty/win32/Service"
// Service TYPE
#define SERVICESTARTTYPE SERVICE_AUTO_START 
// Path to Parameter Key
#define SZPARAMKEY "SYSTEM\\CurrentControlSet\\Services\\JettyHttpServer\\Parameters"
// name of the executable
#define SZAPPNAME           "jettysvc"
// Value name for app parameters
#define SZAPPPARAMS         "AppParameters"



//
//  FUNCTION: ReportStatusToSCMgr()
//
//  PURPOSE: Sets the current status of the service and
//           reports it to the Service Control Manager
//
//  PARAMETERS:
//    dwCurrentState - the state of the service
//    dwWin32ExitCode - error code to report
//    dwWaitHint - worst case estimate to next checkpoint
//
//  RETURN VALUE:
//    TRUE  - success 
//    FALSE - failure
//
BOOL ReportStatus(DWORD dwCurrentState, DWORD dwWin32ExitCode, DWORD dwWaitHint);


//
//  FUNCTION: AddToMessageLog(LPTSTR lpszMsg)
//
//  PURPOSE: Allows any thread to log an error message
//
//  PARAMETERS:
//    lpszMsg - text for message
//
//  RETURN VALUE:
//    none
//
void AddToMessageLog(LPTSTR lpszMsg);

VOID ServiceStart(DWORD dwArgc, LPTSTR *lpszArgv);
VOID ServiceStop();

#ifdef __cplusplus
}
#endif

#endif
