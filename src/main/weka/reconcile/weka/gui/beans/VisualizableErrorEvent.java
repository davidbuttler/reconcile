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
 *    VisualizableErrorEvent.java
 *    Copyright (C) 2005 Mark Hall
 *
 */

package reconcile.weka.gui.beans;

import java.util.EventObject;

import reconcile.weka.gui.visualize.PlotData2D;


/**
 * Event encapsulating error information for a learning scheme
 * that can be visualized in the DataVisualizer
 *
 * @author Mark Hall
 * @version $Revision: 1.1 $
 * @see EventObject
 */
public class VisualizableErrorEvent extends EventObject {

  private PlotData2D m_dataSet;

  public VisualizableErrorEvent(Object source, PlotData2D dataSet) {
    super(source);
    m_dataSet = dataSet;
  }
  
  /**
   * Return the instances of the data set
   *
   * @return an <code>Instances</code> value
   */
  public PlotData2D getDataSet() {
    return m_dataSet;
  }
}
