// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;
import java.io.IOException;
import java.util.HashMap;
import org.mortbay.util.TypeUtil;


/* ------------------------------------------------------------ */
/** Exception for EOF detected. 
 *
 * @version $$
 * @author Greg Wilkins (gregw)
 */
public class EOFException extends IOException
{
    private IOException _ex;

    public IOException getTargetException()
    {
        return _ex;
    }
    
    
    public EOFException()
    {}
    
    public EOFException(IOException ex)
    {
        _ex=ex;
    }

    public String toString()
    {
        return "EOFException("+
            (_ex==null?"":(_ex.toString()))+
            ")";
    }
}
