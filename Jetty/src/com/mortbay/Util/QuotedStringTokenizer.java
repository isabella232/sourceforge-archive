// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/* ------------------------------------------------------------ */
/** StringTokenizer with Quoting 
 *
 * This class is a copy of the java.util.StringTokenizer API and
 * the behaviour is the same, except that single and doulbe quoted
 * string values are recognized.
 * Delimiters within quotes are not considered delimiters.
 * Quotes can be escaped with '\'.
 *
 * @see java.util.Stringtokenizer
 * @version 1.0 Wed Sep 29 1999
 * @author Greg Wilkins (gregw)
 */
public class QuotedStringTokenizer
    extends StringTokenizer
{
    private String _string;
    private String _delim = "\t\n\r";
    private boolean _returnQuotes=false;
    private boolean _returnTokens=false;
    private StringBuffer _token;
    private boolean _hasToken=false;
    private int _i=0;
    private int _lastStart=0;
    
    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str,
                                 String delim,
                                 boolean returnTokens,
                                 boolean returnQuotes)
    {
        super("");
        _string=str;
        if (delim!=null)
            _delim=delim;
        _returnTokens=returnTokens;
        _returnQuotes=returnQuotes;
        
        if (_delim.indexOf("'")>=0 ||
            _delim.indexOf("\"")>=0)
            throw new Error("Can't use quotes as delimiters: "+_delim);
        
        _token=new StringBuffer(_string.length()>1024?512:_string.length()/2);
    }

    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str,
                                 String delim,
                                 boolean returnTokens)
    {
        this(str,delim,returnTokens,false);
    }
    
    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str,
                                 String delim)
    {
        this(str,delim,false,false);
    }

    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str)
    {
        this(str,null,false,false);
    }

    /* ------------------------------------------------------------ */
    public boolean hasMoreTokens()
    {
        // Already found a token
        if (_hasToken)
            return true;
        
        _lastStart=_i;
        
        int state=0;
        boolean escape=false;
        while (_i<_string.length())
        {
            char c=_string.charAt(_i++);
            
            switch (state)
            {
              case 0: // Start
                  if(_delim.indexOf(c)>=0)
                  {
                      if (_returnTokens)
                      {
                          _token.append(c);
                          return _hasToken=true;
                      }
                  }
                  else if (c=='\'')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      state=2;
                  }
                  else if (c=='\"')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      state=3;
                  }
                  else
                  {
                      _token.append(c);
                      _hasToken=true;
                      state=1;
                  }
                  continue;
                  
              case 1: // Token
                  _hasToken=true;
                  if(_delim.indexOf(c)>=0)
                  {
                      if (_returnTokens)
                          _i--;
                      return _hasToken;
                  }
                  else if (c=='\'')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      state=2;
                  }
                  else if (c=='\"')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      state=3;
                  }
                  else
                      _token.append(c);
                  continue;

                  
              case 2: // Single Quote
                  _hasToken=true;
                  if (escape)
                  {
                      escape=false;
                      _token.append(c);
                  }
                  else if (c=='\'')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      state=1;
                  }
                  else if (c=='\\')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      escape=true;
                  }
                  else
                      _token.append(c);
                  continue;

                  
              case 3: // Double Quote
                  _hasToken=true;
                  if (escape)
                  {
                      escape=false;
                      _token.append(c);
                  }
                  else if (c=='\"')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      state=1;
                  }
                  else if (c=='\\')
                  {
                      if (_returnQuotes)
                          _token.append(c);
                      escape=true;
                  }
                  else
                      _token.append(c);
                  continue;
            }
        }

        return _hasToken;
    }

    /* ------------------------------------------------------------ */
    public String nextToken()
        throws NoSuchElementException 
    {
        if (!hasMoreTokens() || _token==null)
            throw new NoSuchElementException();
        String t=_token.toString();
        _token.setLength(0);
        _hasToken=false;
        return t;
    }

    /* ------------------------------------------------------------ */
    public String nextToken(String delim)
        throws NoSuchElementException 
    {
        _delim=delim;
        _i=_lastStart;
        _token.setLength(0);
        _hasToken=false;
        return nextToken();
    }

    /* ------------------------------------------------------------ */
    public boolean hasMoreElements()
    {
        return hasMoreTokens();
    }

    /* ------------------------------------------------------------ */
    public Object nextElement()
        throws NoSuchElementException 
    {
        return nextToken();
    }

    /* ------------------------------------------------------------ */
    /** Not implemented
     */
    public int countTokens()
    {
        return -1;
    }
}












