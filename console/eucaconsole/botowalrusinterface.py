# Copyright 2013 Eucalyptus Systems, Inc.
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

import boto
import ConfigParser
import json
from boto.s3.connection import S3Connection

import eucaconsole
from .botojsonencoder import BotoJsonEncoder
from .walrusinterface import WalrusInterface

# This class provides an implmentation of the clcinterface using boto
class BotoWalrusInterface(WalrusInterface):
    conn = None
    saveclcdata = False

    def __init__(self, clc_host, access_id, secret_key, token):
        boto.set_stream_logger('foo')
        path='/services/Walrus'
        port=8773
        try:
            # this call is just here to check if the ec2 feature is config-ed
            host = eucaconsole.config.get('test', 'ec2.endpoint')
            path = '/'
            port=443
        except ConfigParser.Error:
            pass
        self.conn = S3Connection(access_id, secret_key, host=clc_host,
                                  port=port, path=path,
                                  is_secure=True, security_token=token, debug=2)
        self.conn.http_connection_kwargs['timeout'] = 30

    def __save_json__(self, obj, name):
        f = open(name, 'w')
        json.dump(obj, f, cls=BotoJsonEncoder, indent=2)
        f.close()


    def get_all_buckets(self):
        return self.conn.get_all_buckets()

    def get_all_objects(self, bucket, callbcack=None):
        b = self.conn.get_bucket(bucket)
        return b.get_all_keys()

