// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTML;
import com.mortbay.Util.*;

import com.mortbay.Util.Code;

/* ------------------------------------------------------------ */
/** CSS Style LINK
 *
 * @version 1.0 Sat Sep  4 1999
 * @author Greg Wilkins (gregw)
 */
public class StyleLink extends Tag
{
    public final static String
        REL="REL",
        HREF="HREF",
        TYPE=Style.TYPE,
        MEDIA=Style.MEDIA;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param href The URL of the style sheet
     */
    public StyleLink(String href)
    {
        super("LINK");
        attribute(REL,Style.StyleSheet);
        attribute(HREF,href);
        attribute(TYPE,Style.text_css);
    }
    
    /* ------------------------------------------------------------ */
    /** Full Constructor. 
     * @param rel Style Relationship, default StyleSheet if null.
     * @param href The URL of the style sheet
     * @param type The type, default text/css if null
     * @param media The media, not specified if null
     */
    public StyleLink(String rel, String href, String type, String media)
    {
        super("LINK");
        attribute(REL,rel==null?Style.StyleSheet:rel);
        attribute(HREF,href);
        attribute(TYPE,type==null?Style.text_css:type);
        if (media!=null)
            attribute(MEDIA,media);
    }
    
};








