/*
 * Created on 25-Sep-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mortbay.io.stream;

import java.io.IOException;
import java.io.InputStream;

import org.mortbay.io.ByteArrayBuffer;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class InputBuffer extends ByteArrayBuffer 
{
	InputStream _in;
	
    public InputBuffer(InputStream in, int bufferSize) 
    {
        super(new byte[bufferSize],0,0);
        _in=in;
    }
	
    /* (non-Javadoc)
     * @see org.mortbay.util.Buffer#fill()
     */
    public int fill() 
    {
    	int space=capacity()-putIndex();
    	if (space<=0)
    		return 0;
    	
    	try
    	{
	    	byte[] bytes = array();
		    int n=_in.read(bytes,putIndex(),space);
		    if (n>=0)
		    {
		    	setPutIndex(putIndex()+n);
				return n;
		    }
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    	
    	return -1;
    
    }

}
