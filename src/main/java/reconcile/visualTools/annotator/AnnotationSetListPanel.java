/**
 * OpinionListPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.annotator;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.visualTools.util.AnnotationSetCellRenderer;
import reconcile.visualTools.util.DragMouseAdapter;
import reconcile.visualTools.util.IterableListModel;

public class AnnotationSetListPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;

// JTree treeComponent;
private JList anList;

private MainWindow mainWindow;

private IterableListModel<AnnotationSet> model;

private final Color unSelected = Color.lightGray;

private final Color selected = new Color(175, 175, 255);
List<Annotation> annotations;

public AnnotationSetListPanel(MainWindow mainw) {
  super();
  mainWindow = mainw;
  annotations = new ArrayList<Annotation>();

  setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  // DefaultMutableTreeNode topSourceListNode = new DefaultMutableTreeNode("Source Node List");
  // treeComponent = new JTree(topSourceListNode);
  // addTreeAction();
  model = new IterableListModel<AnnotationSet>();
  anList = new JList(model);
  anList.setFont(MainWindow.font);
  loadDocument(mainw.doc);
  anList.setCellRenderer(new AnnotationSetCellRenderer());
  anList.addFocusListener(new OpinionListFocusListener());
  ListSelectionModel listSelectionModel = anList.getSelectionModel();
  listSelectionModel.addListSelectionListener(new AnnotationSetListPanel.OpinionListSelectionHandler());
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

/**
 * @param doc
 */
private void loadDocument(Document doc)
{
  anList.clearSelection();
  List<String> annotationNames = doc.listAnnotationTypes();
  Collections.sort(annotationNames);
  model.clear();
  for (String name : annotationNames) {
    AnnotationSet as = doc.getAnnotationSet(name);
    model.add(as);
  }

}

public void addToPanel(JComponent inComp)
{
  removeAll();
  add(inComp);
}// method: addToPanel

public void clear()
{
  mainWindow.editPanel.clearAnnotationToModify();
  mainWindow.editPanel.clearAnnotationToSetMin();
  model.clear();
}

public void redraw()
{
  loadDocument(mainWindow.doc);
}

public AnnotationSet getSelectedAnnotation()
{
  int minIndex = anList.getMinSelectionIndex();
  System.out.println("min index: " + minIndex);
  int maxIndex = anList.getMaxSelectionIndex();
  if (minIndex < 0 || minIndex != maxIndex) return null;
  return model.get(anList.getSelectedIndex());
}

public int numAnnotations()
{
  return model.size();
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

    annotations.clear();
    for (int i = minIndex; i <= maxIndex; i++) {
      AnnotationSet a = model.get(maxIndex);
      annotations.addAll(a.getOrderedAnnots());
    }
    mainWindow.annotationPanel.redraw();

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
  annotations.clear();
  if (minIndex >= 0) {
    for (int i = minIndex; i <= maxIndex; i++) {
      AnnotationSet a = (AnnotationSet) anList.getModel().getElementAt(maxIndex);
      annotations.addAll(a.getOrderedAnnots());
    }
  }
  mainWindow.annotationPanel.redraw();
}

@Override
public void focusLost(java.awt.event.FocusEvent focusEvent)
{
  ((JList) focusEvent.getComponent()).setSelectionBackground(unSelected);
}
}

/**
 * @return
 */
public Collection<? extends Annotation> getAnnotations()
{
  return annotations;
}

/**
 * @param selected2
 */
public void select(AnnotationSet selectedSet)
{
  if (selectedSet == null || selectedSet.size() == 0) return;

  int selectedId = 0;
  for (int i = 0; i < model.getSize(); i++) {
    if (selectedSet.getName().equals((model.get(i)).getName())) {
      selectedId = i;
      break;
    }
  }
  anList.addSelectionInterval(selectedId, selectedId);

}

/**
 * @param orig
 */
public void select(String orig)
{
  int selectedId = -1;
  for (int i = 0; i < model.getSize(); i++) {
    if (orig.equals((model.get(i)).getName())) {
      selectedId = i;
      break;
    }
  }
  if (selectedId >= 0) {
    anList.addSelectionInterval(selectedId, selectedId);
  }
  else {
    System.out.println("could not select " + orig);
  }

}

}
