// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

public class OracleAdaptor extends DbAdaptor
{
  /* ------------------------------------------------------------ */
  /**
   * Constructor.
   */
  public OracleAdaptor ()
  {
    dbDriver="oracle.jdbc.driver.OracleDriver";
  }

  /* ------------------------------------------------------------ */
  /**
   * Quote a string value for the database.
   * @param s A string
   * @return The string in quotes with any internal special
   * characters handled.
   */
  public String quote (String s)
  {
    StringBuffer b = new StringBuffer ();
    b.append ("'");

    int i = 0;
    int last = -1;

    while ((i = s.indexOf ("'", i)) != -1)
    {
      b.append (s.substring (last + 1, i));
      b.append ("\\'");
      last = i++;
    }

    if (s.length () > last + 1)
      b.append (s.substring (last + 1));

    b.append ("'");
    s = b.toString ();
    return s;
  }

  /* ------------------------------------------------------------ */
  /**
   * The go string
   * @return The string used by the underlying database to trigger
   * handling of meta data commands. Default is "go".
   */
  public String go ()
  {
    return ";\n";
  }

};
