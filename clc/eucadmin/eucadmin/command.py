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

import StringIO
import subprocess
import time

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

    def __init__(self, command, test=False):
        self.exit_code = 0
        self.error = None
        self._stdout_fp = StringIO.StringIO()
        self._stderr_fp = StringIO.StringIO()
        self.command = command
        self.run()

    def run(self):
        if self.test:
            print self.command
            return 0
        self.process = subprocess.Popen(self.command, shell=True,
                                        stdin=subprocess.PIPE,
                                        stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE)
        while self.process.poll() == None:
            time.sleep(1)
            t = self.process.communicate()
            self._stdout_fp.write(t[0])
            self._stderr_fp.write(t[1])
        self.exit_code = self.process.returncode
        return self.exit_code

    @property
    def status(self):
        return self.exit_code

    @property
    def stdout(self):
        return self._stdout_fp.getvalue()

    @property
    def stderr(self):
        return self._stderr_fp.getvalue()


