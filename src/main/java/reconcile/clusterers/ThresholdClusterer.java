package reconcile.clusterers;

public abstract class ThresholdClusterer
    extends Clusterer {

double threshold;

public double getThreshold()
{
  return threshold;
}

public void setThreshold(double threshold)
{
  this.threshold = threshold;
}

}
