// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/* ================================================================ */
/** Handle a multipart MIME response.
 *
 * @version $Id$
 * @author Greg Wilkins
 * @author Jim Crossley
*/
public class MultiPartResponse
{
    /* ------------------------------------------------------------ */
    private String boundary =
        "com.mortbay.HTTP.MultiPartResponse.boundary."+
        Long.toString(System.currentTimeMillis(),36);
    public String getBoundary()
    {
        return boundary;
    }
    
    /* ------------------------------------------------------------ */    
    /** PrintWriter to write content too.
     */
    private OutputStream out = null; 
    public OutputStream getOut() {return out;}

    /* ------------------------------------------------------------ */
    private boolean inPart=false;
    
    /* ------------------------------------------------------------ */
    protected MultiPartResponse(OutputStream out)
         throws IOException
    {
        this.out=out;
        inPart=false;
    }
    
    /* ------------------------------------------------------------ */
    /** MultiPartResponse constructor.
     * @param response The ServletResponse to which this multipart
     *                 response will be sent.
     */
    public MultiPartResponse(HttpRequest request,
                             HttpResponse response)
         throws IOException
    {
        response.setField(HttpFields.__ContentType,"multipart/mixed;boundary="+boundary);
        out=response.getOutputStream();
        inPart=false;
    }    

    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType)
         throws IOException
    {
        if (inPart)
            out.write(HttpFields.__CRLF.getBytes());
        inPart=true;
        out.write(("--"+boundary+HttpFields.__CRLF).getBytes());
        out.write(("Content-type: "+contentType+
                   HttpFields.__CRLF+HttpFields.__CRLF).getBytes());
    }
    
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType, String[] headers)
         throws IOException
    {
        if (inPart)
            out.write(HttpFields.__CRLF.getBytes());
        inPart=true;
        out.write(("--"+boundary+HttpFields.__CRLF).getBytes());
        out.write(("Content-type: "+contentType+HttpFields.__CRLF).getBytes());
        for (int i=0;headers!=null && i<headers.length;i++)
        {
            out.write(headers[i].getBytes());
            out.write(HttpFields.__CRLF.getBytes());
        }
        out.write(HttpFields.__CRLF.getBytes());
    }
        
    /* ------------------------------------------------------------ */
    /** End the current part.
     * @param lastPart True if this is the last part
     * @exception IOException IOException
     */
    public void close()
         throws IOException
    {
        if (inPart)
            out.write(HttpFields.__CRLF.getBytes());
        out.write(("--"+boundary+"--"+HttpFields.__CRLF).getBytes());
        inPart=false;
    }
    
};




