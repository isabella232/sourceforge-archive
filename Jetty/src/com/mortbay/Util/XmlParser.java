package com.mortbay.Util;

import java.io.*;
import java.sql.*;
import java.net.*;
import java.util.*;
import java.io.InputStream;
import com.mortbay.Util.Code;

import org.xml.sax.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;


/*--------------------------------------------------------------*/
/** XML Parser
 * This class wraps the jaxp parser with convieniant error and
 * entity handlers
 *
 * @see
 * @version 1.0 Fri Jul 28 2000
 * @author Greg Wilkins (gregw)
 */
public class XmlParser
{
    private DocumentBuilderFactory _dbf;
    private DocumentBuilder _db;
    private Map _localDtdMap = new HashMap();
    private SAXParseException _error=null;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception ParserConfigurationException 
     */
    public XmlParser()
    {
	_dbf = DocumentBuilderFactory.newInstance ();
	_dbf.setValidating(true);
	try{_db  =_dbf.newDocumentBuilder();}
	catch(ParserConfigurationException e){Code.fail(e);}
	_db.setErrorHandler(new ErrorHandler()
	    {
		public void warning(SAXParseException ex)
		{
		    Code.debug(ex);
		    Code.warning("WARNING@"+getLocationString(ex)+
				 " : "+ex.toString());
		}
		
		public void error(SAXParseException ex)
		    throws SAXException
		{
		    // Save error and continue to report other errors
		    if(_error==null)
			_error=ex;
		    Code.debug(ex);
		    Code.warning("ERROR@"+getLocationString(ex)+
				 " : "+ex.toString());
		}
		
		public void fatalError(SAXParseException ex)
		    throws SAXException
		{
		    _error=ex;
		    Code.debug(ex);
		    Code.warning("FATAL@"+getLocationString(ex)+
				 " : "+ex.toString());
		    throw ex;
		}	    
		private String getLocationString(SAXParseException ex)
		{
		    return ex.getSystemId()+
			" line:"+ex.getLineNumber()+
			" col:"+ex.getColumnNumber();
		}
	    });
	
	_db.setEntityResolver(new EntityResolver()
	    {
		public InputSource resolveEntity
		    (String pid, String sid)
		    throws IOException
		{
		    String dtd = sid;
		    if (dtd.lastIndexOf("/")>=0)
			dtd=dtd.substring(dtd.lastIndexOf("/")+1);
		    File dtdf = (File)_localDtdMap.get(dtd);
		    if (dtdf!=null)
			return new InputSource(new FileReader(dtdf));
		    return null;
		}
	    });
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param url 
     * @return 
     * @exception IOException 
     * @exception SAXException 
     */
    public synchronized Document parse(String url)
	throws IOException,SAXException
    {
	_error=null;
	Document doc=_db.parse(url);
	if (_error!=null)
	    throw _error;
	return doc;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param file 
     * @return 
     * @exception IOException 
     * @exception SAXException 
     */
    public synchronized Document parse(File file)
	throws IOException,SAXException
    {
	_error=null;
	Document doc=_db.parse(file);
	if (_error!=null)
	    throw _error;
	return doc;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @param local 
     */
    public synchronized void addLocalDTD(String name,File local)
    {
	if (local.exists())
	    _localDtdMap.put(name,local);
    }
    
    /*--------------------------------------------------------------*/
    public static void main(String[] arg)
    {
	try
	{
	    XmlParser parser = new XmlParser();
	    parser.addLocalDTD("jetty.dtd",
			       new File("../../../../etc/jetty.dtd"));
	    
	    String url = "file:"+System.getProperty("user.dir")+
		"/../../../../etc/jetty.xml";
	    Document testDoc = parser.parse(url);
	    
	    System.err.println(testDoc.getDocumentElement());
	    
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
    }  
}
