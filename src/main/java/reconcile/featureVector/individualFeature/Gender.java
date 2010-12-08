package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.GenderEnum;


/*
 * This feature is: C the two np's agree in gender I if they disagree NA if the gender information for either cannot be
 * determined
 */

public class Gender
    extends NominalFeature {

public Gender() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return ICNS;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  FeatureUtils.GenderEnum gen1 = reconcile.features.properties.Gender.getValue(np1, doc);
  FeatureUtils.GenderEnum gen2 = reconcile.features.properties.Gender.getValue(np2, doc);
  /*
  //Special case -- an organization can be refered in different ways
  if(FeatureUtils.getNPSemType(np2, annotations, text).equals(NPSemType.ORGANIZATION)){
  	if(FeatureUtils.isPronoun(np1,annotations, text))
  		if(!FeatureUtils.getNumber(np1, annotations, text).equals(FeatureUtils.Number.SINGLE)||
  				FeatureUtils.getGender(np1, annotations, text).equals(FeatureUtils.Gender.NEUTER))
  			return COMPATIBLE;
  		else
  			return INCOMPATIBLE;
  	else
  		//Not pronoun --can be coreferent with other organizations or things with either gender
  		if(gen1.equals(FeatureUtils.Gender.EITHER)||gen1.equals(gen2))
  			return COMPATIBLE;
  		else
  			if(gen1.equals(FeatureUtils.Gender.UNKNOWN))
  				return NA;
  			else
  				return INCOMPATIBLE;
  }
  //if(FeatureUtils.isPronoun(np2,annotations, text)&&FeatureUtils.getNPSemType(np1, annotations, text).equals(NPSemType.ORGANIZATION))
  //	if(FeatureUtils.getNumber(np2, annotations, text).equals(FeatureUtils.Number.PLURAL) ||
  //			FeatureUtils.getGender(np2, annotations, text).equals(FeatureUtils.Gender.NEUTER))
  //		return COMPATIBLE;
  if(FeatureUtils.getNPSemType(np1, annotations, text).equals(NPSemType.ORGANIZATION)){
  	if(FeatureUtils.isPronoun(np2,annotations, text))
  		if(!FeatureUtils.getNumber(np2, annotations, text).equals(FeatureUtils.Number.SINGLE)||
  				FeatureUtils.getGender(np2, annotations, text).equals(FeatureUtils.Gender.NEUTER))
  			return COMPATIBLE;
  		else
  			return INCOMPATIBLE;
  	else
  		//Not pronoun --can be coreferent with other organizations or things with either gender
  		if(gen2.equals(FeatureUtils.Gender.EITHER)||gen1.equals(gen2))
  			return COMPATIBLE;
  		else 
  			if(gen2.equals(FeatureUtils.Gender.UNKNOWN))
  				return NA;
  			else
  				return INCOMPATIBLE;
  }
  *
  */
  if (gen1.equals(FeatureUtils.GenderEnum.UNKNOWN) || gen2.equals(FeatureUtils.GenderEnum.UNKNOWN)) return NA;
  // if(gen1.equals(FeatureUtils.Gender.NEUTER) || gen2.equals(FeatureUtils.Gender.NEUTER))
  // return NA;
  if (gen1.equals(gen2) && (gen1.equals(GenderEnum.FEMININE) || gen1.equals(GenderEnum.MASC))) return SAME;
  // if(gen1.equals(FeatureUtils.Gender.EITHER)&&!gen2.equals(FeatureUtils.Gender.NEUTER))
  // return COMPATIBLE;
  // if(gen2.equals(FeatureUtils.Gender.EITHER)&&!gen1.equals(FeatureUtils.Gender.NEUTER))
  // return COMPATIBLE;
  if (gen1.equals(FeatureUtils.GenderEnum.EITHER) || gen1.equals(FeatureUtils.GenderEnum.NEUTER)) return COMPATIBLE;
  if (gen2.equals(FeatureUtils.GenderEnum.EITHER) || gen2.equals(FeatureUtils.GenderEnum.NEUTER)) return COMPATIBLE;
  return INCOMPATIBLE;
}

}
