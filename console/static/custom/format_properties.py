#!/usr/bin/python

def build_map (file_path, key_arr=None):
    new_map = {}
    try:
        f=open(file_path, mode='r')
    except:
        print 'Cannot find %s' % file_path
        exit(-1)

    if f:
        for line in f.readlines():
            try:
                (lkey,rval) = line.split('=', 1)
                lkey = lkey.strip()
                rval = rval.strip()
                if key_arr != None:
                    key_arr.append(lkey)
                new_map[lkey]=rval
            except Exception, err:
                if key_arr != None:
                    key_arr.append(line)
        f.close()
    return new_map
    
if __name__ == "__main__":
    import sys
    import os
    import commands
    from optparse import OptionParser
    from argparse import ArgumentParser

    parser = OptionParser(usage='Usage: %prog -t target_language [-t target ..]')
    parser.add_option('-t', '--target', help='Target language (e.g., ko_KR)', action='append', default=[], dest='target')

    (options, args) = parser.parse_args()
    if not options.target or len(options.target) <= 0:
        parser.print_help()
        exit(-1)
    source_arr = []
    source_map= build_map('./Messages_en_US.properties', source_arr);
    
    # open en_US properties file and build a map
    for lang in options.target:
        print "Processing lanugage: %s" % lang
        dest_path = 'Messages_%s.properties' % lang
        dest_map = build_map(dest_path);
        tmp =  './Messages_%s.properties.new' % lang
        contents = []
        contents.append('#auto-formatted from Messages_en_US.properties\n')
        for k in source_arr:
            if dest_map.has_key(k):
                contents.append('%s = %s\n' % (k, dest_map[k]))
            elif source_map.has_key(k):
                contents.append('#warning - translation not found\n')
                contents.append('%s = %s\n' % (k, source_map[k]))
            else:
                contents.append(k)
        g=open(tmp,'w')
        if g:
          g.writelines(contents)
          g.close()
        os.unlink(dest_path)
        os.rename(tmp, dest_path)
