// ===========================================================================
// Copyright (c) 2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.CachedResource;
import org.mortbay.util.Code;
import org.mortbay.util.Resource;
import org.mortbay.util.StringMap;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URI;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ResourceBase
{
    private Map _cache=new HashMap();
    private CachedMetaData _mostRecentlyUsed=null;
    private CachedMetaData _leastRecentlyUsed=null;
    private int _cacheSize;
    
    private int _maxCachedFileSize =400*1024;
    private int _maxCacheSize =4*1024*1024;
    private HttpContext _context=null;
    private boolean _dirAllowed=true;
    private String[] _welcomes=
    {
        "welcome.html",
        "index.html",
        "index.htm",
        "index.jsp"
    };
    
    private String[] _methods=null;
    private String _allowed;
    private StringMap _methodMap = new StringMap();
    {
        setAllowedMethods(new String[]
            {
                HttpRequest.__GET,
                HttpRequest.__POST,
                HttpRequest.__HEAD,
                HttpRequest.__OPTIONS,
                HttpRequest.__TRACE
            });
    }
    
    
    /* ------------------------------------------------------------ */
    public HttpContext getHttpContext()
    {
        return _context;
    }
    
    /* ------------------------------------------------------------ */
    public void setHttpContext(HttpContext context)
    {
        _context=context;
    }
    
    /* ------------------------------------------------------------ */
    public int getMaxCachedFileSize()
    {
        return _maxCachedFileSize;
    }
 
    /* ------------------------------------------------------------ */
    public void setMaxCachedFileSize(int maxCachedFileSize)
    {
        _maxCachedFileSize = maxCachedFileSize;
    }

    /* ------------------------------------------------------------ */
    public int getMaxCacheSize()
    {
        return _maxCacheSize;
    }
 
    /* ------------------------------------------------------------ */
    public void setMaxCacheSize(int maxCacheSize)
    {
        _maxCacheSize = maxCacheSize;
    }

    /* ------------------------------------------------------------ */
    public boolean isDirAllowed()
    {
        return _dirAllowed;
    }
    
    /* ------------------------------------------------------------ */
    public void setDirAllowed(boolean dirAllowed)
    {
        _dirAllowed = dirAllowed;
    }
    
    /* ------------------------------------------------------------ */
    public String[] getAllowedMethods()
    {
        return _methods;
    }

    /* ------------------------------------------------------------ */
    public void setAllowedMethods(String[] methods)
    {
        StringBuffer b = new StringBuffer();
        _methods=methods;
        _methodMap.clear();
        for (int i=0;i<methods.length;i++)
        {
            _methodMap.put(methods[i],methods[i]);
            if (i>0)
                b.append(',');
            b.append(methods[i]);
        }
        _allowed=b.toString();
    }

    /* ------------------------------------------------------------ */
    public boolean isMethodAllowed(String method)
    {
        return _methodMap.get(method)!=null;
    }

    /* ------------------------------------------------------------ */
    public String getAllowedString()
    {
        return _allowed;
    }
    
    /* ------------------------------------------------------------ */
    public String[] getWelcomeFiles()
    {
        return _welcomes;
    }

    /* ------------------------------------------------------------ */
    public void setWelcomeFiles(String[] welcomes)
    {
        if (welcomes==null)
            _welcomes=new String[0];
        else
            _welcomes=welcomes;
    }

    /* ------------------------------------------------------------ */
    public void addWelcomeFile(String welcomeFile)
    {
        if (welcomeFile.startsWith("/") ||
            welcomeFile.startsWith(java.io.File.separator) ||
            welcomeFile.endsWith("/") ||
            welcomeFile.endsWith(java.io.File.separator))
            Code.warning("Invalid welcome file: "+welcomeFile);
        List list = new ArrayList(Arrays.asList(_welcomes));
        list.add(welcomeFile);
        _welcomes=(String[])list.toArray(_welcomes);
    }
    
    /* ------------------------------------------------------------ */
    public void removeWelcomeFile(String welcomeFile)
    {
        List list = new ArrayList(Arrays.asList(_welcomes));
        list.remove(welcomeFile);
        _welcomes=(String[])list.toArray(_welcomes);
    }
    
    /* ------------------------------------------------------------ */
    public Resource getResource(String pathInContext)
        throws IOException
    {    
        // Make the resource
        if (_context==null)
            return null;
        Resource baseResource=_context.getBaseResource();
        if (baseResource==null)
            return null;
        
        Resource resource=null;

        // Cache operations
        synchronized(_cache)
        {
            // Look for it in the cache
            CachedResource cached = (CachedResource)_cache.get(pathInContext);
            if (cached!=null)
            {
                Code.debug("CACHE HIT: ",cached);
                CachedMetaData cmd = (CachedMetaData)cached.getAssociate();
                if (cmd!=null && cmd.isValid())
                    return cached;
            }
            else
            {    
                resource=baseResource.addPath(pathInContext);
                Code.debug("CACHE MISS: ",resource);
                if (resource==null)
                    return null;
            }

            // Is it cacheable?
            long len = resource.length();
            if (resource.exists() &&
                !resource.isDirectory() &&
                len>0 && len<_maxCachedFileSize && len<_maxCacheSize)
            {
                int needed=_maxCacheSize-(int)len;
                while(_cacheSize>needed)
                    _leastRecentlyUsed.invalidate();
            
                cached=resource.cache();
                new CachedMetaData(cached,pathInContext);
                return cached;
            }
        }

        // Non cached response
        MetaData md = new MetaData(resource);
        return resource;
    }

    /* ------------------------------------------------------------ */
    public String getWelcomeFile(Resource resource)
        throws IOException
    {
        if (!resource.isDirectory())
            return null;
        
        for (int i=0;i<_welcomes.length;i++)
        {
            Resource welcome=resource.addPath(_welcomes[i]);
            if (welcome.exists())
                return _welcomes[i];
        }

        return null;
    }
    
    /* ------------------------------------------------------------ */
    public ByteArrayISO8859Writer getDirectoryListing(Resource resource,
                                                      String base,
                                                      boolean parent)
        throws IOException
    {
        if (!_dirAllowed || !resource.isDirectory())
            return null;
        
        String[] ls = resource.list();
        if (ls==null)
            return null;
                
        String title = "Directory: "+base;
        
        ByteArrayISO8859Writer out = new ByteArrayISO8859Writer();
        
        out.write("<HTML><HEAD><TITLE>");
        out.write(title);
        out.write("</TITLE></HEAD><BODY>\n<H1>");
        out.write(title);
        out.write("</H1><TABLE BORDER=0>");
        
        if (parent)
        {
            out.write("<TR><TD><A HREF=");
            out.write(URI.encodePath(URI.addPaths(base,"../")));
            out.write(">Parent Directory</A></TD><TD></TD><TD></TD></TR>\n");
        }
        
        DateFormat dfmt=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                                       DateFormat.MEDIUM);
        for (int i=0 ; i< ls.length ; i++)
        {
            String encoded=URI.encodePath(ls[i]);
            Resource item = resource.addPath(encoded);
            
            out.write("<TR><TD><A HREF=\"");
            String path=URI.addPaths(base,encoded);
            
            if (item.isDirectory() && !path.endsWith("/"))
                path=URI.addPaths(path,"/");
            out.write(path);
            out.write("\">");
            out.write(StringUtil.replace(StringUtil.replace(ls[i],"<","&lt;"),">","&gt;"));
            out.write("&nbsp;");
            out.write("</TD><TD ALIGN=right>");
            out.write(""+item.length());
            out.write(" bytes&nbsp;</TD><TD>");
            out.write(dfmt.format(new Date(item.lastModified())));
            out.write("</TD></TR>\n");
        }
        out.write("</TABLE>\n");
        out.flush();
        return out;
    }
    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class MetaData
    {
        protected String _name;
        protected Resource _resource;
        
        MetaData(Resource resource)
        {
            _resource=resource;
            _name=_resource.toString();
            _resource.setAssociate(this);
        }

        public String getLength()
        {
            return Long.toString(_resource.length());
        }
        
        public String getLastModified()
        {
            return HttpFields.__dateSend.format(new Date(_resource.lastModified()));
        }
        
        public String getEncoding()
        {
            return _context.getMimeByExtension(_name);
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class CachedMetaData extends MetaData
    {
        String _lastModified;
        String _encoding;
        String _length;
        String _key;
        
        CachedResource _cached;
        CachedMetaData _prev;
        CachedMetaData _next;
            
        CachedMetaData(CachedResource resource, String pathInContext)
        {
            super(resource);
            _cached=resource;
            _length=super.getLength();
            _lastModified=super.getLastModified();
            _encoding=super.getEncoding();
            _key=pathInContext;
            
            _next=_mostRecentlyUsed;
            _mostRecentlyUsed=this;
            if (_next!=null)
                _next._prev=this;
            _prev=null;
            if (_leastRecentlyUsed==null)
                _leastRecentlyUsed=this;
            
            _cache.put(_key,resource);

            _cacheSize+=_cached.length();
            
        }
        
        public String getLength()
        {
            return _length;
        }
        
        public String getLastModified()
        {
            return _lastModified;
        }
        
        public String getEncoding()
        {
            return _encoding;
        }
        
        /* ------------------------------------------------------------ */
        boolean isValid()
            throws IOException
        {
            if (_cached.isUptoDate())
            {
                if (_mostRecentlyUsed!=this)
                {
                    CachedMetaData tp = _prev;
                    CachedMetaData tn = _next;
                    
                    _next=_mostRecentlyUsed;
                    _mostRecentlyUsed=this;
                    if (_next!=null)
                        _next._prev=this;
                    _prev=null;
                    
                    if (tp!=null)
                        tp._next=tn;
                    if (tn!=null)
                        tn._prev=tp;
                    
                    if (_leastRecentlyUsed==this && tp!=null)
                        _leastRecentlyUsed=tp;
                }
                return true;
            }

            invalidate();
            return false;
        }

        public void invalidate()
        {
            // Invalidate it
            _cache.remove(_key);
            _cacheSize=_cacheSize-(int)_cached.length();
            
            
            if (_mostRecentlyUsed==this)
                _mostRecentlyUsed=_next;
            else
                _prev._next=_next;
            
            if (_leastRecentlyUsed==this)
                _leastRecentlyUsed=_prev;
            else
                _next._prev=_prev;
            
            _prev=null;
            _next=null;
        }
        
    }
}
