// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

/* ------------------------------------------------------------ */
/** IP gateway.
 * Forwards IP connections to another IP:port address and copies
 * all data received in both directions.  Suitable for opening up
 * a specific port through a firewall.
 *
 * <p><h4>Notes</h4>
 * A property file can be used to specify multiple addresses
 * for forwarding. See the example propertyFile.prp file for
 * details.
 *
 * <p><h4>Usage</h4>
 * <PRE>
 * java com.mortbay.Util.InetGateway [-dump|-summary] [LocalHost:]LocalPort ForwardHost:ForwardPort
 * java com.mortbay.Util.InetGateway propertyFile.prp
 * </pre>
 *
 * @version $Id$
 * @author Greg Wilkins (inspired by M Watson and J Gosnell)
 */
public class InetGateway extends ThreadedServer
{
    /* ------------------------------------------------------------ */
    private static final Class[] __osArg1 = 
    {
        java.io.OutputStream.class 
    };
    private static final Class[] __osArg2 = 
    {
        java.io.OutputStream.class,java.lang.String.class
    };
    private static final String[] __dumpFilter = 
    {
        "com.mortbay.Util.DumpFilterOutputStream"
    };
    private static final String[] __summaryFilter = 
    {
        "com.mortbay.Util.SummaryFilterOutputStream"
    };
    
    /* ------------------------------------------------------------ */
    InetAddrPort _local;
    InetAddrPort _remote;
    Vector _l2rConstructors=new Vector();
    Vector _r2lConstructors=new Vector();

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param local Listen address and port.
     * @param remote Forward address and port.
     * @exception IOException Problem listening on local port.
     */
    public InetGateway (InetAddrPort local,
                        InetAddrPort remote)
        throws IOException
    {
        this(local,remote,null,null);
    }
    
               
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param local Listen address and port.
     * @param remote Forward address and port.
     * @param l2rFilters Array of class names of FilterOutputStream derived
     *                   classes used to filter data from local to remote.
     * @param r2lFilters Array of class names of FilterOutputStream derived
     *                   classes used to filter data from remote to remote.
     * @exception IOException  Problem listening on local port.
     */
    public InetGateway (InetAddrPort local,
                        InetAddrPort remote,
                        String[] l2rFilters,
                        String[] r2lFilters)
        throws IOException
    {
        super(local);
        if (remote.getInetAddress()==null)
            throw new IOException("Remote IP address not specified");
        _local=local;
        _remote=remote;

        String filter=null;
        
        try
        {
            // Get local to remote filter classes
            if (l2rFilters!=null)
            {
                for (int f=l2rFilters.length;f-->0;)
                {
                    filter=(String)l2rFilters[f];
                    Class c = Class.forName(filter);
                    if(!java.io.FilterOutputStream.class.isAssignableFrom(c))
                        throw new IOException("Filter "+ filter +
                                              "is not a FilterOutputStream");

                    Constructor con=null;
                    try
                    {
                        con=c.getConstructor(__osArg2);
                    }
                    catch(Exception e)
                    {
                        con=c.getConstructor(__osArg1);
                    }
                    _l2rConstructors.addElement(con);
                }
            }
        
            // Get remote to local filter classes
            if (r2lFilters!=null)
            {
                for (int f=r2lFilters.length;f-->0;)
                {
                    filter=(String)r2lFilters[f];
                    Class c = Class.forName(filter);
                    if(!java.io.FilterOutputStream.class.isAssignableFrom(c))
                        throw new IOException("Filter "+filter+
                                              " is not a FilterOutputStream");
                    Constructor con=null;
                    try
                    {
                        con=c.getConstructor(__osArg2);
                    }
                    catch(Exception e)
                    {
                        con=c.getConstructor(__osArg1);
                    }
                    _r2lConstructors.addElement(con);
                }
            }
        }
        catch(ClassNotFoundException e)
        {
            Code.debug(e);
            throw new IOException("Invalid filter class "+filter);
        }
        catch(NoSuchMethodException e)
        {
            Code.debug(e);
            throw new IOException("Invalid filter class "+filter);
        }
        catch(SecurityException e)
        {
            Code.debug(e);
            throw new IOException("Invalid filter class "+filter);
        }
    }
    
    /* ------------------------------------------------------------------- */
    protected void handleConnection(InputStream in,OutputStream out)
    {
        Code.debug("Handle InetGateway connection");
        try
        {
            // Create a socket to the remote address
            final Socket socket = new Socket(_remote.getInetAddress(),
                                             _remote.getPort());
            
            // Install remote to local filters
            for (int f=0;f<_r2lConstructors.size();f++)
            {
                Constructor constructor=
                    (Constructor)_r2lConstructors.elementAt(f);
                
                Object[] arg =
                    new Object[constructor.getParameterTypes().length];
                
                arg[0]=out;
                if (arg.length==2)
                    arg[1]=_local+" <<-- "+_remote;
                
                out=(OutputStream) constructor.newInstance(arg);
            }
            final InputStream remoteIn=socket.getInputStream();
            final OutputStream localOut=out;
            
            // Install local to remote filters
            out = socket.getOutputStream();
            for (int f=0;f<_l2rConstructors.size();f++)
            {
                Constructor constructor=
                    (Constructor)_l2rConstructors.elementAt(f);
                Object[] arg =
                    new Object[constructor.getParameterTypes().length];
                arg[0]=out;
                if (arg.length==2)
                    arg[1]=_local+" -->> "+_remote;
                
                out=(OutputStream) constructor.newInstance(arg);
            }
            final InputStream localIn=in;
            final OutputStream remoteOut=out;

            try
            {
                // Create new thread to copy remote in to local out
                new Thread(new Runnable(){
                    public void run(){
                        try { IO.copy(remoteIn,localOut); }
                        catch(Exception e)
                        { if (Code.verbose())Code.debug(e); }
                        finally
                        {
                            try { localOut.close() ; }
                            catch(Exception e) { Code.ignore(e); }
                        }       
                    }
                    },Thread.currentThread().getName()+"+").start();

                // Use this thread to copy local in to remote out
                IO.copy(localIn,remoteOut);
            }
            catch(Exception e)
            {
                if (Code.verbose())
                    Code.ignore(e);
            }
            finally
            {
                try { remoteOut.close() ; }
                catch(Exception e) { Code.ignore(e); }
            }    
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        finally
        {
            System.err.println("COMPLETE: "+Thread.currentThread().toString());
        }
    }

    
    /* ------------------------------------------------------------ */
    public static void usage()
    {
        System.err.println("Usage - ");
        System.err.println("  java com.mortbay.Util.InetGateway [-dump|-summary] [LocalHost:]LocalPort ForwardHost:ForwardPort");
        System.err.println("  java com.mortbay.Util.InetGateway propertyFile.prp");
    }
    
    /* ------------------------------------------------------------ */
    public static void runGateways(String propFile)
        throws IOException
    {
        Properties props = new Properties();
        try {
            InputStream in = new FileInputStream(propFile);
            props.load(in);
        }
        catch (IOException ex)
        {
            usage();
            System.err.println("Bad Properties File: " + propFile);
            System.err.println(ex.getClass().getName() + ": "
                               + ex.getMessage());
            return;
        }
        Enumeration keys = props.keys();
        Vector done = new Vector();

        while (keys.hasMoreElements())
        {
            String key = keys.nextElement().toString();
            int dotIndex = key.lastIndexOf(".");
            if (dotIndex < 0)
            {
                Code.warning("Bad property entry: No Index in "+key);
                continue;
            }
            String prefix = key.substring(0, dotIndex);

            if (!done.contains(prefix))
            {
                done.addElement(prefix);
                
                String local = (String)props.get(prefix+".localAddress");
                String remote = (String)props.get(prefix+".remoteAddress");
                if (local == null || remote == null)
                {
                    Code.warning("Bad property entry: Missing addresses for "+key);
                    continue;
                }
                InetAddrPort localAddr  = new InetAddrPort(local);
                InetAddrPort remoteAddr = new InetAddrPort(remote);

                // Find l2r  filter classes
                Vector local2remote=null;
                String l2r =
                    (String)props.get(prefix+".local2remote");
                if (l2r!=null)
                {
                    local2remote=new Vector();
                    StringTokenizer tok = new StringTokenizer(l2r,",    ");
                    while (tok.hasMoreTokens())
                        local2remote.addElement(tok.nextToken());
                }
                // Find r2l filter classes
                Vector remote2local=null;
                String r2l =
                    (String)props.get(prefix+".remote2local");
                if (r2l!=null)
                {
                    remote2local=new Vector();
                    StringTokenizer tok = new StringTokenizer(r2l,",    ");
                    while (tok.hasMoreTokens())
                        remote2local.addElement(tok.nextToken());
                }
                
                try
                {
                    String[] r2la = new String[local2remote.size()];
                    local2remote.copyInto(r2la);
                    String[] l2ra = new String[remote2local.size()];
                    remote2local.copyInto(r2la);
                    new InetGateway(localAddr,remoteAddr,
                                    l2ra,r2la).start();
                }
                catch (IOException e)
                {
                    throw e;
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String[] argv)
    {
        try
        {
            if (argv.length == 1)
                runGateways(argv[0]);
            else if (argv.length == 2)
                new InetGateway(new InetAddrPort(argv[0]),
                                new InetAddrPort(argv[1])).start();
            else if (argv.length == 3 && "-dump".equals(argv[0]))
                new InetGateway(new InetAddrPort(argv[1]),
                                new InetAddrPort(argv[2]),
                                __dumpFilter,__dumpFilter).start();
            else if (argv.length == 3 && "-summary".equals(argv[0]))
                new InetGateway(new InetAddrPort(argv[1]),
                                new InetAddrPort(argv[2]),
                                __summaryFilter,__summaryFilter).start();
            else
                usage();
        }
        catch (Exception e)
        {
            Code.fail(e);
        }
    }
}
