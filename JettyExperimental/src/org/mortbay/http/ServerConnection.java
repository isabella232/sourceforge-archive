/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 21-Mar-2003
 * $Id$
 * ============================================== */
 
package org.mortbay.http;

import org.mortbay.util.io.Buffer;
import org.mortbay.util.io.BufferUtil;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class ServerConnection
{
	private Buffer _method;
	private int _methodIndex;
	
	private Buffer _uri;
	private int _httpVersion;
	private int _contentLength;

	private class RequestParser implements HttpParser.Handler
	{        
        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#foundField0(org.mortbay.util.Buffer)
         */
        public void gotMethodOrVersion(Buffer ref)
        {
        	_methodIndex=HttpMethod.CACHE.lookupIndex(ref);
        	if (_methodIndex>=0)
        	_method=(_methodIndex>=0)?HttpMethod.CACHE.lookupBuffer(_methodIndex):ref;
        }

        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#foundField1(org.mortbay.util.Buffer)
         */
        public void gotUriOrCode(Buffer ref)
        {
        	_uri=(Buffer)(ref.clone()); 
        }

        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#foundField2(org.mortbay.util.Buffer)
         */
        public void gotVersionOrReason(Buffer ref)
        {
        	_httpVersion=9;
        	if (ref.length()>0)
        	{
        		_httpVersion=10;
        		if (ref.length()==8)
        		{
        			_httpVersion+=
        				(ref.peek(ref.offset()+5)-'0')*10+
        				(ref.peek(ref.offset()+7)-'0');
        			if (_httpVersion<10 || _httpVersion>=100)
        				_httpVersion=10;
        		}
        	}
        }

        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#foundHttpHeader(org.mortbay.util.Buffer)
         */
        public void gotHeader(Buffer name, Buffer val)
        {
        	int header = HttpHeader.CACHE.lookupIndex(name);
        	switch(header)
        	{
        		case HttpHeader.CONTENT_LENGTH_INDEX:
        			_contentLength=BufferUtil.toInt(val);
        	}
        	
            // TODO Auto-generated method stub
            
        }


        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#getContentLength()
         */
        public int gotCompleteHeader()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.util.Buffer)
         */
        public void gotContent(int offset, Buffer ref)
        {
            // TODO Auto-generated method stub
            
        }

        /* ------------------------------------------------------------------------------- */
        /**
         * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
         */
        public void gotCompleteMessage(int contextLength)
        {
            // TODO Auto-generated method stub
            
        }
	}
}
