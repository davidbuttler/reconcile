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
 * Created on Feb 29, 2008
 * 
 * 
 */
package reconcile.util;

import java.io.File;
import java.io.FileFilter;

/**
 * A central place to keep file filters
 * 
 * @author <a href="mailto:buttler1@llnl.gov">David Buttler</a>
 * 
 */
public class Filters {

/**
 * @author <a href="mailto:buttler1@llnl.gov">David Buttler</a>
 * 
 */
static class NamedFileFilter
    implements FileFilter {

private String mName;

/**
 * @param name
 */
public NamedFileFilter(String name) {
  mName = name;
}

/* (non-Javadoc)
 * @see java.io.FileFilter#accept(java.io.File)
 */
public boolean accept(File pathname)
{
  if (pathname.isDirectory() || pathname.getName().equals(mName)) return true;
  return false;
}

}

/**
 * @author <a href="mailto:hysom@llnl.gov">David Hysom</a>
 * 
 */
static class FileFileFilter
    implements FileFilter {

/**
 * @param name
 */
public FileFileFilter() {
}

/* (non-Javadoc)
 * @see java.io.FileFilter#accept(java.io.File)
 */
public boolean accept(java.io.File pathname)
{
  if (pathname.isDirectory() || pathname.isFile()) return true;
  return false;
}

}

/**
 * returns true on directories and files
 */
public static final FileFilter isFileFileFilter = createIsFile();

public static FileFilter createIsFile()
{
  return new FileFileFilter();
}

/**
 * returns true on directories and files named 'raw.txt'
 */
public static final FileFilter rawFileFilter = create("raw.txt");

/**
 * Create a file filter that returns true on all directories and on files with the given name
 */
public static FileFilter create(String name)
{
  return new NamedFileFilter(name);
}
}
