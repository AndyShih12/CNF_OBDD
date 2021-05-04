This repository contains code for the paper:

[Verifying Binarized Neural Networks by Angluin-Style Learning](https://cs.stanford.edu/~andyshih/assets/pdf/SDCsat19.pdf)

```
"Verifying Binarized Neural Networks by Angluin-Style Learning"
Andy Shih, Adnan Darwiche, Arthur Choi
In 22nd International Conference on Theory and Applications of Satisfiability Testing (SAT), 2019

@inproceedings{SDCsat19,
  author    = {Andy Shih and Adnan Darwiche and Arthur Choi},
  title     = {Verifying Binarized Neural Networks by Angluin-Style Learning},
  booktitle = {Proceedings of the 22nd International Conference on Theory and Applications of Satisfiability Testing (SAT)},
  month     = {july},
  year      = {2019},
  keywords  = {conference}
}
```

Requires:
Python2
torch
Java
cmake3


First you have to build the riss-solver coprocessor
Follow the setups in https://github.com/nmanthey/riss-solver/blob/master/doc/TUTORIAL.md
In particular:
```
cd bnn/BinaryNet/cpp/
git clone https://github.com/nmanthey/riss-solver.git
cd riss-solver
mkdir -p build
cd build
cmake ..
make coprocessor
```

Use the ./run_start script to start the experiments.

Change the arguments [num_features, num_hidden, radius, type] as needed.

Check out the paper for more details.

## Contact
For questions, contact us at:

andyshih at cs dot stanford dot edu
