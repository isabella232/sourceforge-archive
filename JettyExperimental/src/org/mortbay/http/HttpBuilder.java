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

import java.io.IOException;
import java.util.Iterator;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.Buffers;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;

/* ------------------------------------------------------------ */
/** HttpBuilder.
 * Builds HTTP Messages.
 * @author gregw
 *
 */
public class HttpBuilder implements HttpTokens
{

    // states
    public final static int STATE_HEADER=0;
    public final static int STATE_CONTENT=2;
    public final static int STATE_FLUSHING=3;
    public final static int STATE_END=4;
  
    // Last Content 
    public final static boolean LAST=true;
    public final static boolean MORE=false;

    // common content
    private static byte[] CRLF_LAST_CHUNK={(byte)'\015',(byte)'\012',(byte)'0',(byte)'\015',(byte)'\012',(byte)'\015',(byte)'\012'};
    private static byte[] LAST_CHUNK={(byte)'0',(byte)'\015',(byte)'\012',(byte)'\015',(byte)'\012'};
    private static byte[] CONTENT_LENGTH_0=Portable.getBytes("Content-Length: 0\015\012");  
    private static byte[] CONNECTION_KEEP_ALIVE=Portable.getBytes("Connection: keep-alive\015\012");
    private static byte[] CONNECTION_CLOSE=Portable.getBytes("Connection: close\015\012");   
    private static byte[] TRANSFER_ENCODING_CHUNKED=Portable.getBytes("Transfer-Encoding: chunked\015\012");
    private static byte[] SERVER=Portable.getBytes("Server: Jetty(experimental)\015\012");
  
    // other statics
    private static int CHUNK_SPACE=12;
    
    // data
    private int _state=STATE_HEADER;
    private int _version=HttpVersions.HTTP_1_1_ORDINAL;
    private int _status=HttpStatus.ORDINAL_200_OK;
    private String _reason;
    
    private long _contentAdded=0;
    private long _contentLength=UNKNOWN_CONTENT;
    private boolean _last=false;
    private boolean _head=false;
    private boolean _close=false;
    
    private Buffers _buffers;    // source of buffers
    private EndPoint _endp;

    private Buffer _header; // Buffer for HTTP header (and maybe small content)
    private Buffer _buffer; // Buffer for copy of passed content
    private Buffer _content; // Buffer passed to addContent
    boolean _direct=false;  // True if _content buffer can be written directly to endp 
    private boolean _needCRLF=false;   
    private boolean _needEOC=false;   
    private boolean _bufferChunked=false;
    
    private int _flushSize=4096; // TODO fix this
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor. 
     */
    public HttpBuilder(Buffers buffers, EndPoint io)
    {
        this._buffers=buffers;
        this._endp=io;
    }
    

    /* ------------------------------------------------------------------------------- */
    public void reset(boolean returnBuffers)
    {
        _state=STATE_HEADER;
        _version=HttpVersions.HTTP_1_1_ORDINAL;
        _status=HttpStatus.ORDINAL_200_OK;
        _last=false;
        _head=false;
        _close=false;
        _contentAdded=0;
        _contentLength=UNKNOWN_CONTENT;
        
        if (returnBuffers)
        {
            if (_header!=null)
                _buffers.returnBuffer(_header);
            _header=null;
            if (_buffer!=null)
                _buffers.returnBuffer(_buffer);
            _buffer=null;
        }
        else
        {
            if (_header!=null)
                _header.clear();
            
            if (_buffer!=null)
            {
                _buffers.returnBuffer(_buffer);
                _buffer=null;
            }
        }
        _content=null;
        _direct=false;
        _needCRLF=false;
        _needEOC=false;
    }
    

    /* ------------------------------------------------------------ */
    public int getState()
    {
        return _state;
    }

    /* ------------------------------------------------------------ */
    public boolean isState(int state)
    {
        return _state==state;
    }

    /* ------------------------------------------------------------ */
    public boolean isComplete()
    {
        return _state==STATE_END;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isCommitted()
    {
        return _state!=STATE_HEADER;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the head.
     */
    public boolean isHead()
    {
        return _head;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param head The head to set.
     */
    public void setHead(boolean head)
    {
        _head = head;
    }
    

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public boolean isPersistent()
    {
        return !_close;
    }
    

    /* ------------------------------------------------------------ */
    public long getContentAdded()
    {
        return _contentAdded;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param version The version of the client the response is being sent to (NB. Not the version in the response, which is the version of the server). 
     */
    public void setVersion(int version)
    {   
        if (_state!=STATE_HEADER)
            Portable.throwIllegalState("STATE!=START");
        _version=version;
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void setRequest(Buffer method, Buffer uri)
    {  
        Portable.throwNotSupported();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param status The status code to send.
     * @param reason the status message to send.
     */
    public void setResponse(int status,String reason)
    {    
        if (_state!=STATE_HEADER)
            Portable.throwIllegalState("STATE!=START");
        
        _status=status;
        _reason=reason;
    }
    
    /* ------------------------------------------------------------ */
    /** Add content.
     * @param content
     * @return true if the buffers are full
     * @throws IOException
     */
    public void addContent(Buffer content,boolean last) throws IOException
    {
        if (content.isImmutable())
            Portable.throwIllegalArgument("immutable");

        if (_last)
            Portable.throwIllegalState("last");
        _last=last;
        
        // Handle any unfinished business?
        if (_content!=null || _bufferChunked)
        {
            flushBuffers();
            if (_content!=null || _bufferChunked)
                Portable.throwIllegalState("FULL");
        }
        
        _content=content;
        _contentAdded+=content.length();
        
        // Handle the content
        if (_head)
            content.clear();
        else if (_endp!=null && _buffer==null && content.length()>0 && _last )
        {
            // TODO - use direct in more cases.
            // Make content a direct buffer
            _direct=true;
        }
        else    
        {
            // Yes - so we better check we have a buffer 
            if (_buffer==null)
                _buffer=_buffers.getBuffer(Buffers.BIG);
            
            // Copy content to buffer;
            int put=_buffer.put(_content);
            if (_content.length()==0)
                _content=null;
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferFull()
    {
        // Should we flush the buffers?
        boolean full= (_state==STATE_FLUSHING || _direct || (_buffer!=null&&_buffer.space()==0) || (_contentLength==CHUNKED_CONTENT && _buffer!=null && _buffer.space()<CHUNK_SPACE));
        return full;
    }
    

    /* ------------------------------------------------------------ */
    public void completeHeader(HttpFields fields, boolean allContentAdded)
    	throws IOException
    {
        if (_state!=STATE_HEADER)
            return;
        
        if (_last&&!allContentAdded)
            Portable.throwIllegalState("last?");
        _last=_last|allContentAdded;
        
        switch(_version)
        {
            case HttpVersions.HTTP_0_9_ORDINAL:
        
                _close=true;
            	_contentLength=EOF_CONTENT;
            	break;
        
            case HttpVersions.HTTP_1_0_ORDINAL:
                _close=true;
                // fall through to default handling
            
            default:
            {   
                // get a header buffer
                if (_header==null)
                    _header=_buffers.getBuffer(Buffers.SMALL);
                
                // add response line
                Buffer line=HttpStatus.getResponseLine(_status);
                
                if (line==null)
                {
                    _header.put(HttpVersions.HTTP_1_1_BUFFER);
                    _header.put((byte)' ');
                    _header.put((byte)('0'+_status/100));
                    _header.put((byte)('0'+(_status%100)/10));
                    _header.put((byte)('0'+(_status%10)));
                    _header.put((byte)' ');
                    byte[] r=Portable.getBytes(_reason==null?"Uknown":_reason);
                    _header.put(r,0,r.length);
                    _header.put(CRLF);
                }
                else if (_reason!=null)
                {
                    _header.put(line.array(),0,HttpVersions.HTTP_1_1_BUFFER.length()+5);
                    byte[] r=Portable.getBytes(_reason);
                    _header.put(r,0,r.length);
                    _header.put(CRLF);
                }
                else
                    _header.put(line);              
                
                // Add headers  
                
                // key field values
                HttpFields.Field content_type=null;
                HttpFields.Field content_length=null;
                HttpFields.Field transfer_encoding=null;
                HttpFields.Field connection=null;
                boolean keep_alive=false;
                
                if (fields!=null)
                {
                    Iterator iter = fields.getFields();
                    
                    while(iter.hasNext())
                    {
                        HttpFields.Field field=(HttpFields.Field)iter.next();
                        
                        switch(field.getNameOrdinal())
                        {
                            case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                                content_length=field;
                            _contentLength=field.getLongValue();
                            
                            if (_contentLength<_contentAdded || _last && _contentLength!=_contentAdded)
                            {
                                // TODO - warn of incorrect content length
                                content_length=null;
                            }
                            
                            // write the field to the header buffer
                            field.put(_header);
                            break;
                            
                            case HttpHeaders.CONTENT_TYPE_ORDINAL:
                                content_type=field;
                            if (BufferUtil.isPrefix(HttpHeaderValues.MULTIPART_BYTERANGES_BUFFER, field.getValueBuffer()))
                                _contentLength=SELF_DEFINING_CONTENT;
                            
                            // write the field to the header buffer
                            field.put(_header);
                            break;
                            
                            case HttpHeaders.TRANSFER_ENCODING_ORDINAL:
                                if (_version==HttpVersions.HTTP_1_1_ORDINAL)
                                    transfer_encoding=field;
                                // Do NOT add yet!
                            break;
                            
                            case HttpHeaders.CONNECTION_ORDINAL:
                                connection=field;
                            
                            int connection_value = field.getValueOrdinal();
                            
                            // TODO handle multivalue Connection
                            _close=HttpHeaderValues.CLOSE_ORDINAL==connection_value;
                            keep_alive=HttpHeaderValues.KEEP_ALIVE_ORDINAL==connection_value;
                            if (keep_alive && _version == HttpVersions.HTTP_1_0_ORDINAL)
                                _close=false;
                            
                            if (_close && _contentLength==UNKNOWN_CONTENT)
                                _contentLength=EOF_CONTENT;
                            
                            // Do NOT add yet!
                            break;
                            
                            default:
                                // write the field to the header buffer
                                field.put(_header);
                        }
                    }
                }
                
                // Calculate how to end content and connection, content length and transfer encoding settings.
                // From RFC 2616 4.4:
                // 1. No body for 1xx, 204, 304 & HEAD response
                // 2. Force content-length?
                // 3. If Transfer-Encoding!=identity && HTTP/1.1 && !Connection==close then chunk
                // 4. Content-Length
                // 5. multipart/byteranges
                // 6. close
                
                switch((int)_contentLength)
                {
                    case UNKNOWN_CONTENT:
                        // It may be that we have no content, or perhaps content just has not been written yet?
                        
                        // Response known not to have a body
                        if (_contentAdded==0 && (_status<200 || _status==204 || _status==304))
                            _contentLength=NO_CONTENT;
                        else if (_last)
                        {
                            // we have seen all the content there is
                            _contentLength=_contentAdded;
                            if (content_length==null)
                            {
                                // known length but not actually set.
                                _header.put(HttpHeaders.CONTENT_LENGTH_BUFFER);
                                _header.put(COLON);
                                _header.put((byte)' ');
                                BufferUtil.putDecLong(_header,_contentLength);
                                _header.put(CRLF);
                            }   
                        }
                        else   
                            // No idea, so we must assume that a body is coming
                            _contentLength=(_close || _version<HttpVersions.HTTP_1_1_ORDINAL)?EOF_CONTENT:CHUNKED_CONTENT;
                        break;
                        
                    case NO_CONTENT:
                        if(content_length==null && _status>=200 && _status!=204 && _status!=304)    
                            _header.put(CONTENT_LENGTH_0);
                        break;
                        
                    case EOF_CONTENT:
                        _close=true;
                        break;
                        
                    case CHUNKED_CONTENT:
                        break;
                        
                    default:       
                        // TODO - maybe allow forced chunking by setting te ???
                        break;
                }
                
                // Add transfer_encoding if needed
                if (_contentLength==CHUNKED_CONTENT)
                {
                    // try to use user supplied encoding as it may have other values.
                    if (transfer_encoding!=null &&  HttpHeaderValues.CHUNKED_ORDINAL != transfer_encoding.getValueOrdinal())
                    {
                        // TODO avoid string conversion here
                        String c = transfer_encoding.getValue();
                        if (c.endsWith(HttpHeaderValues.CHUNKED))
                            transfer_encoding.put(_header);   
                        else
                            Portable.throwIllegalArgument("BAD TE");
                    }
                    else 
                        _header.put(TRANSFER_ENCODING_CHUNKED);
                    
                }
                
                // Handle connection if need be
                if (_close)
                    _header.put(CONNECTION_CLOSE);
                else if (keep_alive && _version==HttpVersions.HTTP_1_0_ORDINAL)
                    _header.put(CONNECTION_KEEP_ALIVE);
                else if (connection!=null)
                    connection.put(_header);
                
                _header.put(SERVER);
                
                // end the header.
                _header.put(CRLF);
                
            }
        }

        _state=STATE_CONTENT;
        
    }

    /* ------------------------------------------------------------ */
    /**
     * Complete the message.
     * @throws IOException
     */
    public void complete()
    	throws IOException
    {   
        if(_state==STATE_END)
            return;
        
        if (_state==STATE_HEADER)
            Portable.throwIllegalState("State==HEADER");
        
        else if (_contentLength>=0 && _contentLength!=_contentAdded)
        {
            // TODO warning.
            _close=true;
        }
        
        if (_state!=STATE_FLUSHING)
        {
            _state=STATE_FLUSHING;
            if (_contentLength==CHUNKED_CONTENT)
                _needEOC=true;
        }
        flushBuffers();
    }
    
    
    /* ------------------------------------------------------------ */
    public void flushBuffers()
    	throws IOException
    {
        if (_state==STATE_HEADER)
            Portable.throwIllegalState("State==HEADER");
        
        prepareBuffers();
        
        if (_endp==null)
        {
            if (_needCRLF)
                _buffer.put(CRLF);
            if (_needEOC)
                _buffer.put(LAST_CHUNK);
            return;
        }
        
        // Keep flushing while there is something to flush (except break below)
        int last_len=-1;
        Flushing: while(true)
        {
            int len=-1;
            int to_flush = 
                ((_header!=null  && _header.length()>0)?4:0) |
                ((_buffer!=null  && _buffer.length()>0)?2:0) |  
                ((_direct &&_content!=null &&_content.length()>0)?1:0);
            
            switch(to_flush)
            {
                case 7: len=_endp.flush(_header,_buffer,_content); // should never happen!
                break;
                case 6: len=_endp.flush(_header,_buffer,null);
                break;
                case 5: len=_endp.flush(_header,_content,null);
                break;
                case 4: len=_endp.flush(_header);
                break;
                case 3: len=_endp.flush(_buffer,_content,null); // should never happen!
                break;
                case 2: len=_endp.flush(_buffer);
                break;
                case 1: len=_endp.flush(_content);
                break;
                case 0:
                {
                    // Nothing more we can write now.
                    if (_header!=null)
                        _header.clear();
                    
                    if (_buffer!=null)
                    {
                        _buffer.clear();
                        if (_contentLength==CHUNKED_CONTENT)
                        {
                            // reserve some space for the chunk header
                            _buffer.setPutIndex(CHUNK_SPACE);
                            _buffer.setGetIndex(CHUNK_SPACE);
                            _bufferChunked=false;
                            
                            // Special case handling for small left over buffer from 
                            // an addContent that caused a buffer flush.
                            if (_content!=null && _content.length()<_buffer.space() && _state!=STATE_FLUSHING)
                            {
                                _buffer.put(_content);
                                _content=null;
                                break Flushing;
                            }
                        }
                    }
                    
                    _direct=false;
                    
                    // Are we completely finished for now?
                    if (!_needCRLF && !_needEOC && (_content==null || _content.length()==0))
                    {
                        if (_state==STATE_FLUSHING)
                        {
                            _state=STATE_END;
                            if (_close)
                                _endp.close();
                        }
                        
                        break Flushing;
                    }
                    
                    // Try to prepare more to write.
                    prepareBuffers();
                }
            }
            
            // If we failed to flush anything twice in a row break
            if (len<=0)
            {
                if (last_len<=0)
                    break Flushing;
                // TODO ?? else
                //     Thread.yield(); // Give other threads a chance and hopefully we unblock.
                break;
            }
            last_len=len;
        }
        
    }
    

    /* ------------------------------------------------------------ */
    private void prepareBuffers()
    	throws IOException
    {   
        // if we are not flushing an existing chunk
        if (!_bufferChunked)
        {
            // Refill buffer if possible
            if (_content!=null && _content.length()>0 && _buffer!=null && _buffer.space()>0)
            {
                _buffer.put(_content);
                if (_content.length()==0)
                    _content=null;
            }
            
            // Chunk buffer if need be
            if (_contentLength==CHUNKED_CONTENT)
            {
                int size = _buffer.length();
                if (size>0)
                {
                    // Prepare a chunk!
                    _bufferChunked=true;
                    
                    // Did we leave space at the start of the buffer.
                    if (_buffer.getIndex()==CHUNK_SPACE)
                    {
                        // Oh yes, goodie! let's use it then!
                        _buffer.poke(_buffer.getIndex()-2,CRLF,0,2);
                        _buffer.setGetIndex(_buffer.getIndex()-2);
                        BufferUtil.prependHexInt(_buffer, size);
                        
                        if (_needCRLF)
                        {
                            _buffer.poke(_buffer.getIndex()-2,CRLF,0,2);
                            _buffer.setGetIndex(_buffer.getIndex()-2);
                            _needCRLF=false;
                        }
                    }
                    else
                    {
                        // No space so lets use the header buffer.
                        if (_needCRLF)
                        {
                            if (_header.length()>0)
                                Portable.throwIllegalState("EOC");
                            _header.put(CRLF);
                            _needCRLF=false;
                        }
                        BufferUtil.putHexInt(_header, size);
                        _header.put(CRLF);
                    }
                    
                    // Add end chunk trailer.
                    if (_buffer.space()>=2)
                        _buffer.put(CRLF);
                    else
                        _needCRLF=true;
                }
                
                // If we need EOC and everything written 
                if (_needEOC && (_content==null || _content.length()==0))
                {
                    if (_needCRLF && _buffer.space()>=2)
                    {
                        _buffer.put(CRLF);
                        _needCRLF=false;
                    }
                 
                    if (!_needCRLF && _needEOC && _buffer.space()>= LAST_CHUNK.length)
                    {
                        _buffer.put(LAST_CHUNK);
                        _needEOC=false;
                    }
                }
            }
        }
        
        if (_content!=null && _content.length()==0)
            _content=null;
            
    }
    

    /* ------------------------------------------------------------ */
    /** Utility method to send an error response.
     * If the builder is not committed, this call is equivalent to 
     * a setResponse, addcontent and complete call.
     * @param code
     * @param reason
     * @param content
     * @param close
     * @throws IOException
     */
    public void sendError(int code,String reason,String content,boolean close)
    	throws IOException
    {
        if (!isCommitted())
        {
            setResponse(code, reason);
            _close=close;
            if (content!=null)
                addContent(new ByteArrayBuffer(content),HttpBuilder.LAST);
            complete();
        }        
    }
}
