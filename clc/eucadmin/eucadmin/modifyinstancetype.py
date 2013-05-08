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

import sys
import os
from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
import eucadmin
from eucadmin import EucAdmin

class ModifyVmTypeAttribute(AWSQueryRequest):
    ServicePath = '/services/Eucalyptus'
    APIVersion = 'eucalyptus'
    ServiceClass = EucAdmin
    Description = 'Modify the definition of an instance type.'

    Params = [Param(name='Disk',
                    short_name='d',
                    long_name='disk',
                    ptype='integer',
                    optional=True,
                    doc='Gigabytes of disk for the root file system image.'),
              Param(name='Cpu',
                    short_name='c',
                    long_name='cpu',
                    ptype='integer',
                    optional=True,
                    doc='Number of virtual CPUs allocated to this type of instance.'),
              Param(name='Memory',
                    short_name='m',
                    long_name='memory',
                    ptype='integer',
                    optional=True,
                    doc='Megabytes of RAM allocated to this instance type..'),
#               Param(name='EphemeralDisk',
#                     short_name='e',
#                     long_name='ephemeral-disk',
#                     ptype='integer',
#                     optional=True,
#                     encoder=encode_disk,
#                     doc='''Modify definition of ephemeral disk in the form ephemeralX=[<device-name>][:<size>[:<format>]], where:
#                     \tephemeralX \tX indicates the ephemeral disk index
#                     \tdevice-name\tA device string of the form /dev/sd[b-e]
#                     \tsize       \tSize in gigabytes.
#                     \tformat     \tFormat of the disk (one of: swap, ext3, none)'''),
              Param(name='Reset',
                    short_name='r',
                    long_name='reset-to-default',
                    ptype='boolean',
                    optional=True,
                    doc='Reset this instance type back to its default values.')]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc='instance type name')]

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection

    def cli_formatter(self, data):
        previousVmtype = getattr(data, 'euca:previousVmType')
        vmtype = getattr(data, 'euca:vmType')
        fmt = 'TYPE\t%s\t%-10.10s%-10d%-10d%-10d'
        print fmt % ("OLD",
                     vmtype['euca:name'],
                     int(vmtype['euca:cpu']),
                     int(vmtype['euca:disk']),
                     int(vmtype['euca:memory']))
        print fmt % ("NEW",
                     vmtype['euca:name'],
                     int(vmtype['euca:cpu']),
                     int(vmtype['euca:disk']),
                     int(vmtype['euca:memory']))

    def main(self, **args):
        return self.send(verb='POST', **args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
