/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.io;

/* ------------------------------------------------------------------------------- */
/** Buffer utility methods.
 * 
 * @version $Revision$
 * @author gregw
 */
public class BufferUtil
{
    static final byte SPACE= 0x20;
    static final byte MINUS= '-';

    /**
     * Convert buffer to an integer.
     * Parses up to the first non-numeric character. If no number is found an
     * IllegalArgumentException is thrown
     * @param buffer A buffer containing an integer. The position is not changed.
     * @return an int 
     */
    public static int toInt(Buffer buffer)
    {
        int val= 0;
        boolean started= false;
        boolean minus= false;
        for (int i= buffer.position(); i < buffer.limit(); i++)
        {
            byte b= buffer.get(i);
            if (b <= SPACE)
            {
                if (started)
                    break;
            }
            else if (b >= '0' && b <= '9')
            {
                val= val * 10 + (b - '0');
                started= true;
            }
            else if (b == MINUS && !started)
            {
                minus= true;
            }
            else
                break;
        }

        if (started)
            return minus ? (-val) : val;
        Portable.throwNumberFormat(buffer.toString());
        return -1;
    }

    public static void putInt(Buffer buffer, int n)
    {
        if (n < 0)
        {
            buffer.put((byte)'-');

            if (n == Integer.MIN_VALUE)
            {
                buffer.put((byte)'2');
                n= 147483648;
            }
            else
                n= -n;
        }

        if (n < 10)
        {
            switch (n)
            {
                case 0 :
                    buffer.put((byte)'0');
                    break;
                case 1 :
                    buffer.put((byte)'1');
                    break;
                case 2 :
                    buffer.put((byte)'2');
                    break;
                case 3 :
                    buffer.put((byte)'3');
                    break;
                case 4 :
                    buffer.put((byte)'4');
                    break;
                case 5 :
                    buffer.put((byte)'5');
                    break;
                case 6 :
                    buffer.put((byte)'6');
                    break;
                case 7 :
                    buffer.put((byte)'7');
                    break;
                case 8 :
                    buffer.put((byte)'8');
                    break;
                case 9 :
                    buffer.put((byte)'9');
                    break;
            }
        }
        else
        {
            boolean started= false;
            // This assumes constant time int arithmatic
            for (int i= 0; i < divisors.length; i++)
            {
                if (n < divisors[i])
                {
                    if (started)
                        buffer.put((byte)'0');
                    continue;
                }

                started= true;
                int d= n / divisors[i];
                buffer.put((byte) (d + '0'));
                n= n - d * divisors[i];
            }
        }
    }

    private static int[] divisors=
        { 1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10, 1 };

    public static void putCRLF(Buffer buffer)
    {
        buffer.put((byte)13);
        buffer.put((byte)10);
    }

}
