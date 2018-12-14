package bnncompiler.model;

import java.util.*;


public class CnfFormula {

  private int[][] clauses;
  private int num_vars;

  public CnfFormula(String cnf_file) {
    cnf_file = cnf_file + " 0\n";
    String[] lines = cnf_file.split("\n");
    ArrayList<Integer> arr = new ArrayList<Integer>();
    int clause_index = 0;

    for (String line : lines) {
      String[] tokens = line.split("[ \t]");

      if (tokens[0] == "c") {
        continue;
      }
      else if (tokens[0] == "p") {
        assert(tokens[1] == "cnf");
        this.num_vars = Integer.parseInt(tokens[2]);
        this.clauses = new int[Integer.parseInt(tokens[3])][];
      }
      else {
        for (String token : tokens) {
          int e = Integer.parseInt(token);
          if (e == 0) {
            this.clauses[clause_index] = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
              this.clauses[clause_index][i] = arr.get(i);
            }
            arr.clear();
            clause_index += 1;
          }
          else {
            arr.add(e);
          }
        }
      }
    }
    assert(clause_index == this.clauses.length);
  }

  public CnfFormula(int num_vars, int[][] clauses) {
    setNumVars(num_vars);
    setClauses(clauses);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("p cnf ");
    sb.append(this.num_vars);
    sb.append(" ");
    sb.append(this.clauses.length);
    sb.append("\n");

    for (int[] clause : clauses) {
      for (int e : clause) {
        sb.append(e);
        sb.append(" ");
      }
      sb.append("0\n");
    }
    return sb.toString();
  }

  public boolean evaluate(int[] instance) { // instance[0] = x_1
    assert(instance.length == this.num_vars);
    for (int[] clause: clauses) {
      boolean good_clause = false;
      for (int e : clause) {
        boolean b0 = instance[Math.abs(e) - 1] == 1;
        boolean b1 = e > 0;
        if (b0 == b1) {
          good_clause = true;
          break;
        }
      }
      if (!good_clause) {
        return false;
      }
    }
    return true;
  }

  public int getNumVars() {
    return this.num_vars;
  }
  public int[][] getClauses() {
    return this.clauses;
  }
  public void setNumVars(int num_vars) {
    this.num_vars = num_vars;
  }
  public void setClauses(int[][] clauses) {
    this.clauses = new int[clauses.length][];
    for (int i = 0; i < clauses.length; i++) {
      this.clauses[i] = clauses[i].clone();
    }
  }
}


