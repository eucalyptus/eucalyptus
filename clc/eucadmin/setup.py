#!/usr/bin/env python
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

import sys
import os
from distutils.core import setup
from distutils.sysconfig import get_python_lib
import fileinput
import ConfigParser

cfg = ConfigParser.ConfigParser()
cfg.read('setup.cfg')
prefix = cfg.get('install', 'prefix')
install_scripts = cfg.get('install', 'install_scripts')
path_header = 'import sys\nsys.path.append(\"%s\")\n' % (get_python_lib(prefix=prefix))

binaries = ["bin/euca-add-user", "bin/euca-add-user-group",
        "bin/euca-delete-user", "bin/euca-delete-user-group",
        "bin/euca-add-user", "bin/euca-add-user-group",
        "bin/euca_conf", "bin/euca-delete-user",
        "bin/euca-delete-user-group",
        "bin/euca-deregister-cluster",
        "bin/euca-deregister-storage-controller",
        "bin/euca-deregister-walrus",
        "bin/euca-describe-clusters",
        "bin/euca-describe-components",
        "bin/euca-describe-properties",
        "bin/euca-describe-services",
        "bin/euca-describe-storage-controllers",
        "bin/euca-describe-user-groups",
        "bin/euca-describe-users",
        "bin/euca-describe-walruses",
        "bin/euca-get-credentials",
        "bin/euca-modify-cluster",
        "bin/euca-modify-property",
        "bin/euca-modify-storage-controller",
        "bin/euca-modify-walrus",
        "bin/euca-register-cluster",
        "bin/euca-register-storage-controller",
        "bin/euca-register-walrus"]
mangled = [ "%s/%s" % (install_scripts,os.path.basename(x)) for x in binaries ]

setup(name="eucadmin",
      version='0.1',
      description="Eucalyptus Admin Tools",
      long_description="CLI tools to help administer Eucalyptus",
      author="Mitch Garnaat",
      author_email="mgarnaat@eucalyptus.com",
      scripts=binaries,
      url="http://eucalyptus.com/",
      packages=['eucadmin'],
      license='BSD',
      platforms='Posix; MacOS X; Windows',
      classifiers=[ 'Development Status :: 5 - Production/Stable',
                      'Intended Audience :: Developers',
                      'License :: OSI Approved :: BSD License',
                      'Operating System :: OS Independent',
                      'Topic :: Internet',
                      ],
      )

if 'install' in sys.argv[1:]:
  for line in fileinput.input(mangled, inplace=1,backup=None):
    if fileinput.isfirstline():
      print line,
      print path_header,
    elif line == 'import sys\n':
      continue
    elif line.startswith('sys.path.append'):
      continue
    else:
      print line,
