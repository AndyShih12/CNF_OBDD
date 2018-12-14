#!/usr/bin/env python

import sdd
import glob

obddas_dir = "/space/andyshih2/BNN_ODD/output/"
num_features = 64

CONSTRAINT_OBDD = True 

def convert_helper(node, mgr, obddas, dp, depth):
  if node == 'T':
    return sdd.sdd_manager_true(mgr)
  if node == 'F':
    return sdd.sdd_manager_false(mgr)
  if node in dp:
    return dp[node]

  ch0, ch1, var = obddas[node][0], obddas[node][1], obddas[node][2] + 1

  if CONSTRAINT_OBDD:
    var = depth + 1

  alpha = sdd.sdd_conjoin(convert_helper(ch0, mgr, obddas, dp, depth+1), sdd.sdd_manager_literal(-1*var,mgr), mgr)
  beta = sdd.sdd_conjoin(convert_helper(ch1, mgr, obddas, dp, depth+1), sdd.sdd_manager_literal(var,mgr), mgr)

  dp[node] = sdd.sdd_disjoin(alpha, beta, mgr)
  return dp[node]

def convert_obddas_to_sdd(filename):
  with open(filename,'r') as f:
    nodes = f.readlines()
  
  nodes = [x.strip().split(' ') for x in nodes]
  nodes = [[int(x) if x.isdigit() else x for x in node[0:3]] + [node[3].count('0') + node[3].count('1')] for node in nodes]  

  node_dict = {}
  for l in nodes:
    node_dict[l[0]] = l[1:]

  #print node_dict

  vtree = sdd.sdd_vtree_new(num_features,"right")
  mgr = sdd.sdd_manager_new(vtree)
  vtree = sdd.sdd_manager_vtree(mgr)

  root = 1
  if root not in node_dict: root = 0
  return convert_helper(root,mgr,node_dict,{},0), vtree, mgr

def main():
  obddas_files = glob.glob(obddas_dir + "*.obddas")

  if CONSTRAINT_OBDD:
    obddas_files = [x for x in obddas_files if "constraint" in x]
  else:
    obddas_files = [x for x in obddas_files if "constraint" not in x]

  for f in obddas_files:
    basename = '.'.join(f.split('.')[:-1])
    print basename
    alpha, vtree, mgr = convert_obddas_to_sdd(f)
    sdd.sdd_save(basename + ".sdd", alpha)
    sdd.sdd_vtree_save(basename + ".vtree", vtree)

if __name__== "__main__":
  main()
