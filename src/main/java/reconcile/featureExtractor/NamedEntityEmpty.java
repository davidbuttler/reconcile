package reconcile.featureExtractor;

import reconcile.data.AnnotationSet;
import reconcile.data.Document;

public class NamedEntityEmpty extends InternalAnnotator {

	public NamedEntityEmpty() {
	}

	public void run(Document doc, String[] annSetNames) {
		//writes out an empty annots file.
		AnnotationSet namedEntities = new AnnotationSet(annSetNames[0]);		
		doc.addAnnotationSet(namedEntities,outputAnnotationFiles);
	}
}
