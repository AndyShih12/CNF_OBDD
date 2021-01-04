require 'torch'
require 'nn'
require 'Models/BinaryLinear'
require 'Models/BatchNormalizationShiftPow2'
require 'Models/BinarizedNeurons'

io.write("Enter the number of hidden nodes for extracting:\n")
num_hidden = io.read("*n")
io.write("Enter the number of features for extracting:\n")
num_features = io.read("*n")

input_file = 'Nets/Net8x8_' .. num_features .. '_' .. num_hidden .. '.t7'
output_txt_file = 'Nets/Net8x8_' .. num_features .. '_' .. num_hidden .. '.txt'
output_threshold_circuit_file = 'Nets/Net8x8_' .. num_features .. '_' .. num_hidden .. '.thresh'

-- to delete
test_files = {
  ['data/8x8/test/digit-0.data'] = 1,
--  ['data/8x8/test/digit-1.data'] = 2,
--  ['data/8x8/test/digit-2.data'] = 2,
--  ['data/8x8/test/digit-3.data'] = 2,
--  ['data/8x8/test/digit-4.data'] = 2,
--  ['data/8x8/test/digit-5.data'] = 2,
--  ['data/8x8/test/digit-6.data'] = 2,
--  ['data/8x8/test/digit-7.data'] = 2,
  ['data/8x8/test/digit-8.data'] = 2,
--  ['data/8x8/test/digit-9.data'] = 2
}

test_data_raw = {}
test_label_raw = {}

for file,digit in pairs(test_files) do
  for line in io.lines(file) do
    array = {}
    for i=1,num_features do
      ind = 1 + 2*i
      if string.sub(line,ind,ind) == '1' then
        array[#array + 1] = 1
      else
        array[#array + 1] = -1
      end
    end
    test_data_raw[#test_data_raw + 1] = array
    test_label_raw[#test_label_raw + 1] = digit
  end
end

test_data = torch.FloatTensor(test_data_raw)
test_label = torch.FloatTensor(test_label_raw)

-- end delete


model = torch.load(input_file)
print(model)
model:evaluate()
A = {}
for i=1,64 do
  A[i] = 2*math.fmod(i,2)-1
end
array_tensor = torch.FloatTensor(A)
output = model:forward(test_data)
--print(model:get(2).output)
a = {{0,0},{0,0}}
for i=1,output:size()[1] do
  classify = (output[i][2] > output[i][1]) and 2 or 1
  truth = test_label[i]
  a[truth][classify] = a[truth][classify] + 1
end
print(a[1][1], a[1][2], a[2][1], a[2][2])
--params, grad_params = model:getParameters()

txt_file = io.open(output_txt_file, 'w')
io.output(txt_file)

layers = {2,6}
dim = {
  [1] = {num_features, num_hidden},
  [2] = {num_hidden, 2}
};

bn_layers = {3}

--for key,value in pairs(model:get(6)) do
--  print(key);
  --print(value);
--end

--print(model:get(6).running_mean)
--print(model:get(6).running_var)
--print(model:get(6).weight)
--print(model:get(6).bias)

--[=[
  n is the number of layers
  s[i][1], s[i][2] are the input, output size of layer i
  layer i has a matrix of weights w[i] of dimension s[i][2] x s[i][1]
  layer i has a matrix pf biases b[i] of dimension s[i][2] x 1

  Output format:
  n
  s[1][1] s[1][2]
  ...
  s[n][1] s[n][2]
  w[1][1][1]
  ...
  w[1][1][s[1][1]]
  ...
  w[1][s[1][2]][1]
  ...
  w[1][s[1][2]][s[1][1]]
  b[1][1]
  ...
  b[1][s[1][2]]
  ...
  w[n][1][1]
  ...
  b[n][s[n][2]]
--]=]



io.write(#layers .. '\n')
for i=1,#layers do
  io.write(string.format('%d %d\n', dim[i][1], dim[i][2]))
end

for i=1,#layers do
  for j=1,dim[i][2] do
    for k=1,dim[i][1] do
      val = (model:get(layers[i]).weight[j][k] > 0) and 1 or -1
      io.write(val .. '\n')
    end
  end
  for j=1,dim[i][2] do
    bias = model:get(layers[i]).bias[j]
    threshold = -1 * bias -- multiply by negative to convert bias to a threshold
    comp = ">="

    if i <= #bn_layers then
      mean = model:get(bn_layers[i]).running_mean[j]
      inv_std = model:get(bn_layers[i]).running_std[j]
      std_ap2 = 1.0/math.pow(2,math.floor(math.log(inv_std)/math.log(2) + 0.5))
      alpha = model:get(bn_layers[i]).weight[j]
      alpha_sign = (alpha > 0) and 1 or -1
      alpha_ap2 = alpha_sign * ( math.pow(2,math.floor(math.log(math.abs(alpha))/math.log(2) + 0.5)))
      gamma = model:get(bn_layers[i]).bias[j]
      threshold = -1.0*(std_ap2/alpha_ap2)*gamma + mean - bias
      if alpha_ap2 < 0 then
        comp = "<"
      end

    end

    io.write(string.format("%s %.10f\n", comp, threshold))
  end
end

print('wrote network parameters from ' .. input_file .. ' to file ' .. output_txt_file)



--[ 
--  Outputting threshold circuit in circuit format.
--
--  First line stores the number of nodes. Then, one line per node. Field 
--  id stores the id of the node. Field n stores the indegree of the node. 
--  Fields c_1 to c_n store the id of the incoming nodes. A negative id 
--  means that the incoming edge has an inverter. Field cmp stores the 
--  comparison (either '>=' or '<'), and field T stores the threshold.
--
--  id n c_1 c_2 ... c_n cmp T
--
--  For example, fields cmp and T as >= 5 means that this node evaluates to
--   1 if the sum of children is >= 5, otherwise this node evaluates to -1.
--  
--  Input nodes are simply represented as:
--
--  id 0
--]



thresh_file = io.open(output_threshold_circuit_file, 'w')
io.output(thresh_file)

num_nodes = dim[1][1]
for i=1,#layers do
  num_nodes = num_nodes + dim[i][2]
end

io.write(num_nodes .. '\n')


for i=1,dim[1][1] do
  cur_id = i
  io.write(string.format('%d 0 \n', cur_id))
end

id_cnt = dim[1][1]

for i=1,#layers do
  for j=1,dim[i][2] do
    cur_id = id_cnt + j
    io.write(string.format('%d %d ', cur_id, dim[i][1]))
    for k=1,dim[i][1] do
      val = (model:get(layers[i]).weight[j][k] > 0) and 1 or -1
      innode_id = id_cnt - dim[i][1] + k
      io.write(val*innode_id .. ' ')
    end

    bias = model:get(layers[i]).bias[j]
    threshold = -1 * bias -- multiply by negative to convert bias to a threshold
    comp = ">="
    if i <= #bn_layers then
      mean = model:get(bn_layers[i]).running_mean[j]
      inv_std = model:get(bn_layers[i]).running_std[j]
      std_ap2 = 1.0/math.pow(2,math.floor(math.log(inv_std)/math.log(2) + 0.5))
      alpha = model:get(bn_layers[i]).weight[j]
      alpha_sign = (alpha > 0) and 1 or -1
      alpha_ap2 = alpha_sign * ( math.pow(2,math.floor(math.log(math.abs(alpha))/math.log(2) + 0.5)))
      gamma = model:get(bn_layers[i]).bias[j]
      threshold = -1.0*(std_ap2/alpha_ap2)*gamma + mean - bias
      if alpha_ap2 < 0 then
        comp = "<"
      end
    end

    io.write(string.format('%s %.10f\n', comp, threshold))
    --io.write(string.format('>= %.6f %f %f %f %f %f\n', threshold, std_ap2, alpha_ap2, gamma, mean, bias))
  end
  id_cnt = id_cnt + dim[i][2]
end

print('wrote threshold circuit from ' .. input_file .. ' to file ' .. output_threshold_circuit_file)

