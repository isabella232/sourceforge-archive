// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

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
    String _fileBase;
    Vector indexFiles=new Vector(2);
    boolean putAllowed;
    boolean deleteAllowed;
    boolean dirAllowed=true;
    String allowHeader = null;
    Hashtable mimeMap;
    CachedFile[] cache=null;
    int nextIn=0;
    Hashtable cacheMap=null;
    int maxCachedFiles;
    int maxCachedFileSize;
    
    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler at the given fileBase and use indexFile
     * as the default directory return file.
     */
    public FileHandler(String fileBase,
                       String indexFile,
                       boolean dirAllowed,
                       boolean putAllowed,
                       boolean deleteAllowed,
                       int maxCachedFiles,
                       int maxCachedFileSize)
    {
        _fileBase=fileBase;
        indexFiles=new Vector();
        indexFiles.addElement(indexFile);
        this.dirAllowed=dirAllowed;
        this.putAllowed=putAllowed;
        this.deleteAllowed=deleteAllowed;

        this.maxCachedFiles=maxCachedFiles;
        this.maxCachedFileSize=maxCachedFileSize;
          
    }

    /* ----------------------------------------------------------------- */
    public void start()
    {
        Log.event("FileHandler started in "+_fileBase);
        if (maxCachedFiles>0 && maxCachedFileSize>0 && cache==null)
        {
            cache=new CachedFile[maxCachedFiles];
            cacheMap=new Hashtable();
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
        cache=null;
        cacheMap.clear();
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
        String realpath=_fileBase;
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
    
    /* ----------------------------------------------------------------- */
    public boolean isPutAllowed()
    {
        return putAllowed;
    }
    
    /* ----------------------------------------------------------------- */
    public void setPutAllowed(boolean putAllowed_)
    {
        putAllowed = putAllowed_;
        allowHeader = null;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isDeleteAllowed()
    {
        return deleteAllowed;
    }
    
    /* ----------------------------------------------------------------- */
    public void setDeleteAllowed(boolean deleteAllowed_)
    {
        deleteAllowed = deleteAllowed_;
        allowHeader = null;
    }

    /* ------------------------------------------------------------ */
    public boolean isDirAllowed()
    {
        return dirAllowed;
    }
    
    /* ----------------------------------------------------------------- */
    public void setDirAllowed(boolean dirAllowed_)
    {
        dirAllowed = dirAllowed_;
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
        if (cache!=null && !endsWithSlash)
        {
            byte[] bytes=null;
            synchronized(cacheMap)
            {
                CachedFile cachedFile=
                    (CachedFile)cacheMap.get(filename);
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
                request.setHandled(true);
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
                if (!endsWithSlash)
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
                for (int i=indexFiles.size();i-->0;)
                {
                    File index = new File(filename+
                                          File.separator +
                                          indexFiles.elementAt(i));
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

        if (!putAllowed)
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

        if (!deleteAllowed)
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
            if (cacheMap!=null)
            {
                CachedFile cachedFile=(CachedFile)cacheMap.get(filename);
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
        if (!deleteAllowed || !putAllowed)
            return;
        
        File file = new File(filename);
        if (!file.exists())
        {
            if (deleteAllowed && putAllowed)
                response.sendError(response.__404_Not_Found);
            return;
        }

        if (!deleteAllowed || !putAllowed)
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
        if (allowHeader == null)
        {
            StringBuffer sb = new StringBuffer(128);
            sb.append(HttpRequest.__GET);
            sb.append(", ");
            sb.append(HttpRequest.__HEAD);
            if (putAllowed){
                sb.append(", ");
                sb.append(HttpRequest.__PUT);
            }
            if (deleteAllowed){
                sb.append(", ");
                sb.append(HttpRequest.__DELETE);
            }
            if (putAllowed && deleteAllowed)
            {
                sb.append(", ");
                sb.append(HttpRequest.__MOVE);
            }
            sb.append(", ");
            sb.append(HttpRequest.__OPTIONS);
            allowHeader = sb.toString();
        }
        response.setField(HttpFields.__Allow, allowHeader);
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
        if (cache!=null && file.length()<maxCachedFileSize)
        {
            byte[] bytes=null;
            synchronized (cacheMap)
            {
                CachedFile cachedFile=cache[nextIn];
                if (cachedFile==null)
                    cachedFile=cache[nextIn]=new CachedFile();
                nextIn=(nextIn+1)%cache.length;
                cachedFile.flush();
                cachedFile.load(filename,file);
                cacheMap.put(filename,cachedFile);
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
            String encoding=getMimeByExtension(file.getName());
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
        if (dirAllowed)
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
                out.print(base);
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
                out.print(path);
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
     * @param filename 
     * @return 
     */
    String getMimeByExtension(String filename)
    {
        int i=filename.indexOf(".");
        String ext;

        if (i<0 || i>=filename.length())
            ext="default";
        else
            ext=StringUtil.asciiToLowerCase(filename.substring(i+1));
        
        if (mimeMap==null)
        {
            mimeMap = new Hashtable();
            mimeMap.put("default","application/octet-stream");
            mimeMap.put("class","application/octet-stream");
            mimeMap.put("html","text/html");
            mimeMap.put("htm","text/html");
            mimeMap.put("txt","text/plain");
            mimeMap.put("java","text/plain");
            mimeMap.put("gif","image/gif");
            mimeMap.put("jpg","image/jpeg");
            mimeMap.put("jpeg","image/jpeg");
            mimeMap.put("au","audio/basic");
            mimeMap.put("snd","audio/basic");
            mimeMap.put("ra","audio/x-pn-realaudio");
            mimeMap.put("ram","audio/x-pn-realaudio");
            mimeMap.put("rm","audio/x-pn-realaudio");
            mimeMap.put("rpm","audio/x-pn-realaudio");
            mimeMap.put("mov","video/quicktime");
            mimeMap.put("jsp","text/plain");
        }
        
        String type = (String)mimeMap.get(ext);
        if (type==null)
            type = (String)mimeMap.get("default");

        return type;
    }

    
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
                if (file.length()>maxCachedFileSize)
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
            encoding=getMimeByExtension(file.getName());
        }
        
        /* ------------------------------------------------------------ */
        void flush()
        {
            if (file!=null)
            {
                Code.debug("FLUSH: ",filename);
                cacheMap.remove(filename);
                filename=null;
                file=null;
            }
        }
    }

    

}
