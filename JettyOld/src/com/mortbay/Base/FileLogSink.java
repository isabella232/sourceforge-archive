// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.Base;

import java.util.*;
import java.io.*;
import java.text.*;


/* ------------------------------------------------------------ */
/** File Log Sink.
 * This implementation of Log Sink writes logs to a file. Currently
 * it is a trivial implementation and represents a place holder for
 * future implementations of file handling which is dated, rolling,
 * etc.
 *
 * @see
 * @version 1.0 Sun Apr 23 2000
 * @author Greg Wilkins (gregw)
 */
public class FileLogSink extends LogSink
{
    /*-------------------------------------------------------------------*/
    private String _fileName=null;
    private PrintWriter _myOut = null;
    
    /* ------------------------------------------------------------ */
    public FileLogSink()
	throws IOException
    {
    	this(System.getProperty("LOG_FILE","log.txt"));
    }
    
    /* ------------------------------------------------------------ */
    public FileLogSink(String filename)
	throws IOException
    {
	super(new PrintWriter(new FileWriter(filename)));
	_myOut = _out;
	_fileName=filename;
    }

    /* ------------------------------------------------------------ */
    public void stop()
    {
	_myOut.close();
    }
}
