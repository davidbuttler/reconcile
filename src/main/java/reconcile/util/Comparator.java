/*
 * Simple program to track changes in Reconcile scores for new installations and new versions.
 */
package reconcile.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Comparator {

public static final Pattern p = Pattern.compile("(\\d\\d\\.\\d\\d)");

private static void extractScore(float[] score, String line)
{
  Matcher m;
  m = p.matcher(line);
  // capture the precision
  if (m.find()) {
    score[0] = Float.parseFloat(m.group(0));
  }

  // capture the recall
  if (m.find()) {
    score[1] = Float.parseFloat(m.group(0));
  }

  // capture the fmeasure
  if (m.find()) {
    score[2] = Float.parseFloat(m.group(0));
  }
}

public static void diffScores(String[] argv) {

  String inFile = argv[0];
  String stdFile = argv[1];
  float[] userMucScore = new float[3];
  float[] userBCubedScore = new float[3];

  System.out.println("user file: " + inFile);
  try {
    readScores(userMucScore, userBCubedScore, inFile);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }

  float[] baseMucScore = new float[3];
  float[] baseBCubedScore = new float[3];

  System.out.println("std file: " + stdFile);
  try {
    readScores(baseMucScore, baseBCubedScore, stdFile);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }

  if (matches(userMucScore, baseMucScore)) {
    System.out.println("System passed perfectly, MUC scores are the same.\n");
  }
  else {
    printDiff(userMucScore, baseMucScore, "MUC");
  }

  if (matches(userBCubedScore, baseBCubedScore)) {
    System.out.println("System passed, B^3 scores the same.\n");
  }
  else {
    printDiff(userBCubedScore, baseBCubedScore, "B-Cubed");
  }

}

public static void diffFiles(String[] argv) {
  String inFile = argv[0];
  String stdFile = argv[1];

}

public static void main(String[] argv)
{
	if(argv[0].equals("-f")) {
		diffFiles(argv);
	}
	else {
		diffScores(argv);
	}
}

private static boolean matches(float[] score, float[] score2)
{
  return (score[0] == score2[0]) && (score[1] == score2[1]) && (score[2] == score2[2]);
}

private static void printDiff(float[] userScore, float[] baseScores, String scoreName)
{
  float recDiff;
  float precDiff;
  float fDiff;
  recDiff = (baseScores[1] - userScore[1]);
  precDiff = (baseScores[0] - userScore[0]);
  fDiff = (baseScores[2] - userScore[2]);
  System.out.println(scoreName + " scores are different!\n");
  System.out.println("Baseline scores:");
  System.out.println("R:" + baseScores[1] + ", P:" + baseScores[0] + ", F:" + baseScores[2]);
  System.out.println("Your system's scores, difference from gold in parenthesis:");
  System.out.println("R:" + userScore[1] + " (" + recDiff + "), P:" + userScore[0] + " (" + precDiff + "), F:"
      + userScore[2] + "(" + fDiff + ")");
}

private static void readLines(String userFile, String stdFile) 
	throws FileNotFoundException, IOException 
{

  FileReader fr1 = new FileReader(userFile);
  BufferedReader br1 = new BufferedReader(fr1);
  
  String line;
  ArrayList al = new ArrayList<String>();
  while ((line = br1.readLine()) != null) {
		al.add(line.trim());
	}
 
	br1.close();
	Iterator itr = al.iterator();


  FileReader fr2 = new FileReader(stdFile);
  BufferedReader br2 = new BufferedReader(fr2);
 
  String userLine; 
  while ((line = br2.readLine()) != null) {
	  line = line.trim();
	  if(itr.hasNext()) {
		  userLine = (String)itr.next();

		  if(line.equals(userLine)) {
				continue;
		  }
		  else {
				System.out.println("---Difference---");
				System.out.println("STD: " + line);
				System.out.println("USR: " + userLine);
		  }
	  }
	  else {
			System.out.println("STD File:" + line );
		}
	}

  while(itr.hasNext()) {
		System.out.println("USR File:" + itr.next());
  }
}

private static void readScores(float[] mucScore, float[] bCubedScore, String file)
    throws FileNotFoundException, IOException
{
  FileReader fr = new FileReader(file);
  BufferedReader br = new BufferedReader(fr);

  String line;
  while ((line = br.readLine()) != null) {
    if (line.startsWith("MUCScore")) {
      System.out.println(line);
      extractScore(mucScore, line);
    }
    else if (line.startsWith("BCubedScore")) {
      System.out.println(line);
      extractScore(bCubedScore, line);
    }
  }
  br.close();
}
}
