#!/usr/bin/env python
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

import sys
import re
from boto.exception import EC2ResponseError
from eucadmin.generic import BooleanResponse
from eucadmin import EucAdmin
from optparse import OptionParser

class Cluster():
  
    def __init__(self, cluster_name=None, host_name=None,
                 port=None, partition=None, state=None):
        self.cluster_name = cluster_name
        self.host_name = host_name
        self.port = port
        self.partition = partition
        self.state = state
        self.euca = EucAdmin()
        self.parser = None
          
    def __repr__(self):
        return 'CLUSTER\t%s\t%s\t%s\t%s\t%s' % (self.partition,
                                                self.cluster_name,
                                                self.host_name,
                                                self.state,
                                                self.detail)

    def error(self, msg):
        print msg
        if self.parser:
            self.parser.print_help()
        sys.exit(1)

    def startElement(self, name, attrs, connection):
        return None

    def endElement(self, name, value, connection):
        if name == 'euca:detail':
            self.detail = value
        elif name == 'euca:state':
            self.state = value
        elif name == 'euca:hostName':
            self.host_name = value
        elif name == 'euca:name':
            self.cluster_name = value
        elif name == 'euca:partition':
            self.partition = value
        else:
            setattr(self, name, value)
  
    def get_describe_parser(self):
        parser = OptionParser("usage: %prog [CLUSTERS...]",
                              version="Eucalyptus %prog VERSION")
        return parser.parse_args()
  
    def cli_describe(self):
        (options, args) = self.get_describe_parser()
        self.cluster_describe(args)
    
    def cluster_describe(self,clusters=None):
        params = {}
        if clusters:
            self.euca.connection.build_list_params(params,clusters,'Name')
        try:
            list = self.euca.connection.get_list('DescribeClusters', params,
                                                 [('euca:item', Cluster)])
            for i in list:
                print i
        except EC2ResponseError, ex:
            self.euca.handle_error(ex)

    def get_register_parser(self):
        self.parser = OptionParser("usage: %prog [options] CLUSTERNAME",
                                   version="Eucalyptus %prog VERSION")
        self.parser.add_option("-H","--host",dest="host",
                               help="Hostname of the cluster.")
        self.parser.add_option("-p","--port",dest="port",type="int",
                               default=8774,
                               help="Port for the cluster.")
        self.parser.add_option("-P","--partition",dest="partition",
                               help="Partition for the cluster.")
        (options,args) = self.parser.parse_args()
        if len(args) != 1:
            s = "ERROR  Required argument CLUSTERNAME is missing or malformed."
            self.error(s)
        else:
            return (options,args)  

    def cli_register(self):
        (options,args) = self.get_register_parser()
        self.register(args[0], options.host,
                      options.port, options.partition, options.port)

    def register(self, partition, name, host, port=8774):
        params = {'Partition':partition,
                  'Name':name,
                  'Host':host,
                  'Port':port}
        try:
            reply = self.euca.connection.get_object('RegisterCluster',
                                                  params,
                                                  BooleanResponse)
            print reply
        except EC2ResponseError, ex:
            self.euca.handle_error(ex)

    def get_deregister_parser(self):
        self.parser = OptionParser("usage: %prog [options] CLUSTERNAME",
                                   version="Eucalyptus %prog VERSION")
        self.parser.add_option("-P","--partition",dest="partition",
                               help="Partition for the cluster.")
        (options,args) = self.parser.parse_args()    
        if len(args) != 1:
            s = "ERROR  Required argument CLUSTERNAME is missing or malformed."
            self.error(s)
        else:
            return (options,args)  
            
    def cli_deregister(self):
        (options,args) = self.get_deregister_parser()
        self.deregister(args[0])

    def deregister(self, name, partition=None):
        params = {'Name':name}
        if partition:
            params['Partition'] = partition
        try:
            reply = self.euca.connection.get_object('DeregisterCluster',
                                                  params,
                                                  BooleanResponse)
            print reply
        except EC2ResponseError, ex:
            self.euca.handle_error(ex)
        
    def get_modify_parser(self):
        self.parser = OptionParser("usage: %prog [options]",
                                   version="Eucalyptus %prog VERSION")
        self.parser.add_option("-p","--property",dest="props",
                               action="append",
                               help="Modify KEY to be VALUE.  Can be given multiple times.",
                               metavar="KEY=VALUE")
        self.parser.add_option("-P","--partition",dest="partition",
                               help="Partition for the cluster.")
        (options,args) = self.parser.parse_args()    
        if len(args) != 1:
            s = "ERROR  Required argument CLUSTERNAME is missing or malformed."
            self.error(s)
        if not options.props:
            self.error("ERROR No options were specified.")
        for i in options.props:
            if not re.match("^[\w.]+=[\w\.]+$", i):
                s = "ERROR Options must be of the form KEY=VALUE.  "
                s += "Illegally formatted option: %s" % i
                self.error(s)
        return (options,args)

    def cli_modify(self):
        (options,args) = self.get_modify_parser()
        self.modify(options.partition,args[0], options.props)

    def modify(self,partition,name,modify_list):
        for entry in modify_list:
            key, value = entry.split("=")
            try:
                params = {'Partition' : partition,
                          'Name' : name,
                          'Attribute' : key,
                          'Value' : value},
                r = self.euca.connection.get_object('ModifyClusterAttribute',
                                                    params,
                                                    BooleanResponse)
                print r
            except EC2ResponseError, ex:
                self.euca.handle_error(ex)

