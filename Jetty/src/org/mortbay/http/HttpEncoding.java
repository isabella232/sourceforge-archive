// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.util.Map;
import org.mortbay.util.Code;

public class HttpEncoding
{
    /* ------------------------------------------------------------ */
    /** Enable transfer encodings.
     * @param in 
     * @param coding Coding enumeration
     * @exception HttpException 
     */
    public void enableEncoding(ChunkableInputStream in,
                               String coding,
                               Map parameters)
        throws HttpException
    {
        try
        {
            if ("gzip".equalsIgnoreCase(coding))
            {
                in.setFilterStream(new java.util.zip.GZIPInputStream(in.getFilterStream()));
            }
            else if ("deflate".equalsIgnoreCase(coding))
            {
                in.setFilterStream(new java.util.zip.InflaterInputStream(in.getFilterStream()));
            }
            else throw new
                HttpException(HttpResponse.__501_Not_Implemented);   
        }
        catch (HttpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Code.warning(e);
            throw new HttpException(HttpResponse.__500_Internal_Server_Error);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Enable a transfer encoding.
     * Enable a transfer encoding on a ChunkableOutputStream.
     * @param out
     * @param coding Coding name 
     * @param parameters Coding parameters or null
     * @exception HttpException 
     */
    public void enableEncoding(ChunkableOutputStream out,
                               String coding,
                               Map parameters)
        throws HttpException
    {
        try
        {
            if ("gzip".equalsIgnoreCase(coding))
            {
                out.setFilterStream(new java.util.zip.GZIPOutputStream(out.getFilterStream()));
            }
            else if ("deflate".equalsIgnoreCase(coding))
            {
                out.setFilterStream(new java.util.zip.DeflaterOutputStream(out.getFilterStream()));
            }
            else
                throw new HttpException(HttpResponse.__501_Not_Implemented);
        }
        catch (HttpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Code.warning(e);
            throw new HttpException(HttpResponse.__500_Internal_Server_Error);
        }
    }
}
