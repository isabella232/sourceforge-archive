// ========================================================================
// Copyright (c) 1996 Intelligent Switched Systems, Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.util.*;

/** Create Database tables
 * <p> Produces output suitable for piping to a sql cli to drop and recreate
 * db tables.
 * <p><h4>Notes</h4>
 * <p> The startable arg should be a Hashtable, where go maps to the string
 * to pass to the interpreter to execute a sql command and tables is a list of
 * classnames that extend Table to create.
 *
 * @version $Id$
 * @author Matthew Watson
*/
public class CreateTables
{
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        if (args.length<=1)
        {
            System.err.println("Usage - java [environment] com.mortbay.JDBC.CreateTables DbAdaptor TableClassName ...");
            System.exit(1);
        }

        String step = null;
        try {
            String dbAdaptor = args[0];
            step = "Instantiate DbAdaptor "+dbAdaptor;
            Code.debug(step);
            Database database = new Database(dbAdaptor, null);
            Class[] dbArg = {com.mortbay.JDBC.Database.class};
            Object[] dbParam = {database};
        
            for (int a=1; a<args.length; a++)
            {
                String arg = args[a];
                step = "Find class Table "+arg;
                Code.debug(step);
                
                Class tableClass = Class.forName(arg);
                
                step = "Find constructor "+arg+".constructor(DataBase)";
                Code.debug(step);
                java.lang.reflect.Constructor constructor =
                    tableClass.getConstructor(dbArg);

                step = "Call constructor "+arg+".constructor(DataBase)";
                Code.debug(step);
                Table table = (Table)constructor.newInstance(dbParam);
                
                step = "Drop Table "+arg;
                Code.debug(step);
                System.out.print(table.drop());
                step = "Create Table "+arg;
                Code.debug(step);
                System.out.print(table.create());
                System.out.flush();
            }
        }
        catch (java.lang.reflect.InvocationTargetException e){
            Code.warning("Problem with "+step,e.getTargetException());
        }
        catch (Exception e){
            Code.warning("Problem with "+step,e);
        }
    }
    /* ------------------------------------------------------------ */
};

