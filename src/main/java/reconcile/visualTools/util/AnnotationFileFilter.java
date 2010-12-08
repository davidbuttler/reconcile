/**
 * XMLFileFilter.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.util;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.filechooser.FileFilter;

/**
 * A convenience implementation of FileFilter that filters out all files except for those type extensions that it knows
 * about.
 * 
 * Extensions are of the type ".foo", which is typically found on Windows and Unix boxes, but not on Macinthosh. Case is
 * ignored.
 * 
 * Example - create a new filter that filerts out all files but gif and jpg image files:
 * 
 * JFileChooser chooser = new JFileChooser(); ExampleFileFilter filter = new ExampleFileFilter( new String{"gif",
 * "jpg"}, "JPEG & GIF Images") chooser.addChoosableFileFilter(filter); chooser.showOpenDialog(this);
 * 
 * @version 1.13 06/13/02
 * @author Jeff Dinkins
 */
public class AnnotationFileFilter
    extends FileFilter {

// private static String TYPE_UNKNOWN = "Type Unknown";
// private static String HIDDEN_FILE = "Hidden File";
private Map<String, AnnotationFileFilter> filters = null;

private String description = null;

private String fullDescription = null;

private boolean useExtensionsInDescription = true;

private String fileExtension;

/**
 * Creates a file filter. If no filters are added, then all files are accepted.
 * 
 * @see #addExtension
 */
public AnnotationFileFilter() {
  this.filters = new HashMap<String, AnnotationFileFilter>();
  fileExtension = "";
}

/**
 * Creates a file filter that accepts files with the given extension. Example: new ExampleFileFilter("jpg");
 * 
 * @see #addExtension
 */
public AnnotationFileFilter(String extension) {
  this(extension, null);
  fileExtension = extension;
}

/**
 * Creates a file filter that accepts the given file type. Example: new ExampleFileFilter("jpg", "JPEG Image Images");
 * 
 * Note that the "." before the extension is not needed. If provided, it will be ignored.
 * 
 * @see #addExtension
 */
public AnnotationFileFilter(String extension, String description) {
  this();
  fileExtension = extension;
  if (extension != null) {
    addExtension(extension);
  }
  if (description != null) {
    setDescription(description);
  }
}

/**
 * Creates a file filter from the given string array. Example: new ExampleFileFilter(String {"gif", "jpg"});
 * 
 * Note that the "." before the extension is not needed adn will be ignored.
 * 
 * @see #addExtension
 */
public AnnotationFileFilter(String[] filters) {
  this(filters, null);
}

/**
 * Creates a file filter from the given string array and description. Example: new ExampleFileFilter(String {"gif",
 * "jpg"}, "Gif and JPG Images");
 * 
 * Note that the "." before the extension is not needed and will be ignored.
 * 
 * @see #addExtension
 */
public AnnotationFileFilter(String[] filters, String description) {
  this();
  for (String filter : filters) {
    // add filters one by one
    addExtension(filter);
  }
  if (description != null) {
    setDescription(description);
  }
}

/**
 * Return true if this file should be shown in the directory pane, false if it shouldn't.
 * 
 * Files that begin with "." are ignored.
 * 
 * @see #getExtension
 * @see FileFilter#accepts
 */
@Override
public boolean accept(File f)
{
  if (f != null) {
    if (f.isDirectory()) return true;
    String extension = getExtension(f);
    if (extension != null && filters.get(getExtension(f)) != null) return true;
    ;
  }
  return false;
}

/**
 * Return the extension portion of the file's name .
 * 
 * @see #getExtension
 * @see FileFilter#accept
 */
public String getExtension(File f)
{
  if (f != null) {
    String filename = f.getName();
    int i = filename.lastIndexOf('.');
    if (i > 0 && i < filename.length() - 1) return filename.substring(i + 1).toLowerCase();
    ;
  }
  return null;
}

/**
 * Adds a filetype "dot" extension to filter against.
 * 
 * For example: the following code will create a filter that filters out all files except those that end in ".jpg" and
 * ".tif":
 * 
 * ExampleFileFilter filter = new ExampleFileFilter(); filter.addExtension("jpg"); filter.addExtension("tif");
 * 
 * Note that the "." before the extension is not needed and will be ignored.
 */
public void addExtension(String extension)
{
  if (filters == null) {
    filters = new Hashtable<String, AnnotationFileFilter>(5);
  }
  filters.put(extension.toLowerCase(), this);
  fullDescription = null;
}

/**
 * Returns the human readable description of this filter. For example: "JPEG and GIF Image Files (*.jpg, *.gif)"
 * 
 * @see setDescription
 * @see setExtensionListInDescription
 * @see isExtensionListInDescription
 * @see FileFilter#getDescription
 */
@Override
public String getDescription()
{
  if (fullDescription == null) {
    if (description == null || isExtensionListInDescription()) {
      fullDescription = description == null ? "(" : description + " (";
      // build the description from the extension list
      Iterator<String> extensions = filters.keySet().iterator();
      if (extensions != null) {
        fullDescription += "." + extensions.next();
        while (extensions.hasNext()) {
          fullDescription += ", ." + extensions.next();
        }
      }
      fullDescription += ")";
    }
    else {
      fullDescription = description;
    }
  }
  return fullDescription;
}

/**
 * Sets the human readable description of this filter. For example: filter.setDescription("Gif and JPG Images");
 * 
 * @see setDescription
 * @see setExtensionListInDescription
 * @see isExtensionListInDescription
 */
public void setDescription(String description)
{
  this.description = description;
  fullDescription = null;
}

/**
 * Determines whether the extension list (.jpg, .gif, etc) should show up in the human readable description.
 * 
 * Only relevent if a description was provided in the constructor or using setDescription();
 * 
 * @see getDescription
 * @see setDescription
 * @see isExtensionListInDescription
 */
public void setExtensionListInDescription(boolean b)
{
  useExtensionsInDescription = b;
  fullDescription = null;
}

/**
 * Returns whether the extension list (.jpg, .gif, etc) should show up in the human readable description.
 * 
 * Only relevent if a description was provided in the constructor or using setDescription();
 * 
 * @see getDescription
 * @see setDescription
 * @see setExtensionListInDescription
 */
public boolean isExtensionListInDescription()
{
  return useExtensionsInDescription;
}

/**
 * Returns file extension type(.xml, .sum1, .mpqa). Main purpose is to distinguish the file extension when open file in
 * the MainMenuBar.
 */
public String getFileExtension()
{
  return fileExtension;
}

}
