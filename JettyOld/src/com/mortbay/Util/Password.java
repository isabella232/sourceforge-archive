// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import com.mortbay.Base.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Get a password 
 *
 * This utility class gets a password or pass phrase either by:<PRE>
 *  + Password is set as a system property.
 *  + The password is prompted for and read from standard input
 *  + A program is run to get the password.
 * </pre>
 * Passwords that begin with EXEC: are interpreted as a command, whose
 * output is read.
 * <p>
 * Passwords that begin with OBF: are de obfiscated.
 * Passwords can be obfiscated by run com.mortbay.Util.Password as a
 * main class.
 * @see
 * @version 1.0 Thu Aug 17 2000
 * @author Greg Wilkins (gregw)
 */
public class Password
{
    private String pw;
    private char[] pwc;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param name 
     */
    public Password(String name)
    {
	this(name,"",null);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param name 
     */
    public Password(String name,String dft,String promptDft)
    {
	pw=System.getProperty(name,dft);
	if (pw==null || pw.length()==0)
	{
	    try
	    {
		System.out.print(name+
				 ((promptDft!=null && promptDft.length()>0)
				  ?" [dft]":"")+" : ");
		System.out.flush();
		byte[] buf = new byte[512];
		int len=System.in.read(buf);
		pw=new String(buf,0,len).trim();
	    }
	    catch(IOException e)
	    {
		Code.warning(e);
	    }
	    if (pw==null || pw.length()==0)
		pw=promptDft;
	}

	// expand password
	while (pw!=null && pw.startsWith("EXEC:"))
	    pw=expand(name,pw.substring(5).trim());

	while (pw!=null && pw.startsWith("OBF:"))
	    pw=deobfiscate(pw);
	pwc = pw.toCharArray();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @param pass 
     * @return 
     */
    private String expand(String name, String pass)
    {
	Process process=null;
	try{
	    process = Runtime.getRuntime().exec(pass);
	    OutputStream out = process.getOutputStream();
	    out.write((name+"\n").getBytes());
	    out.flush();
	    InputStream in = process.getInputStream();
	    byte[] buf = new byte[512];
	    int len=in.read(buf);
	    pass=new String(buf,0,len).trim();
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
	finally
	{
	    if (process!=null)
		process.destroy();
	}
	System.err.println("PW="+pass);
	return pass;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
	return pw;
    }
    
    /* ------------------------------------------------------------ */
    public String toStarString()
    {
	return "********************************************************************************".substring(0,pw.length());
    }

    /* ------------------------------------------------------------ */
    public char[] getCharArray()
    {
	return pwc;
    }

    /* ------------------------------------------------------------ */
    public void zero()
    {
	pw=null;
	java.util.Arrays.fill(pwc,'\0');
	pwc=null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @param s 
     * @return 
     */
    private static String obfiscate(String s)
    {
	StringBuffer buf = new StringBuffer();
	byte[] b = s.getBytes();
	
	synchronized(buf)
	{
	    buf.append("OBF:");
	    for (int i=0;i<b.length;i++)
	    {
		byte b1 = b[i];
		byte b2 = b[s.length()-(i+1)];
		int i1= (int)b1+(int)b2+127;
		int i2= (int)b1-(int)b2+127;
		int i0=i1*256+i2;
		String x=Integer.toString(i0,36);

		switch(x.length())
		{
		  case 1:buf.append('0');
		  case 2:buf.append('0');
		  case 3:buf.append('0');
		  default:buf.append(x);
		}
	    }
	    return buf.toString();
	}
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @param s 
     * @return 
     */
    private static String deobfiscate(String s)
    {
	if (s.startsWith("OBF:"))
	    s=s.substring(4);
	
	byte[] b=new byte[s.length()/2];
	int l=0;
	for (int i=0;i<s.length();i+=4)
	{
	    String x=s.substring(i,i+4);
	    int i0 = Integer.parseInt(x,36);
	    int i1=(i0/256);
	    int i2=(i0%256);
	    b[l++]=(byte)((i1+i2-254)/2);
	}

	return new String(b,0,l);
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param arg 
     */
    public static void main(String[] arg)
    {
	Password pw = new Password("password");
	System.err.println(obfiscate(pw.toString()));
    }
    
}


