package com.mortbay.Util;

import java.io.*;
import java.sql.*;
import java.net.*;
//import com.sun.java.util.collections.*; XXX-JDK1.1
import java.util.*; //XXX-JDK1.2
import java.io.InputStream;
import com.mortbay.Util.Code;

import org.xml.sax.*;
import javax.xml.parsers.*;


/*--------------------------------------------------------------*/
/** XML Parser
 * This class wraps a sax parser with convieniant error and
 * entity handlers and a mini dom-like document tree.
 *
 * @see
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class XmlParser 
{
    private Map _localDtdMap = new HashMap();
    private SAXParserFactory _spf;
    private SAXParser _sp;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception ParserConfigurationException 
     */
    public XmlParser()
    {
	try
	{
	    _spf = SAXParserFactory.newInstance();
	    _spf.setValidating(true);
	    _sp = _spf.newSAXParser ();
	}
	catch(ParserConfigurationException e)
	{
	    Code.warning(e);
	    throw new Error(e.toString());
	}
	catch(SAXException e)
	{
	    Code.warning(e);
	    throw new Error(e.toString());
	}
    }
    

    /* ------------------------------------------------------------ */
    /** 
     * @param url 
     * @return 
     * @exception IOException 
     * @exception SAXException 
     */
    public synchronized Node parse(String url)
	throws IOException,SAXException
    {
	Parser parser=_sp.getParser();
	Handler handler= new Handler();
	parser.setDocumentHandler(handler);
  	parser.setErrorHandler(handler);
  	parser.setEntityResolver(handler);
	parser.parse(url);
	if (handler._error!=null)
	    throw handler._error;
	Node doc=(Node)handler._top.get(0);
	handler.clear();
	return doc;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param file 
     * @return 
     * @exception IOException 
     * @exception SAXException 
     */
    public synchronized Node parse(File file)
	throws IOException,SAXException
    {
	return parse(file.toURL().toString());
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

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Handler extends HandlerBase
    {
	Node _top = new Node(null,null,null);
	SAXParseException _error;
	private Node _context = _top;

	/* ------------------------------------------------------------ */
	void clear()
	{
	    _top=null;
	    _error=null;
	    _context=null;
	}
	
	/* ------------------------------------------------------------ */
	public void startElement (String tag, AttributeList attrs)
	    throws SAXException
	{
	    Node node= new Node(_context,tag,attrs);
	    _context.add(node);
	    _context=node;
	}

	/* ------------------------------------------------------------ */
	public void endElement (String tag)
	    throws SAXException
	{
	    _context=_context._parent;
	}

	/* ------------------------------------------------------------ */
	public void characters (char buf [], int offset, int len)
	    throws SAXException
	{
	    _context.add(new String(buf,offset,len));
	}

	/* ------------------------------------------------------------ */
	public void warning(SAXParseException ex)
	{
	    Code.debug(ex);
	    Code.warning("WARNING@"+getLocationString(ex)+
			 " : "+ex.toString());
	}
		
	/* ------------------------------------------------------------ */
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
	
	/* ------------------------------------------------------------ */
	public void fatalError(SAXParseException ex)
	    throws SAXException
	{
	    _error=ex;
	    Code.debug(ex);
	    Code.warning("FATAL@"+getLocationString(ex)+
			 " : "+ex.toString());
	    throw ex;
	}	    

	/* ------------------------------------------------------------ */
	private String getLocationString(SAXParseException ex)
	{
	    return ex.getSystemId()+
		" line:"+ex.getLineNumber()+
		" col:"+ex.getColumnNumber();
	}
	
	/* ------------------------------------------------------------ */
	public InputSource resolveEntity
	    (String pid, String sid)
	{
	    String dtd = sid;
	    if (dtd.lastIndexOf("/")>=0)
		dtd=dtd.substring(dtd.lastIndexOf("/")+1);
	    File dtdf = (File)_localDtdMap.get(dtd);
	    if (dtdf!=null)
	    {
		try{return new InputSource(new FileReader(dtdf));}
		catch(FileNotFoundException e){Code.ignore(e);}
	    }
	    return null;
	}
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** XML Node.
     * Represents an XML element with optional attributes and
     * ordered content.
     */
    public static class Node extends AbstractList
    {
	Node _parent;
	private ArrayList _list;
	private String _tag;
	private Map _attrs;
	private boolean _lastString=false;
	
	/* ------------------------------------------------------------ */
	Node(Node parent,String tag, AttributeList attrs)
	{
	    _parent=parent;
	    _tag=tag;

	    if (attrs!=null)
	    {
		_attrs=new HashMap(attrs.getLength()+3);
		for (int i = 0; i <attrs.getLength(); i++)
		    _attrs.put(attrs.getName(i),attrs.getValue(i));
	    }
	}
	
	/* ------------------------------------------------------------ */
	public Node getParent()
	{
	    return _parent;
	}  

	/* ------------------------------------------------------------ */
	public String getTag()
	{
	    return _tag;
	} 

	/* ------------------------------------------------------------ */
	/** Get a map of element attributes.
	 * @return Map of attributes or null.
	 */
 	public Map getAttributes()
	{
	    return _attrs;
	}
	
	/* ------------------------------------------------------------ */
	/** Get the number of children nodes.
	 */
 	public int size()
	{
	    if (_list!=null)
		return _list.size();
	    return 0;
	}
	
	/* ------------------------------------------------------------ */
	/** Get the ith child node or content.
	 * @return Node or String.
	 */
 	public Object get(int i)
	{
	    if (_list!=null)
		return _list.get(i);
	    return null;
	}
	
	/* ------------------------------------------------------------ */
	/** Get the first child node with the tag
	 * @param tag 
	 * @return Node or null.
	 */
 	public Node get(String tag)
	{
	    if (_list!=null)
	    {
		for (int i=0;i<_list.size();i++)
		{
		    Object o=_list.get(i);
		    if (o instanceof Node)
		    {
			Node n=(Node)o;
			if (tag.equals(n._tag))
			    return n;
		    }
		}
	    }
	    
	    return null;
	}
	
	/* ------------------------------------------------------------ */
	public void add(int i, Object o)
	{
	    if (_list==null)
		_list=new ArrayList();
	    if (o instanceof String)
	    {
		if (_lastString)
		{
		    int last=_list.size()-1;
		    _list.set(last,(String)_list.get(last)+o);
		}
		else
		    _list.add(i,o);
		_lastString=true;
	    }
	    else
	    {
		_lastString=false;
		_list.add(i,o);
	    }
	}	

	/* ------------------------------------------------------------ */
	public void clear()
	{
	    if (_list!=null)
		_list.clear();
	    _list=null;
	}
	
	/* ------------------------------------------------------------ */
	public synchronized String toString()
	{
	    return toString(true);
	}
	
	/* ------------------------------------------------------------ */
	/** Convert to a string.
	 * @param tag If false, only content is shown.
	 */
 	public synchronized String toString(boolean tag)
	{
	    StringBuffer buf = new StringBuffer();
	    synchronized(buf)
	    {
		toString(buf,tag);
		return buf.toString();
	    }
	}
	
	/* ------------------------------------------------------------ */
	private synchronized void toString(StringBuffer buf,boolean tag)
	{
	    if(tag)
	    {
		buf.append("<");
		buf.append(_tag);
	    }
	    
	    if (_attrs!=null)
	    {
		Iterator i=_attrs.entrySet().iterator();
		while(i.hasNext())
		{
		    Map.Entry entry = (Map.Entry)i.next();
		    buf.append(' ');
		    buf.append(entry.getKey());
		    buf.append("=\"");
		    buf.append(entry.getValue());
		    buf.append("\"");
		}
	    }

	    if (_list!=null)
	    {
		if(tag)
		    buf.append(">");
		
		for (int i=0;i<_list.size();i++)
		{
		    Object o=_list.get(i);
		    if (o==null)
			continue;
		    if (o instanceof Node)
			((Node)o).toString(buf,tag);
		    else
			buf.append(o.toString());
		}

		if(tag)
		{
		    buf.append("</");
		    buf.append(_tag);
		    buf.append(">");
		}
	    }
	    else if (tag)
		buf.append("/>");
	}
	
	/* ------------------------------------------------------------ */
	/** Iterator over named child nodes.
	 * @param tag The tag of the nodes.
	 * @return Iterator over all child nodes with the specified tag.
	 */
 	public Iterator iterator(final String tag)
	{
	    return new Iterator()
		{
		    int c=0;
		    Node _node;

		    /* -------------------------------------------------- */
		    public boolean hasNext()
		    {
			if (_node!=null)
			    return true;
			while (_list!=null && c<_list.size())
			{
			    Object o=_list.get(c);
			    if (o instanceof Node)
			    {
				Node n=(Node)o;
				if (tag.equals(n._tag))
				{
				    _node=n;
				    return true;
				}
			    }
			    c++;
			}
			return false;
		    }
	
		    /* -------------------------------------------------- */
		    public Object next()
		    {
			try
			{
			    if (hasNext())
				return _node;
			    throw new NoSuchElementException();
			}
			finally
			{
			    _node=null;
			    c++;
			}
		    }

		    /* -------------------------------------------------- */
		    public void remove()
		    {
			throw new UnsupportedOperationException("Not supported");
		    }
		    
		};
	}
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
	try
	{
	    XmlParser parser = new XmlParser();
	    parser.addLocalDTD("jetty.dtd",
			       new File("../../../../etc/jetty.dtd"));
	    
	    String url = "file:"+System.getProperty("user.dir")+
		"/../../../../etc/jetty.xml";
	    Node testDoc = parser.parse(url);
	    
	    System.err.println(testDoc);
	    
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
    }
}
