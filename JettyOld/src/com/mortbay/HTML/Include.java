
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
/** Include File Element
 * <p>This Element includes another file.
 * This class expects that the HTTP directory separator '/' will be used.
 * This will be converted to the local directory separator.
 * @see class Element
 * @version $Id$
 * @author Greg Wilkins
*/
public class Include extends Element
{
    Reader in=null;
    
    /* ---------------------------------------------------------------- */
    public Include(String fileName)
	 throws IOException
    {
	this(".",fileName);
    }
    
    /* ---------------------------------------------------------------- */
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

	Code.debug("IncludeTag("+directory+","+fileName+")");

	StringBuffer buf = new StringBuffer();

	try{
	    File file = new File(directory,fileName);
	    if (file.isDirectory())
	    {
		List list = new List(List.Unordered);	
		String[] ls = file.list();
		for (int i=0 ; i< ls.length ; i++)
		    list.add(ls[i]);
		StringWriter sw = new StringWriter();
		list.write(sw);
		in = new StringReader(sw.toString());
	    }
	    else
		in = new BufferedReader(new FileReader(file));
	}
	catch (IOException e){
	    Code.warning("Bad Include("+directory+","+fileName+")",e);
	    throw e;
	}
    }
    
    /* ---------------------------------------------------------------- */
    public void write(Writer out)
	 throws IOException
    {
	IO.copy(in,out);
    }
}
