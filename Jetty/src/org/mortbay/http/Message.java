// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface Message
{
    public InputStream getInputStream() throws IOException;
    public OutputStream getOutputStream() throws IOException;
    
    public Enumeration getFieldNames();
    public Enumeration getFieldValues(String name);
    public Enumeration getFieldValues(String name, String separators);
    
    public boolean containsField(String name);
    public String getField(String name);
    public int getIntField(String name);
    public long getDateField(String name);
    
    public String setField(String name, String value);
    public void setField(String name, List values);
    public void setIntField(String name, int value);
    public void setDateField(String name, long date);
    
    public void addField(String name, String value);
    public void addIntField(String name, int value);
    public void addDateField(String name, long date);

    public String removeField(String name);

    public String getContentType();
    public void setContentType(String type);

    public int getContentLength();
    public void setContentLength(int len);
    
    public String getCharacterEncoding();
    public void setCharacterEncoding(String encoding);
    
    public Object getAttribute(String name);
    public Object setAttribute(String name, Object attribute);
    public Enumeration getAttributeNames();
    public void removeAttribute(String name);
    
}

