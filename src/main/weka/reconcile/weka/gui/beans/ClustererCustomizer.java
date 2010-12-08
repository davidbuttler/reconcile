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
 *    ClustererCustomizer.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package reconcile.weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.Customizer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JPanel;

import reconcile.weka.gui.PropertySheetPanel;



/**
 * GUI customizer for the Clusterer wrapper bean
 *
 * @author <a href="mailto:mutter@cs.waikato.ac.nz">Stefan Mutter</a>
 * @version $Revision: 1.1 $
 */
public class ClustererCustomizer extends JPanel implements Customizer {

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
      .registerEditor(Object [].class,
		      reconcile.weka.gui.GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.classifiers.CostMatrix.class,
		      reconcile.weka.gui.CostMatrixEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.clusterers.Clusterer.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);
  
  private reconcile.weka.gui.beans.Clusterer m_dsClusterer;
  
  private PropertySheetPanel m_ClustererEditor = 
    new PropertySheetPanel();

  
  public ClustererCustomizer() {
    
    setLayout(new BorderLayout());
    add(m_ClustererEditor, BorderLayout.CENTER);
  }
  
  

  /**
   * Set the Clusterer object to be edited
   *
   * @param object an <code>Object</code> value
   */
  public void setObject(Object object) {
    m_dsClusterer = (reconcile.weka.gui.beans.Clusterer)object;
    m_ClustererEditor.setTarget(m_dsClusterer.getClusterer());
    
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
