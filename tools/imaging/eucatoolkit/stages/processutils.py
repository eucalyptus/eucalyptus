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
import os
from select import select
from multiprocessing import Process
import time
import threading
from eucatoolkit import stages
import traceback


def close_all_fds(except_fds=None):
    '''
    Closes all fds outside of stdout,stderr for this process/subprocess.
    :param except_fds: list of files, or fds to 'not' close
    '''
    except_filenos = [1, 2]
    if except_fds is not None:
        for except_fd in except_fds:
            if except_fd is None:
                pass
            elif isinstance(except_fd, int):
                except_filenos.append(except_fd)
            elif hasattr(except_fd, 'fileno'):
                except_filenos.append(except_fd.fileno())
            else:
                raise ValueError('{0} must be an int or have a fileno method'
                                 .format(repr(except_fd)))

    fileno_ranges = []
    next_range_min = 0
    for except_fileno in sorted(except_filenos):
        if except_fileno > next_range_min:
            fileno_ranges.append((next_range_min, except_fileno - 1))
        next_range_min = max(next_range_min, except_fileno + 1)
    fileno_ranges.append((next_range_min, 1024))

    for fileno_range in fileno_ranges:
        os.closerange(fileno_range[0], fileno_range[1])


def monitor_subprocess_io(infile,
                          outfile,
                          sub_stdout=None,
                          sub_stderr=None,
                          chunk_size=None,
                          log_method=None,
                          inactivity_timeout=120):
        '''
        Monitors the io availability of 'infile'. Reads from infile and
        writes to outfile. If there is no activity on infile for a period of
        'inactivity_timeout' seconds than an error is raised. A sub process's
        stdout/err can also be monitored and when read ready, call 'log_method'
        with the read result.
        :param infile: file object to read from and monitor
        :param outfile: file object to write to
        :param chunk_size: size to read/write per iteration
        :param log_method: the method to call with read stdout/err
        :param inactivity_timeout: int seconds to allow for no infile
                                   activity before raising error
        returns bytes written
        '''
        chunk_size = chunk_size or stages._chunk_size
        readfds = [infile.fileno()]
        if log_method:
            if sub_stderr:
                readfds.append(sub_stderr.fileno())
            if sub_stdout:
                readfds.append(sub_stdout.fileno())
        written_bytes = 0
        try:
            # Set additional infile timer using 'last_read', in case
            # other fds are monitored as well such as stdout,stderr
            last_read = time.time()
            done = False
            while not done:
                reads, writes, errors = select(readfds, [], [],
                                               inactivity_timeout)
                if len(reads) > 0:
                    for fd in reads:
                        #check for each fds in read ready list
                        if fd == infile.fileno():
                            last_read = time.time()
                            chunk = infile.read(chunk_size)
                            if chunk:
                                outfile.write(chunk)
                                written_bytes += len(chunk)
                            else:
                                outfile.flush()
                                done = True
                        else:
                            # If infile has not appeared in the read ready list
                            # for the inactivity period raise error
                            read_elapsed = int(time.time() - last_read)
                            if read_elapsed > inactivity_timeout:
                                raise RuntimeError(
                                    'io monitor: {0} seconds elapsed since'
                                    ' last read.'.format(read_elapsed))
                        if sub_stdout and fd == sub_stdout.fileno():
                            msg = ""
                            line = sub_stdout.read()
                            if not line:
                                readfds.remove(sub_stdout.fileno())
                            elif log_method:
                                log_method('subprocess stdout:'
                                           + str(line.strip()))

                        if sub_stderr and fd == sub_stderr.fileno():
                            msg = ""
                            line = sub_stderr.readline()
                            if not line:
                                readfds.remove(sub_stderr.fileno())
                            elif log_method:
                                log_method('subprocess stderr:'
                                           + str(line.strip()))
                else:
                    raise RuntimeError('Monitor IO inactivity timeout fired '
                                       'after {0} seconds'
                                       .format(inactivity_timeout))
        finally:
            outfile.flush()
        return written_bytes


def open_pipe_fileobjs():
    '''
    helper method to create and return pipe file like objs
    :returns read_pipe_file, write_pipe_file
    '''
    pipe_r, pipe_w = os.pipe()
    return os.fdopen(pipe_r), os.fdopen(pipe_w, 'w')


def spawn_process(func, **kwargs):
    p = Process(target=process_wrapper, args=[func], kwargs=kwargs)
    p.start()
    return p


def process_wrapper(func, **kwargs):
    name = getattr(func, '__name__', 'unknown')
    try:
        func(**kwargs)
    except KeyboardInterrupt:
        pass
    except Exception, e:
        traceback.print_exc()
        msg = 'Error in wrapped process {0}:{1}'.format(str(name), str(e))
        print >> os.sys.stderr, msg
        return
    os._exit(os.EX_OK)


def pid_exists(pid):
    '''
    Helper method to send a kill 0 signal to poll if pid is alive
    :returns boolean, true if pid is found, false if not.
    '''
    try:
        #Check to see if pid exists
        os.kill(pid, 0)
        return True
    except OSError, ose:
        if ose.errno == os.errno.ESRCH:
            #Pid was not found
            return False
        else:
            raise ose


def check_and_waitpid(pid, status):
    if pid_exists(pid):
        try:
            os.waitpid(pid, status)
        except OSError:
            pass


def wait_process_in_thread(pid):
    """
    Start a thread that calls os.waitpid on a particular PID to prevent
    zombie processes from hanging around after they have finished.
    """
    if pid and pid_exists(pid):
        pid_thread = threading.Thread(target=check_and_waitpid, args=(pid, 0))
        pid_thread.daemon = True
        pid_thread.start()
        return pid_thread


def check_and_waitpid(pid, status):
    if pid_exists(pid):
        try:
            os.waitpid(pid, status)
        except OSError:
            pass
