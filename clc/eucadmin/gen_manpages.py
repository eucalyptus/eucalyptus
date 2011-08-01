import os
from eucadmin.command import Command

def gen_manpages():
    if not os.path.isdir('man'):
        os.mkdir('man')
    # first remove any existing man pages
    for file in os.listdir('man'):
        os.unlink(os.path.join('man', file))
    for file in os.listdir('bin'):
        cmd_string = """help2man -n "Eucalyptus Admin tool: %s" -o man/%s.1 bin/%s""" % (file, file, file)
        cmd = Command(cmd_string)
        if cmd.status != 0:
            print 'error encountered'
            print cmd.stderr

if __name__ == "__main__":
    gen_manpages()

    
    




