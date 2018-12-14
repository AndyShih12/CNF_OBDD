package bnncompiler.core;

import bnncompiler.model.*;

import java.io.*;
import java.util.*;

public class LearnOddFromCnfWithGuessing extends LearnOdd {

  CnfFormula cnf_formula;
  int cnf_output_term;
  int cnf_num_terms;

  long eq_time;
  long mem_time;
  int eq_calls;
  int mem_calls;

  int[][][] weight;
  double[][] bias;
  int target_class;

  long guess_constant = 0L;

  public static void main(String args[]) {
    LearnOddFromCnfWithGuessing T = new LearnOddFromCnfWithGuessing(24);
  }

  public LearnOddFromCnfWithGuessing(int num_features) {
    super(num_features);

    //String filename = "/space/andyshih2/bnn/BinaryNet/Net8x8_" + String.valueOf(num_features) + "_preprocessed.cnf";
    String filename = "/space/andyshih2/bnn/BinaryNet/Net8x8_" + String.valueOf(num_features) + "_binary_preprocessed.cnf";
    String parameters_filename = "/space/andyshih2/bnn/BinaryNet/Net8x8_" + String.valueOf(num_features) + "_binary.txt";
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
      this.bias = new double[layers][];
      for (int i = 0; i < layers; i++) {
        line = sc.nextLine();
        sc_line = new Scanner(line);
        System.out.println(line);
        int in = sc_line.nextInt();
        int out = sc_line.nextInt();
        this.weight[i] = new int[out][];
        this.bias[i] = new double[out];
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
        for (int j = 0; j < this.bias[i].length; j++) {
          line = sc.nextLine();
          sc_line = new Scanner(line);
          this.bias[i][j] = sc_line.nextDouble();
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

  protected int[] equivalenceQueryGuess(OddAccessStringNode oddas, long guess) {
    int num_features = this.getNumFeatures();
    int[] instance = new int[num_features];
    Random rand = new Random();
    for (int i = 0; i < num_features; i++) {
      instance[i] = rand.nextInt(2);
      //instance[i] = (guess & (1L << i)) > 0 ? 1 : 0;
    }
    //System.out.println(Arrays.toString(instance));

    OddAccessStringNode cur = oddas.getChild(0);
    while (cur.getType() == OddAccessStringNode.NodeType.NORMAL) {
      //System.out.println(Arrays.toString(cur.getAccessString()));
      cur = cur.getChild(instance[cur.getLabel()]);
    }
    int oddas_res = (cur.getType() == OddAccessStringNode.NodeType.ONE_SINK) ? 1 : 0;
    int cnf_res = membershipQuery(instance);

    if (this.eq_calls < 5) {
      return (oddas_res != cnf_res) ? instance : null;
    } else {
      return (oddas_res == 0 && cnf_res == 1) ? instance : null;
    }
  }

  protected int[] equivalenceQuery(OddAccessStringNode oddas) {
    this.eq_calls += 1;
    long start = System.nanoTime();

    int num_features = this.getNumFeatures();
    
    if (this.eq_calls < 1000000000) {
      long counter = 0L;
      while (counter < (1L << num_features)) {
        int[] res = equivalenceQueryGuess(oddas, this.guess_constant);
        if (res != null) {
          if (this.eq_calls % 100 == 0) {
            System.out.println("Eq calls: " + eq_calls + " Eq time: " + eq_time/1000000000. + " Time/Call: " + eq_time/(eq_calls*1000000000.) + " guesses: " + counter);
          }
          long end = System.nanoTime();
          this.eq_time += (end-start);
          return res;
        }
        counter += 1;
        this.guess_constant = (this.guess_constant + 1) % (1L << num_features);
      }
      System.out.println("guess failed");
      return null;
    }
    
    //convert oddas to CNF
    CnfFormula alpha = oddas.convertToCnf(this.cnf_formula.getNumVars()+1);
    
    int num_vars = alpha.getNumVars();
    int num_clauses = this.cnf_formula.getClauses().length + alpha.getClauses().length + 2;

    int[] model = null;
    String cmd = "./riss_script";
    try {
      ProcessBuilder prbuilder = new ProcessBuilder(cmd);
      Process pr = prbuilder.start();
      Writer w = new java.io.OutputStreamWriter(pr.getOutputStream());
      w.append(String.format("p cnf %d %d\n", num_vars, num_clauses));
      for (int[] clause: this.cnf_formula.getClauses()) {
        for (int term : clause) {
          w.append(String.valueOf(term) + " ");
        }
        w.append("0\n");
      }
      for (int[] clause: alpha.getClauses()) {
        for (int term : clause) {
          w.append(String.valueOf(term) + " ");
        }
        w.append("0\n");
      }
      int odd1 = this.cnf_output_term;
      int odd2 = alpha.getNumVars();
      //w.append(String.format("%d %d 0\n", odd1, odd2));
      //w.append(String.format("%d %d 0\n", -1*odd1, -1*odd2));
      w.append(String.format("%d 0\n", odd1));
      w.append(String.format("%d 0\n", -1*odd2));
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

    if (eq_calls % 1 == 0) {
      System.out.println("Eq calls: " + eq_calls + " Eq time: " + eq_time/1000000000. + " Time/Call: " + eq_time/(eq_calls*1000000000.));
    }
    long end = System.nanoTime();
    this.eq_time += (end-start);
    return model;
  }

  protected int membershipQuery(int[] instance) {
    this.mem_calls += 1;
    long start = System.nanoTime();
    int res = membershipQueryBNN(instance);
    long end = System.nanoTime();
    this.mem_time += (end-start);
    
    if (mem_calls % 1000000 == 0) {
      System.out.println("Mem calls: " + mem_calls + " Mem time: " + mem_time/1000000000. + " Time/Call: " + mem_time/(mem_calls*1000000000.) + " call/classify: " + 1.0*this.getClassifyMemCount()/this.getClassifyCount() + " classify_count: " + this.getClassifyCount());
    }

    return res;
  }

  private int membershipQueryBNN(int[] instance) {
    double[] cur_layer = new double[instance.length];
    double[] next_layer = null;
 
    for (int i = 0; i < instance.length; i++) {
      cur_layer[i] = instance[i]-0.5;
    }

    for (int i = 0; i < this.weight.length; i++) {
      next_layer = new double[this.weight[i].length];
      for (int j = 0; j < cur_layer.length; j++) {
        cur_layer[j] = (cur_layer[j] >= 0) ? 1.0 : -1.0;
      }
      for (int j = 0; j < this.weight[i].length; j++) {
        next_layer[j] = this.bias[i][j];
        for (int k = 0; k < this.weight[i][j].length; k++) {
          next_layer[j] += this.weight[i][j][k] * cur_layer[k];
        }
      }
      cur_layer = next_layer.clone();
    }
    
    for (int i = 0; i < next_layer.length; i++) {
      //System.out.println(next_layer[i]);
      if (next_layer[i] > next_layer[this.target_class]) {
        return 0;
      }
    }
    return 1;
  }

}


