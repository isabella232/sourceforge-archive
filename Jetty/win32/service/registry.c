/*
 * registry.c    1.1 (5 May 2000) 1.2 (25 Aug 2000)
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
 
#include<windows.h>
#include<stdio.h>
#include<stdlib.h>
#include<winreg.h>

#include "registry.h"

int getStringValue(LPBYTE lpVal, LPDWORD lpcbLen, HKEY hkRoot, LPCTSTR lpszPath, LPTSTR lpszValue)
{

    LONG result;
    HKEY hKey;

    DWORD dwType;

    result = RegOpenKeyEx(
        hkRoot,
        lpszPath,
        (DWORD)0,
        KEY_EXECUTE | KEY_QUERY_VALUE,
        (PHKEY)&hKey);

    if(result != ERROR_SUCCESS){
        return 1;
    }

    result = RegQueryValueEx(
        hKey,
        lpszValue, 
        NULL, 
        (LPDWORD)&dwType, 
        lpVal, 
        lpcbLen);    

    RegCloseKey(hKey);

    return !(result == ERROR_SUCCESS && 
        (dwType == REG_SZ || dwType == REG_EXPAND_SZ));
}

int setStringValue(CONST BYTE *lpVal, DWORD cbLen, HKEY hkRoot, LPCTSTR lpszPath, LPCTSTR lpszValue)
{

    LONG result;
    HKEY hKey;

    DWORD dwType = REG_SZ;

    result = RegOpenKeyEx(
        hkRoot,
        lpszPath,
        (DWORD)0,
        KEY_WRITE,
        (PHKEY)&hKey);

    if(result != ERROR_SUCCESS){
        return 1;
    }

    result = RegSetValueEx(
        hKey,
        lpszValue, 
        (DWORD)0, 
        dwType, 
        lpVal, 
        cbLen);    

    RegCloseKey(hKey);

    return !(result == ERROR_SUCCESS);
}

int makeNewKey(HKEY hkRoot, LPCTSTR lpszPath)
{
    char *classname = "LocalSystem";

    LONG result;
    HKEY hKey;
    DWORD disposition;


    result = RegCreateKeyEx(
        hkRoot,
        lpszPath,
        (DWORD)0,
        classname,
        REG_OPTION_NON_VOLATILE,
        KEY_ALL_ACCESS,
        NULL,
        (PHKEY)&hKey,
        (LPDWORD) &disposition);

    if(result != ERROR_SUCCESS){
        return 1;
    }


    RegCloseKey(hKey);

    return !(result == ERROR_SUCCESS);
}



