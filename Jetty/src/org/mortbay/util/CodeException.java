// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package org.mortbay.util;

/* ------------------------------------------------------------ */
/** Code Exception.
 * 
 * Thrown by Code.assert or Code.fail
 * @see Code
 * @version  $Id$
 * @author Greg Wilkins
 */
public class CodeException extends RuntimeException
{
    /* ------------------------------------------------------------ */
    /** Default constructor. 
     */
    public CodeException()
    {
        super();
    }

    public CodeException(String msg)
    {
        super(msg);
    }    
}

