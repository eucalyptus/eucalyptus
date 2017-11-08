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
import requestbuilder.auth.aws
import requestbuilder.request
import requestbuilder.service

from eucalyptus_admin.commands import EucalyptusAdmin
from eucalyptus_admin.exceptions import AWSError
from eucalyptus_admin.util import add_fake_region_name


class EC2(requestbuilder.service.BaseService):
    NAME = 'ec2'
    DESCRIPTION = 'Compute service'
    REGION_ENVVAR = 'AWS_DEFAULT_REGION'
    URL_ENVVAR = 'EC2_URL'

    ARGS = [Arg('-U', '--url', metavar='URL',
                help='compute service endpoint URL')]

    def configure(self):
        requestbuilder.service.BaseService.configure(self)
        add_fake_region_name(self)

    def handle_http_error(self, response):
        raise AWSError(response)


class EC2Request(requestbuilder.request.AWSQueryRequest):
    SUITE = EucalyptusAdmin
    SERVICE_CLASS = EC2
    AUTH_CLASS = requestbuilder.auth.aws.HmacV4Auth
    API_VERSION = '2014-10-01'
    METHOD = 'POST'
