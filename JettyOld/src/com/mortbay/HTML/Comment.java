// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

import com.mortbay.Base.Code;

/* ------------------------------------------------------------ */
/** HTML Comment
 * @version 1.0 Sat Sep  4 1999
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
