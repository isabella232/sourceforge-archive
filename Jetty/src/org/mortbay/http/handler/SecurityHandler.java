// ========================================================================
// Copyright (c) 1999-2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.ClientCertAuthenticator;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.PathMap;
import org.mortbay.http.SecurityBase;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.util.B64Code;
import org.mortbay.util.Code;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** Handler to enforce SecurityConstraints.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SecurityHandler extends NullHandler
{   
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private String _authMethod=SecurityBase.__BASIC_AUTH;
    private Map _authRealmMap;
    private String _realmName ;
    private boolean _realmForced=false;
    private SecurityBase _securityBase=new SecurityBase();

    /* ------------------------------------------------------------ */
    public SecurityBase getSecurityBase()
    {
        return _securityBase;
    }
    
    /* ------------------------------------------------------------ */
    public UserRealm getUserRealm()
    {        
        return _securityBase.getUserRealm();
    }
    
    /* ------------------------------------------------------------ */
    public String getRealmName()
    {        
        return _realmName;
    }
    
    /* ------------------------------------------------------------ */
    public void setRealmName(String realmName)
    {
        if (isStarted() &&
            ((_realmName!=null && !_realmName.equals(realmName)) ||
             (_realmName==null && realmName!=null)))
            throw new IllegalStateException("Handler started");
        _realmName=realmName;
        _realmForced=false;
    }
    
    /* ------------------------------------------------------------ */
    public void setRealm(String realmName, UserRealm realm)
    {
        if (isStarted())
            throw new IllegalStateException("Handler started");
        _realmName=realmName;
        _realmForced=realm!=null;
        _securityBase.setUserRealm(realm);
    }
    
    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return _authMethod;
    }
    
    /* ------------------------------------------------------------ */
    public void setAuthMethod(String method)
    {
        if (isStarted() && _authMethod!=null && !_authMethod.equals(method))
            throw new IllegalStateException("Handler started");
        _authMethod = method;
    }
    
    /* ------------------------------------------------------------ */
    public void addSecurityConstraint(String pathSpec,
                                      SecurityConstraint sc)
    {
        _securityBase.addSecurityConstraint(pathSpec,sc);
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        // Check there is a realm
        if (_realmName!=null && _realmName.length()>0)
        {
            
            if (!_realmForced)
                _securityBase.setUserRealm(getHttpContext().getHttpServer().getRealm(_realmName));
            super.start();
            if (_securityBase.getUserRealm()==null)
                Code.warning("Unknown realm: "+_realmName+" for "+this);
        }
        // Or that we have some contraints.
        else if (_securityBase.isAuthConstrained())
        {
            Code.warning("No Realm set for "+this);
            super.start();
            return;
        }

        if (_securityBase.getAuthenticator()==null)
        {
            // Find out the Authenticator.
            if (SecurityBase.__BASIC_AUTH.equalsIgnoreCase(_authMethod))
                _securityBase.setAuthenticator(new BasicAuthenticator());
            else if (SecurityBase.__CERT_AUTH.equalsIgnoreCase(_authMethod))
                _securityBase.setAuthenticator(new ClientCertAuthenticator());
            else
                Code.warning("Unknown Authentication method:"+_authMethod);
        }
        
        super.start();
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        _securityBase.check(pathInContext,request,response);
    }

}

