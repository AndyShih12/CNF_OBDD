package bnncompiler.core;

import bnncompiler.model.*;
import org.sat4j.core.*;
import org.sat4j.minisat.*;
import org.sat4j.specs.*;
import java.util.*;

public class LearnOddFromFunc extends LearnOdd {

  public static void main(String args[]) {
    LearnOddFromFunc T = new LearnOddFromFunc(5);
  }

  public LearnOddFromFunc(int num_features) {
    super(num_features);
    this.run();
  }

  protected int[] equivalenceQuery(OddAccessStringNode oddas) {
    for (int i = 0; i < (1 << this.num_features); i++) {
      int[] instance = new int[this.num_features];

      for (int j = 0; j < this.num_features; j++) {
        instance[j] = (i >> j) % 2;
      }

      int oddas_verdict = evaluateOddas(instance);
      
      /*int oddas_verdict = 0;
      int[][] arr = oddas.convertToCnf(6, false).getClauses();
      int[][] arr2 = new int[arr.length+5][];
      for (int j = 0; j < arr.length; j++) {
        arr2[j] = arr[j].clone();
      }
      for (int j = 0; j < 5; j++) {
        arr2[arr.length + j] = new int[]{(2*instance[j]-1) * (j+1)};
      }
      ISolver solver = SolverFactory.newDefault();
      solver.newVar(5);
      try {
        for (int[] clause : arr2) {
          solver.addClause(new VecInt(clause));
        }        
        IProblem problem = solver;
        oddas_verdict = problem.isSatisfiable() ? 1 : 0;
      } catch (ContradictionException e) {
        System.out.println(e);
      } catch (TimeoutException e) {
        System.out.println(e);
      }
      */
      //System.out.println("oddas_verdict: " + oddas_verdict);
      

      int true_verdict = membershipQuery(instance);
      if (oddas_verdict != true_verdict) {
        return instance;
      }
    }
    return null;
  }
  protected int membershipQuery(int[] instance) {
    /*
    // card > 3
    int card = 0;
    for (int i = 0; i < this.num_features; i++) {
      card += instance[i];
    }
    return (card > 3) ? 1 : 0;
    */

    // parity = 1
    /*
    int parity = 0;
    for (int i = 0; i < this.num_features; i++) {
      parity ^= instance[i];
    }
    return parity;
    */

    
    // (x0 v x2) ^ (!x0 v x3 v x4) ^ (x1 v !x4)
    int cond1 = instance[0] + instance[2];
    int cond2 = (1-instance[0] + instance[3] + instance[4]);
    int cond3 = instance[1] + (1-instance[4]);

    return (cond1 > 0 && cond2 > 0 && cond3 > 0) ? 1 : 0;
    
  }

}


