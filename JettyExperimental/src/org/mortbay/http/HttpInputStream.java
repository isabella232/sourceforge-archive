
package org.mortbay.http;

import java.io.IOException;
import java.io.InputStream;

import org.mortbay.io.Buffer;

/**
 *
 */
public class HttpInputStream extends InputStream 
{
	private Buffer _buffer;
	private Parser _parser=new Parser();
	
    /**
     * 
     */
    public HttpInputStream(Buffer buffer) 
    {
    	_buffer=buffer;
    	_parser=new Parser();
		_parser.parse(_buffer);
    }

    /*
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }


    /**
     * 
     */
    private static class Parser extends HttpParser
    {
    	Buffer  _header;
    	int _contentLength;
    	
        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.io.Buffer)
         */
        public void foundContent(int index, Buffer ref)
        {
            // TODO Auto-generated method stub
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundField0(org.mortbay.io.Buffer)
         */
        public void foundField0(Buffer ref)
        {
        	// TODO
        	String method = HttpMethod.CACHE.normalize(ref).toString();
        	System.err.println("method="+method);
        	_contentLength=HttpParser.NO_CONTENT;
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundField1(org.mortbay.io.Buffer)
         */
        public void foundField1(Buffer ref)
        {
        	// TODO
        	String uri = ref.toString();
        	System.err.println("uri="+uri);
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundField2(org.mortbay.io.Buffer)
         */
        public void foundField2(Buffer ref)
        {
        	// TODO
        	String version=HttpVersion.CACHE.normalize(ref).toString();
			System.err.println("version="+version);
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundHttpHeader(org.mortbay.io.Buffer)
         */
        public void foundHttpHeader(Buffer ref)
        {
            // TODO
			System.err.println("header="+ref);
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundHttpValue(org.mortbay.io.Buffer)
         */
        public void foundHttpValue(Buffer ref)
        {
            // TODO Auto-generated method stub
			String value = ref.toString();
			System.err.println("value="+value);
			
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#getContentLength()
         */
        public int getContentLength()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#headerComplete()
         */
        public void headerComplete()
        {
            // TODO Auto-generated method stub

        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
         */
        public void messageComplete(int contextLength)
        {
            // TODO Auto-generated method stub

        }

}	
}
