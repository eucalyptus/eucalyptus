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
from pprint import pprint
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
        self.id = 'unset'
        self.problems = [] # list of problems (implying workflow is not usable)
        self.description = self.__doc__

    def add_arguments(self, name, subparsers):
        pass

    def check_deps(self, args):
        pass

    def execute(self, args):
        if len(self.problems) > 0:
            raise WF_InsufficientDependencies(self.problems)
        print "running workflow '" + args.name + "'"

# specific workflows, one per class

class WF_DownBundleFS_UpBundle(WF_base):
    """Downloads a file-system bundle, converting to a disk, uploads"""

    def add_arguments(self, id, subparsers):
        self.id = id
        parser = subparsers.add_parser(id, help='a help')
        parser.add_argument('--url-image', metavar='URL', help='URL of the manifest for the image bundle')
        parser.add_argument('--url-kernel', metavar='URL', help='URL of the manifest for the kernel bundle')
        parser.add_argument('--url-ramdisk', metavar='URL', help='URL of the manifest for the ramdisk bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        WF_base.execute(self, args)
        if args.url_image == None or args.url_kernel == None or args.url_ramdisk == None:
            raise WF_InsufficientArguments()


class WF_DownBundle_WriteRaw(WF_base):
    """Downloads a bundle, writes its contents to a file/device"""

    def add_arguments(self, id, subparsers):
        self.id = id
        parser = subparsers.add_parser(id, help='a help')
        parser.add_argument('--url-image', metavar='URL', help='URL of the manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        WF_base.execute(self, args)
        if args.url_image == None:
            raise WF_InsufficientArguments()


class WF_DownBundle_UpVMDK(WF_base):
    """Downloading a bundle, uploads its contents to datastore as VMDK"""

    def add_arguments(self, id, subparsers):
        self.id = id
        parser = subparsers.add_parser(id, help='a help')
        parser.add_argument('--url-image', metavar='URL', help='URL of the manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        WF_base.execute(self, args)
        if args.url_image == None:
            raise WF_InsufficientArguments()


class WF_ReadRaw_UpBundle(WF_base):
    """Bundles contents of a local file/disk and uploads it to Object Store"""

    def add_arguments(self, id, subparsers):
        self.id = id
        parser = subparsers.add_parser(id, help='a help')
        parser.add_argument('--url-image', metavar='URL', help='URL of the manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        WF_base.execute(self, args)
        if args.url_image == None:
            raise WF_InsufficientArguments()


class WF_DownVMDK_UpBundle(WF_base):
    """Downloads a remote VMDK, bundles the disk, and uploads it to Object Store"""

    def add_arguments(self, id, subparsers):
        self.id = id
        parser = subparsers.add_parser(id, help='a help')
        parser.add_argument('--url-image', metavar='URL', help='URL of the manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        WF_base.execute(self, args)
        if args.url_image == None:
            raise WF_InsufficientArguments()


class WF_DownParts_WriteRaw(WF_base):
    """Downloads an import bundle, writes its contents to a file/device"""

    def add_arguments(self, id, subparsers):
        self.id = id
        parser = subparsers.add_parser(id, help='a help')
        parser.add_argument('--url-image', metavar='URL', help='URL of the manifest for the image bundle')

    def check_deps(self, args):
        self.problems = _check_euca2ools(self.problems)

    def execute(self, args):
        WF_base.execute(self, args)
        if args.url_image == None:
            raise WF_InsufficientArguments()

# checking helpers to detect presence of dependencies

def _check_executable(problems, command, dep_name):
    with open(os.devnull, "w") as fnull:
        if subprocess.call(command, stdout=fnull, stderr=fnull) != 0:
            problems.append(dep_name + ' is missing')


def _check_euca2ools(problems):
    _check_executable(problems, ["euca-version"], "euca2ools")
    return problems


WORKFLOWS = {'down-bundle-fs/up-bundle': WF_DownBundleFS_UpBundle(),
             'down-bundle/write-raw': WF_DownBundle_WriteRaw(),
             'down-bundle/up-vmdk': WF_DownBundle_UpVMDK(),
             'read-raw/up-bundle': WF_ReadRaw_UpBundle(),
             'down-vmdk/up-bundle': WF_DownVMDK_UpBundle(),
             'down-parts/write-raw': WF_DownParts_WriteRaw()}

# global arguments, apply to all workflows
parser = argparse.ArgumentParser(prog='euca-run-workflow', description='Run an Imaging Toolkit workflow.')
parser.add_argument('-l', '--list', required=False, help='Lists the available workflows', action="store_true")
subparsers = parser.add_subparsers(dest='name', help='Workflows help')

# let each workflow add its own arguments, all as optional
for name in WORKFLOWS:
    WORKFLOWS[name].add_arguments(name, subparsers)
args = parser.parse_args()

# let each workflow ensure it sees required dependencies
for name in WORKFLOWS:
    WORKFLOWS[name].check_deps(args)

if args.list:
    for name in WORKFLOWS:
        if len(WORKFLOWS[name].problems) == 0:
            print "%30s: %s" % (name, WORKFLOWS[name].description)
    sys.exit(0)

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
