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
 * Created on Mar 26, 2009
 * 
 */
package reconcile.util;

import java.io.File;
import java.util.Iterator;

import reconcile.data.Document;

/**
 * @author David Buttler
 * 
 */
public class File2DocIterable 
    implements Iterable<Document> {

/**
 * @author David Buttler
 * 
 */
private static class File2DocIterator
    implements Iterator<Document> {

private Iterator<File> mIter;

/**
 * return the root dir by default
 * 
 * @param iterator
 */
public File2DocIterator(Iterator<File> iterator) {
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
  return new Document(mIter.next());
}

/* (non-Javadoc)
 * @see java.util.Iterator#remove()
 */
public void remove()
{
  mIter.remove();

}

}

private Iterable<File> mIterable;

public File2DocIterable(Iterable<File> docI) {
  mIterable = docI;
}

/* (non-Javadoc)
 * @see java.lang.Iterable#iterator()
 */
public Iterator<Document> iterator()
{
  return new File2DocIterator(mIterable.iterator());
}

}
