// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.IOException;
import java.io.Writer;

/* -------------------------------------------------------------------- */
/** HTML Tag Element
 * A Tag element is of the generic form &ltTAG attributes... &gt
 * @see  com.mortbay.HTML.Element
 */
public class Tag extends Element
{
    /* ---------------------------------------------------------------- */
    protected String tag;

    /* ---------------------------------------------------------------- */
    public Tag(String tag)
    {
        this.tag=tag;
    }
    
    /* ---------------------------------------------------------------- */
    public void write(Writer out)
         throws IOException
    {
        out.write('<'+tag+attributes()+'>');
    }
}

