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
from subprocess import Popen, PIPE
import binascii
import hashlib
import re


def _verify_signature(cert_filename, signature, data, algorithm):
    '''
    Attempts to verify a signature by comparing the digest in the signature
    vs the calculated digest using the data and algorithm provided. This
    is currently done in separate stages to avoid writing temp files to disk.
    :param cert_filename: local file path to certificate containing pubkey
    :param signature: str buffer containing hex signature
    :param data: str buffer to build digest from
    :param algorithm: algorithm name, ie: md5, sha1, sha256, etc..
    :raises ValueError if digests do not match
    '''
    sig_digest = _get_digest_from_signature(cert_filename=cert_filename,
                                            signature=signature,
                                            algorithm=algorithm)
    data_digest = _get_data_digest(data=data, algorithm=algorithm)
    sig_digest = binascii.hexlify(sig_digest)
    data_digest = binascii.hexlify(data_digest)
    if sig_digest != data_digest:
        raise ValueError('Signature not verified. Signature digest:'
                         + str(sig_digest).encode('hex') + ", data_digest:"
                         + str(data_digest).encode('hex'))
    return {'sig_digest': sig_digest, 'data_digest': data_digest}


def _get_data_signature(data_to_sign, algorithm, key_filename):
    '''
    Creates a signature for 'data_to_sign' using the provided algorithm
    and private key file path. ie: /keys/cloud-pk.pem
    :param data_to_sign: string buffer with data to be signed
    :param algorithm: algorithm name to be used in signature, ie:md5, sha256
    :param key_filename: local path to private key to be used in signing
    :returns signature string.
    '''
    if not data_to_sign or not algorithm or not key_filename:
        raise ValueError("Bad value passed to _get_data_signature."
                         "data:'{0}', algorithm:'{1}', key:'{2}'"
                         .format(data_to_sign, algorithm, key_filename))
    digest = _get_digest_algorithm_from_string(algorithm)
    digest.update(data_to_sign)
    popen = Popen(['openssl', 'pkeyutl', '-sign', '-inkey',
                   key_filename, '-pkeyopt', 'digest:' + digest.name],
                  stdin=PIPE, stdout=PIPE)
    (stdout, _) = popen.communicate(digest.digest())
    return binascii.hexlify(stdout)


def _get_digest_from_signature(cert_filename, signature, algorithm):
    '''
    Attempts to recover the original digest from the signature provided
    using the certificate provided. ie: /keys/cloud-cert.pem
    :param cert_filename: local file path to certificate containing pubkey
    :param signature: str buffer containing hex signature
    :returns digest string
    '''
    digest_type = _get_digest_algorithm_from_string(algorithm)
    if not cert_filename or not signature:
        raise ValueError("Bad value passed to _get_digest_from_signature. "
                         "cert:'{0}', signature:'{1}'"
                         .format(cert_filename, signature))
    popen = Popen(['openssl', 'rsautl', '-verify', '-certin', '-inkey',
                   cert_filename],
                  stdin=PIPE, stdout=PIPE)
    (stdout, _) = popen.communicate(binascii.unhexlify(signature))
    #The digest is the last element in the returned output.
    #The size/offset will vary depending on the digest algorithm
    return stdout[-int(digest_type.digestsize):]


def _get_data_digest(data, algorithm):
    '''
    Returns digest for 'data' provided using the 'algorithm' provided.
    :param data: str buffer to build digest from
    :param algorithm: algorithm name, ie: md5, sha1, sha256, etc..
    :returns digest string
    '''
    if not data:
        raise ValueError('No data provided to _get_data_digest, data:"{0}"'
                         .format(str(data)))
    digest = _get_digest_algorithm_from_string(algorithm)
    digest.update(data)
    return digest.digest()


def _get_digest_algorithm_from_string(algorithm):
    '''
    Helper method to convert a string to a hashlib digest algorithm
    :param algorithm: string representing algorithm, ie: md5, sha1, sha256
    :returns hashlib builtin digest function ie: hashlib.md5()
    '''
    #Manifest prepends text to algorithm, remove it here
    r_algorithm = re.search('(?:sha.*|md5.*)', algorithm.lower()).group()
    try:
        digest = getattr(hashlib, str(r_algorithm))
    except AttributeError as AE:
        AE.args = ['Invalid altorithm type:"' + str(r_algorithm)
                   + '" from:"' + str(algorithm) + '". ' + AE.message]
        raise AE
    return digest()


def _decrypt_hex_key(hex_encrypted_key, key_filename):
    '''
    Attempts to decrypt 'hex_encrypted_key' using key at local
    file path 'key_filename'.
    :param hex_encrypted_key: hex to decrypt
    :param key_filename: local file path to key used to decrypt
    :returns decrypted key
    '''
    if not hex_encrypted_key:
        raise ValueError('Empty hex_encrypted_key passed to decrypt')
    if not key_filename:
        raise ValueError('Empty key_filename passed to decrypt')
    #borrowed from euca2ools...
    popen = Popen(['openssl', 'rsautl', '-decrypt', '-pkcs',
                   '-inkey', key_filename], stdin=PIPE, stdout=PIPE)
    binary_encrypted_key = binascii.unhexlify(hex_encrypted_key)
    (decrypted_key, _) = popen.communicate(binary_encrypted_key)
    try:
        # Make sure it might actually be an encryption key.
        int(decrypted_key, 16)
        return decrypted_key
    except ValueError as VE:
        VE.args = ['Failed to decrypt:"' + str(hex_encrypted_key)
                   + '", and keyfile:"' + str(key_filename) + '".'
                   + VE.message]
        raise VE

def _calc_digest_for_fileobj(file_obj, algorithm, chunk_size=None):
    '''
    Calculated and return the digest for the fileobj provided using the
    hashlib 'alogrithm' provided.
    :param file_obj: file like obj to read compute digest for
    :param algorithm: string representing hashlib type(sha1, md5, etc)
    :param chunksize: # of bytes to read/write per read()/write()
    '''
    chunk_size = chunk_size or 8192
    digest = _get_digest_algorithm_from_string(algorithm)
    while True:
        chunk = file_obj.read(chunk_size)
        if not chunk:
            break
        digest.update(chunk)
    return digest.hexdigest()
