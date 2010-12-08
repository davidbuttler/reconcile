/**
 * OpinionListPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.npPairViewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import reconcile.data.Annotation;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.ModifiedInstancesShort;


public class FeaturesPanel
    extends JPanel {

private static final long serialVersionUID = 4069946459842349854L;
// JTree treeComponent;
private JTable data;
private MainWindow mainWindow;
private DefaultTableModel model;

// private final Color unSelected = Color.lightGray;
// private final Color selected = new Color(175, 175, 255);

public FeaturesPanel(MainWindow mainw) {
  super();
  mainWindow = mainw;
  setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  // DefaultMutableTreeNode topSourceListNode = new DefaultMutableTreeNode("Source Node List");
  // treeComponent = new JTree(topSourceListNode);
  // addTreeAction();
  model = new DefaultTableModel();
  data = new JTable(new DefaultTableModel());
  data.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
  JScrollPane sPane = new JScrollPane(data);

  data.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  data.setAutoCreateRowSorter(true);
  // ListSelectionModel lsm = new DefaultListSelectionModel();
  data.getSelectionModel().addListSelectionListener(new FeatTableSelectionHandler());

  data.setDragEnabled(true);
  MouseListener listener = new DragMouseAdapter();
  data.addMouseListener(listener);
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
  ModifiedInstancesShort mis = mainWindow.features;
  String[][] values;
  if (mis != null) {
    String[] attNames = new String[mis.numAttributes()];
    for (int i = 0; i < mis.numAttributes(); i++) {
      attNames[i] = mis.attribute(i).name();
    }
    values = new String[mis.numInstances()][mis.numAttributes()];
    ;
    for (int j = 0; j < mis.numInstances(); j++) {
      InstanceShort cur = mis.instance(j);
      for (int i = 0; i < cur.numAttributes(); i++) {
        if (cur.attribute(i).isNumeric())
          if (cur.attribute(i).isFeature()) {
            values[j][i] = Double.toString(cur.attribute(i).getOriginalValue(cur.value(i)));
          }
          else {
            values[j][i] = Short.toString(cur.value(i));
          }
        else {
          values[j][i] = cur.attribute(i).value(cur.value(i));
        }
      }
    }
    model = new DefaultTableModel(values, attNames);
  }
  else {
    System.err.println("No data");
  }

  data.setModel(model);
}

public void setListModel()
{
  data.setModel(new DefaultTableModel());
}// method: setTreeModel

public int numVectors()
{
  return data.getModel().getRowCount();
}

private class FeatTableSelectionHandler
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

    for (int i = minIndex; i <= maxIndex; i++) {
      if (lsm.isSelectedIndex(i)) {
        String id1 = (String) data.getModel().getValueAt(data.convertRowIndexToModel(i), 1);
        String id2 = (String) data.getModel().getValueAt(data.convertRowIndexToModel(i), 2);

        // System.out.println("Highlighting "+id1+" and "+id2);
        int clInd = mainWindow.features.classIndex();
        String cl = (String) data.getModel().getValueAt(data.convertRowIndexToModel(i), clInd);
        Color hlt = Color.blue;
        if (cl.equals("+")) {
          hlt = Color.red;
        }
        // System.err.println("Adjusting "+id1+" and "+id2);
        Annotation np1 = mainWindow.npAnnots.get(id1);
        Annotation np2 = mainWindow.npAnnots.get(id2);

        // Higlight the annotation span
        mainWindow.highlightText(np1.getStartOffset(), np1.getEndOffset(), hlt);
        mainWindow.highlightText(np2.getStartOffset(), np2.getEndOffset(), hlt);
      }
    }
  }
}
}

}
