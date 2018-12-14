package bnncompiler.model;

/** Import standard Java classes. */
import java.util.*;
import java.io.*;

public class OddAccessStringNode {
  public enum NodeType { NORMAL, ONE_SINK, ZERO_SINK }

  OddAccessStringNode[] children;
  int[][] path_strings;
  int label;
  int[] access_string;
  NodeType type;
  long id;
  HashSet<OddAccessStringNode> parents;

  public static long counter = 1;
  private static HashMap<ArrayList<Integer>, OddAccessStringNode> oddas_map = new HashMap<ArrayList<Integer>, OddAccessStringNode>();

  public static OddAccessStringNode getNode(int[] e) {
    return oddas_map.get(toArrayInts(e));
  }

  public static Iterator<OddAccessStringNode> getIterator() {
    return oddas_map.values().iterator();
  }

  private static ArrayList<Integer> toArrayInts(int[] e) {
    ArrayList<Integer> arr = new ArrayList<Integer>();
    for (int i : e) {
      arr.add(i);
    }
    return arr;
  }

  public OddAccessStringNode(int label, int[] access_string, NodeType type) {
    this.children = new OddAccessStringNode[2];
    this.path_strings = new int[2][];
    this.label = label;
    this.access_string = access_string;
    this.type = type;
    this.parents = new HashSet<OddAccessStringNode>();

    this.id = counter;
    this.oddas_map.put(toArrayInts(access_string), this);
    counter += 1;
  }

  @Override
  public int hashCode() {
    return (int)(this.getId());
  }
  @Override
  public boolean equals(Object obj) { // no reduction, since learned ODD is already reduced
    if (obj==null || !(obj instanceof OddAccessStringNode)) {
      return false;
    }
    OddAccessStringNode other = ( OddAccessStringNode ) obj;
    return this.getId() == other.getId();
  }

  public void addParent(OddAccessStringNode parent) {
    this.parents.add(parent);
  }
  public void removeParent(OddAccessStringNode parent) {
    this.parents.remove(parent);
  }

  public Iterator<OddAccessStringNode> getParents() {
    return this.parents.iterator();
  }

  public void setId(int id) {
    this.id = id;
  }

  public long getId() {
    return this.id;
  }

  public void setLabel(int label) {
    this.label = label;
  }

  public int getLabel() {
    return this.label;
  }

  public void setChild(int val, int[] s, OddAccessStringNode child) {
    if (this.children[val] != null) {
      this.children[val].removeParent(this);
    }

    this.children[val] = child;
    this.setPathString(val, s);

    if (this.children[val] != null) {
      this.children[val].addParent(this);
    }
  }

  public void setAccessString(int[] s) {
    this.access_string = s;
  }

  public void setPathString(int val, int[] str) {
    this.path_strings[val] = str;
  }

  public OddAccessStringNode getChild(int val) {
    return this.children[val];
  }

  public int[] getAccessString() {
    return this.access_string;
  }

  public int[] getPathString(int val) {
    return this.path_strings[val];
  }

  public NodeType getType() {
    return this.type;
  }

  public void writeToFile(String filename) {
     File file = new File(filename);
     try {
      PrintWriter writer = new PrintWriter(file);
      HashMap<OddAccessStringNode,String> hm = new HashMap<OddAccessStringNode,String>();
      writeToFileHelper(this, hm, writer);
      writer.close();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  private void writeToFileHelper(OddAccessStringNode root, HashMap<OddAccessStringNode, String> hm, PrintWriter writer) {
    if (hm.containsKey(root)) {
      return;
    }
    if (root.getType() == OddAccessStringNode.NodeType.ONE_SINK || root.getType() == OddAccessStringNode.NodeType.ZERO_SINK) {
      hm.put(root, (root.getType() == OddAccessStringNode.NodeType.ONE_SINK) ? "T" : "F");
    }
    else {
      String cur = String.valueOf(hm.size());
      hm.put(root, cur);
      writeToFileHelper(root.getChild(0), hm, writer);
      writeToFileHelper(root.getChild(1), hm, writer);
      writer.println(cur + " " + hm.get(root.getChild(0)) + " " + hm.get(root.getChild(1))
          + " " + Arrays.toString(root.getAccessString()).replace(" ","")
          + " " + Arrays.toString(root.getPathString(0)).replace(" ","")
          + " " + Arrays.toString(root.getPathString(1)).replace(" ",""));
    }
  }

  public CnfFormula convertToCnf(int start_index) {
    OddAccessStringNode root = this.getChild(0);

    //uses Tseytin transformation to encode OBDD as CNF
    HashMap<OddAccessStringNode, Integer> hm = new HashMap<OddAccessStringNode, Integer>();
    ArrayList<ArrayList<Integer>> arr_list = convertToCnfHelper(root, hm, start_index);

    int num_vars = hm.get(root);
    //arr_list.add(new ArrayList<Integer>(Arrays.asList(num_vars)));

    int[][] arr = new int[arr_list.size()][];
    for (int i = 0; i < arr_list.size(); i++) {
      ArrayList<Integer> row = arr_list.get(i);
      arr[i] = new int[row.size()];
      for (int j = 0; j < row.size(); j++) {
        arr[i][j] = row.get(j);
      } 
    }

    return new CnfFormula(num_vars, arr);
  }

  private ArrayList<ArrayList<Integer>> convertToCnfHelper(OddAccessStringNode root, HashMap<OddAccessStringNode, Integer> hm, int index) {
    ArrayList<ArrayList<Integer>> arr = new ArrayList<ArrayList<Integer>>();
    if (hm.containsKey(root)) {
      return arr;
    }
    if (root.getType() == OddAccessStringNode.NodeType.ONE_SINK) {
      arr.add(new ArrayList<Integer>(Arrays.asList(index)));
      hm.put(root, index);
      return arr;
    }
    if (root.getType() == OddAccessStringNode.NodeType.ZERO_SINK) {
      arr.add(new ArrayList<Integer>(Arrays.asList(-1*index)));
      hm.put(root, index);
      return arr;
    }

    ArrayList<ArrayList<Integer>> arr0 = convertToCnfHelper(root.getChild(0), hm, index);
    index = Math.max(index, hm.get(root.getChild(0)) + 1);
    ArrayList<ArrayList<Integer>> arr1 = convertToCnfHelper(root.getChild(1), hm, index);
    index = Math.max(index, hm.get(root.getChild(1)) + 1);

    arr.addAll(arr0);
    arr.addAll(arr1);

    int r = index, x = root.getLabel() + 1;
    int c0 = hm.get(root.getChild(0)), c1 = hm.get(root.getChild(1));

    //System.out.println("x: " + x);
    arr.add(new ArrayList<Integer>(Arrays.asList(-1*r, c0, x)));
    arr.add(new ArrayList<Integer>(Arrays.asList(-1*r, c1, -1*x)));
    arr.add(new ArrayList<Integer>(Arrays.asList(-1*r, c0, c1)));
    arr.add(new ArrayList<Integer>(Arrays.asList(r, -1*c0, x)));
    arr.add(new ArrayList<Integer>(Arrays.asList(r, -1*c1, -1*x)));    

    hm.put(root, index);
    return arr;
  }

}


