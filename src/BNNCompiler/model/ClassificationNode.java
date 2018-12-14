package bnncompiler.model;

import java.util.*;

public class ClassificationNode {
  public enum NodeType { LEAF, SINGLE, TWIN }

  ClassificationNode[] children;
  ClassificationNode last_twin_node;
  ClassificationNode parent;
  int[] d_string;
  NodeType type;
  long id;

  public static long counter = 1;
  private static HashMap<ArrayList<Integer>, ClassificationNode> classification_trees_map = new HashMap<ArrayList<Integer>, ClassificationNode>();

  public static ClassificationNode getNode(int[] e) {
    return classification_trees_map.get(toArrayInts(e));
  }

  private static ArrayList<Integer> toArrayInts(int[] e) {
    ArrayList<Integer> arr = new ArrayList<Integer>();
    for (int i : e) {
      arr.add(i);
    }
    return arr;
  }

  public ClassificationNode(int[] d_string, NodeType type) {
    this.children = new ClassificationNode[2];
    this.d_string = d_string.clone();
    this.type = type;

    this.setLastTwinNode(null);

    this.id = counter;
    if (this.type == NodeType.LEAF) {
      this.classification_trees_map.put(toArrayInts(d_string), this);
    }
    counter += 1;
  }

  @Override
  public int hashCode() {
    return (int)(this.getId());
  }
  @Override
  public boolean equals(Object obj) {
    if (obj==null || !(obj instanceof ClassificationNode)) {
      return false;
    }
    ClassificationNode other = ( ClassificationNode ) obj;
    return this.getId() == other.getId();
  }

  public void setId(int id) {
    this.id = id;
  }

  public long getId() {
    return this.id;
  }

  public void setParent(ClassificationNode parent) {
    this.parent = parent;
  }

  public ClassificationNode getParent() {
    return this.parent;
  }

  public void setChild(int val, ClassificationNode child) {
    this.children[val] = child;
    this.children[val].setParent(this);
    this.children[val].setLastTwinNode(this.getLastTwinNode());
  }

  public ClassificationNode getChild(int index) {
    return this.children[index];
  }

  public void setLastTwinNode(ClassificationNode last_twin_node) {
    this.last_twin_node = (this.getType() == NodeType.TWIN) ? this : last_twin_node;
    
    for (ClassificationNode child : this.children) {
      if (child != null) {
        child.setLastTwinNode(this.last_twin_node);
      }
    }
  }

  public ClassificationNode getLastTwinNode() {
    return this.last_twin_node;
  }

  public void setDString(int[] d_string) {
    this.d_string = d_string;
  }

  public int[] getDString() {
    return this.d_string;
  }

  public NodeType getType() {
    return this.type;
  }
}


