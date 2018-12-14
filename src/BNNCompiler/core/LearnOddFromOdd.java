package bnncompiler.core;

import bnncompiler.model.*;
import org.sat4j.core.*;
import org.sat4j.minisat.*;
import org.sat4j.specs.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

public class LearnOddFromOdd extends LearnOdd {

  CnfFormula cnf_formula;
  long eq_time;
  long mem_time;
  int eq_calls;
  int mem_calls;
  OddAccessStringNode dummy;

  ISolver solver;
  int membership_unit;
  int equivalence_one_sink;
  int equivalence_zero_sink;

  private static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  public static void main(String args[]) {
    LearnOddFromOdd T = new LearnOddFromOdd(20, "output/adaptive_testing.netHV2.odd");
  }

  public LearnOddFromOdd(int num_features, String file) {
    super(num_features);

    try {
      String file_str = readFile(file, Charset.forName("UTF-8"));
      String[] lines = file_str.split("\n");
      int num_nodes = lines.length + 2;

      int[] indeg = new int[num_nodes];
      int[] label = new int[num_nodes];
      int[][] children = new int[num_nodes][];
      OddAccessStringNode[] nodes = new OddAccessStringNode[num_nodes];
      nodes[num_nodes-1] = new OddAccessStringNode(num_features, new int[]{}, OddAccessStringNode.NodeType.ONE_SINK);
      nodes[num_nodes-2] = new OddAccessStringNode(num_features, new int[]{}, OddAccessStringNode.NodeType.ZERO_SINK);

      for (String line : lines) {
        String[] ids = line.split(" ");
        for (int i = 1; i <= 2; i++) {
          if (ids[i].equals("T") || ids[i].equals("F")) {
            ids[i] = ids[i].equals("T") ? Integer.toString(num_nodes-1) : Integer.toString(num_nodes-2);
          }
        }
        int cur = Integer.parseInt(ids[0]);
        int ch0 = Integer.parseInt(ids[1]);
        int ch1 = Integer.parseInt(ids[2]);

        children[cur] = new int[]{ch0, ch1};
        indeg[ch0] += 1;
        indeg[ch1] += 1;        
      }
      int root_index = -1;
      for (int i = 0; i < num_nodes; i++) {
        if (indeg[i] == 0) {
          root_index = i;
          break;
        }
      }

      LinkedList<Integer> queue = new LinkedList<Integer>();
      queue.add(root_index);
      nodes[root_index] = new OddAccessStringNode(0, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
      while (queue.size() > 0) {
        Integer cur = queue.remove();
        for (int i = 0; i < 2; i++) {
          if (nodes[children[cur][i]] == null) {
            queue.add(children[cur][i]);
            nodes[children[cur][i]] = new OddAccessStringNode(nodes[cur].getLabel() + 1, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
          }
          nodes[cur].setChild(i, new int[]{}, nodes[children[cur][i]]);
        }
      }

      dummy = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
      dummy.setChild(0, new int[]{}, nodes[root_index]);
      dummy.setChild(1, new int[]{}, nodes[root_index]);
      this.cnf_formula = dummy.convertToCnf(num_features+1);

      this.setupCnf();

      System.out.println("executing external command");
      String cmd = "./riss_script";
      Process pr = Runtime.getRuntime().exec(cmd);
      Writer w = new java.io.OutputStreamWriter(pr.getOutputStream());
      w.append("p cnf 3 1\n1 2 3 0\n");
      w.flush();
      w.close();
      try {
        pr.waitFor();
      } catch (InterruptedException e) {
        System.out.println(e);
      }
      BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
      String line = "";
      while ((line = buf.readLine()) != null) {
        System.out.println(line);
      }

      //this.run();
    } catch (IOException e) {
      System.out.println("Cannot read file: " + file);
    }

    System.out.println(this.cnf_formula.getClauses().length);
    System.out.println("Eq calls: " + eq_calls + " Eq time: " + eq_time/1000000000. + " Time/Call: " + eq_time/(eq_calls*1000000000.));
    System.out.println("Mem calls: " + mem_calls + " Mem time: " + mem_time/1000000000. + " Time/Call: " + mem_time/(mem_calls*1000000000.));
  }

  private void setupCnf() {
    this.solver = SolverFactory.newDefault();
    this.solver.newVar(this.cnf_formula.getNumVars());
    try {
      for (int[] clause : this.cnf_formula.getClauses()) {
        //System.out.println("clause: " + Arrays.toString(clause));
        this.solver.addClause(new VecInt(clause));
      }
      this.membership_unit = this.cnf_formula.getNumVars() + 1;
      this.equivalence_one_sink = this.cnf_formula.getNumVars() + 2;
      this.equivalence_zero_sink = this.cnf_formula.getNumVars() + 3;
      this.solver.addClause(new VecInt(new int[]{this.membership_unit}));
      this.solver.addClause(new VecInt(new int[]{this.equivalence_one_sink}));
      this.solver.addClause(new VecInt(new int[]{this.equivalence_zero_sink * -1}));
    } catch (ContradictionException e) {
      System.out.println(e);
    }
  }

  int sign(int x) {
    return ((x > 0) ? 1 : 0) - ((x < 0) ? 1 : 0);
  }

  protected int[] equivalenceQuery(OddAccessStringNode oddas) {
    this.eq_calls += 1;
    long start = System.nanoTime();
    //convert oddas to CNF
    int num_features = this.getNumFeatures();
    CnfFormula alpha = oddas.convertToCnf(this.cnf_formula.getNumVars()+1 +3); //account for hard-coded unit clauses
    this.solver.newVar(alpha.getNumVars());
    
    ArrayList<IConstr> constraints = new ArrayList<IConstr>();
    try {
      int cur_one_sink = 0;
      int cur_zero_sink = 0;
      for (int[] clause : alpha.getClauses()) {
        if (clause.length == 1) {
          if (clause[0] > 0) {
            cur_one_sink = clause[0];
          } else {
            cur_zero_sink = Math.abs(clause[0]);
          }
        }
      }
      for (int[] clause : alpha.getClauses()) {
        for (int i = 0; i < clause.length; i++) {
          if (Math.abs(clause[i]) == cur_one_sink) {
            clause[i] = sign(clause[i]) * equivalence_one_sink;
          } else if (Math.abs(clause[i]) == cur_zero_sink) {
            clause[i] = sign(clause[i]) * equivalence_zero_sink;
          }
        }
        if (clause.length > 1) { // can't add any unit clauses since removing them is not supported by sat4j
          IConstr constr = this.solver.addClause(new VecInt(clause));
          constraints.add(constr);
          if (constr == null) {
            System.out.println("null clause: " + Arrays.toString(clause));
          }
        }
      }
      int odd1 = this.cnf_formula.getNumVars();
      int odd2 = alpha.getNumVars();
      if (odd2 == cur_one_sink || odd2 == cur_zero_sink) {
        odd2 = (odd2 == cur_one_sink)? equivalence_one_sink : equivalence_zero_sink;
      }
      constraints.add(solver.addClause(new VecInt(new int[]{odd1, odd2})));
      constraints.add(solver.addClause(new VecInt(new int[]{-1*odd1, -1*odd2})));
      //System.out.println("clause: " + Arrays.toString(new int[]{odd1, odd2}));

      IProblem problem = this.solver;
      if (problem.isSatisfiable()) {
        int[] model = problem.model();
        for (int i = 0; i < model.length; i++) {
          model[i] = (model[i] > 0) ? 1 : 0;
        }
        //System.out.println(Arrays.toString(model));
        int[] trim_model = Arrays.copyOfRange(model, 0, num_features);
        //System.out.println("counterexample: " + Arrays.toString(trim_model));
        //System.out.println("    " + membershipQueryHelper(trim_model, this.cnf_formula) + " " + membershipQueryHelper(trim_model, alpha));

        long end = System.nanoTime();
        this.eq_time += (end-start);

        //cleanup clauses
        for (int i = 0; i < constraints.size(); i++) {
          for (int j = i+1; i < constraints.size(); i++) {
            if (constraints.get(i) != null && constraints.get(i) == constraints.get(j)) {
              System.out.println("BLASPHEMOUS");
            }
          }
        }
        for (int i = constraints.size()-1; i >= 0; i--) {
          IConstr constraint = constraints.get(i);
          if (constraint != null) {
            if (!this.solver.removeConstr(constraint)) {
              System.out.println("FAILED TO REMOVE CONSTRAINT!\n!\n");
            }
          }
        }

        return Arrays.copyOfRange(model, 0, num_features);
      }
    } catch (ContradictionException e) {
      System.out.println(e);
    } catch (TimeoutException e) {
      System.out.println(e);
    }

    long end = System.nanoTime();
    this.eq_time += (end-start);

    //cleanup clauses
    for (IConstr constraint : constraints) {
      if (constraint != null) {
        this.solver.removeConstr(constraint);
      }
    }
    return null;
  }
  protected int membershipQuery(int[] instance) {
    long start = System.nanoTime();
    //int res = membershipQueryHelper(instance, this.cnf_formula);
    
    
    OddAccessStringNode cur = this.dummy.getChild(0);
    while (cur.getType() == OddAccessStringNode.NodeType.NORMAL) {
      //System.out.println(Arrays.toString(cur.getAccessString()));
      cur = cur.getChild(instance[cur.getLabel()]);
    }
    //System.out.println(Arrays.toString(cur.getAccessString()));
    //System.out.println((cur.getType() == OddAccessStringNode.NodeType.ONE_SINK) ? 1 : 0);
    int res = (cur.getType() == OddAccessStringNode.NodeType.ONE_SINK) ? 1 : 0;
    

    long end = System.nanoTime();
    this.mem_time += (end-start);
    this.mem_calls += 1;
    return res;
  }

  protected int membershipQueryHelper(int[] instance) {
    this.solver.newVar(this.cnf_formula.getNumVars());
    ArrayList<IConstr> constraints = new ArrayList<IConstr>();
    try {
      for (int i = 0; i < instance.length; i++) {
        int evid = (i+1) * (2*instance[i] - 1);
        constraints.add(solver.addClause(new VecInt(new int[]{ evid })));
        //System.out.println("clause: " + evid);
      }
      constraints.add(solver.addClause(new VecInt(new int[]{ this.cnf_formula.getNumVars() })));

      IProblem problem = this.solver;
      if (problem.isSatisfiable()) {
        for (IConstr constraint : constraints) {
          this.solver.removeConstr(constraint);
        }
        return 1;
      }
    } catch (ContradictionException e) {
      System.out.println(e);
    } catch (TimeoutException e) {
      System.out.println(e);
    }

    for (IConstr constraint : constraints) {
      this.solver.removeConstr(constraint);
    }
    return 0;
  }
}


