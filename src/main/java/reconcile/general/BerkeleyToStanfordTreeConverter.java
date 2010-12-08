package reconcile.general;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;

public class BerkeleyToStanfordTreeConverter {

public static Tree convert(edu.berkeley.nlp.ling.Tree<String> t)
{
  TreeFactory tf = new LabeledScoredTreeFactory();
  Tree result;
  t = TreeAnnotations.unAnnotateTree(t);
  // recursive method to traverse a tree while adding spans of nodes to the
  // annotset
  if (t.isLeaf()) {
    result = tf.newLeaf(t.getLabel());
  }
  else {
    List<edu.berkeley.nlp.ling.Tree<String>> children = t.getChildren();

    List<Tree> newChildren = new ArrayList<Tree>();
    for (edu.berkeley.nlp.ling.Tree<String> tr : children) {
      Tree cur = convert(tr);

      newChildren.add(cur);
    }
    result = tf.newTreeNode(t.getLabel(), newChildren);

  }

  return result;
}
}
