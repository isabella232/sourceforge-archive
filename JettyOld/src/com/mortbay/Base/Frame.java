// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.Base;

import java.util.*;
import java.io.*;


/*-----------------------------------------------------------------------*/
/** Access the current execution frame
 * <p> This is derived from ISS.Base, which was derived from
 * the original com.mortbay.Base.  It is intended that derived
 * com.mortbay.Log classes will pass this debug to ISS.Base.
 */
public class Frame
{
    /*-------------------------------------------------------------------*/
    /** Shared static instances, reduces object creation at expense
     * of lock contention in multi threaded debugging */
    private static Throwable __throwable = new Throwable();
    private static StringWriter __stringWriter = new StringWriter();
    private static StringBuffer __writerBuffer = __stringWriter.getBuffer();
    private static PrintWriter __out = new PrintWriter(__stringWriter,false);
    
    /*-------------------------------------------------------------------*/
    public String _stack;
    public String _method= "unknownMethod";
    public int _depth=1;
    public String _thread= "unknownThread";
    public String _file= "UnknownFile";

    String _where;
    private int _lineStart=0;
    private int _lineEnd;
    
    /*-------------------------------------------------------------------*/
    /** Construct a frame
     */
    public Frame()
    {
	// Dump the stack
	synchronized(__writerBuffer)
	{
	    __writerBuffer.setLength(0);
	    __throwable.fillInStackTrace();
	    __throwable.printStackTrace(__out);
	    __out.flush();
	    _stack = __writerBuffer.toString();
	}

	// Extract stack components
	_lineStart=_stack.indexOf("\n",_lineStart)+1;
	_lineStart=_stack.indexOf("\n",_lineStart)+1;
	_lineEnd=_stack.indexOf("\n",_lineStart);
	_where= _stack.substring(_lineStart,_lineEnd);

	complete();
    }
    
    /*-------------------------------------------------------------------*/
    /** Construct a frame
     * @param ignoreFrames number of levels of stack to ignore
     */
    public Frame(int ignoreFrames)
    {
	// Dump the stack
	synchronized(__writerBuffer)
	{
	    __writerBuffer.setLength(0);
	    __throwable.fillInStackTrace();
	    __throwable.printStackTrace(__out);
	    __out.flush();
	    _stack = __writerBuffer.toString();
	}

	// Extract stack components
	_lineStart=_stack.indexOf("\n",_lineStart)+1;
	_lineStart=_stack.indexOf("\n",_lineStart)+1;
	for (int i=ignoreFrames;i-->0;)
	    _lineStart=_stack.indexOf("\n",_lineStart)+1;
	_lineEnd=_stack.indexOf("\n",_lineStart);
	_where= _stack.substring(_lineStart,_lineEnd);

	complete();
    }
    
    /* ------------------------------------------------------------ */
    /** private Constructor. 
     * @param ignoreFrames Number of frames to ignore
     * @param partial Partial construction if true
     */
    Frame(int ignoreFrames, boolean partial)
    {
	// Dump the stack
	synchronized(__writerBuffer)
	{
	    __writerBuffer.setLength(0);
	    __throwable.fillInStackTrace();
	    __throwable.printStackTrace(__out);
	    __out.flush();
	    _stack = __writerBuffer.toString();
	}

	// Extract stack components
	_lineStart=_stack.indexOf("\n",_lineStart)+1;
	_lineStart=_stack.indexOf("\n",_lineStart)+1;
	for (int i=ignoreFrames;i-->0;)
	    _lineStart=_stack.indexOf("\n",_lineStart)+1;
	_lineEnd=_stack.indexOf("\n",_lineStart);
	_where= _stack.substring(_lineStart,_lineEnd);

	if (!partial)
	    complete();
    }
    
    
    /* ------------------------------------------------------------ */
    /** Complete partial constructor
     */
    void complete()
    {
	// trim stack
	_stack = _stack.substring(_lineStart);
	
	// calculate stack depth
	int i=0;
	while ((i=_stack.indexOf(_stack,i+1))>0)
		_depth++;

	// extract details
	if (_where!=null)
	{
	    int lb = _where.indexOf('(');
	    int rb = _where.indexOf(')');
	    if (lb>=0 && rb >=0 && lb<rb)
		_file = _where.substring(lb+1,rb).trim();
	    
	    int at = _where.indexOf("at");
	    if (at >=0 && (at+3)<_where.length())
		_method = _where.substring(at+3);
	}

	// Get Thread name
	_thread = Thread.currentThread().getName();

	// Handle nulls
	if (_method==null)
	    _method= "unknownMethod";
	if (_file==null)
	    _file= "UnknownFile";
    }
    
    /*-------------------------------------------------------------------*/
    public String file()
    {
	return _file;
    }
    
    /*-------------------------------------------------------------------*/
    public String toString()
    {
	return "["+_thread + "]" + _method;
    }
}
