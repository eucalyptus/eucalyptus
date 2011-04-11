# Copyright (c) 2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: Mitch Garnaat mgarnaat@eucalyptus.com

# # enable/disable services
# if [ -r $EUCALYPTUS/var/lib/eucalyptus/services ]; then
# 	for x in `cat $EUCALYPTUS/var/lib/eucalyptus/services` ; do
# 		TO_START="$TO_START $x"
# 	done
# fi
# if [ -n "$DISABLED" -o -n "$ENABLED" ]; then
# 	for x in $TO_START $ENABLED ; do
# 		to_start="Y"
# 		for y in $DISABLED ; do
# 			if [ "$x" = "$y" ]; then
# 				to_start="N"
# 			fi
# 		done
# 		[ $to_start = "Y" ] && echo $x
# 	done | sort | uniq > $EUCALYPTUS/var/lib/eucalyptus/services
# fi

import os

class Enable(object):

    def __init__(self, config, service, enable):
        self.config = config
        self.service = service
        self.enable = enable

    def main(self):
        path = os.path.join(self.config['EUCALYPTUS'],
                            'var/lib/eucalpytus/services')
        fp = open(path)
        s = fp.read()
        fp.close()
        current = s.split()
        if self.enable:
            if self.service not in current:
                current.append(self.service)
        else:
            if self.service in current:
                current.remove(self.service)
        current.sort()
        fp = open(path, 'w')
        fp.write('\n'.join(current))
        fp.close()
                                          
