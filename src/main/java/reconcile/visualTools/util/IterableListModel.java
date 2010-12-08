/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National
 * Laboratory. Written by David Buttler, buttler1@llnl.gov CODE-400187 All rights reserved. This file is part of
 * RECONCILE
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License (as published by the Free Software Foundation) version 2, dated June 1991. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the IMPLIED WARRANTY OF MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the terms and conditions of the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA For full text see license.txt
 * 
 * Created on Jan 29, 2009
 * 
 */
package reconcile.visualTools.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;

/**
 * @author David Buttler
 * 
 */
public class IterableListModel<T>
    extends AbstractListModel
    implements Iterable<T> {

/**
   * 
   */
private static final long serialVersionUID = 1L;
private List<T> delegate = new ArrayList<T>();

/**
 * Returns the number of components in this list.
 * <p>
 * This method is identical to <code>size</code>, which implements the <code>List</code> interface defined in the 1.2
 * Collections framework. This method exists in conjunction with <code>setSize</code> so that <code>size</code> is
 * identifiable as a JavaBean property.
 * 
 * @return the number of components in this list
 * @see #size()
 */
public int getSize()
{
  return delegate.size();
}

/**
 * Returns the number of components in this list.
 * 
 * @return the number of components in this list
 * @see Vector#size()
 */
public int size()
{
  return delegate.size();
}

/**
 * Tests whether this list has any components.
 * 
 * @return <code>true</code> if and only if this list has no components, that is, its size is zero; <code>false</code>
 *         otherwise
 * @see Vector#isEmpty()
 */
public boolean isEmpty()
{
  return delegate.isEmpty();
}

/**
 * Returns an enumeration of the components of this list.
 * 
 * @return an enumeration of the components of this list
 * @see Vector#elements()
 */
public Iterator<T> iterator()
{
  return delegate.iterator();
}

/**
 * Tests whether the specified object is a component in this list.
 * 
 * @param elem
 *          an object
 * @return <code>true</code> if the specified object is the same as a component in this list
 * @see Vector#contains(Object)
 */
public boolean contains(T elem)
{
  return delegate.contains(elem);
}

/**
 * Searches for the first occurrence of <code>elem</code>.
 * 
 * @param elem
 *          an object
 * @return the index of the first occurrence of the argument in this list; returns <code>-1</code> if the object is not
 *         found
 * @see Vector#indexOf(Object)
 */
public int indexOf(T elem)
{
  return delegate.indexOf(elem);
}

/**
 * Returns the index of the last occurrence of <code>elem</code>.
 * 
 * @param elem
 *          the desired component
 * @return the index of the last occurrence of <code>elem</code> in the list; returns <code>-1</code> if the object is
 *         not found
 * @see Vector#lastIndexOf(Object)
 */
public int lastIndexOf(T elem)
{
  return delegate.lastIndexOf(elem);
}

/**
 * Returns the component at the specified index. Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is
 * negative or not less than the size of the list. <blockquote> <b>Note:</b> Although this method is not deprecated, the
 * preferred method to use is <code>get(int)</code>, which implements the <code>List</code> interface defined in the 1.2
 * Collections framework. </blockquote>
 * 
 * @param index
 *          an index into this list
 * @return the component at the specified index
 * @see #get(int)
 * @see Vector#elementAt(int)
 */
public T elementAt(int index)
{
  return delegate.get(index);
}

/**
 * Returns the first component of this list. Throws a <code>NoSuchElementException</code> if this vector has no
 * components.
 * 
 * @return the first component of this list
 * @see Vector#firstElement()
 */
public T firstElement()
{
  return delegate.get(0);
}

/**
 * Returns the last component of the list. Throws a <code>NoSuchElementException</code> if this vector has no
 * components.
 * 
 * @return the last component of the list
 * @see Vector#lastElement()
 */
public T lastElement()
{
  return delegate.get(delegate.size() - 1);
}

/**
 * Adds the specified component to the end of this list.
 * 
 * @param obj
 *          the component to be added
 * @see Vector#addElement(Object)
 */
public void add(T obj)
{
  int index = delegate.size();
  delegate.add(obj);
  fireIntervalAdded(this, index, index);
}

/**
 * Removes the first (lowest-indexed) occurrence of the argument from this list.
 * 
 * @param obj
 *          the component to be removed
 * @return <code>true</code> if the argument was a component of this list; <code>false</code> otherwise
 * @see Vector#removeElement(Object)
 */
public boolean remove(T obj)
{
  int index = indexOf(obj);
  boolean rv = delegate.remove(obj);
  if (index >= 0) {
    fireIntervalRemoved(this, index, index);
  }
  return rv;
}

/**
 * Removes all components from this list and sets its size to zero. <blockquote> <b>Note:</b> Although this method is
 * not deprecated, the preferred method to use is <code>clear</code>, which implements the <code>List</code> interface
 * defined in the 1.2 Collections framework. </blockquote>
 * 
 * @see #clear()
 * @see Vector#removeAllElements()
 */
public void removeAll()
{
  int index1 = delegate.size() - 1;
  delegate.clear();
  if (index1 >= 0) {
    fireIntervalRemoved(this, 0, index1);
  }
}

/**
 * Returns a string that displays and identifies this object's properties.
 * 
 * @return a String representation of this object
 */
@Override
public String toString()
{
  return delegate.toString();
}

/* The remaining methods are included for compatibility with the
 * Java 2 platform Vector class.
 */

/**
 * Returns an array containing all of the elements in this list in the correct order.
 * 
 * @return an array containing the elements of the list
 * @see Vector#toArray()
 */
public Object[] toArray()
{
  return delegate.toArray();
}

/**
 * Returns the element at the specified position in this list.
 * <p>
 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out of range (
 * <code>index &lt; 0 || index &gt;= size()</code>).
 * 
 * @param index
 *          index of element to return
 */
public T get(int index)
{
  return delegate.get(index);
}

/**
 * Replaces the element at the specified position in this list with the specified element.
 * <p>
 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out of range (
 * <code>index &lt; 0 || index &gt;= size()</code>).
 * 
 * @param index
 *          index of element to replace
 * @param element
 *          element to be stored at the specified position
 * @return the element previously at the specified position
 */
public T set(int index, T element)
{
  T rv = delegate.get(index);
  delegate.set(index, element);
  fireContentsChanged(this, index, index);
  return rv;
}

/**
 * Inserts the specified element at the specified position in this list.
 * <p>
 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out of range (
 * <code>index &lt; 0 || index &gt; size()</code>).
 * 
 * @param index
 *          index at which the specified element is to be inserted
 * @param element
 *          element to be inserted
 */
public void add(int index, T element)
{
  delegate.add(index, element);
  fireIntervalAdded(this, index, index);
}

/**
 * Removes the element at the specified position in this list. Returns the element that was removed from the list.
 * <p>
 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out of range (
 * <code>index &lt; 0 || index &gt;= size()</code>).
 * 
 * @param index
 *          the index of the element to removed
 */
public T remove(int index)
{
  T rv = delegate.remove(index);
  fireIntervalRemoved(this, index, index);
  return rv;
}

/**
 * Removes all of the elements from this list. The list will be empty after this call returns (unless it throws an
 * exception).
 */
public void clear()
{
  int index1 = delegate.size() - 1;
  delegate.clear();
  if (index1 >= 0) {
    fireIntervalRemoved(this, 0, index1);
  }
}

/**
 * Deletes the components at the specified range of indexes. The removal is inclusive, so specifying a range of (1,5)
 * removes the component at index 1 and the component at index 5, as well as all components in between.
 * <p>
 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index was invalid. Throws an
 * <code>IllegalArgumentException</code> if <code>fromIndex &gt; toIndex</code>.
 * 
 * @param fromIndex
 *          the index of the lower end of the range
 * @param toIndex
 *          the index of the upper end of the range
 * @see #remove(int)
 */
public void removeRange(int fromIndex, int toIndex)
{
  if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex must be <= toIndex");
  for (int i = toIndex; i >= fromIndex; i--) {
    delegate.remove(i);
  }
  fireIntervalRemoved(this, fromIndex, toIndex);
}

/* (non-Javadoc)
 * @see javax.swing.ListModel#getElementAt(int)
 */
public Object getElementAt(int index)
{

  return get(index);
}
}
