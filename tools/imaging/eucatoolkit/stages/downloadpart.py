__author__ = 'clarkmatthew'

import requests
from requests.exceptions import HTTPError
import hashlib

class DownloadPart(object):
    def __init__(self,
                 get_url,
                 part_index,
                 chunk_size=8192,
                 digest_algorithm='SHA1'):
        self.get_url = get_url
        self.part_index = part_index
        self.chunk_size = chunk_size
        self.digest_algorithm = 'SHA1'
        self.written_digest = None
        self.bytes_written = 0

    def download(self, dest_fileobj, chunk_size=None):
        bytes = 0
        chunk_size = chunk_size or self.chunk_size
        if self.digest_algorithm:
            if self.digest_algorithm.upper() == 'SHA1':
                digest = hashlib.sha1()
            elif self.digest_algorithm.upper() == 'MD5':
                digest = hashlib.md5()
        r = requests.get(self.get_url, stream=True)
        try:
            r.raise_for_status()
        except HTTPError as HE:
            HE.args = [str(self) + "\n" + str(HE.message)]
            raise HE
        for chunk in r.iter_content(chunk_size):
            dest_fileobj.write(chunk)
            digest.update(chunk)
            bytes += len(chunk)
            self.bytes_written = bytes
        dest_fileobj.flush()
        self.written_digest = str(digest.hexdigest())
        return bytes

    def __repr__(self):
        return 'DownloadPart({0}, {1} )'.format(
            repr(self.part_index), repr(self.get_url))
