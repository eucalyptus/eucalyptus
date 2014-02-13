__author__ = 'clarkmatthew'

import requests
from requests.exceptions import HTTPError


class DownloadPart(object):
    def __init__(self,
                 get_url,
                 part_index,
                 chunk_size=8192):
        self.get_url = get_url
        self.part_index = part_index
        self.chunk_size = chunk_size

    def download(self, dest_fileobj, chunk_size=None):
        bytes = 0
        chunk_size = chunk_size or self.chunk_size
        r = requests.get(self.get_url, stream=True)
        try:
            r.raise_for_status()
        except HTTPError as HE:
            HE.args = [str(self) + "\n" + str(HE.message)]
            raise HE
        for chunk in r.iter_content(chunk_size):
            dest_fileobj.write(chunk)
            bytes += len(chunk)
        dest_fileobj.flush()
        return bytes

    def __repr__(self):
        return 'DownloadPart({0}, {1} )'.format(
            repr(self.part_index), repr(self.get_url))
