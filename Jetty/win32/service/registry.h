/*
 * registry.h    1.1 (5 May 2000) 1.2 (25 Aug 2000)
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

#ifndef _REGISTRY_H
#define _REGISTRY_H


#ifdef __cplusplus
extern "C" {
#endif

//
//  FUNCTION: getStringValue()
//
//  PURPOSE: Fetches a REG_SZ or REG_EXPAND_SZ string value
//           from a specified registry key    
//
//  PARAMETERS:
//    lpVal - a string buffer for the desired value
//    lpcbLen  - pointer to LONG value with buffer length
//    hkRoot - the primary root key, e.g. HKEY_LOCAL_MACHINE
//    lpszPath - the registry path to the subkey containing th desired value
//    lpszValue - the name of the desired value    
//
//  RETURN VALUE:
//    0 on success, 1 on failure
//
int getStringValue(LPBYTE lpVal, LPDWORD lpcbLen, HKEY hkRoot, LPCTSTR lpszPath, LPTSTR lpszValue);

//
//  FUNCTION: setStringValue()
//
//  PURPOSE: Assigns a REG_SZ value to a 
//           specified registry key    
//
//  PARAMETERS:
//    lpVal - Constant byte array containing the value
//    cbLen  - data length
//    hkRoot - the primary root key, e.g. HKEY_LOCAL_MACHINE
//    lpszPath - the registry path to the subkey containing th desired value
//    lpszValue - the name of the desired value    
//
//  RETURN VALUE:
//    0 on success, 1 on failure
//
int setStringValue(CONST BYTE *lpVal, DWORD cbLen, HKEY hkRoot, LPCTSTR lpszPath, LPCTSTR lpszValue);


//
//  FUNCTION: makeNewKey()
//
//  PURPOSE: Creates a new key at the specified path  
//
//  PARAMETERS:
//    hkRoot - the primary root key, e.g. HKEY_LOCAL_MACHINE
//    lpszPath - the registry path to the new subkey
//
//  RETURN VALUE:
//    0 on success, 1 on failure
//
int makeNewKey(HKEY hkRoot, LPCTSTR lpszPath);


#ifdef __cplusplus
}
#endif

#endif
