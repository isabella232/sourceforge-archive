// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.InterruptedException;

// =======================================================================
public class SmtpMail implements Runnable 
{
    // -------------------------------------------------------------------
    Thread thread;
    String to;
    String from;
    String subject;
    Vector ccList;
    StringWriter buffer;
    PrintWriter bodyOut;
    String mailhost;
    boolean error;

    // -------------------------------------------------------------------
    public SmtpMail(String to, String from, String subject) 
    {
        this.to      = to;
        this.from    = from;
        this.subject = subject;
        buffer       = new StringWriter();
        bodyOut      = new PrintWriter(buffer);
        ccList       = new Vector();

        mailhost = System.getProperty("MAILHOST");
        if (mailhost==null || mailhost.length()==0)
            mailhost = "mailhost";
    }
  
    // ---------------------------------------------------------------------
    final public  void cc(String addr) 
    {
        if (addr!=null)
        {
            
            StringTokenizer tokenizer
            = new StringTokenizer(addr, ", \t\n\r", false);

            while ((tokenizer.hasMoreTokens()))
            {
                String t = tokenizer.nextToken();
                System.err.println("CC: "+t);
                ccList.addElement(t);
            }
        }
    }  
  
    // ---------------------------------------------------------------------
    final public  void mailhost(String host) 
    {
        mailhost=host;
    }

    // ---------------------------------------------------------------------
    final public void send( ) 
    {   
        thread = new Thread(this,"SMTP mailer");
        thread.start();
    }
    
    // ---------------------------------------------------------------------
    final public void join( )
        throws InterruptedException
    {   
        thread.join();
    }
  
    // ---------------------------------------------------------------------
    final public PrintWriter body( ) 
    {
        return bodyOut;
    }
  

    // ---------------------------------------------------------------------
    final public void run( ) 
    {
        error = false;
        try
        {       
            Code.debug("Mailhost:"+mailhost);
            Socket socket = new Socket(mailhost,25);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            
            String ccAddr = new String();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IO.copyThread(socket.getInputStream(),bout);
            
            String s="HELO "+InetAddress.getLocalHost().getHostName();
            Code.debug(s);
            out.println(s);
            
            s="MAIL FROM: <"+from+">";
            Code.debug(s);
            out.println(s);
            
            s="RCPT TO: <"+to+">";
            Code.debug(s);
            out.println(s);
            
            Thread.yield();
            
            for (int i=0; i < ccList.size() ; i++)
            {
                s="RCPT TO: <"+ccList.elementAt(i)+">";
                Code.debug(s);
                out.println(s);
                
                ccAddr += ccList.elementAt(i).toString() + ' ';
            }

            s="DATA";
            Code.debug(s);
            out.println(s);

            s="To: "+to;
            Code.debug(s);
            out.println(s);
            
            if (ccAddr.length()>0)
            {
                s="Cc: "+ccAddr;
                Code.debug(s);
                out.println(s);
            }
            
            Thread.yield();
            
            s="From: "+from;
            Code.debug(s);
            out.println(s);
            s="Subject: "+subject+"\n";
            Code.debug(s);
            out.println(s);
            if (Code.verbose())
                Code.debug(buffer.toString());
            else
                Code.debug("BODY");
            out.print(buffer.toString());
            
            Code.debug(".\nQUIT");
            out.println();
            out.println(".");
            out.println("QUIT");
            out.flush();
            Thread.sleep(2000);
            if (Code.debug())
                Code.debug(bout.toString());
            
            out.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            error = true;
        }
    }
    // ---------------------------------------------------------------------
    public boolean doitNow(){
        run();
        return !error;
    }


    // ---------------------------------------------------------------------
    public static void main(String[] args)
    {
        if (args.length < 2)
        {
            System.err.println("Usage - java com.mortbay.Util.SmtpMail <toAddr> <subject>");
            System.exit(1);
        }

        try{
            SmtpMail m = new SmtpMail(args[0],"SmtpMail",args[1]);

            IO.copy(new InputStreamReader(System.in),
                    m.body());

            m.send();
            m.join();
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
    }
}





