// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

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
        for (int i= buffer.getIndex(); i < buffer.putIndex(); i++)
        {
            byte b= buffer.peek(i);
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

    public static void putHexInt(Buffer buffer, int n)
    {

        if (n < 0)
        {
            buffer.put((byte)'-');

            if (n == Integer.MIN_VALUE)
            {
                buffer.put("80000000".getBytes(),0,8);
                return;
            }
            n= -n;
        }

        if (n < 0x10)
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
                case 10 :
                    buffer.put((byte)'A');
                    break;
                case 11 :
                    buffer.put((byte)'B');
                    break;
                case 12 :
                    buffer.put((byte)'C');
                    break;
                case 13 :
                    buffer.put((byte)'D');
                    break;
                case 14 :
                    buffer.put((byte)'E');
                    break;
                case 15 :
                    buffer.put((byte)'F');
                    break;
            }
        }
        else
        {
            boolean started= false;
            // This assumes constant time int arithmatic
            for (int i= 0; i < hexDivisors.length; i++)
            {
                if (n < hexDivisors[i])
                {
                    if (started)
                        buffer.put((byte)'0');
                    continue;
                }

                started= true;
                int d= n / hexDivisors[i];
                if (d<10)
                    buffer.put((byte) (d + '0'));
                else
                    buffer.put((byte) (d-10+ 'A'));
                n= n - d * hexDivisors[i];
            }
        }
    }
    
    public static void putDecInt(Buffer buffer, int n)
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
            for (int i= 0; i < decDivisors.length; i++)
            {
                if (n < decDivisors[i])
                {
                    if (started)
                        buffer.put((byte)'0');
                    continue;
                }

                started= true;
                int d= n / decDivisors[i];
                buffer.put((byte) (d + '0'));
                n= n - d * decDivisors[i];
            }
        }
    }

    private static int[] decDivisors=
        { 1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10, 1 };

    private static int[] hexDivisors=
        { 0x10000000, 0x1000000, 0x100000, 0x10000, 0x1000, 0x100, 0x10, 1 };


    public static void putCRLF(Buffer buffer)
    {
        buffer.put((byte)13);
        buffer.put((byte)10);
    }
    
    public static boolean isPrefix(Buffer prefix,Buffer buffer)
    {
        if (prefix.length()>buffer.length())
            return false;
        int bi=buffer.getIndex();
        for (int i=prefix.getIndex(); i<prefix.putIndex();i++)
            if (prefix.peek(i)!=buffer.peek(bi++))
                return false;
        return true;
    }


}
