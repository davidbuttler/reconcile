/**
 * OpinionListPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.annotator;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.general.Constants;
import reconcile.visualTools.util.AnnotationCellRenderer;
import reconcile.visualTools.util.DragMouseAdapter;
import reconcile.visualTools.util.IterableListModel;

public class AnnotationListPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;

// JTree treeComponent;
private JList anList;

private MainWindow mainWindow;

private Map<String, Set<Annotation>> chains;

private IterableListModel<Annotation> model;

private final Color unSelected = Color.lightGray;

// private final Color selected = new Color(175, 175, 255);

private JLabel nextIdLabel;

private int sort_by_chains = -1;

private int maxId;

// private JTextField maxIdField;

public AnnotationListPanel(MainWindow mainw) {
  super();
  chains = new TreeMap<String, Set<Annotation>>();

  mainWindow = mainw;
  setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  // DefaultMutableTreeNode topSourceListNode = new DefaultMutableTreeNode("Source Node List");
  // treeComponent = new JTree(topSourceListNode);
  // addTreeAction();
  model = new IterableListModel<Annotation>();
  anList = new JList(model);
  anList.setFont(MainWindow.font);
  anList.setCellRenderer(new AnnotationCellRenderer(mainWindow));
  anList.addFocusListener(new OpinionListFocusListener());
  ListSelectionModel listSelectionModel = anList.getSelectionModel();
  listSelectionModel.addListSelectionListener(new AnnotationListPanel.OpinionListSelectionHandler());
  JScrollPane sPane = new JScrollPane(anList);
  anList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  anList.setDragEnabled(true);
  MouseListener listener = new DragMouseAdapter();
  anList.addMouseListener(listener);
  this.setAlignmentX(Component.LEFT_ALIGNMENT);
  this.setAlignmentY(Component.TOP_ALIGNMENT);
  add(sPane);

  JPanel maxIdPanel = new JPanel();
  maxIdPanel.setLayout(new BoxLayout(maxIdPanel, BoxLayout.X_AXIS));
  nextIdLabel = new JLabel("next ID:");
  nextIdLabel.setFont(MainWindow.font);
  maxIdPanel.add(nextIdLabel);
  add(maxIdPanel);

  JPanel hiliteAllPanel = new JPanel();
  hiliteAllPanel.setLayout(new BoxLayout(hiliteAllPanel, BoxLayout.X_AXIS));
  JButton hilite_all = new JButton("show all mentions");
  hilite_all.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      hiliteAllMentions();
    }
  });
  hilite_all.setFont(MainWindow.font);
  hiliteAllPanel.add(hilite_all);
  add(hiliteAllPanel);

  /*
    JPanel sortByChainPanel = new JPanel();
    sortByChainPanel.setLayout(new BoxLayout(sortByChainPanel, BoxLayout.X_AXIS));
    JButton sortByChain = new JButton("sort by chain");
    sortByChain.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sortByChain();
      }
    });
    sortByChain.setFont(MainWindow.font);
    sortByChainPanel.add(sortByChain);
    add(sortByChainPanel);
    */

  JPanel sortByChainPanel = new JPanel();
  sortByChainPanel.setLayout(new BoxLayout(sortByChainPanel, BoxLayout.X_AXIS));
  JCheckBox sortByChain = new JCheckBox("sort by chain");
  sortByChain.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      sortByChain();
    }
  });
  sortByChain.setFont(MainWindow.font);
  sortByChainPanel.add(sortByChain);
  add(sortByChainPanel);

  // maxIdField = new JTextField();
  // maxIdField.setFont(MainWindow.font);
  // maxIdField.setSize(10, 16);
  // maxIdPanel.add(maxIdField);
  // add(opinionList);
  // add(new JScrollPane(treeComponent));
}

private void setMaxId(int id)
{
  maxId = id;
  nextIdLabel.setText("next ID: " + (maxId + 1));
}

public int getMaxId()
{
  computeChains(model, chains);
  int max = -1;
  for (String id : chains.keySet()) {
    int myId = Integer.parseInt(id);
    if (myId > max) {
      max = myId;
    }
  }
  return max;
}

public void clear()
{
  chains.clear();
  model.clear();
  anList.clearSelection();
  anList.repaint();
}

public void redraw()
{
  anList.clearSelection();
  model.clear();

  for (Annotation a : mainWindow.annotationSetPanel.getAnnotations()) {
    model.add(a);
  }
  computeChains(model, chains);
  int max = -1;
  for (String id : chains.keySet()) {
    int myId = Integer.parseInt(id);
    if (myId > max) {
      max = myId;
    }
  }
  setMaxId(max);

  if (sort_by_chains == 1) {
    model.clear();
    System.out.println("size: " + model.size());
    HashSet<String> seen = new HashSet<String>();
    for (Set<Annotation> s : chains.values()) {
      for (Annotation a : s) {
        if (seen.contains(a.getAttribute(Constants.CE_ID))) {
          break;
        }
        seen.add(a.getAttribute(Constants.CE_ID));
        model.add(a);
      }
    }
  }
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
  anList.setModel(new IterableListModel<Annotation>());
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
  int maxIndex = lsm.getMaxSelectionIndex();
  List<Integer> indices = new ArrayList<Integer>();
  for (int i = minIndex; i <= maxIndex; i++) {
    if (lsm.isSelectedIndex(i)) {
      indices.add(i);
    }
  }
  highlightSpans(indices);
}
}

public void highlightSpans(Annotation a)
{
  Map<String, String> features = a.getFeatures();
  String id = features.get(Constants.CE_ID);

  // AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
  ArrayList<Integer> indices = new ArrayList<Integer>();
  int j = -1;
  for (Annotation ant : model) {
    ++j;
    features = ant.getFeatures();
    if (id.equals(features.get(Constants.CE_ID))) {
      indices.add(j);
      break;
    }
  }
  highlightSpans(indices);
}

public void highlightSpans(List<Integer> indices)
{
  mainWindow.editPanel.clearAnnotationToModify();
  mainWindow.editPanel.clearAnnotationToSetMin();

  mainWindow.tvPanel.clearHighlights();

  // if only one annotation is selected,
  // highlight all mentions in the corresponding coreference chain
  if (indices.size() == 1) {
    Annotation a = model.get(indices.get(0));
    mainWindow.editPanel.setAnnotation(a, true);
    String id = a.getAttribute(Constants.CE_ID);
    if (id != null) {
      Set<Annotation> chain = chains.get(id);
      if (chain != null) {
        for (Annotation cur : chain) {
          if (!cur.equals(a)) {
            mainWindow.highlightText(cur.getStartOffset(), cur.getEndOffset(), Color.GREEN);
          }
        }
      }
    }
  }

  // highlight the (one or more) selected annotation
  for (int i : indices) {
    Annotation a = model.get(i);
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

/**
 * 
 */
public void deletedSelected()
{
  mainWindow.editPanel.clearAnnotationToModify();
  AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
  if (set != null) {
    // Find out which indexes are selected.
    int minIndex = anList.getMinSelectionIndex();
    int maxIndex = anList.getMaxSelectionIndex();
    for (int i = minIndex; i <= maxIndex; i++) {
      if (anList.isSelectedIndex(i)) {
        Annotation a = model.get(i);
        set.remove(a);
      }
    }
    mainWindow.getDoc().writeAnnotationSet(set);

  }

  AnnotationSet selected = mainWindow.annotationSetPanel.getSelectedAnnotation();
  mainWindow.annotationSetPanel.redraw();
  mainWindow.annotationSetPanel.select(selected);
  redraw();
}

public JList getAnnotationList()
{
  return anList;
}

/**
 *
 */
public Annotation getAndDeleteAnnotation(int idx)
{
  Annotation result = null;
  AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
  if (set != null) {
    result = model.get(idx);
    set.remove(result);
  }
  return result;
}

private void hiliteAllMentions()
{
  // AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
  ArrayList<Integer> indices = new ArrayList<Integer>();
  int j = 0;
  for (@SuppressWarnings("unused")
  Annotation ant : model) {
    indices.add(j++);
  }
  highlightSpans(indices);
}

private void sortByChain()
{
  sort_by_chains *= -1;
  redraw();
}

}
