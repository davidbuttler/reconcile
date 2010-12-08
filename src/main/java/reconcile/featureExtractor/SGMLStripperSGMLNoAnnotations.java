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
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Utils;


public class SGMLStripperSGMLNoAnnotations extends SGMLStripper {

private AnnotationSet markups;
private int skip;
private static final String[] FIELDS_TO_SKIP = { "DOCNO", "DD", "DOCID", "CO", "IN", "SO", "NWORDS", "STORYID" }; // "DATE",
                                                                                                                  // "TRAILER"
private boolean preamble = false;
private boolean headline = false;
private boolean trailer = false;
private boolean insertNewline = false;
private boolean inText = false;
FileWriter rawTextFile;
FileWriter originalRawTextFile;
int offset;
Stack<Annotation> anStack;

/*
 * Call the parent's constructor. 
 */
public SGMLStripperSGMLNoAnnotations() {
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

  try {
    while ((line = br.readLine()) != null) {
	  //System.out.println(line);
     if (line.startsWith("<DOC id")) {
        int rabIndex = line.indexOf(">");
        String outline = "<DOC" + line.substring(rabIndex, line.length()) + "\n";
        out.write(outline);
        continue;
     }

      line = line.replaceAll("&", "@amp;");
		out.write(line + "\n");
    }
  }
	catch (IOException ex) {
    System.err.println(ex);
  }

	out.flush();
	out.close();
	br.close();
}

@Override
public void run(Document doc, String[] annSetNames)
{
  String inputFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.sgml";
  String textFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.txt";
  String origTextFile = doc.getAbsolutePath() + Utils.SEPARATOR + "orig.raw.txt";
  AnnotationSet nps = new AnnotationSet("gsNPs");

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
    originalRawTextFile = new FileWriter(origTextFile);
    markups = new AnnotationSet(annSetNames[0]);
    anStack = new Stack<Annotation>();

    offset = 0;
    skip = 0;

    // Parse the incoming XML file.
    xmlr.parse(new InputSource(reader));

    doc.writeAnnotationSet(markups);
	 doc.writeAnnotationSet(nps);
    rawTextFile.close();
    originalRawTextFile.close();
  }
  catch (IOException ex) {
    throw new RuntimeException(ex);
  }
  catch (SAXException e) {
    throw new RuntimeException(e);
  }
}

/*
 * Grabs the opening SGML tag. 
 */
@Override
public void startElement(String uri, String name, String qName, Attributes atts)
{

  /*
   * Skipping the following fields: DOCNO, DD, SO, IN, DATELINE
   */

  if (FeatureUtils.memberArray(qName, FIELDS_TO_SKIP)) {
    skip++;
  }

  if ("PREAMBLE".equals(qName) || "SLUG".equals(qName)) {
    preamble = true;
  }
  if ("TEXT".equals(qName) || "TXT".equals(qName)) {
    inText = true;
  }
  if ("HL".equals(qName)) {
    headline = false;
  }
  if ("TRAILER".equals(qName)) {
    trailer = true;
  }

  Map<String, String> attributes = new TreeMap<String, String>();

  for (int i = 0; i < atts.getLength(); i++) {
    String n = atts.getQName(i);
    String val = atts.getValue(i);
    // The min attribute refers to text, so it needs to be unescaped like
    // the rest of the text
    if (n.equals("MIN")) {
      val = unescapeText(val);
      if (preamble) {
        val = val.replaceAll("-", " ");
      }
    }
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
  if ("PREAMBLE".equals(qName) || "SLUG".equals(qName)) {
    preamble = false;
  }
  if ("TEXT".equals(qName) || "TXT".equals(qName)) {
    inText = false;
  }
  if ("HL".equals(qName)) {
    headline = false;
  }
  if ("SLUG".equals(qName)) {
    try {
      rawTextFile.write("\n");
      originalRawTextFile.write("\n");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    offset++;
  }
  if ("TRAILER".equals(qName)) {
    trailer = false;
  }
  if (FeatureUtils.memberArray(qName, FIELDS_TO_SKIP)) {// ||"DATELINE".equals(qName))
    skip--;
    top.setEndOffset(offset);
  }
  else {
    top.setEndOffset(offset);
  }
}

/*
 * This prints out all the text between the tags we care about to 
 * a file. 
 *
 */
@Override
public void characters(char ch[], int start, int length)
{
  String text = "";
  for (int i = start; i < start + length; i++) {
    text += ch[i];
  }

  text = unescapeText(text);
  if (headline) {
    text = text.replaceAll("@", " ");
  }

  // Clean the preamble -- remove -'s and double newlines
  if (preamble) {
    // System.out.println("Text: ("+insertNewline+") "+text);
    if (insertNewline && ch[start] == '\n') {
      text = "\n" + text;
    }
    text = text.replaceAll("-", " ");
    text = text.replaceAll("([A-Z\\)]\\s*\\n)", "$1\n");
    text = text.replaceAll("BC(\\W*)", "  $1");
    insertNewline = text.matches("(.|\\n)*[A-Z]\\W?");

    // System.out.println("next: "+ch[start+length]);
    // System.out.println("New : "+text);
  }
  else {
    insertNewline = false;
    String end = "";
    if (text.endsWith("\n")) {
      end = "\n";
    }
    String[] texts = text.split("\n");
    String text1 = "";
    boolean first = true;
    for (String t : texts) {
      if (inText && t.startsWith("@")) {
        t = t.replaceAll(".*", " ");
        // System.out.println(t);
      }
      if (first) {
        first = false;
      }
      else {
        text1 += "\n";
      }
      text1 += t;
    }
    text1 += end;
    // if(!text.equals(text1))
    // System.out.println("!===="+text1+"-"+text);
    text = text1;
  }
  // Clean up the trailer
  if (trailer && !text.matches("(\\d|\\-)+")) {
    text = text.replaceAll(".*", " ");
  }

  char[] textCh = text.toCharArray();
  // for (int i = start; i < start + length; i++) {
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
  text = text.replaceAll("@amp;", "&");
  text = text.replaceAll("&MD;", "-");
  text = text.replaceAll("&AMP;", "&");
  text = text.replaceAll("&LR;", "");

  text = text.replaceAll("``|''", "\"");
  return text;
}

}
