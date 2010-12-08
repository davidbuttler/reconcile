package reconcile.filter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import reconcile.data.Annotation;
import reconcile.data.Document;

public class DuncanPairs extends PairGenerator {
	private ArrayList<String> lines;
	private ArrayList<String> comments;
	
	private Annotation getNP(int begin, int end) {
		for (Annotation np : nps) {
			if (np.compareSpan(begin, end) == 0) {
				return np;
			}
			
		}		
		return null;		
	}
	
	public DuncanPairs() {
	
	}
	
	@Override
	public void initialize(Annotation[] nps, Document doc, boolean training)
	{
	  super.initialize(nps, doc, training);
	  lines = new ArrayList<String>();
	  comments = new ArrayList<String>();
	  
	  try { 	  
		  BufferedReader pairFileReader = new BufferedReader(new FileReader(doc.getAnnotationSetFile("duncan_pairs")));
		  
		  String line;
		  while ((line = pairFileReader.readLine()) != null) {
			  line = line.trim();
			  if (line.startsWith("#")) {
				  comments.add(line);			  	
			  }
			  else {				
				  lines.add(line);
			  }
		  }
	  }
	  catch(IOException ioe) {
		System.out.println("Problem reading Duncan Pairs file.");  
	  }	    
	}
	
	@Override
	public boolean hasNext()
	{
	  if (lines.size() > 0) return true;
	  return false;  
	}
	
	@Override
	public Annotation[] nextPair() {
		if (hasNext()) {
			
			String line = lines.get(0);
			lines.remove(0);
			
			String[] tokens = line.split("\t");
			
			String bytespan1 = tokens[1];
			String bytespan2 = tokens[2];
						
			Annotation np1 = getNP(Integer.parseInt(bytespan1.split(",")[0]), Integer.parseInt(bytespan1.split(",")[1]));
			Annotation np2 = getNP(Integer.parseInt(bytespan2.split(",")[0]), Integer.parseInt(bytespan2.split(",")[1]));
			
			if (np1 == null || np2 == null) {
				System.out.println("AAAAAAAH! Nulls");				
			}
					    		
			//System.out.println("Pairing: " + doc.getAnnotString(np1).replace("\n", " ") + " & " + doc.getAnnotString(np2).replace("\n", " "));
			return new Annotation[] { np1, np2 };
		}
		else {
			return null;		
		}
	}	
}


