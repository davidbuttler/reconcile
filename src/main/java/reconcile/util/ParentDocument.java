package reconcile.util;

import java.io.File;
import java.util.Iterator;

import reconcile.data.Document;

/**
 * Given an iterator over a list of files, return an iterator over a list of the parent directory of those files. This
 * is useful in the case of reconcile where the directory for all items related to a file is the parent of a readily
 * identifiable file (raw.txt). And, instead of calling them files, here we will call them a Document, which is our
 * abstraction of what a file looks like with all of the associated meta data as companion files (eg annotations).
 * 
 * @author David Buttler
 * 
 */
public class ParentDocument
    implements Iterable<Document> {

private Iterable<File> mBaseIterable;

public ParentDocument(Iterable<File> baseIterable) {
  mBaseIterable = baseIterable;
}

public Iterator<Document> iterator()
{
  return new ParentDocumentIterator(mBaseIterable.iterator());
}

}

class ParentDocumentIterator
    implements Iterator<Document> {

private Iterator<File> mBase;

public ParentDocumentIterator(Iterator<File> base) {
  mBase = base;
}

public boolean hasNext()
{
  return mBase.hasNext();
}

public Document next()
{
    return new Document(mBase.next().getParentFile());
}

public void remove()
{
  mBase.remove();

}

}
