
from distutils.core import setup
import glob
import os

DATA_DIR='/usr/share/eucalyptus-ui'

def getDataFiles(path):
    return [ (os.path.join(DATA_DIR, root),
        [ os.path.join(root, f) for f in files ])
            for root, _, files in os.walk(path) if files ]

data_files = getDataFiles("static") + getDataFiles("mockdata")
data_files.append(('/etc/eucalyptus-ui', ['server/console.ini']))

setup (name="Eucalyptus Management Console",
    version = "0",
    description = "Eucalyptus User Interface Console",
    long_description = "Eucalyptus User Interface Console",
    author = "Sang-Min Park, David Kavanagh, Vasiliy Kochergin",
    author_email = "community@eucalyptus.com",
    license = "GPL v3",
    url = "http://www.eucalytpus.com",
    packages = ['server'],
    package_data={'server': ['server/console.ini']},
    scripts = ['launch.py'],
    data_files = data_files
)

