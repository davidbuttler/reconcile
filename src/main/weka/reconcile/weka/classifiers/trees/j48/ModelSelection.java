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
 *    ModelSelection.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package reconcile.weka.classifiers.trees.j48;

import java.io.Serializable;

import reconcile.weka.core.Instances;


/**
 * Abstract class for model selection criteria.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public abstract class ModelSelection implements Serializable {

  /**
   * Selects a model for the given dataset.
   *
   * @exception Exception if model can't be selected
   */
  public abstract ClassifierSplitModel selectModel(Instances data) throws Exception;

  /**
   * Selects a model for the given train data using the given test data
   *
   * @exception Exception if model can't be selected
   */
  public ClassifierSplitModel selectModel(Instances train, Instances test) 
       throws Exception {

    throw new Exception("Model selection method not implemented");
  }
}
