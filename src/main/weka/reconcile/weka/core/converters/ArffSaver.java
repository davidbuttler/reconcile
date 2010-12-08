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
 *    ArffSaver.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package reconcile.weka.core.converters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;


/**
 * Writes to a destination in arff text format.
 *
 * Valid options:
 *
 * -i input arff file <br>
 * The input filw in ARFF format. <p>
 *
 * -o the output file <br>
 * The output file. The prefix of the output file is sufficient. If no output file is given, Saver tries to use standard out. <p>
 *
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 * @see Saver
 */
public class ArffSaver extends AbstractFileSaver implements BatchConverter, IncrementalConverter {

    
  /** Constructor */  
  public ArffSaver(){
  
      resetOptions();
  }
   
   
  /**
   * Returns a string describing this Saver
   * @return a description of the Saver suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes to a destination that is in arff (attribute relation file format) "
      +"format. ";
  }

  
  /**
   * Returns a description of the file type.
   *
   * @return a short file description
   */
  public String getFileDescription() {
    return "Arff data files";
  }

  /**
   * Resets the Saver 
   */
  public void resetOptions() {

    super.resetOptions();
    setFileExtension(".arff");
  }


  
  /** Saves an instances incrementally. Structure has to be set by using the
   * setStructure() method or setInstances() method.
   * @param inst the instance to save
   * @throws IOException throws IOEXception if an instance cannot be saved incrementally.
   */  
  public void writeIncremental(Instance inst) throws IOException{
  
      int writeMode = getWriteMode();
      Instances structure = getInstances();
      PrintWriter outW = null;
      
      if(getRetrieval() == BATCH || getRetrieval() == NONE)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      if(getWriter() != null)
          outW = new PrintWriter(getWriter());
          
      if(writeMode == WAIT){
        if(structure == null){
            setWriteMode(CANCEL);
            if(inst != null)
                System.err.println("Structure(Header Information) has to be set in advance");
        }
        else
            setWriteMode(STRUCTURE_READY);
        writeMode = getWriteMode();
      }
      if(writeMode == CANCEL){
          if(outW != null)
              outW.close();
          cancel();
      }
      if(writeMode == STRUCTURE_READY){
          setWriteMode(WRITE);
          //write header
          Instances header = new Instances(structure,0);
          if(retrieveFile() == null || outW == null)
              System.out.println(header.toString());
          else{
              outW.print(header.toString());
              outW.print("\n");
              outW.flush();
          }
          writeMode = getWriteMode();
      }
      if(writeMode == WRITE){
          if(structure == null)
              throw new IOException("No instances information available.");
          if(inst != null){
          //write instance 
              if(retrieveFile() == null || outW == null)
                System.out.println(inst);
              else{
                outW.println(inst);
                m_incrementalCounter++;
                //flush every 100 instances
                if(m_incrementalCounter > 100){
                    m_incrementalCounter = 0;
                    outW.flush();
                }
              }
          }
          else{
          //close
              if(outW != null){
                outW.flush();
                outW.close();
              }
              m_incrementalCounter = 0;
              resetStructure();
          }
      }
  }
  
  /** Writes a Batch of instances
   * @throws IOException throws IOException if saving in batch mode is not possible
   */
  public void writeBatch() throws IOException {
  
      if(getInstances() == null)
          throw new IOException("No instances to save");
      if(getRetrieval() == INCREMENTAL)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      setRetrieval(BATCH);
      setWriteMode(WRITE);
      if(retrieveFile() == null || getWriter() == null){
          System.out.println((getInstances()).toString());
          setWriteMode(WAIT);
          return;
      }
      PrintWriter outW = new PrintWriter(getWriter());
      outW.print((getInstances()).toString());
      outW.flush();
      outW.close();
      setWriteMode(WAIT);
  }

  /**
   * Main method.
   *
   * @param options should contain the options of a Saver.
   */
  public static void main(String [] options) {
      
      StringBuffer text = new StringBuffer();
      try{
        ArffSaver asv = new ArffSaver();
        text.append("\n\nArffSaver options:\n\n");
        Enumeration enumi = asv.listOptions();
        while (enumi.hasMoreElements()) {
            Option option = (Option)enumi.nextElement();
            text.append(option.synopsis()+'\n');
            text.append(option.description()+'\n');
        }
        try {
          asv.setOptions(options);  
        } catch (Exception ex) {
            System.out.println("\n"+text);
            System.exit(1);
	}
        //incremental
        /*asv.setRetrieval(INCREMENTAL);
        Instances instances = asv.getInstances();
        asv.setStructure(instances);
        for(int i = 0; i < instances.numInstances(); i++){ //last instance is null and finishes incremental saving
            asv.writeIncremental(instances.instance(i));
        }
        asv.writeIncremental(null);*/
        
        //batch
        asv.writeBatch();
      } catch (Exception ex) {
	ex.printStackTrace();
	}
      
    }
}
  
  
