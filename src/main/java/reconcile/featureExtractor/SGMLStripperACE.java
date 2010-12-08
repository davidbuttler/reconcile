package reconcile.featureExtractor;

/*
 * SGMLStripperACE.java nathan; Removes SGML tags from ACE corpora; produces ace_annots and raw.txt files. This class
 * removes all SGML tags from the actual newswire article.
 */
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
import reconcile.features.FeatureUtils;
import reconcile.general.Utils;


public class SGMLStripperACE
    extends SGMLStripper {

private AnnotationSet markups;
private int skip;
// private static final String[] FIELDS_TO_SKIP = {"DOCNO", "DOCID", "DOCTYPE", "DATETIME", "HEADER", "HEADLINE",
// "SLUG", "DATE_TIME", "TRAILER"};
private static final String[] FIELDS_TO_INCLUDE = { "TEXT", "TXT" };
private FileWriter rawTextFile;
private FileWriter originalRawTextFile;
private int offset;

Stack<Annotation> anStack;

public SGMLStripperACE() {
  super();
}

/*
 * Gets rid of some bad charactes in ACE04
 */
@Override
public void format(BufferedReader br, FileWriter out)
    throws IOException
{
  String line;

  try {
    while ((line = br.readLine()) != null) {
      line = line.replaceAll("&", "&amp;");
      out.write(line + "\n");
    }
  }
  catch (IOException ex) {
    System.err.println(ex);
  }

  out.close();
  br.close();

  return;
}

@Override
public void run(Document doc, String[] annSetNames)
{
  String inputFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.sgml";
  String textFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.txt";
  String origTextFile = doc.getAbsolutePath() + Utils.SEPARATOR + "orig.raw.txt";
  String outFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.formatted";
  Utils.getConfig();

  try {
    FileReader reader = new FileReader(inputFile);
    FileWriter writer = new FileWriter(outFile);
    XMLReader xmlr = XMLReaderFactory.createXMLReader();
    xmlr.setContentHandler(handler);
    xmlr.setErrorHandler(handler);

    rawTextFile = new FileWriter(textFile);
    originalRawTextFile = new FileWriter(origTextFile);
    markups = new AnnotationSet(annSetNames[0]);
    anStack = new Stack<Annotation>();
    offset = 0;
    skip = 1;

    BufferedReader br = new BufferedReader(reader);
    try {
      format(br, writer);
      reader.close();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    reader = new FileReader(outFile);

    // Parse the incoming XML file.
    xmlr.parse(new InputSource(reader));
    rawTextFile.close();
    originalRawTextFile.close();
    addResultSet(doc,markups);
    reader.close();
    writer.close();
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

  /*
   * Skip some fields
   */
  if (FeatureUtils.memberArray(qName, FIELDS_TO_INCLUDE)) {
    skip--;
  }

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

  if (FeatureUtils.memberArray(qName, FIELDS_TO_INCLUDE)) {
    skip++;
    top.setEndOffset(offset);
  }
  else {
    top.setEndOffset(offset);
  }
}

/*
 * This prints out all the text between the tags we care about to 
 * a file.	 
 */
@Override
public void characters(char ch[], int start, int length)
{
  String text = new String(ch, start, length);

  text = unescapeText(text);
  char[] textCh = text.toCharArray();
  for (char element : textCh) {
    try {
      /*
       * If the current tag is one we don't care about, then 
       * replace all characters with spaces. 
       */
      if (skip > 0) {
        rawTextFile.write(" ");
      }
      else {
        rawTextFile.write(element);
      }

      originalRawTextFile.write(element);
      offset++;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

public String unescapeText(String text)
{
  text = text.replaceAll("@amp;", "&    ");
  text = text.replaceAll("&amp;", "&    ");
  text = text.replaceAll("&AMP;", "&    ");
  text = text.replaceAll("&\\w\\w;", "    ");
  text = text.replaceAll("&\\w\\w\\w;", "     ");

  return text;
}
}
