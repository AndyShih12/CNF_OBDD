import math
from cnf import * 

def sequential_counter(l, D, SQ_id):
  ''' l is a list of Term objects. D is a constant. This returns
  a Cnf object with the constraint sum(l) >= D
  '''
  m = len(l)
  r = []
  cnf_clauses = Cnf([])

  if D < 1:
    term = Term(annotation='%s:R0-0' % (SQ_id))
    return (term, Cnf([Clause([term])]))
  elif m < D:
    term = Term(annotation='%s:R0-0' % (SQ_id))
    return (term, Cnf([Clause([term.neg()])]))

  for i in range(m):
    r.append([])
    for j in range(D+1):
      r[i].append(Term(annotation='%s:R%d-%d' % (SQ_id, i, j)))
  
  cnf_clauses += equiv(l[0], r[0][1])
  for j in range(2, D+1):
    cnf_clauses += Cnf([Clause([r[0][j].neg()])])
  for i in range(1,m):
    cnf_clauses += Cnf([
      Clause([r[i][1].neg(), l[i], r[i-1][1]]),
      Clause([r[i][1], l[i].neg()]),
      Clause([r[i][1], r[i-1][1].neg()])])
  
  for i in range(1,m):
    for j in range(2, D+1):
      cnf_clauses += Cnf([
        Clause([r[i][j].neg(), l[i], r[i-1][j]]),
        Clause([r[i][j].neg(), r[i-1][j-1], r[i-1][j]]),
        Clause([r[i][j], l[i].neg(), r[i-1][j-1].neg()]),
        Clause([r[i][j], r[i-1][j].neg()])])

  #print "Sequential counter: %d %d -> %d" % (m, D, len(cnf_clauses.clauses))

  return (r[m-1][D], cnf_clauses)

def internal_layer_to_cnf(x, a, thresh, comp, layer_id):
  si = len(x)
  so = len(a)
  assert( si == len(a[0]) ), 'input lengths do not match!'
  assert( so == len(thresh) ), 'output lengths do not match!'

  output_terms = []
  cnf_clauses = Cnf([])

  for i in range(so):
    l = [None for _ in range(si)]
    sum_a_pos = 0
    sum_a_neg = 0
    for j in range(si):
      if a[i][j] == 1:
        l[j] = x[j]
        sum_a_pos += 1
      else:
        assert( a[i][j] == -1 ), 'invalid input'
        l[j] = x[j].neg()
        sum_a_neg -= 1

    C = thresh[i]
    D = (int)(math.ceil(C/2.0 + (sum_a_pos+sum_a_neg)/2.0)) + abs(sum_a_neg)
    (final_term, clauses) = sequential_counter(l, D, '%s:S%d' % (layer_id, i))

    if comp[i] == "<":
      final_term = final_term.neg()

    output_terms.append(final_term)
    cnf_clauses += clauses

    #print i, len(cnf_clauses.clauses)
  
  return (output_terms, cnf_clauses)

def output_layer_to_cnf(x, a, thresh, comp, layer_id):
  si = len(x)
  so = len(a)
  assert( si == len(a[0]) ), 'input lengths do not match!'
  assert( so == len(thresh) ), 'output lengths do not match!'

  d = [[] for _ in range(so)]
  output_terms = []
  cnf_clauses = Cnf([])

  for i in range(so):
    for j in range(so):
      l = []
      sum_a_i = 0
      sum_a_j = 0
      sum_a_pos = 0
      sum_a_neg = 0
      for k in range(si):
        sum_a_i += a[i][k]
        sum_a_j += a[j][k]
        if a[i][k] == 1 and a[j][k] == -1:
          l.append(x[k])
          sum_a_pos += 1
        elif a[i][k] == -1 and a[j][k] == 1:
          l.append(x[k].neg())
          sum_a_neg -= 1

      bi, bj = -1*thresh[i], -1*thresh[j]
      E = math.ceil( (bj - bi + sum_a_i - sum_a_j) / 2.0 )
      D = (int)(math.ceil(E/2.0)) + abs(sum_a_neg)
      (final_term, clauses) = sequential_counter(l, D, '%s:S%d-%d' % (layer_id, i, j))
      d[i].append(final_term)
      cnf_clauses += clauses
    
    (final_term, clauses) = sequential_counter(d[i], so, '%s:S%d' % (layer_id, i))
    output_terms.append(final_term)
    cnf_clauses += clauses
    
  return (output_terms, cnf_clauses)


def sign(x):
  return 1 if x >= 0 else -1

def read_input(filename):
  with open(filename, 'r') as f:
    layers = (int)(f.readline().strip())
    dim = [[] for _ in range(layers)]
    for i in range(layers):
      line = f.readline().strip().split(' ')
      dim[i] = [(int)(line[0]), (int)(line[1])]
    
    weight = [[[None for _ in range(dim[i][0])] for _ in range(dim[i][1])] for i in range(layers)]
    threshold = [[None for _ in range(dim[i][1])] for i in range(layers)]
    comp = [[None for _ in range(dim[i][1])] for i in range(layers)]

    for i in range(layers):
      for j in range(dim[i][1]):
        for k in range(dim[i][0]):
          weight[i][j][k] = sign((float)(f.readline().strip()))
      for j in range(dim[i][1]):
        line = f.readline().strip().split(' ')
        comp[i][j] = line[0]
        threshold[i][j] = (float)(line[1])
  return (layers, dim, weight, threshold, comp)


def write_output(cnf_array, num_terms, filename):
  with open(filename, 'w') as f:
    f.write('p cnf %d %d\n' % (num_terms, len(cnf_array)))
    for clause in cnf_array:
      f.write(' '.join(map(str,clause)) + ' 0\n')
  print "Wrote cnf to %s" % filename

def main():
  '''
    Outputs a CNF formula representing a BNN
    CNF variables 1 to num_features correspond to variables 1 to num_features in the BNN
    CNF variable num_feature+1 represents the output of the BNN
  '''

  input_str = raw_input("Enter num_hidden and num_features as space separated integers\n").split()
  print input_str 
  num_hidden, num_features = (int)(input_str[0]), (int)(input_str[1])

  input_file = 'Nets/Net8x8_%d_%d.txt' % (num_features, num_hidden)
  output_file = 'Nets/Net8x8_%d_%d.cnf' % (num_features, num_hidden)
  (layers, dim, weight, threshold, comp) = read_input(input_file)

  cnf_clauses = Cnf([])
  input_terms = [Term(annotation='in' + str(i)) for i in range(dim[0][0])]
  output_terms = input_terms
  for i in range(layers):
    if i < layers - 1:
      (output_terms, output_clauses) = internal_layer_to_cnf(output_terms, weight[i], threshold[i], comp[i], 'L' + str(i))
    else:
      (output_terms, output_clauses) = output_layer_to_cnf(output_terms, weight[i], threshold[i], comp[i], 'L' + str(i))  
    cnf_clauses += output_clauses
    print len(output_clauses.clauses)

  #print len(cnf_clauses.clauses)

  s = set()
  d = {}

  for clause in cnf_clauses.clauses:
    for term in clause.terms:
      s.add(abs(term.tid))

  sorted_s = sorted(s)
  for i, tid in enumerate(sorted_s):
    d[tid] = i+1
    
  cnf_array = []
  for clause in cnf_clauses.clauses:
    clause_array = []
    for term in clause.terms:
      clause_array.append(d[abs(term.tid)] * sign(term.tid))
    cnf_array.append(clause_array)
  print len(cnf_array)

  #for t in input_terms:
  #  print d[abs(t.tid)]
  #for t in output_terms:
  #  print d[abs(t.tid)]  

  # swap target term with term (n+1)
  swap0 = dim[0][0] + 1
  swap1 = d[abs(output_terms[0].tid)]
  for i,clause in enumerate(cnf_array):
    for j,term in enumerate(clause):
      if abs(term) == swap0:
        cnf_array[i][j] = swap1 * sign(term)
      elif abs(term) == swap1:
        cnf_array[i][j] = swap0 * sign(term)

  print "swapped %d with %d" % (swap0, swap1)
  write_output(cnf_array, len(s), output_file)

if __name__ == "__main__":
  main()
