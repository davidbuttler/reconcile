package reconcile.featureExtractor;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import reconcile.data.AnnotationSet;
import reconcile.data.Document;


public class ParagraphSplitter
    extends InternalAnnotator {

/**
   *
   */
private static final String PARAGRAPH = "paragraph_split";
private final Pattern paragraphRE = Pattern.compile("(\\n\\W*\\n)|(\\n\\t)|(\\n\\s\\s+)");

public ParagraphSplitter() {
}

@Override
public void run(Document doc, String[] annSetNames)
{
  AnnotationSet paragraph = parse(doc.getText(), annSetNames[0]);

  addResultSet(doc,paragraph);
}

public AnnotationSet parse(String text, String paragraphSetName)
{
  AnnotationSet paragraph = new AnnotationSet(paragraphSetName);

  String[] pars = paragraphRE.split(text);
  int prevEndPos = 0;
  int counter = 0;

  for (String par : pars) {
    if (par.length() > 0 && !par.matches("\\s+")) {
      int newEndPos = text.indexOf(par, prevEndPos) + par.length();
      newEndPos = Math.min(newEndPos, text.length() - 1);

      Map<String, String> feat = new TreeMap<String, String>();
      feat.put("parNum", String.valueOf(counter++));
      // add each sentence to the annotation set
      paragraph.add(prevEndPos, newEndPos + 1, PARAGRAPH, feat);

      prevEndPos = newEndPos;
    }
  }

  if (prevEndPos < text.length() && !text.substring(prevEndPos).trim().equals("")) {
    Map<String, String> feat = new TreeMap<String, String>();
    feat.put("parNum", String.valueOf(counter++));
    paragraph.add(prevEndPos, text.length() - 1, PARAGRAPH, feat);
  }

  if (paragraph.size() == 0) {
    Map<String, String> feat = new TreeMap<String, String>();
    feat.put("parNum", String.valueOf(counter++));
    paragraph.add(0, text.length() - 1, PARAGRAPH, feat);
  }

  return paragraph;
}
}
