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
import time
from requests.exceptions import HTTPError, Timeout, ConnectionError
from cryptoutils import _get_digest_algorithm_from_string
from eucatoolkit import stages


class DownloadPart(object):

    def __init__(self,
                 get_url,
                 part_index,
                 chunk_size=None,
                 digest_algorithm='MD5',
                 digest=None,
                 max_buffer_size=None,
                 logger=None):
        self.get_url = str(get_url)
        self.debug_url = str(self.get_url).split('?')[0] # rem sensitive info
        self.part_index = part_index
        self.chunk_size = chunk_size or stages._chunk_size
        self.digest_algorithm = digest_algorithm
        self.digest = digest
        self.log = logger or stages.get_logger()
        self.max_buffer_size = max_buffer_size or stages._max_part_buffer_size
        self.written_digest = None
        self.bytes_written = 0

    def download(self,
                 dest_fileobj,
                 chunk_size=None,
                 connection_timeout=30,
                 max_attempts=2,
                 max_buffer_size=None):
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
        :param max_buffer_size: size in bytes
        returns bytes downloaded/written
        '''
        attempt = 0
        response = None
        max_buffer_size = max_buffer_size or self.max_buffer_size
        self.log.debug('Downloading part:{0}'.format(self.debug_url))
        chunk_size = chunk_size or self.chunk_size
        digest = _get_digest_algorithm_from_string(self.digest_algorithm)
        #Attempt get request to get_url...
        while attempt < max_attempts:
            attempt += 1
            part_buffer = ""
            content_length = None
            bytes = 0
            self.written_digest = None
            response = self._get_respsonse(
                url=self.get_url,
                max_attempts=max_attempts,
                connection_timeout=connection_timeout)
            try:
                content_length = response.headers.get('content-length', None)
                if content_length is not None:
                    content_length = long(content_length)
                # If the size of the part is less than the max buffer size,
                # store the contents in a temp buffer to allow the download to be
                # be retried in the case of a failure before potentially
                # writing bad data to an unbundle pipeline
                if content_length <= max_buffer_size:
                    for chunk in response.iter_content(chunk_size):
                        part_buffer += chunk
                        digest.update(chunk)
                        bytes += len(chunk)
                else:
                    # The remote part is bigger than the allowed buffer size
                    # Begin writing directly to destination fileobj
                    for chunk in response.iter_content(chunk_size):
                        dest_fileobj.write(chunk)
                        digest.update(chunk)
                        bytes += len(chunk)
                    dest_fileobj.flush()
                if bytes != content_length:
                    raise ValueError(
                        'Part:"{0}". Expected Size:"{1}", Downloaded:"{2}"'
                        .format(self.part_index, content_length, bytes))
                self.bytes_written = bytes
                # Download is complete validate digest
                self.written_digest = self._validate_written_digest(
                    response=response,
                    digest=digest)
                # If checksum was valid, write part to destination
                if part_buffer:
                    dest_fileobj.write(part_buffer)
                    dest_fileobj.flush()
            except ValueError as VE:
                if attempt <= max_attempts:
                    raise
                else:
                    self.log.warn(
                        'Attempt:{0}/{1}, {2}'
                        .format(attempt, max_attempts, str(VE)))
                # Back off and retry
                time.sleep(10)
            else:
                return bytes

    def _validate_written_digest(self, response, digest):
        # Record the hex digest string
        written_digest = str(digest.hexdigest())
        # Check the written/downloaded digest vs the original part digest...
        if self.digest:
            # The digest was provided at __init__ ...
            if self.digest != written_digest:
                raise ValueError(
                    'Part:"{0}". Expected Digest:"{1}" Written Digest:{2}'
                    .format(self.part_index, self.digest, written_digest))
        # If the digest algorithm is md5 try to use the header content...
        elif digest.name == 'md5' and hasattr(response, 'headers'):
            # Attempt to get the digest from the http headers...
            content_md5 = response.headers.get('content_md5', None)
            if content_md5:
                content_md5 = str(content_md5).strip('"').strip()
                if (written_digest and
                        (str(written_digest) != str(content_md5))):
                    raise ValueError(
                        'Part:"{0}". Expected Digest:"{1}" != '
                        'Written Digest:{2}'
                        .format(self.part_index,
                                content_md5,
                                written_digest))
                else:
                    # Digest was verified log for debugging purposes
                    self.log.debug(
                        'Part:"{0}". Expected Digest:"{1}" Written Digest:{2}'
                        .format(self.part_index,
                                content_md5,
                                written_digest))
            elif 'etag' in response.headers:
                # Try to compare with etag
                # Since etag is not a reliable md5 checksum, just warn here
                etag = str(response.headers.get('etag')).strip('"').strip()
                etag_msg = ('Part:"{0}". etag:"{1}" vs Written Digest:"{2}"'
                            .format(self.part_index,
                                    etag,
                                    written_digest))
                if written_digest != etag:
                    err_msg = etag_msg + ", etag does not equal written digest"
                    # If the etag was 32 hex chars assume this is the md5 sum
                    # and raise the error, if not just log a warning
                    if len(etag) == 32:
                        try:
                            int(etag, 16)
                        except ValueError:
                            pass
                        else:
                            raise ValueError(err_msg)
                    self.log.warn(err_msg)
                else:
                    # Digest was verified log for debugging purposes
                    self.log.debug(etag_msg + ", etag == digest")
        return written_digest

    def _get_respsonse(self, url, max_attempts, connection_timeout):
        attempt = 0
        response = None
        # Attempt get request to get_url...
        while not response and attempt < max_attempts:
            attempt += 1
            try:
                response = requests.get(self.get_url,
                                        stream=True,
                                        timeout=connection_timeout)
                try:
                    response.raise_for_status()
                except HTTPError as HE:
                    http_err = str("{0}, get() err, attempt:{1}/{2}, err:\n{3}"
                                   .format(str(self),
                                           str(attempt),
                                           str(max_attempts),
                                           str(HE.message)))
                    self.log.warn(http_err)
                    if attempt >= max_attempts:
                        HE.args = [http_err]
                        raise HE
                    response = None
                    continue
            except (Timeout, ConnectionError) as CE:
                # If there was a connection error retry up max_attempts
                if attempt >= max_attempts:
                    err_msg = str("{0}, Connection failure. Attempts:{1}/{2}"
                                  .format(str(self),
                                          str(attempt),
                                          str(max_attempts)))
                    self.log.warn(err_msg)
                    CE.args = [err_msg + "\n" + str(CE.message)]
                    raise CE
            # Back off and retry
            if not response:
                time.sleep(5)
        return response

    def __repr__(self):
        return 'DownloadPart({0}, {1} )'.format(
            repr(self.part_index), repr(self.debug_url))
