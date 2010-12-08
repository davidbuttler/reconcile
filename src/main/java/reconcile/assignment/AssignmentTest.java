package reconcile.assignment;

import java.util.Random;

/**
 * Test harness for the assignment package
 */
public class AssignmentTest {

public static void main(String[] args)
{
  int sizei = 2, sizej = 3;
  Random rand = new Random();
  double[][] links = new double[sizei][];
  double[][] links1 = new double[sizej][sizei];
  // Arrays.fill(link)
  for (int i = 0; i < sizei; i++) {
    links[i] = new double[sizej];
    for (int j = 0; j < sizej; j++) {
      links[i][j] = rand.nextDouble() * 50;
      links1[j][i] = links[i][j];
    }
  }

  printArray(links);
  System.out.println();

  AssignmentProblem ap = new AssignmentProblem(links);
  int[][] solution = ap.solve(new HungarianAlgorithm());

  printArray(solution);
  System.out.println("====");
  AssignmentProblem ap1 = new AssignmentProblem(links1);
  int[][] solution1 = ap1.solve(new HungarianAlgorithm());

  printArray(solution1);

  double cost = 0, rcost = 0;
  for (int i = 0; i < solution.length; i++) {
    if (solution[i][0] >= 0) {
      cost += links[solution[i][0]][i];
    }
  }

  for (int i = 0; i < solution1.length; i++) {
    if (solution1[i][0] >= 0) {
      rcost += links1[solution1[i][0]][i];
    }
  }
  System.out.println("\ncost is " + cost + " reverse cost is " + rcost);
}

public static void printArray(int[] arg)
{
  if (arg == null) return;
  StringBuilder result = new StringBuilder();
  for (int i : arg) {
    result.append(i).append("\t");
  }
  System.out.println(result);
}

public static void printArray(double[] arg)
{
  StringBuilder result = new StringBuilder();
  for (double i : arg) {
    result.append(i).append("\t");
  }
  System.out.println(result);
}

public static void printArray(int[][] arg)
{
  for (int i[] : arg) {
    printArray(i);
  }
}

public static void printArray(double[][] arg)
{
  for (double i[] : arg) {
    printArray(i);
  }
}

}
