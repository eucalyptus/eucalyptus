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
import lxml.etree
import lxml.objectify
import os
import requests
from io import BytesIO
from downloadpart import DownloadPart
from eucatoolkit import stages
from eucatoolkit.stages import get_logger
from cryptoutils import _decrypt_hex_key, _verify_signature


class DownloadManifest(object):

    def __init__(self, loglevel=None):
        #self._log = logging.getLogger(stages._logger_name)
        self.version = None
        self.file_format = None
        self.key_filename = None
        self.sig_key_filename = None
        self.enc_key = None
        self.enc_iv = None
        self.download_image_size = None
        self.unbundled_image_size = None
        self.expected_size = None
        self.part_count = None
        self.image_parts = []
        self.signature = None
        self.signature_algorithm = None
        self.computed_signature = None
        self.log = get_logger()
        self._chunk_size = stages._chunk_size

    @classmethod
    def _validate_manifest_xsd(cls, manifest_fileobj, xsd_fileobj):
        '''
        Attemps to valide the xml provided in 'manifest_fileobj' against
        xsd provided in 'xsd_fileobj'.
        :param manifest_fileobj: file like obj containing xml to validate
        :param xsd_fileobj: file like obj containing xsd used to validate
        '''
        xsd_doc = lxml.etree.parse(xsd_fileobj)
        xsd_schema = lxml.etree.XMLSchema(xsd_doc)
        xml_doc = lxml.etree.parse(manifest_fileobj)
        xsd_schema.assertValid(xml_doc)

    @classmethod
    def read_from_file(cls,
                       manifest_path,
                       xsd=None,
                       key_filename=None,
                       sig_key_filename=None):
        '''
        Reads xml from local file path 'manifest_path' and attempts to
        convert and validate the resulting DownloadManifest obj.
        :param manifest_path: local file path to read manifest xml from
        :param xsd: local file path to xsd used to validate xml
        :param key_filename: optional path to key used to decrypt manifest
                             fields/values.
        :returns DownloadManifest obj.
        '''
        xsd_fileobj = None
        manifest_fileobj = None
        manifest_path = os.path.expanduser(os.path.abspath(manifest_path))
        if not os.path.isfile(manifest_path):
            raise ValueError("Manifest '{0}' file not accessible"
                             .format(manifest_path))
        if xsd:
            if not isinstance(xsd, file):
                xsd = open(xsd)
        try:
            manifest_fileobj = open(manifest_path)
            return cls._read_from_fileobj(manifest_fileobj,
                                          xsd=xsd,
                                          key_filename=key_filename,
                                          sig_key_filename=sig_key_filename)
        finally:
            if xsd_fileobj:
                xsd_fileobj.close()
            if manifest_fileobj:
                manifest_fileobj.close()

    @classmethod
    def read_from_url(cls,
                      manifest_url,
                      chunk_size=None,
                      xsd=None,
                      key_filename=None,
                      sig_key_filename=None):
        '''
        Reads xml at the provided 'manifest_url' and attempts to convert and
        validate the resulting DownloadManifest.
        :param manifest_url: URL to download the 'download manifest xml'
        :param chunksize: # of bytes to read/write per read()/write()
        :param xsd: local file path to xsd used to validate xml
        :param key_filename: optional path to key used to decrypt manifest
                             fields/values.
        :returns DowloadManifest obj.
        '''
        manifest_url = str(manifest_url)
        chunk_size = chunk_size or stages._chunk_size
        fileobj = BytesIO()
        r = requests.get(manifest_url, stream=True)
        r.raise_for_status()
        for chunk in r.iter_content(chunk_size):
            fileobj.write(chunk)
        fileobj.flush()
        fileobj.seek(0)
        return cls._read_from_fileobj(manifest_fileobj=fileobj,
                                      xsd=xsd,
                                      key_filename=key_filename,
                                      sig_key_filename=sig_key_filename)

    @classmethod
    def _read_from_fileobj(cls,
                           manifest_fileobj,
                           xsd=None,
                           key_filename=None,
                           sig_key_filename=None):
        '''
        Main method used to parse and create a downloadmanifest obj from the
        xml provided in 'manifest_fileobj'.
        :param manifest_fileobj: fileobj to read download manifest xml from
        :param xsd: optional localpath to xsd used to validate xml
        :param key_filename: optional path to key used to decrypt manifest
                             fields/values.
        returns DownloadManifest obj.
        '''
        #if the xsd was provided use it to validate this manifest xml
        if xsd is not None:
            cls._validate_manifest_xsd(manifest_fileobj, xsd)
        xml = lxml.objectify.parse(manifest_fileobj).getroot()
        manifest = cls()
        manifest.key_filename = key_filename
        manifest.sig_key_filename = sig_key_filename
        manifest.version = xml.version.text.strip()
        manifest.signature = xml.signature.text.strip()
        manifest.signature_algorithm = xml.signature.get('algorithm').strip()
        #Raw, Bundle, etc...
        manifest.file_format = xml.find('file-format').text.strip()
        #If the key was provided, validate manifest signature...
        data_to_sign = lxml.etree.tostring(xml.version)
        data_to_sign += lxml.etree.tostring(xml.__getattr__('file-format'))
        if manifest.file_format == 'BUNDLE':
            data_to_sign += lxml.etree.tostring(xml.bundle)
        data_to_sign += lxml.etree.tostring(xml.image)
        manifest.data_to_sign = data_to_sign

        if sig_key_filename:
            data_to_sign = lxml.etree.tostring(xml.version)
            data_to_sign += lxml.etree.tostring(xml.__getattr__('file-format'))
            if manifest.file_format == 'BUNDLE':
                data_to_sign += lxml.etree.tostring(xml.bundle)
            data_to_sign += lxml.etree.tostring(xml.image)
            _verify_signature(cert_filename=sig_key_filename,
                              signature=manifest.signature,
                              data=data_to_sign,
                              algorithm=manifest.signature_algorithm)
        #If this manifest contains a bundle get and decrypt keys,iv...
        if manifest.file_format == 'BUNDLE':
            bundle = xml.bundle
            manifest.unbundled_image_size = long(bundle.find('unbundled-size'))
            manifest.enc_key = bundle.find('encrypted-key').text.strip()
            manifest.enc_iv = bundle.find('encrypted-iv').text.strip()
            if key_filename:
                manifest.enc_key = _decrypt_hex_key(
                    manifest.enc_key, key_filename=key_filename)
                manifest.enc_iv = _decrypt_hex_key(
                    manifest.enc_iv, key_filename=key_filename)
        #Get the size of the image to be downloaded, pre-unbundle
        manifest.download_image_size = long(xml.image.size)
        #Get/create part information...
        for part in xml.image.parts.iter(tag='part'):
            manifest.image_parts.append(None)
        for xml_part in xml.image.parts.iter(tag='part'):
            part_index = int(xml_part.get('index'))
            get_url = xml_part.find('get-url').text.strip()
            digest = xml_part.find('digest')
            #Default to MD5 to use http header content if digest not present
            digest_algorithm = 'MD5'
            if digest:
                digest_algorithm = str(digest.get('algorithm'))
            manifest.image_parts[part_index] = DownloadPart(
                get_url=get_url,
                part_index=part_index,
                digest=digest,
                digest_algorithm=digest_algorithm)
        #Part count may/may not be included. Otherwise will be derived
        partcount = xml.image.parts.get('count')
        if partcount is not None:
            manifest.part_count = int(partcount)
            if len(manifest.image_parts) != manifest.part_count:
                raise ValueError(
                    'Part count {0} does not equal parts found:{1}'
                    .format(len(manifest.image_parts)), manifest.part_count)
        else:
            manifest.part_count = len(manifest.image_parts)
        for index, part in enumerate(manifest.image_parts):
            if part is None:
                raise ValueError('part {0} must not be None'.format(index))
        return manifest

    def get_part_by_index(self, index):
        for part in self.image_parts:
            if part.part_index == index:
                return part
        return None

    def __repr__(self):
        buf = ""
        for key in self.__dict__:
            if isinstance(self.__dict__[key], list):
                buf += "\n" + str(key) + " -->:"
                for item in self.__dict__[key]:
                    buf += "\n\t" + str(item)
            else:
                buf += "\n" + str(key) + " -->: " + str(self.__dict__[key])
        return buf
