class Term():
  count = 1
  def __init__(self, tid=None, annotation=''):
    self.annotation = annotation
    if not tid:
      tid = Term.count
      Term.count += 1
    self.tid = tid
  def neg(self):
    return Term(tid=-1*self.tid, annotation=self.annotation)
  def __repr__(self):
    return '%d%s' % (self.tid, self.annotation)

class Clause():
  def __init__(self, terms):
    self.terms = terms
  def __repr__(self):
    return self.terms.__repr__()

class Cnf():
  def __init__(self, clauses):
    self.clauses = clauses
  def __repr__(self):
    return self.clauses.__repr__()
  def __add__(self, other_cnf):
    return Cnf(self.clauses + other_cnf.clauses)

def implies(a, b):
  ''' a and b are Term objects. This returns a Cnf object with
  the constraint a => b
  '''
  return Cnf([Clause([a.neg(), b])])

def equiv(a, b):
  ''' a and b are Term objects. This returns a Cnf object with
  the constraint a <=> b
  '''
  return implies(a, b) + implies(b,a)

