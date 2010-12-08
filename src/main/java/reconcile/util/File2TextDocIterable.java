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
import java.util.ArrayList;
import java.util.Iterator;

import reconcile.data.Document;
import reconcile.data.TextDocument;

/**
 * @author David Buttler
 * 
 */
public class File2TextDocIterable 
    implements Iterable<Document> {

/**
 * @author David Buttler
 * 
 */
private ArrayList<Document> docs;

public File2TextDocIterable(Iterable<File> docI) {
	docs=new ArrayList<Document>();
	for(File f: docI)
	  docs.add(new TextDocument(f));
}

/* (non-Javadoc)
 * @see java.lang.Iterable#iterator()
 */
public Iterator<Document> iterator()
{
  return docs.iterator();
}

}
