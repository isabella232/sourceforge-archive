// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.Base;

import java.util.*;
import java.io.*;


/*-----------------------------------------------------------------------*/
/** Access the current execution frame
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
    private static final String __lineSeparator = System.getProperty("line.separator");
    
    /*-------------------------------------------------------------------*/
    /** The full stack of where the Frame was created. */
    public String _stack;
    /** The Method (including the "(file.java:99)") the Frame was created in */
    public String _method= "unknownMethod";
    /** The stack depth where the Frame was created (main is 1) */
    public int _depth=0;
    /** Name of the Thread the Frame was created in */
    public String _thread= "unknownThread";
    /** The file and linenumber of where the Frame was created. */
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
	internalInit(1, false);
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
	internalInit(ignoreFrames+1, false);
    }
    
    /* ------------------------------------------------------------ */
    /** package private Constructor. 
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
	internalInit(ignoreFrames+1, partial);
    }
    
    /* ------------------------------------------------------------ */
    /** Internal only Constructor. */
    protected Frame(String stack, int ignoreFrames, boolean partial)
    {
	_stack = stack;
	internalInit(ignoreFrames, partial);
    }
    
    /* ------------------------------------------------------------ */
    protected void internalInit(int ignoreFrames, boolean partial){
	// Extract stack components
	_lineStart = _stack.indexOf(__lineSeparator,_lineStart)+1;
	for (int i = 0; _lineStart > 0 && i < ignoreFrames; i++){
	    _lineStart = _stack.indexOf(__lineSeparator,_lineStart)+1;
	}
	_lineEnd = _stack.indexOf(__lineSeparator,_lineStart);
	if (_lineEnd < _lineStart || _lineStart < 0){
	    _where = null;
	    _stack = null;
	} else {
	    _where = _stack.substring(_lineStart,_lineEnd);
	    if (!partial) complete();
	}
    }
    
    /* ------------------------------------------------------------ */
    /** Complete partial constructor
     */
    void complete()
    {
	// trim stack
	// XXX - Need to handle Java2 correctly
	if (_stack != null) {
	    _stack = _stack.substring(_lineStart);
	} else {
	    // Handle nulls
	    if (_method==null)
		_method= "unknownMethod";
	    if (_file==null)
		_file= "UnknownFile";
	    return;
	}
	
	// calculate stack depth
	int i=0;
	while ((i=_stack.indexOf(__lineSeparator,i+1))>0)
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
    
    /* ------------------------------------------------------------ */
    /** Get a Frame representing the function one level up in this frame
     * @return parent frame or null if none
     */
    public Frame getParent(){
	Frame f = new Frame(_stack, 0, false);
	if (f._where == null) return null;
	f._thread = _thread;
	return f;
    }
    
    /* ------------------------------------------------------------ */
    private static void testChecker(Test t, Frame f, String desc,
				    String method, int depth,
				    String thread, String file)
    {
	t.checkContains(f._method, method, desc+": method");
	t.checkEquals(f._depth, depth, desc+": depth");
	t.checkEquals(f._thread, thread, desc+": thread");
	t.checkContains(f._file, file, desc+": file");
    }
    
    /* ------------------------------------------------------------ */
    static void test(){
	realTest();
    }
    
    /* ------------------------------------------------------------ */
    static void realTest(){
	Test t = new Test("Frame");
	Frame f = new Frame();
	testChecker(t, f, "default constructor",
		    "com.mortbay.Base.Frame.realTest",
		    3, "main", "Frame.java");
	f = f.getParent();
	testChecker(t, f, "getParent",
		    "com.mortbay.Base.Frame.test",
		    2, "main", "Frame.java");
	f = f.getParent();
	f = f.getParent();
	t.checkEquals(f, null, "getParent() off top of stack");
	f = new Frame(1);
	testChecker(t, f, "new Frame(1)",
		    "com.mortbay.Base.Frame.test",
		    2, "main", "Frame.java");
	f = new Frame(1, true);
	testChecker(t, f, "partial",
		    "unknownMethod", 0, "unknownThread", "UnknownFile");
	f.complete();
	testChecker(t, f, "new Frame(1)",
		    "com.mortbay.Base.Frame.test",
		    2, "main", "Frame.java");
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String argv[]) {
	test();
	Test.report();
    }
}
