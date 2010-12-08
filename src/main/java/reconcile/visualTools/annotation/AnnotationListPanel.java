/**
 * OpinionListPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.annotation;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.visualTools.util.AnnotationCellRenderer;
import reconcile.visualTools.util.DragMouseAdapter;

public class AnnotationListPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;

// JTree treeComponent;
private JList anList;

private MainWindow mainWindow;

private ArrayList<AnnotationSet> anSets;

private DefaultListModel model;

private final Color unSelected = Color.lightGray;

private final Color selected = new Color(175, 175, 255);

public AnnotationListPanel(MainWindow mainw) {
  super();
  mainWindow = mainw;
  setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  // DefaultMutableTreeNode topSourceListNode = new DefaultMutableTreeNode("Source Node List");
  // treeComponent = new JTree(topSourceListNode);
  // addTreeAction();
  anSets = mainw.annotSets == null ? new ArrayList<AnnotationSet>() : mainw.annotSets;
  model = new DefaultListModel();
  anList = new JList(model);
  for (AnnotationSet as : anSets) {
    for (Annotation a : as) {
      model.addElement(a);
    }
  }
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
  // add(opinionList);
  // add(new JScrollPane(treeComponent));
}

public void addToPanel(JComponent inComp)
{
  removeAll();
  add(inComp);
}// method: addToPanel

public void redraw()
{
  anSets = mainWindow.annotSets;
  System.out.println("Adding annotation set #" + anSets.size());
  model = new DefaultListModel();
  for (AnnotationSet as : anSets) {
    for (Annotation a : as) {
      model.addElement(a);
    }
  }
  anList.setModel(model);
}

public void setListModel()
{
  anList.setModel(new DefaultListModel());
}// method: setTreeModel

public int numAnnotations()
{
  return anList.getModel().getSize();
}

private class OpinionListSelectionHandler
    implements ListSelectionListener {

public void valueChanged(ListSelectionEvent e)
{
  ListSelectionModel lsm = (ListSelectionModel) e.getSource();

  // int firstIndex = e.getFirstIndex();
  // int lastIndex = e.getLastIndex();
  // boolean isAdjusting = e.getValueIsAdjusting();

  if (lsm.isSelectionEmpty()) {
    mainWindow.tvPanel.clearHighlights();
  }
  else {

    mainWindow.tvPanel.clearHighlights();
    // Find out which indexes are selected.
    int minIndex = lsm.getMinSelectionIndex();
    int maxIndex = lsm.getMaxSelectionIndex();

    if (maxIndex - minIndex == 0) {
      // if only one annotation selected
      Annotation a = (Annotation) anList.getModel().getElementAt(maxIndex);
      if (a.getType().equalsIgnoreCase("NP") && a.getAttribute("CorefID") != null) {
        String coref = a.getAttribute("CorefID");
        // System.err.println("Coreferent np "+coref);
        int corefID = Integer.parseInt(coref);
        if (corefID >= 0) {
          ListModel lm = anList.getModel();
          for (int i = 0; i < lm.getSize(); i++) {

            Annotation cur = (Annotation) lm.getElementAt(i);
            if (cur.getType().equalsIgnoreCase("NP") && coref.equalsIgnoreCase(cur.getAttribute("CorefID"))
                && !cur.equals(a)) {
              mainWindow.highlightText(cur.getStartOffset(), cur.getEndOffset(), Color.GREEN);
              // System.err.println("anti "+cur);
            }
          }
        }
      }
    }
    for (int i = minIndex; i <= maxIndex; i++) {
      if (lsm.isSelectedIndex(i)) {
        Annotation a = (Annotation) anList.getModel().getElementAt(i);
        // If there is a GOV or AUTO SPAN attribute, highlight it in gray
        String gov = a.getAttribute("GOV");
        if (gov == null) {
          gov = a.getAttribute("AutoSpan");
        }
        if (gov != null) {
          String[] govA = gov.split("\\,");
          int govStart = Integer.parseInt(govA[0]);
          int govEnd = Integer.parseInt(govA[1]);
          mainWindow.highlightText(govStart, govEnd, Color.GRAY);
        }
        // Higlight the annotation span
        mainWindow.highlightText(a.getStartOffset(), a.getEndOffset(), Color.BLUE);
      }

    }

  }
}
}

private class OpinionListFocusListener
    extends FocusAdapter {

@Override
public void focusGained(FocusEvent focusEvent)
{
  JList opList = (JList) focusEvent.getComponent();
  opList.setSelectionBackground(selected);
  mainWindow.tvPanel.clearHighlights();
  // Find out which indexes are selected.
  int minIndex = opList.getMinSelectionIndex();
  int maxIndex = opList.getMaxSelectionIndex();
  for (int i = minIndex; i <= maxIndex; i++) {
    if (opList.isSelectedIndex(i)) {
      // Highlight the opinion source
      Annotation a = (Annotation) anList.getModel().getElementAt(i);
      mainWindow.highlightText(a.getStartOffset(), a.getEndOffset(), Color.BLUE);
    }
  }
}

@Override
public void focusLost(java.awt.event.FocusEvent focusEvent)
{
  ((JList) focusEvent.getComponent()).setSelectionBackground(unSelected);
}
}
}
