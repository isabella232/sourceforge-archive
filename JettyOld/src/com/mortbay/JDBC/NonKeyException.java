// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================


package com.mortbay.JDBC;

/* ---------------------------------------------------------------- */
public class NonKeyException extends Exception
{
    final public Column column;
    
    public NonKeyException(Column c)
    {
        super("Column "+c.getName()+" is not a key column");
        column=c;
    }
}

