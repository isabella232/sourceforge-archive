/*
  GNUJSP - a free JSP implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>
  Copyright (C) 1998-1999, Vincent Partington <vinny@klomp.org>

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
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, =
  USA.
*/

package org.gjt.jsp;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.GenericServlet;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter; // for jsdk 2.0 compat
import javax.servlet.RequestDispatcher; // for jsdk 2.0 compat
import javax.servlet.ServletRequest; // for jsdk 2.0 compat
import javax.servlet.ServletResponse; // for jsdk 2.0 compat
import java.net.HttpURLConnection; // for jsdk 2.0 compat

import org.gjt.jsp.jsdk20.HttpServletRequestWrapper;
import org.gjt.jsp.jsdk20.HttpServletResponseWrapper;
import org.gjt.jsp.jsdk20.ServletConfigWrapper;
import org.gjt.jsp.jsdk20.ServletContextWrapper;

public class JspServlet extends HttpServlet 
    implements JspMsg
{
    // FIXME: should we use that in gnujsp 1.0 in general? Needed? (alph)
    private static final String        lineSeparator
	= System.getProperty("line.separator");

    /** make GNUJSP use packages/sub directoried for generated files */ 
    private static boolean	usePackages = false;

    private File		scratchDir;
    private String[]		javacArgs;
    private boolean		debug;
    /** this is for extensive path debugging */
    private boolean             pathDebug = false;
    private File		pageBase;
    private String              denyURI;
    private boolean             checkDeps;
    private boolean             checkClass;
    private Hashtable		pages;
    private JspParser           parser;
    private String		dirSep;
        
    // If we emulate jsdk 2.1 on jsdk 2.0 we need one ServletContext
    // so that jsp servlets may share their data
    private ServletContextWrapper jspServletContext = null;
    // the server environment needs page base and has to use it.
    private boolean usePageBase = false;
    private int time = 0;
 
    static {
	JspFactory.setDefaultFactory (new JspFactoryImpl ());
	Emitter.setEmitter("java", new JavaEmitter());
    }

    public JspServlet () {
	pages = new Hashtable();
    }
    
    /** 
     * This is part of GenericServlet in JSDK 2.1, but for backward
     * compatibility we override.
     */
    public void log(String message, Throwable throwable) {
	if (PageContextImpl.JSDK20) {
	    log(message);
	    log(getStackTrace(throwable));
	} else super.log(message, throwable);
    }

    public void init (ServletConfig config) throws ServletException {
	super.init (config);
	ServletContext context = config.getServletContext ();
	boolean jserv = false;

	// engine work arounds
	if(PageContextImpl.JSDK20) {
	    String serverInfo = context.getServerInfo();
	    if("Rhapsody".equals(System.getProperty( "os.name" ))) {
		// allways use page base
		usePageBase = true;
	    } else if(serverInfo.startsWith("ApacheJServ/1.0")) {
		jserv = true;
	    } else if(serverInfo.startsWith("vqServer/1.9.")) {
		usePageBase = true;
	    }
	}

	if ((jserv || config.getInitParameter ("jserv") != null)
	    && time > 1)
	    return; /* workaround for a really weird bug w/apache jserv*/
	/* init() gets called twice before any request is
	   received, and then once for the first request,
	   but this time without any config args set!
	   I have a suspicition that this is because every time
	   it sees a reference to a servlet in the config (zone)
	   file, it instantiates one.  - Aug. 15, 1999
	*/

	time++;

	String scratchPath     = config.getInitParameter ("scratchdir");
	String compilerCommand = config.getInitParameter ("compiler");

	String pageBaseProp    = config.getInitParameter ("pagebase");    

	checkDeps = !"false".equals(config.getInitParameter ("checkdependencies"));
	checkClass = "true".equals(config.getInitParameter ("checkclass"));
	usePackages = "true".equals(config.getInitParameter ("usepackages"));
	dirSep = usePackages ? "._" : "__";
	denyURI = config.getInitParameter("denyuri");
	debug = "true".equals(config.getInitParameter("debug"));
	/** for extensive path debugging */
	pathDebug = "true".equals(config.getInitParameter("pathdebug"));

	// eg. DE or US
	String countryProp     = config.getInitParameter ("country"); 
	// eg. de or en
	String languageProp    = config.getInitParameter ("language");

	if(countryProp != null && languageProp != null) {
	    JspConfig.setLocale(new Locale(languageProp, countryProp));
	}

	if (scratchPath == null) {
	    throw new ServletException 
		(JspConfig.getLocalizedMsg(ERR_gnujsp_missing_init_parameter)
		 + ": \"scratchdir\", " + time);
	}

	if (pageBaseProp == null && usePageBase) {
	    throw new ServletException 
		(JspConfig.getLocalizedMsg(ERR_gnujsp_missing_init_parameter)
		+ ": \"pageBase\", " + time);
	}

	if(debug) { 
	    log("Debugging enabled"); 
	}

	if (compilerCommand == null)
	    compilerCommand = "builtin-javac -classpath %classpath%" +
		File.pathSeparator +
		"%scratchdir% -d %scratchdir% -deprecation -encoding %encoding% %source%";

	scratchDir = new File (scratchPath);

	StringTokenizer st = new StringTokenizer (compilerCommand);

	javacArgs  = new String [st.countTokens ()];

	for (int i = 0;  i < javacArgs.length;  ++i)
	    javacArgs[i] = st.nextToken ();
   
	if (pageBaseProp != null)
	    pageBase = new File (pageBaseProp);

	if(debug) {
	    log("scratchdir="+ scratchDir.getAbsolutePath()
		+", checkDeps="+checkDeps
		+", pagebase="+pageBaseProp
		+", forcePageBase="+usePageBase
		+", usePackages="+usePackages);
	}

	if((! scratchDir.exists() && !scratchDir.mkdirs ())
	   || !scratchDir.isDirectory()
	   ) {
	    log(JspConfig.getLocalizedMsg(ERR_gnujsp_could_not_create)
		+" '"+scratchDir.getAbsolutePath()+"'");
	    throw new ServletException
		(JspConfig.getLocalizedMsg(ERR_gnujsp_could_not_create)
		 + " scratchDir");
	}

	parser = new JspParser(config);
	// for use of pagebase for some engines
	parser.setUsePageBase(usePageBase);

	if(debug) {
	    parser.setDebug(true);
	    log("JspServlet: initialized");
	}
    }

    private static String urlDecode(String val) {
        StringBuffer	buf = new StringBuffer(val.length());
        char		c;

	for (int i = 0; i < val.length(); i++) {
	    c = val.charAt(i);
	    if(c == '%') {
		try {
		    buf.append((char)Integer.parseInt (val.substring (i+1,i+3),
						       16));
		    i += 2;
		    continue;
		} catch(Exception e) { }
	    } else if(c == '+') {
		buf.append(' ');
		continue;
	    }
	    buf.append(c);
	}
        return buf.toString();
    }

    // Deal with different ways servlet engines hide /dir1/dir2/file.jsp
    private static String requestToJspURI (HttpServletRequest req) {
	return (((req.getPathInfo() != null) && !"".equals(req.getPathInfo())) ? req.getPathInfo() :
		((req.getRequestURI() != null) ? urlDecode(req.getRequestURI()) :
		 ((req.getServletPath() != null) ? req.getServletPath() :
		  null)));
    }


    protected void service (HttpServletRequest request,
			    HttpServletResponse response)
	throws IOException, ServletException
    {
	if(debug) {
	    log("service started");
	    /*
	    log("getPathInfo() = '"+request.getPathInfo()+"'");
	    log("getRequestURI() = '"+request.getRequestURI()+"'");
	    log("getServletPath() = '"+request.getServletPath()+"'");
	    log("getPathTranslated() = '"+request.getPathTranslated()+"'");
	    log("sc.getRealPath(getServletPath()) = '"
	        +getServletConfig().getServletContext()
	           .getRealPath(request.getServletPath())+"'");
	    */
	}

	String jspURI  = requestToJspURI (request);
	if ((denyURI != null) && (jspURI.startsWith(denyURI))) {
	    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}
	Page   page    = null;

	// synchronize needed,
	// multiple JspServlet.service() calls
	// for same page are possible here (alph)
	synchronized(pages) {
	    // Create the page if it doesn't exist
	    if ((page = (Page) pages.get (jspURI)) == null) {
		page = new Page (jspURI);
		pages.put (jspURI, page);
	    }
	}
	
	// Forward the request to the page
	try {
	    page.process (request, response);
	} catch (FileNotFoundException fnf) {
	    if(debug) {
		log("service: file not found: "+ fnf.getMessage());
	    }
	    response.sendError (HttpServletResponse.SC_NOT_FOUND, "JSP File Not Found");
	} catch (ParseException pe) {
	    response.setContentType ("text/html");
	    //response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    PrintWriter w = new PrintWriter (response.getWriter ());
	    pe.writeHTMLMessage(w);
	    // Should not hurt and vqserver 1.9.17 needs it. (alph)
	    w.flush();
	    return;
	} catch (CompileException ce) {
	    response.setContentType ("text/html");
	    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    PrintWriter w = new PrintWriter (response.getWriter ());
	    w.println("<HTML><HEAD><TITLE>GNUJSP Compiler Exception"
		      +"</TITLE></HEAD><BODY><PRE>");
	    w.println(JspConfig.getLocalizedMsg(ERR_gnujsp_error_compiling_source_file)
		      +": " + page.jspURL);
	    // FIXME
	    //	    w.println("Compilation command was: "+page.compileString);
	    w.println("");
	    // FIXME: htmlEncode? (alph)
	    // FIXME: needed in PageContext,too! (alph)
	    // FIXME: use in other exceptions (alph)
	    w.println(transcribeErrors(page, ce.getMessage()));
	    w.println("</PRE></BODY></HTML>");
	    // Should not hurt and vqserver 1.9.17 needs it. (alph)
	    w.flush();
	} catch (Throwable t) {
	    log(JspConfig.getLocalizedMsg(ERR_gnujsp_unhandled_exception), t);

	    // FIXME: There are problems with this and flushing.
	    response.setContentType ("text/html");
	    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    PrintWriter w = new PrintWriter (response.getWriter ());
	    w.println ("<body bgcolor=\"#ffffff\"><pre>");
	    w.println (JspConfig.getLocalizedMsg
		       (ERR_gnujsp_exception_while_servicing_request)
		       + " "+ jspURI + ":");
	    if (page != null) {
		// FIXME
		//		w.println ("Resulting java file would be "+page.javaFile);
		//		w.println ("Resulting class file would be "+page.classFile);
		//		w.println ("Compilation command was: "+page.compileString);
	    }
	    w.println ("");
	    // FIXME: HTML-Encode?
	    w.print(getStackTrace (t));
	    w.println ("</pre></body>");
	    // Should not hurt and vqserver 1.9.17 needs it. (alph)
	    w.flush();
	}
    }

    /** Destroy all servlets generated by this JspServlet. */
    public void destroy() {
	super.destroy();

	Enumeration e = pages.elements ();
	while (e.hasMoreElements()) {
	    Page p = (Page) pages.get (e.nextElement());
	    p.destroy ();
	}
	pages.clear();
    }

    /**
     * Developers: Change this to ensure that pages get recompiled when
     * you add new code to the system.  Format is YYYYMMDD[SERIAL].
     */
    static long getCompilerVersion() {
	return 1999101701L;
    }

    /** Identifies the version of the GNUJSP servlet. */
    public String getServletInfo() {
	return "GNUJSP 1.0.0"; 
    }
    
    // this is the ServletConfig to be passed to JspPages
    private ServletConfig getJspServletConfig() {
	if(jspServletContext == null)
	    jspServletContext = 
		new ServletContextWrapper(getServletContext(),
					  this);
	return PageContextImpl.JSDK20 
	    ? new ServletConfigWrapper(getServletConfig(), 
				       jspServletContext)
	    : getServletConfig();
    }

    /**
     * Transcribe error messages to point to line number
     * in jsp file.
     */
    private String transcribeErrors(Page page, String errors) {
	LineNumberReader	in;
	BufferedReader		in2;
	Vector			map;
	Enumeration		e;
	MapEntry		entry;
	StringBuffer		errBuf;
	String			s, jspFile;
	int			i, j, k, l, javaLineNr, jspLineNr;
	String                  posCodePrefix = Pos.getCodePrefix();
	File                  javaFile = page.getSourceFile();
	String               filePath = javaFile.getPath();

	try {
	    /* Build mapping from java line numbers to jsp file/line# 
	     * combinations.
	     * We could have done this while writing the java file, but using
	     * LineNumberReader is easier, and it would take extra time when
	     * no errors occur.
	     */
	    in = new LineNumberReader(new FileReader(javaFile));
	    in.setLineNumber(1);
	    map = new Vector();
	    try {
		while((s = in.readLine()) != null) {
		    if(s.startsWith(posCodePrefix)) {
			try {
			    MapEntry me = new MapEntry(in.getLineNumber(),
							new Pos(s));
			    map.addElement(me);
			} catch(NumberFormatException nfexc) { 
			} catch(IllegalArgumentException ee) { }
		    }
		}
	    } finally {
		in.close();
	    }

	    /* Now we read every line of the error messages and translate any
	     * file references there.
	     */
	    in2 = new BufferedReader(new StringReader(errors));
	    errBuf = new StringBuffer();
	    try {
		while((s = in2.readLine()) != null) {
		    i = s.indexOf(filePath);
		    if(i != -1) {
			j = i + filePath.length();
			if(j < s.length()-1 
			   && s.charAt(j) == ':' 
			   && Character.isDigit(s.charAt(j+1))) {
			    j++;
			    k = j;
			    while(k < s.length() 
				  && Character.isDigit(s.charAt(k))) {
				k++;
			    }
			    l = k;
			    while(l+1 < s.length() 
				  && s.charAt(l) == ':' 
				  && Character.isDigit(s.charAt(l+1))) {
				l += 2;
				while(l < s.length() 
				      && Character.isDigit(s.charAt(l))) {
				    l++;
				}
			    }
			    
			    try {
				javaLineNr = Integer.parseInt(s.substring(j, k));
				jspFile = null;
				jspLineNr = 0;
				for(e = map.elements(); 
				    e.hasMoreElements(); ) {
				    entry = (MapEntry) e.nextElement();
				    if(entry.javaLineNr > javaLineNr) {
					break;
				    }
				    jspFile = entry.jspPos.getFile();
				    if(entry.jspPos.hasExtraInfo() &&
				       "+".equals(entry.jspPos.getExtraInfo())) {
					// one source code line corresponds to multiple
					// java code lines
					jspLineNr = entry.jspPos.getLine();
				    } else {
					// one source code line corresponds to one
					// java code line
					jspLineNr = entry.jspPos.getLine()
					    + (javaLineNr - entry.javaLineNr);
				    }
				}
				// valid translation found: use it
				if(jspFile != null) {
				    errBuf.append(s.substring(0, i));
				    errBuf.append("<B>").append(jspFile).append((char) ':').append(jspLineNr).append("</B>");
				    errBuf.append("<!-- ").append(s.substring(i, l)).append(" -->");
				    errBuf.append(s.substring(l)).append(lineSeparator);
				    continue;
				}
			    } catch(NumberFormatException nfexc2) { }
			}
		    }
		    errBuf.append(s).append(lineSeparator);
		}
		return errBuf.toString();
	    } finally {
		in2.close();
	    }
	} catch(IOException ioexc) {
	    return errors;
	}
    }
	
    public static File getFileName (File scratchdir, String classname, String ext, boolean mkdir) {
        File resp;
        if (usePackages) {
            resp = new File (scratchdir, classname.replace ('.', File.separatorChar) + ext);
            if (mkdir)
                (new File (resp.getParent ())).mkdirs ();
	}
        else
            resp = new File (scratchdir, classname + ext);

        return resp;
    }

    private String getStackTrace(Throwable e) {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	e.printStackTrace(pw);
	pw.flush();
	return sw.toString();
    }

    /**
     * FIXME: does using static here has any benefits (alph)
     * (copied from gnujsp 0.9)
     */ 

    private static class MapEntry {
	int	javaLineNr;
	Pos     jspPos;
	
	MapEntry(int javaLineNr, Pos pos) {
	    this.javaLineNr = javaLineNr;
	    this.jspPos = pos;
	}

    }

    class Page {
	// The virtual path of this JSP page
	private String jspURI;
	
	// The actual path of the JSP file
	private URL jspURL;

	// The generated java file
	private File sourceFile = null;

	// The servlet generated for this JSP page
	private HttpJspPage servlet;

	// The exception that occurred when generating the servlet,
	// or null if all is well.
	private Exception generationException;

	// A classloader instance handling this servlet.
	private JspClassLoader classLoader;

	// The set of dependencies.
	// Dependencies consist of the JSP file itself and any files
	// which are included statically using the include directive.
        private URL[] deps;

	/** Constructs an empty page object for the given URI. */
	Page(String jspURI) {
	    this.jspURI = jspURI;
	}

	/** Process a servlet request destined for the 
	    generated JSP servlet. */
	// FIXME: Deal with exceptions more elegantly.
	void process (HttpServletRequest request, HttpServletResponse response)
	    throws ServletException, IOException, ClassNotFoundException,
		   IllegalAccessException, InstantiationException, Exception
	{
	    // Only allow one thread to compile this at a time
	    synchronized (this) {
		if (needToRecompile (request)) {
		    generationException = null;
		    try {
			if(jspURL == null) {
			    throw new ServletException
				(JspConfig.getLocalizedMsg
				 (ERR_gnujsp_internal_error)
				 + ": jspURL==null");
			}
			if (debug) log("Parsing " + jspURL);
			StackedReader sr = new StackedReader
			    (new InputStreamReader(jspURL.openStream()), 
			     jspURI);
			JspNode body = parser.parse(sr);

			// StackedReader knows what files were included.
			String[] predeps = sr.getAllIncludes();
			sr.close();

			deps = new URL [predeps.length];
			StringBuffer concatDeps = new StringBuffer();
			for (int i = 0; i < predeps.length; i++) {
			    if (debug) log("Depends on " + predeps[i]);
			    if (i > 0) concatDeps.append(',');
			    concatDeps.append(predeps[i]);
			    deps[i] = getResourceImpl(predeps[i], request);
			}
			body.setAttribute("gnujspDeps", concatDeps.toString());
			body.setAttribute("gnujspCompilerVersion", 
					  String.valueOf(getCompilerVersion()));
			String className = getClassName(jspURI);
			String classPart = className;
			String packageName = null;
			int lastDot = className.lastIndexOf ('.');
			if (lastDot != -1) {
			    packageName = className.substring (0, lastDot);
			    classPart = className.substring (lastDot + 1);
			}

			// This is a kludge for JavaEmitter.
			body.setAttribute("package", packageName);
			body.setAttribute("class", classPart);

			sourceFile = JspServlet.getFileName (scratchDir, 
							     className, 
							     ".java", true);
			if (debug) log("Emitting source to " + sourceFile);
			BufferedWriter w = 
			    new BufferedWriter(new FileWriter(sourceFile));
			Emitter.emit(w, body);
			w.close();

			if (debug) log("Compiling " + sourceFile);
			compile(sourceFile, body.getAttribute("charset"));
			// FIXME: Broken if we ever start using packages.
			if (debug) log("Loading " + className);
			load(className);
			if (debug) log("Initializing servlet " + className);
			// I need a pointer to JspServlet
			// for jsp:include/jsp:forward on jsdk 2.0 (alph)
			servlet.init(getJspServletConfig());
		    } catch (Exception e) {
			generationException = e;
		    }
		}
	    }
	    if (generationException != null) throw generationException;
	    if (request.getParameter("jsp_precompile") == null) {
		servlet.service ((PageContextImpl.JSDK20) 
				 ? new HttpServletRequestWrapper(request) 
				 : request, response);
	    }
	}
    
	URL getResourceImpl(String path, HttpServletRequest request) 
	    throws MalformedURLException {
	    URL retval = null;
	    ServletContext sc = getServletConfig().getServletContext();
	    if (PageContextImpl.JSDK20) {
		String s = request.getRequestURI();
		if (pathDebug) log("getResIm: s1="+s);
		// FIXME: this does not make much sense!
		if (request.getPathInfo() != null)
		    s = request.getPathInfo();
		if (pathDebug) log("getResIm: s2="+s);
		if (s == null || "".equals(s)) 
		    s = request.getServletPath();
		if (pathDebug) log("getResIm: s3="+s);
		String p = sc.getRealPath(s);
		if (pathDebug) log("getResIm: p1="+p);
		if (p == null) p = request.getPathTranslated();
		if (pathDebug) log("getResIm: p2="+p);
		// prepend path with pageBase. Vqserver or mac (alph)
		if(usePageBase) {
		    p = (new File(pageBase,p)).getAbsolutePath(); 
		}
		if (pathDebug) log("getResIm: p3="+p);
		// this works hopefully on jserv/NT
		if(p==null && pageBase != null && s != null) {
		    p = pageBase + s;
		}
		if(p == null) {
		    log(JspConfig.getLocalizedMsg
			(ERR_gnujsp_could_not_find_resourceimpl));
		    throw new RuntimeException
			(JspConfig.getLocalizedMsg
			 (ERR_gnujsp_could_not_find_resourceimpl));
		}

		retval = new URL("file:" + p);
		if(debug) log("getResIm: return "+retval.toString());
	    } else {
		retval = sc.getResource(path);
		// This is a workaround for problematic engines 
		// Remind me to file a bug report for Jigsaw. --Wes
		if (retval == null) {
		    log(JspConfig.getLocalizedMsg(ERR_gnujsp_broken_getresource));
		    retval = new URL("file:" + sc.getRealPath(path));
		}
	    }
	    return retval;
	}

	boolean needToRecompile (HttpServletRequest request) 
	    throws MalformedURLException, ServletException {

	    if (checkClass && (servlet == null)) {
		// See if class is already compiled and current.
		try {
		    // On success, this sets servlet to a new instance.
		    load(getClassName(jspURI));

		    long compilerVersion = getCompilerVersion();
		    if (servlet instanceof HttpJspPageImpl) {
			compilerVersion = ((HttpJspPageImpl) servlet).
			    _gnujspGetCompilerVersion();
		    } else { // Try reflection
			try {
			    compilerVersion = ((Long) servlet.getClass().getMethod
					       ("_gnujspGetCompilerVersion", 
						null).invoke(null, null)).longValue();
			} catch (Exception reflectEx) { }
		    }
		    if (compilerVersion < getCompilerVersion()) {
			servlet = null;
		    } else {
			String[] predeps = null;
			if (servlet instanceof HttpJspPageImpl) {
			    predeps = ((HttpJspPageImpl) servlet)._gnujspGetDeps();
			} else { // Try reflection
			    try {
				predeps = (String[]) servlet.getClass().
				    getMethod("_gnujspGetDeps", null).invoke(null, null);
			    } catch (Exception reflectEx) { }
			}
			
			if (predeps != null) {
			    deps = new URL [predeps.length];
			    for (int i = 0; i < predeps.length; i++) {
				if (debug) log("Depends on " + predeps[i]);
				deps[i] = getResourceImpl(predeps[i], request);
			    }
			} // else what? FIXME.
			
			// We should really only do this after checking deps
			servlet.init(getJspServletConfig());
		    }

		} catch (ClassNotFoundException e) {
		    // We have to do the compiling.
		} catch (IllegalAccessException e) {
		    // FIXME
		} catch (InstantiationException e) {
		    // FIXME
		} catch (ServletException e) {
		    // Thrown by servlet.init()
		    generationException = e;
		    throw e;
		}
	    }

	    if (jspURL == null) {
		try {
		    jspURL = getResourceImpl(jspURI, request);
		} catch (MalformedURLException mue) {
		    generationException = mue;
		    throw mue;
		}
	    }

	    if (servlet == null) return true;
	    if (deps == null) return true; // First time
	   
	    if (checkDeps) {
		long compileTime = Long.MAX_VALUE; // Workaround non HttpJspPageImpl
		if (servlet instanceof HttpJspPageImpl) {
		    compileTime = ((HttpJspPageImpl) servlet)._gnujspGetTimestamp();
		} else { // Try reflection
		    try {
			compileTime = ((Long) servlet.getClass().getMethod("_gnujspGetTimestamp", null).invoke(null, null)).longValue();
		    } catch (Exception reflectEx) { }
		}
		    
		if (debug) log("compileTime was " + compileTime);
		try {
		    for (int i = 0;  i < deps.length;  ++i) {
			long l;
			// Ugly kludge for local files.  Shame on Sun for not
			// providing getLastModified() for FileURLConnections.
			if (deps[i].toString().startsWith("file:")) {
			    l = new File(deps[i].toString().substring(5)).lastModified();
			} else {
			    l = deps[i].openConnection().getLastModified();
			}
			if (debug) log(deps[i] + " timestamp " + l);
			if (compileTime < l)
			    return true;
		    }
		} catch (IOException e) {
		    generationException = e;
		    return false;
		}
	    }
	    
	    return false;
	}

	// Invoke the target language compiler
	void compile (File javaFile, String charset) throws CompileException {
	    String[] compilerArgs;
	    boolean useBuiltinJavac;
	    ByteArrayOutputStream compilerOut = new ByteArrayOutputStream();
	    PrintStream compilerOutStream = new PrintStream(compilerOut, true);
	    int j;
	   
	    // build compiler command line
	    if (javacArgs[0].equals("builtin-javac")) {
		compilerArgs = new String [javacArgs.length - 1];
		useBuiltinJavac = true;
		j = 0;
	    } else {
		compilerArgs = new String [javacArgs.length];
		compilerArgs[0] = javacArgs[0];
		useBuiltinJavac = false;
		j = 1;
	    }
	   
	    compilerOutStream.print (javacArgs[0]);

	    for(int i = 1; i < javacArgs.length; i++) {
		String arg  = javacArgs[i];
		String repl = null;
	   
		if (arg.indexOf ("%classpath%") != -1)
		    repl = System.getProperty ("java.class.path");
		else if (arg.indexOf ("%scratchdir%") != -1)
		    repl = scratchDir.toString ();
		else if (arg.indexOf ("%source%") != -1)
		    repl = javaFile.toString ();
		else if (arg.indexOf ("%encoding%") != -1)
		    repl = (charset != null) 
			? charset : System.getProperty("file.encoding");
	
		int first = arg.indexOf ('%');
		int last  = arg.lastIndexOf ('%');
	
		if (first != -1 && last != -1 && first != last) {
		    compilerArgs[j] = (arg.substring (0, first) + repl +
				       arg.substring (last+1));
		} else {
		    compilerArgs[j] = arg;
		}

		compilerOutStream.print((char) ' ');
		compilerOutStream.print (compilerArgs[j++]);
	    }
	   
	    compilerOutStream.println();
	    if (debug) {
		log("Compiler command: " + compilerOut.toString());
	    }

	    if (useBuiltinJavac) {
			try {
		    Class javacMain = Class.forName("sun.tools.javac.Main");
		    Constructor twoArg = javacMain.getConstructor(new Class[] {
			OutputStream.class, String.class });
		    Object javac = twoArg.newInstance(new Object[] {
			compilerOutStream, "javac" });
		    Method compile = javacMain.getMethod("compile", 
							 new Class[] {
			String[].class });
		    Object compileOut = compile.invoke(javac, new Object[] {
			compilerArgs });
		    if (!((Boolean) compileOut).booleanValue()) {
			generationException = new CompileException(compilerOut.toString());
			throw (CompileException) generationException;
		    }
		} catch (Exception e) { // Many types of reflection errors
		    generationException = new CompileException(e.getMessage());
		    throw (CompileException) generationException;
		}
	    } else {
		Process		p;
		BufferedReader	stdout = null;
		BufferedReader  stderr = null;
		String		line;
		int		exitValue = -1;
		long		classLastModified;
	
		// FIXME
		//		classLastModified = classFile.lastModified();
	
		try {
		    p = Runtime.getRuntime().exec (compilerArgs);
		    stdout = new BufferedReader (new InputStreamReader
						 (p.getInputStream()));
		    stderr = new BufferedReader (new InputStreamReader
						 (p.getErrorStream()));
		   
		    while((line = stdout.readLine()) != null)
			compilerOutStream.println(line);
		   
		    while((line = stderr.readLine()) != null)
			compilerOutStream.println(line);
		   
		    try {
			p.waitFor();
			exitValue = p.exitValue();
		    } catch(InterruptedException ix) {
		    }
		} catch(IOException ex) {
		    ex.printStackTrace(compilerOutStream);
		}

		// FIXME
		/*
		  if (classFile.lastModified() == classLastModified) {
		    compilerOutStream.println
			("[no class file has been written]");
		    exitValue = -1;
		}
		*/

		if (exitValue != 0) {
		    throw new CompileException (compilerOut.toString ());
		}
	    }
	    //compileTime = System.currentTimeMillis();

	}

	void load (String className)
	    throws ClassNotFoundException, IllegalAccessException,
		   InstantiationException {
	    classLoader = new JspClassLoader (scratchDir);
	    Class c = classLoader.loadClass (className);
	    servlet = (HttpJspPage) c.newInstance ();
	}

	void destroy () {

	    try {
		servlet.destroy ();

	    } catch (Exception e) {
		/* no code intended - no one needs to know about this */
	    } finally {

		servlet = null;
	    }
	}

	String getClassName (String jspFile) {
	    StringTokenizer st;
	    StringBuffer    buf;
	    String	    token;
	    char	    c;
	    int		    i;

	    st  = new StringTokenizer (jspFile,
				       String.valueOf (File.separatorChar));

	    buf = new StringBuffer (jspFile.length() + 32);

	    buf.append("jsp");

	    while (st.hasMoreTokens ()) {
		token = st.nextToken();
	
		buf.append(dirSep);

		for (i = 0;  i < token.length();  i++) {
		    c = token.charAt(i);

		    if (Character.isJavaIdentifierPart (c))
			buf.append (c);
		    else
			buf.append ((char) '_').
			    append (Integer.toHexString (c));
		}
	    }
	
	    return buf.toString();
	}
	/**
	 * we need access to the java source for line number
	 * translation
	 */
	File getSourceFile() {
	    return sourceFile;
	}
    }
    /**
     * jsp:include/jsp:forward support for jsdk20
     */

    public void doForward(ServletContext sc,
			  String relativeURL,
			  ServletRequest srequest, 
			  ServletResponse sresponse) 
	throws ServletException, IOException
    {
	String s = null;
	HttpServletRequestWrapper request 
	    = (HttpServletRequestWrapper) srequest;
	HttpServletResponse response = (HttpServletResponse) sresponse;

	// We set parameters similiar to include  but don't need to 
	// wrap it because old values should not be used anymore. (alph)

	request.setAttribute("javax.servlet.forward.request", 
			      relativeURL);

	/* servletpath component */ 
	s = request.getServletPath();
	if(s != null) {
	    request.setAttribute("javax.servlet.forward.servlet", s);
	}
	/* the pathinfo component */
	request.setAttribute("javax.servlet.forward.path",
			      relativeURL
			      );
	/* the querystring */
	s = request.getQueryString();
	if(s != null) {
	    request.setAttribute("javax.servlet.forward.query", s);
	}
	String   jspURI = sc.getRealPath(relativeURL);

	if(pathDebug)
	    log("doForward: jspURI1 = "+jspURI);

	if(jspURI == null) {
 	    jspURI = request.getRequestURI();
	    if(pathDebug)
		log("doForward: jspURI2 = "+jspURI);
	}
	
	if(jspURI == null) {
 	    jspURI = request.getPathTranslated();
	    if(pathDebug)
		log("doForward: jspURI3 = "+jspURI);
	}

	if(jspURI == null) {
	    throw new FileNotFoundException
		(JspConfig.getLocalizedMsg
		 (ERR_sp10_2_13_5_could_not_find_forward_file)
		 + ": " + relativeURL);
	}
	try {
	    Page page = null;

	    // synchronize needed,
	    // multiple JspServlet.service() calls
	    // for same page are possible here (alph)
	    synchronized(pages) {
		if ((page = (Page) pages.get (jspURI)) == null) {
		    
		    page = new Page (jspURI);
		    
		    pages.put (jspURI, page);
		}
	    }
	    // FIXME: need to change/wrap response?
	    page.process (request, response);
	} catch(FileNotFoundException e) {
	    // FIXME: another way without exception to check if jsp 
	    // file exists?

	    // the file was not found as a jsp page, 
	    // so we try it as a servlet

	    if(!doServletInclude(jspURI, relativeURL,
				 request,
				 response)) {
		// the file was not found as a jsp page or servlet,
		// This is not up to spec, but works for some cases
		((HttpServletResponse) response).sendRedirect(relativeURL);
	    }
	} catch(Exception e) {
	    // IllegalAccess, ClassNotFoundm Instantiation
	    // FIXME: reason? how to handle?
	    throw new ServletException(e.toString());
	}
    }

    public void doInclude(ServletContext sc,
			  String relativeURL,
			  JspWriter jspWriter,
			  ServletRequest srequest, 
			  ServletResponse sresponse) 
	throws ServletException, IOException
    {
	String s = null;
	if(relativeURL == null) 
	    throw new IOException
		(JspConfig.getLocalizedMsg(ERR_gnujsp_internal_error)
		 + ": relativeURL==null");

	HttpServletRequestWrapper request 
	    = (HttpServletRequestWrapper) srequest;
	HttpServletResponse response = (HttpServletResponse) sresponse;

	// B.9.2 Additional attributes for include
	// Should they be set on request? on wrapped request?
	// We can't remove them after include, so I guess
	// it's a wrapped request. (alph)
	HttpServletRequestWrapper wrequest = new
	    HttpServletRequestWrapper(request);

	wrequest.setAttribute("javax.servlet.include.request", 
			      relativeURL);

	/* servletpath component */ 
	s = request.getServletPath();
	if(s != null) {
	    wrequest.setAttribute("javax.servlet.include.servlet", s);
	}
	/* the pathinfo component */
	wrequest.setAttribute("javax.servlet.include.path",
			      relativeURL
			      );
	/* the querystring */
	s = request.getQueryString();
	if(s != null) {
	    wrequest.setAttribute("javax.servlet.include.query", s);
	}

	String   jspURI = sc.getRealPath(relativeURL);

	if(jspURI == null) {
 	    jspURI = wrequest.getRequestURI();
	}
	
	if(jspURI == null)
 	    jspURI = wrequest.getPathTranslated();

	if(jspURI == null) {
	    throw new FileNotFoundException
		(JspConfig.getLocalizedMsg
		 (ERR_sp10_2_13_5_could_not_find_include_file)
		 + ": " + relativeURL);
	}
	try {
	    Page page = null;

	    // synchronize needed,
	    // multiple JspServlet.service() calls
	    // for same page are possible here (alph)
	    synchronized(pages) {
		if ((page = (Page) pages.get (jspURI)) == null) {
		    
		    page = new Page (jspURI);
		    
		    pages.put (jspURI, page);
		}
	    }
	    JspWriter w = new org.gjt.jsp.jsdk20.JspWriterImpl(jspWriter, 
					      8192 /* FIXME */, 
					      false);
	    HttpServletResponseWrapper wres = 
		new HttpServletResponseWrapper(response);
	    PrintWriter pw = new PrintWriter(w); 
	    wres.setWriter(pw);
	    
	    page.process (wrequest, wres);
	    pw.flush();
	} catch(FileNotFoundException e) {
	    // FIXME: another way without exception to check if jsp 
	    // file exists?

	    // the file was not found as a jsp page, 
	    // so we try it as a servlet

	    if(!doServletInclude(jspURI, relativeURL,
				 wrequest,
				 response)) {
		// the file was not found as a jsp page or servlet,
		// so we try to get it via HTTP protocol
		doHTTPInclude(relativeURL, jspWriter, wrequest);
	    }
	} catch(Exception e) {
	    // IllegalAccess, ClassNotFound, Instantiation
	    // FIXME: reason? Which to pass?
	    if(debug) {
		log("doInclude: exception", e);
	    }
	    throw new ServletException(e.toString());
	}
    }

    private boolean doServletInclude(String jspURI,
				  String relativeURL,
				  HttpServletRequest request,
				  HttpServletResponse res) 
	throws IOException, ServletException
    {
	// relativeURI?
	String servletName = jspURI.substring(jspURI.lastIndexOf("/")+1);
	Servlet servlet = getServletContext().getServlet(servletName);
	if(servlet != null) {
	    // we need a response wrapper to that
	    // the servlet may use res.getServletOutputStream()
	    // without getting java.lang.IllegalStateException
	    // because getWriter() was already called.
	    // I didn't used it in doInclude() because it
	    // is not needed and may break things (not tested) (alph)
	    HttpServletResponse response = 
		new HttpServletResponseWrapper(res);
	    
	    
	    // FIXME: calling init must be done by servlet engine!
	    // FIXME: we don't know if it is already called!
	    // FIXME: document that! (alph)
	    // servlet.init(info);
	    
	    // This is from ApacheJSSI:
	    
	    // there is no defined way to get a STM-servlet to execute
	    // it. The behaviour is undefined
	    if (servlet instanceof SingleThreadModel &&
		servlet instanceof GenericServlet) {
		((GenericServlet) servlet).log 
		    (JspConfig.getLocalizedMsg
		     (ERR_gnujsp_unsafe_singletread_execute));
	    }

	    // execute servlet
	    servlet.service((ServletRequest) request, 
			    (ServletResponse) response);
	}

	return (servlet != null);
    }
	    
    private void doHTTPInclude(String relativeURL, JspWriter out, 
			       HttpServletRequest request) 
	throws IOException
    {
	int resCode = 0;
	try {
	    char buf[] = new char[8192];
	    URL url = new URL(makeRequestURL(request));
		
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    BufferedReader r = 
		new BufferedReader(new InputStreamReader(conn.getInputStream()));

	    int i = 0;
	    while((i = r.read(buf, 0, buf.length)) > -1) {
		if(i > 0) {
		    out.write(buf, 0, i);
		}
	    }
	    r.close();
	    resCode = conn.getResponseCode();
	    conn.disconnect();
	} catch(MalformedURLException ee) {
	    throw new FileNotFoundException(relativeURL);
	}
	if(resCode == HttpServletResponse.SC_NOT_FOUND ) {
	    throw new FileNotFoundException(resCode + ": "+ relativeURL);
	}
    }
    /**
     *
     * reconstruct the url this page was called with
     *
     */ 
    private String makeRequestURL(HttpServletRequest req) 
	throws IOException
    {
	int port = req.getServerPort();
	String strPort = "";
	if(port != 80)
		strPort = ":" + port;

	String path = req.getPathInfo();
	if(path == null || path.equals(""))
	    path = req.getServletPath();

        if(path == null)	
	    throw new IOException(JspConfig.getLocalizedMsg
				  (ERR_gnujsp_could_not_reconstruct_url));
          
	return req.getScheme()+"://"+req.getServerName()+strPort+path;        
    }
}
