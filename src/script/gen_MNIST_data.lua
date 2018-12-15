require 'torchx';
--files = paths.indexdir('data/8x8/train/', 'data', true)

train_files = {
  ['data/8x8/train/digit-0.data'] = 1,
--  ['data/8x8/train/digit-1.data'] = 2,
--  ['data/8x8/train/digit-2.data'] = 2,
--  ['data/8x8/train/digit-3.data'] = 3,
--  ['data/8x8/train/digit-4.data'] = 4,
--  ['data/8x8/train/digit-5.data'] = 5,
--  ['data/8x8/train/digit-6.data'] = 6,
--  ['data/8x8/train/digit-7.data'] = 7,
  ['data/8x8/train/digit-8.data'] = 2,
--  ['data/8x8/train/digit-9.data'] = 9
}
test_files = {
  ['data/8x8/test/digit-0.data'] = 1,
--  ['data/8x8/test/digit-1.data'] = 2,
--  ['data/8x8/test/digit-2.data'] = 2,
--  ['data/8x8/test/digit-3.data'] = 3,
--  ['data/8x8/test/digit-4.data'] = 4,
--  ['data/8x8/test/digit-5.data'] = 5,
--  ['data/8x8/test/digit-6.data'] = 6,
--  ['data/8x8/test/digit-7.data'] = 7,
  ['data/8x8/test/digit-8.data'] = 2,
--  ['data/8x8/test/digit-9.data'] = 9
}

io.write("Enter the number of features for gen_data:\n")
num_features = io.read("*n")

train_data_raw = {}
train_label_raw = {}

NUMBER_OF_TRAINING_EIGHTS = 276

for file,digit in pairs(train_files) do
  cnt=0
  for line in io.lines(file) do
    if cnt==NUMBER_OF_TRAINING_EIGHTS then break end
    cnt = cnt+1
    array = {}
    for i=1,num_features do
      ind = 1 + 2*i
      if string.sub(line,ind,ind) == '1' then
        array[#array + 1] = 1
      else
        array[#array + 1] = -1
      end
    end
    train_data_raw[#train_data_raw + 1] = array
    train_label_raw[#train_label_raw + 1] = digit
  end
end


valid_data_raw = {}
valid_label_raw = {}

test_data_raw = {}
test_label_raw = {}

for file,digit in pairs(test_files) do
  cnt=0
  for line in io.lines(file) do
    if cnt==NUMBER_OF_TRAINING_EIGHTS then break end
    cnt = cnt+1
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


--Move one-fifth of the train0 data set (even entries) to validation data set
n = #train_data_raw
ind=1
for i=1,n do
  if (i%5 == 0) then
    valid_data_raw[#valid_data_raw + 1] = train_data_raw[i]
    valid_label_raw[#valid_label_raw + 1] = train_label_raw[i]
    ind = ind + 1
  end
  test_data_raw[i] = test_data_raw[ind]
  test_label_raw[i] = test_label_raw[ind]
  ind = ind + 1
end

--print (#test_data_raw) -- 4649 x 32
--print (#test_label_raw) -- 4649 x 1

--Reshape the tables to Tensors
train_data = torch.Tensor(train_data_raw)
train_label = torch.Tensor(train_label_raw)
test_data = torch.Tensor(test_data_raw)
test_label = torch.Tensor(test_label_raw)
valid_data = torch.Tensor(valid_data_raw)
valid_label = torch.Tensor(valid_label_raw)

--Create the table to save
train_data_to_write = {data = train_data, label = train_label}
test_data_to_write = {data = test_data, label = test_label}
valid_data_to_write = {data = valid_data, label = valid_label}

--Save the table
torch.save("./PreProcData/MNIST8x8/mnist_8x8_" .. num_features .. "_train.t7", train_data_to_write)
torch.save("./PreProcData/MNIST8x8/mnist_8x8_" .. num_features .. "_test.t7", test_data_to_write)
torch.save("./PreProcData/MNIST8x8/mnist_8x8_" .. num_features .. "_valid.t7", valid_data_to_write)

print("Wrote train/test/valid data to: ./PreProcData/MNIST8x8/")
