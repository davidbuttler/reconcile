package reconcile.general;

public class Constants {

// A class that defines some constants used throughout the application
// Turn on and off the debug mode
public static boolean DEBUG = false;

// Strings that correspond to different annotation set names
public static final String SENT = "Sentence";
public static final String PAR = "Paragraph";
public static final String POS = "PartOfSpeech";
public static final String TOKEN = "Token";
public static final String PARSE = "Parse";
public static final String DEP = "Dependency";
public static final String PARTIAL_PARSE = "PartialParse";
public static final String ORIG = "OriginalMarkup";
public static final String NP = "MarkableNP";
public static final String GS_NP = "GS_CEs";
public static final String NE = "NamedEntities";

// Constants used in the file structure
public static final String ANNOT_DIR_NAME = "annotations";
public static final String FEAT_DIR_NAME = "features";
public static final String PRED_DIR_NAME = "predictions";

public static final String FEAT_FILE_NAME = "features";
public static final String PRED_FILE_NAME = "predictions";
public static final String CLUSTER_FILE_NAME = "coref_output";
public static final String PROPERTIES_FILE_NAME = "npProperties";
public static final String GS_OUTPUT_FILE = "gsNPs";
public static final String GS_NE_OUTPUT_FILE = "gsNEs";
public static boolean PAR_NUMS_UNAVAILABLE = false;

// Constants used for different attributes
/**
 * cluster id is the identifier for the chain a coreferent entity appears in
 */
public static final String CLUSTER_ID = "CorefID";
/**
 * CE == coreferent entity
 */
public static final String CE_ID = "NO";

// The names of some feature that are used often in other features
public static final String APPOSITIVE = "Appositive";
public static final String PREDNOM = "Prednom";
public static final String ALIAS = "Alias";

/**
 * 
 */
public static final String RESPONSE_NPS = "responseNPs";

// Well-known Attributes
/**
 * start of the head for this NP
 */
public static final String HEAD_START = "HEAD_START";
/**
 * end of the head for this NP
 */
public static final String HEAD_END = "HEAD_END";

/**
 * id of the matching gold attribute
 */
public static final String MATCHED_GS_NP = "matched_gs_np";

// annotation types
/**
 * coref type string
 */
public static final String COREF = "coref";

}
