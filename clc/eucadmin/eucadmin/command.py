# Copyright (c) 2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: Mitch Garnaat mgarnaat@eucalyptus.com

import subprocess
import os
import time
import shlex

# See https://github.com/kennethreitz/envoy

class Response(object):
    """A command's response"""

    def __init__(self, process=None):
        super(Response, self).__init__()
        self._process = process
        self.command = None
        self.std_err = None
        self.std_out = None
        self.status_code = None

    def __repr__(self):
        if len(self.command):
            return '<Response [{0}]>'.format(self.command[0])
        else:
            return '<Response>'
        
class Command(object):
    """
    A little utility class to wrap calls to shell commands.
    You pass in the command name and any command args.
    The command will  then be executed in a pipe and the output
    and status will be available as the attributes "output"
    and "status".  The value of the "status" attribute will
    be zero if the command was successful or non-zero if it
    was not successful.
    """

    def __init__(self, command):
        self.history = []
        self.run(command)

    def run(self, command, data=None):
        command = command.split('|')
        command = map(shlex.split, command)

        for c in command:

            if len(self.history):
                data = self.history[-1].std_out

            p = subprocess.Popen(c,
                universal_newlines=True,
                shell=False,
                env=os.environ,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )

            out, err = p.communicate(input=data)

            r = Response(process=p)

            r.command = c
            r.std_out = out
            r.std_err = err
            r.status_code = p.returncode

            self.history.append(r)

        return self.status
            
    @property
    def status(self):
        return sum([r.status_code for r in self.history])

    @property
    def stdout(self):
        if len(self.history):
            rslt = self.history[-1].std_out
        else:
            rslt = ''
        return rslt

    @property
    def stderr(self):
        return ''.join([r.std_err for r in self.history])


