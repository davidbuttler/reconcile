/*
 * OpinionCellRenderer.java
 * 
 * Created on August 7, 2007 by ves
 */

package reconcile.visualTools;

import java.awt.Component;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import reconcile.data.Annotation;


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

public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
    boolean cellHasFocus)
{
  if (value instanceof Annotation) {
    // System.err.println("Rendering..."+value);
    Annotation a = (Annotation) value;
    StringBuilder s = new StringBuilder(a.getType());
    s.append(" - ");
    Map<String, String> features = a.getFeatures();
    for (String k : features.keySet()) {
      s.append(k).append(": ").append(features.get(k)).append(" ");
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
  if (value instanceof String) {
    setText((String) value);
    setEnabled(list.isEnabled());
    setFont(list.getFont());
    setOpaque(true);
    return this;
  }
  throw new RuntimeException("Invalid value in opinion list");
}
}
