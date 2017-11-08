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

from requestbuilder import Arg, MutuallyExclusiveArgList

from eucalyptus_admin.commands.ec2 import EC2Request


class MigrateInstances(EC2Request):
    DESCRIPTION = ('Migrate one instance from its current host, or '
                   'migrate all instances from a specific host')
    ARGS = [MutuallyExclusiveArgList(
                Arg('-s', '--source', dest='SourceHost', metavar='HOST',
                    help='remove all instances from a specific host'),
                Arg('-i', '--instance', dest='InstanceId', metavar='INSTANCE',
                    help='remove one instance from its current host'))
            .required(),
            MutuallyExclusiveArgList(
                Arg('--include-dest', action='append', route_to=None,
                    metavar='HOST', help='''allow migration to only a
                    specific host (may be used more than once)'''),
                Arg('--exclude-dest', action='append', route_to=None,
                    metavar='HOST', help='''allow migration to any host
                    except a specific one (may be used more than once)'''))]

    def preprocess(self):
        if self.args.get('include_dest'):
            self.params['AllowHosts'] = True
            self.params['DestinationHost'] = self.args['include_dest']
        elif self.args.get('exclude_dest'):
            self.params['AllowHosts'] = False
            self.params['DestinationHost'] = self.args['exclude_dest']
