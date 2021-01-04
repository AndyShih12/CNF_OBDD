package bnncompiler.core;

import bnncompiler.model.*;

import java.util.*;

public abstract class LearnOdd {

  ClassificationNode[] classification_trees;
  OddAccessStringNode oddas;
  OddNode odd;
  int num_features;

  long classify_count = 0;
  public long getClassifyCount() { return this.classify_count; }
  long classify_mem_count = 0;
  public long getClassifyMemCount() { return this.classify_mem_count; }

  protected abstract int[] equivalenceQuery(OddAccessStringNode oddas);
  protected abstract int membershipQuery(int[] instance); 

  public LearnOdd(int num_features) {
    this.num_features = num_features;
    this.classification_trees = new ClassificationNode[this.num_features + 1];
  }

  public void run() {
    learnOddas();
    this.oddas.writeToFile("output/myOBDDASz.obddas");
  }

  private void learnOddas() {
    oddas = new OddAccessStringNode(-1,new int[]{},OddAccessStringNode.NodeType.NORMAL);
    oddas.setChild(0, new int[]{}, new OddAccessStringNode(0,new int[]{},OddAccessStringNode.NodeType.ONE_SINK));
    oddas.setChild(1, new int[]{}, new OddAccessStringNode(0,new int[]{},OddAccessStringNode.NodeType.ONE_SINK));
    int[] e0 = equivalenceQuery(oddas);
    if (e0 == null) {
      return;
    }

    oddas = new OddAccessStringNode(-1,new int[]{},OddAccessStringNode.NodeType.NORMAL);
    oddas.setChild(0, new int[]{}, new OddAccessStringNode(0,new int[]{},OddAccessStringNode.NodeType.ZERO_SINK));
    oddas.setChild(1, new int[]{}, new OddAccessStringNode(0,new int[]{},OddAccessStringNode.NodeType.ZERO_SINK));
    int[] e1 = equivalenceQuery(oddas);
    if (e1 == null) {
      return;
    }

    int counter = 0;
    initialHypothesis(e0, e1);
    //System.out.println(Arrays.toString(e0) + " " + Arrays.toString(e1));
    int[] e = equivalenceQuery(oddas);
    while (e != null) {
      //System.out.println("---");
      //System.out.println("e: " + Arrays.toString(e));
      //if (counter % 100 == 0) {
        this.oddas.writeToFile("output/myOBDDAS" + counter + ".obddas");
      //}
      counter += 1;
      updateHypothesis(e);
      e = equivalenceQuery(oddas);
    }
    return;
  }
  
  private void initialHypothesis(int[] e0, int[] e1) {
    int m = this.num_features;
    int low = 1, high = m; // m_q(low-1) = 0, m_q(high) = 1
    System.out.println("membership e0: " + membershipQuery(e0) + " membership e1: " + membershipQuery(e1));
    while (low < high) {
      int mid = (low + high)/2;
      int[] e = cro(e0, e1, mid);
      if (membershipQuery(e) == 1) {
        high = mid;
      }
      else {
        low = mid+1;
      }
    }

    //initialize classification trees
    int i = low;
    //System.out.println("i: " + i);
    for (int j = 0; j <= m; j++) {
      if (j == m-i) {
        this.classification_trees[j] = new ClassificationNode(suf(e1, i), ClassificationNode.NodeType.TWIN);
        this.classification_trees[j].setChild(0,
            new ClassificationNode(getMu(j), ClassificationNode.NodeType.LEAF));
        this.classification_trees[j].setChild(1,
            new ClassificationNode(pre(e0, m-low), ClassificationNode.NodeType.LEAF));
      }
      else if (j == m) {
        this.classification_trees[j] = new ClassificationNode(new int[]{}, ClassificationNode.NodeType.SINGLE);
        this.classification_trees[j].setChild(0,
            new ClassificationNode(cro(e0, e1, i-1), ClassificationNode.NodeType.LEAF));
        this.classification_trees[j].setChild(1,
            new ClassificationNode(cro(e0, e1, i), ClassificationNode.NodeType.LEAF));
      }
      else {
        this.classification_trees[j] = new ClassificationNode(getMu(j), ClassificationNode.NodeType.LEAF);
      }
    }

    //initialize oddas
    int[] e = suf(e1,i);
    //System.out.println("init: " + Arrays.toString(pre(e0, m-i)));
    OddAccessStringNode inner_node = new OddAccessStringNode(m-i, pre(e0, m-i), OddAccessStringNode.NodeType.NORMAL);
    inner_node.setChild(e[0], suf(e1,i), new OddAccessStringNode(m, cro(e0,e1,i), OddAccessStringNode.NodeType.ONE_SINK));
    inner_node.setChild(1-e[0], flip(suf(e1,i)), new OddAccessStringNode(m, cro(e0,e1,i-1), OddAccessStringNode.NodeType.ZERO_SINK));
    
    oddas = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
    oddas.setChild(0, pre(e0, m-i), inner_node);
    oddas.setChild(1, pre(e0, m-i), inner_node);
  }

  private void updateHypothesis(int[] e) {
    int m = this.num_features;
    int t = membershipQuery(e);

    OddAccessStringNode[] p = new OddAccessStringNode[m+2];
    p[0] = this.oddas;
    int k = 1;
    while (p[k-1].getType() == OddAccessStringNode.NodeType.NORMAL) {
      int label = (p[k-1].getLabel() == -1) ? 0 : e[p[k-1].getLabel()];
      p[k] = p[k-1].getChild(label);
      k += 1;
    }

    int low = 0, high = k-2; //mq(low) = t, mq(high+1) = 1-t
    while (low < high) {
      int mid = (low+high+1)/2;
      int[] pas = p[mid].getAccessString();
      if (membershipQuery(concat(pas, suf(e,m-pas.length))) == t) {
        low = mid;
      }
      else {
        high = mid-1;
      }
    }

    int i = low;
    int label = (p[i].getLabel() == -1) ? 0 : e[p[i].getLabel()];
    int[] q = p[i].getPathString(label);
    int[] test = concat(concat(p[i].getAccessString(), q), suf(e,m-p[i+1].getAccessString().length));
    
    //System.out.println("i: " + i + " k: " + k);
    //System.out.println("q: " + Arrays.toString(q));
    //System.out.println("p_i q e_i+1: " + Arrays.toString(test) + " " + evaluateOddas(test));
    if (membershipQuery(test) == t) {
      //System.out.println("NodeSplit");
      nodeSplit(e, t, p[i], p[i+1], q);
    }
    else {
      //System.out.println("NewBranchingNode");
      newBranchingNode(e, t, p[i], p[i+1], q);
    }
  }

  private void nodeSplit(int[] e, int t, OddAccessStringNode p0, OddAccessStringNode p1, int[] q) {
    if (p0 == this.oddas) {
      System.out.println("splitting dummy root");
    }

    // step 1 & 2
    int m = this.num_features;
    int i0 = p0.getAccessString().length, i1 = p1.getAccessString().length;
    int[] e1 = suf(e, m - i1);
    int[] v = concat(p0.getAccessString(), q);
    
    OddAccessStringNode pv = new OddAccessStringNode(v.length, v, OddAccessStringNode.NodeType.NORMAL);
    p0.setChild(q[0], q, pv);
    
    // step 3 & 4
    ClassificationNode tp1 = ClassificationNode.getNode(p1.getAccessString());
    ClassificationNode par = tp1.getParent();
    ClassificationNode t1 = new ClassificationNode(e1, ClassificationNode.NodeType.SINGLE);
    ClassificationNode tv = new ClassificationNode(v, ClassificationNode.NodeType.LEAF);
    //System.out.println("p0: " + Arrays.toString(p0.getAccessString()));
    //System.out.println("p1: " + Arrays.toString(p1.getAccessString()));
    //System.out.println("q: " + Arrays.toString(q));
    //System.out.println(par.getLastTwinNode() + " " + par.getType() + " " + Arrays.toString(par.getDString()));
    if (par == null) {
      this.classification_trees[v.length] = t1;
    }
    else {
      int tp1_index = (Arrays.equals(par.getChild(0).getDString(), p1.getAccessString())) ? 0 : 1;
      par.setChild(tp1_index, t1);
    }    
    t1.setChild(t, tv);
    t1.setChild(1-t, tp1);
 
    // step 5
    // re-route parents of p1 if necessary
    Iterator<OddAccessStringNode> it = p1.getParents();
    ArrayList<OddAccessStringNode> par_array = new ArrayList<OddAccessStringNode>();
    while (it.hasNext()) {
      par_array.add(it.next());
    }
    for (OddAccessStringNode parent : par_array) {
      int p1_index = (parent.getChild(0) == p1) ? 0 : 1;
      int[] test = concat(parent.getAccessString(), parent.getPathString(p1_index));
      //System.out.println(Arrays.toString(parent.getAccessString()) + " " + Arrays.toString(parent.getPathString(p1_index)) + " " + Arrays.toString(p1.getAccessString()));
      //System.out.println(i1 + " " + test.length + " " + Arrays.toString(parent.getPathString(p1_index)));
      int[] result = classify(this.classification_trees[i1], test);
      if (Arrays.equals(result, pv.getAccessString())) {
        parent.setChild(p1_index, parent.getPathString(p1_index), pv);
      }
    }

    // step 6 & 7
    int[] twin = tv.getLastTwinNode().getDString();
    for (int k = 1; k <= twin.length; k++) {
      int[] test = concat(v, pre(twin, k));
      int[] result = classify(this.classification_trees[v.length+k], test);
      if (!Arrays.equals(result, getMu(v.length+k))) {
        pv.setChild(twin[0], pre(twin, k), OddAccessStringNode.getNode(result));
        break;
      }
    }
    twin = flip(twin);
    for (int k = 1; k <= twin.length; k++) {
      int[] test = concat(v, pre(twin, k));
      int[] result = classify(this.classification_trees[v.length+k], test);
      if (!Arrays.equals(result, getMu(v.length+k))) {
        pv.setChild(twin[0], pre(twin, k), OddAccessStringNode.getNode(result));
        break;
      }
    }
  }

  private void newBranchingNode(int[] e, int t, OddAccessStringNode p0, OddAccessStringNode p1, int[] q) {
    // step 1 & 2
    int m = this.num_features;
    int i0 = p0.getAccessString().length, i1 = p1.getAccessString().length;
    int[] e0 = suf(e, m - i0);
    int[] e1 = suf(e, m - i1);
    int[] f = pre(e0, q.length);

    int low = 1, high = q.length; // mq(low-1) = 1-t, mq(high) = t
    while (low < high) {
      int mid = (low + high)/2;
      //System.out.println("q: " + Arrays.toString(q) + " f: " + Arrays.toString(f));
      //System.out.println("x: " + mid + " cro(q,f,x): " + Arrays.toString(cro(q, f, mid)));
      int[] test = concat(concat(p0.getAccessString(), cro(q, f, mid)), e1);
      if (membershipQuery(test) == t) {
        high = mid;
      }
      else {
        low = mid+1;
      }
    }

    // step 3
    int j = low;
    int[] v = concat(p0.getAccessString(), pre(q, q.length - j));
    OddAccessStringNode pv = new OddAccessStringNode(v.length, v, OddAccessStringNode.NodeType.NORMAL);

    // step 4 & 5 & 6 & 7 & 8
    if (j != q.length) {
      if (p0 == this.oddas) { //if p0 is dummy node, both edges need to reroute to v
        p0.setChild(0, pre(q, q.length - j), pv);
        p0.setChild(1, pre(q, q.length - j), pv);
      }
      else {
        p0.setChild(q[0], pre(q, q.length - j), pv);
      }
      pv.setChild(suf(q,j)[0], suf(q, j), p1);
    }
    else { //add new dummy node
      pv = this.oddas;
      pv.setLabel(0);
      this.oddas = new OddAccessStringNode(-1, new int[]{}, OddAccessStringNode.NodeType.NORMAL);
      this.oddas.setChild(0, new int[]{}, pv);
      this.oddas.setChild(1, new int[]{}, pv);
    }

    // step 9 & 10 & 11
    int[] r = suf(e, m - v.length);
    int[] s = r;
    if (t == 0) {
      s = flip(s);
    }

    //System.out.println("pv children: " + Arrays.toString(suf(q,j)) + " " + Arrays.toString(r));
    //System.out.println("p0: " + Arrays.toString(p0.getAccessString()));
    //System.out.println("p1: " + Arrays.toString(p1.getAccessString()));
    //System.out.println("pv: " + Arrays.toString(pv.getAccessString()));

    // step 12 & 13
    ClassificationNode tmu = ClassificationNode.getNode(getMu(v.length));
    ClassificationNode par = tmu.getParent();
    ClassificationNode t1 = new ClassificationNode(s, ClassificationNode.NodeType.TWIN);
    ClassificationNode tv = new ClassificationNode(v, ClassificationNode.NodeType.LEAF);

    //System.out.println("j: " + j);
    //System.out.println("|v|: " + v.length);
    if (par == null) {
      this.classification_trees[v.length] = t1;
    }
    else {
      int mu_index = (Arrays.equals(par.getChild(0).getDString(), getMu(v.length))) ? 0 : 1;
      par.setChild(mu_index, t1);
    }
    t1.setChild(0, tmu);
    t1.setChild(1, tv);

    // step 14
    // re-route oddas edges if necessary
    Iterator<OddAccessStringNode> it = OddAccessStringNode.getIterator();
    ArrayList<OddAccessStringNode> node_array = new ArrayList<OddAccessStringNode>();
    while (it.hasNext()) {
      node_array.add(it.next());
    }
    // Either dummy node or dummy node child may be missing, so must include them explicitly.
    node_array.add(this.oddas);
    node_array.add(this.oddas.getChild(0));
    for (OddAccessStringNode node : node_array) {
      if (node.getLabel() >= pv.getLabel()) {
        continue;
      }
      for (int i = 0; i < 2; i++) {
        if (node.getChild(i) == null || node.getChild(i).getLabel() <= pv.getLabel()) {
          //System.out.println("classify: " + Arrays.toString(node.getAccessString()) + " " + Arrays.toString(node.getChild(i).getAccessString()));
          continue;
        }
        int[] g = pre(node.getPathString(i), pv.getLabel() - node.getLabel());
        int[] test = concat(node.getAccessString(), g);
        int[] result = classify(this.classification_trees[pv.getLabel()], test);
        //System.out.println("classify: " + Arrays.toString(test) + " " + Arrays.toString(result));
        if (Arrays.equals(result, pv.getAccessString())) {
          node.setChild(i, g, pv);
        }
      }
    } 

    // step 15
    for (int k = 1; k <= r.length; k++) {
      int[] test = concat(v, pre(r, k));
      //System.out.println(Arrays.toString(test));
      int[] result = classify(this.classification_trees[v.length+k], test);
      if (!Arrays.equals(result, getMu(v.length+k))) {
        pv.setChild(r[0], pre(r, k), OddAccessStringNode.getNode(result));
        //System.out.println("break: " + Arrays.toString(result) + " " + Arrays.toString(OddAccessStringNode.getNode(result).getAccessString()));
        //System.out.println("break: " + (OddAccessStringNode.getNode(result).getType() == OddAccessStringNode.NodeType.ZERO_SINK));
        break;
      }
    }

  }

  private int[] classify(ClassificationNode classification_tree, int[] e) {
    this.classify_count += 1;
    while (classification_tree.getType() != ClassificationNode.NodeType.LEAF) {
      this.classify_mem_count += 1;
      int[] test = classification_tree.getDString();
      if (classification_tree.getType() == ClassificationNode.NodeType.SINGLE) {
        if (membershipQuery(concat(e, test)) == 1) {
          classification_tree = classification_tree.getChild(1);
        }
        else {
          classification_tree = classification_tree.getChild(0);
        }
      }
      else { // type is TWIN
        int[] testflip = flip(test);
        if (membershipQuery(concat(e, test)) == 1 && membershipQuery(concat(e, testflip)) == 0) {
          classification_tree = classification_tree.getChild(1);
        }
        else {
          classification_tree = classification_tree.getChild(0);
        }
      }
    }
    return classification_tree.getDString();
  }

  protected int evaluateOddas(int[] instance) {
    OddAccessStringNode cur = this.oddas.getChild(0);
    while (cur.getType() == OddAccessStringNode.NodeType.NORMAL) {
      //System.out.println(Arrays.toString(cur.getAccessString()));
      cur = cur.getChild(instance[cur.getLabel()]);
    }
    //System.out.println(Arrays.toString(cur.getAccessString()));
    //System.out.println((cur.getType() == OddAccessStringNode.NodeType.ONE_SINK) ? 1 : 0);
    return (cur.getType() == OddAccessStringNode.NodeType.ONE_SINK) ? 1 : 0;
  }

  private int[] flip(int[] e) {
    int[] flip_e = e.clone();
    flip_e[0] = 1-e[0];
    return flip_e;
  }
  private int[] pre(int[] e, int i) {
    return Arrays.copyOfRange(e, 0, i);
  }
  private int[] suf(int[] e, int i) {
    int m = e.length;
    return Arrays.copyOfRange(e, m-i, m);
  }
  private int[] concat(int[] e0, int[] e1) {
    int[] res = Arrays.copyOf(e0, e0.length + e1.length);
    System.arraycopy(e1, 0, res, e0.length, e1.length);
    return res;
  }
  private int[] cro(int[] e0, int[] e1, int i) {
    int m = e0.length;
    return concat(pre(e0,m-i), suf(e1,i));
  }

  private int[] getMu(int level) {
    return new int[]{-1*(level+2)};
  }

  public int getNumFeatures() {
    return num_features;
  }
}


