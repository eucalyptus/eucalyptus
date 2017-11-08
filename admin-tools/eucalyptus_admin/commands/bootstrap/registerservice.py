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

from requestbuilder import Arg

from eucalyptus_admin.commands.bootstrap import BootstrapRequest


class RegisterService(BootstrapRequest):
    DESCRIPTION = 'Register a new instance of a service'
    ARGS = [Arg('Name', metavar='SVCINSTANCE',
                help='a name for the new instance of a service'),
            Arg('-t', '--type', dest='Type', required=True,
                help="the new service instance's type (required)"),
            Arg('-h', '--host', metavar='IP', dest='Host', required=True,
                help='''the host the new instance of the service runs on
                (required)'''),
            Arg('--port', dest='Port', type=int,
                help='''the port the new instance of the service runs on
                (default for cluster: 8774, otherwise: 8773)'''),
            Arg('-z', '--availability-zone', metavar='ZONE', dest='Partition',
                help='''availability zone to register the new service instance
                in.  This is required only for services of certain types.''')]

    def preprocess(self):
        if not self.params['Port']:
            if self.args.get('Type').lower() == 'cluster':
                self.params['Port'] = 8774
            else:
                self.params['Port'] = 8773
