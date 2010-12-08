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
 *    MultipleClassifiersCombiner.java
 *    Copyright (C) 2004 Eibe Frank
 *
 */

package reconcile.weka.classifiers;

import java.util.Enumeration;
import java.util.Vector;

import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;


/**
 * Abstract utility class for handling settings common to
 * meta classifiers that build an ensemble from multiple classifiers.  
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public abstract class MultipleClassifiersCombiner extends Classifier {
  
  /** Array for storing the generated base classifiers. */
  protected Classifier[] m_Classifiers = {
    new reconcile.weka.classifiers.rules.ZeroR()
  };

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
	      "\tFull class name of classifier to include, followed\n"
	      + "\tby scheme options. May be specified multiple times.\n"
	      + "\t(default: \"weka.classifiers.rules.ZeroR\")",
	      "B", 1, "-B <classifier specification>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a scheme
   * included for selection followed by options to the classifier
   * (required, option should be used once for each classifier).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    // Iterate through the schemes
    Vector classifiers = new Vector();
    while (true) {
      String classifierString = Utils.getOption('B', options);
      if (classifierString.length() == 0) {
	break;
      }
      String [] classifierSpec = Utils.splitOptions(classifierString);
      if (classifierSpec.length == 0) {
	throw new IllegalArgumentException("Invalid classifier specification string");
      }
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      classifiers.addElement(Classifier.forName(classifierName,
						classifierSpec));
    }
    if (classifiers.size() == 0) {
      classifiers.addElement(new reconcile.weka.classifiers.rules.ZeroR());
    }
    Classifier [] classifiersArray = new Classifier [classifiers.size()];
    for (int i = 0; i < classifiersArray.length; i++) {
      classifiersArray[i] = (Classifier) classifiers.elementAt(i);
    }
    setClassifiers(classifiersArray);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    int current = 0;
    String[] options = new String [superOptions.length + m_Classifiers.length * 2];
    for (int i = 0; i < m_Classifiers.length; i++) {
      options[current++] = "-B";
      options[current++] = "" + getClassifierSpec(i);
    }
    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);
    return options;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifiersTipText() {
    return "The base classifiers to be used.";
  }

  /**
   * Sets the list of possible classifers to choose from.
   *
   * @param classifiers an array of classifiers with all options set.
   */
  public void setClassifiers(Classifier [] classifiers) {

    m_Classifiers = classifiers;
  }

  /**
   * Gets the list of possible classifers to choose from.
   *
   * @return the array of Classifiers
   */
  public Classifier [] getClassifiers() {

    return m_Classifiers;
  }
  
  /**
   * Gets a single classifier from the set of available classifiers.
   *
   * @param index the index of the classifier wanted
   * @return the Classifier
   */
  public Classifier getClassifier(int index) {

    return m_Classifiers[index];
  }
  
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param index the index of the classifier string to retrieve, starting from
   * 0.
   * @return the classifier string, or the empty string if no classifier
   * has been assigned (or the index given is out of range).
   */
  protected String getClassifierSpec(int index) {
    
    if (m_Classifiers.length < index) {
      return "";
    }
    Classifier c = getClassifier(index);
    return c.getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler)c).getOptions());
  }
}
