
// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/* ------------------------------------------------------------ */
/** Wraps multiple exceptions.
 *
 * @version 1.0 Thu Mar 29 2001
 * @author Greg Wilkins (gregw)
 */
public class MultiException extends Exception
{
    private List nested;

    /* ------------------------------------------------------------ */
    public MultiException()
    {
        super("Multiple exceptions");
    }

    /* ------------------------------------------------------------ */
    public void add(Exception e)
    {
        if(nested==null)
            nested=new ArrayList();
        nested.add(e);
    }

    /* ------------------------------------------------------------ */
    public int size()
    {
        if (nested==null)
            return 0;
        return nested.size();
    }
    
    /* ------------------------------------------------------------ */
    public List getExceptions()
    {
        if (nested==null)
            return Collections.EMPTY_LIST;
        return Collections.unmodifiableList(nested);
    }
    
    /* ------------------------------------------------------------ */
    public Exception getException(int i)
    {
        if (nested==null)
            return null;
        return (Exception)nested.get(i);
    }

    /* ------------------------------------------------------------ */
    /** Throw a multiexception.
     * If this multi exception is empty then no action is taken. If it
     * contains a single exception that is thrown, otherwise the this
     * multi exception is thrown. 
     * @exception Exception 
     */
    public void ifExceptionThrow()
        throws Exception
    {
        if (nested!=null)
        {
            if (nested.size()==1)
                throw (Exception)nested.get(0);
            throw this;
        }
    }
    
}
