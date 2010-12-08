/*
 * 
 * ves; March 28, 2007
 */

package reconcile.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

import reconcile.general.Utils;


/**
 * This is an implementation of the AnnotationWriter interface. Writes out annotations embedded as sgml in the original
 * text.
 * 
 */

public class AnnotationWriterEmbedded {

public void write(AnnotationSet anns, String dirName, String comment)
{
  String filename = dirName + "/" + anns.getName();
  try {
    PrintWriter out = new PrintWriter(new File(filename));
    write(anns, out, comment);
  }
  catch (FileNotFoundException fnfe) {
    throw new RuntimeException(fnfe);
  }
}

/** Write out an annotation set */
public void write(AnnotationSet anns, PrintWriter out, String text)
{
  Annotation[] anotL = anns.getOrderedAnnots(new OffsetComparatorNestedLast());

  // First, order all annoation tags by the offset
  Stack<Tag> endTags = new Stack<Tag>();
  ArrayList<Tag> tags = new ArrayList<Tag>();
  for (Annotation a : anotL) {
    // System.out.println(a);
    if (a.getStartOffset() == a.getEndOffset()) {
      Tag tag = getSingleTag(a);
      int curOf = a.getStartOffset();
      while (endTags.size() > 0 && endTags.peek().offset <= curOf) {
        tags.add(endTags.pop());
      }
      tags.add(tag);
    }
    else {
      Tag tag = getStartTag(a);
      int curOf = a.getStartOffset();

      while (endTags.size() > 0 && endTags.peek().offset <= curOf) {
        tags.add(endTags.pop());
      }
      tags.add(tag);
      endTags.push(getEndTag(a));
    }
  }
  while (!endTags.isEmpty()) {
    tags.add(endTags.pop());
  }
  text += " ";
  int previous = 0;
  for (Tag t : tags) {
    // System.out.println(t);
    int of = t.offset;
    // System.out.println(previous+"--"+of);
    String spanText;
    try {
      spanText = Utils.getAnnotText(previous, of, text);
    }
    catch (Exception e) {
      System.out.println(t);
      System.out.println(previous + "--" + of);
      System.out.println(anns.getOverlapping(of - 1, previous + 1));
      throw new RuntimeException(e);
    }
    out.print(spanText + t.text);
    previous = of;
  }
  if(previous<text.length()-1){
	  out.print(Utils.getAnnotText(previous, text.length()-1, text));
  }
  out.flush();
}

static private class Tag {

public Tag(String t, int o) {
  text = t;
  offset = o;
}
public String text;
public int offset;

@Override
public String toString()
{
  return text + " --> " + offset;
}
}

public Tag getStartTag(Annotation a)
{
  StringBuilder text = new StringBuilder(a.getType());
  Map<String, String> features = a.getFeatures();
  if (features != null) {
    for (String currFeat : features.keySet()) {
      text.append(" ").append(currFeat).append("=\"").append((features.get(currFeat)).replaceAll("\\n", " ")).append(
          "\"");
    }
  }
  String ttext = "<" + text.toString() + ">";
  Tag result = new Tag(ttext, a.getStartOffset());
  return result;
}

public Tag getEndTag(Annotation a)
{
  String text = a.getType();
  text = "</" + text + ">";
  Tag result = new Tag(text, a.getEndOffset());
  return result;
}

public Tag getSingleTag(Annotation a)
{
  StringBuilder text = new StringBuilder(a.getType());
  Map<String, String> features = a.getFeatures();
  if (features != null) {
    for (Object currFeat : features.keySet()) {
      text.append(" ").append(currFeat).append("=\"").append((features.get(currFeat)).replaceAll("\\n", " ")).append(
          "\"");
    }
  }
  String resultText = "</" + text.append(">").toString();
  Tag result = new Tag(resultText, a.getStartOffset());
  return result;
}
}
