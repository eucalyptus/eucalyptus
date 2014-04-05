#!/usr/bin/python -tt

# Copyright 2011-2012 Eucalyptus Systems, Inc.
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

import glob
import sys
import os
from distutils.command.build_scripts import build_scripts
from distutils.command.install_data import install_data
from distutils.core import setup
from distutils.sysconfig import get_python_lib
import fileinput
import ConfigParser

cfg = ConfigParser.ConfigParser()
cfg.read('setup.cfg')
prefix  = cfg.get('install', 'prefix')
version = cfg.get('meta',    'version')

class build_scripts_with_path_headers(build_scripts):
    def run(self):
        build_scripts.run(self)
        self.path_header = get_python_lib(prefix=prefix).replace('dist-packages', 'site-packages')
        self.outfiles = [os.path.join(self.build_dir, os.path.basename(script))
                         for script in self.distribution.scripts]
        self.add_paths_to_scripts()

    def add_paths_to_scripts(self):
        print 'adding path %s to scripts' % self.path_header
        for line in fileinput.input(self.outfiles, inplace=1, backup=None):
            if fileinput.isfirstline():
                print line.rstrip()
                print 'import sys'
                print 'sys.path.append("%s")' % self.path_header
            elif line.strip() == 'import sys':
                pass
            elif line.strip().startswith('sys.path.append'):
                pass
            else:
                print line.rstrip()

setup(name="eucatoolkit",
      version=version,
      description="Eucalyptus Imaging Toolkit",
      long_description="CLI tools to help manage images in Eucalyptus",
      author="Eucalyptus Systems",
      author_email="devel@eucalyptus.com",
      url="http://eucalyptus.com/",
      packages=['eucatoolkit', 'eucatoolkit.stages'],
      license='BSD',
      platforms='Posix; MacOS X; Windows',
      classifiers=[ 'Development Status :: 5 - Production/Stable',
                      'Intended Audience :: Developers',
                      'License :: OSI Approved :: BSD License',
                      'Operating System :: OS Independent',
                      'Topic :: Internet',
                      ],
      install_requires=[
          "argparse", "lxml", "requests",
      ],
      data_files=[],
      cmdclass={'build_scripts': build_scripts_with_path_headers},
      scripts=["bin/euca-run-workflow"]
      )
