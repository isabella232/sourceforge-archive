// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.mortbay.http.ChunkableInputStream;
import org.mortbay.http.ChunkableOutputStream;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpEncoding;
import org.mortbay.http.HttpContext;
import org.mortbay.http.OutputObserver;
import org.mortbay.util.Code;

/* ------------------------------------------------------------ */
/** Handler to test TE transfer encoding.
 * If 'gzip' or 'deflate' is in the query string, the
 * response is given that transfer encoding
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ContentEncodingHandler
    extends NullHandler
    implements OutputObserver
{
    private HttpEncoding _httpEncoding;
    private int _minimumLength=512;
    
    /* ------------------------------------------------------------ */
    public void initialize(HttpContext context)
    {
        super.initialize(context);
        _httpEncoding = context.getHttpServer().getHttpEncoding();    
    }

    /* ------------------------------------------------------------ */
    public void setMinimumLength(int l)
    {
        _minimumLength=l;
    }
    
    /* ------------------------------------------------------------ */
    public int getMinimumLength()
    {
        return _minimumLength;
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        // Handle request encoding
        String encoding=request.getField(HttpFields.__ContentEncoding);
        if (encoding!=null)
        {
            Map encodingParams=null;
            if (encoding.indexOf(";")>0)
            {
                encodingParams=new HashMap(3);
                encoding=HttpFields.valueParameters(encoding,encodingParams);
            }
            
            _httpEncoding.enableEncoding((ChunkableInputStream)request.getInputStream(),
                                         encoding,encodingParams);
            request.setState(HttpMessage.__MSG_EDITABLE);
            request.removeField(HttpFields.__ContentEncoding);
            request.setState(HttpMessage.__MSG_RECEIVED);
        }
        
        // Handle response encoding.
        List list =
            HttpFields.qualityList(request.getFieldValues(HttpFields.__AcceptEncoding,
                                                          HttpFields.__separators));
        for (int i=0;i<list.size();i++)
        {
            encoding = HttpFields.valueParameters((String)list.get(i),null);
            
            if (_httpEncoding.knownEncoding(encoding))
            {
                // We can handle this encoding, so observe for content.
                ChunkableOutputStream out = (ChunkableOutputStream)response.getOutputStream();
                out.addObserver(this,response);
                response.setAttribute("Encoding",encoding);
                break;
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public void outputNotify(ChunkableOutputStream out, int event, Object data)
    {
        switch (event)
        {
          case OutputObserver.__FIRST_WRITE:
              HttpResponse response = (HttpResponse)data;
              String encoding=(String)response.getAttribute("Encoding");
              if (encoding==null)
                  return;

              // check the known length
              int l=response.getContentLength();
              if (l>=0 && l<_minimumLength)
              {
                  if (Code.debug())
                      Code.debug("Abort encoding due to size: "+l);
                  return;
              }

              // Is it already encoded?
              if (response.getField(HttpFields.__ContentEncoding)!=null)
              {
                  Code.debug("Already encoded!");
                  return;
              }

              // Initialize encoding!
              try
              {
                  Code.debug("Enable: ",encoding);
                  _httpEncoding.enableEncoding(out,encoding,null);
                  response.setField(HttpFields.__ContentEncoding,encoding);
              }
              catch(Exception e)
              {
                  Code.ignore(e);
                  break;
              }
              response.removeField(HttpFields.__ContentLength);
              break;

          default:
        }
    }
    

}




