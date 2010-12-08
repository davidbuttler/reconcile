/*
 * stripSGML.java nathan; Mar 15,  2010
 */
package reconcile.featureExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.xml.sax.Attributes;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Utils;


public class SGMLStripperRawTextWithNoAnnotations extends SGMLStripper {

FileWriter rawTextFile;
FileWriter originalRawTextFile;
int offset;
Stack<Annotation> anStack;

/*
 * Call the parent's constructor. 
 */
public SGMLStripperRawTextWithNoAnnotations() {
  super();
}

/*
 * Preprocesses the input file so things like ampersands don't break
 * the parser. 
 */
@Override
public void format(BufferedReader br, FileWriter out) throws IOException
{
}

@Override
public void run(Document doc, String[] annSetNames)
{
	AnnotationSet nps = new AnnotationSet("gsNPs");
	
	//create a dummy gsNPs file.
	doc.writeAnnotationSet(nps);
	
	/*
	 * TODO remove & from raw.txt files too. 
	String inputFile = doc.getAbsolutePath() + Utils.SEPARATOR + "raw.txt";
	String origFile = doc.getAbsolutePath() + Utils.SEPARATOR + "orig_raw.txt";
	
	try {
		FileReader reader = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(reader);
	}
	catch(IOException ioe) {
		throw new RuntimeException(ioe);	
	}
	*/

	
}

/*
 * Grabs the opening SGML tag. 
 */
@Override
public void startElement(String uri, String name, String qName, Attributes atts)
{

}

/*
 * Grabs the closing tag. 
 */
@Override
public void endElement(String uri, String name, String qName)
{
}

/*
 * This prints out all the text between the tags we care about to 
 * a file. 
 *
 */
@Override
public void characters(char ch[], int start, int length)
{
}
}
