package bnncompiler.core;

import bnncompiler.model.*;

import java.io.*;
import java.util.*;

public class Brute {

  class NodeKey {
    long ch0, ch1;
    NodeKey(long x, long y) {this.ch0 = x; this.ch1 = y;}
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof NodeKey)) {
        return false;
      }
      NodeKey other = (NodeKey) o;
      return other.ch0 == this.ch0 && other.ch1 == this.ch1;
    }
    @Override
    public int hashCode() {
      return (int)(ch0+ch1);
    }
  }

  class BigArray {
    long[][] array;
    long size;
    int split = 28;
    BigArray(long size) {
      this.array = new long[(int)(size >> split) + 1][];
      for (int i = 0; i < this.array.length; i++) {
        this.array[i] = new long[(1 << split)];
      }
      this.size = size;
    }
    public void set(long index, long val) {
      this.array[(int)(index >> split)][(int)(index % (1L << split))] = val;
    }
    public long get(long index) {
      return this.array[(int)(index >> split)][(int)(index % (1L << split))];
    }
  }

  int[][][] weight;
  double[][] thresh;
  String[][] comp;
  int target_class;
  int num_features;

  public static void main(String args[]) {
    Brute T = new Brute(24);
  }

  public Brute(int num_features) {
    this.num_features = num_features;
    String parameters_filename = "/space/andyshih2/bnn/BinaryNet/Nets/Net8x8_" + String.valueOf(num_features) + "_5.txt";
    this.target_class = 0;

    try {
      // read parameters
      Scanner sc = new Scanner(new File(parameters_filename));
      String line = sc.nextLine();
      Scanner sc_line = new Scanner(line);
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

    } catch (FileNotFoundException e) {
      System.out.println(e);
    }
    this.brute();
  }

  private void brute() {
    int n = this.num_features;
    
    BigArray array0 = new BigArray(1L << n);
    BigArray array1 = new BigArray(1L << n);
    BigArray truth_table = new BigArray(1L << n);
    BigArray parent = new BigArray(1L << n);    

    for (long i = 0; i < (1L << n); i++) {
      if (i % 10000000 == 0) System.out.println(i);
      int[] instance = new int[n];
      for (int j = 0; j < n; j++) {
        instance[j] = (i & (1L << j)) > 0L ? 1 : 0;
      }
      truth_table.set(i, membershipQuery(instance));
    }

    for (long i = (1L << n)-1; i >= 1; i--) {
      if (i % 10000000 == 0) System.out.println(i);
      parent.set(i, i);
      array0.set(i, i*2);
      array1.set(i, i*2 + 1);

      if (array0.get(i) >= (1L << n)) {
        array0.set(i, truth_table.get( array0.get(i) - (1L << n) ) > 0L ? -1L : -2L);
      }      
      if (array1.get(i) >= (1L << n)) {
        array1.set(i, truth_table.get( array1.get(i) - (1L << n) ) > 0L ? -1L : -2L);
      }
    }

    HashMap<NodeKey, Long> hm = new HashMap<NodeKey, Long>();
    for (long i = (1L << n)-1; i >= 1; i--) {
      if (i % 10000000 == 0) System.out.println(i);
      long ch0 = array0.get(i);
      long ch1 = array1.get(i);
      long ch[] = new long[] {
        ch0 < 0L ? ch0 : parent.get(ch0),
        ch1 < 0L ? ch1 : parent.get(ch1)
      };
      NodeKey key = new NodeKey(ch[0], ch[1]);
      if (hm.containsKey(key)) {
        parent.set(i, parent.get(hm.get(key)));
      }
      else {
        hm.put(key, i);
      }
    }
    System.out.println("HASHMAP SIZE: " + hm.size());
  }

  private int membershipQuery(int[] instance) {
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
      cur_layer = next_layer.clone();
    }

    //System.out.println(Arrays.toString(next_layer)); 
    for (int i = 0; i < next_layer.length; i++) {
      if (next_layer[i] > next_layer[this.target_class]) {
        return 0;
      }
    }
    return 1;
  }
}


