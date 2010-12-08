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
import java.util.HashSet;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;

import com.google.common.collect.Maps;

public class AnnotationEditPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;

private MainWindow mainWindow;

private JTextField status;

private JTextField startField;

private JTextField endField;

private JTextField idField;

private JTextField refField;

private int modify = -1;
// the index of the annotation that has been selected for modification;
// -1 indicates no annotation has been selected

private JButton modify_button;

private Color modify_background_color; // also used for the SetMin button

private JButton min_button;

private JButton string_match_button;

private int set_min = -1;
// the index of the annotation that has been selected for modification;
// -1 indicates no annotation has been selected

private AnnotationListPanel apanel;

public AnnotationEditPanel(MainWindow mainw) {
  super();
  mainWindow = mainw;
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

public void setAnnotationListPanel(AnnotationListPanel a)
{
  apanel = a;
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
  JButton delete = new JButton("Delete");
  delete.setFont(MainWindow.font);
  delete.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      delete();

    }
  });
  panel.add(delete);

  status = new JTextField();
  status.setFont(MainWindow.font);
  panel.add(status);

  JButton save = new JButton("Save");
  save.setFont(MainWindow.font);
  save.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      save();
    }
  });
  panel.add(save);

  modify_button = new JButton("Modify");
  modify_button.setFont(MainWindow.font);
  modify_background_color = modify_button.getBackground();
  modify_button.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      modify();

    }
  });
  panel.add(modify_button);

  min_button = new JButton("Set MIN");
  min_button.setFont(MainWindow.font);
  min_button.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      setMin();
    }
  });
  panel.add(min_button);

  string_match_button = new JButton("string match");
  string_match_button.setFont(MainWindow.font);
  string_match_button.addActionListener(new ActionListener() {

    public void actionPerformed(ActionEvent e)
    {
      stringMatch();
    }
  });
  panel.add(string_match_button);
  return panel;
}

private void stringMatch()
{
  status.setText("");
  int start = Integer.parseInt(startField.getText().trim());
  int end = Integer.parseInt(endField.getText().trim());
  int id = Integer.parseInt(idField.getText().trim());

  Document doc = mainWindow.getDoc();
  String txt = doc.getText();
  txt = txt.replace("\n", " ").toLowerCase();
  String what_to_match = txt.substring(start, end);
  int len = what_to_match.length();

  AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
  HashSet<String> already_there = new HashSet<String>();
  for (Annotation a : set) {
    already_there.add(a.getStartOffset() + " " + a.getEndOffset());
    Map<String, String> f = a.getFeatures();
    if (f.get("HEAD_START") != null) {
      String b = f.get("HEAD_START");
      String e = f.get("HEAD_END");
      already_there.add(b + " " + e);
    }
  }

  int idx = 0;
  HashSet<String> hash = new HashSet<String>();
  while (true) {
    idx = txt.indexOf(what_to_match, idx);
    if (idx == -1) {
      break;
    }
    hash.add(idx + " " + (idx + len));
    ++idx;
  }

  int count = 0;
  for (String s : hash) {
    if (!already_there.contains(s)) {
      String[] t = s.split(" ");
      startField.setText(t[0]);
      endField.setText(t[1]);
      idField.setText(Integer.toString(apanel.getMaxId() + 1));
      refField.setText(Integer.toString(id));
      save();
      ++count;
    }
  }
  status.setText("added: " + count + " coreferent mentions");

}

protected void delete()

{
  status.setText("");
  mainWindow.annotationPanel.deletedSelected();

}

/**
 * 
 */
protected void save()
{
  status.setText("");
  Annotation a = null;
  try {

    int start = Integer.parseInt(startField.getText().trim());
    int end = Integer.parseInt(endField.getText().trim());

    Map<String, String> features = Maps.newTreeMap();
    if (set_min != -1) {
      a = mainWindow.annotationPanel.getAndDeleteAnnotation(set_min);
      features = a.getFeatures();
      features.put(Constants.HEAD_START, Integer.toString(start));
      features.put(Constants.HEAD_END, Integer.toString(end));
      Document doc = mainWindow.getDoc();
      features.put("MIN", doc.getAnnotText(start, end));
      int id = Integer.parseInt(idField.getText().trim());
      String ref = refField.getText().trim();
      features.put(Constants.CE_ID, String.valueOf(id));
      if (!"".equals(ref)) {
        int refId = Integer.parseInt(ref); // we want to ensure that the ref is an int, just like the id
        features.put(Constants.CLUSTER_ID, String.valueOf(refId));
      }
    }
    else if (modify != -1) {
      a = mainWindow.annotationPanel.getAndDeleteAnnotation(modify);
      a.setStartOffset(start);
      a.setEndOffset(end);
      features = a.getFeatures();
      int id = Integer.parseInt(idField.getText().trim());
      String ref = refField.getText().trim();
      features.put(Constants.CE_ID, String.valueOf(id));
      if (!"".equals(ref)) {
        int refId = Integer.parseInt(ref); // we want to ensure that the ref is an int, just like the id
        features.put(Constants.CLUSTER_ID, String.valueOf(refId));
      }
    }
    else {
      int id = Integer.parseInt(idField.getText().trim());
      String ref = refField.getText().trim();
      features.put(Constants.CE_ID, String.valueOf(id));
      if (!"".equals(ref)) {
        int refId = Integer.parseInt(ref); // we want to ensure that the ref is an int, just like the id
        features.put(Constants.CLUSTER_ID, String.valueOf(refId));
      }
      a = new Annotation(0, start, end, Constants.COREF, features);
    }

    AnnotationSet set = mainWindow.annotationSetPanel.getSelectedAnnotation();
    if (set == null) {
      status.setText("Must select a single annotation set to add annotation to");
    }
    else {
      boolean added = set.add(a);
      if (added) {
        Document doc = mainWindow.getDoc();
        doc.writeAnnotationSet(set);
      }
      else {
        status.setText("annotation not added");
      }
    }

  }
  catch (NumberFormatException e) {
    status.setText("NFE: " + e.getMessage());
  }

  AnnotationSet selected = mainWindow.annotationSetPanel.getSelectedAnnotation();
  mainWindow.annotationSetPanel.redraw();
  mainWindow.annotationSetPanel.select(selected);
  mainWindow.editPanel.clearAnnotationToModify();
  mainWindow.editPanel.clearAnnotationToSetMin();
  mainWindow.annotationPanel.redraw();
  if (a != null) {
    // mainWindow.aPanel.highlightSpans(a);
    apanel.highlightSpans(a);
  }
}

protected void modify()
{
  if (modify != -1) {
    clearAnnotationToModify();
    return;
  }
  if (set_min != -1) return;

  modify = getModify();
  if (modify != -1) {
    mainWindow.editPanel.modify_button.setBackground(Color.green);
  }
}

protected void setMin()
{
  if (set_min != -1) {
    clearAnnotationToSetMin();
    return;
  }
  if (modify != -1) return;

  set_min = getModify();
  if (set_min != -1) {
    mainWindow.editPanel.min_button.setBackground(Color.green);
  }
}

private int getModify()
{
  // Find out which index is selected.
  JList anList = mainWindow.annotationPanel.getAnnotationList();
  int minIndex = anList.getMinSelectionIndex();
  int maxIndex = anList.getMaxSelectionIndex();
  int modify = -1; // there's also a global modify!
  for (int i = minIndex; i <= maxIndex; i++) {
    if (anList.isSelectedIndex(i)) {
      if (modify != -1) {
        status.setText("you have multiple annotations selected; can only modify one");
        modify = -2;
        break;
      }
      else {
        modify = i;
      }
    }
  }
  if (modify == -1) {
    status.setText("you must select an annotation before clicking modify or Set Min");
  }
  if (modify == -2) {
    modify = -1;
  }

  return modify;
}

/**
 * returns the id of the selected annotation, if a single annotation was selected, and the modify button was clicked;
 * returns -1 otherwise.
 */
public int getAnnotationToModify()
{
  return modify;
}

public int getAnnotationToSetMin()
{
  return set_min;
}

/**
 *
 */
public void clearAnnotationToModify()
{
  modify = -1;
  mainWindow.editPanel.modify_button.setBackground(modify_background_color);

}

public void clearAnnotationToSetMin()
{
  set_min = -1;
  mainWindow.editPanel.min_button.setBackground(modify_background_color);

}

/**
 * updates values in the text fields in the edit panel: start, end, id, ref
 */
public void setAnnotation(Annotation a, boolean update_id_and_ref)
{
  if (a.getStartOffset() == a.getEndOffset()) return;
  startField.setText(String.valueOf(a.getStartOffset()));
  endField.setText(String.valueOf(a.getEndOffset()));

  if (update_id_and_ref) {
    String id = a.getAttribute(Constants.CE_ID);
    if (id == null && modify == -1 && set_min == -1) {
      id = "";
    }
    idField.setText(id);

    String ref = a.getAttribute(Constants.CLUSTER_ID);
    if (ref == null && modify == -1 && set_min == -1) {
      ref = "";
    }
    refField.setText(ref);
  }
}
}
