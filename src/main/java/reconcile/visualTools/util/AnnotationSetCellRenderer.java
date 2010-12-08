/*
 * OpinionCellRenderer.java
 * 
 * Created on August 7, 2007 by ves
 */

package reconcile.visualTools.util;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import reconcile.data.AnnotationSet;

/**
 * 
 * @author ves
 */
public class AnnotationSetCellRenderer
    extends JLabel
    implements ListCellRenderer {

/**
   * 
   */
private static final long serialVersionUID = 1L;

public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
    boolean cellHasFocus)
{
  if (!(value instanceof AnnotationSet)) throw new RuntimeException("Invalid value in AnnotationSet list");
  // System.err.println("Rendering..."+value);
  AnnotationSet a = (AnnotationSet) value;
  String s = a.getName() + " (" + a.size() + ")";
  setText(s);
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
