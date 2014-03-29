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
import argparse
from argparse import ArgumentTypeError, ArgumentError
from urlparse import urlparse
import os
import sys
import subprocess
import traceback
from io import BytesIO
from downloadmanifest import DownloadManifest
from processutils import close_all_fds, open_pipe_fileobjs, spawn_process
from processutils import monitor_subprocess_io, wait_process_in_thread
from eucatoolkit import stages


class DownloadImage(object):

    def __init__(self, dest_file=None, **kwargs):
        parser = argparse.ArgumentParser(description=
                                         "Download parts from manifest")
        parser.add_argument('-m', '--manifest', dest='manifest', required=True,
                            help='''Path to 'download-manifest. Use '-' to read
                            manifest from stdin''')
        parser.add_argument('-d', '--dest', dest='destination',
                            help='''Destination path to write image to.
                            Use '-' for stdout.''')
        parser.add_argument('-k', '--privatekey', dest='privatekey',
                            help='''file containing the private key to decrypt
                            the bundle with.''')
        parser.add_argument('-c', '--cloudcert', dest='cloudcert',
                            required=True,
                            help='''file containing the cloud cert used
                            to verify manifest signature.''')
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
                            help='''Enable debug to a log file''')
        parser.add_argument('--logfile', dest='logfile', default=None,
                            help='''log file path to write to''')
        parser.add_argument('--loglevel', dest='loglevel', default='INFO',
                            help='''log level for output''')
        parser.add_argument('--dumpmanifest', dest='dumpmanifest',
                            action='store_true', default=False,
                            help='''Get and show manifest then exit''')
        parser.add_argument('--reportprogress', dest='reportprogress',
                            default=False, action='store_true',
                            help='''Output progress information to stderr''')
        #Set any kwargs from init to default values for parsed args
        #Handle the cli arguments...
        if not kwargs:
            arg_list = sys.argv[1:]
        else:
            arg_list = []
        self.parser = parser
        #Handle any kwargs at __init__  assign to argparse...
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
        if dest_file is not None:
            self.args.destination = dest_file
        if self.args.destination == "-":
            force_stderr = True
        else:
            force_stderr = False
        self.log = stages.get_logger(self.args.loglevel,
                                     logfile=self.args.logfile,
                                     force_stderr=force_stderr,
                                     debug=self.args.debug)
        self.log.debug('Parsed Args: ' + str(self.args))
        self._setup()

    def _setup(self):
        '''
        Basic setup of this module from args provided at init.
        '''
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
        if not self.args.cloudcert:
            raise argparse.ArgumentError(self.args.cloudcert,
                                         "Cloud cert must be provided to "
                                         "verify manifest signature")
        #Read the manifest from src provided into a manifest obj...
        self._get_download_manifest_obj()

    def _read_manifest_from_stdin(self, read_fileobj=None, chunk_size=None):
        '''
        Attempts to read xml provided to stdin and convert it to a
        downloadmanifest obj.
        :returns downloadmanifest obj.
        '''
        chunk_size = chunk_size or stages._chunk_size
        read_fileobj = read_fileobj or sys.stdin
        self.log.debug('Reading Manifest from stdin')
        fileobj = BytesIO()
        while True:
            chunk = read_fileobj.read(chunk_size)
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
                key_filename=self.args.privatekey,
                sig_key_filename=self.args.cloudcert)
        return manifest

    def _read_manifest_from_file(self, filepath=None):
        '''
        Attempts to read xml contiained at localfile path and convert it to a
        downloadmanifest obj.
        :returns downloadmanifest obj.
        '''
        filepath = filepath or self.args.manifest
        self.log.debug('Reading from local manifest file:' + str(filepath))
        #Read manifest into BundleManifest obj...
        return DownloadManifest.read_from_file(
            filepath,
            self.args.xsd,
            key_filename=self.args.privatekey,
            sig_key_filename=self.args.cloudcert)

    def _read_manifest_from_url(self, url=None):
        '''
        Attempts to read xml provided at the provided url and convert it to a
        downloadmanifest obj.
        :returns downloadmanifest obj.
        '''
        url = url or self.args.manifest
        self.log.debug('Reading from remote manifest from url: ' + str(url))
        return DownloadManifest.read_from_url(
            manifest_url=url,
            xsd=self.args.xsd,
            key_filename=self.args.privatekey,
            sig_key_filename=self.args.cloudcert)

    def _get_download_manifest_obj(self, manifest_input=None):
        '''
        Helper method to return a downloadmanifest obj by determining which
        format the manifest input was provided; stdin, filepath, or url. The
        manifest is stored in self.args.manifest and returned as a result.
        :param manifest_input: filepath, fileobj, URL, or downloadmanifest obj
        :returns downloadmanifest
        '''
        self.log.debug('Create DownloadManifest obj from the manifest '
                       'argument...')
        manifest = manifest_input or self.args.manifest
        if manifest:
            if not isinstance(manifest, DownloadManifest):
                if manifest == '-' or isinstance(DownloadManifest, file):
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
                        # Reading from remote manifest from url
                        # For now limit urls to http(s)...
                        if not parsed_url.scheme in ['http', 'https']:
                            raise ArgumentTypeError('Manifest url only '
                                                    'supports http, https at '
                                                    'this time')
                        self.args.manifest = self._read_manifest_from_url()
        else:
            raise argparse.ArgumentError(None, 'Manifest is required (-m)')
        return self.args.manifest

    def _download_parts_to_fileobj(self, manifest, dest_fileobj):
        '''
        Attempts to iterate through all parts contained in 'manifest' and
        download and concatenate each part to 'dest_fileobj'. If the
        manifest contains the intended image size, and the resulting bytes
        downloaded does not match this size, ValueError is raised.
        :param manifest: downloadmanifest obj
        :param dest_fileobj: file like object to write downloaded parts to
        :returns bytes downloaded
        '''
        bytes = 0
        for part_index in xrange(0, manifest.part_count):
            part = manifest.get_part_by_index(part_index)
            self.log.debug('Downloading part#:' + str(part.part_index))
            bytes += part.download(dest_fileobj=dest_fileobj) or 0
            self.log.debug('Wrote bytes:' + str(bytes) + "/"
                           + str(manifest.download_image_size) + ", digest:"
                           + str(part.written_digest))
            if self.args.reportprogress:
                stages.report_status('"bytes_downloaded":%d' % bytes)
        if manifest.download_image_size is not None:
            if bytes != manifest.download_image_size:
                raise ValueError('Bytes Downloaded:"{0}" does not equal '
                                 'manifest image size:"{1}"'
                                 .format(bytes, manifest.download_image_size))
        return bytes

    def _download_parts_pipe_wrapper(self,
                                     manifest,
                                     dest_fileobj,
                                     close_fd_excludes=[]):
        close_fd_excludes.extend([dest_fileobj])
        close_all_fds(close_fd_excludes)
        return self._download_parts_to_fileobj(manifest=manifest,
                                               dest_fileobj=dest_fileobj)

    def _download_to_unbundlestream(self,
                                    dest_fileobj,
                                    manifest=None,
                                    tools_path=None,
                                    inactivity_timeout=120):
        '''
        Attempts to iterate through all parts contained in 'manifest' and
        download and concatenate each part to euca2ools unbundle stream.
        :params manifest: downloadmanifest obj
        :tools_path: optional path to euca2ools euca-bundle-stream cmd
        '''
        download_r = None
        download_w = None
        monitor_w = None
        monitor_r = None
        unbundle_ps = None
        download_ps = None
        wait_threads = []
        if not dest_fileobj or isinstance(dest_fileobj, basestring):
            raise AttributeError('Dest fileobj must be file like obj, value:'
                                 '"{0}"'.format(str(dest_fileobj)))
        manifest = manifest or self.args.manifest
        if tools_path is None:
            tools_path = self.args.toolspath or ""
        unbundle_tool_path = tools_path+'euca-unbundle-stream'

        unbundle_ps_args = [unbundle_tool_path,
                            '-e', str(manifest.enc_key),
                            '-v', str(manifest.enc_iv),
                            '-d', "-",  # write to process's stdout
                            '--maxbytes', str(self.args.maxbytes)]

        #Enable debug on this subprocess if local arg is set
        if self.args.debug:
            unbundle_ps_args.append('--debug')
        self.log.debug('Running "' + str(unbundle_tool_path) + '" with '
                       'args:' + ",".join(str(x) for x in unbundle_ps_args))
        try:
            download_r, download_w = open_pipe_fileobjs()
            monitor_r, monitor_w = open_pipe_fileobjs()
            #Begin the unbundle portion of this pipeline...
            unbundle_ps = subprocess.Popen(unbundle_ps_args,
                                           stdin=download_r,
                                           stdout=monitor_w,
                                           stderr=subprocess.PIPE,
                                           close_fds=True,
                                           bufsize=-1)
            download_r.close()
            monitor_w.close()
            self.log.debug('Starting download parts process to feed unbundle')
            #Iterate through all parts in manifest and download to unbundle
            download_ps = spawn_process(self._download_parts_pipe_wrapper,
                                        manifest=manifest,
                                        dest_fileobj=download_w)
            download_w.close()
            self.log.debug('Starting process monitor')
            # Process io monitor sits on top/end of unbundle pipe
            # It attempts to gather information on the progress of the
            # unbundle pipeline and provide information as to the bytes
            # written to the destination file obj.
            bytes = monitor_subprocess_io(infile=monitor_r,
                                          outfile=dest_fileobj,
                                          sub_stderr=unbundle_ps.stderr,
                                          log_method=self.log.debug,
                                          inactivity_timeout=inactivity_timeout)
            self.log.debug('Done with unbundle pipeline...')
            if self.args.reportprogress:
                stages.report_status('"bytes_unbundled":%d' % bytes)
            #Do some final wait/cleanup...
            for ps in [unbundle_ps, download_ps]:
                if ps:
                    wait_thread = wait_process_in_thread(ps.pid)
                    if wait_thread:
                        wait_threads.append(wait_thread)
            # Monitor the subprocess pids in a separate threads, use join()
            # timeout to kill processes if needed
            for wait_thread in wait_threads:
                if wait_thread:
                    wait_thread.join(timeout=inactivity_timeout)
        except Exception, UBE:
            if not self.args.reportprogress:
                traceback.print_exc()
            for ps in [unbundle_ps, download_ps]:
                if ps:
                    try:
                        ps.terminate()
                    except:
                        pass
            raise UBE
        finally:
            for f_pipe in [monitor_r, monitor_w, download_r, download_w]:
                if f_pipe:
                    try:
                        f_pipe.close()
                    except:
                        pass
        return bytes

    def _validate_written_image_size(self, expected_size, filepath):
        '''
        Attempts to compare an expected file size with the size found on disk.
        :param expected_size: size in bytes
        :param filepath: path to local file to read and compare size to
        :raises ValueError
        '''
        self.log.debug('Validating size:"{0}", for file:{1}:'
                       .format(expected_size, filepath))
        # Check size raise os.error if file is not accessible...
        file_size = os.path.getsize(filepath)
        if file_size != expected_size:
            raise ValueError('Written Image size:{0} does not equal expected '
                             'size:{1}'.format(file_size, expected_size))

    def main(self):
        manifest = self.args.manifest
        #Dump manifest obj to screen and exit, if debug arg given.
        if self.args.dumpmanifest:
            print str(manifest)
            os.sys.exit(0)
        dest_file = self.args.destination
        dest_file_name = self.args.destination
        bytes = 0
        #If this image is bundled, download parts to unbundle stream
        #All other formats can be downloaded directly to destination
        try:
            expected_size = manifest.download_image_size
            if isinstance(dest_file, file):
                dest_file_name = '<stdout>'
                dest_fileobj = dest_file
            elif dest_file == "-":
                dest_file_name = '<stdout>'
                dest_fileobj = os.fdopen(os.dup(os.sys.stdout.fileno()), 'w')
            else:
                dest_file_name = str(dest_file)
                dest_fileobj = open(dest_file, 'w')
            if manifest.file_format == 'BUNDLE':
                expected_size = manifest.unbundled_image_size
                if not self.args.privatekey:
                    raise ArgumentError(self.args.privatekey,
                                    'Bundle type needs privatekey -k')
                bytes = self._download_to_unbundlestream(
                    dest_fileobj=dest_fileobj, manifest=manifest)
            else:
                with dest_fileobj:
                    bytes = self._download_parts_to_fileobj(
                        manifest=manifest, dest_fileobj=dest_fileobj)
            #Done with the download, now check the resulting image size.
            self.log.debug('Downloaded bytes:"{0}"'.format(str(bytes)))
            self.log.debug('manifest download image size:'
                       + str(manifest.download_image_size))
            self.log.debug('manifest unbundled size:'
                       + str(manifest.unbundled_image_size))
            #If destination was not stdout, check dest file size.
            if dest_file != "-":
                self._validate_written_image_size(expected_size=expected_size,
                                              filepath=dest_file)
                self.log.info('Download Image wrote "{0}" bytes to: {1}'
                          .format(str(bytes), str(dest_file_name)))
            else:
                self.log.debug('Download Image wrote "{0}" bytes to: {1}'
                           .format(str(bytes), str(dest_file_name)))
        except Exception, E:
            if self.args.reportprogress:
                stages.report_error(str(E))
            else:
                raise E

if __name__ == '__main__':
    try:
        di = DownloadImage().main()
        os.sys.exit(0)
    except Exception, E:
        print >> sys.stderr, "Caught exception:'" + str(E) + "'"
        traceback.print_exc()
        os.sys.exit(1)
