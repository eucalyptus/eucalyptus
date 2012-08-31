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

from boto.roboto.param import Param
from eucadmin.reportsrequest import ReportsRequest
import os

class ReportsExport(ReportsRequest):
    Description = 'Export reporting data'

    Args = [Param(name='file', long_name='file',
        ptype='string', optional=False, request_param=False,
        doc='The path to the resulting reporting data export file')]

    def name(self):
        return 'ExportData'

    def check_export_file(self):
        if os.path.exists(self.file):
            msg = 'file %s already exists, ' % self.file
            msg += 'please remove and try again'
            raise IOError(msg)

    def cli_formatter(self, data):
        export = getattr( data, 'Data', None )
        if export is None:
            raise IOError('Error reading export response')
        f = open( self.file, 'w')
        f.write( export )
        f.close()
        print 'Exported data to ' + self.file

    def process_args(self, **args):
        super(ReportsExport, self).process_args( **args )
        self.file = self.args['file']
        self.check_export_file()
