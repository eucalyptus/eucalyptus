import argparse
from argparse import ArgumentTypeError, ArgumentError
from urlparse import urlparse
import os
import sys
import threading
import subprocess
from io import BytesIO
import logging
from downloadmanifest import DownloadManifest


class DownloadImage(object):
    _chunk_size = 8192

    def __init__(self, **kwargs):
        parser = argparse.ArgumentParser(description=
                                         "Download parts from manifest")
        parser.add_argument('-m', '--manifest', dest='manifest', required=True,
                            help='''Path to 'download-manifest. Use '-' to read
                            manifest from stdin''')
        parser.add_argument('-d', '--dest', dest='destination', required=True,
                            help='''Destination path to write image to.
                            Use '-' for stdout.''')
        parser.add_argument('-k', '--privatekey', dest='privatekey',
                            help='''file containing the private key to decrypt
                            the bundle with.''')
        parser.add_argument('-x', '--xsd', dest='xsd', default=None,
                            help='''Path to 'download-manifest xsd used
                            to validate manfiest xml.''')
        parser.add_argument('--toolspath', dest='toolspath', default=None,
                            help='''Local path to euca2ools.''')
        parser.add_argument('--maxbytes', dest='maxbytes', default=0,
                            help='''Maximum bytes allowed to be written to the
                            destination.''')
        parser.add_argument('--debug', dest='debug', default=False,
                            action='store_true',
                            help='''Enable debug to stdout''')
        parser.add_argument('--logfile', dest='logfile', default=None,
                            help='''log file path to write to''')
        parser.add_argument('--loglevel', dest='loglevel', default='WARNING',
                            help='''log level for output''')
        parser.add_argument('--dumpmanifest', dest='dumpmanifest',
                            help='''Get and show manifest then exit''')


        #Set any kwargs from init to default values for parsed args...
        #Handle the cli arguments...
        arg_list = sys.argv[1:]
        self.parser = parser
        #Handle any kwargs at __init__ ...
        for kwarg in kwargs:
            for key in parser._option_string_actions:
                if parser._option_string_actions[key].dest == str(kwarg):
                    option = parser._option_string_actions[key]
                    arg_value = [option.option_strings[0]]
                    #Is there a better way to handle this for consts?
                    if not option.const:
                        arg_value.append(kwargs[kwarg])
                    arg_list.extend(arg_value)
        self.args = parser.parse_args(arg_list)
        self._setup_logger()
        self.log.debug('Parsed Args: ' + str(self.args))
        self._setup()

    def _setup_logger(self):
        self.log = logging.getLogger(self.__class__.__name__)
        loglevel = str(self.args.loglevel).upper() or 'WARNING'
        fmt = logging.Formatter('%(asctime)s-%(levelname)s:%(name)s(' +
                                str(os.getpid()) + '): %(message)s')
        if self.args.debug:
            loglevel = logging.DEBUG
        elif hasattr(logging, loglevel):
            loglevel = int(getattr(logging, loglevel))
        else:
            raise ArgumentTypeError('Invalid Log level "' + loglevel)
        self.log.setLevel(loglevel)
        if not self.log.handlers:
            if self.args.logfile:
                self.log.handlers.append(
                    logging.FileHandler(self.args.logfile))
            if self.args.debug:
                self.log.handlers.append(logging.StreamHandler(sys.stderr))
        for hndlr in self.log.handlers:
            hndlr.setLevel(loglevel)
            hndlr.setFormatter(fmt)
            self.log.addHandler(hndlr)

    def _setup(self):
        self.log.debug('Starting configure...')
        #Get optional destination directory...
        dest_file = self.args.destination
        if not isinstance(dest_file, file) and not (dest_file == "-"):
            dest_file = os.path.expanduser(os.path.abspath(dest_file))
            self.args.destination = dest_file

        xsd_file = self.args.xsd
        if xsd_file:
            if not isinstance(xsd_file, file):
                xsd_file = os.path.expanduser(os.path.abspath(xsd_file))
                self.args.xsd = xsd_file
        #Read the manifest from src provided into a manifest obj...
        self._get_download_manifest_obj()

    def _read_manifest_from_stdin(self):
        self.log.debug('Reading Manifest from stdin')
        fileobj = BytesIO()
        while True:
            chunk = sys.stdin.read(self._chunk_size)
            if not chunk:
                break
            self.log.debug('Chunk:' + str(chunk))
            fileobj.write(chunk)
        fileobj.flush()
        fileobj.seek(0)
        with fileobj:
            manifest = DownloadManifest._read_from_fileobj(
                manifest_fileobj=fileobj,
                xsd=self.args.xsd,
                key_filename=self.args.privatekey)
        return manifest

    def _read_manifest_from_file(self):
        self.log.debug('Reading from local manifest file')
        manifest_path = os.path.expanduser(os.path.abspath(
            self.args.manifest))
        if not os.path.exists(manifest_path):
            raise ArgumentTypeError("Manifest '{0}' does not exist"
                                    .format(self.args.manifest))
        if not os.path.isfile(manifest_path):
            raise ArgumentTypeError("Manifest '{0}' is not a file"
                                    .format(self.args.manifest))
        #Read manifest into BundleManifest obj...
        return DownloadManifest.read_from_file(
                manifest_path,
                self.args.xsd,
                key_filename=self.args.privatekey)

    def _read_manifest_from_url(self):
        self.log.debug('Reading from remote manifest from url')
        return DownloadManifest.read_from_url(
                manifest_url=self.args.manifest,
                xsd=self.args.xsd,
                key_filename=self.args.privatekey)

    def _get_download_manifest_obj(self):
        self.log.debug('Create DownloadManifest obj from the manifest '
                       'argument...')
        manifest = self.args.manifest
        if manifest:
            if not isinstance(manifest, DownloadManifest):
                if manifest == '-':
                    self.args.manifest = self._read_manifest_from_stdin()
                else:
                    #see if manifest is a url or local path
                    try:
                        parsed_url = urlparse(str(manifest))
                    except Exception as pe:
                        self.log.debug('Error parsing manifest argument as'
                                       ' url, trying local path:' + str(pe))
                    if not parsed_url or not parsed_url.scheme:
                        self.args.manifest = self._read_manifest_from_file()
                    else:
                        self.log.debug('Reading from remote manifest from url')
                        #For now limit urls to http(s)...
                        if not parsed_url.scheme in ['http', 'https']:
                            raise ArgumentTypeError('Manifest url only '
                                                    'supports http, https at '
                                                    'this time')
                        self.args.manifest = self._read_manifest_from_url()
        else:
            raise argparse.ArgumentError(None, 'Manifest is required (-m)')
        return self.args.manifest

    @classmethod
    def _open_pipe_fileobjs(cls):
        pipe_r, pipe_w = os.pipe()
        return os.fdopen(pipe_r), os.fdopen(pipe_w, 'w')

    def _download_parts_to_fileobj(self, manifest, dest_fileobj):
        bytes = 0
        for part in manifest.image_parts:
            self.log.debug('Downloading part:' + str(part.get_url))
            bytes += part.download(dest_fileobj=dest_fileobj) or 0
        return bytes

    def _download_to_unbundlestream(self,
                                    manifest=None,
                                    tools_path=None):
        download_r = None
        download_w = None
        unbundle_ps = None
        bytes = 0
        manifest = manifest or self.args.manifest
        if tools_path is None:
            tools_path = self.args.toolspath or ""
        unbundle_tool_path = tools_path+'euca-unbundlestream'
        unbundle_ps_args = [unbundle_tool_path,
                            '-e', str(manifest.enc_key),
                            '-v', str(manifest.enc_iv),
                            '-d', str(self.args.destination),
                            '--maxbytes', str(self.args.maxbytes)]
        if self.args.debug:
            unbundle_ps_args.append('--debug')
        try:
            unbundle_ps = subprocess.Popen(unbundle_ps_args)
            
            download_r, download_w = self._open_pipe_fileobjs()
            unbundle_ps.stdin = download_r
            bytes = self._download_parts_to_fileobj(manifest=manifest,
                                                    dest_fileobj=download_w)
        finally:
            try:
                unbundle_ps.terminate()
            except:
                pass
            msg = 'Wrote "' + str(bytes) + '" to unbundlestream'
            if bytes:
                self.log.debug(msg)
            else:
                self.log.critical(msg)
            if download_r:
                download_r.close()
            if download_w:
                download_w.close()

    def main(self):
        manifest = self.args.manifest
        if self.args.dumpmanifest:
            print str(manifest)
            return
        self.log.debug('\nMANIFEST:' + str(manifest))
        dest_file = self.args.destination
        if manifest.file_format == 'BUNDLE':
            if not self.args.privatekey:
                raise ArgumentError(self.args.privatekey,
                                    'Bundle type needs privatekey -k')
            return self._download_to_unbundlestream(manifest=manifest)
        else:
            if dest_file == "-":
                dest_file_name = '<stdout>'
                dest_file = os.fdopen(os.dup(os.sys.stdout.fileno()), 'w')
            else:
                dest_file_name = str(dest_file)
                dest_file = open(dest_file, 'w')
            with dest_file:
                    self._download_parts_to_fileobj(manifest=manifest,
                                                    dest_fileobj=dest_file)


if __name__ == '__main__':
    DownloadImage().main()
