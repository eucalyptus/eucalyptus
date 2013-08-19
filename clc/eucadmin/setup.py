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

class ExecutableDataFiles(install_data):
    def run(self):
        install_data.run(self)
        for x in self.outfiles:
            if x.startswith(sys.prefix+"/lib/eucadmin/validator-scripts/"):
                os.chmod(x, 0755)

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

admin_scripts = ["bin/euca_conf",
                 "bin/euca-configure-vmware",
                 "bin/euca-deregister-arbitrator",
                 "bin/euca-deregister-cloud",
                 "bin/euca-deregister-cluster",
                 "bin/euca-deregister-storage-controller",
                 "bin/euca-deregister-vmware-broker",
                 "bin/euca-deregister-walrus",
                 "bin/euca-describe-arbitrators",
                 "bin/euca-describe-clouds",
                 "bin/euca-describe-clusters",
                 "bin/euca-describe-components",
                 "bin/euca-describe-nodes",
                 "bin/euca-describe-properties",
                 "bin/euca-describe-services",
                 "bin/euca-describe-storage-controllers",
                 "bin/euca-describe-vmware-brokers",
                 "bin/euca-describe-walruses",
                 "bin/euca-get-credentials",
                 "bin/euca-modify-cluster",
                 "bin/euca-modify-property",
                 "bin/euca-modify-service",
                 "bin/euca-modify-storage-controller",
                 "bin/euca-modify-walrus",
                 "bin/euca-migrate-instances",
                 "bin/euca-register-arbitrator",
                 "bin/euca-register-cloud",
                 "bin/euca-register-cluster",
                 "bin/euca-register-storage-controller",
                 "bin/euca-register-vmware-broker",
                 "bin/euca-register-walrus",
                 "bin/euca-validator",
                 "bin/eureport-delete-data",
                 "bin/eureport-export-data",
                 "bin/eureport-generate-report",
                 ]

setup(name="eucadmin",
      version=version,
      description="Eucalyptus Admin Tools",
      long_description="CLI tools to help administer Eucalyptus",
      author="Mitch Garnaat",
      author_email="mgarnaat@eucalyptus.com",
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
      install_requires=[
          "argparse",
          "PyYAML",
          "paramiko",
          "PyGreSQL",
          "M2Crypto",
      ],
      data_files=[
          (prefix+"/lib/eucadmin", ['config/validator.yaml']),
          (prefix+"/lib/eucadmin/validator-scripts", glob.glob('validator-scripts/*')),
      ],
      cmdclass={'build_scripts': build_scripts_with_path_headers,
                'install_data':  ExecutableDataFiles},
      scripts=admin_scripts,
      )
