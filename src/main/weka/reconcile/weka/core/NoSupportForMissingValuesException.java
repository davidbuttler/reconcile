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
 *    NoSupportForMissingValuesException.java
 *    Copyright (C) 2002 Eibe Frank
 *
 */

package reconcile.weka.core;

/**
 * Exception that is raised by an object that is unable to process 
 * data with missing values.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class NoSupportForMissingValuesException extends WekaException {

  /**
   * Creates a new NoSupportForMissingValuesException with no message.
   *
   */
  public NoSupportForMissingValuesException() {

    super();
  }

  /**
   * Creates a new NoSupportForMissingValuesException.
   *
   * @param message the reason for raising an exception.
   */
  public NoSupportForMissingValuesException(String message) {

    super(message);
  }
}
