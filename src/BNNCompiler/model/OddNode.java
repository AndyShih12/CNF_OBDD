package bnncompiler.model;

public class OddNode {
  public enum NodeType { NORMAL, ONE_SINK, ZERO_SINK }

  OddNode[] children;
  int label;
  NodeType type;
  long id;

  public static long counter = 1;

  public OddNode(int label, NodeType type) {
    this.children = new OddNode[2];
    this.label = label;
    this.type = type;

    this.id = counter;
    counter += 1;
  }

  @Override
  public int hashCode() {
    return (int)(this.getId());
  }
  @Override
  public boolean equals(Object obj) { // no reduction, since learned ODD is already reduced
    if (obj==null || !(obj instanceof OddNode)) {
      return false;
    }
    OddNode other = ( OddNode ) obj;
    return this.getId() == other.getId();
  }

  public void setId(int id) {
    this.id = id;
  }

  public long getId() {
    return this.id;
  }

  public void setChild(int val, OddNode child) {
    this.children[val] = child;
  }

  public OddNode getChild(int index) {
    return this.children[index];
  }

  public NodeType getType() {
    return this.type;
  }
}


