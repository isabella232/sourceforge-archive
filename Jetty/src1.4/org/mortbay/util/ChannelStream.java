/*
 * ChannelStream.java
 * $Id$
 */

package org.mortbay.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;


/**
 * Provider for Channel utilities.
 * @author  Jan Hlavatý
 */
public abstract class ChannelStream {
    
    public static class OutputChannelStream extends OutputStream {
        
        WritableByteChannel _channel = null;
        byte[] _onebyte = null;
        byte[] _cached_array = null;
        ByteBuffer _cached_buffer = null;
        
        
        OutputChannelStream(WritableByteChannel ch) {
            _channel = ch;
        }
        
        public synchronized void write(int param) throws java.io.IOException {
            if (_onebyte == null) {
                _onebyte = new byte[1];
            }
            _onebyte[0] = (byte) param;
            this.write(_onebyte);
        }
        
        public synchronized void write(byte[] b, int offset, int length) throws java.io.IOException {
            if (b != _cached_array) {
                _cached_array = b;
                _cached_buffer = ByteBuffer.wrap(b);
            }
            _cached_buffer.limit(offset+length);
            _cached_buffer.position(offset);
            ChannelStream.write(_channel, _cached_buffer);
        }
    }
    
    static int write(WritableByteChannel ch, ByteBuffer bb) throws IOException {
    	if (ch instanceof SelectableChannel) {
    	    SelectableChannel sc = (SelectableChannel)ch;
    	    synchronized (sc.blockingLock()) {
    		if (!sc.isBlocking())
    		    throw new IllegalBlockingModeException();
    		return ch.write(bb);
    	    }
    	} else {
    	    return ch.write(bb);
    	}
    }
    
    public static OutputStream newOutputStream(WritableByteChannel ch) {
        return new ChannelStream.OutputChannelStream(ch);
    }
}
