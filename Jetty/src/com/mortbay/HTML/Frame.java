// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

/** FrameSet
 * @version $Id$
 * @author Greg Wilkins
*/
public class Frame
{
    String src=null;
    String name=null;
    
    String scrolling="auto";
    String resize="";
    String border="";
    
    /* ------------------------------------------------------------ */
    /** Frame constructor
     */
    Frame(){}
    
    /* ------------------------------------------------------------ */
    public Frame border(boolean threeD, int width, String color)
    {
        border=" FRAMEBORDER="+(threeD?"yes":"no");
        if (width>=0)
            border+=" BORDER="+width;

        if (color!=null)
            border+=" BORDERCOLOR="+color;
        return this;
    }
    /* ------------------------------------------------------------ */
    public Frame name(String name,String src)
    {
        this.name=name;
        this.src=src;
        return this;
    }
    
    /* ------------------------------------------------------------ */
    public Frame src(String s)
    {
        src=s;
        return this;
    }
    
    /* ------------------------------------------------------------ */
    public Frame name(String n)
    {
        name=n;
        return this;
    }

    /* ------------------------------------------------------------ */
    public Frame scrolling(boolean s)
    {
        scrolling=s?"yes":"no";
        return this;
    }
    
    /* ------------------------------------------------------------ */
    public Frame resize(boolean r)
    {
        resize=r?"":" NORESIZE";
        return this;
    }
    
    /* ----------------------------------------------------------------- */
    void write(Writer out)
         throws IOException
    {
        out.write("<FRAME SCROLLING="+scrolling+resize+border);
        
        if(src!=null)
            out.write(" SRC="+src);
        if(name!=null)
            out.write(" NAME="+name);
        out.write(">");
    }
};






