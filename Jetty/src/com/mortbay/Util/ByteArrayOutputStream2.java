// ===========================================================================
// Copyright (c) 2001 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import java.io.ByteArrayOutputStream;

/* ------------------------------------------------------------ */
/** ByteArrayOutputStream with public internals

 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ByteArrayOutputStream2 extends ByteArrayOutputStream
{
    public ByteArrayOutputStream2(){super();}
    public ByteArrayOutputStream2(int size){super(size);}
    public byte[] getBuf(){return buf;}
    public int getCount(){return count;}
}
