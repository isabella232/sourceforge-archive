// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTML;
import java.io.IOException;
import java.io.Writer;


/* ------------------------------------------------------------ */
/** HTML Comment
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class Comment extends Composite
{
    /* ----------------------------------------------------------------- */
    public void write(Writer out)
         throws IOException
    {
        out.write("<!--\n");
        super.write(out);
        out.write("\n-->");
    }
};
