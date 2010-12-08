/**
 * OpinionListPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.predictionViewer;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PredictionEditPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;


private JTextField status;

private JTextField startField;

private JTextField endField;

private JTextField idField;

private JTextField refField;



public PredictionEditPanel(MainWindow mainw) {
  super();
  setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  // create a panel for the status and the buttons
  add(createButtonPanel());
  // create a panel to enter the info
  add(createOffsetPanel());

  // DefaultMutableTreeNode topSourceListNode = new DefaultMutableTreeNode("Source Node List");
  // treeComponent = new JTree(topSourceListNode);
  // addTreeAction();
  this.setAlignmentX(Component.LEFT_ALIGNMENT);
  this.setAlignmentY(Component.TOP_ALIGNMENT);
  // add(opinionList);
  // add(new JScrollPane(treeComponent));
}


/**
 * @return
 */
private Component createOffsetPanel()
{
  JPanel panel = new JPanel();
  panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
  JPanel offsetPanel = new JPanel();
  offsetPanel.setLayout(new BoxLayout(offsetPanel, BoxLayout.Y_AXIS));
  offsetPanel.add(new JLabel("Annotation:"));

  JPanel startPanel = new JPanel();
  startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.X_AXIS));
  JLabel startLabel = new JLabel("Start: ");
  startLabel.setFont(MainWindow.font);
  startPanel.add(startLabel);
  startPanel.add(Box.createGlue());
  startField = new JTextField();
  startField.setFont(MainWindow.font);
  startPanel.add(startField);
  offsetPanel.add(startPanel);

  JPanel endPanel = new JPanel();
  endPanel.setLayout(new BoxLayout(endPanel, BoxLayout.X_AXIS));
  JLabel endLabel = new JLabel("End: ");
  endLabel.setFont(MainWindow.font);
  endPanel.add(endLabel);
  endPanel.add(Box.createGlue());
  endField = new JTextField();
  endField.setFont(MainWindow.font);
  endPanel.add(endField);
  offsetPanel.add(endPanel);

  panel.add(offsetPanel);

  JPanel corefPanel = new JPanel();
  corefPanel.setLayout(new BoxLayout(corefPanel, BoxLayout.Y_AXIS));
  corefPanel.add(new JLabel(""));

  JPanel idPanel = new JPanel();
  idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
  JLabel idLabel = new JLabel("ID: ");
  idLabel.setFont(MainWindow.font);
  idPanel.add(idLabel);
  idPanel.add(Box.createGlue());
  idField = new JTextField();
  idField.setFont(MainWindow.font);
  idPanel.add(idField);
  corefPanel.add(idPanel);

  JPanel refPanel = new JPanel();
  refPanel.setLayout(new BoxLayout(refPanel, BoxLayout.X_AXIS));
  JLabel refLabel = new JLabel("REF: ");
  refLabel.setFont(MainWindow.font);
  refPanel.add(refLabel);
  refPanel.add(Box.createGlue());
  refField = new JTextField();
  refField.setFont(MainWindow.font);
  refPanel.add(refField);
  corefPanel.add(refPanel);

  panel.add(corefPanel);

  return panel;
}

/**
 * @return
 */
private Component createButtonPanel()
{
  JPanel panel = new JPanel();
  panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

  status = new JTextField();
  status.setFont(MainWindow.font);
  panel.add(status);


  return panel;
}




}
