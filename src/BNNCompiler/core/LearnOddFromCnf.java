package bnncompiler.core;

import bnncompiler.model.*;

import java.io.*;
import java.util.*;

public class LearnOddFromCnf extends LearnOdd {

  CnfFormula cnf_formula;
  int cnf_output_term;
  int cnf_num_terms;

  long eq_time;
  long mem_time;
  int eq_calls;
  int mem_calls;

  int[][][] weight;
  double[][] thresh;
  String[][] comp;
  int target_class;

  public static void main(String args[]) {
    LearnOddFromCnf T = new LearnOddFromCnf(64);
  }

  public LearnOddFromCnf(int num_features) {
    super(num_features);

    int num_hidden = 5;
    String filename = "/space/andyshih2/bnn/BinaryNet/Nets/Net8x8_" + String.valueOf(num_features) + "_" + String.valueOf(num_hidden) + "_pp.cnf";
    String parameters_filename = "/space/andyshih2/bnn/BinaryNet/Nets/Net8x8_" + String.valueOf(num_features) + "_" + String.valueOf(num_hidden) + ".txt";
    this.target_class = 0;

    try {
      Scanner sc = new Scanner(new File(filename));

      String first_line = sc.nextLine();
      Scanner sc_line = new Scanner(first_line);
      String throwaway = sc_line.next();
      throwaway = sc_line.next();
      int num_terms = sc_line.nextInt(); // maximum index of the cnf terms
      int num_clauses = sc_line.nextInt();

      int[][] cnf_array = new int[num_clauses][];
  
      int line_num = 0;
      ArrayList<Integer> array = new ArrayList<Integer>(); 
      while (sc.hasNextLine()) {
        String line = sc.nextLine();
        if (line.charAt(0) == 'c') {
          continue;
        }
        sc_line = new Scanner(line);
        while (sc_line.hasNextInt()) {
          int x = sc_line.nextInt();
          if (x == 0) {
            cnf_array[line_num] = new int[array.size()];
            for (int i = 0 ; i < array.size(); i++) {
              cnf_array[line_num][i] = array.get(i);
            }
            line_num++;
            array.clear();
          }
          else {
            array.add(x);
          }
        }
      }

      this.cnf_formula = new CnfFormula(num_terms, cnf_array);
      this.cnf_output_term = num_features + 1; // make sure this is compatible with the cnf encoding
      this.cnf_num_terms = num_terms;


      // read parameters
      sc = new Scanner(new File(parameters_filename));
      String line = sc.nextLine();
      sc_line = new Scanner(line);
      int layers = sc_line.nextInt();
      this.weight = new int[layers][][];
      this.thresh = new double[layers][];
      this.comp = new String[layers][];
      for (int i = 0; i < layers; i++) {
        line = sc.nextLine();
        sc_line = new Scanner(line);
        System.out.println(line);
        int in = sc_line.nextInt();
        int out = sc_line.nextInt();
        this.weight[i] = new int[out][];
        this.thresh[i] = new double[out];
        this.comp[i] = new String[out];
        for (int j = 0; j < out; j++) {
          this.weight[i][j] = new int[in];
        }
      }
      for (int i = 0; i < layers; i++) {
        for (int j = 0; j < this.weight[i].length; j++) {
          for (int k = 0; k < this.weight[i][j].length; k++) {
            line = sc.nextLine();
            sc_line = new Scanner(line);
            double d = sc_line.nextDouble();
            this.weight[i][j][k] = (d >= 0) ? 1 : -1;
          }
        }
        for (int j = 0; j < this.thresh[i].length; j++) {
          line = sc.nextLine();
          sc_line = new Scanner(line);
          this.comp[i][j] = sc_line.next();
          this.thresh[i][j] = sc_line.nextDouble();
        }
      }
      System.out.println("Done reading");

      this.run();
    } catch (FileNotFoundException e) {
      System.out.println(e);
    }
    
    System.out.println(this.cnf_formula.getClauses().length);
    System.out.println("Eq calls: " + eq_calls + " Eq time: " + eq_time/1000000000. + " Time/Call: " + eq_time/(eq_calls*1000000000.));
    System.out.println("Mem calls: " + mem_calls + " Mem time: " + mem_time/1000000000. + " Time/Call: " + mem_time/(mem_calls*1000000000.));
  }


  protected int[] equivalenceQuery(OddAccessStringNode oddas) {
    OddAccessStringNode care_set = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
    care_set.setChild(0, new int[]{}, new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.ONE_SINK));
    care_set.setChild(1, new int[]{}, new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.ONE_SINK));

    care_set = kAwayFromInst(5);

    return equivalenceQuery(oddas, care_set);
  }

  private OddAccessStringNode kAwayFromInst(int k) {
    int num_features = this.getNumFeatures();
    int inst[] = new int[]{

      
      0, 0, 1, 1, 1, 1, 0, 0,
      0, 1, 1, 1, 1, 1, 1, 0,
      1, 1, 0, 0, 0, 0, 1, 1,
      1, 1, 0, 0, 0, 0, 1, 1,
      1, 1, 0, 0, 0, 0, 1, 1,
      1, 1, 0, 0, 0, 0, 1, 1,
      0, 1, 1, 1, 1, 1, 1, 0,
      0, 0, 1, 1, 1, 1, 0, 0
      
      /*0, 0, 1, 1, 1, 1, 0, 0,
      0, 1, 1, 1, 1, 1, 1, 0,
      0, 1, 0, 0, 0, 0, 1, 0,
      0, 0, 1, 1, 1, 1, 0, 0,
      0, 0, 1, 1, 1, 1, 0, 0,
      0, 1, 0, 0, 0, 0, 1, 0,
      0, 1, 1, 1, 1, 1, 1, 0,
      0, 0, 1, 1, 1, 1, 0, 0*/

      /*
      0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 1, 0, 0, 1, 0, 0,
      0, 0, 1, 0, 0, 1, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0,
      0, 1, 0, 0, 0, 0, 1, 0,
      0, 1, 1, 0, 0, 1, 1, 0,
      0, 0, 1, 1, 1, 1, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0*/
    };

    OddAccessStringNode dummy = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
    OddAccessStringNode root = new OddAccessStringNode(0, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
    OddAccessStringNode one_sink = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.ONE_SINK);
    OddAccessStringNode zero_sink = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.ZERO_SINK);
    OddAccessStringNode nodes[][] = new OddAccessStringNode[num_features][];

    k += 1;

    for (int i = 1; i < num_features; i++) {
      nodes[i] = new OddAccessStringNode[k];
      for (int j = 0; j < k; j++) {
        nodes[i][j] = new OddAccessStringNode(i, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
      }
      for (int j = 0; j < k; j++) {
        if (i > 1) {
          nodes[i-1][j].setChild(inst[i-1], new int[]{}, nodes[i][j]);
          nodes[i-1][j].setChild(1-inst[i-1], new int[]{}, (j+1 < k) ? nodes[i][j+1] : zero_sink);
        }
      }
    }

    root.setChild(inst[0], new int[]{}, nodes[1][0]);
    root.setChild(1-inst[0], new int[]{}, nodes[1][1]);
    
    for (int j = 0; j < k; j++) {
      nodes[num_features-1][j].setChild(inst[num_features-1], new int[]{}, one_sink);
      nodes[num_features-1][j].setChild(1-inst[num_features-1], new int[]{}, (j+1 < k) ? one_sink : zero_sink);
    }

    dummy.setChild(0, new int[]{}, root);
    dummy.setChild(1, new int[]{}, root);

    dummy.writeToFile("./constraint.obddas");

    return dummy;
  }

  protected int[] equivalenceQuery(OddAccessStringNode oddas, OddAccessStringNode care_set) {
    this.eq_calls += 1;
    long start = System.nanoTime();
    int num_features = this.getNumFeatures();
    
    //convert oddas to CNF
    CnfFormula alpha = oddas.convertToCnf(this.cnf_formula.getNumVars()+1);

    //encode care_set constraint
    CnfFormula beta = care_set.convertToCnf(alpha.getNumVars()+1);
    
    int num_vars = beta.getNumVars();
    int num_clauses = this.cnf_formula.getClauses().length + alpha.getClauses().length + beta.getClauses().length + 3;

    int[] model = null;
    String cmd = "./riss_binary/riss_script";

    //System.out.println("Run riss command");
    try {
      ProcessBuilder prbuilder = new ProcessBuilder(cmd);
      prbuilder.redirectErrorStream(true);
      Process pr = prbuilder.start();
      Writer w = new java.io.OutputStreamWriter(pr.getOutputStream());
      w.append(String.format("p cnf %d %d\n", num_vars, num_clauses));
      //---System.out.println(String.format("p cnf %d %d", num_vars, num_clauses));

      int cnt_flush = 0;

      // add cnf, alpha, beta
      CnfFormula formulas[] = new CnfFormula[]{ this.cnf_formula, alpha, beta };
      for (CnfFormula formula : formulas) {
        for (int[] clause: formula.getClauses()) {
          for (int term : clause) { 
            w.append(String.valueOf(term) + " ");
            //---System.out.print(term + " " );
          }
          w.append("0\n");
          //---System.out.println("0");
          w.flush(); cnt_flush+=1;
        }
      }


      int odd1 = this.cnf_output_term;
      int odd2 = alpha.getNumVars();
      int odd3 = beta.getNumVars();

      //System.out.println(odd1 + " " + odd2 + " " + odd3);

      w.append(String.format("%d %d 0\n", odd1, odd2));
      w.append(String.format("%d %d 0\n", -1*odd1, -1*odd2));
      w.append(String.format("%d 0\n", odd3)); // enforce care_set
      w.flush(); cnt_flush+=1;
      w.close();

      //---System.out.println(String.format("%d %d 0", odd1, odd2));
      //---System.out.println(String.format("%d %d 0", -1*odd1, -1*odd2));
      //---System.out.println(String.format("%d 0", odd3));

      System.out.println(alpha.getNumVars());
      //System.out.println("num_clauses: " + num_clauses + " num_flushes: " + cnt_flush);
      //System.out.println("Wait for riss");

      try {
        pr.waitFor();
      } catch (InterruptedException e) {
        System.out.println(e);
      }
      //System.out.println("Done waiting");
      BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));

      String line = "";
      StringBuilder sb = new StringBuilder();
      while ((line = buf.readLine()) != null) {
        if (line.charAt(0) == 'v')
          sb.append(line);
      }
      
      if (sb.length() != 0) {
        line = sb.toString();
        Scanner sc = new Scanner(line);
        String dummy = sc.next();
        model = new int[num_features];
        for (int i = 0; i < num_features; i++) {
          model[i] = sc.nextInt();
          model[i] = (model[i] > 0) ? 1 : 0;
        }
      }
    } catch (IOException e) {
      System.out.println(e);
    }



    long end = System.nanoTime();
    if (eq_calls % 1 == 0) {
      System.out.println("Eq calls: " + eq_calls + " Eq time: " + eq_time/1000000000. + " Time/Call: " + (end-start)/1000000000.);
    }
    this.eq_time += (end-start);

    //System.out.println(Arrays.toString(model));
    //System.out.println(super.evaluateOddas(model) + " " + membershipQueryBNN(model));

    return model;
  }

  protected int membershipQuery(int[] instance) {
    this.mem_calls += 1;
    long start = System.nanoTime();
    int res = membershipQueryBNN(instance);
    long end = System.nanoTime();
    this.mem_time += (end-start);
    
    if (mem_calls % 100000 == 0) {
      System.out.println("Mem calls: " + mem_calls + " Mem time: " + mem_time/1000000000. + " Time/Call: " + mem_time/(mem_calls*1000000000.));
    }

    return res;
  }

  private int membershipQueryBNN(int[] instance) {
    double[] cur_layer = new double[instance.length];
    double[] next_layer = null;
 
    for (int i = 0; i < instance.length; i++) {
      cur_layer[i] = 2*instance[i] - 1;
    }

    for (int i = 0; i < this.weight.length; i++) {
      next_layer = new double[this.weight[i].length];
      for (int j = 0; j < this.weight[i].length; j++) {
        for (int k = 0; k < this.weight[i][j].length; k++) {
          next_layer[j] += this.weight[i][j][k] * cur_layer[k];
        }
      }
      for (int j = 0; j < next_layer.length; j++) {
        if (i < this.weight.length-1) {
          if (this.comp[i][j].equals(">=")) {
            next_layer[j] = (next_layer[j] >= this.thresh[i][j]) ? 1.0 : -1.0;
          }
          else {
            next_layer[j] = (next_layer[j] < this.thresh[i][j]) ? 1.0 : -1.0;
          }
        }
        else {
          next_layer[j] -= this.thresh[i][j];
        }
      }
      //System.out.println(Arrays.toString(cur_layer));
      cur_layer = next_layer.clone();
    }
    //System.out.println(Arrays.toString(next_layer));
   
    //System.out.println(Arrays.toString(next_layer)); 
    for (int i = 0; i < next_layer.length; i++) {
      if (next_layer[i] > next_layer[this.target_class]) {
        return 0;
      }
    }
    return 1;
  }

}


