package org.mortbay.http;

import org.mortbay.util.Buffer;
import org.mortbay.util.BufferView;
import org.mortbay.util.Portable;

/**
 * @author gregw
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class HttpParser
{
    // Terminal symbols.
    static final byte COLON= (byte)':';
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
    public static final int STATE_CHUNK= 4;

    public static final int CHUNKED_CONTENT= -2;
    public static final int EOF_CONTENT= -1;
    public static final int NO_CONTENT= 0;

    /** Constructor. 
     * @param handler 
     */
    private HttpParser()
    {
    }

    /** 
     * @param buf 
     * @exception ParserException 
     */
    public static int parse(Handler handler, BufferView source)
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
    protected static void parseBuffer(
        Handler handler,
        BufferView source,
        Context ctx)
    {
        if (ctx == null)
            ctx= new Context();

        // Handler header
        while (ctx.state < STATE_END && source.available() > 0)
        {
            byte ch= source.get();
            if (ctx.eol == CARRIAGE_RETURN && ch == LINE_FEED)
            {
            	ctx.eol=LINE_FEED;
                continue;
            }
            ctx.eol= 0;

            switch (ctx.state)
            {
                case STATE_START :
                    if (ch > SPACE)
                    {
                        source.markPosition(-1);
                        ctx.state= STATE_FIELD0;
                    }
                    break;

                case STATE_FIELD0 :
                    if (ch == SPACE)
                    {
                        handler.foundField0(source.marked());
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
                        source.markPosition(-1);
                        ctx.state= STATE_FIELD1;
                    }
                    else if (ch < SPACE)
                        throw new RuntimeException(ctx.toString(source));
                    break;

                case STATE_FIELD1 :
                    if (ch == SPACE)
                    {
                        handler.foundField1(source.marked());
                        ctx.state= STATE_SPACE2;
                    }
                    else if (ch < SPACE)
                        throw new RuntimeException(ctx.toString(source));
                    break;

                case STATE_SPACE2 :
                    if (ch > SPACE)
                    {
                        source.markPosition(-1);
                        ctx.state= STATE_FIELD2;
                    }
                    else if (ch < SPACE)
                        throw new RuntimeException(ctx.toString(source));
                    break;

                case STATE_FIELD2 :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        handler.foundField2(source.marked());
                        ctx.eol= ch;
                        ctx.state= STATE_HEADER;
                    }
                    break;

                case STATE_HEADER :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
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
                    else if (ch == COLON || ch == SPACE || ch == TAB)
                    {
                        ctx.trimLength= -1;
                        ctx.state= STATE_HEADER_VALUE;
                    }
                    else
                    {
                        ctx.trimLength= 1;
                        source.markPosition(-1);
                        ctx.state= STATE_HEADER_NAME;
                    }
                    break;

                case STATE_HEADER_NAME :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (ctx.trimLength > 0)
                            handler.foundHttpHeader(
                                source.marked(ctx.trimLength));
                        ctx.eol= ch;
                        ctx.state= STATE_HEADER;
                    }
                    else if (ch == COLON)
                    {
                        if (ctx.trimLength > 0)
                            handler.foundHttpHeader(
                                source.marked(ctx.trimLength));
                        ctx.trimLength= -1;
                        ctx.state= STATE_HEADER_VALUE;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (ctx.trimLength == -1)
                            source.markPosition(-1);
                        ctx.trimLength= source.position() - source.mark();
                    }
                    break;

                case STATE_HEADER_VALUE :
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (ctx.trimLength > 0)
                            handler.foundHttpValue(
                                source.marked(ctx.trimLength));

                        ctx.eol= ch;
                        ctx.state= STATE_HEADER;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (ctx.trimLength == -1)
                            source.markPosition(-1);
                        ctx.trimLength= source.position() - source.mark();
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
				ctx.eol=source.get();
                continue;
            }
			ctx.eol= 0;

            switch (ctx.state)
            {
                case STATE_EOF_CONTENT :
                    chunk= source.get(-1);
                    handler.foundContent(ctx.contentOffset, chunk);
                    ctx.contentOffset += chunk.length();
                    break;

                case STATE_CONTENT :
                    int length= source.available();
                    int remaining= ctx.contentLength - ctx.contentOffset;
                    if (remaining == 0)
                    {
                        ctx.state= STATE_END;
                        break;
                    }
                    else if (length > remaining)
                        length= remaining;
                    System.err.println(length);
                    chunk= source.get(length);
                    handler.foundContent(ctx.contentOffset, chunk);
                    ctx.contentOffset += chunk.length();

                    break;

                case STATE_CHUNKED_CONTENT :
                    break;

                case STATE_CHUNK :
                    break;
            }
        }
    }

    protected static class Context
    {
        public int state= STATE_START;
        public byte eol;
        public int trimLength;
        public int contentLength;
        public int contentOffset;

        private String toString(BufferView buf)
        {
            return "CTX";
        }
    }

    public interface Handler
    {
        /**
         * This is the method called by parser when the HTTP version is found
         */
        public abstract void foundField0(Buffer ref);

        /**
         * This is the method called by parser when HTTP response code is found
         */
        public abstract void foundField1(Buffer ref);

        /**
         * This is the method called by parser when HTTP response reason is found
         */
        public abstract void foundField2(Buffer ref);

        /**
         * This is the method called by parser when A HTTP Header name is found
         */
        public abstract void foundHttpHeader(Buffer ref);

        /**
         * This is the method called by parser when a HTTP Header value is found
         */
        public abstract void foundHttpValue(Buffer ref);

        public abstract void headerComplete();

        public abstract int getContentLength();

        public abstract void foundContent(int offset, Buffer ref);

        public abstract void messageComplete(int contextLength);

    }
}
