/*
 * jettysvc.c adapted from:
 *
 * javaservice.c    1.1 (5 May 2000) 1.2 (25 Aug 2000)
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
 * provided with Microsoft Visual C++ 4.2, and from sample code
 * supporting Sun Microsystems Inc. JNI Java Tutorial
 */


#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <process.h>
#include <tchar.h>
#include <jni.h>

//#include<pthread.h>

#include "service.h"
#include "parseargs.h"

#define MAX_OPTIONS 20

HANDLE  hServerStopEvent = NULL;
LPTSTR *lpszJavaArgs=NULL, *lpszAppArgs=NULL;
DWORD dwJLen=0,dwALen=0;
LPTSTR wrkdir;

//We'll make the following references global, as we'll use these
//to call the java runtime, with a particular class and methodID to
//pass Service Control Manager events to our Java app. In this
//example, we will only pass a Service Stopped event. In order
//to pass SCM events, the java app must implement SCMEventListener
//and register with a singleton instance of SCMEventManager with
//addSCMEventListener.

static JNIEnv *env;
static jclass emgrc;
static JavaVM *vm;


// The following code invokes the JVM with the main class of the Java app
// that will run as a service. Portions are based on example source code
// at the Java Tutorial web site, with modifications for Java 1.2, multi-
// threaded processing, and multiple arguments.

// Console event handler routine.
BOOL WINAPI logoffHandler(DWORD dwCtrlType)
{
    switch (dwCtrlType)
    {

      case CTRL_C_EVENT:
      case CTRL_CLOSE_EVENT:
      case CTRL_SHUTDOWN_EVENT:
          ServiceStop();
          return TRUE;

      case CTRL_LOGOFF_EVENT:
          return TRUE;
    }
    return FALSE;
}



void _CRTAPI1 invokeJVM(void *dummy)
{
    jint res;
    jclass cls;    
    jmethodID mid;
    jstring jstr;
    jobjectArray args;
    JavaVMInitArgs vm_args;
    JavaVMOption options[MAX_OPTIONS];



    UINT i;

    if(NULL != wrkdir)
    {
        if(0 != _chdir(wrkdir))
        {
            AddToMessageLog(TEXT("Unable to change working directory."));
        }
    }


    if(dwJLen > MAX_OPTIONS)
    {
        AddToMessageLog(TEXT("Max. number of Java args exceeded."));
        return;
    }

    // Assign the arguments for the JVM, such as the classpath,
    // RMI codebase, etc.
    for(i=0; i<dwJLen; i++)
    {
        options[i].optionString = lpszJavaArgs[i];
    }

    vm_args.version = JNI_VERSION_1_2;
    vm_args.options = options;
    vm_args.nOptions = dwJLen;
    vm_args.ignoreUnrecognized = TRUE;

    res =JNI_CreateJavaVM(&vm, (void **)&env, &vm_args);

    if (res < 0)
    {
        AddToMessageLog(TEXT("Cannot create Java VM."));
        return;
    }

    // Get the main class
    cls = (*env)->FindClass(env, SZMAINCLASS);

    if (cls == 0)
    {
        AddToMessageLog(TEXT("Cannot find main class."));
        return;
    }

    // Get the method ID for the class's main(String[]) function. 
    mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");

    if (mid == 0)
    {
        AddToMessageLog(TEXT("Cannot find main method."));
        return;
    }

    // If there are arguments, create an ObjectArray sized to contain the
    // argument list, and then scan the list, inserting each argument into
    // the ObjectArray.
    if(dwALen > 0)
    {
        args = (*env)->NewObjectArray(env, dwALen,
                                      (*env)->FindClass(env, "java/lang/String"), NULL);
        if (args == 0)
        {
            AddToMessageLog(TEXT("Out of Memory!"));
            return;
        }
        for(i=0; i<dwALen; i++)
        {
            jstr = (*env)->NewStringUTF(env, lpszAppArgs[i]);
            if (jstr == 0)
            {
                AddToMessageLog(TEXT("Out of Memory!"));
                return;
            }
            (*env)->SetObjectArrayElement(env, args, i, jstr); 
        }
    }

    // Otherwise, create an empty array. This is needed to avoid
    // creating an overloaded main that takes no arguments in the Java
    // app, and then getting a different method ID to the no-argument
    // main() method in this invoker code.
    else
    {
        args = (*env)->NewObjectArray(env, 0,
                                      (*env)->FindClass(env, "java/lang/String"), NULL);
    }


    //Now, get the class of the java SCMEventManager
    emgrc  = (*env)->FindClass(env, SZMAINCLASS);
    if (emgrc  == 0)
        AddToMessageLog(TEXT("Cannot find SZMAINCLASS class."));

    // Run the main class...
    (*env)->CallStaticVoidMethod(env, cls, mid, args);

    SetConsoleCtrlHandler(logoffHandler,TRUE);

}

//Here we attempt to call a java instance method from native code. We obtained
//a global reference to the singleton SCMEventManager object associated with our
//Java application.
//
//Call this method to pass an event ID to Java's SCMEventManager.dispatchSCMEvent
//In this example, we only call this from the ServiceStopped() function, below,
//using eventid = 0, which our Java app is programmed to recognize as a Stop
//Service event. Java's SCMEventManager.dispatchSCMEvent will then pass a new
//SCMEvent object (initiallized with eventid) to all registered SCMEventListeners
//The listeners provide a handleSCMEvent method, to take some action in response
//to the event.

VOID PassSCMEvent(int eventid)
{
    jint j = (jint)eventid;


    jmethodID mid;
    jclass cls;

    (*vm)->AttachCurrentThread(vm,(void **)&env,NULL);

    //emgrc is the global reference to the SCMEventManager class
    mid = (*env)->GetStaticMethodID(env, emgrc, "dispatchSCMEvent", "(I)V");
    if (mid == 0)
    {
        AddToMessageLog(TEXT("Cannot find SCMEventManager.dispatchSCMEvent."));
        goto finished;
    }

    (*env)->CallStaticVoidMethod(env, emgrc, mid, j);

    (*vm)->DetachCurrentThread(vm);

 finished:
    ;
}

    
// This method is called from ServiceMain() when NT starts the service
// or by runService() if run from the console.

VOID ServiceStart (DWORD dwArgc, LPTSTR *lpszArgv)
{


    // Let the service control manager know that the service is
    // initializing.
    if (!ReportStatus(SERVICE_START_PENDING, 
                      NO_ERROR,              
                      3000))                 
        //goto cleanup;
        return;


    // Create a Stop Event
    hServerStopEvent = CreateEvent(NULL,    
                                   TRUE,    
                                   FALSE,   
                                   NULL);


    if ( hServerStopEvent == NULL)
        goto cleanup;

    lpszJavaArgs = getJavaArgs(lpszJavaArgs, &dwJLen, dwArgc, lpszArgv);

    lpszAppArgs = getAppArgs(lpszAppArgs, &dwALen, dwArgc, lpszArgv);

    wrkdir = getWorkingDirectory(dwArgc, lpszArgv);

    if (!ReportStatus(SERVICE_RUNNING,NO_ERROR,0))
    {
        goto cleanup;
    }

    // After the initialization is complete (we've checked for arguments) and
    // the service control manager has been told the service is running, invoke
    // the Java application. If clients are unable to access 
    // the server, check the event log for messages that should indicate any errors
    // that may have occured while firing up Java...

    invokeJVM(NULL);

    // Wait for the stop event to be signalled.
    WaitForSingleObject(hServerStopEvent,INFINITE);

 cleanup:

    if (hServerStopEvent)
        CloseHandle(hServerStopEvent);
}

VOID ServiceStop()
{
    UINT i;

    if(0 != emgrc)
        PassSCMEvent(SERVICE_STOPPED);

    // Release any allocated data and pointers
    for(i=0;i<dwJLen;i++){
        GlobalFree((HGLOBAL)lpszJavaArgs[i]);
    }
    if(lpszJavaArgs > 0)
        GlobalFree((HGLOBAL)lpszJavaArgs);

    for(i=0;i<dwALen;i++){
        GlobalFree((HGLOBAL)lpszAppArgs[i]);
    }
    if(lpszAppArgs > 0)
        GlobalFree((HGLOBAL)lpszAppArgs);

    // Signal the stop event.
    if ( hServerStopEvent ){
        SetEvent(hServerStopEvent);
    }

}



