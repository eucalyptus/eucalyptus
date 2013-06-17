#!/usr/bin/python

# Copyright 2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

def build_map (file_path, key_arr=None):
    new_map = {}
    try:
        f=open(file_path, mode='r')
    except:
        print 'Cannot find %s' % file_path
        exit(-1)

    if f:
        warning = False
        for line in f.readlines():
            try:
                if line.startswith('#warning - translation not found'):
                    warning = True
                    continue
                # don't add lines that are preceded by #warning
                if warning == False:
                    (lkey,rval) = line.split('=', 1)
                    lkey = lkey.strip()
                    rval = rval.strip()
                    if key_arr != None:
                        key_arr.append(lkey)
                    new_map[lkey]=rval
                else:
                    warning = False
            except Exception, err:
                if key_arr != None:
                    key_arr.append(line)
        f.close()
    return new_map
    
if __name__ == "__main__":
    import sys
    import os
    import commands
    import re
    from optparse import OptionParser
    from argparse import ArgumentParser

    parser = OptionParser(usage='Usage: %prog -l language [-l language ..]')
    parser.add_option('-l', '--lang', help='Language (e.g., ko_KR)', action='append', default=[], dest='language')

    (options, args) = parser.parse_args()
    if not options.language or len(options.language) <= 0:
        parser.print_help()
        exit(-1)

    # open en_US properties file and build a map
    source_arr = []
    source_map= build_map('./Messages.properties', source_arr);
    
    for lang in options.language:
        print "Processing lanugage: %s" % lang
        dest_path = 'Messages_%s.properties' % lang
        dest_map = build_map(dest_path);
        tmp =  './Messages_%s.properties.new' % lang
        contents = []
        contents.append('#auto-formatted from Messages_en_US.properties\n')
        for k in source_arr:
            if dest_map.has_key(k):
                # check if there are any special chars in the destination that do not exist in the source
                ds = re.sub('[^{}<>]','',dest_map[k])
                ss = re.sub('[^{}<>]','',source_map[k])
                if ds != ss:
                    contents.append('#warning - special characters <> {} mismatch. Original: %s\n' % source_map[k])
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
