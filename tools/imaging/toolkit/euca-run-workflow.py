# Copyright 2009-2013 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

# main
import os
import subprocess
import argparse
import sys

# our custom workflow-related exception types

class WF_InsufficientArguments(RuntimeError):
   pass

class WF_InsufficientDependencies(RuntimeError):
   pass

# base workflow class

class WF_base():
    def __init__(self):
        self.problems = [] # list of problems (implying workflow is not usable)
    @staticmethod
    def add_arguments(parser):
        pass
    def check_deps(self, args):
        pass
    def execute(self, args):
        raise NotImplementedError

# specific workflows, one per class

class WF_DownBundleFS_UpBundle(WF_base):
    """Workflow for downloading a file-system bundle, converting it to a disk, and uploading.
    """

    @staticmethod
    def add_arguments(parser):
        _add_argument(parser, '--url-image', help='URL of the download manifest for the image bundle')
        _add_argument(parser, '--url-kernel', help='URL of the download manifest for the kernel bundle')
        _add_argument(parser, '--url-ramdisk', help='URL of the download manifest for the ramdisk bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        if len(self.problems) > 0:
            raise WF_InsufficientDependencies(self.problems)
        if args.url_image == None or args.url_kernel == None or args.url_ramdisk == None:
            raise WF_InsufficientArguments()
        print "running workflow '" + args.name + "'"

class WF_DownBundle_WriteRaw(WF_base):
    """Workflow for downloading a bundle and writing its contents to a file.
    """

    @staticmethod
    def add_arguments(parser):
        _add_argument(parser, '--url-image', help='URL of the download manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        if args.url_image == None:
            raise WF_InsufficientArguments()
        print "running workflow '" + args.name + "'"

class WF_DownBundle_UpVMDK(WF_base):
    """Workflow for downloading a bundle and uploading its contents as VMDK.
    """

    @staticmethod
    def add_arguments(parser):
        _add_argument(parser, '--url-image', help='URL of the download manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        if args.url_image == None:
            raise WF_InsufficientArguments()
        print "running workflow '" + args.name + "'"

# helper that guards against adding an already existing argument (since workflows may reuse them)

def _add_argument(parser, name, help=''):
    try:
        parser.add_argument(name, required=False, help=help)
    except argparse.ArgumentError:
        pass

# checking helpers to detect presence of dependencies

def _check_executable(problems, command, dep_name):
    with open(os.devnull, "w") as fnull:
        if subprocess.call(command, stdout=fnull, stderr=fnull) != 0:
            problems.append(dep_name + ' is missing')
        else:
            print "found " + dep_name

def _check_euca2ools(problems):
    _check_executable(problems, ["euca-version"], "euca2ools")
    return problems

WORKFLOWS = {'down-bundle-fs/up-bundle': WF_DownBundleFS_UpBundle(),
             'down-bundle/write-raw': WF_DownBundle_WriteRaw(),
             'down-bundle/up-vmdk': WF_DownBundle_UpVMDK()}

# global arguments, apply to all workflows
parser = argparse.ArgumentParser(description='Run an Imaging Toolkit workflow.')
parser.add_argument('--name', required=True, help='Name of the workflow to run')

# let each workflow add its own arguments, all as optional
for name in WORKFLOWS:
    WORKFLOWS[name].add_arguments(parser)
args = parser.parse_args()

# let each workflow ensure it sees required dependencies
for name in WORKFLOWS:
    WORKFLOWS[name].check_deps(args)

# run the desired workflow
try:
    WORKFLOWS[args.name].execute(args)
except KeyError:
    print "unknown workflow '" + args.name + "'"
    sys.exit(2)
except WF_InsufficientArguments, e:
    print "insufficient arguments for workflow '" + args.name + "'"
except WF_InsufficientDependencies, e:
    print "workflow '" + args.name + "' is unavailable! Problems:"
    for problem in e.args[0]:
        print "\t - " + problem
