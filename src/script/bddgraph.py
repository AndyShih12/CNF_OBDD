import glob, os
from subprocess import call

#os.chdir('output')
files = glob.glob('*.obddas')

include_labels = True

for file in files:
  print file

  with open(file) as f:
    lines = f.readlines()

  output_file = file[8:] + '.gv'
  with open(output_file, 'w') as f:
    f.write('digraph G {')
    for l in lines:
      arr = l.strip().split(' ')
      arr[3] = arr[3].translate(None,'[,]')
      arr[4] = arr[4].translate(None,'[,]')
      arr[5] = arr[5].translate(None,'[,]')

      if include_labels:
        f.write(arr[0] + ' [label="' + arr[3] + '"]')
        f.write(arr[0] + ' -> ' + arr[1] + ' [label="' + arr[4] + '"] [style=dotted] ;')
        f.write(arr[0] + ' -> ' + arr[2] + ' [label="' + arr[5] + '"];')
      else:
        f.write(arr[0] + ' [label=""]')
        f.write(arr[0] + ' -> ' + arr[1] + ' [label=""] [style=dotted] ;')
        f.write(arr[0] + ' -> ' + arr[2] + ' [label=""];')
    f.write('}')

  call(['dot', '-Tpdf', output_file, '-o', 'o' + output_file[:-3] + '.pdf']);
