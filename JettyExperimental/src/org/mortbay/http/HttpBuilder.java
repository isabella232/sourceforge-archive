//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.http;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class HttpBuilder implements HTTP
{
    // states
    public final static int STATE_START=0;
    public final static int STATE_HEADER=1;
    public final static int STATE_CONTENT=2;
    public final static int STATE_END=3;
    
    private static byte[] CRLF_LAST_CHUNK={(byte)'\015',(byte)'\012',(byte)'0',(byte)'\015',(byte)'\012',(byte)'\015',(byte)'\012'};
    private static byte[] LAST_CHUNK={(byte)'0',(byte)'\015',(byte)'\012',(byte)'\015',(byte)'\012'};
    private static byte[] CONNECTION_CLOSE=Portable.getBytes("Connection:close\015\012");
    private static byte[] CONNECTION_KEEP_ALIVE=Portable.getBytes("Connection:keep-alive\015\012");
    private static byte[] CONTENT_LENGTH_0=Portable.getBytes("ContentLength:0\015\012");
    private static byte[] TRANSFER_ENCODING_CHUNKED=Portable.getBytes("Transfer-Encoding:chunked\015\012");
    
    private static final int __versionLength=HttpVersions.HTTP_1_1_BUFFER.length();
    private static byte[][] __start = new byte[600][];
    
    static
    {
        for (int i=0;i<__start.length;i++)
        {
            Buffer buf = HttpStatus.CACHE.get(i);
            if (buf==null)
                continue;
            
            __start[i]=new byte[__versionLength+5+buf.length()+2];
            HttpVersions.HTTP_1_1_BUFFER.peek(0, __start[i], 0, __versionLength);
            __start[i][__versionLength+0]=' ';
            __start[i][__versionLength+1]=(byte)('0'+i/100);
            __start[i][__versionLength+2]=(byte)('0'+(i%100)/10);
            __start[i][__versionLength+3]=(byte)('0'+(i%10));
            __start[i][__versionLength+4]=' ';
            buf.peek(0, __start[i], __versionLength+5, buf.length());
            __start[i][__versionLength+5+buf.length()]=HttpParser.CARRIAGE_RETURN;
            __start[i][__versionLength+6+buf.length()]=HttpParser.LINE_FEED;
        }
    }
    static
    
    
    private int _state=STATE_START;
    private int _version=0;
    private int _status=0;
    private EndPoint _endp;
    private Buffer _buffer;
    private int _contentLength=UNKNOWN_CONTENT;
    private int _contentWritten=0;
    private boolean _close=false;
    private boolean _keepAlive=false;
    private boolean _contentLengthSet =false;
    private boolean _transferEncodingSet=false;
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor. 
     */
    public HttpBuilder(Buffer buffer, EndPoint io)
    {
        this._buffer=buffer;
        this._endp=io;
    }
    
    public void reset()
    {
        _state=STATE_START;
        _version=0;
        _status=0;
        _contentLength=UNKNOWN_CONTENT;
        _contentWritten=0;
        _close=false;
        _keepAlive=false;
        _contentLengthSet =false;
        _transferEncodingSet=false;
    }
    
    public boolean isPersistent()
    {
        return !_close && _contentLength!=EOF_CONTENT;
    }
    
    public void startResponse(int version,int status,String reason)
    {   
        if (_state!=STATE_START)
            Portable.throwIllegalState("STATE!=START");
        
        _version=version;
        
        if (version==HttpVersions.HTTP_0_9_ORDINAL)
        {
            _close=true;
            _contentLength=EOF_CONTENT;
        }
        else
        {
            _status=status;
            
            byte[] start=__start[status];
        
            if (start==null)
            {
                if (reason==null)
                    reason="Unknown";
                _buffer.put(HttpVersions.HTTP_1_1_BUFFER);
                _buffer.put((byte)' ');
                _buffer.put((byte)('0'+status/100));
                _buffer.put((byte)('0'+(status%100)/10));
                _buffer.put((byte)('0'+(status%10)));
                _buffer.put((byte)' ');
                byte[] r=Portable.getBytes(reason);
                _buffer.put(r,0,r.length);
                _buffer.put(CRLF);
            }
            else if (reason!=null)
            {
                _buffer.put(start,0,__versionLength+5);
                byte[] r=Portable.getBytes(reason);
                _buffer.put(r,0,r.length);
                _buffer.put(CRLF);
            }
            else
            {
                _buffer.put(start,0,start.length);
            }
        }
        
        _state=STATE_HEADER;
    }
    
    
    public void header(Buffer name,Buffer value)
    {
        if (_state==STATE_START)
            Portable.throwIllegalState("STATE==START");
        if (_state!=STATE_HEADER)
            return;
        if (_version<HttpVersions.HTTP_1_0_ORDINAL)
            return;
        
        boolean ok=true;
        
        int header = HttpHeaders.CACHE.getOrdinal(name);
        
        int value_ordinal;
        switch (header)
        {
            case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                _contentLengthSet=true;
            	if (_transferEncodingSet && _contentLength==CHUNKED_CONTENT)
            	    Portable.throwIllegalArgument("contentlength && chunk");
                _contentLength=BufferUtil.toInt(value);
            	break;
            	
            case HttpHeaders.CONTENT_TYPE_ORDINAL:
                if (BufferUtil.isPrefix(HttpHeaderValues.MULTIPART_BYTERANGES_BUFFER, value))
                    _contentLength=SELF_DEFINING_CONTENT;
                else if (_contentLength==UNKNOWN_CONTENT)
                    _contentLength=(_close || _version==HttpVersions.HTTP_1_0_ORDINAL)?EOF_CONTENT:CHUNKED_CONTENT;
                break;
            
            case HttpHeaders.TRANSFER_ENCODING_ORDINAL:
                _transferEncodingSet=true;
            	value_ordinal = HttpHeaderValues.CACHE.getOrdinal(value);
            	if (HttpHeaderValues.CHUNKED_ORDINAL == value_ordinal)
            	{
            	    if (_contentLength>=0)
            	        Portable.throwIllegalArgument("chunk && contentlength");
            	    _contentLength = CHUNKED_CONTENT;
            	}
            	else
            	{
            	    // TODO avoid string conversion here
            	    String c = value.toString();
            	    if (c.endsWith(HttpHeaderValues.CHUNKED))
            	    {
                	    if (_contentLength>=0)
                	        Portable.throwIllegalArgument("chunk && contentlength");
            	        _contentLength = CHUNKED_CONTENT;
            	    }
            	    else if (c.indexOf(HttpHeaderValues.CHUNKED) >= 0) 
            	        Portable.throwIllegalArgument("BAD TE");
            	}
            
                break;
            
            case HttpHeaders.CONNECTION_ORDINAL:
                ok=false;
            	value_ordinal = HttpHeaderValues.CACHE.getOrdinal(value);
            	// TODO handle multivalue Connection
                _close=HttpHeaderValues.CLOSE_ORDINAL==value_ordinal;
                _keepAlive=HttpHeaderValues.KEEP_ALIVE_ORDINAL==value_ordinal;
                
                if (_close && _contentLength==UNKNOWN_CONTENT)
                    _contentLength=EOF_CONTENT;
                    
                break;
        }
        

        if (ok)
        {
            _buffer.put(name);
            _buffer.put(COLON);
            _buffer.put(value);
            if (_buffer.put(CRLF)<2)
                Portable.throwIllegalState("Header>Buffer.capacity()");
        }
        
        
    }
    
    public void header(Buffer name,int value)
    {
        if (_state==STATE_START)
            Portable.throwIllegalState("STATE==START");
        if (_state==STATE_HEADER)
        {
            _buffer.put(name);
            _buffer.put(COLON);
            BufferUtil.putDecInt(_buffer,value);
            if (_buffer.put(CRLF)<2)
                Portable.throwIllegalState("Header>Buffer.capacity()");
        }
        
        int header = HttpHeaders.CACHE.getOrdinal(name);
        switch (header)
        {
            case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                _contentLengthSet=true;
                _contentLength=value;
            	break;
            	
        }
    }
    
    public int content(Buffer content,boolean last)
    {
        if (_state==STATE_HEADER)
        {
            if (last && (_contentLength==UNKNOWN_CONTENT || _contentLength==EOF_CONTENT))
                _contentLength=content.length();
            else if (_contentLength==UNKNOWN_CONTENT )
                _contentLength=(_close || _version<=HttpVersions.HTTP_1_0_ORDINAL)?EOF_CONTENT:CHUNKED_CONTENT;
            completeHeader();
        }
        
        if (_state!=STATE_CONTENT)
            Portable.throwIllegalState("STATE=="+_state);
        
        int len=0;
        switch(_contentLength)
        {
            case CHUNKED_CONTENT:
                int space=_buffer.space();
                if (content.length()+24<space)
                {
                    len=content.length();
                    BufferUtil.putHexInt(_buffer, len);
                    _buffer.put(CRLF);
                    _buffer.put(content);
                    _buffer.put(last?CRLF_LAST_CHUNK:CRLF);
                }
                else if (space>24)
                {
                    len = space-24;
                    BufferUtil.putHexInt(_buffer, len);
                    _buffer.put(CRLF);
                    _buffer.poke(_buffer.putIndex(),content);
                    _buffer.setPutIndex(_buffer.putIndex()+len);
                    _buffer.put(last?CRLF_LAST_CHUNK:CRLF);
                }
                break;
                
            case EOF_CONTENT:	
            default:
                // TODO check space or bypass etc.
                len=_buffer.put(content);
            	break;
        }

        _contentWritten+=len;
        
        if (last)
        {
            if (_contentLength>0)
            {
                while(_contentWritten<_contentLength)
                {
                    _buffer.put(SPACE);
                    _contentWritten++;
                }
            }
            _state=STATE_END;
        }
        
        return len;
    }
    
    public void complete()
    {
        if(_state==STATE_END)
            return;
        
        if (_state==STATE_HEADER)
        {
            if (_contentLength==UNKNOWN_CONTENT)
                _contentLength=NO_CONTENT;
            completeHeader();
        }
        
        if (_state!=STATE_CONTENT)
            Portable.throwIllegalState("STATE=="+_state);

        if (_contentLength>0)
        {
            while(_contentWritten<_contentLength)
            {
                _buffer.put(SPACE);
                _contentWritten++;
            }
        }
        
        switch(_contentLength)
        {
            case NO_CONTENT:	
                break;
                
            case CHUNKED_CONTENT:
                _buffer.put(LAST_CHUNK);
                break;
                
            case EOF_CONTENT:	
                break;
                
            default:
            	break;
        }
        
        _state=STATE_END;
    }

    private void completeHeader()
    {
        // From RFC 2616 4.4:
        // 1. No body for 1xx, 204, 304 & HEAD response
        // 2. Force content-length?
        // 3. If Transfer-Encoding!=identity && HTTP/1.1 && !Connection==close then chunk
        // 4. Content-Length
        // 5. multipart/byteranges
        // 6. close
     
        if (_version>=HttpVersions.HTTP_1_0_ORDINAL)
        {
            switch(_contentLength)
            {
                case UNKNOWN_CONTENT:
                    _contentLength=NO_CONTENT;
                    
                case NO_CONTENT:
                    if(!_contentLengthSet && _status>=200 && _status!=204 && _status!=304)    
                        _buffer.put(CONTENT_LENGTH_0);
                    break;
                    
                case EOF_CONTENT:
                    _close=true;
                    break;
                    
                case CHUNKED_CONTENT:
                    if (!_transferEncodingSet) 
                        _buffer.put(TRANSFER_ENCODING_CHUNKED);  
                    break;
                    
                default:       
                    if (!_contentLengthSet)
                    {
                        _buffer.put(HttpHeaders.CONTENT_LENGTH_BUFFER);
                        _buffer.put(COLON);
                        BufferUtil.putDecInt(_buffer,_contentLength);
                        if (_buffer.put(CRLF)<2)
                            Portable.throwIllegalState("Header>Buffer.capacity()");
                    }
                    else if (_keepAlive && _version==HttpVersions.HTTP_1_0_ORDINAL)
                        _buffer.put(CONNECTION_KEEP_ALIVE);
            }

            if (_close)
                _buffer.put(CONNECTION_CLOSE);
        
            _buffer.put(CRLF);
        }
        _state=STATE_CONTENT;
        
    }
}
