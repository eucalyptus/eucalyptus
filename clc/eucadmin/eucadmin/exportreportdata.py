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

from StringIO import StringIO
from eucadmin.patched_gzip import GzipFile
import zlib
from boto.roboto.param import Param
from eucadmin.reportsrequest import ReportsRequest, ReportsServiceClass
from xml.sax.saxutils import XMLGenerator, XMLFilterBase
import os, sys, xml.sax, xml.sax.handler, boto

class GzipReportsServiceClass(ReportsServiceClass):
    def build_base_http_request(self, method, path, auth_path,
                                params=None, headers=None, data='', host=None):
        headers = { 'Accept-Encoding': 'gzip' }
        return super(GzipReportsServiceClass, self)\
        .build_base_http_request( method, path, auth_path, params, headers, data, host )

class ExportReportData(ReportsRequest):
    ServiceClass = GzipReportsServiceClass
    Description = 'Export reporting data'

    Params = [
        Param(name='Start',
            short_name='s', long_name='start-date',
            ptype='datetime',
            doc='the inclusive start date for the exported data (e.g. 2012-08-19)'),
        Param(name='End',
            short_name='e', long_name='end-date',
            ptype='datetime',
            doc='the exclusive end date for the exported data (e.g. 2012-08-26)'),
        Param(name='Dependencies',
            short_name='d', long_name='dependencies',
            ptype='boolean',
            doc='include event dependencies from outside the requested period'),
        Param(name='Force',
            short_name='F', long_name='force',
            ptype='boolean', request_param=False,
            doc='overwrite output file if it exists'),
        ]
    Args = [
        Param(name='File', long_name='file',
            ptype='string', optional=True, request_param=False,
            doc='optional path to the resulting reporting data export file'),
        ]

    def check_export_file(self):
        if self.file is not None and os.path.exists(self.file) and not self.force:
            msg = 'file %s already exists, ' % self.file
            msg += 'please remove and try again'
            raise IOError(msg)

    def cli_formatter(self, data):
        if self.file is not None:
            print 'Exported data to ' + self.file

    # Override send to handle large response streaming and compression
    def send(self, verb='GET', **args):
        self.process_args(**args)
        self.process_filters()
        conn = self.get_connection(**self.connection_args)
        self.http_response = conn.make_request(self.name(),
            self.request_params,
            verb=verb)
        if self.http_response.status == 200:
            # Handle content encoding
            if self.http_response.getheader( 'Content-Encoding', 'identity' ) == 'gzip':
                source = GzipFile(fileobj=self.http_response)
            else:
                source = self.http_response
            # Process response without reading it all into memory
            if self.file is not None:
                f = open( self.file, 'w')
                self.body = self.write_export( f, source )
                f.close()
            else:
                self.body = self.write_export( sys.stdout, source )
            # Process non-export content in regular way
            boto.log.debug(self.body)
            self.aws_response = boto.jsonresponse.Element(list_marker=self.list_markers,
                item_marker=self.item_markers)
            h = boto.jsonresponse.XmlHandler(self.aws_response, self)
            self.parse(h, StringIO(self.body))
            return self.aws_response
        else:
            boto.log.error('%s %s' % (self.http_response.status,
                                      self.http_response.reason))
            boto.log.error('%s' % self.body)
            raise conn.ResponseError(self.http_response.status,
                self.http_response.reason,
                self.body)

    def parse(self, handler, data ):
        parser = xml.sax.make_parser()
        parser.setFeature(xml.sax.handler.feature_namespaces, 1)
        parser.setContentHandler(handler)
        parser.setErrorHandler(handler)
        input_source = xml.sax.InputSource()
        input_source.setByteStream(data)
        parser.parse(input_source)

    def write_export(self, file, source):
        generator = ExportExtractor(file)
        self.parse(generator, source)
        return generator.getReply()

    def process_args(self, **args):
        super(ExportReportData, self).process_args( **args )
        self.file = self.args['file']
        self.force = self.args['force']
        self.process_date_param('Start')
        self.process_date_param('End')
        self.check_export_file()

class ExportExtractor(XMLFilterBase):
    def __init__(self, file):
        XMLFilterBase.__init__(self)
        self.generator = XMLGenerator(file, 'UTF-8')
        self.generator.startPrefixMapping(u'', u'http://www.eucalyptus.com/ns/reporting/export/2012-08-24/')
        self.replyData = StringIO()
        self.replyGenerator = XMLGenerator( self.replyData, 'UTF-8' )
        self.switchTarget( self.replyGenerator )

    def startDocument(self):
        self.generator.startDocument()
        XMLFilterBase.startDocument(self)

    def endElementNS(self, name, qname):
        XMLFilterBase.endElementNS(self, name, qname)
        namespace, element = name
        if namespace == u'http://www.eucalyptus.com/ns/reporting/export/2012-08-24/' and element == u'Export':
            self.switchTarget( self.replyGenerator )

    def startElementNS(self, name, qname, attrs):
        namespace, element = name
        if namespace == u'http://www.eucalyptus.com/ns/reporting/export/2012-08-24/' and element == u'Export':
            self.switchTarget( self.generator )
        XMLFilterBase.startElementNS(self, name, qname, attrs)

    def switchTarget(self, target):
        self._cont_handler = target
        self._dtd_handler = target
        self._ent_handler = target
        self._err_handler = target

    def getReply(self):
        return self.replyData.getvalue()
