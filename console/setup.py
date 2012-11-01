
from distutils.core import setup
import glob
import os

DATA_DIR='/usr/share/eucalyptus-console'

def getDataFiles(path):
    return [ (os.path.join(DATA_DIR, root),
        [ os.path.join(root, f) for f in files ])
            for root, _, files in os.walk(path) if files ]

data_files = getDataFiles("static")
data_files.append(('/etc/eucalyptus-console', ['eucaconsole/console.ini']))

setup (name="Eucalyptus Management Console",
    version = "0",
    description = "Eucalyptus User Interface Console",
    long_description = "Eucalyptus User Interface Console",
    author = "Sang-Min Park, David Kavanagh, Vasiliy Kochergin",
    author_email = "community@eucalyptus.com",
    license = "GPL v3",
    url = "http://www.eucalytpus.com",
    packages = ['eucaconsole', 'esapi', 'esapi/codecs'],
    package_data={'eucaconsole': ['eucaconsole/console.ini']},
    scripts = ['euca-console-server'],
    data_files = data_files
)

