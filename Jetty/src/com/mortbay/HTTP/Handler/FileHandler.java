// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.sun.java.util.collections.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.util.*;
import java.text.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** FileHandler.
 *
 * Serves files from a single file or directory and implements
 * the GET, HEAD, DELETE, OPTIONS, MOVE methods and the
 * IfModifiedSince and IfUnmodifiedSince header fields.
 * A simple memory cache is also provided to reduce file I/O.
 *
 * @version 1.0 Mon Oct 11 1999
 * @author Greg Wilkins (gregw)
 */
public class FileHandler extends NullHandler
{
    /* ----------------------------------------------------------------- */
    private String _allowHeader = null;
    private CachedFile[] _cache=null;
    private int _nextIn=0;
    private Map _cacheMap=null;
    
    
    /* ------------------------------------------------------------ */
    boolean _dirAllowed =true;
    public boolean isDirAllowed()
    {
	return _dirAllowed;
    }
    public void setDirAllowed(boolean dirAllowed)
    {
	_dirAllowed = dirAllowed;
    }
    
    /* ------------------------------------------------------------ */
    boolean _putAllowed =false;
    public boolean isPutAllowed()
    {
	return _putAllowed;
    }
    public void setPutAllowed(boolean putAllowed)
    {
	_putAllowed = putAllowed;
    }

    /* ------------------------------------------------------------ */
    boolean _delAllowed=false;
    public boolean isDelAllowed()
    {
	return _delAllowed;
    }
    public void setDelAllowed(boolean delAllowed)
    {
	_delAllowed = delAllowed;
    }

    
    /* ------------------------------------------------------------ */
    List _indexFiles =new ArrayList(2);
    {
	_indexFiles.add("index.html");
	_indexFiles.add("index.htm");
    }
    public List getIndexFiles()
    {
	return _indexFiles;
    }
    public void setIndexFiles(List indexFiles)
    {
	_indexFiles = indexFiles;
    }

    
    /* ------------------------------------------------------------ */
    int _maxCachedFiles =64;
    public int getMaxCachedFiles()
    {
	return _maxCachedFiles;
    }
    public void setMaxCachedFiles(int maxCachedFiles_)
    {
	_maxCachedFiles = maxCachedFiles_;
    }
    
    /* ------------------------------------------------------------ */
    int _maxCachedFileSize =40960;
    public int getMaxCachedFileSize()
    {
	return _maxCachedFileSize;
    }
    public void setMaxCachedFileSize(int maxCachedFileSize)
    {
	_maxCachedFileSize = maxCachedFileSize;
    }


    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler.
     */
    public FileHandler()
    {}
    
    /* ----------------------------------------------------------------- */
    public void start()
    {
        Log.event("FileHandler started in "+getHandlerContext().getFileBase());
        if (_maxCachedFiles>0 && _maxCachedFileSize>0 && _cache==null)
        {
            _cache=new CachedFile[_maxCachedFiles];
            _cacheMap=new HashMap();
        }
        super.start();
    }
    
    /* ----------------------------------------------------------------- */
    public void stop()
    {
        super.stop();
    }
    
    /* ----------------------------------------------------------------- */
    public void destroy()
    {
        _cache=null;
        if( _cacheMap != null)
	    _cacheMap.clear();
        super.destroy();
    }

    /* ------------------------------------------------------------ */
    /** Translate path to a real file path.
     * @param pathSpec 
     * @param path 
     * @return 
     */
    public String realPath(String pathSpec, String path)
        throws IllegalArgumentException
    {
        String realpath=getHandlerContext().getFileBase();;
        if (pathSpec.startsWith("*."))
        {
            realpath+=path;
        }
        else
        {
            String info=PathMap.pathInfo(pathSpec,path);
            if (info!=null)
                realpath+=info;
        }
        
        return realpath;
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathSpec,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if (!isStarted())
            return;
        
        // Extract and check filename
        String path = request.getPath();
        if (path.indexOf("..")>=0)
            throw new HttpException(HttpResponse.__403_Forbidden);
        String filename = realPath(pathSpec,path);
        
        Code.debug("FILE=",filename,
                   "\nMETHOD=",request.getMethod());
        
        // check filename
        boolean endsWithSlash= filename.endsWith("/");
        if (endsWithSlash)
            filename = filename.substring(0,filename.length()-1);

        String method=request.getMethod();
        if (method.equals(HttpRequest.__GET) ||
            method.equals(HttpRequest.__HEAD))
            handleGet(request, response, path, filename, endsWithSlash);        
        else if (method.equals(HttpRequest.__PUT))
            handlePut(request, response, path, filename);
        else if (method.equals(HttpRequest.__DELETE))
            handleDelete(request, response, path, filename);
        else if (method.equals(HttpRequest.__OPTIONS))
            handleOptions(response);
        else if (method.equals(HttpRequest.__MOVE))
            handleMove(request, response, pathSpec, path, filename);
        else {
            Code.debug("Unknown action:"+method);
            // anything else...
            if (new File(filename).exists())
                response.sendError(response.__501_Not_Implemented);
            else
                return;
        }
    }

    /* ------------------------------------------------------------------- */
    void handleGet(HttpRequest request, HttpResponse response,
                   String path, String filename,
                   boolean endsWithSlash)
        throws IOException
    {
        Code.debug("Looking for ",filename);
        
        // Try a cache lookup
        if (_cache!=null && !endsWithSlash)
        {
            byte[] bytes=null;
            synchronized(_cacheMap)
            {
                CachedFile cachedFile=
                    (CachedFile)_cacheMap.get(filename);
                if (cachedFile!=null &&cachedFile.isValid())
                {
                    if (!checkGetHeader(request,response,cachedFile.file))
                        return;
                    bytes=cachedFile.prepare(response);
                }
            }
            if (bytes!=null)
            {
                Code.debug("Cache hit: "+filename);
                OutputStream out = response.getOutputStream();
                out.write(bytes);
                out.flush();
                return;
            }
        }
        
        // Look for it normally
        File file = new File(filename);
        if (file.exists())
        {
            // Check modified dates
            if (!checkGetHeader(request,response,file))
                return;
                
            // check if directory
            if (file.isDirectory())
            {
                if (!endsWithSlash && !path.equals("/"))
                {
                    Code.debug("Redirect to directory/");
                        
                    int port=request.getPort();
                    String q=request.getQuery();
                    if (q!=null&&q.length()==0)
                        q=null;
                    response.setField(HttpFields.__Location,
                                      "http://"+
                                      request.getHost()+
                                      (port==0?"":(":"+port))+
                                      path+"/"+
                                      (q==null?"":("?"+q)));
                    response.sendError(HttpResponse.__301_Moved_Permanently);
                    return;
                }
                    
                // See if index file exists
                boolean indexSent=false;
                for (int i=_indexFiles.size();i-->0;)
                {
                    File index = new File(filename+
                                          File.separator +
                                          _indexFiles.get(i));
                    if (index.isFile())
                    {
			// Check modified dates
			if (!checkGetHeader(request,response,index))
			    return;
                        sendFile(request,response,filename,index);
                        indexSent=true;
                        break;
                    }
                }
                
                if (!indexSent)
                    sendDirectory(request,response,file,!("/".equals(path)));
            }
            // check if it is a file
            else if (file.isFile())
            {
                if (!endsWithSlash)
                    sendFile(request,response,filename,file);
            }
            else
                // don't know what it is
                Code.warning("Unknown file type");    
        }
    }

    
    /* ------------------------------------------------------------ */
    /* Check modification date headers.
     */
    private boolean checkGetHeader(HttpRequest request,
                                   HttpResponse response,
                                   File file)
        throws IOException
    {
        if (!request.getMethod().equals(HttpRequest.__HEAD))
        {
            // check any modified headers.
            long date=0;
            if ((date=request.getDateField(HttpFields.__IfModifiedSince))>0)
            {
                if (file.lastModified() <= date)
                {
                    response.sendError(response.__304_Not_Modified);
                    return false;
                }
            }
            
            if ((date=request.
                 getDateField(HttpFields.__IfUnmodifiedSince))>0)
            {
                if (file.lastModified() > date)
                {
                    response.sendError(response.__412_Precondition_Failed);
                    return false;
                }
            }
        }
        return true;
    }
    
            
    /* ------------------------------------------------------------ */
    void handlePut(HttpRequest request, HttpResponse response,
                   String path, String filename)
        throws IOException
    {
        Code.debug("PUT ",path," in ",filename);

        if (!_putAllowed)
            return;
        
        try
        {
            int toRead = request.getIntField(HttpFields.__ContentLength);
            InputStream in = request.getInputStream();
            FileOutputStream fos = new FileOutputStream(filename);
            final int bufSize = 1024;
            byte bytes[] = new byte[bufSize];
            int read;
            Code.debug(HttpFields.__ContentLength+"="+toRead);
            while (toRead > 0 &&
                   (read = in.read(bytes, 0,
                                   (toRead>bufSize?bufSize:toRead))) > 0)
            {
                toRead -= read;
                fos.write(bytes, 0, read);
                Code.debug("Read " + read + "bytes: " + bytes);
            }
            in.close();
            fos.close();
            request.setHandled(true);
            response.sendError(response.__204_No_Content);
        }
        catch (SecurityException sex)
        {
            Code.warning(sex);
            response.sendError(response.__403_Forbidden,
                               sex.getMessage());
        }
        catch (Exception ex)
        {
            Code.warning(ex);
        }
    }

    /* ------------------------------------------------------------ */
    void handleDelete(HttpRequest request, HttpResponse response,
                      String path, String filename)
        throws IOException
    {
        Code.debug("DELETE ",path," from ",filename);    
        
        File file = new File(filename);

        if (!file.exists())
            return;

        if (!_delAllowed)
        {
            setAllowHeader(response);
            response.sendError(response.__405_Method_Not_Allowed);
            return;
        }
            
        try
        {
            // delete the file
            file.delete();
            
            // flush the cache
            if (_cacheMap!=null)
            {
                CachedFile cachedFile=(CachedFile)_cacheMap.get(filename);
                if (cachedFile!=null)
                    cachedFile.flush();
            }
            
            // Send response
            request.setHandled(true);
            response.sendError(response.__204_No_Content);
        }
        catch (SecurityException sex)
        {
            Code.warning(sex);
            response.sendError(response.__403_Forbidden, sex.getMessage());
        }
    }

    
    /* ------------------------------------------------------------ */
    void handleMove(HttpRequest request, HttpResponse response,
                    String pathSpec, String path, String filename)
        throws IOException
    {
        if (!_delAllowed || !_putAllowed)
            return;
        
        File file = new File(filename);
        if (!file.exists())
        {
            if (_delAllowed && _putAllowed)
                response.sendError(response.__404_Not_Found);
            return;
        }

        if (!_delAllowed || !_putAllowed)
        {
            setAllowHeader(response);
            response.sendError(response.__405_Method_Not_Allowed);
            return;
        }
        
        String newPath = request.getField("New-uri");
        if (newPath.indexOf("..")>=0)
        {
            response.sendError(response.__405_Method_Not_Allowed,
                               "File contains ..");
            return;
        }

        // Find path
        try
        {    
            String newFilename = realPath(pathSpec,newPath);
            File newFile = new File(newFilename);
            Code.debug("Moving "+filename+" to "+newFilename);
            file.renameTo(newFile);
            request.setHandled(true);
            response.sendError(response.__204_No_Content);
        }
        catch (Exception ex)
        {
            Code.warning(ex);
            setAllowHeader(response);
            response.sendError(response.__405_Method_Not_Allowed,
                               "Error:"+ex);
            return;
        }
    }
    
    /* ------------------------------------------------------------ */
    void handleOptions(HttpResponse response)
        throws IOException
    {
        setAllowHeader(response);
        response.commit();
    }
    
    /* ------------------------------------------------------------ */
    void setAllowHeader(HttpResponse response)
    {
        if (_allowHeader == null)
        {
            StringBuffer sb = new StringBuffer(128);
            sb.append(HttpRequest.__GET);
            sb.append(", ");
            sb.append(HttpRequest.__HEAD);
            if (_putAllowed){
                sb.append(", ");
                sb.append(HttpRequest.__PUT);
            }
            if (_delAllowed){
                sb.append(", ");
                sb.append(HttpRequest.__DELETE);
            }
            if (_putAllowed && _delAllowed)
            {
                sb.append(", ");
                sb.append(HttpRequest.__MOVE);
            }
            sb.append(", ");
            sb.append(HttpRequest.__OPTIONS);
            _allowHeader = sb.toString();
        }
        response.setField(HttpFields.__Allow, _allowHeader);
    }
    
    /* ------------------------------------------------------------ */
    void sendFile(HttpRequest request,
                  HttpResponse response,
                  String filename,
                  File file)
        throws IOException
    {
        Code.debug("sendFile: ",file.getAbsolutePath());

        // Can the file be cached?
        if (_cache!=null && file.length()<_maxCachedFileSize)
        {
            byte[] bytes=null;
            synchronized (_cacheMap)
            {
                CachedFile cachedFile=_cache[_nextIn];
                if (cachedFile==null)
                    cachedFile=_cache[_nextIn]=new CachedFile();
                _nextIn=(_nextIn+1)%_cache.length;
                cachedFile.flush();
                cachedFile.load(filename,file);
                _cacheMap.put(filename,cachedFile);
                bytes=cachedFile.prepare(response);
            }
            if (bytes!=null)
            {
                OutputStream out = response.getOutputStream();
                out.write(bytes);
                request.setHandled(true);
                return;
            }
        }
        else
        {
            InputStream in=null;
            int len=0;
            String encoding=getHandlerContext().getMimeByExtension(file.getName());
            response.setField(HttpFields.__ContentType,encoding);
            len = (int)file.length();
            response.setIntField(HttpFields.__ContentLength,len);
            
            response.setDateField(HttpFields.__LastModified,
                                  file.lastModified());
            in = new FileInputStream(file);

            try
            {
                response.getOutputStream().write(in,len);
            }
            finally
            {
                request.setHandled(true);
                in.close();
            }
        }
        
    }


    /* ------------------------------------------------------------------- */
    void sendDirectory(HttpRequest request,
                       HttpResponse response,
                       File file,
                       boolean parent)
         throws IOException
    {
        if (_dirAllowed)
        {
            Code.debug("sendDirectory: "+file);
            String base = request.getPath();
            if (!base.endsWith("/"))
                base+="/";
        
            response.setField(HttpFields.__ContentType,
                              "text/html");
            if (request.getMethod().equals(HttpRequest.__HEAD))
            {
                // Bail out here otherwise we build the page fruitlessly and get
                // hit with a HeadException when we try to write the page...
                response.commit();
                return;
            }
        
            String title = "Directory: "+base;

            ChunkableOutputStream out=response.getOutputStream();

            out.print("<HTML><HEAD><TITLE>");
            out.print(title);
            out.print("</TITLE></HEAD><BODY>\n<H1>");
            out.print(title);
            out.print("</H1><TABLE BORDER=0>");
        
            if (parent)
            {
                out.print("<TR><TD><A HREF=");
                out.print(padSpaces(base));
                out.print("../>Parent Directory</A></TD><TD></TD><TD></TD></TR>\n");
            }
        
            DateFormat dfmt=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                                           DateFormat.MEDIUM);
            String[] ls = file.list();
            for (int i=0 ; i< ls.length ; i++)
            {
                File item = new File(file.getPath()+File.separator+ls[i]);
                out.print("<TR><TD><A HREF=");
                String path=base+ls[i];
                if (item.isDirectory())
                    path+="/";
                out.print(padSpaces(path));
                out.print(">");
                out.print(ls[i]);
                out.print("&nbsp;");
                out.print("</TD><TD ALIGN=right>");
                out.print(""+item.length());
                out.print(" bytes&nbsp;</TD><TD>");
                out.print(dfmt.format(new Date(item.lastModified())));
                out.print("</TD></TR>\n");
            }
            out.println("</TABLE>");
            out.flush();
            request.setHandled(true);
        }
        else
        {
            // directory request not allowed
            response.sendError(HttpResponse.__403_Forbidden,
                               "Directory access not allowed");
        }
    }


    
    /* ------------------------------------------------------------ */
    /**
     * Replaces spaces by %20
     */
    private String padSpaces(String str)
    {
	return StringUtil.replace(str," ","%20");
    }
    

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Holds a cached file.
     * It is assumed that threads accessing CachedFile have
     * the parents cacheMap locked. 
     */
    private class CachedFile
    {
        String filename;
        File file;
        long lastModified;
        byte[] bytes;
        String encoding;

        /* ------------------------------------------------------------ */
        boolean isValid()
            throws IOException
        {
            // check we have a file still
            if (file==null)
                return false;
            
            // check if the file is still there
            if (!file.exists())
            {
                flush();
                return false;
            }
            
            // check if the file has changed
            if( lastModified!=file.lastModified())
            {
                // If it is too big
                if (file.length()>_maxCachedFileSize)
                {
                    flush();
                    return false;
                }
                // reload the changed file
                load(filename,file);
            }
            return true;
        }
        
        /* ------------------------------------------------------------ */
        byte[] prepare(HttpResponse response)
            throws IOException
        {
            Code.debug("HIT: ",filename);
            response.setField(HttpFields.__ContentType,encoding);
            response.setIntField(HttpFields.__ContentLength,bytes.length);
            response.setDateField(HttpFields.__LastModified,lastModified);
            return bytes;
        }

        /* ------------------------------------------------------------ */
        void load(String filename,File file)
            throws IOException
        {
            this.filename=filename;
            this.file=file;
            lastModified=file.lastModified();
            bytes = new byte[(int)file.length()];
            Code.debug("LOAD: ",filename);
            InputStream in=new FileInputStream(file);
            int read=0;
            while (read<bytes.length)
            {
                int len=in.read(bytes,read,bytes.length-read);
                if (len==-1)
                    throw new IOException("Unexpected EOF: "+file);
                read+=len;
            }
            in.close();
            encoding=getHandlerContext().getMimeByExtension(file.getName());
        }
        
        /* ------------------------------------------------------------ */
        void flush()
        {
            if (file!=null)
            {
                Code.debug("FLUSH: ",filename);
                _cacheMap.remove(filename);
                filename=null;
                file=null;
            }
        }
    }
}
