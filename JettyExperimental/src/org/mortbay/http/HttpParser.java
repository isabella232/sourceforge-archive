package org.mortbay.http;

import org.mortbay.util.Portable;
import org.mortbay.util.io.Buffer;
import org.mortbay.util.io.ByteArrayBuffer;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpParser
{
    // Terminal symbols.
    static final byte COLON= (byte)':';
    static final byte SEMI_COLON= (byte)';';
    static final byte SPACE= 0x20;
    static final byte CARRIAGE_RETURN= 0x0D;
    static final byte LINE_FEED= 0x0A;
    static final byte TAB= 0x09;

    // States
    public static final int STATE_START= -9;
    public static final int STATE_FIELD0= -8;
    public static final int STATE_SPACE1= -7;
    public static final int STATE_FIELD1= -6;
    public static final int STATE_SPACE2= -5;
    public static final int STATE_FIELD2= -4;
    public static final int STATE_HEADER= -3;
    public static final int STATE_HEADER_NAME= -2;
    public static final int STATE_HEADER_VALUE= -1;
    public static final int STATE_END= 0;
    public static final int STATE_EOF_CONTENT= 1;
    public static final int STATE_CONTENT= 2;
    public static final int STATE_CHUNKED_CONTENT= 3;
    public static final int STATE_CHUNK_SIZE= 4;
    public static final int STATE_CHUNK_PARAMS= 5;
    public static final int STATE_CHUNK= 6;

    public static final int CHUNKED_CONTENT= -2;
    public static final int EOF_CONTENT= -1;
    public static final int NO_CONTENT= 0;

    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    private HttpParser()
    {
    }

    /* ------------------------------------------------------------------------------- */
    /** parse.
     * @param handler
     * @param source
     * @return
     */
    public static int parse(Handler handler, Buffer source)
    {
        /* Initialize global variables. */
        source.mark(-1);
        Context ctx= new Context();

        // Parse stream
        while (ctx.state != STATE_END)
        {
            parseBuffer(handler, source, ctx);
            source.compact();
            int filled= source.fill();
            if (filled < 0 && ctx.state == STATE_EOF_CONTENT)
            {
                ctx.state= STATE_END;
                handler.messageComplete(ctx.contentOffset);
            }

            if (filled <= 0)
                break;
        }

        // Exception if we are not at a finishing state.
        if (ctx.state != STATE_END)
        {
            Portable.throwIllegalState("Unexpected end of stream: " + source);
        }

        return ctx.state;
    }

    /**
     * Method parseBuffer.
     * @param handler
     * @param buf
     * @param ctx
     */
    protected static void parseBuffer(Handler handler, Buffer source, Context ctx)
    {
        if (ctx == null)
            ctx= new Context();

        byte ch;

        // Handler header
        while (ctx.state < STATE_END && source.available() > 0)
        {
            ch= source.get();
            if (ctx.eol == CARRIAGE_RETURN && ch == LINE_FEED)
            {
                ctx.eol= LINE_FEED;
                continue;
            }
            ctx.eol= 0;

            switch (ctx.state)
            {
                case STATE_START :
                    if (ch > SPACE)
                    {
                        source.markOffset();
                        ctx.state= STATE_FIELD0;
                    }
                    break;

                case STATE_FIELD0 :
                    if (ch == SPACE)
                    {
                        handler.gotMethodOrVersion(source.marked());
                        ctx.state= STATE_SPACE1;
                    }
                    else if (ch < SPACE)
                    {
                        handler.headerComplete();
                        handler.messageComplete(ctx.contentOffset);
                        ctx.state= STATE_END;
                    }
                    break;

                case STATE_SPACE1 :
                    if (ch > SPACE)
                    {
                        source.markOffset();
                        ctx.state= STATE_FIELD1;
                    }
                    else if (ch < SPACE)
                        throw new RuntimeException(ctx.toString(source));
                    break;

                case STATE_FIELD1 :
                    if (ch == SPACE)
                    {
                        handler.gotUriOrCode(source.marked());
                        ctx.state= STATE_SPACE2;
                    }
                    else if (ch < SPACE)
                        throw new RuntimeException(ctx.toString(source));
                    break;

                case STATE_SPACE2 :
                    if (ch > SPACE)
                    {
                        source.markOffset();
                        ctx.state= STATE_FIELD2;
                    }
                    else if (ch < SPACE)
                        throw new RuntimeException(ctx.toString(source));
                    break;

                case STATE_FIELD2 :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        handler.gotVersionOrReason(source.marked());
                        ctx.eol= ch;
                        ctx.length=-1;
                        ctx.state= STATE_HEADER;
                    }
                    break;

                case STATE_HEADER :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                    	if (ctx.length>0 && ctx.name.length()>0)
                    	{
                    		handler.gotHeader(ctx.name,source.marked(ctx.length));
                    		ctx.name.clear();
                    		ctx.length=-1;
                    	}
                    	
                        handler.headerComplete();
                        ctx.contentLength= handler.getContentLength();
                        ctx.contentOffset= 0;
                        ctx.eol= ch;
                        switch (ctx.contentLength)
                        {
                            case HttpParser.EOF_CONTENT :
                                ctx.state= STATE_EOF_CONTENT;
                                break;
                            case HttpParser.CHUNKED_CONTENT :
                                ctx.state= STATE_CHUNKED_CONTENT;
                                break;
                            case HttpParser.NO_CONTENT :
                                handler.messageComplete(ctx.contentOffset);
                                ctx.state= STATE_END;
                                break;
                            default :
                                ctx.state= STATE_CONTENT;
                                break;
                        }
                    }
                    else if (ch == SPACE || ch == TAB)
                    {
                        ctx.state= STATE_HEADER_VALUE;
                    }
                    else
                    {
						if (ctx.name.length()>0)
						{
							if (ctx.length>=0)
								handler.gotHeader(ctx.name,source.marked(ctx.length));
							else
								handler.gotHeader(ctx.name,null);
						} 
						ctx.name.clear();
                        ctx.length= 1;
                        source.markOffset();
                        ctx.state= STATE_HEADER_NAME;
                    }
                    break;

                case STATE_HEADER_NAME :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (ctx.length > 0)
                        {
                        	ctx.name.mimic(source.marked(ctx.length));
                        	ctx.length=-1;
                        }
                        ctx.eol= ch;
                        ctx.state= STATE_HEADER;
                    }
                    else if (ch == COLON)
                    {
                        if (ctx.length > 0)
							ctx.name.mimic(source.marked(ctx.length));
                        ctx.length= -1;
                        ctx.state= STATE_HEADER_VALUE;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (ctx.length == -1)
                            source.markOffset();
                        ctx.length= source.offset() - source.mark();
                    }
                    break;

                case STATE_HEADER_VALUE :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        ctx.eol= ch;
                        ctx.state= STATE_HEADER;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (ctx.length == -1)
                            source.markOffset();
                        ctx.length= source.offset() - source.mark();
                    }
                    break;
            }
        }

        // Handle content
        Buffer chunk;
        while (ctx.state > STATE_END && source.available() > 0)
        {	
            if (ctx.eol == CARRIAGE_RETURN && source.peek() == LINE_FEED)
            {
                ctx.eol= source.get();
                continue;
            }
            ctx.eol= 0;

            switch (ctx.state)
            {
                case STATE_EOF_CONTENT :
                    chunk= source.get(-1);
                    handler.gotContent(ctx.contentOffset, chunk);
                    ctx.contentOffset += chunk.length();
                    break;

                case STATE_CONTENT :
                    {
                        int length= source.available();
                        int remaining= ctx.contentLength - ctx.contentOffset;
                        if (remaining == 0)
                        {
                            ctx.state= STATE_END;
                            handler.messageComplete(ctx.contentOffset);
                            break;
                        }
                        else if (length > remaining)
                            length= remaining;
                        chunk= source.get(length);
                        handler.gotContent(ctx.contentOffset, chunk);
                        ctx.contentOffset += chunk.length();
                    }
                    break;

                case STATE_CHUNKED_CONTENT :
                    ch= source.peek();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                        ctx.eol= source.get();
                    else if (ch <= SPACE)
                        source.get();
                    else
                    {
						ctx.chunkLength=0;
						ctx.chunkOffset=0;
                        ctx.state= STATE_CHUNK_SIZE;
                    }
                    break;

                case STATE_CHUNK_SIZE :
                    ch= source.get();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        ctx.eol= ch;
						if (ctx.chunkLength==0)
						{
							ctx.state=STATE_END;
							handler.messageComplete(ctx.contentOffset);
						}
						else
							ctx.state= STATE_CHUNK;
                    }
                    else if (ch <= SPACE || ch == SEMI_COLON)
                        ctx.state= STATE_CHUNK_PARAMS;
                    else if (ch >= '0' && ch <= '9')
                        ctx.chunkLength= ctx.chunkLength * 16 + (ch - '0');
                    else if (ch >= 'a' && ch <= 'f')
                        ctx.chunkLength= ctx.chunkLength * 16 + (10+ch - 'a');
                    else if (ch >= 'A' && ch <= 'F')
                        ctx.chunkLength= ctx.chunkLength * 16 + (10+ch - 'A');
                    else
                        Portable.throwRuntime("bad chunk char: " + ch);
                        
                    break;

                case STATE_CHUNK_PARAMS :
                    ch= source.get();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        ctx.eol= ch;
                        if (ctx.chunkLength==0)
                        {
                        	ctx.state=STATE_END;
                        	handler.messageComplete(ctx.contentOffset);
                        }
                        else
	                        ctx.state= STATE_CHUNK;
                    }
                    break;

                case STATE_CHUNK :
                    {
                        int length= source.available();
                        int remaining= ctx.chunkLength - ctx.chunkOffset;
                        if (remaining == 0)
                        {
                            ctx.state= STATE_CHUNKED_CONTENT;
                            break;
                        }
                        else if (length > remaining)
                            length= remaining;
                        chunk= source.get(length);
                        handler.gotContent(ctx.contentOffset, chunk);
                        ctx.contentOffset += chunk.length();
                        ctx.chunkOffset += chunk.length();
                    }
                    break;
            }
        }
    }

    /**
     * @author gregw
     */
    protected static class Context
    {
        public int state= STATE_START;
        public byte eol;
        public int length;
        public int contentLength;
        public int contentOffset;
        public int chunkLength;
        public int chunkOffset;
        public ByteArrayBuffer name = new ByteArrayBuffer(null,0,0,true);

        private String toString(Buffer buf)
        {
        	return "state="+state+" length="+length;
        }
    }

    /**
     * @author gregw
     */
    public interface Handler
    {
        /**
         * This is the method called by parser when the HTTP request method or response version is found
         */
        public abstract void gotMethodOrVersion(Buffer ref);

        /**
         * This is the method called by parser when HTTP request URI or response code is found
         */
        public abstract void gotUriOrCode(Buffer ref);

        /**
         * This is the method called by parser when HTTP request version or response reason is found
         */
        public abstract void gotVersionOrReason(Buffer ref);

        /**
         * This is the method called by parser when A HTTP Header name is found
         */
        public abstract void gotHeader(Buffer name, Buffer value);

        public abstract void headerComplete();

        public abstract int getContentLength();

        public abstract void gotContent(int offset, Buffer ref);

        public abstract void messageComplete(int contextLength);

    }
}
