# Copyright 2015 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

from distutils.command.build_py import build_py
from distutils.command.sdist import sdist
import glob
import os.path

from setuptools import find_packages, setup

from eucalyptus_admin import __version__


class build_py_with_git_version(build_py):
    '''Like build_py, but also hardcoding the version in __init__.__version__
       so it's consistent even outside of the source tree'''

    def build_module(self, module, module_file, package):
        build_py.build_module(self, module, module_file, package)
        print module, module_file, package
        if module == '__init__' and '.' not in package:
            version_line = "__version__ = '{0}'\n".format(__version__)
            old_init_name = self.get_module_outfile(self.build_lib, (package,),
                                                    module)
            new_init_name = old_init_name + '.new'
            with open(new_init_name, 'w') as new_init:
                with open(old_init_name) as old_init:
                    for line in old_init:
                        if line.startswith('__version__ ='):
                            new_init.write(version_line)
                        else:
                            new_init.write(line)
                new_init.flush()
            os.rename(new_init_name, old_init_name)


class sdist_with_git_version(sdist):
    '''Like sdist, but also hardcoding the version in __init__.__version__ so
       it's consistent even outside of the source tree'''

    def make_release_tree(self, base_dir, files):
        sdist.make_release_tree(self, base_dir, files)
        version_line = "__version__ = '{0}'\n".format(__version__)
        old_init_name = os.path.join(base_dir, 'eucalyptus_admin/__init__.py')
        new_init_name = old_init_name + '.new'
        with open(new_init_name, 'w') as new_init:
            with open(old_init_name) as old_init:
                for line in old_init:
                    if line.startswith('__version__ ='):
                        new_init.write(version_line)
                    else:
                        new_init.write(line)
            new_init.flush()
        os.rename(new_init_name, old_init_name)


setup(name='eucalyptus_admin',
      version=__version__,
      description='Eucalyptus cloud adminsitration tools',
      long_description='Eucalyptus cloud administration tools',
      author='Eucalyptus Systems, Inc.',
      author_email='support@eucalyptus.com',
      url='http://www.eucalyptus.com',
      scripts=sum((glob.glob('bin/euctl'),
                   glob.glob('bin/euserv-*')),
                  []),
      data_files=[('share/man/man1', glob.glob('man/*.1')),
                  ('share/man/man7', glob.glob('man/*.7'))],
      packages=find_packages(),
      install_requires=['PyYAML', 'requestbuilder>=0.4'],
      license='BSD (Simplified)',
      platforms='Posix; MacOS X',
      classifiers=['Development Status :: 4 - Beta',
                   'Intended Audience :: System Administrators',
                   'License :: OSI Approved :: Simplified BSD License',
                   'Operating System :: OS Independent',
                   'Programming Language :: Python',
                   'Programming Language :: Python :: 2',
                   'Programming Language :: Python :: 2.6',
                   'Programming Language :: Python :: 2.7'],
      cmdclass={'build_py': build_py_with_git_version,
                'sdist': sdist_with_git_version})
