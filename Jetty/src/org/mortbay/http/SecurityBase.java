// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.util.Code;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** 
 */
public class SecurityBase
{
    public final static String __BASIC_AUTH="BASIC";
    public final static String __FORM_AUTH="FORM";
    public final static String __CERT_AUTH="CLIENT-CERT";    
    
    /* ------------------------------------------------------------ */
    protected PathMap _constraintMap=new PathMap();
    protected UserRealm _realm;
    protected Authenticator _authenticator;
    
    /* ------------------------------------------------------------ */
    public UserRealm getUserRealm()
    {        
        return _realm;
    }
    
    /* ------------------------------------------------------------ */
    public void setUserRealm(UserRealm realm)
    {
        _realm=realm;
    }
    
    /* ------------------------------------------------------------ */
    public Authenticator getAuthenticator()
    {        
        return _authenticator;
    }
    
    /* ------------------------------------------------------------ */
    public void setAuthenticator(Authenticator authenticator)
    {
        _authenticator=authenticator;
    }
    
    /* ------------------------------------------------------------ */
    public void addSecurityConstraint(String pathSpec, SecurityConstraint sc)
    {
        List scs = (List)_constraintMap.get(pathSpec);
        if (scs==null)
        {
            scs=new ArrayList(2);
            _constraintMap.put(pathSpec,scs);
        }
        scs.add(sc);
        
        Code.debug("added ",sc," at ",pathSpec);
    }

    /* ------------------------------------------------------------ */
    public boolean isAuthConstrained()
    {
        Iterator i = _constraintMap.values().iterator();
        while(i.hasNext())
        {
            Iterator j= ((ArrayList)i.next()).iterator();
            while(j.hasNext())
            {
                SecurityConstraint sc = (SecurityConstraint)j.next();
                if (sc.isAuthenticate())
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    /* ------------------------------------------------------------ */
    public boolean check(String pathInContext,
                         HttpRequest request,
                         HttpResponse response)
        throws HttpException, IOException
    {
        // Get all path matches
        List scss =_constraintMap.getMatches(pathInContext);
        if (scss!=null)
        {          
            Code.debug("Security Constraint on ",pathInContext," against ",scss);
            
            // for each path match
        matches:
            for (int m=0;m<scss.size();m++)
            {
                // Get all constraints
                Map.Entry entry=(Map.Entry)scss.get(m);
                if (Code.verbose())
                    Code.debug("Check ",pathInContext," against ",entry);

                List scs = (List)entry.getValue();
                
                switch (SecurityConstraint.check(scs,
                                                 _authenticator,
                                                 _realm,
                                                 pathInContext,
                                                 request,
                                                 response))
                {
                  case -1: return false; // Auth failed.
                  case 0: continue; // No constraints matched
                  case 1: break matches; // Passed a constraint.
                }
            }
        }
        
        return true;
    }
   

}

