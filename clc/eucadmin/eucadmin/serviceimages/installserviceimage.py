# Copyright 2014 Eucalyptus Systems, Inc.
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
import os.path
import subprocess
import sys
import yaml

import eucadmin.serviceimages


# The way this suite of tools is meant to work is by handing the name
# of a dependency to install to euimage tools so they can find and
# install the appropriate service image using the appropriate profile.
# The euimage tools currently lack the notion of a dependency or even an
# image repository, however, so for now we use a "dumb" implementation
# that installs a pack from a known filesystem location and knows nothing
# about querying what is in the system at all.
#
# This doesn't use roboto for a few reasons:
#   1.  It isn't a web service request
#   2.  Writing it using roboto isn't worth the effort since we'll port
#       it to requestbuilder and do other major fixes anyway.


def install_service_image(service_name, metadata, pack_filename, bucket,
                          show_progress=False):
    # FIXME: The metadata express the images they need in terms
    # of dependencies.  This should really consult euimage tools to
    # determine what pack to install and/or just tell euimage to install
    # it, whatever it is, but right now, euimage has no notion of a
    # dependency or even a repository.  The "dumb" implementation we
    # use here just uses a hardcoded list instead.
    if service_name not in metadata.get('services') or {}:
        raise ValueError('no image data for service "{0}"'
                         .format(service_name))
    # Install the image
    service_to_profile_map = {'database.worker': 'database-worker',
                              'imaging.worker': 'imaging-worker',
                              'loadbalancing.worker': 'loadbalancing-worker'}
    assert service_name in service_to_profile_map
    process = subprocess.Popen(
        ('euimage-install-pack', '--profile',
         service_to_profile_map[service_name], '--bucket', bucket,
         ('--progress' if show_progress else '--no-progress'), pack_filename),
        stdout=subprocess.PIPE)
    output, _ = process.communicate()
    if process.poll() != 0:
        raise subprocess.CalledProcessError(
            process.poll(), 'euimage-install-pack', output=output)
    for line in output.splitlines():
        if 'IMAGE' in line:
            image_id = line.partition('IMAGE')[2].strip()
        if image_id:
            break
    else:
        raise RuntimeError('euimage-install-pack did not output an image ID')
    # Set the property
    prop_name = metadata['services'][service_name].get('set-image-property')
    if prop_name:
        subprocess.check_call(('euca-modify-property', '-p',
                               '{0}={1}'.format(prop_name, image_id)))
    return image_id


def main():
    # This command assumes the relevant credentials are in the environment.
    parser = argparse.ArgumentParser(
        version=eucadmin.__version__,
        description='install a service image')
    parser.add_argument('service', metavar='SERVICE',
                        help='name of the service to install (required)')
    parser.add_argument('-b', '--bucket', required=True,
                        help=('name of the bucket to upload the image '
                              'to (required)'))
    parser.add_argument('--progress', dest='show_progress',
                        action='store_true', default=sys.stdout.isatty(),
                        help='show progress')
    parser.add_argument('--no-progress', dest='show_progress',
                        action='store_false', default=sys.stdout.isatty(),
                        help='do not show progress')
    parser.add_argument('--metadata-file', help=argparse.SUPPRESS)
    parser.add_argument('--pack', help=argparse.SUPPRESS)
    parser.add_argument('--debug', action='store_true',
                        help='show debug output')
    args = parser.parse_args()
    try:
        md_filename = (args.metadata_file or
                       eucadmin.serviceimages.SERVICE_METADATA_FILENAME)
        pack_filename = (args.pack or
                         eucadmin.serviceimages.SERVICE_IMAGE_PACK_FILENAME)
        with open(md_filename) as md_file:
            metadata = yaml.load(md_file)
        install_service_image(args.service, metadata, pack_filename,
                              args.bucket, show_progress=args.show_progress)
    except EnvironmentError as err:
        err_bits = ['{0}:'.format(os.path.basename(sys.argv[0])), 'error:']
        if getattr(err, 'strerror', None):
            err_bits.append(err.strerror)
        if getattr(err, 'filename', None):
            err_bits[-1] += ':'
            err_bits.append(err.filename)
        print >> sys.stderr, ' '.join(err_bits)
        if args.debug:
            raise
    except Exception as err:
        if len(err.args) > 0 and err.args[0]:
            msg = err.args[0]
        else:
            msg = str(err)
        print >> sys.stderr, '{0}: error: {1}'.format(
            os.path.basename(sys.argv[0]), msg)
        if args.debug:
            raise


if __name__ == '__main__':
    main()
