/*
 * service.c    1.1 (5 May 2000) 1.2 (25 Aug 2000)
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

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <process.h>
#include <tchar.h>

#include "service.h"
#include "parseargs.h"
#include "registry.h"


//global variables
SERVICE_STATUS          ssStatus;       
SERVICE_STATUS_HANDLE   sshStatusHandle;
DWORD                   dwErr = 0;
BOOL                    bConsole = FALSE;
TCHAR                   szErr[256];

#define SZFAILURE "StartServiceControlDispatcher failed!"
#define SZSCMGRFAILURE "OpenSCManager failed - %s\n"

// Create an error message from GetLastError() using the
// FormatMessage API Call...




LPTSTR GetLastErrorText( LPTSTR lpszBuf, DWORD dwSize )
{
    DWORD dwRet;
    LPTSTR lpszTemp = NULL;



    dwRet = FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER | 
        FORMAT_MESSAGE_FROM_SYSTEM |FORMAT_MESSAGE_ARGUMENT_ARRAY,
        NULL,
        GetLastError(),
        LANG_NEUTRAL,
        (LPTSTR)&lpszTemp,
        0,
        NULL);

    // supplied buffer is not long enough
    if (!dwRet || ((long)dwSize < (long)dwRet+14)){
        lpszBuf[0] = TEXT('\0');
    }
    else{
        lpszTemp[lstrlen(lpszTemp)-2] = TEXT('\0');  //remove cr and newline character
        _stprintf( lpszBuf, TEXT("%s (0x%x)"), lpszTemp, GetLastError());
    }

    if (lpszTemp){
        GlobalFree((HGLOBAL) lpszTemp);
    }

    return lpszBuf;
}


// We'll try to install the service with this function, and save any
// runtime args for the service itself as a REG_SZ value in a registry 
// subkey

void installService(int argc, char **argv)
{
    SC_HANDLE   schService;
    SC_HANDLE   schSCManager;

    TCHAR szPath[512];

    TCHAR szAppParameters[2048];

    // Get the full path and filename of this program
    if ( GetModuleFileName( NULL, szPath, 512 ) == 0 ){
        _tprintf(TEXT("Unable to install %s - %s\n"), TEXT(SZSERVICEDISPLAYNAME), 
            GetLastErrorText(szErr, 256));
        return;
    }

    // Next, get a handle to the service control manager
    schSCManager = OpenSCManager(
                        NULL,                   
                        NULL,                   
                        SC_MANAGER_ALL_ACCESS   
                        );
    
    if ( schSCManager ){

        schService = CreateService(
            schSCManager,               // SCManager database
            TEXT(SZSERVICENAME),        // name of service
            TEXT(SZSERVICEDISPLAYNAME), // name to display
            SERVICE_ALL_ACCESS,         // desired access
            SERVICE_WIN32_OWN_PROCESS,  // service type
            SERVICESTARTTYPE    ,       // start type
            SERVICE_ERROR_NORMAL,       // error control type
            szPath,                     // service's binary
            NULL,                       // no load ordering group
            NULL,                       // no tag identifier
            TEXT(SZDEPENDENCIES),       // dependencies
            NULL,                       // LocalSystem account
            NULL);                      // no password

        if (schService){

            _tprintf(TEXT("%s installed.\n"), TEXT(SZSERVICEDISPLAYNAME) );

            // Close the handle to this service object
            CloseServiceHandle(schService);

            // Try to create a subkey to hold the runtime args for the JavaVM and
            // Java application
            if(0 != makeNewKey(HKEY_LOCAL_MACHINE, SZPARAMKEY)){
                _tprintf(TEXT("Could not create Parameters subkey.\n"));
            }else{

                //Create an argument string from the argument list
                convertArgListToArgString((LPTSTR) szAppParameters,2, argc, argv);
                if(NULL == szAppParameters){
                    _tprintf(TEXT("Could not create AppParameters string.\n"));
                }

                else{

                    // Try to save the argument string under the new subkey
                    if(0 != setStringValue(szAppParameters, strlen(szAppParameters)+1, 
                        HKEY_LOCAL_MACHINE, SZPARAMKEY, SZAPPPARAMS)){
                            _tprintf(TEXT("Could not save AppParameters value.\n"));
                    }

                }
            }

        }
        else{
            _tprintf(TEXT("CreateService failed - %s\n"), GetLastErrorText(szErr, 256));
        }

        // Close the handle to the service control manager database
        CloseServiceHandle(schSCManager);
    }
    else{
        _tprintf(TEXT(SZSCMGRFAILURE), GetLastErrorText(szErr,256));
    }
}


// We'll try to stop, and then remove the service using this function.

void removeService()
{
    SC_HANDLE   schService;
    SC_HANDLE   schSCManager;


    // First, get a handle to the service control manager
    schSCManager = OpenSCManager(
                        NULL,                   
                        NULL,                   
                        SC_MANAGER_ALL_ACCESS   
                        );
    if (schSCManager){

        // Next get the handle to this service...
        schService = OpenService(schSCManager, TEXT(SZSERVICENAME), SERVICE_ALL_ACCESS);

        if (schService){

            // Now, try to stop the service by passing a STOP code thru the control manager
            if (ControlService( schService, SERVICE_CONTROL_STOP, &ssStatus)){
                
                _tprintf(TEXT("Stopping %s."), TEXT(SZSERVICEDISPLAYNAME));
                // Wait a second...
                Sleep( 1000 );

                // Poll the status of the service for SERVICE_STOP_PENDING
                while(QueryServiceStatus( schService, &ssStatus)){

                    // If the service has not stopped, wait another second
                    if ( ssStatus.dwCurrentState == SERVICE_STOP_PENDING ){
                        _tprintf(TEXT("."));
                        Sleep( 1000 );
                    }
                    else
                        break;
                }

                if ( ssStatus.dwCurrentState == SERVICE_STOPPED )
                    _tprintf(TEXT("\n%s stopped.\n"), TEXT(SZSERVICEDISPLAYNAME) );
                else
                    _tprintf(TEXT("\n%s failed to stop.\n"), TEXT(SZSERVICEDISPLAYNAME) );
            }

            // Now try to remove the service...
            if(DeleteService(schService))
                _tprintf(TEXT("%s removed.\n"), TEXT(SZSERVICEDISPLAYNAME) );
            else
                _tprintf(TEXT("DeleteService failed - %s\n"), GetLastErrorText(szErr,256));
            
            //Close this service object's handle to the service control manager
            CloseServiceHandle(schService);
        }
        else{
            _tprintf(TEXT("OpenService failed - %s\n"), GetLastErrorText(szErr,256));
        }
        
        // Finally, close the handle to the service control manager's database
        CloseServiceHandle(schSCManager);
    }
    else{
        _tprintf(TEXT(SZSCMGRFAILURE), GetLastErrorText(szErr,256));
    }
}

// This function permits running the Java application from the 
// console.

void runService(int argc, char ** argv)
{
    DWORD dwArgc;
    LPTSTR *lpszArgv;

#ifdef UNICODE
    lpszArgv = CommandLineToArgvW(GetCommandLineW(), &(dwArgc) );
#else
    dwArgc   = (DWORD) argc;
    lpszArgv = argv;
#endif

    _tprintf(TEXT("Running %s.\n"), TEXT(SZSERVICEDISPLAYNAME));


    // Do it! But since this is a console start, skip the first two
	// arguments in the arg list being passed, and reduce its size by
	// two also. (The first two command line args should be ignored
	// in a console run.)
    ServiceStart( dwArgc - 2, lpszArgv + 2);
}


// If running as a service, use event logging to post a message
// If not, display the message on the console.

VOID AddToMessageLog(LPTSTR lpszMsg)
{
    TCHAR   szMsg[256];
    HANDLE  hEventSource;
    LPCTSTR lpszStrings[2];

    if (!bConsole)
    {
        dwErr = GetLastError();

        hEventSource = RegisterEventSource(NULL, TEXT(SZSERVICENAME));

        _stprintf(szMsg, TEXT("%s error: %d"), TEXT(SZSERVICENAME), dwErr);
        lpszStrings[0] = szMsg;
        lpszStrings[1] = lpszMsg;

        if (hEventSource != NULL) {
            ReportEvent(hEventSource, 
                EVENTLOG_ERROR_TYPE,  
                0,                    
                0,                    
                NULL,                 
                2,                    
                0,                    
                lpszStrings,          
                NULL);                

            DeregisterEventSource(hEventSource);
        }
    }
    else{
        _tprintf(TEXT("%s\n"), lpszMsg);
    }
}


// Throughout the program, calls to SetServiceStatus are required
// which are handled by calling this function. Here, the non-constant
// members of the SERVICE_STATUS struct are assigned and SetServiceStatus
// is called with the struct. Note that we will not report to the service
// control manager if we are running as  console application.

BOOL ReportStatus(DWORD dwCurrentState,
                         DWORD dwWin32ExitCode,
                         DWORD dwWaitHint)
{
    static DWORD dwCheckPoint = 1;
    BOOL bResult = TRUE;


    if ( !bConsole ) 
    {
        if (dwCurrentState == SERVICE_START_PENDING)
            ssStatus.dwControlsAccepted = 0;
        else
            ssStatus.dwControlsAccepted = SERVICE_ACCEPT_STOP;

        ssStatus.dwCurrentState = dwCurrentState;
        ssStatus.dwWin32ExitCode = dwWin32ExitCode;
        ssStatus.dwWaitHint = dwWaitHint;

        if ( ( dwCurrentState == SERVICE_RUNNING ) ||
             ( dwCurrentState == SERVICE_STOPPED ) )
            ssStatus.dwCheckPoint = 0;
        else
            ssStatus.dwCheckPoint = dwCheckPoint++;

        if (!(bResult = SetServiceStatus( sshStatusHandle, &ssStatus))) {
            AddToMessageLog(TEXT("SetServiceStatus"));
        }
    }

    return bResult;
}

// Each Win32 service must have a control handler to respond to
// control requests from the dispatcher.

VOID WINAPI controlHandler(DWORD dwCtrlCode)
{

    switch(dwCtrlCode)
    {

        case SERVICE_CONTROL_STOP:
            // Request to stop the service. Report SERVICE_STOP_PENDING
            // to the service control manager before calling ServiceStop()
            // to avoid a "Service did not respond" error.
            ReportStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
            ServiceStop(bConsole);
            return;

        
        case SERVICE_CONTROL_INTERROGATE:
            // This case MUST be processed, even though we are not
            // obligated to do anything substantial in the process.
            break;

         default:
            // Any other cases...
            break;

    }

    // After invocation of this function, we MUST call the SetServiceStatus
    // function, which is accomplished through our ReportStatus function. We
    // must do this even if the current status has not changed.
    ReportStatus(ssStatus.dwCurrentState, NO_ERROR, 0);
}

// The ServiceMain function is the entry point for the service.

void WINAPI serviceMain(DWORD dwArgc, LPTSTR *lpszArgv)
{
    
    TCHAR szAppParameters[1024];
    LONG lLen = 1024;

    LPTSTR *lpszNewArgv = NULL;
    DWORD dwNewArgc;
    
    UINT i;


 
    // Call RegisterServiceCtrlHandler immediately to register a service control 
    // handler function. The returned SERVICE_STATUS_HANDLE is saved with global 
    // scope, and used as a service id in calls to SetServiceStatus.
    sshStatusHandle = RegisterServiceCtrlHandler( TEXT(SZSERVICENAME), controlHandler);

    if (!sshStatusHandle)
        goto finally;

    // The global ssStatus SERVICE_STATUS structure contains information about the
    // service, and is used throughout the program in calls made to SetStatus through
    // the ReportStatus function.
    ssStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
    ssStatus.dwServiceSpecificExitCode = 0;


    // If we could guarantee that all initialization would occur in less than one
    // second, we would not have to report our status to the service control manager.
    // For good measure, we will assign SERVICE_START_PENDING to the current service
    // state and inform the service control manager through our ReportStatus function.
    if (!ReportStatus(SERVICE_START_PENDING, NO_ERROR, 3000))
        goto finally;

    // When we installed this service, we probably saved a list of runtime args
    // in the registry as a subkey of the key for this service. We'll try to get
    // it here...
    if(0 != getStringValue(szAppParameters,&lLen, HKEY_LOCAL_MACHINE, SZPARAMKEY, SZAPPPARAMS)){
        dwNewArgc = 0;
        lpszNewArgv = NULL;
    }
    else{
        //If we have an argument string, convert it to a list of argc/argv type...
        lpszNewArgv = convertArgStringToArgList(lpszNewArgv, &dwNewArgc, szAppParameters);
    }

    // Do it! In ServiceStart, we'll send additional status reports to the
    // service control manager, especially the SERVICE_RUNNING report once 
    // our JVM is initiallized and ready to be invoked.
    ServiceStart(dwNewArgc, lpszNewArgv);

    // Release the allocated storage used by our arg list. Java programmers
    // might remember this kind of stuff.
    for(i=0; i<dwNewArgc; i++){
        GlobalFree((HGLOBAL)lpszNewArgv[i]);
    }
    if(dwNewArgc > 0)
        GlobalFree((HGLOBAL)lpszNewArgv);

finally:

    // Report the stopped status to the service control manager, if we have
    // a valid server status handle.
     if (sshStatusHandle)
        (VOID)ReportStatus( SERVICE_STOPPED, dwErr, 0);
}

// Display instructions for use...

void printUsage()
{
    printf("\nUsage:\n\n");
    printf("%s <options> [<-D|-X>JavaVMArg1 <-D|-X>JavaVMArg2...] [JavaAppArg1 JavaAppArg2...] [wrkdir=pathname]\n\n",
        SZAPPNAME);
    printf("Options:\n\n");
    printf("-i  to install the service\n");
    printf("-r  to remove the service\n");
    printf("-c  to run as a console app\n\n");
    printf("-D  precedes arguments to be passed to the Java VM\n");
    printf("-X  precedes arguments to be passed to the Java VM\n");
	printf("wrkdir=path to working directory. winnt/system32 is the default\n");
	printf("       if none is specified.\n\n");
	printf("All other arguments are passed to the Java application.\n\n");
    printf("Example:\n\n");
    printf("%s -i -Djava.class.path=c:\\myapp\\lib\\myapp.jar apparg1 wrkdir=c:\\myapp\\support\n\n", SZAPPNAME);
    printf("The above installs the coded Java app as a service, supplies the -D prefixed\n");
    printf("arguments to the JVM, and changes to the specified working directory. Other\n");
    printf("arguments are supplied to the Java app's main class when the service starts.\n");
}


int _CRTAPI1 main(int argc, char **argv)
{
    // The StartServiceCtrlDispatcher requires this table to specify
    // the ServiceMain function to run in the calling process. The first
    // member in this example is actually ignored, since we will install
    // our service as a SERVICE_WIN32_OWN_PROCESS service type. The NULL
    // members of the last entry are necessary to indicate the end of 
    // the table;
    SERVICE_TABLE_ENTRY serviceTable[] =
    {
        { TEXT(SZSERVICENAME), (LPSERVICE_MAIN_FUNCTION)serviceMain },
        { NULL, NULL }
    };

	if(NULL == strstr(SZPARAMKEY,SZSERVICENAME)){
		printf("\nSZSERVICENAME service name does not match SZPARAMKEY\n");
		printf("Please correct this in service.h and recompile.\n");
		exit(0);
	}


    // This app may be started with one of three arguments, -i, -r, and
    // -c, or -?, followed by actual program arguments. These arguments 
    // indicate if the program is to be installed, removed, run as a 
    // console application, or to display a usage message.
    if(argc > 1){
        if(!_stricmp(argv[1],"-i") || !_stricmp(argv[1],"/i")){
            installService(argc,argv);
        }
        else if(!_stricmp(argv[1],"-r") || !_stricmp(argv[1],"/r")){
            removeService(argc,argv);
        }
        else if(!_stricmp(argv[1],"-c") || !_stricmp(argv[1],"/c")){
            bConsole = TRUE;
            runService(argc,argv);
        }
        else if(!_stricmp(argv[1],"-?") || !_stricmp(argv[1],"/?")){
            printUsage();
        }
        else{
            printf("\nUnrecognized option: %s\n", argv[1]);
            printUsage();
        }
        exit(0);
    }

    // If main is called without any arguments, it will probably be by the
    // service control manager, in which case StartServiceCtrlDispatcher
    // must be called here. A message will be printed just in case this 
    // happens from the console.

    printf("\nCalling StartServiceCtrlDispatcher...please wait.\n");
    if (!StartServiceCtrlDispatcher(serviceTable)){
            printf("\n%s\n", SZFAILURE);    
            printf("\nFor help, type\n\n%s /?\n\n", SZAPPNAME);
            AddToMessageLog(TEXT(SZFAILURE));
    }
}

