// ========================================================================
// A very basic CGI Servlet, for use, originally, with Jetty
// (www.mortbay.com). It's heading towards CGI/1.1 compliance, but
// still lacks a few features - the basic stuff is here though...
// Copyright 2000 Julian Gosnell <jules_gosnell@yahoo.com>
// Released under the terms of the Jetty License.
//
// For all problems with this servlet please email jules_gosnell@yahoo.com,
// NOT the Jetty support lists
//
// 2000/10/18 - modifications by Preston L. Bannister (preston@home.com)
//      Added cache of script lookups.
//      Added test for forbidden relative directory up links.
//      Filled out support for CGI 1.1, 1.2 and common use variables.
//      Added support for CGI on Windows via Windows Scripting Host.
//      Added a bit of paranoia in invocation...
//      Added a (configurable) standard "runner" for CGI scripts.
//      Added a (configurable) standard "extension" for CGI scripts.
//      Added documentation of the variables supported.
//      Added more flexible environment handling.
//      Added support for non-ASCII character encodings.
//      Removed Jetty dependencies.
// ========================================================================

package uk.org.gosnell.Servlets;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
    <h3>NOTES ON USE</h3>

    <p>Note that the working directory may be different than what
    existing CGI scripts expect. Many implementations of CGI set
    the working directory of the subprocess to be the directory
    containing the executable file. This servlet does <i>not</i> set
    the working directory, as Java does <i> not</i> provide a means to
    set the working directory.
    Fortunately for us, the current CGI specification this is only <i>
    suggests</i> that the working directory be set (to the directory
    containing the script).</p>

    <h3>STANDARD CGI VARIABLES</h3>

    <p>This is the list of CGI 1.1 environment variables available to CGI
    programs called by CgiServlet.</p>

    <p>For reference see:</p>

    <blockquote>The WWW Common Gateway Interface Version 1.1
    <br><a href="http://web.golux.com/coar/cgi/draft-coar-cgi-v11-03-clean.html">
    http://web.golux.com/coar/cgi/draft-coar-cgi-v11-03-clean.html</a></blockquote>

    <p>NOTE: Should CGI variables listed below are not provided by
    CgiServlet (and are documented as such).</p>

    <h3>CGI 1.1 VARIABLES</h3>

    <dl>
    <dt>AUTH_TYPE <i>(recommended)</i></dt>

    <dd>Contains the type of authentication being used to limit access to
    the current document. E.g. "Basic" or "Digest".</dd>

    <dt>CONTENT_LENGTH <i>(required)</i></dt>

    <dd>Contains the length of information provided by a client POST or
    PUT.</dd>

    <dt>CONTENT_TYPE <i>(required)</i></dt>

    <dd>For queries that have attached information, such as HTTP POST and
    PUT, this is the content type of the data.</dd>

    <dt>GATEWAY_INTERFACE <i>(required)</i></dt>

    <dd>The revision of the CGI specification to which this server
    complies. Contains "CGI/1.1".</dd>

    <dt>PATH_INFO <i>(required)</i></dt>

    <dd>The extra path information, as given by the client. Scripts can be
    accessed by their virtual pathname, followed by extra information at
    the end of this path. The extra information is sent as PATH_INFO.</dd>

    <dt>PATH_TRANSLATED <i>(optional)</i></dt>

    <dd>The server provides a translated version of PATH_INFO, which takes
    the path and does any virtual-to-physical mapping to it.</dd>

    <dt>QUERY_STRING <i>(required)</i></dt>

    <dd>The query information that follows the ? in the URL that referenced
    this script.</dd>

    <dt>REMOTE_ADDR <i>(required)</i></dt>

    <dd>The IP address of the remote host making the request.</dd>

    <dt>REMOTE_HOST <i>(recommended)</i></dt>

    <dd>The hostname making the request.
    <br>
    <i>The DNS lookup to map a host address to host name can be quite
    expensive. Turned off by default.</i></dd>

    <dt>REMOTE_IDENT <i>(optional)</i></dt>

    <dd>If the HTTP server supports RFC 931 identification, this variable
    is set to the remote username retrieved from the server. Use this
    variable for logging only.
    <br>
    <i>Not currently supported</i></dd>

    <dt>REMOTE_USER <i>(optional)</i></dt>

    <dd>If the server supports user authentication and the script is
    protected, this is the username they have authenticated as.</dd>

    <dt>REQUEST_METHOD <i>(required)</i></dt>

    <dd>The method with which the request was made. For HTTP, this is GET,
    HEAD, POST, and so on.</dd>

    <dt>SCRIPT_NAME <i>(required)</i></dt>

    <dd>Virtual path to the script being executed (for self-referencing
    URLs).</dd>

    <dt>SERVER_NAME <i>(required)</i></dt>

    <dd>The server's hostname, DNS alias, or IP address as it appears in
    self-referencing URLs.</dd>

    <dt>SERVER_PORT <i>(required)</i></dt>

    <dd>The port number to which the request was sent.</dd>

    <dt>SERVER_PROTOCOL <i>(required)</i></dt>

    <dd>The name and revision of the protocol used for the request. Format:
    protocol/revision.</dd>

    <dt>SERVER_SOFTWARE <i>(required)</i></dt>

    <dd>The name and version of the information server software answering
    the request (and running the gateway). Format: name/version.</dd>
    </dl>

    <h3>CGI 1.2 variables</h3>

    <dl>
    <dt>HTTP_ACCEPT</dt>

    <dd>Contains the contents of any "Accept:" headers supplied by the
    client (the list of MIME formats that the browser can accept).</dd>

    <dt>HTTP_ACCEPT_CHARSET</dt>

    <dd>Contains the contents of any "Accept-Charset:" headers supplied by
    the client.</dd>

    <dt>HTTP_ACCEPT_ENCODING</dt>

    <dd>Contains the contents of any "Accept-Encoding:" headers supplied by
    the client.</dd>

    <dt>HTTP_ACCEPT_LANGUAGE</dt>

    <dd>Contains the contents of any "Accept-Language:" headers supplied by
    the client.</dd>

    <dt>HTTP_COOKIE</dt>

    <dd>Contains the contents of any "Cookie:" header supplied by the
    client.</dd>

    <dt>HTTP_FROM</dt>

    <dd>Contains the contents of any "From:" header supplied by the client.
    This may contain the e-mail address of the client user. This is
    generally unreliable, as usually users choose not to supply this
    information. If they do give it they can choose any e-mail address they
    want -- there is no guarantee that this is, in fact, the real e-mail
    address of the client user.</dd>

    <dt>HTTP_FORWARDED</dt>

    <dd>Contains the contents of any "Forwarded:" header supplied by the
    client.</dd>

    <dt>HTTP_HOST</dt>

    <dd>Contains the contents of the "Host:" header supplied by the client.
    This should contain the one of the aliases for the host on which the
    server is running. It should be the hostname from the URL that the
    client is requesting. Thus a client seeking
    "http://www.serverhost.com:8000/foo.html" should supply
    "www.serverhost.com" in this header. Many browsers do not do this. It
    is required in HTTP/1.1.</dd>

    <dt>HTTP_PRAGMA</dt>

    <dd>Contains the contents of any "Pragma:" header supplied by the
    client.</dd>

    <dt>HTTP_PROXY_AUTHORIZATION</dt>

    <dd>Contains the contents of any "Proxy-Authorization:" header supplied
    by the client.</dd>

    <dt>HTTP_RANGE</dt>

    <dd>Contains the contents of any "Range:" header supplied by the
    client.</dd>

    <dt>HTTP_REFERER</dt>

    <dd>Contains the contents of any "Referer:" header supplied by the
    client. The referring document that linked to this page or submitted
    form data.</dd>

    <dt>HTTP_USER_AGENT</dt>

    <dd>Contains the contents of any "User-Agent:" header supplied by the
    client. The name and version of the user's browser.</dd>
    </dl>

    <h3>CGI common extensions</h3>

    <dl>
    <dt>AUTH_USER</dt>

    <dd>Same as REMOTE_USER.</dd>

    <dt>DOCUMENT_ROOT</dt>

    <dd>Contains the absolute path to your web server root data
    directory.</dd>

    <dt>REQUEST_URI</dt>

    <dd>Same as SCRIPT_NAME??</dd>

    <dt>SCRIPT_FILENAME</dt>

    <dd>Same as REQUEST_URI?
    The name of the CGI program being executed and its path relative to
    the system root.</dd>

    <dt>URL_SCHEME</dt>

    <dd>Contains "http" normally or "https" in case the server has been
    modified to use the Secure Sockets Layer (SSL) protocol.
    WN_DIR_PATH</dd>
    </dl>
*/

public class CgiServlet extends HttpServlet
{
    // Feature: read the existing environment variables.
    private static final boolean bReadEnvironment = true;

    // Feature: how the CGI variables are passed to the CGI script.
    // If false the CGI variables are written to the CGI input stream.
    // If true the CGI variables *replace* the environment (no inheritance).
    private static final boolean bReplaceEnvironment = true;

    // Feature: pass all or some HTTP headers to CGI.
    private static final boolean bPassAllHeaders = false;
    private static final boolean bPassSomeHeaders = true;
    private static final boolean bSupportCommonExtensions = false;

    // Feature: support for REMOTE_HOST name lookup (expensive).
    private static final boolean bRemoteHostLookup = false;

    // Apply a standard extension to all CGI script names.
    protected String extension = "";

    // Use a standard shell to invoke all CGI scripts (if supplied).
    protected String runner = null;

    // Character encoding to use write to and read from CGI.
    protected String encoding = System.getProperty("file.encoding");

    // Base environment (read once) to be merged with CGI variables.
    private  EnvironmentVector baseEnvironment;

    //
    // The search for executables can be expensive,
    // so keep a cache of path -> executable mappings.
    //
    // Cache misses (as null strings) as well as hits.
    //

    private Properties cache = new Properties();

    /**
     * Flush all the cached path -> executable mappings,
     * This is primarily useful during development when things change.
     */

    protected void flushCache() {
        cache = new Properties();
    }

    /**
     * Find a path -> executable CGI script mapping.
     * This whole operation is rather expensive,
     * so cache the result (good or bad).
     * <p>
     * We don't support CGI scripts in subdirectories of the root
     * CGI directory as this gets expensive rather quickly.
     *
     * @param scriptName what executable to look for
     * @exception ServletException when nothing found
     */

    protected String findExecutable(String scriptName) throws ServletException {
        String pathlist = getInitParameter("Roots");

        // Slap on a standard extension (if any).
        scriptName += extension;

        // Check for a cached association.
        String scriptFilename = cache.getProperty(scriptName);
        if (null != scriptFilename) {
            // We have looked for this mapping before.
            if (0 == scriptFilename.length()) {
                // We did not find a mapping before.
                throw new ServletException("No CGI for \"" + scriptName + "\"");
            }
            // We already have a mapping for this scriptName.
            return scriptFilename;
        }

        // Trap hack attempts to use relative uplinks.
        if ((0 <= scriptName.indexOf("/../")) || scriptName.endsWith("/..")) {
            cache.put(scriptName,""); // remember this bogus lookup
            throw new ServletException("Bogus CGI \"" + scriptName + "\"");
        }

        // Adjust from HTTP path to native path separators.
        String pathRelative = scriptName.replace('/',File.separatorChar);

        // Try each "root" directory in turn, looking for the executable.
        StringTokenizer roots = new StringTokenizer(pathlist,";");
        while (roots.hasMoreTokens()) {
            String root = roots.nextToken().replace('/',File.separatorChar);
            // log("Try ROOT \"" + root + "\" NAME \"" + pathRelative + "\"");
            File fileExecutable = new File(root,pathRelative);
            // log("Try CGI "+fileExecutable);
            if (!fileExecutable.isFile()) {
                continue;
            }
            // No extra path beyond the script name (exact match)!
            //
            scriptFilename = fileExecutable.getAbsolutePath();
            cache.put(scriptName,scriptFilename); // remember this lookup.
            // log("Use CGI "+scriptFilename);
            return scriptFilename;
        }

        // We fail if we get here.
        cache.put(scriptName,""); // remember this failed lookup
        throw new ServletException("Nothing found for CGI \"" + scriptName + "\"");
    }

    //
    //  Specialized Vector for building up an environment string array.
    //

    static final class EnvironmentVector extends java.util.Vector {
        final void setenv(String name,String value) {
            if ((null == value) || (0 == value.length())) {
                return;
            }
            addElement(name + "=" + value);
        }
        final void setenv(String name,String value,boolean use) {
            if (use) {
                setenv(name,value);
            }
        }
        final String[] toStringArray() {
            String[] env = new String[size()];
            copyInto(env);
            return env;
        }
        final StringBuffer toStringBuffer() {
            StringBuffer sb = new StringBuffer();
            int n = size();
            for (int i = 0; i < n; ++i) {
                String s = (String) elementAt(i);
                sb.append(s);
                sb.append("\n");
            }
            sb.append("\n");
            return sb;
        }
    };

    //
    //  Build up the CGI environment.
    //

    protected EnvironmentVector buildEnvironment(
        HttpServletRequest request,
        String scriptFilename,
        String scriptName,
        String pathInfo
        )
    {
        EnvironmentVector ev = (null == baseEnvironment) ? new EnvironmentVector() : (EnvironmentVector) baseEnvironment.clone();

        // -----------------
        // CGI 1.1 variables
        // -----------------

        ev.setenv("AUTH_TYPE"           ,request.getAuthType());
        ev.setenv("CONTENT_LENGTH"      ,""+request.getContentLength(),(0 <= request.getContentLength()));
        ev.setenv("CONTENT_TYPE"        ,request.getContentType());
        ev.setenv("GATEWAY_INTERFACE"   ,"CGI/1.1");
        ev.setenv("PATH_INFO"           ,pathInfo);
        ev.setenv("PATH_TRANSLATED"     ,request.getRealPath(pathInfo));
        ev.setenv("QUERY_STRING"        ,request.getQueryString());
        ev.setenv("REMOTE_ADDR"         ,request.getRemoteAddr());
        ev.setenv("REMOTE_USER"         ,request.getRemoteUser());
        ev.setenv("REQUEST_METHOD"      ,request.getMethod());
        ev.setenv("SCRIPT_NAME"         ,scriptName);
        ev.setenv("SERVER_NAME"         ,request.getServerName());
        ev.setenv("SERVER_PORT"         ,""+request.getServerPort());
        ev.setenv("SERVER_PROTOCOL"     ,request.getProtocol());
        ev.setenv("SERVER_SOFTWARE"     ,getServletContext().getServerInfo());

        // The DNS lookup to map a host address to host name
        // can be quite expensive.  Turn off by default.
        if (bRemoteHostLookup) {
            ev.setenv("REMOTE_HOST"     ,request.getRemoteHost());
        }

        // TBD - where might this come from??
        // ev.setenv("REMOTE_IDENT"        ,?????????????);

        // -----------------
        // CGI 1.2 variables
        // -----------------

        if (bPassAllHeaders) {
            // Brute force interpretation of CGI 1.2.
            // We MAY pass all request headers from the HTTP request
            // on to CGI, with the head names prefixed with "HTTP_",
            // uppercased, and with '-' replaced with '_'.
            // IMHO this is NOT a great idea.
            Enumeration e = request.getHeaderNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                String value = request.getHeader(name);
                name = "HTTP_" + name.replace('-','_').toUpperCase();
                ev.setenv(name,value);
            }
        }
        if (bPassSomeHeaders) {
            // Selective inclusion of HTTP headers.
            ev.setenv("HTTP_ACCEPT"                 ,request.getHeader("Accept"));
            ev.setenv("HTTP_ACCEPT_CHARSET"         ,request.getHeader("Accept-Charset"));
            ev.setenv("HTTP_ACCEPT_ENCODING"        ,request.getHeader("Accept-Encoding"));
            ev.setenv("HTTP_ACCEPT_LANGUAGE"        ,request.getHeader("Accept-Language"));
            ev.setenv("HTTP_COOKIE"                 ,request.getHeader("Cookie"));
            ev.setenv("HTTP_FROM"                   ,request.getHeader("From"));
            ev.setenv("HTTP_FORWARDED"              ,request.getHeader("Forwarded"));
            ev.setenv("HTTP_HOST"                   ,request.getHeader("Host"));
            ev.setenv("HTTP_PRAGMA"                 ,request.getHeader("Pragma"));
            ev.setenv("HTTP_PROXY_AUTHORIZATION"    ,request.getHeader("Proxy-authorization"));
            ev.setenv("HTTP_RANGE"                  ,request.getHeader("Range"));
            ev.setenv("HTTP_REFERER"                ,request.getHeader("Referer"));
            ev.setenv("HTTP_USER_AGENT"             ,request.getHeader("User-Agent"));
        }

        // ---------------------
        // CGI common extensions
        // ---------------------

        if (bSupportCommonExtensions) {
            // Other variables in common(?) use.
            ev.setenv("AUTH_USER"           ,request.getRemoteUser());
            ev.setenv("DOCUMENT_ROOT"       ,request.getRealPath("/"));
            ev.setenv("REQUEST_URI"         ,request.getRequestURI());
            ev.setenv("SCRIPT_FILENAME"     ,scriptFilename);
            ev.setenv("URL_SCHEME"          ,request.getScheme());
            ev.setenv("HTTPS"               ,"ON","https".equals(request.getScheme()));
        }

        return ev;
    }

    //
    //  Helper thread to write anything needed to CGI async.
    //

    protected final static class CgiWriter implements Runnable
    {
        EnvironmentVector environment;
        InputStream isContent;
        int cbContent;
        String encodingContent;
        Process processCGI;
        String encodingCGI;

        public void setup(
            EnvironmentVector environment,
            HttpServletRequest request,
            Process process,
            String encoding
            ) throws IOException, UnsupportedEncodingException
        {
            this.environment = environment;
            this.isContent = request.getInputStream();
            this.cbContent = request.getContentLength();
            this.encodingContent = request.getCharacterEncoding();
            this.processCGI = process;
            this.encodingCGI = encoding;
        }

        public void run() {
            try {
                OutputStream os = processCGI.getOutputStream();
                BufferedWriter writer;
                try {
                    OutputStreamWriter osw = new OutputStreamWriter(os,encodingCGI);
                    writer = new BufferedWriter(osw);
                } catch (UnsupportedEncodingException e) {
                    os.close();
                    processCGI.destroy();
                    return;
                }
                try {
                    if (!bReplaceEnvironment) {
                        String buffer = environment.toStringBuffer().toString();
                        writer.write(buffer);
                    }
                    if (0 < cbContent) {
                        byte[] buffer = new byte[cbContent];
                        isContent.read(buffer);
                        String s = new String(buffer,encodingContent);
                        writer.write(s);
                    }
                } finally {
                    try {
                        int exitValue = processCGI.waitFor();
                        // TBD what do we want to do with non-zero exits?
                    } catch (InterruptedException e) {
                        e.printStackTrace();    // DEBUG
                    }
                    try { writer.close(); } catch (IOException e) {/*ignore*/}
                }
            } catch (IOException e) {
                e.printStackTrace();    // DEBUG
                processCGI.destroy();
            }
        }
    }

    //
    //  Read any HTTP headers from the CGI output.
    //

    protected Properties readHeaders(BufferedReader reader) throws IOException {
        Properties headers = new Properties();
        String name = "BOGUS";
        for (;;) {
            String line = reader.readLine();
            if (0 == line.length()) {
                break;
            }
            int i = line.indexOf(':');
            if (0 < i) {
                name = line.substring(0,i).trim();
                String value = line.substring(i+1).trim();
                headers.put(name,value);
            } else {
                // We have a continuation value(?)
                String value = headers.getProperty(name,"") + " " + line.trim();
                headers.put(name,value);
            }
        }
        return headers;
    }

    //
    //  Write and interpret headers into the response.
    //

    protected void setResponseHeaders(
        Properties headers,
        HttpServletResponse response
        ) throws ServletException
    {
        Enumeration e = headers.keys();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = headers.getProperty(name);

            // Special handling for the "Status" header.
            if (name.equalsIgnoreCase("Status")) {
                StringTokenizer st = new StringTokenizer(value);
                try {
                    switch(st.countTokens()) {
                    case 1:
                        response.setStatus(Integer.parseInt(st.nextToken()));
                        break;
                    case 2:
                        response.setStatus(Integer.parseInt(st.nextToken()),st.nextToken("\n"));
                        break;
                    }
                } catch ( NumberFormatException ignore ) {
                    // ignore a malformed header
                }
                continue;
            }

            // Special handling for the "Location" header.
            if (name.equalsIgnoreCase("Location")) {
                response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                response.setHeader(name,value);
                continue;
            }

            // Special handling for the "Content-type" header.
            if (name.equalsIgnoreCase("Content-type")) {
                response.setContentType( value );
                continue;
            }

            // Special handling for the "Content-length" header.
            if (name.equalsIgnoreCase("Content-length")) {
                try {
                    response.setContentLength(Integer.parseInt(value));
                } catch ( NumberFormatException ignore ) {
                    // ignore a malformed header
                }
                continue;
            }

            // No special handling, so just copy the header.
            response.setHeader(name,value);
        }
    }

    //
    //  Copy remaining CGI output into the response.
    //

    protected void copyContent(
        BufferedReader reader,
        HttpServletResponse response
        ) throws ServletException, IOException
    {
        PrintWriter writer = response.getWriter();
        for (;;) {
            String line = reader.readLine();
            if (null == line) {
                break;
            }
            writer.println(line);
        }

        // There are all sorts of issues here...
        // ... what to do with binary content
        // ... what to do when the character encoding changes the length
        // ... what to do about multi-part responses (and encodings)
        // ... how can big outputs be copied more efficiently
        // Pragmatically we likely can ignore most or all of the above.
    }

    //
    //  Read the output from CGI and assemble response.
    //

    protected void readResponse(
        InputStream is,
        HttpServletResponse response
        ) throws ServletException, IOException
    {
        // First the we need to treat the CGI output as characters.
        BufferedReader reader = new BufferedReader(new InputStreamReader(is,encoding));
        try {
            Properties headers = readHeaders(reader);
            setResponseHeaders(headers,response);
            copyContent(reader,response);
        } finally {
            reader.close();
        }
    }

    /**
     * Service a CGI request.
     */

    public void service(
        HttpServletRequest request,
        HttpServletResponse response
        ) throws ServletException, IOException
    {
        // The SCRIPT_NAME is the first node in the URI past the servlet path.
        // I.e. "/dump: in "http://server/cgi/dump/foo"
        // We don't attempt to handle scripts in subdirectories.

        String scriptName = request.getPathInfo();
        String pathInfo = "";

        int iExtra = scriptName.indexOf('/',1);
        if (0 < iExtra) {
            // We have extra path beyond the script name
            pathInfo = scriptName.substring(iExtra);
            scriptName = scriptName.substring(iExtra);
        }

        // Map the script name to the actual script file name.
        String scriptFilename = findExecutable(scriptName);

        // Add the runner (if any) to get the command to run.
        String command = ((0 < runner.length()) ? (runner + " " + scriptFilename) : scriptFilename);

        // Build the CGI script environment.
        EnvironmentVector environment = buildEnvironment(request,scriptName,scriptFilename,pathInfo);

        log("EXEC: " + command);
        Process process;
        if (bReplaceEnvironment) {
            process = Runtime.getRuntime().exec(command,environment.toStringArray());
        } else {
            process = Runtime.getRuntime().exec(command);
        }

        // Write any input to the CGI script is (async).
        CgiWriter writer = new CgiWriter();
        writer.setup(environment,request,process,encoding);
        new Thread(writer).start();

        // Read CGI output and assemble our response.
        try {
            readResponse(process.getInputStream(),response);
        } catch (IOException e) {
            // browser has closed its input stream - we should
            // terminate script and clean up...
            e.printStackTrace();    // DEBUG
            log("Client closed connection!");
            process.destroy();
        }

        // think about this a bit more :
        // what should we do with our processes error stream ?
        // what should we do with it's exit value ?
    }

    /**
     * Read the environment variables (hopefully) output by the given command.
     */

    protected EnvironmentVector readEnvironment(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),encoding));
            EnvironmentVector env = new EnvironmentVector();
            try {
                for (;;) {
                    String line = reader.readLine();
                    if (null == line) {
                        break;
                    }
                    // make sure we're looking at name=value
                    int i = line.indexOf('=');
                    if (0 < i) {
                        env.addElement(line);
                    }
                }
            } finally {
                reader.close();
            }
            int exitValue = process.waitFor();
            log("EXIT(" + exitValue + ") FROM readEnvironment: " + command);
            return env;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Windows specific initialization.
     * <p>
     * On Windows we don't really want to use the default command
     * interpreter (cmd.exe or command.exe) as it is simply too
     * brain-dead (at least compared to the Bourne shell).
     * <p>
     * "Windows Scripting Host" is a rather nice alternative that
     * is installed on all current versions of Windows, is a free
     * download for older versions of Windows, and is quite powerful.
     * <p>
     * It might even be enough to get a Windows logo :).
     */

    protected void initOnWindows(ServletConfig config) throws ServletException {
        runner = "cscript //nologo";
        extension = ".wsf";
        // Read the environment (once) for later merge.
        if (bReadEnvironment) {
            baseEnvironment = readEnvironment("cmd /c set");  // not going to work on Win95/98/ME?
        }
    }

    /**
     * Unix specific initialization.
     * <p>The Bourne shell (/bin/sh) is a natural default choice here.
     */

    protected void initOnUnix(ServletConfig config) throws ServletException {
        runner = "/bin/sh";
        extension = ".sh";
        // Read the environment (once) for later merge.
        if (bReadEnvironment) {
            baseEnvironment = readEnvironment("/bin/sh -c set");
        }
    }

    /**
     * One time initialization of servlet.
     * This is where we decide on a platform-specific stategy.
     * Might want to allow the configuration to override.
     * <p>
     * Servlet initialization parameters:
     * <dl>
     * <dt>Runner
     * <dd>The program used to run CGI scripts.
     *     Default on Windows is "csript.exe //nologo".
     *     Default elsewhere is "/bin/sh".
     *     If set an empty string the CGI script is run directly.
     * <dt>Extension
     * <dd>The file extension required of CGI scripts.
     *     Default on Windows is ".wsf".
     *     Default on Unix is ".sh".
     * <dt>Encoding
     * <dd>The character encoding to use when writing to and reading
     *     from a CGI process.  Default is the same as the System
     *     property "file.encoding".  Essential on non-ASCII systems.
     * </dl>
     */

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Need the encoding so we can read the environment.
        String v = config.getInitParameter("Encoding");
        if (null != v) {
            encoding = v;
        }

        // Pick up the platform specific defaults.
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            initOnWindows(config);
        } else {
            initOnUnix(config);     // an optimistic assumption
        }

        // Override the platform specific defaults if explicitly configured.
        v = config.getInitParameter("Runner");
        if (null != v) {
            runner = v;
        }
        v = config.getInitParameter("Extension");
        if (null != v) {
            extension = v;
        }
    }
};
