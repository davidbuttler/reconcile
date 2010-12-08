/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    FilterCustomizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package reconcile.weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;

import reconcile.weka.filters.Filter;
import reconcile.weka.gui.GenericObjectEditor;


/**
 * GUI customizer for the filter bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 */
public class FilterCustomizer extends JPanel
  implements Customizer {
  
  static {
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.core.SelectedTag.class,
		      reconcile.weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.filters.Filter.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.attributeSelection.ASSearch.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.attributeSelection.ASEvaluation.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.classifiers.Classifier [].class,
		      reconcile.weka.gui.GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(Object [].class,
		      reconcile.weka.gui.GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.classifiers.Classifier.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.classifiers.CostMatrix.class,
		      reconcile.weka.gui.CostMatrixEditor.class);
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private reconcile.weka.gui.beans.Filter m_filter;
  private GenericObjectEditor m_filterEditor = 
    new GenericObjectEditor(true);
 
  public FilterCustomizer() {
    try {
      m_filterEditor.
	setClassType(Filter.class);      
      m_filterEditor.setValue(new reconcile.weka.filters.unsupervised.attribute.Add());
      m_filterEditor.addPropertyChangeListener(new PropertyChangeListener() {
	  public void propertyChange(PropertyChangeEvent e) {
	    repaint();
	    if (m_filter != null) {
	      Filter editedF = (Filter)m_filterEditor.getValue();
	      m_filter.setFilter(editedF);
	      // should pass on the property change to any other interested
	      // listeners
	    }
	  }
	});
      //      System.out.println("Here");
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    setLayout(new BorderLayout());
    add(m_filterEditor.getCustomEditor(), BorderLayout.CENTER);
  }
  
  /**
   * Set the filter bean to be edited
   *
   * @param object a Filter bean
   */
  public void setObject(Object object) {
    m_filter = (reconcile.weka.gui.beans.Filter)object;
    m_filterEditor.setValue(m_filter.getFilter());
  }

  /**
   * Add a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }
}

