
// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.util.*;
import java.io.*;

/* -------------------------------------------------------------------- */
/** Include File, InputStream or Reader Element
 * <p>This Element includes another file.
 * This class expects that the HTTP directory separator '/' will be used.
 * This will be converted to the local directory separator.
 * @see class Element
 * @version $Id$
 * @author Greg Wilkins
*/
public class Include extends Element
{
    Reader reader=null;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * Include file
     * @param directory Directory name
     * @param fileName file name
     * @exception IOException File not found
     */
    public Include(String directory,
		   String fileName)
	 throws IOException
    {
	if (directory==null)
	    directory=".";
 
	if (File.separatorChar != '/')
	{
	    directory = directory.replace('/',File.separatorChar);
	    fileName  = fileName .replace('/',File.separatorChar);
	}

	Code.debug("IncludeTag(",directory,",",fileName,")");
	includeFile(new File(directory,fileName));
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * Include file.
     * @param fileName Filename
     * @exception IOException File not found
     */
    public Include(String fileName)
	throws IOException
    {
	if (File.separatorChar != '/')
	    fileName  = fileName .replace('/',File.separatorChar);
	Code.debug("IncludeTag(",fileName,")");
	includeFile(new File(fileName));
    }

    /* ------------------------------------------------------------ */
    /** Constructor.
     * Include file.
     * @param file file
     * @exception IOException File not found
     */
    public Include(File file)
	throws IOException
    {
	Code.debug("IncludeTag(",file,")");
	includeFile(file);
    }

    /* ------------------------------------------------------------ */
    /** Constructor.
     * Include InputStream.
     * @param in stream
     * @exception IOException
     */
    public Include(InputStream in)
	throws IOException
    {
	reader=new InputStreamReader(in);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * Include Reader.
     * @param in reader
     * @exception IOException
     */
    public Include(Reader in)
	throws IOException
    {
	reader=in;
    }
    
    /* ------------------------------------------------------------ */
    private void includeFile(File file)
	throws IOException
    {
	if (!file.exists())
	    throw new FileNotFoundException(file.toString());
	
	if (file.isDirectory())
	{
	    List list = new List(List.Unordered);	
	    String[] ls = file.list();
	    for (int i=0 ; i< ls.length ; i++)
		list.add(ls[i]);
	    StringWriter sw = new StringWriter();
	    list.write(sw);
	    reader = new StringReader(sw.toString());
	}
	else
	{
	    reader = new BufferedReader(new FileReader(file));
	}
    }
    

    /* ---------------------------------------------------------------- */
    public void write(Writer out)
	 throws IOException
    {
	try{
	    IO.copy(reader,out);
	}
	finally
	{
	    reader.close();
	    reader=null;
	}
    }
}









