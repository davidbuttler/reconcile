package reconcile.general;

import java.util.Arrays;

/**
 * Efficiently maintains a disjoint set of integers. Allows for the operations: union and find.
 */

public class UnionFind {

/** contains a map of the root indices and size of each set **/
/* if map[i] is negative, then i is the root of the set with -map[i] elements */
/* if map[i] is non-negative, then i is not the root. recurse on map[i] */
private int[] map;

public UnionFind(int capacity) {
  map = new int[capacity + 1];

  for (int i = 0; i < map.length; ++i) {
    map[i] = -1;
  }
}

public int merge(int a, int b)
{
  // System.out.println("Merging "+a+" and "+b+":");
  // printMap();
  int aIndx = find(a);
  int bIndx = find(b);
  if (aIndx == bIndx) return aIndx;
  // merge smaller set into larger set
  if (map[bIndx] > map[aIndx]) { // b is less negative, therefore smaller
    map[aIndx] += map[bIndx];
    map[bIndx] = aIndx;
    return aIndx;
  }
  else {
    map[bIndx] += map[aIndx];
    map[aIndx] = bIndx;
    return bIndx;
  }
}

public int find(int a)
{
  // Check for capacity violations
  // if that happens, double the capacity of the pointers array
  if (a >= map.length) {
    int[] newMap = new int[map.length * 2];
    for (int j = 0; j < map.length; j++) {
      newMap[j] = map[j];
    }
    for (int j = map.length; j < newMap.length; j++) {
      newMap[j] = -1;
    }
    map = newMap;
  }
  // System.out.println(a);
  if (map[a] < 0) return a;

  return (map[a] = find(map[a]));
}

public int getCapacity()
{
  return map.length;
}

public void printMap()
{
  System.out.println(Arrays.toString(map));
}
}
