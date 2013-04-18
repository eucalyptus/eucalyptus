import sys
import bdb
import traceback
try:
    import epdb as debugger
except ImportError:
    import pdb as debugger

def gen_except_hook(debugger_flag, debug_flag):
  def excepthook(typ, value, tb):
    if typ is bdb.BdbQuit:
      sys.exit(1)
    sys.excepthook = sys.__excepthook__

    if debugger_flag and sys.stdout.isatty() and sys.stdin.isatty():
      if debugger.__name__ == 'epdb':
        traceback.print_exception(type, value, tb)
        debugger.post_mortem(tb, typ, value)
      else:
        print traceback.print_tb(tb)
        debugger.post_mortem(tb)
    elif debug_flag:
      print traceback.print_tb(tb)
      sys.exit(1)
    else:
      print value
      sys.exit(1)

  return excepthook

