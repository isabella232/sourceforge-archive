/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.jsp;

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.servlet.*;

class JspParser
    implements JspMsg {
    private boolean debug = false;
    private ServletConfig config;
    // force use of pageBase for some engines
    private boolean usePageBase = false;

    // maximal string length that get written
    // as a single constant string.
    // (real string may be a few bytes longer)
    private static int MAX_STRING_LENGTH = 32000;
    // Used when we open a new file for inclusion.
    private static final int BUFFER_SIZE = 8192;

    /**
     * Create a new parser instance; there should be a one to one mapping
     * between JspServlet instances and parser instances.  In general,
     * the parser is a stateless black box that is used to generate an AST
     * in the form of JspNodes.  We need the current ServletConfig in
     * order to accurately process static includes.
     */
    public JspParser(ServletConfig config) {
	this.config = config;
    }
    /**
     * Enable/disable debugging
     */
    public void setDebug(boolean b) { debug = b; }

    /**
     * Enable/disable forced use of pageBase
     */
    public void setUsePageBase(boolean b) { usePageBase = b; }

    /**
     * Parses the JSP page indicated and returns a JspNode with type
     * JSP_BODY.
     *
     * @exception ParseException if a parsing error occurred
     * @exception IOException if there was a problem reading from the reader
     */
    public JspNode parse(StackedReader r) throws ParseException, IOException {
	JspNode body = new JspNode(JspNode.JSP_BODY);
	parseJsp(r, body);
	return body;
    }

    private void addTextNode(JspNode body, Pos pos, StringBuffer textChunk) {
	String s = textChunk.toString();
	// optimize: if string is empty, don't add empty node (alph)
	if(s.length() > 0) {
	    JspNode textNode = new JspNode(JspNode.TEMPLATE_TEXT);
	    textNode.setPos(pos);
	    textNode.setAttribute("text", s);
	    body.addChild(textNode);
	}
    }

    /**
     * Parses a section of a JSP document, up to a /jsp element or the
     * end of the file.
     */
    public void parseJsp(StackedReader r, JspNode body) throws IOException {
	int in;
	char c;
	char state = '>';
	boolean closeTag = false;
	boolean inHtmlComment = false;
	// remember beanIds seen to check for duplicate Ids (2.12.2)
	Hashtable beanIds = null;
	// remember where textChunk started
	Pos pos = null;

	StringBuffer textChunk = null;

	while ((in = r.read ()) != -1) {
	    c = (char) in;
	    switch (state) {
	    case '>': /* first character after a '>' */
		if(textChunk == null)
		    textChunk = new StringBuffer();
		else
		    textChunk.setLength(0);

		// remember where textChunk started
		pos = r.getPos();
		state = '|';

	    case '|': /* characters between '>' and '<' */
	    case 'X': /* in HTML comment, after '-' */
	    case 'Y': /* in HTML comment, after '--' */
		if (c == '<') state = '<';
		else {
		    if (inHtmlComment) {
			if (c == '-') {
			    if (state == 'X') state = 'Y';
			    else if (state != 'Y') state = 'X';
			} else if (c == '>' && state == 'Y') {
			    inHtmlComment = false;
			    state = '|';
			} else state = '|';
		    }
		    // avoid static strings beyond this length,
		    // java compiler will have problems with them.
		    if(textChunk.length() >= MAX_STRING_LENGTH) {
			addTextNode(body, pos, textChunk);
			pos = r.getPos();
			textChunk.setLength(0);
		    }
		    textChunk.append(c);
		}
		break;

	    case '<':  /* seen < */
		closeTag = false;
		// <%= expression %> is the only thing legal in
		// HTML comments (well, and <jsp:expression>, FIXME)
		if (c == '%') {
		    state = '%';
		    break;
		} else if (inHtmlComment) {
		    textChunk.append('<');
		    textChunk.append(c);
		    state = '|'; /* <? */
		} else {
		    switch (c) {
		    case 'j': state = 'j'; break;
		    case '!': state = '!'; break;
		    case '/': state = '/'; break;
		    case '\\': state = '\\'; break;
		    default:
			textChunk.append('<');
			textChunk.append(c);
			state = '|'; /* <? */
		    }
		}
		break;

	    case '!':   /* seen <! */
		if (c == '-') {
		    state = '_';
		} else {
		    textChunk.append("<!");
		    textChunk.append(c);
		    state = '|'; /* <!? */
		}
		break;

	    case '_':   /* seen <!- */
		textChunk.append("<!-");
		textChunk.append(c);
		if (c == '-') {
		    inHtmlComment = true;
		}
		state = '|'; /* <!? */
		break;

	    case '\\':  /* seen <\ */
		textChunk.append('<');
		if (c != '%') textChunk.append('\\');
		textChunk.append(c);
		state = '|'; /* <? */
		break;

	    case '%':   /* seen <% */
		// Only expressions are valid in HTML comments
		if (inHtmlComment && c != '=') {
		    textChunk.append("<%");
		    textChunk.append(c);
		    state = '|';
		    break;
		}
		if (c != '-') {
		    addTextNode(body, pos, textChunk);
		}
		switch (c) {
		case '@':
		    parseDirective (r, body, false);
		    state = '>';
		    break;/* <%@ */
		case '=':
		    {
			Pos pos2 = r.getPos();
			JspNode n = parseExpression (r, body, false);
			n.setPos(pos2);
			body.addChild(n);
		    }
		    state = '>';
		    break;/* <%= */
		case '!':
		    parseDeclaration (r, body, false);
		    state = '>';
		    break;/* <%! */
		case '-':
		    state = '-';
		    break;           /* <%- */
		default:   /* <% */
		    {
			Pos pos2 = r.getPos();
			JspNode n = parseScriptlet(r,c, body, false);
			n.setPos(pos2);
			body.addChild(n);
		    }
		    state = '>';
		    break;
		}
		break;
	    case '-':   /* seen <%- */
		switch (c) {
		case '-':
		    readToMatch(r, "--%>"); // skip the JSP comment
		    state = '|';
		    break;/* saw <%--*/
		default:
		    textChunk.append("<%-");
		    textChunk.append(c);
		    state = '|'; /* <%-!-*/
		}
		break;

	    case 'j':   /* seen <j */
		switch (c) {
		case 's':
		    state = 's';
		    break;           /* saw <js */
		default:
		    textChunk.append("<j");
		    textChunk.append(c);
		    state = '|';  /* saw <j!s */
		}
		break;

	    case 's':   /* seen <js */
		switch (c) {
		case 'p':                                     /* saw <jsp */
		    addTextNode(body, pos, textChunk);
		    if (closeTag) {
			parseCloseTag(r,body);
			return;
		    }
		    JspNode n = parseTag (r, body);
		    if (n != null) {
			// check for duplicate id's (2.12.2)
			// FIXME: what do do with jsp:useBean inside
			// jsp:useBean? valid? If yes, beanIds is wrong"
			if(n.getType() == JspNode.JSP_USEBEAN) {
			    if(beanIds == null) {
				beanIds = new Hashtable(5);
			    }
			    String id = n.getAttribute("id");
			    if(id == null) {
				throw new ParseException
				    (r.getPos(),
				     ERR_sp10_2_13_1_missing_attr_id);
			    }
			    if(beanIds.containsKey(id)) {
				throw new ParseException
				    (r.getPos(),
				     ERR_sp10_2_12_2_duplicate_id);
			    }
			    // FIXME: may store JspNode here for error messages?
			    beanIds.put(id,id);
			}
			// 2.13.2.1 only jsp:useBean can define elements and
			// definition must appear before jsp:setProperty element
			if(n.getType() == JspNode.JSP_SETPROPERTY) {
			    String name = n.getAttribute("name");
			    /*
			      FIXME: to make that work, beanIds must be an attribute
			      of body. setProperty may be inside jsp:useBean tag
			      and adding of id to beanIds must be moved.
			      This may be done using setPageObjectAttribute/getPOA.

			    if(beanIds == null
			       || name == null
			       || !beanIds.containsKey(name)) {
				throw new ParseException(r.getPos(),
							 ERR_sp10_2_13_2_undefined_bean_instance);
			    }
			    */
			}

			body.addChild(n);
		    }
		    state = '>';
		    break;
		default:
		    textChunk.append("<js");
		    textChunk.append(c);
		    state = '|'; /* <js!p*/
		}
		break;

	    case '/':   /* seen </ */
		switch (c) {
		case 'j':
		    closeTag = true;
		    state = 'j';
		    break; /* saw </j */
		default:
		    textChunk.append("</");
		    textChunk.append(c);
		    state = '|'; /* </!j */
		}
	    }
	}

	if (state == '|') {
	    addTextNode(body, pos, textChunk);
	}

	// If we reached EOF while expecting a close tag, throw exception.
	int type = body.getType();
	if (type != JspNode.JSP_BODY)
	    throw new ParseException
		(r.getPos(),
		 JspConfig.getLocalizedMsg(ERR_gnujsp_missing_closing_tag)
		 + ": </jsp:" + JspNode.tagNames[type] + "> "
		 + JspConfig.getLocalizedMsg(ERR_gnujsp_check_if_you_forgot)
		 + " <jsp:" + JspNode.tagNames[type] + " ... />.");
	return;
    }

    // Returns the last character read (the one following the token)
    char readToken (StackedReader r, StringBuffer sb, boolean skipWhitespace) throws IOException {
	char ch;

	// Skip whitespace if allowed
	if (skipWhitespace) {
	    do {
		ch = readNoEOF(r);
	    } while (Character.isWhitespace (ch));
	} else ch = readNoEOF(r);


	while (Character.isJavaIdentifierPart (ch)) {
	    sb.append (ch);
	    ch = readNoEOF(r);
	}

	return ch;
    }

    /**
     * Read (name="value")* (%|/)?>
     * if percent, then can be terminated by %>.
     * if percent is false, then can be terminated by > or by />
     * @return true if terminated by />
     */
    boolean readNameAndValues (StackedReader r, Hashtable t,
			       boolean isXMLStyle)
	throws IOException {

	StringBuffer sb   = new StringBuffer ();
	String       name = null;
	char c;

	while (true) {

	    do {
		/* skip whitespace*/
		c = readNoEOF(r);
	    } while (Character.isWhitespace (c));

	    if (!isXMLStyle && c == '%')
		if ((c = readNoEOF(r)) == '>')        /* saw %> */
		    break;
		else
		    throw new ParseException
			(r.getPos(),
			 ERR_sp10_2_7_1_parse_error_expected_gt_after_percent);
	    else if (isXMLStyle && c == '>')
		return false;
	    else if (isXMLStyle && c == '/')
		if ((c = readNoEOF(r)) == '>')
		    return true;
		else
		    throw new ParseException
			(r.getPos(),
			 ERR_sp10_2_7_1_parse_error_expected_gt_after_slash);

	     /* read name */
	    while (Character.isJavaIdentifierPart ((char) c)) {
		sb.append ((char) c);
		c = (char) r.read ();
	    }

	    name = sb.toString ();

	    if (name.length () == 0)
		throw new ParseException
		    (r.getPos(),
		     ERR_gnujsp_parse_attr_unnamed_jsp_directive);

	    sb.setLength (0);
	    do {
		c = readNoEOF(r);
 	    } while (Character.isWhitespace(c) || c == '=');    /* skip to first non-whitespace after = */

	    // if token begins with quote, record which kind.  Otherwise throw exception
	    // (JSP 1.0 spec requires quotes around directive values)
	    char quoteType = '"';
	    if ( c == '"' || c == '\'' ) {
		quoteType = c;
	    } else {
		throw new ParseException( r.getPos(), ERR_gnujsp_directive_value_missing_quotes );
	    }

	    /* in attributes:
	     *     " as \" resp. ' as \'
	     *     %> is escaped as %\> (not translated here)
	     *     <% is escaped as <\% (not translated here)
	     */
	    for (;;) { // forever (or up to an end quote)
		c = readNoEOF(r);
		if (c == '\\')
		    if ((c = readNoEOF(r)) == quoteType) {
			/* remember about possible escaped quotes */
			sb.append (quoteType);
		    } else {
			sb.append ('\\').append ((char)c);
		    }
		else if (c == quoteType)
		    break;
		else
		    sb.append (c);
	    }

	    t.put (name, sb.toString ());

	    sb.setLength (0);
	}

	return false;
    }

    void parseDeclaration (StackedReader r, JspNode body,
			   boolean isXMLStyle)
	throws IOException {

	Pos pos = r.getPos();
	String out = readToMatch(r, isXMLStyle ? "</jsp:declaration>" : "%>");
	if (!isXMLStyle) out = unescape(out);


	// FIXME: for good error messages for errors in declaration
	// we need a node abstraction for attribute declaration.
	// the pos insert is valid for java code only. (alph)

	String current = (String) body.getPageAttribute("declaration");
	body.setPageAttribute("declaration", ((current == null) ? "" : current)
			  + pos.toJavaCode() +"\n"
			  + out);
    }

    void parseDirective (StackedReader r, JspNode body, boolean isXMLStyle) throws IOException {
	StringBuffer buffer = new StringBuffer();
	char lastChar = readToken (r, buffer, !isXMLStyle);
	String directive = buffer.toString();

	Hashtable table     = new Hashtable (3);

	readNameAndValues (r, table, isXMLStyle);

	if (directive.equals("page")) {
	    Enumeration e = table.keys();

	    String key;
	    while (e.hasMoreElements()) {
		key = (String) e.nextElement();
		if ("language".equals(key) ||
		    "extends".equals(key) ||
		    "session".equals(key) ||
		    "buffer".equals(key) ||
		    "autoFlush".equals(key) ||
		    "isThreadSafe".equals(key) ||
		    "info".equals(key) ||
		    "errorPage".equals(key) ||
		    "isErrorPage".equals(key)) {
		    body.setPageAttribute(key, (String) table.get(key));
		} else if ("import".equals(key)) {
		    String current = (String) body.getPageAttribute("import");
		    body.setPageAttribute(key, ((current == null) ? "" : current + ",") + table.get(key));
		} else if ("contentType".equals(key)) {
		    String charset = "ISO8859_1";
		    String old = body.getPageAttribute("contentType");
		    if (old != null) throw new ParseException(r.getPos(), ERR_sp10_2_7_1_duplicate_attribute_contenttype);
		    String contentType = (String) table.get(key);
		    body.setPageAttribute(key, contentType);
		    // searching for attribute "charset" inside
		    // content-type string exactly as JServ does.
		    int start = contentType.indexOf("charset=");
		    int end;

		    if (start != -1 ) {
			String encoding = contentType.substring(start + "charset=".length());

			if ((end = encoding.indexOf(";")) > -1) {
			    charset = encoding.substring(0, end);
			}
		    }
		    body.setPageAttribute("charset", charset);
		} else {
		    throw new ParseException
			(r.getPos(),
			 JspConfig.getLocalizedMsg(ERR_sp10_2_7_1_invalid_attribute)
			 + ", " + key);
		}
	    }
	} else if (directive.equals("include")) {
	    String file = (String) table.get ("file");
	    if (file == null)
		throw new ParseException (r.getPos(),
					  ERR_sp10_2_7_6_missing_attr_file);
	    else {
		if(debug) {
		    System.err.println("include directive: file = "+file);
		}
		// FIXME this isn't very clean..
		int p1 = file.indexOf(':');
		int p2 = file.indexOf('/');
		if ((p1 != -1) && (p1 < p2)) {
		    throw new ParseException
			(r.getPos(),
			 ERR_sp10_2_7_6_illegal_attr_file_must_be_relative);
		}

		// The proper way to do this, in JSDK 2.1, is to use
		// ServletContext.getResourceAsStream(path)
		// If we're using JSDK 2.0, we have to work around it.

		// First calculate path as the URI
		if (!file.startsWith("/")) {
		    String current = (String) r.getCurrentInclude();
		    file = current.substring(0, current.lastIndexOf('/') + 1)
			+ file;
		}
		if(debug) {
		    System.err.println("include directive: file2 = "+file);
		}
		// normalize URI (remove "." and ".." sequences)
		// Note: leading ".." will be thrown away so we can't leave our
		// page base.
		URL resource = new URL("file:"+file);
		file = resource.getFile();
		if(debug) {
		    System.err.println("include directive: file3 = "+file);
		}

		resource = null;
		if (PageContextImpl.JSDK20) {
		    String realPath = config.getServletContext().getRealPath(file);
		    if(realPath == null) {
			// Use pagebase if all else fails.
			String pagebase = config.getInitParameter("pagebase");
			if (pagebase == null)
			    throw new ParseException
				(r.getPos(),
				 ERR_gnujsp_missing_pagebase);
			// Note that file always startsWith("/")
			realPath = pagebase + file;
		    } else if(usePageBase) {
			if(debug) {
			    System.err.println("include: realPath="+realPath);
			}
			String pagebase = config.getInitParameter("pagebase");
			realPath = (new File(pagebase,realPath)).getAbsolutePath();
		    }
		    resource = new URL("file:" + realPath);
		} else {
		    resource = config.getServletContext().getResource(file);
		    if (resource == null) {
			System.err.println(JspConfig.getLocalizedMsg
					   (ERR_gnujsp_broken_getresource));
			String realPath = config.getServletContext().getRealPath(file);
			if(realPath == null) {
			    // Use pagebase if all else fails.
			    String pagebase = config.getInitParameter("pagebase");
			    if (pagebase == null)
				throw new ParseException
				    (r.getPos(),
				     ERR_gnujsp_evtl_missing_pagebase);
			    // Note that file always startsWith("/")
			    realPath = pagebase + file;
			}
			if(realPath == null) {
			    throw new ParseException
				(r.getPos(), ERR_gnujsp_realpath_is_null);
			}
			resource = new URL("file:" + realPath);
		    }
		}
		if(debug) {
		    System.err.println("include: using resource: "+resource);
		}
		r.pushReader(new LineNumberReader(new InputStreamReader(resource.openStream()), BUFFER_SIZE), file);
	    }
	} else if (directive.equals("taglib")) {
	    throw new ParseException (r.getPos(),
				      ERR_gnujsp_taglib_not_supported);
	} else {
	    throw new ParseException (r.getPos(),
				      ERR_gnujsp_unrecognized_jsp_directive);
	}
    }

    JspNode parseExpression (StackedReader r, JspNode body,
			     boolean isXMLStyle) throws IOException {
	JspNode expression = new JspNode(JspNode.JSP_EXPRESSION, body);

	String out = readToMatch(r, isXMLStyle ? "</jsp:expression>" : "%>");
	if (!isXMLStyle) out = unescape(out);

	JspNode textNode = new JspNode(JspNode.TEMPLATE_TEXT, body);
	textNode.setAttribute("text", out);
	expression.addChild(textNode);
	return expression;
    }

    JspNode parseScriptlet (StackedReader r, char firstChar, JspNode body,
			    boolean isXMLStyle) throws IOException {
	JspNode scriptlet = new JspNode(JspNode.JSP_SCRIPTLET, body);

	String out = firstChar
	    + readToMatch(r, isXMLStyle ? "</jsp:scriptlet>" : "%>");
	if (!isXMLStyle) out = unescape(out);

	JspNode textNode = new JspNode(JspNode.TEMPLATE_TEXT, body);
	textNode.setAttribute("text", out);
	scriptlet.addChild(textNode);
	return scriptlet;
    }

    /**
     * Reads the next character from the Reader; throws an exception if
     * it gets an EOF.
     */
    private char readNoEOF(StackedReader r) throws IOException {
	int i = r.read();
	if (i == -1) throw new ParseException(r.getPos(),
					      ERR_gnujsp_unexpected_eof);
	return (char) i;
    }

    // Ripped this out of JavaEmitter with minor changes..
    // Turns %\> into %>
    static String unescape(String val) {
	StringBuffer b = new StringBuffer();
	int prev = 0;
	int i = 0;
	int pos;
	do {
	    pos = val.indexOf("%\\>", i);
	    if (-1 == pos) break;
	    i = 1 + pos;
	    b.append(val.substring(prev, i));
	    prev = ++i;
	} while (true);
	b.append(val.substring(prev));
	return b.toString();
    }

    // e.g. readToMatch(..., "</jsp:expression>"); or "%>"
    // This function does not do escaping.
    String readToMatch(StackedReader r, String match) throws IOException {
	int state = 0; // We have read 0 matching characters
	StringBuffer out = new StringBuffer();
	char ch;
	for (;;) { // forever
	    ch = readNoEOF(r);
	    if (ch == match.charAt(state)) {
		if (++state == match.length()) return out.toString();
	    } else {
		if (state > 0) {
		    out.append(match.substring(0,state));
		    state = 0;
		}
		out.append(ch);
	    }
	} // forever
    }

    void parseCloseTag(StackedReader r, JspNode body) throws IOException {
	char ch = (char) r.read ();
	if (ch != ':') throw new ParseException(r.getPos(),
						ERR_gnujsp_bad_jsp_tag_format);

	StringBuffer tagBuffer = new StringBuffer();
	ch = readToken(r, tagBuffer, false);
	String tag = tagBuffer.toString();

	// Check if tag matches what we're currently parsing.
	if (JspNode.typeFor(tag) != body.getType()) {
	    throw new ParseException(r.getPos(),
				     JspConfig.getLocalizedMsg(ERR_gnujsp_parse_close_tag_does_not_match_open_tag)
				     + ": </jsp:" + tag + ">, <jsp:"
				     + JspNode.tagNames[body.getType()]
				     + ">\n"
				     + JspConfig.getLocalizedMsg(ERR_gnujsp_check_if_you_forgot)
				     + " <jsp:"
				     + JspNode.tagNames[body.getType()] + "/>");
	}
    }

    JspNode parseTag (StackedReader r, JspNode body) throws IOException {
	char ch = readNoEOF(r);
	if (ch != ':')
	    throw new ParseException(r.getPos(),
				     ERR_gnujsp_bad_jsp_tag_format);

	StringBuffer tagBuffer = new StringBuffer();
	ch = readToken(r, tagBuffer, false);
	String tag = tagBuffer.toString();

	if ("directive".equals(tag) && ch == '.') {
	    parseDirective(r, body, true);

	    // WWB: We don't handle </jsp:directive.*> -- I can't tell from
	    // the spec. whether those are valid or not.  I don't see any
	    // reason to implement them..

	    return null;
	}

	boolean endSlash = false;
	Hashtable table = new Hashtable (3);
	Pos pos = r.getPos(); // the start position of the current tag

	// Check if we're already at the end.
	if (ch == '/') {
	    endSlash = true;
	    if ((ch = readNoEOF(r)) != '>') {
		throw new ParseException(pos, ERR_gnujsp_bad_jsp_tag_format);
	    }
	} else if (ch != '>') {
	    endSlash = readNameAndValues (r, table, true);
	}

	JspNode node = null;
	if (tag.equals("forward")) {
	    node = new JspNode(JspNode.JSP_FORWARD, body);
	    node.setAttribute("page", (String) table.get("page"));
	} else if (tag.equals("getProperty")) {
	    String name = (String) table.get("name");
	    String property = (String) table.get("property");
	    node = new JspNode(JspNode.JSP_GETPROPERTY, body);
	    node.setAttribute("name", name);
	    node.setAttribute("property", property);
	} else if (tag.equals("include")) {
	    node = new JspNode(JspNode.JSP_INCLUDE, body);
	    node.setAttribute("page", (String) table.get("page"));
	    node.setAttribute("flush", (String) table.get("flush"));
	} else if (tag.equals("plugin")) {
	    throw new ParseException (r.getPos(),
				      ERR_gnujsp_jspplugin_not_supported);
	} else if (tag.equals("setProperty")) {
	    node = new JspNode(JspNode.JSP_SETPROPERTY, body);
	    node.setAttribute("name", (String) table.get("name"));
	    node.setAttribute("property", (String) table.get("property"));
	    node.setAttribute("param", (String) table.get("param"));
	    node.setAttribute("value", (String) table.get("value"));
	} else if (tag.equals("useBean")) {
	    node = new JspNode(JspNode.JSP_USEBEAN, body);
	    node.setAttribute("id", (String) table.get ("id"));
	    node.setAttribute("scope", (String) table.get ("scope"));
	    node.setAttribute("type", (String) table.get ("type"));
	    node.setAttribute("class", (String) table.get ("class"));
	    node.setAttribute("beanName", (String) table.get ("beanName"));
	} else if (tag.equals("expression")) {
	    return parseExpression(r,  body, true);
	} else if (tag.equals("declaration")) {
	    parseDeclaration(r, body, true);
	    return null;
	} else if (tag.equals("scriptlet")) {
	    return parseScriptlet(r, ' ', body, true);
	}

	if (node == null)
	    throw new ParseException(r.getPos(),
				     ERR_gnujsp_unrecognized_jsp_tag);

	// remember where the tag was defined
	node.setPos(pos);

	if (endSlash) return node;

	// Recursively parse the tag
	parseJsp(r, node);

	return node;
    }
}
