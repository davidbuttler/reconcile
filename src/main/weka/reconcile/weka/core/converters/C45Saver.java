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
 *    C45Saver.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package reconcile.weka.core.converters;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;


/**
 * Writes to a destination in the format used by the C4.5 slgorithm.
 * The output are two files: *.names, *.data
 *
 * Valid options:
 *
 * -i input arff file <br>
 * The input filw in ARFF format. <p>
 *
 * -o the output file <br>
 * The output file. The prefix of the output file is sufficient.<p>
 *
 * -c class index <br>
 * The index of the class attribute. first and last are valid as well (default: last). <p>
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 * @see Saver
 */
public class C45Saver extends AbstractFileSaver implements BatchConverter, IncrementalConverter, OptionHandler {

  /** Constructor */  
  public C45Saver(){
  
      resetOptions();
  }
   
  /**
   * Returns a string describing this Saver
   * @return a description of the Saver suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes to a destination that is in the format used by the C4.5 algorithm.\nTherefore it outputs a names and a data file.";
  }

  
  /**
   * Returns a description of the file type.
   *
   * @return a short file description
   */
  public String getFileDescription() {
    return "C4.5 file format";
  }

  /**
   * Resets the Saver 
   */
  public void resetOptions() {

    super.resetOptions();
    setFileExtension(".names");
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
      
      if(structure != null){
          if(structure.classIndex() == -1){
            structure.setClassIndex(structure.numAttributes()-1);
            System.err.println("No class specified. Last attribute is used as class attribute.");
          }
          if(structure.attribute(structure.classIndex()).isNumeric())
            throw new IOException("To save in C4.5 format the class attribute cannot be numeric.");
      }
      if(getRetrieval() == BATCH || getRetrieval() == NONE)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      if(retrieveFile() == null || getWriter() == null){
          throw new IOException("C4.5 format requires two files. Therefore no output to standard out can be generated.\nPlease specifiy output files using the -o option.");
      }
      
      
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
          //write header: here names file
          for (int i = 0; i < structure.attribute(structure.classIndex()).numValues(); i++) {
            outW.write(structure.attribute(structure.classIndex()).value(i));
            if (i < structure.attribute(structure.classIndex()).numValues()-1) {
                outW.write(",");
            } else {
                outW.write(".\n");
            }
          }
          for (int i = 0; i < structure.numAttributes(); i++) {
            if (i != structure.classIndex()) {
                outW.write(structure.attribute(i).name()+": ");
                if (structure.attribute(i).isNumeric() || structure.attribute(i).isDate()) {
                    outW.write("continuous.\n");
                } else {
                    Attribute temp = structure.attribute(i);
                    for (int j = 0; j < temp.numValues(); j++) {
                        outW.write(temp.value(j));
                        if (j < temp.numValues()-1) {
                            outW.write(",");
                        } else {
                            outW.write(".\n");
                        }
                    }
                }
            }
          }
          outW.flush();
          outW.close();
          
          writeMode = getWriteMode();
          
          String out = retrieveFile().getAbsolutePath();
          setFileExtension(".data");
          out = out.substring(0, out.lastIndexOf('.')) + getFileExtension();
          File namesFile = new File(out);
          try{
            setFile(namesFile);
            setDestination(namesFile);
          } catch(Exception ex){
            throw new IOException("Cannot create data file, only names file created.");
          }
          if(retrieveFile() == null || getWriter() == null){
            throw new IOException("Cannot create data file, only names file created.");
          }
          outW = new PrintWriter(getWriter());
      }
      if(writeMode == WRITE){
          if(structure == null)
              throw new IOException("No instances information available.");
          if(inst != null){
            //write instance: here data file
            for(int j = 0; j < inst.numAttributes(); j++){
                if(j != structure.classIndex()){
                    if (inst.isMissing(j)) {
                        outW.write("?,");
                    } else 
                        if (structure.attribute(j).isNominal() || 
                            structure.attribute(j).isString()) {
                                outW.write(structure.attribute(j).value((int)inst.value(j))+",");
                        } else {
                                outW.write(""+inst.value(j)+",");
                        }
                    }
            }
            // write the class value
            if (inst.isMissing(structure.classIndex())) {
                outW.write("?");
            } 
            else {
                outW.write(structure.attribute(structure.classIndex()).value((int)inst.value(structure.classIndex())));
            }
            outW.write("\n");
            //flushes every 100 instances
            m_incrementalCounter++;
            if(m_incrementalCounter > 100){
                m_incrementalCounter = 0;
                outW.flush();
            }
          }
          else{
          //close
              if(outW != null){
                outW.flush();
                outW.close();
              }
              setFileExtension(".names");
              m_incrementalCounter = 0;
              resetStructure();
          }
      }
  }

  
  /** Writes a Batch of instances
   * @throws IOException throws IOException if saving in batch mode is not possible
   */
  public void writeBatch() throws IOException {
      
      Instances instances = getInstances();
      
      if(instances == null)
          throw new IOException("No instances to save");
      if(instances.classIndex() == -1){
          instances.setClassIndex(instances.numAttributes()-1);
          System.err.println("No class specified. Last attribute is used as class attribute.");
      }
      if(instances.attribute(instances.classIndex()).isNumeric())
          throw new IOException("To save in C4.5 format the class attribute cannot be numeric.");
      if(getRetrieval() == INCREMENTAL)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      
      setRetrieval(BATCH);
      if(retrieveFile() == null || getWriter() == null){
          throw new IOException("C4.5 format requires two files. Therefore no output to standard out can be generated.\nPlease specifiy output files using the -o option.");
      }
      setWriteMode(WRITE);
      //print names file
      setFileExtension(".names");
      PrintWriter outW = new PrintWriter(getWriter());
      for (int i = 0; i < instances.attribute(instances.classIndex()).numValues(); i++) {
        outW.write(instances.attribute(instances.classIndex()).value(i));
        if (i < instances.attribute(instances.classIndex()).numValues()-1) {
            outW.write(",");
        } else {
            outW.write(".\n");
	}
      }
      for (int i = 0; i < instances.numAttributes(); i++) {
        if (i != instances.classIndex()) {
            outW.write(instances.attribute(i).name()+": ");
            if (instances.attribute(i).isNumeric() || instances.attribute(i).isDate()) {
                outW.write("continuous.\n");
            } else {
                Attribute temp = instances.attribute(i);
		for (int j = 0; j < temp.numValues(); j++) {
                    outW.write(temp.value(j));
		    if (j < temp.numValues()-1) {
			outW.write(",");
		    } else {
			outW.write(".\n");
		    }
		 }
             }
        }
      }
      outW.flush();
      outW.close();
      
      //print data file
      String out = retrieveFile().getAbsolutePath();
      setFileExtension(".data");
      out = out.substring(0, out.lastIndexOf('.')) + getFileExtension();
      File namesFile = new File(out);
      try{
        setFile(namesFile);
        setDestination(retrieveFile());
      } catch(Exception ex){
          throw new IOException("Cannot create data file, only names file created.");
      }
      if(retrieveFile() == null || getWriter() == null){
          throw new IOException("Cannot create data file, only names file created.");
      }
      outW = new PrintWriter(getWriter());
      // print data file
      for (int i = 0; i < instances.numInstances(); i++) {
	Instance temp = instances.instance(i);
        for(int j = 0; j < temp.numAttributes(); j++){
            if(j != instances.classIndex()){
                if (temp.isMissing(j)) {
		      outW.write("?,");
		    } else if (instances.attribute(j).isNominal() || 
			       instances.attribute(j).isString()) {
		      outW.write(instances.attribute(j).value((int)temp.value(j))+",");
		    } else {
		      outW.write(""+temp.value(j)+",");
		    }
            }
        }
        // write the class value
        if (temp.isMissing(instances.classIndex())) {
            outW.write("?");
        } 
        else {
            outW.write(instances.attribute(instances.classIndex()).value((int)temp.value(instances.classIndex())));
        }
        outW.write("\n");
      }
      outW.flush();
      outW.close();
      setFileExtension(".names");
      setWriteMode(WAIT);
  }
  
  
   /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    FastVector newVector = new FastVector(3);

    newVector.addElement(new Option("The input file", "i", 1, 
				    "-i <the input file>"));
    
    newVector.addElement(new Option("The output file", "o", 1, 
				    "-o <the output file>"));
    
    newVector.addElement(new Option("The class index", "c", 1, 
				    "-c <the class index>"));
    
    return newVector.elements();
  }

 
/**
   * Parses a given list of options. Valid option is:<p>
   *   
   * -i input arff file <br>
   * The input filw in ARFF format. <p>
   *  
   * -o the output file <br>
   * The output file. The prefix of the output file is sufficient.<p>
   *
   * -c class index <br>
   * The index of the class attribute. first and last are valid as well (default: last). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    String outputString = Utils.getOption('o', options);
    String inputString = Utils.getOption('i', options);
    String indexString = Utils.getOption('c', options);
    
    ArffLoader loader = new ArffLoader();
    
    resetOptions();
    
    if(inputString.length() != 0){
        try{
            File input = new File(inputString);
            loader.setFile(input);
            setInstances(loader.getDataSet());
        } catch(Exception ex){
            throw new IOException("No data set loaded. Data set has to be arff format.");
        }
    }
    else
        throw new IOException("No data set to save.");
    if (outputString.length() != 0){ 
        //add appropriate file extension
        if(!outputString.endsWith(getFileExtension())){
            if(outputString.lastIndexOf('.') != -1)
                outputString = (outputString.substring(0,outputString.lastIndexOf('.'))) + getFileExtension();
            else
                outputString = outputString + getFileExtension();
        }
        try{
            File output = new File(outputString);
            setFile(output);
        } catch(Exception ex){
            throw new IOException("Cannot create output file.");
        } finally{
            setDestination(retrieveFile());
        }
    }
    if(indexString.length() != 0){
        if(indexString.equals("first"))
            getInstances().setClassIndex(0);
        else{
            if(indexString.equals("last"))
                getInstances().setClassIndex(getInstances().numAttributes()-1);
            else{
                int classIndex = Integer.parseInt(indexString);
                if(classIndex >=0 && classIndex < getInstances().numAttributes())
                    getInstances().setClassIndex(classIndex);
                else
                    throw new IOException("Invalid class index");
            }
        }
    }
    else
        getInstances().setClassIndex(getInstances().numAttributes()-1);

  }

  /**
   * Gets the current settings of the C45Saver object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;
    if(retrieveFile() != null){
        options[current++] = "-o"; options[current++] = "" + retrieveFile();
    }
    else{
        options[current++] = "-o"; options[current++] = "";
    }
    if(getInstances() != null){
        options[current++] = "-i"; options[current++] = "" + getInstances().relationName();
        options[current++] = "-c"; options[current++] = "" + getInstances().classIndex();
    }
    else{
        options[current++] = "-i"; options[current++] = "";
        options[current++] = "-c"; options[current++] = "";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }



  /**
   * Main method.
   *
   * @param options should contain the options of a Saver.
   */
  public static void main(String [] options) {
      
      StringBuffer text = new StringBuffer();
      try {
	C45Saver csv = new C45Saver();
        text.append("\n\nC45Saver options:\n\n");
        Enumeration enumi = csv.listOptions();
        while (enumi.hasMoreElements()) {
            Option option = (Option)enumi.nextElement();
            text.append(option.synopsis()+'\n');
            text.append(option.description()+'\n');
        }
        try {
          csv.setOptions(options);  
        } catch (Exception ex) {
            System.out.println("\n"+text);
            System.exit(1);
	}
        //incremental
        /*
        csv.setRetrieval(INCREMENTAL);
        Instances instances = csv.getInstances();
        csv.setStructure(instances);
        for(int i = 0; i < instances.numInstances(); i++){ //last instance is null and finishes incremental saving
            csv.writeIncremental(instances.instance(i));
        }
        csv.writeIncremental(null);
        */
        
        //batch
        csv.writeBatch();
      } catch (Exception ex) {
	ex.printStackTrace();
	}
      
    }
}
  
  
