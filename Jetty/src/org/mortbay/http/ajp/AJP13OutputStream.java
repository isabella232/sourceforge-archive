// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.ajp;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import org.mortbay.util.Code;
import org.mortbay.http.BufferedOutputStream;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpResponse;



public class AJP13OutputStream extends BufferedOutputStream
{
    private AJP13Packet _packet;
    private boolean _complete;
    private boolean _completed;
    private boolean _persistent=true;
    private byte[] _byte={(byte)0};
    private AJP13Packet _ajpResponse;

    /* ------------------------------------------------------------ */
    AJP13OutputStream(OutputStream out,int bufferSize)
    {
        super(out,bufferSize,7,7,0);
        setFixed(true);
        _packet=new AJP13Packet(_buf);
        
        _packet.addByte((byte)'A');
        _packet.addByte((byte)'B');
        _packet.addInt(0);
        
        setBypassBuffer(false);
        setFixed(true);
        
        _ajpResponse=new AJP13Packet(bufferSize);
        _ajpResponse.addByte((byte)'A');
        _ajpResponse.addByte((byte)'B');
        _ajpResponse.addInt(0);
    }

    
    /* ------------------------------------------------------------ */
    public void writeHeader(HttpMessage httpMessage)
        throws IOException
    {
        HttpResponse response= (HttpResponse)httpMessage; 
        response.setState(HttpMessage.__MSG_SENDING);
        
        _ajpResponse.resetData();
        _ajpResponse.addByte(AJP13Packet.__SEND_HEADERS);
        _ajpResponse.addInt(response.getStatus());
        _ajpResponse.addString(response.getReason());
        
        int mark=_ajpResponse.getMark();
        _ajpResponse.addInt(0);        int nh=0;
        Enumeration e1=response.getFieldNames();
        while(e1.hasMoreElements())
        {
            String h=(String)e1.nextElement();
            Enumeration e2=response.getFieldValues(h);
            while(e2.hasMoreElements())
            {
                _ajpResponse.addHeader(h);
                _ajpResponse.addString((String)e2.nextElement());
                nh++;
            }
        }

        if (nh>0)
            _ajpResponse.setInt(mark,nh);
        _ajpResponse.setDataSize();

        write(_ajpResponse);
        _ajpResponse.resetData();
    }
    
    /* ------------------------------------------------------------ */
    public void write(AJP13Packet packet)
        throws IOException
    {
        packet.write(_out);
    }
    
    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {
        super.flush();

        if (_complete && !_completed)
        {
            _completed=true;
            
            _packet.resetData();
            _packet.addByte(AJP13Packet.__END_RESPONSE);
            _packet.addBoolean(_persistent);
            _packet.setDataSize();
            write(_packet);
            _packet.resetData();
        }
    }

    /* ------------------------------------------------------------ */
    public void close()
        throws IOException
    {
        _complete=true;
        flush();
    }
    
    /* ------------------------------------------------------------ */
    public void resetStream()
    {
        _complete=false;
        _completed=false;
        super.resetStream();
    }
    
    /* ------------------------------------------------------------ */
    public void destroy()
    {
        if (_packet!=null)_packet.destroy();
        _packet=null;
        if (_ajpResponse!=null)_ajpResponse.destroy();
        _ajpResponse=null;
        _byte=null;
        _out=null;
    }
    
    /* ------------------------------------------------------------ */
    public void end()
        throws IOException
    {
        _persistent=false;
    }
    
    /* ------------------------------------------------------------ */
    protected void wrapBuffer()
        throws IOException
    {
        if (size()==0)
            return;

        prewrite(_buf,0,7);
        _packet.resetData();
        _packet.addByte(AJP13Packet.__SEND_BODY_CHUNK);
        _packet.setDataSize(size()-4);

        // System.out.println(Thread.currentThread()+" AJP13 buf "+_packet.toString());
    }
    
    /* ------------------------------------------------------------ */
    protected void bypassWrite(byte[] b, int offset, int length)
        throws IOException
    {
        Code.notImplemented();
    }
    
}
