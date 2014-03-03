#!/usr/bin/python -tt

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
import requests
from requests.exceptions import HTTPError, Timeout, ConnectionError
import hashlib
from eucatoolkit import stages


class DownloadPart(object):

    def __init__(self,
                 get_url,
                 part_index,
                 chunk_size=None,
                 digest_algorithm='MD5',
                 digest=None,
                 logger=None):
        self.get_url = str(get_url)
        self.part_index = part_index
        self.chunk_size = chunk_size or stages._chunk_size
        self.digest_algorithm = digest_algorithm
        self.digest = digest
        self.log = logger or stages.get_logger()
        self.written_digest = None
        self.bytes_written = 0

    def download(self,
                 dest_fileobj,
                 chunk_size=None,
                 connection_timeout=30,
                 max_attempts=2):
        '''
        Attempts to download/get() self.get_url and write to 'dest_fileobj'
        Attempts to validate the downloaded content against self.digest using
        self.digest_algorithm. If self.digest is not provided and attempt
        to compare the calcluated digest against content_md5 or the etag is
        done. Failure to match self.digest or content_md5 will result in
        an error, while failure to match the etag will only print a warning.
        :param dest_fileobj: the file like object to write/download to
        :param chunk_size: the size to read/write per iteration
        :param connection_timeout: int timeout for establishing connection
        :param max_attempts: attempts to connect upon failure
        returns bytes downloaded/written
        '''
        bytes = 0
        attempt = 0
        response = None
        self.log.debug('Downloading part:{0}'.format(self.get_url))
        chunk_size = chunk_size or self.chunk_size
        if self.digest_algorithm:
            if self.digest_algorithm.upper() == 'SHA1':
                digest = hashlib.sha1()
            elif self.digest_algorithm.upper() == 'MD5':
                digest = hashlib.md5()
        #Attempt get request to get_url...
        while not response and attempt < max_attempts:
            attempt += 1
            try:
                response = requests.get(self.get_url,
                                        stream=True,
                                        timeout=connection_timeout)
            except (Timeout, ConnectionError) as CE:
                #If there was a connection error retry up max_attempts
                if attempt >= max_attempts:
                    err_msg = str("{0}, Connection failure. Attempts:{1}"
                                  .format(str(self), str(attempt)))
                    self.log(err_msg)
                    CE.args = [err_msg + "\n" + str(CE.message)]
                    raise CE
        try:
            response.raise_for_status()
        except HTTPError as HE:
            HE.args = [str(self) + "\n" + str(HE.message)]
            raise HE
        #Begin writing to destination fileobj
        for chunk in response.iter_content(chunk_size):
            dest_fileobj.write(chunk)
            digest.update(chunk)
            bytes += len(chunk)
            self.bytes_written = bytes
        dest_fileobj.flush()
        #Download is complete record checksum
        self.written_digest = str(digest.hexdigest())
        #Check the written/downloaded digest vs the original part digest...
        if self.digest:
            #The digest was provided at __init__ ...
            if self.digest != self.written_digest:
                raise ValueError(
                    'Part:"{0}". Expected Digest:"{1}" Written Digest:{2}'
                    .format(self.part_index, self.digest, self.written_digest))
        #If the digest algorithm is md5 try to use the header content...
        elif (digest.name == 'md5' and hasattr(response, 'headers')):
            #Attempt to get the digest from the http headers...
            if 'Content-MD5' in response.headers:
                cont_md5 = str(response.headers.get('content_md5')).strip('"')
                cont_md5 = cont_md5.strip()
                if (self.written_digest and
                        (str(self.written_digest) != str(cont_md5))):
                    raise ValueError(
                        'Part:"{0}". Expected Digest:"{1}" != '
                        'Written Digest:{2}'
                        .format(self.part_index,
                                cont_md5,
                                self.written_digest))
                else:
                    self.log.debug(
                        'Part:"{0}". Expected Digest:"{1}" Written Digest:{2}'
                        .format(self.part_index,
                                cont_md5,
                                self.written_digest))
            elif 'etag' in response.headers:
                #Try to compare with etag
                #Since etag is not a reliable md5 checksum, just warn here
                etag = str(response.headers.get('etag')).strip('"').strip()
                etag_msg = ('Part:"{0}". etag:"{1}" vs Written Digest:"{2}"'
                            .format(self.part_index,
                                    etag,
                                    self.written_digest))
                if self.written_digest != etag:
                    self.log.warn(etag_msg + ", etag != digest")
                else:
                    self.log.debug(etag_msg + ", etag == digest")
        return bytes

    def __repr__(self):
        return 'DownloadPart({0}, {1} )'.format(
            repr(self.part_index), repr(self.get_url))
