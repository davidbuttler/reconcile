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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

import reconcile.data.Document;

/**
 * @author David Buttler
 * 
 */
public class Doc2InputStreamIterable
    implements Iterable<InputStream> {

/**
 * @author David Buttler
 * 
 */
private static class Doc2InputStreamIterator
    implements Iterator<InputStream> {

private Iterator<Document> mIter;
private FileExtractor mEx;

public Doc2InputStreamIterator(Iterator<Document> iterator, FileExtractor ex) {
  mIter = iterator;
  mEx = ex;
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
public InputStream next()
{
  if (mEx == null) {
    Document d = mIter.next();
    return new ByteArrayInputStream(d.getText().getBytes());
  }
  return mEx.getFile(mIter.next());
}

/* (non-Javadoc)
 * @see java.util.Iterator#remove()
 */
public void remove()
{
  mIter.remove();

}

}

private Iterable<Document> mIterable;
private FileExtractor mEx;

public Doc2InputStreamIterable(Iterable<Document> docI) {
  mIterable = docI;
}

public Doc2InputStreamIterable(Iterable<Document> docI, FileExtractor ex) {
  mIterable = docI;
  mEx = ex;
}

/* (non-Javadoc)
 * @see java.lang.Iterable#iterator()
 */
public Iterator<InputStream> iterator()
{
  return new Doc2InputStreamIterator(mIterable.iterator(), mEx);
}

}
