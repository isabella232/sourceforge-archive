/*
 * $Header$
 * $Revision$
 * $Date$
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 

package org.apache.jasper.compiler;

import java.net.URL;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;

import org.xml.sax.*;
import com.mortbay.XML.XmlParser;
import com.mortbay.Util.Resource;

import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.JasperException;
import org.apache.jasper.Constants;
//import org.apache.jasper.runtime.JspLoader;


/**
 * Implementation of the TagLibraryInfo class from the JSP spec. 
 *
 * @author Anil K. Vijendran
 * @author Mandar Raje
 */
public class TagLibraryInfoImpl extends TagLibraryInfo {
    static private final String TLD = "META-INF/taglib.tld";
    static private final String WEBAPP_INF = "/WEB-INF/web.xml";

    XmlParser.Node tld;

    Hashtable jarEntries;

    JspCompilationContext ctxt;

    

    private final void print(String name, String value, PrintWriter w) {
        if (value != null) {
            w.print(name+" = {\n\t");
            w.print(value);
            w.print("\n}\n");
        }
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        print("tlibversion", tlibversion, out);
        print("jspversion", jspversion, out);
        print("shortname", shortname, out);
        print("urn", urn, out);
        print("info", info, out);

        for(int i = 0; i < tags.length; i++)
            out.println(tags[i].toString());
        
        return sw.toString();
    }
    
    // XXX FIXME
    // resolveRelativeUri and/or getResourceAsStream don't seem to properly
    // handle relative paths when dealing when home and getDocBase are set
    // the following is a workaround until these problems are resolved.
    private InputStream getResourceAsStream(String uri) 
        throws FileNotFoundException 
    {
        if (uri.indexOf(':') > 0) {
            // may be fully qualified (Windows) or may be a URL.  Let
            // getResourceAsStream deal with it.
            return ctxt.getResourceAsStream(uri);
        } else {
            // assume it translates to a real file, and use getRealPath
            String real = ctxt.getRealPath(uri);
            return (real == null) ? null : new FileInputStream(real);
        }
    }

    public TagLibraryInfoImpl(JspCompilationContext ctxt, String prefix, String uriIn) 
        throws IOException, JasperException
    {
        super(prefix, uriIn);

	this.ctxt = ctxt;
        ZipInputStream zin;
        InputStream in = null;
        URL url = null;
        boolean relativeURL = false;
	this.uri = uriIn;

        // Parse web.xml.
        InputStream is = getResourceAsStream(WEBAPP_INF);

        if (is != null) {
            XmlParser.Node webtld =
                JspUtil.parseXMLDoc(is,
                                    Constants.WEBAPP_DTD_RESOURCE,
                                    Constants.WEBAPP_DTD_PUBLIC_ID);
            Iterator nList =  webtld.iterator("taglib");

            while(nList.hasNext())
            {
                String tagLoc = null;
                boolean match = false;
                XmlParser.Node e = (XmlParser.Node)nList.next();
                
                // Assume only one entry for location and uri.
                XmlParser.Node uriElem =  e.get("taglib-uri");
                String tmpUri = uriElem.toString(false,true);
                
                if (tmpUri != null)
                {
                    tmpUri = tmpUri.trim();
                    if (tmpUri.equals(uriIn))
                    {
                        match = true;
                        XmlParser.Node locElem = e.get("taglib-location");
                        tagLoc =  locElem.toString(false,true);
                        if (tagLoc != null)
                            tagLoc = tagLoc.trim();
                    }
                }
                if (match == true && tagLoc != null)
                {
                    this.uri = tagLoc;
                    
                    // If this is a relative path, then it has to be
                    // relative to where web.xml is.
                    
                    // I'm taking the simple way out. Since web.xml 
                    // has to be directly under WEB-INF, I'm making 
                    // an absolute URI out of it by prepending WEB-INF
                    
                    if (!uri.startsWith("/") && isRelativeURI(uri))
                        uri = "/WEB-INF/"+uri;
                }
            }
        }

        // Try to resolve URI relative to the current JSP page
        if (!uri.startsWith("/") && isRelativeURI(uri))
            uri = ctxt.resolveRelativeUri(uri);


        if (!uri.endsWith("jar")) {
	    in = getResourceAsStream(uri);
	    
	    if (in == null)
		throw new JasperException(Constants.getString("jsp.error.tld_not_found",
							      new Object[] {TLD}));
	    // Now parse the tld.
	    parseTLD(in);
	}
	    
	// FIXME Take this stuff out when taglib changes are thoroughly tested.
        // 2000.11.15 commented out the 'copy to work dir' section,
        // which I believe is what this FIXME comment referred to. (pierred)
	if (uri.endsWith("jar")) {
	    
	    if (!isRelativeURI(uri)) {
		url = new URL(uri);
		in = url.openStream();
	    } else {
		relativeURL = true;
		in = getResourceAsStream(uri);
	    }
	    
	    zin = new ZipInputStream(in);
	    this.jarEntries = new Hashtable();
	    this.ctxt = ctxt;
	    
            /* NOT COMPILED
	    // First copy this file into our work directory! 
	    {
		File jspFile = new File(ctxt.getJspFile());
                String parent = jspFile.getParent();
                String jarFileName = ctxt.getOutputDir();
                if (parent != null) {
                   jarFileName = jarFileName + File.separatorChar +
                       parent;
                }
                File jspDir = new File(jarFileName);
		jspDir.mkdirs();
	    
		if (relativeURL)
		    jarFileName = jarFileName+File.separatorChar+new File(uri).getName();
		else                    
		    jarFileName = jarFileName+File.separatorChar+
			new File(url.getFile()).getName();
	    
		Constants.debug("jsp.message.copyinguri", 
                                new Object[] { uri, jarFileName });
	    
		if (relativeURL)
		    copy(getResourceAsStream(uri),
			 jarFileName);
		else
		    copy(url.openStream(), jarFileName);
	    
	        ctxt.addJar(jarFileName);
	    }
            */ // END NOT COMPILED
	    boolean tldFound = false;
	    ZipEntry entry;
	    while ((entry = zin.getNextEntry()) != null) {
		if (entry.getName().equals(TLD)) {
		    /*******
		     * This hack is necessary because XML reads until the end 
		     * of an inputstream -- does not use available()
		     * -- and closes the inputstream when it can't
		     * read no more.
		     */
		    
		    // BEGIN HACK
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    int b;
		    while (zin.available() != 0) {
			b = zin.read();
			if (b == -1)
			    break;
			baos.write(b);
		    }

		    baos.close();
		    ByteArrayInputStream bais 
			= new ByteArrayInputStream(baos.toByteArray());
		    // END HACK
		    tldFound = true;
		    parseTLD(bais);
		} else {
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    int b;
		    while (zin.available() != 0) {
			b = zin.read();
			if (b == -1)
			    break;
			baos.write(b);
		    }
		    baos.close();
		    jarEntries.put(entry.getName(), baos.toByteArray());
		}
		zin.closeEntry();
	    }
	    
	    if (!tldFound)
		throw new JasperException(Constants.getString("jsp.error.tld_not_found",
							      new Object[] {
		    TLD
			}
							      ));
	} // Take this out (END of if(endsWith("jar")))
    }
    
    /** Returns true if the given URI is relative in this web application, false if it is an internet URI.
     */
    private boolean isRelativeURI(String uri) {
        return (uri.indexOf(':') == -1);
    }
    
        
    private void parseTLD(InputStream in) 
        throws JasperException
    {
	tld = JspUtil.parseXMLDoc(in,
				  Constants.TAGLIB_DTD_RESOURCE,
				  Constants.TAGLIB_DTD_PUBLIC_ID);

        Vector tagVector = new Vector();
        Iterator list = tld.iterator();
        

        while (list.hasNext())
        {
	    Object tmp = list.next();
	    if (! (tmp instanceof XmlParser.Node)) continue;
            XmlParser.Node e = (XmlParser.Node) tmp;
            String tname = e.getTag();
            if (tname.equals("tlibversion")) {
                String t = e.toString(false,true);
                if (t != null)
                    this.tlibversion = t;
            } else if (tname.equals("jspversion")) {
                String t = e.toString(false,true);
                if (t != null)
                    this.jspversion = t;
            } else if (tname.equals("shortname")) {
                String t = e.toString(false,true);
                if (t != null)
                    this.shortname = t;
            } else if (tname.equals("urn")) {
                String t = e.toString(false,true);
                if (t != null)
                    this.urn = t;
            } else if (tname.equals("info")) {
                String t = e.toString(false,true);
                if (t != null)
                    this.info = t;
            } else if (tname.equals("tag"))
                tagVector.addElement(createTagInfo(e));
            else
                Constants.warning("jsp.warning.unknown.element.in.TLD", 
                                  new Object[] {
                                      e.getTag()
                                  });
        }

        this.tags = new TagInfo[tagVector.size()];
        tagVector.copyInto (this.tags);
    }

    private TagInfo createTagInfo(XmlParser.Node elem) throws JasperException {
        String name = null, tagclass = null, teiclass = null;
        String bodycontent = "JSP"; // Default body content is JSP
	String info = null;
        
        Vector attributeVector = new Vector();
        Iterator list = elem.iterator();
        while (list.hasNext())
        {
            Object tmp  =  list.next();
	    if (! (tmp instanceof XmlParser.Node)) continue;
	    XmlParser.Node e = (XmlParser.Node) tmp;
            String tname = e.getTag();
            if (tname.equals("name")) {
                String t = e.toString(false,true);
                if (t != null)
                    name = t;
            } else if (tname.equals("tagclass")) {
                String t = e.toString(false,true);
                if (t != null)
                    tagclass = t;
            } else if (tname.equals("teiclass")) {
                String t = e.toString(false,true);
                if (t != null)
                    teiclass = t;
            } else if (tname.equals("bodycontent")) {
                String t = e.toString(false,true);
                if (t != null)
                    bodycontent = t;
            } else if (tname.equals("info")) {
                String t = e.toString(false,true);
                if (t != null)
                    info = t;
            } else if (tname.equals("attribute"))
                attributeVector.addElement(createAttribute(e));
            else 
                Constants.warning("jsp.warning.unknown.element.in.tag", 
                                  new Object[] {
                                      e.getTag()
                                  });
        }
	TagAttributeInfo[] tagAttributeInfo 
            = new TagAttributeInfo[attributeVector.size()];
	attributeVector.copyInto (tagAttributeInfo);

        TagExtraInfo tei = null;

        if (teiclass != null && !teiclass.equals(""))
            try {
                Class teiClass = ctxt.getClassLoader().loadClass(teiclass);
                tei = (TagExtraInfo) teiClass.newInstance();
	    } catch (ClassNotFoundException cex) {
                Constants.warning("jsp.warning.teiclass.is.null",
                                  new Object[] {
                                      teiclass, cex.getMessage()
                                  });
            } catch (IllegalAccessException iae) {
                Constants.warning("jsp.warning.teiclass.is.null",
                                  new Object[] {
                                      teiclass, iae.getMessage()
                                  });
            } catch (InstantiationException ie) {
                Constants.warning("jsp.warning.teiclass.is.null",
                                  new Object[] {
                                      teiclass, ie.getMessage()
                                  });
            }

        TagInfo taginfo = new TagInfo(name, tagclass, bodycontent,
                                      info, this, 
                                      tei,
                                      tagAttributeInfo);
        return taginfo;
    }

    TagAttributeInfo createAttribute(XmlParser.Node elem)
    {
        String name = null;
        boolean required = false, rtexprvalue = false, reqTime = false;
        String type = null;
        
        Iterator list = elem.iterator();
        while (list.hasNext())
        {
            Object tmp  = list.next();
	    if (! (tmp instanceof XmlParser.Node)) continue;
	    XmlParser.Node e = (XmlParser.Node) tmp;
            String tname = e.getTag();
            if (tname.equals("name"))  {
                String t = e.toString(false,true);
                if (t != null)
                    name = t;
            } else if (tname.equals("required"))  {
                String t = e.toString(false,true);
                if (t != null)
                    required = Boolean.valueOf(t).booleanValue();
            } else if (tname.equals("rtexprvalue")) {
                String t = e.toString(false,true);
                if (t != null)
                    rtexprvalue = Boolean.valueOf(t).booleanValue();
            } else if (tname.equals("type")) {
                String t = e.toString(false,true);
                if (t != null)
                    type = t;
            } else 
                Constants.warning("jsp.warning.unknown.element.in.attribute", 
                                  new Object[] {
                                      e.getTag()
                                  });
        }
        
	//     return new TagAttributeInfo(name, required, rtexprvalue, type);
        return new TagAttributeInfo(name, required, type, rtexprvalue);
    }

    static void copy(InputStream in, String fileName) 
        throws IOException, FileNotFoundException 
    {
        byte[] buf = new byte[1024];

        FileOutputStream out = new FileOutputStream(fileName);
        int nRead;
        while ((nRead = in.read(buf, 0, buf.length)) != -1)
            out.write(buf, 0, nRead);
    }

}
