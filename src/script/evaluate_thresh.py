class Node(object):
  def __init__(self, node_id, children, comparison, threshold):
    self.node_id = node_id
    self.children = children
    self.comparison = comparison
    self.threshold = threshold

class Network(object):
  def __init__(self, node_dict, input_nodes, output_nodes):
    self.node_dict = node_dict
    self.input_nodes = input_nodes
    self.output_nodes = output_nodes

def readBNN(input_file):
  with open(input_file, 'r') as f:
    num_nodes = (int)(f.readline().strip())
    node_dict = {}
    input_nodes = []
    non_output_nodes = set()

    for i in xrange(num_nodes):
      line = f.readline().strip().split(' ')
      node_id = int(line[0])
      num_children = int(line[1])
      
      if not num_children:
        input_nodes.append(node_id)
        node_dict[node_id] = Node(node_id, None, None, None)
        continue

      children = [int(x) for x in line[2:2+num_children]]
      comparison = line[-2]
      #comparison = line[-7]
      threshold = float(line[-1])
      #threshold = [float(x) for x in line[-5:]]

      non_output_nodes |= set([abs(x) for x in children])
      #print node_id, num_children, children, comparison, threshold
      node_dict[node_id] = Node(node_id, children, comparison, threshold)

  output_nodes = list( set(node_dict.keys()) - non_output_nodes )
  return Network(node_dict, input_nodes, output_nodes)

def sign(x):
  if x == 0:
    return 0
  return 1 if x > 0 else -1

def evaluateBNN(network, instance):
  '''
    evaluates a threshold circuit
  '''

  evaluation = instance.copy()

  for i in sorted(network.node_dict.keys()):
    if i in evaluation:
      evaluation[i] = 2*evaluation[i] - 1
      continue

    node = network.node_dict[i]
    activation = 0
    for ch in node.children:
      #print evaluation[abs(ch)], sign(ch), activation
      activation += evaluation[abs(ch)] * sign(ch)
    
    
    if i in network.output_nodes:
      evaluation[i] = activation - node.threshold
    else: # binarize
      if node.comparison == ">=":
        evaluation[i] = 2*int(activation >= node.threshold) - 1
      elif node.comparison == "<":
        evaluation[i] = 2*int(activation < node.threshold) - 1
      else:
        raise Exception("Unsupported operation: %s", node.comparison)
    
    ''' 
    activation += node.threshold[4]
    if i in network.output_nodes:
      evaluation[i] = activation
    else: # binarize
      if node.comparison == ">=":
        activation = ((activation - node.threshold[3])/node.threshold[0])*node.threshold[1] + node.threshold[2]
        evaluation[i] = 1 if activation >= 0 else -1
    '''
  #print evaluation
  #print [evaluation[x] for x in network.output_nodes]
  # return true if class is 0
  return evaluation[network.output_nodes[0]] > evaluation[network.output_nodes[1]]


def readOBDD(input_file):
  with open(input_file,'r') as f:
    lines = f.readlines()

  nodes = [line.strip().split(' ')[0:4] for line in lines]
  nodes = [[int(x) if x.isdigit() else x for x in node[0:3]] + [node[3].count('0') + node[3].count('1')] for node in nodes]

  nodes.sort()
  nodes = nodes[::-1]

  node_dict = {}
  for i,n in enumerate(nodes):
  #  if i == 0 or nodes[i][0] != nodes[i-1][0]+1:
  #    print nodes[i]
    node_dict[n[0]] = n[1:]
  return node_dict

def evaluateOBDD(obdd, instance):

  instance = instance.copy()
  # 1-indexed instance
  for i in xrange(len(instance)):
    instance[i] = instance[i+1]
  cur = 1
  
  while cur != 'T' and cur != 'F':
    #print cur
    node = obdd[cur]
    if instance[node[-1]] == 0:
      cur = node[0]
    else:
      cur = node[1]

  return cur == 'T'


test_files = {
  'data/8x8/test/digit-0.data' : 1,
#  'data/8x8/test/digit-1.data' : 0,
#  'data/8x8/test/digit-2.data' : 0,
#  'data/8x8/test/digit-3.data' : 0,
#  'data/8x8/test/digit-4.data' : 0,
#  'data/8x8/test/digit-5.data' : 0,
#  'data/8x8/test/digit-6.data' : 0,
#  'data/8x8/test/digit-7.data' : 0,
  'data/8x8/test/digit-8.data' : 0,
#  'data/8x8/test/digit-9.data' : 0
}

def test(network):
  #y = [0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0]
  #y = [0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
  #y = [1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1] # class0
  #y = [0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1] # notclass0
  #y = [1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1] # class0

  #print evaluateBNN(network, {x+1 : 2*y[x]-1 for x in xrange(len(network.input_nodes))})
  
  #import sys
  #sys.exit()

  confusion_matrix = [[0 for _ in xrange(2)] for _ in xrange(2)]
  for ff,k in test_files.iteritems():
    with open(ff,'r') as f:
      lines = f.readlines()
      instances = [[int(x) for i,x in enumerate(line[2:-2]) if i%2 == 0] for line in lines]
      
      for i,instance in enumerate(instances):
        out = evaluateBNN(network, {x+1 : 2*instance[x]-1 for x in xrange(len(network.input_nodes))})
        #print out, k
        confusion_matrix[k][ out ] += 1
  print confusion_matrix

  g = confusion_matrix
  total = g[0][0] + g[0][1] + g[1][0] + g[1][1]
  print 1.0*g[0][0] / (g[0][0] + g[0][1]), 1.0*g[1][1] / (g[1][0] + g[1][1]), 1.0*(g[0][0] + g[1][1]) / total


def prettyprint(instance):
  # one indexed instance
  output = ""
  for i in xrange(0,8):
    for j in xrange(0,8):
      output = output + str(instance[8*i+j + 1]) + " "
    output = output + "\n"
  return output


def main():

  input_str = raw_input("Enter num_hidden and num_features as space separated integers\n").split()
  #print input_str
  num_hidden, num_features = (int)(input_str[0]), (int)(input_str[1])

  thresh_input_file = 'Nets/Net8x8_%d_%d.thresh' % (num_features, num_hidden)
  bnn = readBNN(thresh_input_file)
  #test(bnn)

  #import sys
  #sys.exit()

  obdd_input_file = '/space/andyshih2/BNN_ODD/output/eps5_smiley.obddas'
  obdd = readOBDD(obdd_input_file)

  '''
  instance = [
    0, 0, 1, 1, 1, 1, 0, 0,
    0, 1, 1, 1, 1, 1, 1, 0,
    0, 1, 0, 0, 0, 0, 1, 0,
    0, 0, 1, 1, 1, 1, 0, 0,
    0, 0, 1, 1, 1, 1, 0, 0,
    0, 1, 0, 0, 0, 0, 1, 0,
    0, 1, 1, 1, 1, 1, 1, 0,
    0, 0, 1, 1, 1, 1, 0, 0
  ]
  '''

  
  instance = [
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 1, 0, 0, 1, 0, 0,
    0, 0, 1, 0, 0, 1, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 1, 0, 0, 0, 0, 1, 0,
    0, 1, 1, 1, 1, 1, 1, 0,
    0, 0, 1, 1, 1, 1, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0
  ]
  
  instance = {i+1: x for i,x in enumerate(instance)}

  print instance
  print evaluateBNN(bnn, instance)
  print evaluateOBDD(obdd, instance)

  import sys
  sys.exit()
  
  import random
  for i in xrange(1000):
    instancecopy = instance.copy()
    debug = ""
    transform = ""
    for j in xrange(7):
      ind = random.randint(1,64)
      instancecopy[ind] = 1-instance[ind]
      transform += prettyprint(instancecopy) + "\n"
      debug = debug + str(ind) + " "
    a = evaluateBNN(bnn, instancecopy)
    b = evaluateOBDD(obdd, instancecopy)
    debug = "%s %s %s" % (debug, str(a), str(b))
    #print debug
    if a != b:
      print prettyprint(instancecopy)
      print prettyprint(instance)
      print debug
  

if __name__ == "__main__":
  main()
