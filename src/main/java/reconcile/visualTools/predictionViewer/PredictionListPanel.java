/**
 * OpinionListPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.predictionViewer;

import gov.llnl.text.util.InputStreamLineIterable;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.visualTools.util.DragMouseAdapter;
import reconcile.visualTools.util.IterableListModel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PredictionListPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;

// JTree treeComponent;
private JList predList;

private MainWindow mainWindow;

private Map<String, Set<Annotation>> chains;

private IterableListModel<String> model;

private final Color unSelected = Color.lightGray;

// private final Color selected = new Color(175, 175, 255);


private static final Pattern pSpace = Pattern.compile("\\s+");

private static final Pattern pComma = Pattern.compile(",");

private JComboBox thresholdSelector;
private Map<String, Annotation> idMap = Maps.newHashMap();

private JSplitPane splitPane;

// private JTextField maxIdField;

public PredictionListPanel(MainWindow mainw) {
  super();
  chains = new TreeMap<String, Set<Annotation>>();

  mainWindow = mainw;

  setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  // DefaultMutableTreeNode topSourceListNode = new DefaultMutableTreeNode("Source Node List");
  // treeComponent = new JTree(topSourceListNode);
  // addTreeAction();
  model = new IterableListModel<String>();
  predList = new JList(model);
  predList.setFont(MainWindow.font);
//  predList.setCellRenderer(new AnnotationCellRenderer(mainWindow));
  predList.addFocusListener(new OpinionListFocusListener());
  ListSelectionModel listSelectionModel = predList.getSelectionModel();
  listSelectionModel.addListSelectionListener(new PredictionListPanel.OpinionListSelectionHandler());
  JScrollPane sPane = new JScrollPane(predList);
  predList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  predList.setDragEnabled(true);
  MouseListener listener = new DragMouseAdapter();
  predList.addMouseListener(listener);
  this.setAlignmentX(Component.LEFT_ALIGNMENT);
  this.setAlignmentY(Component.TOP_ALIGNMENT);
//  add(sPane);

  JPanel thresholdPanel = new JPanel();
//  thresholdPanel.setLayout(new BoxLayout(thresholdPanel, BoxLayout.X_AXIS));
  thresholdPanel.setLayout(new FlowLayout());
  JLabel thresholdLabel = new JLabel("threshold:");
  thresholdLabel.setFont(MainWindow.font);
  thresholdPanel.add(thresholdLabel);
  thresholdSelector = new JComboBox(new String[]{"0.0","0.1","0.2","0.3","0.4","0.5","0.6","0.7","0.8","0.9", "1.0"});
  thresholdPanel.add(thresholdSelector);
//  add(thresholdPanel);

  splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sPane, thresholdPanel);
  splitPane.setDividerLocation(660);
  add(splitPane);
}


public void clear()
{
  chains.clear();
  model.clear();
  predList.clearSelection();
  predList.repaint();
}

public void redraw()
{
  predList.clearSelection();
  model.clear();

  Document doc = mainWindow.getDoc();
  
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);
  idMap.clear();
  for (Annotation a: nps) {
//    System.out.println("id: "+a.getId()+", features: " +a.getFeatures());
    idMap.put(a.getAttribute("NO"), a);
  }
  
  InputStream predictions = doc.readPredictionFile();
  if (predictions != null) {
    try {
      for (String line : InputStreamLineIterable.iterate(predictions)) {
        String[] s = pSpace.split(line);
        if (s.length != 2) {
          System.out.println("bad prediction line: " + line);
          continue;
        }
        double val = Double.parseDouble(s[1]);
        if (val >= getThreshold()) {
          String[] c = pComma.split(s[0]);
          if (c.length != 3) {
            System.out.println("bad prediction line: " + line);
            continue;
          }
          String aId = c[1];
          String bId = c[2];
          model.add(aId + "," + bId);
        }
      }
    }
    catch (NumberFormatException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

}

/**
 * @return
 */
private double getThreshold()
{
  String val = (String) thresholdSelector.getSelectedItem();
  System.out.println("threshold: "+val);
  return Double.parseDouble(val);
}


/**
 * 
 */
public static void computeChains(Iterable<Annotation> model, Map<String, Set<Annotation>> chains)
{
  chains.clear();
  // first go through and make every annotation match a chain
  for (Annotation a : model) {
    String id = a.getAttribute(Constants.CE_ID);
    if (id != null) {
      try {
        Set<Annotation> chain = chains.get(id);
        if (chain == null) {
          chain = new TreeSet<Annotation>();
          chains.put(id, chain);
        }
        chain.add(a);
      }
      catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
  }

  // now merge chains when one refs the other
  for (Annotation a : model) {
    String id = a.getAttribute(Constants.CE_ID);
    String ref = a.getAttribute(Constants.CLUSTER_ID);
    if (id != null) {
      Set<Annotation> idChain = chains.get(id);
      if (ref != null) {
        Set<Annotation> refChain = chains.get(ref);
        if (refChain == null) {
          refChain = new TreeSet<Annotation>();
          chains.put(ref, refChain);
        }
        refChain.addAll(idChain);
        for (Annotation refA : refChain) {
          String oId = refA.getAttribute(Constants.CE_ID);
          chains.put(oId, refChain);
        }
      }
    } // else we don't need to merge with anybody else so nothing left to do
  }

}

@SuppressWarnings("unused")
private void printChain(String key)
{
  System.out.print("chain " + key + ": ");
  for (Annotation z : chains.get(key)) {
    System.out.print(z.getAttribute(Constants.CE_ID) + ", ");
  }
  System.out.println();
}

public void setListModel()
{
  predList.setModel(new IterableListModel<Annotation>());
}// method: setTreeModel

public int numAnnotations()
{
  return model.getSize();
}

private class OpinionListSelectionHandler
    implements ListSelectionListener {

// I broke this into two functions in order to fix the
// situation: after clicking 'save,' all highlighting in
// the text disappears
public void valueChanged(ListSelectionEvent e)
{

  // Find out which indexes are selected.
  ListSelectionModel lsm = (ListSelectionModel) e.getSource();
  int minIndex = lsm.getMinSelectionIndex();
  highlightSpans(model.get(minIndex));
}
}

public void highlightSpans(String prediction)
{
  String c[] = pComma.split(prediction);
  Annotation a =idMap.get(c[0]);
  Annotation b = idMap.get(c[1]);
  
  // AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
  List<Annotation> indices = Lists.newArrayList(a, b);
  
  highlightSpans(indices);
}

public void highlightSpans(List<Annotation> annotes)
{

  mainWindow.tvPanel.clearHighlights();


  // highlight the (one or more) selected annotation
  for (Annotation a : annotes) {
    // Higlight the annotation span
    mainWindow.highlightText(a.getStartOffset(), a.getEndOffset(), Color.BLUE);
    Map<String, String> features = a.getFeatures();
    String start = features.get(Constants.HEAD_START);
    String end = features.get(Constants.HEAD_END);
    if (start != null) {
      int start_ = Integer.parseInt(start);
      int end_ = Integer.parseInt(end);
      mainWindow.highlightText(start_, end_, Color.RED);
    }
  }

}

private class OpinionListFocusListener
    extends FocusAdapter {

@Override
public void focusGained(FocusEvent focusEvent)
{
  // I'm commenting out this block, since highlighting is performed
  // by valueChanged; this block causes the problem that, the
  // first time an annotation is highlighted, the entire
  // chain isn't highlighted -dah 2/1/09
  /*
    JList opList = (JList) focusEvent.getComponent();
    opList.setSelectionBackground(selected);
    mainWindow.tvPanel.clearHighlights();
    // Find out which indexes are selected.
    int minIndex = opList.getMinSelectionIndex();
    int maxIndex = opList.getMaxSelectionIndex();
    for (int i = minIndex; i <= maxIndex; i++) {
      if (opList.isSelectedIndex(i)) {
        // Highlight the opinion source
        Annotation a = model.get(i);
        mainWindow.highlightText(a.getStartOffset(), a.getEndOffset(), Color.BLUE);
      }
    }
    */
}

@Override
public void focusLost(java.awt.event.FocusEvent focusEvent)
{
  ((JList) focusEvent.getComponent()).setSelectionBackground(unSelected);
}
}


public JList getAnnotationList()
{
  return predList;
}

}
