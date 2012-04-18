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

import boto.utils
import urlparse

class Heartbeat(object):

    def __init__(self, host, service=None, port=8773,
                 path='services/Heartbeat',
                 is_secure=False):
        self.host = host
        self.service = service
        self.port = port
        self.path = path
        self.is_secure = is_secure
        if self.is_secure:
            self.scheme = 'https'
        else:
            self.scheme = 'http'
        t = (self.scheme, '%s:%s' % (self.host, self.port), self.path, '', '')
        self.url = urlparse.urlunsplit(t)
        self.data = None
        self.get_heartbeat_data()

    def __repr__(self):
        return '<Heartbeat: %s>' % self.url

    def _get_value(self, value):
        if value == 'true':
            value = True
        elif value == 'false':
            value = False
        return value

    def get_heartbeat_data(self):
        resp = boto.utils.retry_url(self.url)
        lines = resp.splitlines()
        self.data = {}
        for line in lines:
            pairs = line.split()
            t = pairs[0].split('=')
            d = {}
            self.data[t[1]] = d
            for pair in pairs[1:]:
                t = pair.split('=')
                d[t[0]] = self._get_value(t[1])

    def cli_formatter(self):
        for key in self.data:
            val = self.data[key]
            print '%s:\tlocal=%s\tinitialize=%s\tenabled=%s' % (key, val['local'],
                                                                val['initialized'],
                                                                val['enabled'])
