// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas.callback;

import javax.security.auth.callback.Callback;


/* ---------------------------------------------------- */
/** ObjectCallback
 *
 * <p>Can be used as a LoginModule Callback to
 * obtain a user's credential as an Object, rather than
 * a char[], to which some credentials may not be able
 * to be converted
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Tue Apr 15 2003
 * @author Jan Bartel (janb)
 */
public class ObjectCallback implements Callback
{

    protected Object _object;
    
    public void setObject(Object o)
    {
        _object = o;
    }

    public Object getObject ()
    {
        return _object;
    }


    public void clearObject ()
    {
        _object = null;
    }
    
    
}
