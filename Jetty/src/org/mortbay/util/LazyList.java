// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/* ------------------------------------------------------------ */
/** Lazy List creation.
 * A List helper class that attempts to avoid unneccessary List
 * creation.   If a method needs to create a List to return, but it is
 * expected that this will either be empty or frequently contain a
 * single item, then using LazyList will avoid additional object
 * creations by using Collections.EMPTY_LIST or
 * Collections.singletonList where possible.
 *
 * <p><h4>Usage</h4>
 * <pre>
 *   LazyList lazylist =null;
 *   while(loopCondition)
 *   {
 *     Object item = getItem();
 *     if (item.isToBeAdded())
 *         lazylist = LazyList.add(lazylist,item);
 *   }
 *   return LazyList.getList(lazylist);
 * </pre>
 *
 * An ArrayList of default size is used as the initial LazyList.
 *
 * @see java.util.List
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class LazyList
{
    private Object _first;
    private List _list;

    /* ------------------------------------------------------------ */
    private LazyList(Object first)
    {
        _first=first;
    }
    
    /* ------------------------------------------------------------ */
    /** Add an item to a LazyList 
     * @param list The list to add to or null if none yet created.
     * @param item The item to add.
     * @return The lazylist created or added to.
     */
    public static LazyList add(LazyList list, Object item)
    {
        if (list==null)
            return new LazyList(item);

        if (list._list!=null)
        {
            list._list.add(item);
            return list;
        }

        list._list=new ArrayList();
        list._list.add(list._first);
        list._list.add(item);
        return list;    
    }

    /* ------------------------------------------------------------ */
    /** Get the real List from a LazyList.
     * 
     * @param list A LazyList returned from LazyList.add(Object)
     * @return The List of added items, which may be an EMPTY_LIST
     * or a SingletonList.
     */
    public static List getList(LazyList list)
    {
        return getList(list,false);
    }
    

    /* ------------------------------------------------------------ */
    /** Get the real List from a LazyList.
     * 
     * @param list A LazyList returned from LazyList.add(Object) or null
     * @param nullForEmpty If true, null is returned instead of an
     * empty list.
     * @return The List of added items, which may be null, an EMPTY_LIST
     * or a SingletonList.
     */
    public static List getList(LazyList list, boolean nullForEmpty)
    {
        if (list==null)
            return nullForEmpty?null:Collections.EMPTY_LIST;
        if (list._list==null)
            return Collections.singletonList(list._first);
        return list._list;
    }


    /* ------------------------------------------------------------ */
    /** The size of a lazy List 
     * @param list  A LazyList returned from LazyList.add(Object) or null
     * @return the size of the list.
     */
    public static int size(LazyList list)
    {
        if (list==null)
            return 0;
        if (list._list==null)
            return 1;
        return list._list.size();
    }
    
    /* ------------------------------------------------------------ */
    /** Get item from the list 
     * @param list  A LazyList returned from LazyList.add(Object) or null
     * @param int i index
     * @return the item from the list.
     */
    public static Object get(LazyList list, int i)
    {
        if (list==null)
            throw new IndexOutOfBoundsException();
        
        if (list._list==null)
        {
            if (i==0)
                return list._first;
            throw new IndexOutOfBoundsException();
        }
            
        return list._list.get(i);
    }
    
}

