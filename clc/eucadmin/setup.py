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

try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

from eucadmin import __version__

install_requires = ['boto', 'psutils']

setup(name = "eucadmin",
      version = __version__,
      description = "Eucalyptus Admin Tools",
      long_description="CLI tools to help administer Eucalyptus",
      author = "Mitch Garnaat",
      author_email = "mgarnaat@eucalyptus.com",
      scripts = ["bin/euca-add-user", "bin/euca-add-user-group",
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
                 "bin/euca-register-walrus"],
      install_requires=install_requires,
      url = "http://eucalyptus.com/",
      packages = ['eucadmin'],
      license = 'BSD',
      platforms = 'Posix; MacOS X; Windows',
      classifiers = [ 'Development Status :: 5 - Production/Stable',
                      'Intended Audience :: Developers',
                      'License :: OSI Approved :: BSD License',
                      'Operating System :: OS Independent',
                      'Topic :: Internet',
                      ],
      )
