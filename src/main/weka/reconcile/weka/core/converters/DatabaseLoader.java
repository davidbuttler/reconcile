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
 *    DatabaseLoader.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package reconcile.weka.core.converters;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;



/**
 * Reads from a database.
 * Can read a database in batch or incremental mode.
 * In inremental mode MySQL and HSQLDB are supported.
 * For all other DBMS set a pseudoincremental mode is used:
 * In pseudo incremental mode the instances are read into main memory all at once and then incrementally provided to the user.
 * For incremental loading the rows in the database table have to be ordered uniquely.
 * The reason for this is that every time only a single row is fetched by extending the user" query by a LIMIT clause.
 * If this extension is impossible instances will be loaded pseudoincrementally. To ensure that every row is fetched exaclty once, they have to ordered.
 * Therefore a (primary) key is necessary.This approach is chosen, instead of using JDBC driver facilities, because the latter one differ betweeen different drivers.
 * If you use the DatabaseSaver and save instances by generating automatically a primary key (its name is defined in DtabaseUtils), this primary key will
 * be used for ordering but will not be part of the output. The user defined SQL query to extract the instances should not contain LIMIT and ORDER BY clauses (see -Q option). 
 * In addition, for incremental loading,  you can define in the DatabaseUtils file how many distinct values a nominal attribute is allowed to have. If this number is exceeded, the column will become a string attribute.  
 * In batch mode no string attributes will be created.
 *
 * Available options are: 
 * -Q the query to specify which tuples to load<br>
 * The query must have the form:
 * SELECT *|<column-list> FROM <table> [WHERE}
 * (default: SELECT * FROM Results0).<p>
 *
 * -P comma separted list of columns that are a unqiue key <br>
 * Only needed for incremental loading, if it cannot be detected automatically<p>
 *
 * -I <br>
 * Sets incremental loading
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 * @see Loader
 */
public class DatabaseLoader extends AbstractLoader implements BatchConverter, IncrementalConverter, DatabaseConverter, OptionHandler {

  /** The header information that is retrieved in the beginning of incremental loading */
  protected Instances m_structure;
  
  /** Used in pseudoincremental mode. The whole dataset from which instances will be read incrementally.*/
  private Instances m_datasetPseudoInc;
  
  /** Set of instances that equals m_structure except that the auto_generated_id column is not included as an attribute*/
  private Instances m_oldStructure;
  
  /** The database connection */
  private DatabaseConnection m_DataBaseConnection;
  
  /** The user defined query to load instances. (form: SELECT *|<column-list> FROM <table> [WHERE <condition>]) */
  private String m_query = "Select * from Results0";;
  
  /** Flag indicating that pseudo incremental mode is used (all instances load at once into main memeory and then incrementally from main memory instead of the database) */
  private boolean m_pseudoIncremental;
  
  /** Limit when an attribute is treated as string attribute and not as a nominal one because it has to many values. */
  private int m_nominalToStringLimit;
  
  /** The number of rows obtained by m_query, eg the size of the ResultSet to load*/
  private int m_rowCount;
  
  /** Indicates how many rows has already been loaded incrementally */
  private int m_counter;
  
  /** Decides which SQL statement to limit the number of rows should be used. DBMS dependent. Algorithm just tries several possibilities. */
  private int m_choice;
  
  /** Flag indicating that incremental process wants to read first instance*/
  private boolean m_firstTime;
  
  /** Flag indicating that incremental mode is chosen (for command line use only)*/
  private boolean m_inc;
  
  /** Contains the name of the columns that uniquely define a row in the ResultSet. Ensures a unique ordering of instances for indremental loading.*/
  private FastVector m_orderBy;
  
  /** Stores the index of a nominal value */
  private Hashtable [] m_nominalIndexes;
  
  /**  Stores the nominal value*/
  private FastVector [] m_nominalStrings;
  
  /** Name of the primary key column that will allow unique ordering necessary for incremental loading. The name is specified in the DatabaseUtils file.*/
  private String m_idColumn;
  
  /* Type mapping used for reading*/
  public static final int STRING = 0;
  public static final int BOOL = 1;
  public static final int DOUBLE = 2;
  public static final int BYTE = 3;
  public static final int SHORT = 4;
  public static final int INTEGER = 5;
  public static final int LONG = 6;
  public static final int FLOAT = 7;
  public static final int DATE = 8; 
  
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
  
  /**
   * Constructor
   */
  public DatabaseLoader() throws Exception{
  
      reset();
      m_pseudoIncremental=false;
      String props=PROPERTIES.getProperty("nominalToStringLimit");
      m_nominalToStringLimit = Integer.parseInt(props);
      m_idColumn=PROPERTIES.getProperty("idColumn");
  }

  /**
   * Returns a string describing this Loader
   * @return a description of the Loader suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads Instances from a Database";
  }

  

  /** Resets the Loader ready to read a new data set
   * @throws Exception if an error occurs while disconnecting from the database
   */
  @Override
  public void reset() throws Exception{

    resetStructure();
    if(m_DataBaseConnection != null && m_DataBaseConnection.isConnected())
        m_DataBaseConnection.disconnectFromDatabase();
    m_DataBaseConnection = new DatabaseConnection();
    m_orderBy = new FastVector();
    //    m_query = "Select * from Results0";
    m_inc = false;
    
  }
  
  
  /** Resets the structure of instances*/
  public void resetStructure(){
  
      m_structure = null;
      m_datasetPseudoInc = null;
      m_oldStructure = null;
      m_rowCount = 0;
      m_counter = 0;
      m_choice = 0;
      m_firstTime = true;
      setRetrieval(NONE);
  }
  
  
  /**
   * Sets the query to execute against the database
   * @param q the query to execute
   */
  public void setQuery(String q) {
    q = q.replaceAll("[fF][rR][oO][mM]","FROM");
    q = q.replaceFirst("[sS][eE][lL][eE][cC][tT]","SELECT");  
    m_query = q;
  }

  /**
   * Gets the query to execute against the database
   * @return the query
   */
  public String getQuery() {
    return m_query;
  }
  
  /**
   * the tip text for this property
   * @return the tip text
   */
  public String queryTipText(){
  
      return "The query that should load the instances."
        +"\n The query has to be of the form SELECT <column-list>|* FROM <table> [WHERE <conditions>]";
  }
  
  /**
   * Sets the key columns of a database table
   * @param keys a String containing the key columns in a comma separated list.
   */
  public void setKeys(String keys){
  
    m_orderBy.removeAllElements();
    StringTokenizer st = new StringTokenizer(keys, ",");
    while (st.hasMoreTokens()) {
        String column = st.nextToken();
        column = column.replaceAll(" ","");
        m_orderBy.addElement(column);
    }
  }
  
   /**
   * Gets the key columns' name
   * @return name of the key columns'
   */
  public String getKeys(){
  
      StringBuffer key = new StringBuffer();
      for(int i = 0;i < m_orderBy.size(); i++){
        key.append((String)m_orderBy.elementAt(i));
        if(i != m_orderBy.size()-1)
          key.append(", ");
      }
      return key.toString();
  }
  
  /**
   * the tip text for this property
   * @return the tip text
   */
  public String keysTipText(){
  
      return "For incremental loading a unique identiefer has to be specified."
        +"\nIf the query includes all columns of a table (SELECT *...) a primary key"
        +"\ncan be detected automatically depending on the JDBC driver. If that is not possible"
        +"\nspecify the key columns here in a comma separated list.";
  }
  
  /**
   * Sets the database URL
   * @param string with the database URL
   */
  public void setUrl(String url){
      
      m_DataBaseConnection.setDatabaseURL(url);
    
  }
  
  /**
   * Gets the URL
   * @return the URL
   */
  public String getUrl(){
  
      return m_DataBaseConnection.getDatabaseURL();
  }
  
  /**
   * the tip text for this property
   * @return the tip text
   */
  public String urlTipText(){
  
      return "The URL of the database";
  }
  
  /**
   * Sets the database user
   * @param the database user name
   */
  public void setUser(String user){
   
      m_DataBaseConnection.setUsername(user);
  }
  
  /**
   * Gets the user name
   * @return name of database user
   */
  public String getUser(){
   
      return m_DataBaseConnection.getUsername();
  }
  
  /**
   * the tip text for this property
   * @return the tip text
   */
  public String userTipText(){
  
      return "The user name for the database";
  }
  
  /**
   * Sets user password for the database
   * @param the password
   */
  public void setPassword(String password){
   
      m_DataBaseConnection.setPassword(password);
  }
  
  /**
   * the tip text for this property
   * @return the tip text
   */
  public String passwordTipText(){
  
      return "The database password";
  }
  
  
  /** Sets the database url
   * @param url the database url
   * @param userName the user name
   * @param password the password
   */  
  public void setSource(String url, String userName, String password){
  
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
  public void setSource(String url){
  
      try{
        m_DataBaseConnection = new DatabaseConnection();
        m_DataBaseConnection.setDatabaseURL(url);
      } catch(Exception ex) {
            printException(ex);
       }    
  }
  
  /** Sets the database url using the DatabaseUtils file */  
  public void setSource() throws Exception{
  
        m_DataBaseConnection = new DatabaseConnection();
  }
  
  /**
   * Opens a connection to the database
   *
   */
  public void connectToDatabase() {
   
      try{
        if(!m_DataBaseConnection.isConnected()){
            m_DataBaseConnection.connectToDatabase();
        }
      } catch(Exception ex) {
	printException(ex);
       }    
  }
  
  
  /** Returns the table name or all after the FROM clause of the user specified query
   * to retrieve instances.
   * @param onlyTableName true if only the table name should be returned, false otherwise
   * @return the end of the query
   */  
  private String endOfQuery(boolean onlyTableName){
      String table;
      int beginIndex, endIndex;
      
      beginIndex = m_query.indexOf("FROM ")+5;
      while(m_query.charAt(beginIndex) == ' ')
          beginIndex++;
      endIndex = m_query.indexOf(" ",beginIndex);
      if(endIndex != -1 && onlyTableName)
          table = m_query.substring(beginIndex,endIndex);
      else
          table = m_query.substring(beginIndex);
      if(m_DataBaseConnection.getUpperCase())
          table = table.toUpperCase();
      return table;
  }
  
  /** Checks for a unique key using the JDBC driver's method:
   * getPrimaryKey(), getBestRowIdentifier().
   * Depending on their implementation a key can be detected.
   * The key is needed to order the instances uniquely for an inremental loading.
   * If an existing key cannot be detected, use -P option.
   * @throws Exception if database error occurs
   * @return true, if a key could have been detected, false otherwise
   */  
  private boolean checkForKey() throws Exception {
  
      String query = m_query;
      
      query = query.replaceAll(" +"," ");
      //query has to use all columns
      if(!query.startsWith("SELECT *"))
          return false;
      m_orderBy.removeAllElements();
      if(!m_DataBaseConnection.isConnected())
            m_DataBaseConnection.connectToDatabase();
      DatabaseMetaData dmd = m_DataBaseConnection.getMetaData();
      String table = endOfQuery(true);
      //System.out.println(table);
      //check for primary keys
      ResultSet rs = dmd.getPrimaryKeys(null,null,table);
      while(rs.next()){
          m_orderBy.addElement(rs.getString(4));
      }
      rs.close();
      if(m_orderBy.size() != 0)
          return true;
      //check for unique keys
      rs = dmd.getBestRowIdentifier(null,null,table,DatabaseMetaData.bestRowSession,false);
      ResultSetMetaData rmd = rs.getMetaData();
      int help = 0;
      while(rs.next()){
          m_orderBy.addElement(rs.getString(2));
          help++;
      }
      rs.close();
      if(help == rmd.getColumnCount()){
          m_orderBy.removeAllElements();
      }
      if(m_orderBy.size() != 0)
          return true;
      
      return false;
  }
  
  /** Converts string attribute into nominal ones for an instance read during
   * incremental loading
   * @param rs The result set
   * @param i the index of the nominal value
   * @throws Exception exception if it cannot be converted
   */  
  private void stringToNominal(ResultSet rs, int i) throws Exception{
  
      while(rs.next()){
        String str = rs.getString(1);
        if(!rs.wasNull()){
            Double index = (Double)m_nominalIndexes[i - 1].get(str);
            if (index == null) {
                index = new Double(m_nominalStrings[i - 1].size());
                m_nominalIndexes[i - 1].put(str, index);
                m_nominalStrings[i - 1].addElement(str);
            }
        }
      }
  }
  
  /** Used in incremental loading. Modifies the SQL statement,
   * so that only one instance per time is tretieved and the instances are ordered
   * uniquely.
   * @param query the query to modify for incremental loading
   * @param offset sets which tuple out of the uniquely ordered ones should be returned
   * @param choice the kind of query that is suitable for the used DBMS
   * @return the modified query that returns only one result tuple.
   */  
  private String limitQuery(String query, int offset, int choice){
  
      String limitedQuery;
      StringBuffer order = new StringBuffer();
      String orderByString = "";
      
      if(m_orderBy.size() != 0){
        order.append(" ORDER BY ");
        for(int i = 0; i < m_orderBy.size()-1; i++){
            if(m_DataBaseConnection.getUpperCase())
                order.append(((String)m_orderBy.elementAt(i)).toUpperCase());
            else
                order.append((String)m_orderBy.elementAt(i));
            order.append(", ");
        }
        if(m_DataBaseConnection.getUpperCase())
            order.append(((String)m_orderBy.elementAt(m_orderBy.size()-1)).toUpperCase());
        else
            order.append((String)m_orderBy.elementAt(m_orderBy.size()-1));
        orderByString = order.toString();
      }
      if(choice == 0){
          limitedQuery = query.replaceFirst("SELECT","SELECT LIMIT "+offset+" 1");
          limitedQuery = limitedQuery.concat(orderByString);
          return limitedQuery;
      }
      if(choice == 1){
          limitedQuery = query.concat(orderByString+" LIMIT 1 OFFSET "+offset);
          return limitedQuery;
      }
      limitedQuery = query.concat(orderByString+" LIMIT "+offset+", 1");
      //System.out.println(limitedQuery);
      return limitedQuery;
  }
  
  /** Counts the number of rows that are loaded from the database
   * @throws Exception if the number of rows cannot be calculated
   * @return the entire number of rows
   */  
  private int getRowCount() throws Exception{
  
    String query = "SELECT COUNT(*) FROM "+endOfQuery(false);
    if(m_DataBaseConnection.execute(query) == false) {
        throw new Exception("Cannot count results tuples.");
    }
    ResultSet rs = m_DataBaseConnection.getResultSet();
    rs.next();
    int i = rs.getInt(1);
    rs.close();
    return i;
  }


  /**
   * Determines and returns (if possible) the structure (internally the 
   * header) of the data set as an empty set of instances.
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if an error occurs
   */
  @Override
  public Instances getStructure() throws IOException {

    if (m_DataBaseConnection == null) {
      throw new IOException("No source database has been specified");
    }
    connectToDatabase();
  pseudo:
      try{
    if(m_pseudoIncremental && m_structure == null){
        if (getRetrieval() == BATCH) {
            throw new IOException("Cannot mix getting instances in both incremental and batch modes");
        }
        setRetrieval(NONE);  
        m_datasetPseudoInc = getDataSet();
        m_structure = new Instances(m_datasetPseudoInc,0);
        setRetrieval(NONE);
        return m_structure;
    }
    if (m_structure == null) {
        if(!m_DataBaseConnection.tableExists(endOfQuery(true)))
            throw new IOException("Table does not exist.");
        //finds out which SQL statement to use for the DBMS to limit the number of resulting rows to one
        int choice = 0;
        boolean rightChoice = false;
        while (!rightChoice){
            try{
                if (m_DataBaseConnection.execute(limitQuery(m_query,0,choice)) == false) {
                    throw new IOException("Query didn't produce results");
                }
                m_choice = choice;
                rightChoice = true;
            }
            catch (SQLException ex) {
                choice++;
                if(choice == 3){
                    System.out.println("Incremental loading not supported for that DBMS. Pseudoincremental mode is used if you use incremental loading.\nAll rows are loaded into memory once and retrieved incrementally from memory instead of from the database.");
                    m_pseudoIncremental = true;
                    break pseudo;
                }
            }
        }
        String end = endOfQuery(false);
        ResultSet rs = m_DataBaseConnection.getResultSet();
        ResultSetMetaData md = rs.getMetaData();
        rs.close();
        int numAttributes = md.getColumnCount();
        int [] attributeTypes = new int [numAttributes];
        m_nominalIndexes = new Hashtable [numAttributes];
        m_nominalStrings = new FastVector [numAttributes];
        for (int i = 1; i <= numAttributes; i++) {
            switch (m_DataBaseConnection.translateDBColumnType(md.getColumnTypeName(i))) {
                case STRING :
                    //System.err.println("String --> nominal");
                    ResultSet rs1;
                    String columnName = md.getColumnName(i);
                    if(m_DataBaseConnection.getUpperCase())
                        columnName = columnName.toUpperCase();
                    m_nominalIndexes[i - 1] = new Hashtable();
                    m_nominalStrings[i - 1] = new FastVector();
                    String query = "SELECT COUNT(DISTINCT( "+columnName+" )) FROM " + end;
                    if (m_DataBaseConnection.execute(query) == true){
                        rs1 = m_DataBaseConnection.getResultSet();
                        rs1.next();
                        int count = rs1.getInt(1);
                        rs1.close();
                        if(count > m_nominalToStringLimit || m_DataBaseConnection.execute("SELECT DISTINCT ( "+columnName+" ) FROM "+ end) == false){
                            attributeTypes[i - 1] = Attribute.STRING;
                            break;
                        }
                        rs1 = m_DataBaseConnection.getResultSet();
                    }
                    else{
                        //System.err.println("Count for nominal values cannot be calculated. Attribute "+columnName+" treated as String.");
                        attributeTypes[i - 1] = Attribute.STRING;
                        break;
                    }
                    attributeTypes[i - 1] = Attribute.NOMINAL;
                    stringToNominal(rs1,i);
                    rs1.close();
                    break;
                case BOOL:
                    //System.err.println("boolean --> nominal");
                    attributeTypes[i - 1] = Attribute.NOMINAL;
                    m_nominalIndexes[i - 1] = new Hashtable();
                    m_nominalIndexes[i - 1].put("false", new Double(0));
                    m_nominalIndexes[i - 1].put("true", new Double(1));
                    m_nominalStrings[i - 1] = new FastVector();
                    m_nominalStrings[i - 1].addElement("false");
                    m_nominalStrings[i - 1].addElement("true");
                    break;
                case DOUBLE:
                    //System.err.println("BigDecimal --> numeric");
                    attributeTypes[i - 1] = Attribute.NUMERIC;
                    break;
                case BYTE:
                    //System.err.println("byte --> numeric");
                    attributeTypes[i - 1] = Attribute.NUMERIC;
                    break;
                case SHORT:
                    //System.err.println("short --> numeric");
                    attributeTypes[i - 1] = Attribute.NUMERIC;
                    break;
                case INTEGER:
                    //System.err.println("int --> numeric");
                    attributeTypes[i - 1] = Attribute.NUMERIC;
                    break;
                case LONG:
                    //System.err.println("long --> numeric");
                    attributeTypes[i - 1] = Attribute.NUMERIC;
                    break;
                case FLOAT:
                    //System.err.println("float --> numeric");
                    attributeTypes[i - 1] = Attribute.NUMERIC;
                    break;
                case DATE:
                    attributeTypes[i - 1] = Attribute.DATE;
                    break;
                default:
                    //System.err.println("Unknown column type");
                    attributeTypes[i - 1] = Attribute.STRING;
            }
        }
        FastVector attribInfo = new FastVector();
        for (int i = 0; i < numAttributes; i++) {
            /* Fix for databases that uppercase column names */
            //String attribName = attributeCaseFix(md.getColumnName(i + 1));
            String attribName = md.getColumnName(i + 1);
            switch (attributeTypes[i]) {
                case Attribute.NOMINAL:
                    attribInfo.addElement(new Attribute(attribName, m_nominalStrings[i]));
                    break;
                case Attribute.NUMERIC:
                    attribInfo.addElement(new Attribute(attribName));
                    break;
                case Attribute.STRING:
                    attribInfo.addElement(new Attribute(attribName, (FastVector)null));
                    break;
                case Attribute.DATE:
                    attribInfo.addElement(new Attribute(attribName, (String)null));
                    break;
                default:
                    throw new IOException("Unknown attribute type");
            }
        }
        m_structure = new Instances(endOfQuery(true), attribInfo,0);
        //get rid of m_idColumn
        if(m_DataBaseConnection.getUpperCase())
              m_idColumn = m_idColumn.toUpperCase();
        //System.out.println(m_structure.attribute(0).name().equals(idColumn));
        if(m_structure.attribute(0).name().equals(m_idColumn)){
            m_oldStructure = new Instances(m_structure,0);
            m_oldStructure.deleteAttributeAt(0);
            //System.out.println(m_structure);
        }
        else
            m_oldStructure = new Instances(m_structure,0);
    }
    else{
        if(m_oldStructure == null)
            m_oldStructure = new Instances(m_structure,0);
    }
    m_DataBaseConnection.disconnectFromDatabase();
    }
    catch(Exception ex) {
        ex.printStackTrace();
	printException(ex);
    } 
    return m_oldStructure;
    
  }
  
  

  /**
   * Return the full data set in batch mode (header and all intances at once).
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if there is no source or parsing fails
   */
  @Override
  public Instances getDataSet() throws IOException {

    if (m_DataBaseConnection == null) {
      throw new IOException("No source database has been specified");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");
    }
    setRetrieval(BATCH);
    connectToDatabase();
    
    
    Instances result = null;
    try{
    if (m_DataBaseConnection.execute(m_query) == false) 
      throw new Exception("Query didn't produce results");
    ResultSet rs = m_DataBaseConnection.getResultSet();
    ResultSetMetaData md = rs.getMetaData();
    // Determine structure of the instances
    int numAttributes = md.getColumnCount();
    int [] attributeTypes = new int [numAttributes];
    m_nominalIndexes = new Hashtable [numAttributes];
    m_nominalStrings = new FastVector [numAttributes];
    for (int i = 1; i <= numAttributes; i++) {
      switch (m_DataBaseConnection.translateDBColumnType(md.getColumnTypeName(i))) {
	
      case STRING :
        ResultSet rs1;
        String columnName = md.getColumnName(i);
        if(m_DataBaseConnection.getUpperCase())
            columnName = columnName.toUpperCase();
        String end = endOfQuery(false);
        m_nominalIndexes[i - 1] = new Hashtable();
        m_nominalStrings[i - 1] = new FastVector();
        if(m_DataBaseConnection.execute("SELECT DISTINCT ( "+columnName+" ) FROM "+ end) == false){
            throw new Exception("Nominal values cannot be retrieved");
        }
        rs1 = m_DataBaseConnection.getResultSet();
        attributeTypes[i - 1] = Attribute.NOMINAL;
        stringToNominal(rs1,i);
        rs1.close();  
	break;
      case BOOL:
	//System.err.println("boolean --> nominal");
	attributeTypes[i - 1] = Attribute.NOMINAL;
	m_nominalIndexes[i - 1] = new Hashtable();
	m_nominalIndexes[i - 1].put("false", new Double(0));
	m_nominalIndexes[i - 1].put("true", new Double(1));
	m_nominalStrings[i - 1] = new FastVector();
	m_nominalStrings[i - 1].addElement("false");
	m_nominalStrings[i - 1].addElement("true");
	break;
      case DOUBLE:
	//System.err.println("BigDecimal --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case BYTE:
	//System.err.println("byte --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case SHORT:
	//System.err.println("short --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case INTEGER:
	//System.err.println("int --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case LONG:
	//System.err.println("long --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case FLOAT:
	//System.err.println("float --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case DATE:
	attributeTypes[i - 1] = Attribute.DATE;
	break;
      default:
	//System.err.println("Unknown column type");
	attributeTypes[i - 1] = Attribute.STRING;
      }
    }

    // Step through the tuples
    //System.err.println("Creating instances...");
    FastVector instances = new FastVector();
    while(rs.next()) {
      float[] vals = new float[numAttributes];
      for(int i = 1; i <= numAttributes; i++) {
	switch (m_DataBaseConnection.translateDBColumnType(md.getColumnTypeName(i))) {
	case STRING :
	  String str = rs.getString(i);
	  
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
            } else {
                Double index = (Double)m_nominalIndexes[i - 1].get(str);
                if (index == null) {
                    index = new Double(m_structure.attribute(i-1).addStringValue(str));
                }
                vals[i - 1] = index.floatValue();
            }
	  break;
	case BOOL:
	  boolean boo = rs.getBoolean(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (boo ? 1 : 0);
	  }
	  break;
	case DOUBLE:
	  float dd = rs.getFloat(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] =  dd;
	  }
	  break;
	case BYTE:
	  byte by = rs.getByte(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = by;
	  }
	  break;
	case SHORT:
	  short sh = rs.getByte(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = sh;
	  }
	  break;
	case INTEGER:
	  int in = rs.getInt(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = in;
	  }
	  break;
	case LONG:
	  long lo = rs.getLong(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = lo;
	  }
	  break;
	case FLOAT:
	  float fl = rs.getFloat(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = fl;
	  }
	  break;
	case DATE:
          Date date = rs.getDate(i);
          if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
            // TODO: Do a value check here.
            vals[i - 1] = date.getTime();
          }
          break;
	default:
	  vals[i - 1] = Instance.missingValue();
	}
      }
      Instance newInst;
      newInst = new Instance(1, vals);
      instances.addElement(newInst);
    }   
    
    // Create the header and add the instances to the dataset
    //System.err.println("Creating header...");
    FastVector attribInfo = new FastVector();
    for (int i = 0; i < numAttributes; i++) {
      /* Fix for databases that uppercase column names */
      //String attribName = attributeCaseFix(md.getColumnName(i + 1));
      String attribName = md.getColumnName(i + 1);
      switch (attributeTypes[i]) {
      case Attribute.NOMINAL:
	attribInfo.addElement(new Attribute(attribName, m_nominalStrings[i]));
	break;
      case Attribute.NUMERIC:
	attribInfo.addElement(new Attribute(attribName));
	break;
      case Attribute.STRING:
	attribInfo.addElement(new Attribute(attribName, (FastVector)null));
	break;
      case Attribute.DATE:
	attribInfo.addElement(new Attribute(attribName, (String)null));
	break;
      default:
	throw new IOException("Unknown attribute type");
      }
    }
    result = new Instances(endOfQuery(true), attribInfo, 
				     instances.size());
    for (int i = 0; i < instances.size(); i++) {
      result.add((Instance)instances.elementAt(i));
    }
    rs.close();
    m_DataBaseConnection.disconnectFromDatabase();
    //get rid of m_idColumn
    if(m_DataBaseConnection.getUpperCase())
        m_idColumn = m_idColumn.toUpperCase();
    if(result.attribute(0).name().equals(m_idColumn)){
        result.deleteAttributeAt(0);
    }
    m_structure = new Instances(result,0);
    }
    catch(Exception ex) {
	printException(ex);
        StringBuffer text = new StringBuffer();
        if(m_query.equals("Select * from Results0")){
            text.append("\n\nDatabaseLoader options:\n");
            Enumeration enumi = listOptions();
            while (enumi.hasMoreElements()) {
                Option option = (Option)enumi.nextElement();
                text.append(option.synopsis()+'\n');
                text.append(option.description()+'\n');
            }
            System.out.println(text);
        }
    }
    //System.out.println(result);
    return result;
  }
  
  /** Reads an instance from a database.
   * @param rs the ReusltSet to load
   * @throws Exception if instance cannot be read
   * @return an instance read from the database
   */  
  private Instance readInstance(ResultSet rs) throws Exception{
  
      FastVector instances = new FastVector();
      ResultSetMetaData md = rs.getMetaData();
      int numAttributes = md.getColumnCount();
      float[] vals = new float[numAttributes];
      m_structure.delete();
      for(int i = 1; i <= numAttributes; i++) {
	switch (m_DataBaseConnection.translateDBColumnType(md.getColumnTypeName(i))) {
	case STRING :
	  String str = rs.getString(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    Double index = (Double)m_nominalIndexes[i - 1].get(str);
	    if (index == null) {
              index = new Double(m_structure.attribute(i-1).addStringValue(str));
	    }
	    vals[i - 1] = index.floatValue();
	  }
	  break;
	case BOOL:
	  boolean boo = rs.getBoolean(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (boo ? 1 : 0);
	  }
	  break;
	case DOUBLE:
	  //	  BigDecimal bd = rs.getBigDecimal(i, 4); 
	  float dd = rs.getFloat(i);
	  // Use the column precision instead of 4?
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    //	    newInst.setValue(i - 1, bd.doubleValue());
	    vals[i - 1] =  dd;
	  }
	  break;
	case BYTE:
	  byte by = rs.getByte(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = by;
	  }
	  break;
	case SHORT:
	  short sh = rs.getByte(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = sh;
	  }
	  break;
	case INTEGER:
	  int in = rs.getInt(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = in;
	  }
	  break;
	case LONG:
	  long lo = rs.getLong(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = lo;
	  }
	  break;
	case FLOAT:
	  float fl = rs.getFloat(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = fl;
	  }
	  break;
	case DATE:
          Date date = rs.getDate(i);
          if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
            // TODO: Do a value check here.
            vals[i - 1] = date.getTime();
          }
          break;
	default:
	  vals[i - 1] = Instance.missingValue();
	}
      }
       Instance inst = new Instance(1, vals);
       //get rid of m_idColumn
       if(m_DataBaseConnection.getUpperCase())
              m_idColumn = m_idColumn.toUpperCase();
       if(m_structure.attribute(0).name().equals(m_idColumn)){
            inst.deleteAttributeAt(0);
            m_oldStructure.add(inst);
            inst = m_oldStructure.instance(0);
            m_oldStructure.delete(0);
       }
       else{
        //instances is added to and deleted from the structure to get the true nominal values instead of the index of the values.
        m_structure.add(inst);
        inst = m_structure.instance(0);
        m_structure.delete(0);
       }
       return inst;
       
  }

  /**
   * Read the data set incrementally---get the next instance in the data 
   * set or returns null if there are no
   * more instances to get. If the structure hasn't yet been 
   * determined by a call to getStructure then method does so before
   * returning the next instance in the data set.
   *
   * @return the next instance in the data set as an Instance object or null
   * if there are no more instances to be read
   * @exception IOException if there is an error during parsing
   */
  @Override
  public Instance getNextInstance() throws IOException {

      
    if (m_DataBaseConnection == null) 
      throw new IOException("No source database has been specified"); 
    if (getRetrieval() == BATCH) {
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");
    }
    //pseudoInremental: Load all instances into main memory in batch mode and give them incrementally to user
    if(m_pseudoIncremental){
        if (m_structure == null){
            setRetrieval(NONE);  
            getStructure();
        }
        setRetrieval(INCREMENTAL);
        if(m_datasetPseudoInc.numInstances() > 0){
            Instance current = m_datasetPseudoInc.instance(0);
            m_datasetPseudoInc.delete(0);
            return current;
        }
        else{
            resetStructure();
            return null;
        }
    }
    //real incremental mode. At the moment(version 1.0) only for MySQL and HSQLDB (Postgres not tested, should work)
    setRetrieval(INCREMENTAL);
    try{
        if(!m_DataBaseConnection.isConnected())
            connectToDatabase();
        //if no header determined yet, do it
        if(m_structure == null)
            m_structure = getStructure();
        //if no key columns specified by user, try to detect automatically
        if(m_firstTime && m_orderBy.size() == 0){
            if(!checkForKey())
                throw new Exception("A unique order cannot be detected automatically.\nYou have to use SELECT * in your query to enable this feature.\nMaybe JDBC driver is not able to detect key.\nDefine primary key in your database or use -P option (command line) or enter key columns in the GUI.");
        }
        if(m_firstTime){
            m_firstTime = false;
            m_rowCount = getRowCount();
        }
        //as long as not all rows has been loaded
        if(m_counter < m_rowCount){
            if (m_DataBaseConnection.execute(limitQuery(m_query,m_counter,m_choice)) == false) {
                throw new Exception("Tuple could not be retrieved.");
            }
            m_counter++;
            ResultSet rs = m_DataBaseConnection.getResultSet();
            rs.next();
            Instance current = readInstance(rs);
            rs.close();
            return current;
        }
        else{
            m_DataBaseConnection.disconnectFromDatabase();
            resetStructure();
            return null;
        }
    }catch(Exception ex) {
        printException(ex);
    }
    return null;
  }
  
  
  
  /** Gets the setting
   * @return the current setting
   */  
  public String[] getOptions() {
      
    Vector options = new Vector();
    
    options.add("-Q"); 
    options.add(getQuery());
    
    StringBuffer text = new StringBuffer();
    for (int i = 0; i < m_orderBy.size(); i++) {
      if (i > 0)
        text.append(", ");
      text.append((String) m_orderBy.elementAt(i));
    }
    options.add("-P"); 
    options.add(text.toString());
    
    if (m_inc)
      options.add("-I");
    
    return (String[]) options.toArray(new String[options.size()]);
  }
  
  /** Lists the available options
   * @return an enumeration of the available options
   */  
  public java.util.Enumeration listOptions() {
      
     FastVector newVector = new FastVector(3);

     newVector.addElement(new Option("\tSQL query of the form SELECT <list of columns>|* FROM <table> [WHERE] to execute (default Select * From Results0).",
				     "Q",1,"-Q <query>"));
     newVector.addElement(new Option("\tList of column names uniquely defining a DB row (separated by ', ').\n\tUsed for incremental loading."
        +"\n\tIf not specified, the key will be determined automatically, if possible with the used JDBC driver.\n\tThe auto ID column created by the DatabaseSaver won't be loaded.",
				     "P",1,"-P<list of column names>"));
     newVector.addElement(new Option("\tSets incremental loading", "I", 0,
				    "-I"));
     
     return  newVector.elements();
  }
  
  /** Sets the options.
   *
   * Available options are:
   * -Q the query to specify which tuples to load<br>
   * The query must have the form:
   * SELECT *|<column-list> FROM <table> [WHERE}
   * (default: SELECT * FROM Results0).<p>
   *
   * -P comma separted list of columns that are a unqiue key <br>
   * Only needed for incremental loading, if it cannot be detected automatically<p>
   *
   * -I <br>
   * Sets incremental loading
   *
   * @param options the options
   * @throws Exception if options cannot be set
   */  
  public void setOptions(String[] options) throws Exception {
      
    String optionString, keyString;
    optionString = Utils.getOption('Q',options);
    keyString = Utils.getOption('P',options);
    reset();
    if (optionString.length() != 0)
      setQuery(optionString);
    m_orderBy.removeAllElements();
    m_inc = Utils.getFlag('I', options);
    if(m_inc){
        StringTokenizer st = new StringTokenizer(keyString, ",");
        while (st.hasMoreTokens()) {
            String column = st.nextToken();
            column = column.replaceAll(" ","");
            m_orderBy.addElement(column);
        }
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

  /** Main method.
   * @param options the options
   */
  public static void main(String [] options) {
    
      DatabaseLoader atf;
      try {
	atf = new DatabaseLoader();
        atf.setOptions(options);
        atf.setSource();
        if(!atf.m_inc)
            System.out.println(atf.getDataSet());
        else{
            System.out.println(atf.getStructure());
            Instance temp;
            do {
            temp = atf.getNextInstance();
            if (temp != null) {
                System.out.println(temp);
            }
            } while (temp != null);
        }
      } catch (Exception e) {
	e.printStackTrace();
        System.out.println("\n"+e.getMessage());
      }
    
  }
  
  
  
}
