// ===========================================================================
// Copyright (c) 2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

import javax.servlet.http.Cookie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.Credential;


public class HashSSORealm implements SSORealm
{
    private static Log log = LogFactory.getLog(HashSSORealm.class);

    /* ------------------------------------------------------------ */
    public static final String SSO_COOKIE_NAME = "SSO_ID";
    private HashMap _ssoId2Principal = new HashMap();
    private HashMap _ssoUsername2Id = new HashMap();
    private HashMap _ssoPrincipal2Credential = new HashMap();
    private transient Random _random = new SecureRandom();
    
    /* ------------------------------------------------------------ */
    public Credential getSingleSignOn(HttpRequest request,
                                      HttpResponse response)
    {
        String ssoID = null;
        Cookie[] cookies = request.getCookies();
        for (int i = 0; i < cookies.length; i++)
        {
            if (cookies[i].getName().equals(SSO_COOKIE_NAME))
            {
                ssoID = cookies[i].getValue();
                break;
            }
        }
        if(log.isDebugEnabled())log.debug("get ssoID="+ssoID);
        
        Principal principal=null;
        Credential credential=null;
        synchronized(_ssoId2Principal)
        {
            principal=(Principal)_ssoId2Principal.get(ssoID);
            credential=(Credential)_ssoPrincipal2Credential.get(principal);
        }
        
        if(log.isDebugEnabled())log.debug("SSO principal="+principal);
        
        if (principal!=null && credential!=null)
        {
            if (response.getHttpContext().getRealm().reauthenticate(principal))
            {
                request.setUserPrincipal(principal);
                request.setAuthUser(principal.getName());
                return credential;
            }
            else
            {
                synchronized(_ssoId2Principal)
                {
                    _ssoId2Principal.remove(ssoID);
                    _ssoPrincipal2Credential.remove(principal);
                    _ssoUsername2Id.remove(principal.getName());
                }    
            }
        }
        return null;
    }
    
    
    /* ------------------------------------------------------------ */
    public void setSingleSignOn(HttpRequest request,
                                HttpResponse response,
                                Principal principal,
                                Credential credential)
    {
        
        String ssoID=null;
        
        synchronized(_ssoId2Principal)
        {
            // Create new SSO ID
            while (true)
            {
                ssoID = Long.toString(Math.abs(_random.nextLong()),
                                      30 + (int)(System.currentTimeMillis() % 7));
                if (!_ssoId2Principal.containsKey(ssoID))
                    break;
            }
            
            if(log.isDebugEnabled())log.debug("set ssoID="+ssoID);
            _ssoId2Principal.put(ssoID,principal);
            _ssoPrincipal2Credential.put(principal,credential);
            _ssoUsername2Id.put(principal.getName(),ssoID);
        }
        
        Cookie cookie = new Cookie(SSO_COOKIE_NAME, ssoID);
        cookie.setPath("/");
        response.addSetCookie(cookie);
    }
    
    
    /* ------------------------------------------------------------ */
    public void clearSingleSignOn(String username)
    {
        synchronized(_ssoId2Principal)
        {
            Object ssoID=_ssoUsername2Id.remove(username);
            Object principal=_ssoId2Principal.remove(ssoID);
            _ssoPrincipal2Credential.remove(principal);
        }        
    }
}
