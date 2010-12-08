/*
 * Copyright 1996 The Regents of the University of California. All rights reserved.
 * -----------------------------------------------------------------------
 * -----------------------------------------------------------------------
 * 
 * This work was produced at the University of California, Lawrence Livermore National Laboratory (UC LLNL) under
 * contract no. W-7405-ENG-48 (Contract 48) between the U.S. Department of Energy (DOE) and The Regents of the
 * University of California (University) for the operation of UC LLNL. The rights of the Federal Government are reserved
 * under Contract 48 subject to the restrictions agreed upon by the DOE and University as allowed under DOE Acquisition
 * Letter 97-1.
 * 
 * For full text see LICENSE.TXT
 * 
 * Created on Sept 7, 2004
 */
package reconcile.util;

import gov.llnl.text.util.Environment;

import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;


/**
 * Utility for parsing and retrieving command line arguments.
 * Arguments are treated as strings and assumed to be of the form:
 *
 *   -x [string]
 *
 * where: 'x' is any character;
 *        if absent, 'string' is given the value '1'
 * 
 * @author David Hysom
 * @created November 19, 2005
 * @version 1.0
 */
public class RuntimeArgs {
private static RuntimeArgs _instance;

/**
 * 
 */
static public void setRuntimeArgs(RuntimeArgs s) {
	_instance = s;
}

/**
 * 
 */
static public RuntimeArgs getRuntimeArgs() {
	if (_instance == null) {
		// System.err.println("\nERROR: WrapperRepository.getRuntimeArgs() - arg
		// = null");
		// System.exit(-1);
		_instance = new RuntimeArgs();
	}
	return _instance;
}


protected Map<String, String> mArgs = null;

public RuntimeArgs() {
	mArgs = Maps.newTreeMap();
}


  public RuntimeArgs(String[] args) {
	  this();
     if (args == null) {
        throw new IllegalArgumentException("args cannot be null");
     }

    String value;
    for (int i = 0; i < args.length; i++) {
      if (args[i].substring(0,1).equals("-")) {
        if (   i < args.length-1 
            && ! args[i+1].substring(0,1).equals("-")) {
          value = args[i+1];
        } else {
          value = "1";
        }
        mArgs.put(args[i].substring(1, args[i].length()), value);
      }
    }
  }

  /**
   * Returns true is there a command line switch 's'
   * is found; 's' should EXCLUDE the '-' character;
   */
  public boolean hasSwitch(String s) {
    if (mArgs.containsKey(s)) return true;
    return false;
  }

  
  /**
   * Returns the value associated with the command line switch 's';
   * 's' should EXCLUDE the '-' character;
   * If the switch is not found returns null.
   */
  public String get(String s) 
  {
    String value = null;

    //first, query the properties for the parameter
    Properties prop = System.getProperties();
    value = prop.getProperty(s);

    if (mArgs.containsKey(s)) {
      value = mArgs.get(s);
    }

    if (value != null) {
      value = Environment.convert(value);
    }

    return value;
  }

  @Override
  public String toString() {
  StringBuilder out = new StringBuilder();
  for (String key : mArgs.keySet()) {
    String value = mArgs.get(key);
    out.append("-").append(key).append(" ").append(value).append(" ");
  }
  return out.toString();
  } 

  public static void main(String [] args) {
    String s = "--help -h -c hello goodbye -x where -z";
    String [] ss = s.split(" ");
    RuntimeArgs a = new RuntimeArgs(ss);

    System.out.println("testing has switches: ");
    System.out.println("--help = " + a.hasSwitch("--help"));
    System.out.println("-help = " + a.hasSwitch("-help"));
    System.out.println("-x " + a.hasSwitch("-x"));

    System.out.println("testing get: ");
    System.out.println("--help = " + a.get("--help"));
    System.out.println("-x " + a.get("-x"));
    System.out.println("-c " + a.get("-c"));
    System.out.println("-z " + a.get("-z"));
  }
}

