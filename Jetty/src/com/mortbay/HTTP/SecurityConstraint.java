// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;



public class SecurityConstraint
    implements Cloneable
{
    public final static int
        DC_NONE=0,
        DC_INTEGRAL=1,
        DC_CONFIDENTIAL=2;
    
    private String _name;
    private List _methods;
    private List _roles;
    private int _dataConstraint=DC_NONE;


    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public SecurityConstraint()
    {}

    /* ------------------------------------------------------------ */
    /** Conveniance Constructor. 
     * @param name 
     * @param role 
     */
    public SecurityConstraint(String name,String role)
    {
        setName(name);
        addRole(role);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name 
     */
    public void setName(String name)
    {
        _name=name;
    }    

    /* ------------------------------------------------------------ */
    /** 
     * @param method 
     */
    public void addMethod(String method)
    {
        if (_methods==null)
            _methods=new ArrayList(3);
        _methods.add(method);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param method 
     * @return True if this constraint applies to the method. 
     */
    public boolean forMethod(String method)
    {
        if (_methods==null)
            return true;
        return _methods.contains(method);
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param role 
     */
    public void addRole(String role)
    {
        if (_roles==null)
            _roles=new ArrayList(3);
        _roles.add(role);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Iterator of role names
     */
    public Iterator roles()
    {
        if (_roles==null)
            return Collections.EMPTY_LIST.iterator();
        return _roles.iterator();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the constraint requires request authentication
     */
    public boolean isAuthenticated()
    {
        return _roles!=null && _roles.size()>0;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param c 
     */
    public void setDataConstraint(int c)
    {
        if (c<0 || c>DC_CONFIDENTIAL)
            throw new IllegalArgumentException("Constraint out of range");
        _dataConstraint=c;
    }


    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public int getDataConstraint()
    {
        return _dataConstraint;
    }

    
    /* ------------------------------------------------------------ */
    public Object clone()
    {
        SecurityConstraint sc=null;
        try{
            sc = (SecurityConstraint)super.clone();
            if (_methods!=null)
                sc._methods=new ArrayList(_methods);
            if (_roles!=null)
                sc._roles=new ArrayList(_roles);
        }
        catch (CloneNotSupportedException e)
        {
            Code.fail("Oh yes it does");
        }
        return sc;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String toString()
    {
        return "SC{"+_name+
            ","+_methods+
            ","+_roles+
            ","+(_dataConstraint==DC_NONE
                 ?"NONE}"
                 :(_dataConstraint==DC_INTEGRAL?"INTEGRAL}":"CONFIDENTIAL}"));
    }
}
