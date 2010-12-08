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
 * Created on Nov 19, 2008
 * 
 */
package reconcile.util;

import java.util.Arrays;
import java.util.Iterator;

import reconcile.data.Document;

/**
 * @author David Buttler
 * 
 */
public class String2DocIterable
    implements Iterable<Document> {

/**
 * @author David Buttler
 * 
 */
private static class String2DocIterator
    implements Iterator<Document> {

private Iterator<String> mIter;

/**
 * return the root dir by default
 * 
 * @param iterator
 */
public String2DocIterator(Iterator<String> iterator) {
  mIter = iterator;
}

/* (non-Javadoc)
 * @see java.util.Iterator#hasNext()
 */
public boolean hasNext()
{
  return mIter.hasNext();
}

/* (non-Javadoc)
 * @see java.util.Iterator#next()
 */
public Document next()
{
  String d = mIter.next();
  return new Document(d);
}

/* (non-Javadoc)
 * @see java.util.Iterator#remove()
 */
public void remove()
{
  mIter.remove();

}

}

private Iterable<String> mIterable;

public String2DocIterable(Iterable<String> docI) {
  mIterable = docI;
}

public String2DocIterable(String[] docI) {
  mIterable = Arrays.asList(docI);
}

/* (non-Javadoc)
 * @see java.lang.Iterable#iterator()
 */
public Iterator<Document> iterator()
{
  return new String2DocIterator(mIterable.iterator());
}

}
