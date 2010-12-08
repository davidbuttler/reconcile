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
 *    DatabaseSaver.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package reconcile.weka.core.converters;


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;



/**
 * Writes to a database (tested with MySQL, InstantDB, HSQLDB).
 *
 * Available options are:
 * -T <table name> <br>
 * Sets the name of teh table (default: the name of the relation)<p>
 *
 * -P <br>
 * If set, a primary key column is generated automatically (containing the row number as INTEGER). The name of this columns is defined in the DatabaseUtils file.<p>
 *
 * -i <input-file> <br>
 * Specifies an ARFF file as input (for command line use) <p>
 *
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class DatabaseSaver extends AbstractSaver implements BatchConverter, IncrementalConverter, DatabaseConverter, OptionHandler {
    
  /** The database connection */
  private DatabaseConnection m_DataBaseConnection;
  
  /** The name of the tablein which the instances should be stored */
  private String m_tableName;
  
  /** An input arff file (for command line use) */
  private String m_inputFile;
  
  /** The database specific type for a string (read in from the properties file) */
  private String m_createText;
  
  /** The database specific type for a double (read in from the properties file) */
  private String m_createDouble;
  
  /** The database specific type for an int (read in from the properties file) */
  private String m_createInt;
  
  /** The name of the primary key column that will be automatically generated (if enabled). The name is read from DatabaseUtils.*/
  private String m_idColumn;
  
  /** counts the rowsand used as a primary key value */
  private int m_count;
  
  /** Flag indicating if a primary key column should be added */
  private boolean m_id;
  
  /** Flag indicating whether the default name of the table is the relaion name or not.*/
  private boolean m_tabName;
  
  /** The property file for the database connection */
  protected static String PROPERTY_FILE
 = "reconcile/weka/experiment/DatabaseUtils.props";
  
  /** Properties associated with the database connection */
  protected static Properties PROPERTIES;

  /** reads the property file */
  static {

    try {
      PROPERTIES = Utils.readProperties(PROPERTY_FILE);
   
    } catch (Exception ex) {
      System.err.println("Problem reading properties. Fix before continuing.");
      System.err.println(ex);
    }
  }
  
   /** Constructor
    * @throws Exception throws Exception if property file cannot be read
    */
  public DatabaseSaver() throws Exception{
  
      resetOptions();
      m_createText = PROPERTIES.getProperty("CREATE_STRING");
      m_createDouble = PROPERTIES.getProperty("CREATE_DOUBLE");
      m_createInt = PROPERTIES.getProperty("CREATE_INT");
      m_idColumn = PROPERTIES.getProperty("idColumn");
  }
  
  /** Resets the Saver ready to save a new data set
   */
  @Override
  public void resetOptions(){

    super.resetOptions();
    setRetrieval(NONE);
    m_tableName = "";
    m_count = 1;
    m_id = false;
    m_tabName = true;
    try{
        if(m_DataBaseConnection != null && m_DataBaseConnection.isConnected())
            m_DataBaseConnection.disconnectFromDatabase();
        m_DataBaseConnection = new DatabaseConnection();
    }catch(Exception ex) {
        printException(ex);
    }    
  }
  
  /** Cancels the incremental saving process and tries to drop the table if the write mode is CANCEL. */  
  @Override
  public void cancel(){
  
      if(getWriteMode() == CANCEL){
          try{
              m_DataBaseConnection.execute("DROP TABLE "+m_tableName);
              if(m_DataBaseConnection.tableExists(m_tableName))
                System.err.println("Table cannot be dropped.");
          }catch(Exception ex) {
              printException(ex);
          }
          resetOptions();
      }
  }
   
  /**
   * Returns a string describing this Saver
   * @return a description of the Saver suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes to a database";
  }

  
  /** Sets the table's name
   * @param tn the name of the table
   */  
  public void setTableName(String tn){
   
      m_tableName = tn;
  }
  
  /** Gets the table's name
   * @return the table's name
   */  
  public String getTableName(){
  
      return m_tableName;
  }
  
  /** Returns the tip text fo this property*/
  public String tableNameTipText(){
  
      return "Sets the name of the table.";
  }
  
  /** En/Dis-ables the automatic generation of a primary key
   * @param boolean flag 
   */  
  public void setAutoKeyGeneration(boolean flag){
  
      m_id = flag;
  }
  
   /** Gets whether or not a primary key will be generated automatically
   * @return true if a primary key column will be generated, false otherwise
   */  
  public boolean getAutoKeyGeneration(){
   
      return m_id;
  }
  
  /** Returns the tip text fo this property*/
  public String autoKeyGenerationTipText(){
  
      return "If set to true, a primary key column is generated automatically (containing the row number as INTEGER). The name of the key is read from DatabaseUtils (idColumn)"
        +" This primary key can be used for incremental loading (requires an unique key). This primary key will not be loaded as an attribute.";
  }
  
  /** En/Dis-ables that the relation name is used for the name of the table (default enabled).
   * @param boolean flag
   */  
  public void setRelationForTableName(boolean flag){
  
      m_tabName = flag;
  }
  
  /** Gets whether or not the relation name is used as name of the table
   * @return true if the relation name is used as the name of the table, false otherwise
   */  
  public boolean getRelationForTableName(){
   
      return m_tabName;
  }
  
  /** Returns the tip text fo this property*/
  public String relationForTableNameTipText(){
  
      return "If set to true, the relation name will be used as name for the database table. Otherwise the user has to provide a table name.";
  }
  
  /** Sets the database URL
   * @param the URL
   */  
  public void setUrl(String url){
      
      m_DataBaseConnection.setDatabaseURL(url);
    
  }
  
  /** Gets the database URL
   * @return the URL
   */  
  public String getUrl(){
  
      return m_DataBaseConnection.getDatabaseURL();
  }
  
  /** Returns the tip text fo this property*/
  public String urlTipText(){
  
      return "The URL of the database";
  }
  
  /** Sets the database user
   * @param the user name
   */  
  public void setUser(String user){
   
      m_DataBaseConnection.setUsername(user);
  }
  
  /** Gets the database user
   * @return the user name
   */  
  public String getUser(){
   
      return m_DataBaseConnection.getUsername();
  }
  
  /** Returns the tip text fo this property*/
  public String userTipText(){
  
      return "The user name for the database";
  }
  
  /** Sets the database password
   * @param the password
   */  
  public void setPassword(String password){
   
      m_DataBaseConnection.setPassword(password);
  }
  
  /** Returns the tip text fo this property*/
  public String passwordTipText(){
  
      return "The database password";
  }
      
   /** Sets the database url
   * @param url the database url
   * @param userName the user name
   * @param password the password
   */  
  public void setDestination(String url, String userName, String password){
  
      try{
        m_DataBaseConnection = new DatabaseConnection();
        m_DataBaseConnection.setDatabaseURL(url);
        m_DataBaseConnection.setUsername(userName);
        m_DataBaseConnection.setPassword(password);
      } catch(Exception ex) {
            printException(ex);
      }    
  }
  
  /** Sets the database url
   * @param url the database url
   */  
  public void setDestination(String url){
  
      try{
        m_DataBaseConnection = new DatabaseConnection();
        m_DataBaseConnection.setDatabaseURL(url);
      } catch(Exception ex) {
            printException(ex);
       }    
  }
  
  /** Sets the database url using the DatabaseUtils file */  
  public void setDestination(){
  
      try{
        m_DataBaseConnection = new DatabaseConnection();
      } catch(Exception ex) {
            printException(ex);
       }    
  }
  
   /**
   * Opens a connection to the database
   *
   */
  public void connectToDatabase() {
   
      try{
        if(!m_DataBaseConnection.isConnected())
            m_DataBaseConnection.connectToDatabase();
      } catch(Exception ex) {
	printException(ex);
       }    
  }
  
  /** Writes the structure (header information) to a database by creating a new table.*/
  private void writeStructure() throws Exception{
  
      StringBuffer query = new StringBuffer();
      Instances structure = getInstances();
      query.append("CREATE TABLE ");
      if(m_tabName || m_tableName.equals(""))
        m_tableName = structure.relationName();
      if(m_DataBaseConnection.getUpperCase()){
        m_tableName = m_tableName.toUpperCase();
        m_createInt = m_createInt.toUpperCase(); 
        m_createDouble = m_createDouble.toUpperCase(); 
        m_createText = m_createText.toUpperCase(); 
      }
      m_tableName = m_tableName.replaceAll("[^\\w]","_");
      query.append(m_tableName);
      if(structure.numAttributes() == 0)
          throw new Exception("Instances have no attribute.");
      query.append(" ( ");
      if(m_id){
        if(m_DataBaseConnection.getUpperCase())
              m_idColumn = m_idColumn.toUpperCase();
        query.append(m_idColumn);
        query.append(" ");
        query.append(m_createInt);
        query.append(" PRIMARY KEY,");
      }
      for(int i = 0;i < structure.numAttributes(); i++){
          Attribute att = structure.attribute(i);
          String attName = att.name();
          attName = attName.replaceAll("[^\\w]","_");
          if(m_DataBaseConnection.getUpperCase())
              query.append(attName.toUpperCase());
          else
              query.append(attName);
          if(att.isDate())
              query.append(" DATE");
          else{
              if(att.isNumeric())
                  query.append(" "+m_createDouble);
              else
                  query.append(" "+m_createText);
          }
          if(i != structure.numAttributes()-1)
              query.append(", ");
      }
      query.append(" )");
      //System.out.println(query.toString());
      m_DataBaseConnection.execute(query.toString());
      if(!m_DataBaseConnection.tableExists(m_tableName)){
          throw new IOException("Table cannot be built.");
      }
  }
  
  private void writeInstance(Instance inst) throws Exception{
  
      StringBuffer insert = new StringBuffer();
      insert.append("INSERT INTO ");
      insert.append(m_tableName);
      insert.append(" VALUES ( ");
      if(m_id){
        insert.append(m_count);
        insert.append(", ");
        m_count++;
      }
      for(int j = 0; j < inst.numAttributes(); j++){
        if(inst.isMissing(j))
            insert.append("NULL");
        else{
            if((inst.attribute(j)).isNumeric())
                insert.append(inst.value(j));
            else{
                String stringInsert = "'"+inst.stringValue(j)+"'";
                stringInsert = stringInsert.replaceAll("''","'");
                insert.append(stringInsert);
            }
        }
        if(j != inst.numAttributes()-1)
            insert.append(", ");
      }
      insert.append(" )");
      //System.out.println(insert.toString());
      if (m_DataBaseConnection.execute(insert.toString()) == false && m_DataBaseConnection.getUpdateCount() < 1) {
        throw new IOException("Tuple cannot be inserted.");
      }
  }
  
  /** Saves an instances incrementally. Structure has to be set by using the
   * setStructure() method or setInstances() method. When a structure is set, a table is created. 
   * @param inst the instance to save
   * @throws IOException throws IOEXception.
   */  
  @Override
  public void writeIncremental(Instance inst) throws IOException{
  
      int writeMode = getWriteMode();
      Instances structure = getInstances();
      
      if(m_DataBaseConnection == null)
           throw new IOException("No database has been set up.");
      if(getRetrieval() == BATCH)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      setRetrieval(INCREMENTAL);
      
      try{
        if(!m_DataBaseConnection.isConnected())
            connectToDatabase();
        if(writeMode == WAIT){
            if(structure == null){
                setWriteMode(CANCEL);
                if(inst != null)
                    throw new Exception("Structure(Header Information) has to be set in advance");
            }
            else
                setWriteMode(STRUCTURE_READY);
            writeMode = getWriteMode();
        }
        if(writeMode == CANCEL){
          cancel();
        }
        if(writeMode == STRUCTURE_READY){
          setWriteMode(WRITE);
          writeStructure();
          writeMode = getWriteMode();
        }
        if(writeMode == WRITE){
          if(structure == null)
              throw new IOException("No instances information available.");
          if(inst != null){
            //write instance 
            writeInstance(inst);  
          }
          else{
          //close
              m_DataBaseConnection.disconnectFromDatabase();
              resetStructure();
              m_count = 1;
          }
        }
      }catch(Exception ex) {
            printException(ex);
       }    
  }
  
  /** Writes a Batch of instances
   * @throws IOException throws IOException
   */
  @Override
  public void writeBatch() throws IOException {
  
      Instances instances = getInstances();
      if(instances == null)
          throw new IOException("No instances to save");
      if(getRetrieval() == INCREMENTAL)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      if(m_DataBaseConnection == null)
           throw new IOException("No database has been set up.");
      setRetrieval(BATCH);
      try{
          if(!m_DataBaseConnection.isConnected())
              connectToDatabase();
          setWriteMode(WRITE);
          writeStructure();
          for(int i = 0; i < instances.numInstances(); i++){
            writeInstance(instances.instance(i));
          }
          m_DataBaseConnection.disconnectFromDatabase();
          setWriteMode(WAIT);
          resetStructure();
          m_count = 1;
      } catch(Exception ex) {
            printException(ex);
       }    
  }

    /**Prints an exception
   * @param ex the exception to print
   */  
  private void printException(Exception ex){
  
      System.out.println("\n--- Exception caught ---\n");
	while (ex != null) {
		System.out.println("Message:   "
                                   + ex.getMessage ());
                if(ex instanceof SQLException){
                    System.out.println("SQLState:  "
                                   + ((SQLException)ex).getSQLState ());
                    System.out.println("ErrorCode: "
                                   + ((SQLException)ex).getErrorCode ());
                    ex = ((SQLException)ex).getNextException();
                }
                else
                    ex = null;
		System.out.println("");
	}
      
  }
  
  /** Gets the setting
   * @return the current setting
   */  
  public String[] getOptions() {
    Vector options = new Vector();

    if ( (m_tableName != null) && (m_tableName.length() != 0) ) {
      options.add("-T"); 
      options.add(m_tableName);
    }
    
    if (m_id)
        options.add("-P");

    if ( (m_inputFile != null) && (m_inputFile.length() != 0) ) {
      options.add("-i"); 
      options.add(m_inputFile);
    }
    
    return (String[]) options.toArray(new String[options.size()]);
  }
  
  /** Lists the available options
   * @return an enumeration of the available options
   */  
  public java.util.Enumeration listOptions() {
      
     FastVector newVector = new FastVector(3);

     newVector.addElement(new Option("\tThe name of the table (default: the relation name).",
				     "T",1,"-T <table name>"));
     newVector.addElement(new Option("\tAdd an ID column as primary key. The name is specified in the DatabaseUtils file. The DatabaseLoader won't load this column.",
				     "P",0,"-P"));
     newVector.addElement(new Option("\tInput file in arff format that should be saved in database.",
				     "i",1,"-i<input file name>"));
     
     return  newVector.elements();
  }
  
  /** Sets the options.
   *
   * Available options are:
   * -T <table name> <br>
   * Sets the name of teh table (default: the name of the relation)<p>
   *
   * -P <br>
   * If set, a primary key column is generated automatically (containing the row number as INTEGER)<p>
   *
   * -i <input-file> <br>
   * Specifies an ARFF file as input (for command line use) <p>
   *
   * @param options the options
   * @throws Exception if options cannot be set
   */  
  public void setOptions(String[] options) throws Exception {
      
    String tableString, inputString;
    tableString = Utils.getOption('T',options);
    inputString = Utils.getOption('i',options);
    resetOptions();
    if(tableString.length() != 0){
        m_tableName = tableString;
        m_tabName = false;
    }
    m_id = Utils.getFlag('P', options);
    if(inputString.length() != 0){
        try{
            m_inputFile = inputString;
            ArffLoader al = new ArffLoader();
            File inputFile = new File(inputString);
            al.setSource(inputFile);
            setInstances(al.getDataSet());
            //System.out.println(getInstances());
            if(tableString.length() == 0)
                m_tableName = getInstances().relationName();
        }catch(Exception ex) {
            printException(ex);
            ex.printStackTrace();
      }    
    }
  }
  
  /**
   * Main method.
   *
   * @param options should contain the options of a Saver.
   */
  public static void main(String [] options) {
      
      StringBuffer text = new StringBuffer();
      text.append("\n\nDatabaseSaver options:\n");
      try {
	DatabaseSaver asv = new DatabaseSaver();
        try {
          Enumeration enumi = asv.listOptions();
          while (enumi.hasMoreElements()) {
            Option option = (Option)enumi.nextElement();
            text.append(option.synopsis()+'\n');
            text.append(option.description()+'\n');
        }  
          asv.setOptions(options);
          asv.setDestination();
        } catch (Exception ex) {
            ex.printStackTrace();
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
        System.out.println(text);
	}
      
    }
}
  
  
