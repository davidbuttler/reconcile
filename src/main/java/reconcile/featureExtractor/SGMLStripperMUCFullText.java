/*
 * stripSGML.java nathan; May 23, 2007
 */

package reconcile.featureExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Utils;


public class SGMLStripperMUCFullText
    extends SGMLStripper {

private AnnotationSet markups;
private int skip;
FileWriter rawTextFile;
int offset;
Stack<Annotation> anStack;

/*
 * Call the parent's constructor. 
 */
public SGMLStripperMUCFullText() {
  super();
}

/*
 * Preprocesses the input file so things like ampersands don't break
 * the parser. 
 */
@Override
public void format(BufferedReader br, FileWriter out)
    throws IOException
{
  String line;
  boolean paragraph = false;
  boolean muc6 = Utils.getConfig().getString("DATASET").equals("muc6") ? true : false;

  try {
    while ((line = br.readLine()) != null) {
      line = line.replaceAll("&", "&amp;");

      // For MUC 7
      if (!muc6) {
        if (line.startsWith("<STORYID")) {
          int rabIndex = line.indexOf(">");
          String outline = "<STORYID" + line.substring(rabIndex, line.length() - 1) + "\n";
          out.write(outline);
          continue;
        }

        if (line.startsWith("<SLUG")) {
          int rabIndex = line.indexOf(">");
          String outline = "<SLUG" + line.substring(rabIndex, line.length() - 1) + "\n";
          out.write(outline);
          continue;
        }
        if ((line.contains("<p>") && paragraph) || line.contains("</TEXT>")) {
          out.write("</p>\n");
        }

        if (line.contains("<p>") && !paragraph) {
          paragraph = true;
        }
      }

      out.write(line.trim() + "\n");
    }
  }
  catch (IOException ex) {
    System.err.println(ex);
  }

  out.close();
  br.close();
}

@Override
public void run(Document doc, String[] annSetNames)
{
  String inputFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.sgml";
  String textFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.txt";

  try {
    /* The new file will be called raw.txt */
    String outFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.formatted";

    FileWriter writer = new FileWriter(outFile);
    FileReader reader = new FileReader(inputFile);

    XMLReader xmlr = XMLReaderFactory.createXMLReader();

    xmlr.setContentHandler(handler);
    xmlr.setErrorHandler(handler);

    BufferedReader br = new BufferedReader(reader);

    try {
      format(br, writer);
      reader.close();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    reader = new FileReader(outFile);
    rawTextFile = new FileWriter(textFile);
    markups = new AnnotationSet(annSetNames[0]);
    anStack = new Stack<Annotation>();

    offset = 0;
    skip = 0;

    // Parse the incoming XML file.
    xmlr.parse(new InputSource(reader));

    addResultSet(doc,markups);
    rawTextFile.close();
  }
  catch (Exception ex) {
    throw new RuntimeException(ex);
  }
}

/*
 * Grabs the opening SGML tag. 
 */
@Override
public void startElement(String uri, String name, String qName, Attributes atts)
{

  Map<String, String> attributes = new TreeMap<String, String>();

  for (int i = 0; i < atts.getLength(); i++) {
    String n = atts.getQName(i);
    String val = atts.getValue(i);
    attributes.put(n, val);
  }

  int id = markups.add(offset, 0, qName, attributes);
  Annotation cur = markups.get(id);
  anStack.push(cur);
}

/*
 * Grabs the closing tag. 
 */
@Override
public void endElement(String uri, String name, String qName)
{
  Annotation top = anStack.pop();

  if (!top.getType().equals(name)) throw new RuntimeException("SGML type mismatch");

  top.setEndOffset(offset);
}

/*
 * This prints out all the text between the tags we care about to 
 * a file. 
 *
 */
@Override
public void characters(char ch[], int start, int length)
{

  /*
   * If the current tag is one we don't care about, then 
   * skip all the text. 
   */
  if (skip > 0) return;

  for (int i = start; i < start + length; i++) {
    try {
      rawTextFile.write(ch[i]);
      offset++;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
}
