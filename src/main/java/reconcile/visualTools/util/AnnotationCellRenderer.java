/*
 * OpinionCellRenderer.java
 * 
 * Created on August 7, 2007 by ves
 */

package reconcile.visualTools.util;

import java.awt.Component;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import reconcile.data.Annotation;
import reconcile.data.Document;

/**
 * 
 * @author ves
 */
public class AnnotationCellRenderer
    extends JLabel
    implements ListCellRenderer {

/**
   * 
   */
private static final long serialVersionUID = 1L;
private Pattern pSpace = Pattern.compile("\\s+");
private DocHolder myDocHolder;

// private Set<String> allowableFeatures = Sets.newHashSet(Constants.CE_ID, Constants.CLUSTER_ID);

public AnnotationCellRenderer(DocHolder dh) {
  myDocHolder = dh;
}

public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
    boolean cellHasFocus)
{
  if (!(value instanceof Annotation)) throw new RuntimeException("Invalid value in opinion list");
  Document doc = myDocHolder.getDoc();
  // System.err.println("Rendering..."+value);
  Annotation a = (Annotation) value;
  StringBuilder s = new StringBuilder();
  if (a.getEndOffset() > doc.length()) {
    s.append("error");
  }
  else {
    s.append(a.getType());
    s.append(" - ");
    Map<String, String> features = a.getFeatures();
    for (String k : features.keySet()) {
      s.append(k).append(": ").append(features.get(k)).append(" ");
    }
    if (a.getStartOffset() >= 0 && a.getEndOffset() >= 0) {
      s.append("[").append(pSpace.matcher(doc.getAnnotText(a)).replaceAll(" ")).append("]");
    }
  }
  setText(s.toString());
  if (isSelected) {
    setBackground(list.getSelectionBackground());
    setForeground(list.getSelectionForeground());
  }
  else {
    setBackground(list.getBackground());
    setForeground(list.getForeground());
  }
  setEnabled(list.isEnabled());
  setFont(list.getFont());
  setOpaque(true);
  return this;
}
}
