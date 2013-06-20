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

from distutils.core import setup
import glob
import os
import shutil

DATA_DIR='/usr/share/eucalyptus-console'

def getVersion():
    with open('../VERSION') as fp:
        return fp.read().rstrip()

def getDataFiles(path):
    return [ (os.path.join(DATA_DIR, root),
        [ os.path.join(root, f) for f in files ])
            for root, _, files in os.walk(path, followlinks=True) if files ]

data_files = getDataFiles("static")
data_files.append(('/etc/eucalyptus-console', ['eucaconsole/console.ini']))

# Read in version and generate version module
version = getVersion()
with open('eucaconsole/version.py', 'w') as fp:
    fp.write("""# Do not edit. This file is generated.
__version__ = '%s'
""" % (version))

# Overwrite console.ini with the default version
# We want to make sure we don't include the developer config
if os.path.isfile('eucaconsole/console.ini'):
    shutil.move('eucaconsole/console.ini', 'eucaconsole/console.ini.bak')
shutil.copyfile('eucaconsole/console.ini.default', 'eucaconsole/console.ini')

setup (name="Eucalyptus Management Console",
    version = version,
    description = "Eucalyptus User Interface Console",
    long_description = "Eucalyptus User Interface Console",
    author = "Sang-Min Park, David Kavanagh, Vasiliy Kochergin",
    author_email = "community@eucalyptus.com",
    license = "GPL v3",
    url = "http://www.eucalyptus.com",
    packages = ['eucaconsole', 'esapi', 'esapi/codecs'],
    package_data={'eucaconsole': ['eucaconsole/console.ini']},
    scripts = ['euca-console-server'],
    data_files = data_files
)

