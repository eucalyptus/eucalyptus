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

#
# This class defines an interface that must be extended for either talking
# to the CLC itself or for a testing mock
#
# NOTE: all methods are expected to return boto value objects.
#
class WatchInterface(object):

    ##
    # cloud watch methods
    ##
    def get_metric_statistics(self, period, start_time, end_time, metric_name, namespace, statistics, dimensions=None, unit=None, callbcack=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def list_metrics(self, next_token=None, dimensions=None, metric_name=None, namespace=None, callbcack=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def put_metric_data(self, namespace, name, value=None, timestamp=None, unit=None, dimensions=None, statistics=None, callbcack=None):
        raise NotImplementedError("Are you sure you're using the right class?")

