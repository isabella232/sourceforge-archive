// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

/* --------------------------------------------------------------------- */
/** A HTML Table element
 * <p> The Table Element works by calling newRow and then adding cells or
 * headings.
 * <p>Notes<br>
 * Tables are implemented by nesting a cell Block within a row Block
 * within the table which is also a Block (see nest() on class Composite).
 * Once a row and cell have been created, calling add or attributes on
 * the table actually calls the cell.
 *
 * @see com.mortbay.HTML.Element
 */
public class Table extends Block
{    
    /* ----------------------------------------------------------------- */
    private Block row = null;
    private Block cell = null;
    private static Hashtable threadNestingMap = null;
    private CompositeFactory cellNestFactory = null;

    /* ----------------------------------------------------------------- */
    /** Construct Table
     */
    public Table(int border)
    {
	super("TABLE");
	attribute("BORDER",border);
	if (threadNestingMap!=null)
	    cellNestFactory = (CompositeFactory)
		threadNestingMap.get(Thread.currentThread());
    }

    /* ----------------------------------------------------------------- */
    /** Construct Table with attributes
     */
    public Table(int border, String attributes)
    {
	this(border);
	attribute(attributes);
    }

    /* ----------------------------------------------------------------- */
    /** Create new table row.
     * Attributes set after this call and before a call to newCell or
     * newHeader are considered row attributes.
     */
    public Table newRow()
    {
	unnest();
	nest(row = new Block("TR"));
	cell=null;
	return this;
    }

    /* ----------------------------------------------------------------- */
    /** Create new table row with attributes
     * Attributes set after this call and before a call to newCell or
     * newHeader are considered row attributes.
     */
    public Table newRow(String attributes)
    {
	newRow();
	row.attribute(attributes);
	return this;	
    }

    /* ----------------------------------------------------------------- */
    /* Create a new Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     */
    private void newBlock(String m)
    {
	if (row==null)
	    newRow();
	else
	    row.unnest();
	row.nest(cell=new Block(m));

	if (cellNestFactory!=null)
	    cell.nest(cellNestFactory.newComposite());	
    }
    
    /* ----------------------------------------------------------------- */
    /* Create a new Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     */
    public Table newCell()
    {
	newBlock("TD");
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /* Create a new Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     * @return This table for call chaining
     */
    public Table newCell(String attributes)
    {
	newCell();
	cell.attribute(attributes);
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /* Add a new Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     * @return This table for call chaining
     */
    public Table addCell(Object o)
    {
	newCell();
	cell.add(o);
	return this;
    }

    /* ----------------------------------------------------------------- */
    /* Add a new Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     * @return This table for call chaining
     */
    public Table addCell(Object o, String attributes)
    {
	addCell(o);
	cell.attribute(attributes);
	return this;
    }
	
    /* ----------------------------------------------------------------- */
    /* Create a new Heading in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     */
    public Table newHeading()
    {
	newBlock("TH");
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /* Add a new heading Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     * @return This table for call chaining
     */
    public Table addHeading(Object o)
    {
	newHeading();
	cell.add(o);
	return this;
    }

    /* ----------------------------------------------------------------- */
    /* Add a new heading Cell in the current row.
     * Adds to the table after this call and before next call to newRow,
     * newCell or newHeader are added to the cell.
     * @return This table for call chaining
     */
    public Table addHeading(Object o,String attributes)
    {
	addHeading(o);
	cell.attribute(attributes);
	return this;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the table cell spacing
     * @param s spacing in pixels
     * @return This table for call chaining
     */
    public Table cellSpacing(int s)
    {
	attribute("CELLSPACING",s);
	return this;
    }
    

    /* ------------------------------------------------------------ */
    /** Set the table cell padding
     * @param padding the cell padding in pixels
     * @return This table for call chaining
     */
    public Table cellPadding(int padding)
    {
	attribute("CELLPADDING",padding);
	return this;
    }
    
    /* ------------------------------------------------------------ */
    /** Set horizontal and vertical spacing
     * @param h horizontal spacing
     * @param v vertical spacing
     * @return This table for call chaining
     */
    public Table spacing(int h, int v)
    {
	if (h>=0)
	    attribute("HSPACE",h);
	if (v>=0)
	    attribute("VSPACE",v);
	return this;
    }

    /* ----------------------------------------------------------------- */
    /** Get the current row Block element.
     * Use this call for setting row attributes.
     * @return The Block instance which has been nested in the table as
     * the row
     */
    public Block row()
    {
	return row;
    }
    
    /* ----------------------------------------------------------------- */
    /** Get the current cell Block element.
     * Use this call for setting cell attributes.
     * @return The Block instance which has been nested in the row as
     * the cell
     */
    public Block cell()
    {
	return cell;
    }
    
    /* ----------------------------------------------------------------- */
    /** Add cell nesting factory
     * Set the CompositeFactory for this thread. Each new cell in the
     * table added by this thread will have a new Composite from this
     * factory nested in the Cell.
     * @param factory The factory for this Thread. If null clear this
     * threads factory.
     * @deprecated Use setNestingFactory or setThreadNestingFactory
     */
    public static void setCellNestingFactory(CompositeFactory factory)
    {
	if (threadNestingMap==null)
	    threadNestingMap= new Hashtable();
	
	if (factory == null)
	    threadNestingMap.remove(Thread.currentThread());
	else
	    threadNestingMap.put(Thread.currentThread(),factory);
    }
    
    /* ----------------------------------------------------------------- */
    /** Add cell nesting factory for thread
     * Set the CompositeFactory for this thread. Each new cell in the
     * table added by this thread will have a new Composite from this
     * factory nested in the Cell.
     * @param factory The factory for this Thread. If null clear this
     * threads factory.
     */
    public static void setThreadNestingFactory(CompositeFactory factory)
    {
	if (threadNestingMap==null)
	    threadNestingMap= new Hashtable();
	
	if (factory == null)
	    threadNestingMap.remove(Thread.currentThread());
	else
	    threadNestingMap.put(Thread.currentThread(),factory);
    }
    
    /* ----------------------------------------------------------------- */
    /** Add cell nesting factory for table
     * Set the CompositeFactory for this thread. Each new cell in the
     * table added by this thread will have a new Composite from this
     * factory nested in the Cell.
     * @param factory The factory for this Thread. If null clear this
     * threads factory.
     */
    public void setNestingFactory(CompositeFactory factory)
    {
	cellNestFactory = factory;
    }   
}








