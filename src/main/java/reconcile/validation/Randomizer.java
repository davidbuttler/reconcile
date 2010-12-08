package reconcile.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Randomizer {

public static <T> Iterable<T> shuffleArray(Iterable<T> array, long seed)
{
  Random r = new Random(seed);
  List<T> in = Lists.newArrayList();
  for (T el : array) {
    in.add(el);
  }

  int length = Iterables.size(array);
  List<T> out = new ArrayList<T>(length);
  for (int i = 0; i < length; i++) {
    // chose an element at random
    int index = r.nextInt(in.size());
    out.set(i, in.get(index));
    in.remove(index);
  }
  return out;
}
}
