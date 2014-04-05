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
import logging
import os
import sys

_chunk_size = 8192
_max_part_buffer_size = 11534336
_logger_name = 'DownloadImage'


def _str_to_log_level(loglevel):
    '''
    Helper method to map a provided string 'loglevel' to a logging
    loglevel.
    :param loglevel: str representing a log level. ie DEBUG, INFO, etc
    :returns logging.loglevel
    '''
    if hasattr(logging, loglevel):
        loglevel = int(getattr(logging, loglevel))
    else:
        raise ValueError('Invalid Log level "' + loglevel)
    return loglevel

def report_status(status_msg):
    sys.stderr.write('{ "status": { %s } }\n' % status_msg)

def report_error(error_msg):
    sys.stderr.write('{ "error": "%s" }\n' % error_msg)

def get_logger(loglevel=None,
               logfile=None,
               force_stderr=False,
               debug=False):
    '''
    Attempts to setup a logger based up on the provided inputs; loglevel,
    logfile, debug.
    :param loglevel: optional str log level. ie DEBUG, INFO, etc
    :param logfile: optional file path to be logged to
    :param debug: boolean. If true log level is set to debug,
                  and streamhandler will write to stderr.
    '''
    log = logging.getLogger(_logger_name)
    if log.handlers and not loglevel and not logfile:
        return log
    loglevel = str(loglevel or 'INFO').upper()
    loglevel = _str_to_log_level(loglevel)
    if debug:
        if loglevel > logging.DEBUG:
            loglevel = logging.DEBUG
    fmt = logging.Formatter('%(asctime)s-%(levelname)s:%(name)s(' +
                            str(os.getpid()) + '): %(message)s')
    log.setLevel(loglevel)
    if not log.handlers:
        if logfile:
            log.handlers.append(
                logging.FileHandler(logfile))
        if debug or force_stderr:
            log.handlers.append(logging.StreamHandler(sys.stderr))
        else:
            log.handlers.append(logging.StreamHandler(sys.stdout))
    for hndlr in log.handlers:
        hndlr.setLevel(loglevel)
        hndlr.setFormatter(fmt)
        log.addHandler(hndlr)
    return log
