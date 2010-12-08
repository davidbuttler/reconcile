package reconcile.featureExtractor;

/*
 * SGMLStripperACEKey.java nathan; July 7, 2008 Removes SGML tags from ACE corpora; produces ace_annots. This class
 * removes XML tags from the key.xml file and provides info for scoring.
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
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import reconcile.SystemConfig;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;
import reconcile.general.Utils;


public class SGMLStripperACEKey
    extends SGMLStripper {

private AnnotationSet nps;
private int skip = 0;
private Stack<Annotation> anStack;
private int id;
private int numMentions = 0;
private String semantic = "";
private boolean inHead = false;
private boolean inExtent = false;
private boolean inStart = false;
private boolean inEnd = false;
private boolean inType = false;
private String currentMin = "";
private String currentStart = "";
private String currentEnd = "";
private String currentType = ""; // for ace03/phase 2 only
private SystemConfig cfg;
private static final String[] FIELDS_TO_SKIP = { "event", "event_argument", "event_mention", "relation",
    "relation_argument", "relation_mention", "relation_mention_argument", "timex2", "time2_mention", "timex2_argument",
    "entity_attributes", "value", "value_mention", "value_argument" };

public SGMLStripperACEKey() {
  super();
}

@Override
public void format(BufferedReader br, FileWriter out)
    throws IOException
{
  return;
}

public String translateNEType(String sem)
{
  if (sem.startsWith("LOC"))
    return "LOCATION";
  else if (sem.startsWith("ORG"))
    return "ORGANIZATION";
  else if (sem.startsWith("GPE"))
    return "LOCATION";
  else if (sem.startsWith("PER"))
    return "PERSON";
  else if (sem.startsWith("FAC"))
    return "LOCATION";
  else if (sem.startsWith("VEH"))
    return "VEHICLE";
  else
    return "OTHER";
}

@Override
public void run(Document doc, String[] annSetNames)
{
  String keyFile = doc.getAbsolutePath() + Utils.SEPARATOR + "key.xml";
  cfg = Utils.getConfig();
  id = 0;
  inHead = false;
  inExtent = false;
  inStart = false;
  inEnd = false;
  inType = false;
  skip = 0;
  currentStart = "";
  currentEnd = "";
  currentMin = "";
  currentType = "";


  try {
    FileReader reader = new FileReader(keyFile);
    XMLReader xmlr = XMLReaderFactory.createXMLReader();
    AnnotationSet gsNEs;

    xmlr.setContentHandler(handler);
    xmlr.setErrorHandler(handler);

    AnnotationSet markups = doc.getAnnotationSet(Constants.ORIG);
    markups.setName(annSetNames[0]);
    //markups.setName("ace_annots");
    nps = new AnnotationSet("nps");
    anStack = new Stack<Annotation>();

    // Parse the incoming XML file.
    xmlr.parse(new InputSource(reader));
    if (Constants.DEBUG) {
      nps.checkForCrossingWordBoundaries(doc);
    }

    gsNEs = new AnnotationSet(Constants.GS_NE_OUTPUT_FILE);

    // System.out.println(nps);
    for (Annotation k : nps.get("COREF")) {
      if (k.getAttribute("CATEGORY").startsWith("NAM")) {
        int start = Integer.parseInt(k.getAttribute(Constants.HEAD_START));
        int end = Integer.parseInt(k.getAttribute(Constants.HEAD_END));
        // if you don't want to use heads...
        // start = k.getStartOffset();
        // end = k.getEndOffset();
        String sem = k.getAttribute("SEMANTIC");
        String type = translateNEType(sem);
        if (type != null) {
          gsNEs.add(start, end, type);
        }
      }
    }

    // gold nps (with singletons)
    int counter = 0;
    // FileWriter fw = new FileWriter("duplicates-" + cfg.getDataset() + ".txt",true);
    // fw.write("Document: " + docNo + "\n");
    for (Annotation np : nps) {
      if (np.getType().equals("COREF")) {
//        Map<String, String> features = np.getFeatures();
        // features.put(Constants.CE_ID, new Integer(counter).toString());
        // features.put(Constants.HEAD_START, np.getAttribute(Constants.HEAD_START));
        // features.put(Constants.HEAD_END, np.getAttribute(Constants.HEAD_END));
        Annotation tmp = np;
        AnnotationSet dups = markups.getDuplicates(tmp);
        if (dups == null) {
          System.out.println("Null: " + tmp);
          throw new RuntimeException(np.toString());
        }
        for (Annotation an : dups) {
          // fw.write(Utils.getAnnotText(an, text).replaceAll("\\n", " ") + "-> (" + an.getStartOffset() + "-" +
          // an.getEndOffset() + ")\n");

          // remove the old annotation
          markups.remove(an);

          // Check different cases
          if (tmp.getAttribute(Constants.HEAD_START).equals(an.getAttribute(Constants.HEAD_START))
              && tmp.getAttribute(Constants.HEAD_END).equals(an.getAttribute(Constants.HEAD_END))) {
            // the two have the same heads
            String ref1 = an.getAttribute("REFERENCE");
            if (ref1 == null) {
              markups.add(tmp);
            }
            else {
              if (ref1.equalsIgnoreCase("INTENDED")) {
                // keep the first
                markups.add(an);
              }
              else {
                // String ref2 = tmp.getAttribute("REFERENCE");
                // if(!ref2.equalsIgnoreCase("INTENDED"))
                // throw new RuntimeException("No intended reference");
                // keep the second
                markups.add(tmp);
              }
            }
          }
          else {

            // set the offsets to the heads
            an.setStartOffset(new Integer(an.getAttribute(Constants.HEAD_START)));
            an.setEndOffset(new Integer(an.getAttribute(Constants.HEAD_END)));

            // fw.write("Changed to:\n");
            // fw.write(Utils.getAnnotText(an, text).replaceAll("\\n", " ") + "-> (" + an.getStartOffset() + "-" +
            // an.getEndOffset() + ")\n");

            tmp.setStartOffset(new Integer(tmp.getAttribute(Constants.HEAD_START)));
            tmp.setEndOffset(new Integer(tmp.getAttribute(Constants.HEAD_END)));
            // fw.write("New :\n");
            // fw.write(Utils.getAnnotText(tmp, text).replaceAll("\\n", " ") + "-> (" + tmp.getStartOffset() + "-" +
            // tmp.getEndOffset() + ")\n");

            if ((an.getStartOffset() != tmp.getStartOffset()) && (an.getEndOffset() != tmp.getEndOffset())) {
              markups.add(an);
              markups.add(tmp);
            }
            else {
              // fw.write("Still duplicate, only adding one.\n");
              markups.add(tmp);
            }
          }
        }
        if (dups.size() == 0) {
          markups.add(tmp);
        }
      }
    }
    AnnotationSet gsNPs = new AnnotationSet(Constants.GS_OUTPUT_FILE);
    counter = 0;
    for (Annotation n : markups.get("COREF")) {
      Map<String, String> features = new TreeMap<String, String>();
      features.put(Constants.CE_ID, String.valueOf(counter));
      features.put(Constants.HEAD_START, n.getAttribute(Constants.HEAD_START));
      features.put(Constants.HEAD_END, n.getAttribute(Constants.HEAD_END));
      gsNPs.add(++counter, n.getStartOffset(), n.getEndOffset(), "NP", features);
    }
    gsNPs = labelClusterIDs(nps);
    gsNPs.setName(Constants.GS_OUTPUT_FILE);
    addResultSet(doc,markups);
    addResultSet(doc,gsNEs);
    addResultSet(doc,gsNPs);
    // fw.write("--END-DOCUMENT--\n");
    // fw.close();
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
  if (FeatureUtils.memberArray(qName, FIELDS_TO_SKIP)) {
    skip++;
  }

  if (skip > 0) return;

  if (qName.equals("entity")) {
    numMentions = 0;
    semantic = atts.getValue("TYPE") + ":" + atts.getValue("SUBTYPE");
  }
  else if (qName.equals("entity_mention")) {
    numMentions++;
    id++;
    Map<String, String> attributes = new TreeMap<String, String>();

    // this could be overwritten later.
    attributes.put("SINGLETON", "FALSE");

    if (!cfg.getDataset().equals("ace03") || !cfg.getDataset().startsWith("ace-phase2")) {
      attributes.put("SEMANTIC", semantic);
    }

    for (int i = 0; i < atts.getLength(); i++) {
      String n = atts.getQName(i);
      String val = atts.getValue(i);

      // make sure the type and id values are not overwritten.
      if (n.equals("TYPE")) {
        attributes.put("CATEGORY", val);
      }
      else if (n.equals("ID")) {
        attributes.put("ID", String.valueOf(id));
      }
      else {
        attributes.put(n, val);
      }
    }

    // check to see if this coreferent to anything else
    if (numMentions > 1) {
      attributes.put("REF", String.valueOf(id - 1));
    }

    // save this annotation
    Annotation cur = new Annotation(id, -1, -1, "COREF", attributes);
    anStack.push(cur);
  }
  else if (qName.equals("entity_type")
      && (cfg.getDataset().equals("ace03") || cfg.getDataset().startsWith("ace-phase2"))) {
    inType = true;
    currentType = "";
  }
  else if (qName.equals("head")) {
    inHead = true;
  }
  else if (qName.equals("extent")) {
    inExtent = true;
  }
  else if (qName.equals("start") && (cfg.getDataset().equals("ace03") || cfg.getDataset().startsWith("ace-phase2"))) {
    inStart = true;
    currentStart = "";
  }
  else if (qName.equals("end") && (cfg.getDataset().equals("ace03") || cfg.getDataset().startsWith("ace-phase2"))) {
    inEnd = true;
    currentEnd = "";
  }
  else if (qName.equals("charseq")) {
    if (inHead) {
      Annotation top = anStack.pop();
      int start, end;

      if (cfg.getDataset().equals("ace05") || cfg.getDataset().equals("ace04")) {
        start = Integer.parseInt(atts.getValue("START"));
        end = Integer.parseInt(atts.getValue("END"));
        top.setAttribute(Constants.HEAD_START, String.valueOf(start));
        top.setAttribute(Constants.HEAD_END, String.valueOf(end + 1));
      }

      anStack.push(top);
    }
    else if (inExtent) {
      Annotation top = anStack.pop();
      int start, end;

      if (cfg.getDataset().equals("ace05") || cfg.getDataset().equals("ace04")) {
        start = Integer.parseInt(atts.getValue("START"));
        end = Integer.parseInt(atts.getValue("END"));
        top.setStartOffset(start);
        top.setEndOffset(end + 1);
      }

      anStack.push(top);
    }
    else {
      // charseq outside of head or extent.
      System.err.println("There was a problem, erroneous charseq found.");
    }
  }
}

/*
 * Grabs the closing tag. 
 */
@Override
public void endElement(String uri, String name, String qName)
{
  if (FeatureUtils.memberArray(qName, FIELDS_TO_SKIP)) {
    skip--;
    return;
  }

  if (skip > 0 || anStack.empty()) return;

  if (qName.equals("entity")) {
    if (numMentions == 1) {
      // then we have singleton.
      Annotation top = anStack.pop();
      top.setAttribute("SINGLETON", "TRUE");
      anStack.push(top);
    }

    for (Annotation a : anStack) {
      nps.add(a);
    }

    anStack.clear();
    semantic = "";
  }
  else if (qName.equals("entity_mention")) {
    currentMin = "";

    // ace03/phase2 ner stuff
    if (cfg.getDataset().equals("ace03") || cfg.getDataset().startsWith("ace-phase2")) {
      Annotation top = anStack.pop();
      if (!currentType.equals("")) {
        top.setAttribute("SEMANTIC", currentType);
      }
      anStack.push(top);
    }
  }
  else if (qName.equals("entity_type")) { // && (cfg.getDataset().equals("ace03") ||
                                          // cfg.getDataset().startsWith("ace-phase2"))) {
    inType = false;
  }
  else if (qName.equals("head")) {
    Annotation top = anStack.pop();
    if (!currentMin.equals("") && !cfg.getDataset().equals("ace03")) {
      top.setAttribute("MIN", currentMin);
      currentMin = "";
    }
    inHead = false;
    anStack.push(top);
  }
  else if (qName.equals("extent")) {
    inExtent = false;
  }
  else if (qName.equals("charseq"))
    return;
  else if (qName.equals("start")) {
    Annotation top = anStack.pop();
    if (inHead) {
      if (!currentStart.equals("")) {
        int start = new Integer(currentStart);
        top.setAttribute(Constants.HEAD_START, String.valueOf(start));
      }
    }
    else {
      if (!currentStart.equals("")) {
        int start = new Integer(currentStart);
        top.setStartOffset(start);
      }
    }
    currentStart = "";
    anStack.push(top);
    inStart = false;
  }
  else if (qName.equals("end")) {
    Annotation top = anStack.pop();

    if (inHead) {
      // System.out.println("Current end "+currentEnd);
      if (!currentEnd.equals("")) {
        int end = new Integer(currentEnd);
        top.setAttribute(Constants.HEAD_END, String.valueOf(end + 1));
      }
    }
    else {
      // System.out.println("Current end "+currentEnd);
      if (!currentEnd.equals("")) {
        int end = new Integer(currentEnd);
        top.setEndOffset(end + 1);
      }
    }
    currentEnd = "";
    anStack.push(top);
    inEnd = false;
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

  if (inHead && (!cfg.getDataset().equals("ace03") && !cfg.getDataset().startsWith("ace-phase2"))) {
    currentMin += text;
  }

  // grab the ne for ace 03
  if (inType && (cfg.getDataset().equals("ace03") || cfg.getDataset().startsWith("ace-phase2"))) {
    currentType += text;
    inType = false;
  }

  if (inHead && !inExtent && inStart) {
    currentStart += text;
  }
  else if (inHead && !inExtent && inEnd) {
    // System.out.println("OLD end: "+currentEnd);
    currentEnd += text;
  }
  else if (!inHead && inExtent && inStart) {
    currentStart += text;
  }
  else if (!inHead && inExtent && inEnd) {
    // System.out.println("OLD end: "+currentEnd);
    currentEnd += text;
  }
}
}
