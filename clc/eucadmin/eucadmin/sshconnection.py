# Software License Agreement (BSD License)
#
# Copyright (c) 2009-2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: matt.clark@eucalyptus.com
"""
Created on Mar 7, 2012
@author: clarkmatthew

simple class to establish an ssh session
example usage:
    import sshconnection
    ssh = SshConnection( '192.168.1.1', keypath='/home/testuser/keyfile.pem')

    #use sys() to get either a list of output lines or a single string buffer depending on listformat flag
    output = ssh.sys('ls /dev/sd*',timeout=10)
    print output[0]
    print ssh.lastcmd+" exited with code: "+str(ssh.lastexitcode)

    #...or use cmd to get a dict of output, exitstatus, and elapsed time to execute...
    out = ssh.cmd('ping 192.168.1.2 -c 1 -W 5')

     print out['cmd']+" exited with status:"+out['status']+", elapsed time:"+out['elapsed']
     print out['output']

example with proxy:
    import sshconnection

    instance_private_ip = '10.1.1.5'
    instance_user = 'root'
    instance_keypath = '/tmp/instancekey.pem'

    proxy_ip = '192.168.1.2'
    proxy_username = 'testuser'
    #proxy_password = 'foo'
    proxy_keypath = '/home/testuser/keyfile.pem'


    example output from ipython:

    In[4]: instance_ssh = SshConnection( instance_private_ip, username=instance_user, keypath=instance_keypath,
            proxy=proxy_ip, proxy_username = proxy_username, proxy_keypath = proxy_keypath, debug_connect = True)
    ssh_connect args:
    hostname:10.1.1.5
    username:root
    password:None
    keypath:/tmp/instancekey.pem
    timeout:60
    retry:1
    IPV6 DNS lookup disabled, do IPV4 resolution and pass IP to connect()
    10.1.1.5, is already an ip, dont do host lookup...
    192.168.1.2, is already an ip, dont do host lookup...
    SSH connection attempt(1 of 2), host:'root@10.1.1.5', using ipv4:10.1.1.5, thru proxy:'192.168.1.2'
    Using Keypath:/tmp/instancekey.pem
    SSH - Connected to 10.1.1.5 via proxy host:192.168.1.2:22

    In [5]: instance_ssh.sys('hostname')
    Out[5]: ['euca_10_1_1_5.eucalyptus_cloud.com']

"""


import copy
import os
import paramiko
import re
import select
import socket
import time




class SshCbReturn():
    def __init__(self, stop=False, statuscode=-1, settimer=0, buf=None, nextargs=None):
        """
        Used to return data from an ssh cmd callback method that can be used to handle output as it's rx'd instead of...
        waiting for the cmd to finish and returned buffer. See SshConnection.cmd() for more info.
        The call back must return type SshCbReturn.
        :param stop: If cb returns stop==True, recv loop will end, and channel will be closed, cmd will return.
        :param settimer: if cb settimer is > 0, timer timeout will be adjusted for this time
        :param statuscode: if cb statuscode is != -1 cmd status will return with this value
        :param nextargs: if cb nextargs is set, the next time cb is called these args will be passed instead
        :param buf: if cb buf is not None, the cmd['output'] buffer will be appended with this buf
        """
        self.stop = stop
        self.statuscode = statuscode
        self.settimer = settimer
        self.nextargs = nextargs or []
        self.buf = buf


class SshConnection():
    cmd_timeout_err_code = -100
    cmd_not_executed_code = -99

    def __init__(self,
                 host,
                 username='root',
                 password=None,
                 keypair=None,
                 keypath=None,
                 proxy=None,
                 proxy_username='root',
                 proxy_password=None,
                 proxy_keyname=None,
                 proxy_keypath=None,
                 priv_key_file=None,
                 use_pub_keys=True,
                 enable_ipv6_dns=False,
                 timeout=60,
                 retry=1,
                 debugmethod=None,
                 verbose=False,
                 debug_connect=False):
        """


        :param host: -mandatory - string, hostname or ip address to establish ssh connection to
        :param username: - optional - string, username used to establish ssh session when keypath is not provided
        :param password: - optional - string, password used to establish ssh session when keypath is not provided
        :param keypair: - optional - boto keypair object, used to attept to derive local path to ssh key if present
        :param keypath:  - optional - string, path to ssh key
        :param proxy: - optional - host to proxy ssh connection through
        :param proxy_username:  - optional ssh username of proxy host for authentication
        :param proxy_password: - optional ssh password of proxy host for authentication
        :param proxy_keypath: - optional path to ssh key to use for proxy authentication
        :param proxy_keyname: - optional keyname for proxy authentication, will attempt to derive keypath from this
        :param enable_ipv6_dns: - optional - boolean to allow ipv6 dns hostname resolution
        :param timeout: - optional - integer, tcp timeout in seconds
        :param retry: - optional - integer, # of attempts made to establish ssh session without auth failures
        :param debugmethod: - method, used to handle debug msgs
        :param verbose: - optional - boolean to flag debug output on or off mainly for cmd execution
        :param debug_connect: - optional - boolean to flag debug output on or off for connection related operations
        """

        self.host = host
        self.username = username
        self.password = password
        self.keypair = keypair
        self.keypath = keypath
        self.proxy = proxy
        self.proxy_username = proxy_username
        self.proxy_password = proxy_password
        self.proxy_keyname = proxy_keyname
        self.proxy_keypath = proxy_keypath
        self.use_pub_keys = use_pub_keys
        self.enable_ipv6_dns = enable_ipv6_dns
        self.timeout = timeout
        self.retry = retry
        self.debugmethod = debugmethod
        self.verbose = verbose
        self.priv_key_file = priv_key_file
        self.debug_connect = debug_connect

        #Used to store the last cmd attempted and it's exit code
        self.lastcmd = ""
        self.lastexitcode = SshConnection.cmd_not_executed_code

        if self.keypair is not None:
            self.keypath = os.getcwd() + "/" + self.keypair.name + ".pem"
        if self.keypath is not None:
            self.debug("SSH connection has hostname:" + str(self.host) + " user:" +
                       str(self.username) + " and keypath: " + str(self.keypath))
        else:
            self.debug("SSH connection has hostname:" + str(self.host) + " user:" +
                       str(self.username) + " password:" + str(self.mask_password(password)))
        if proxy:
            if self.proxy_keyname is not None:
                self.proxy_keypath = os.getcwd() + "/" + self.proxy_keyname + ".pem"
            if self.proxy_keypath is not None:
                self.debug("SSH proxy has hostname:" + str(self.proxy) + " user:" +
                           str(self.proxy_username) + " and keypath: " + str(self.proxy_keypath))
            else:
                self.debug("SSH proxy has hostname:" + str(self.proxy) + " user:" +
                           str(proxy_username) + " password:" + str(self.mask_password(proxy_password)))

        if self.use_pub_keys or \
                self.keypath is not None or \
                ((self.username is not None) and (self.password is not None)):
            self.connection = self.get_ssh_connection(self.host,
                                                      username=self.username,
                                                      password=self.password,
                                                      keypath=self.keypath,
                                                      proxy_username=self.proxy_username,
                                                      proxy_password=self.proxy_password,
                                                      proxy_keypath=self.proxy_keypath,
                                                      enable_ipv6_dns=self.enable_ipv6_dns,
                                                      timeout=self.timeout,
                                                      retry=self.retry,
                                                      verbose=self.debug_connect)
        else:
            raise Exception("Need either a keypath or username+password to create ssh connection")

    def get_proxy_transport(self,
                            proxy_host=None,
                            dest_host=None,
                            port=22,
                            proxy_username='root',
                            proxy_password=None,
                            proxy_keypath=None,
                            priv_key_file=None,
                            verbose=True):
        """


        :param priv_key_file: private key file. If 'None' will check global self.priv_key_file, 
                              standard locations (~/.ssh/id_{rsa,dsa}.pub), and ssh-agent's
                              stored keys.
        :param verbose: print debug
        :param proxy_host: hostname of ssh proxy
        :param port: ssh proxy port
        :param dest_host: end host to connect to
        :param proxy_username: proxy username for ssh authentication
        :param proxy_password: proxy password for ssh authentication
        :param proxy_keypath: local path to key used for ssh authentication
        :return: paramiko transport
        """
        proxy_host = ((proxy_host or self.proxy),port)
        dest_host = ((dest_host or self.host),port)
        proxy_username = proxy_username or self.proxy_username
        proxy_password = proxy_password or self.proxy_password
        proxy_keypath = proxy_keypath or self.proxy_keypath

        #Make sure there is at least one likely way to authenticate...
        ssh = paramiko.SSHClient()
        if self.use_pub_keys or \
                        proxy_keypath is not None or \
                        ((proxy_username is not None) and (proxy_password is not None)):
            p_transport = paramiko.Transport(proxy_host)
            ssh._transport = p_transport
            p_transport.start_client()
            if proxy_keypath:
                priv_key = paramiko.RSAKey.from_private_key_file(proxy_keypath)
                p_transport.auth_publickey(proxy_username,priv_key)
            elif proxy_password:
                p_transport.auth_password(proxy_username, proxy_password)
            else:
                priv_key_files = []
                file = priv_key_file or self.priv_key_file
                if file and os.path.isfile(file):
                    priv_key_files.append(file)
                self.debug("Proxy auth -Using local keys, no keypath/password provided, trying:" + str(file or ''),
                           verbose=verbose)
                ssh._auth(proxy_username, None,None,priv_key_files, True, True)
                p_transport = ssh._transport
            #forward from 127.0.0.1:<free_random_port> to |dest_host|
            channel = p_transport.open_channel('direct-tcpip', dest_host, ('127.0.0.1', 0))
            return paramiko.Transport(channel)
        else:
            raise Exception("Need either a keypath or username+password to create ssh proxy connection")


    def debug(self, msg, verbose=None):
        """
        simple method for printing debug.
        :param msg: - mandatory - string to be printed
        :param verbose: boolean to override global verbose flag
        """
        if verbose is None:
            verbose = self.verbose
        if verbose is True:
            if self.debugmethod is None:
                print (str(msg))
            else:
                self.debugmethod(msg)

    def ssh_sys_timeout(self, chan, start, cmd):
        """
        callback to be scheduled during ssh cmds which have timed out.
        :param chan: - paramiko channel to be closed
        :param start - time.time() used to calc elapsed time when this fired for debug
        :param cmd - the command ran
        """
        chan.close()
        elapsed = time.time() - start
        raise CommandTimeoutException(
            "SSH Command timer fired after " + str(int(elapsed)) + " seconds. Cmd:'" + str(cmd) + "'")


    def sys(self, cmd, verbose=False, timeout=120, listformat=True, code=None):
        """
        Issue a command cmd and return output in list format

        :param cmd: - mandatory - string representing the command to be run  against the remote ssh session
        :param verbose: - optional - will default to global setting, can be set per cmd() as well here
        :param timeout: - optional - integer used to timeout the overall cmd() operation in case of remote blockingd
        :param listformat:  - optional - format output into single buffer or list of lines
        :param code: - optional - expected exitcode, will except if cmd's  exitcode does not match this value
        """
        out = self.cmd(cmd, verbose=verbose, timeout=timeout, listformat=listformat)
        output = out['output']
        if code is not None:
            if out['status'] != code:
                self.debug(output)
                raise CommandExitCodeException('Cmd:' + str(cmd) + ' failed with status code:'
                                               + str(out['status']) + ", output:" + str(output))
        return output


    def cmd(self, cmd, verbose=None, timeout=120, listformat=False, cb=None, cbargs=[], get_pty=True):
        """ 
        Runs a command 'cmd' within an ssh connection. 
        Upon success returns dict representing outcome of the command.

        Returns dict:
            ['cmd'] - The command which was executed
            ['output'] - The std out/err from the executed command
            ['status'] - The exitcode of the command. Note in the case a call back fires, this exitcode is unreliable.
            ['cbfired']  - Boolean to indicate whether or not the provided callback fired (ie returned False)
            ['elapsed'] - Time elapsed waiting for command loop to end. 
        Arguments:
        :param cmd: - mandatory - string representing the command to be run  against the remote ssh session
        :param verbose: - optional - will default to global setting, can be set per cmd() as well here
        :param timeout: - optional - integer used to timeout the overall cmd() operation in case of remote blocking
        :param listformat: - optional - boolean, if set returns output as list of lines, else a single buffer/string
        :param cb: - optional - callback, method that can be used to handle output as it's rx'd instead of...
                        waiting for the cmd to finish and return buffer. Called like: cb(ssh_cmd_out_buffer, *cbargs)
                        Must accept string buffer, and return an integer to be used as cmd status. 
                        Must return type 'sshconnection.SshCbReturn'
                        If cb returns stop, recv loop will end, and channel will be closed.
                        if cb settimer is > 0, timer timeout will be adjusted for this time
                        if cb statuscode is != -1 cmd status will return with this value
                        if cb nextargs is set, the next time cb is called these args will be passed instead of cbargs
        :param cbargs: - optional - list of arguments to be appended to output buffer and passed to cb

        """
        if verbose is None:
            verbose = self.verbose
        ret = {}
        cbfired = False
        cmd = str(cmd)
        self.lastcmd = cmd
        self.lastexitcode = SshConnection.cmd_not_executed_code
        start = time.time()
        status = None
        if verbose:
            self.debug("[" + self.username + "@" + str(self.host) + "]# " + cmd)
        try:
            tran = self.connection.get_transport()
            if tran is None:
                self.debug("SSH transport was None, attempting to restablish ssh to: "+str(self.host))
                self.refresh_connection()
                tran = self.connection.get_transport()
            chan = tran.open_session()
            chan.set_combine_stderr(False)
            chan.settimeout(timeout)
            if get_pty:
                chan.get_pty()
            chan.exec_command(cmd)
            output = ""
            err = ""
            fd = chan.fileno()
            chan.setblocking(0)
            cmdstart = start = time.time()
            newdebug = "\n"
            while True and chan.closed == 0:
                try:
                    rl, wl, xl = select.select([fd], [], [], timeout)
                except select.error:
                    break
                elapsed = int(time.time() - start)
                if elapsed >= timeout:
                    raise CommandTimeoutException(
                        "SSH Command timer fired after " + str(int(elapsed)) + " seconds. Cmd:'" + str(cmd) + "'")
                time.sleep(0.05)

                if len(rl) > 0:
                    while chan.recv_stderr_ready():
                        data = chan.recv_stderr(1024)
                        err += data

                    while chan.recv_ready():
                        new = chan.recv(1024)
                        if new is not None:
                            #We have data to handle...
                            #Run call back if there is one, let call back handle data read in
                            if cb is not None:
                                #If cb returns false break, end rx loop, return cmd outcome/output dict. 
                                #cbreturn = SshCbReturn()
                                cbreturn = cb(new, *cbargs)
                                #Let the callback control whether or not to continue
                                if cbreturn.stop:
                                    cbfired = True
                                    #Let the callback dictate the return code, otherwise -1 for connection err may occur
                                    if cbreturn.statuscode != -1:
                                        status = cbreturn.statuscode
                                    else:
                                        status = self.lastexitcode = chan.recv_exit_status()
                                    chan.close()
                                    break
                                else:
                                    #Let the callback update its calling args if needed
                                    cbargs = cbreturn.nextargs or cbargs
                                    #Let the callback update/reset the timeout if needed
                                    if cbreturn.settimer > 0:
                                        start = time.time()
                                        timeout = cbreturn.settimer
                                    if cbreturn.buf:
                                        output += cbreturn.buf
                            else:
                                #if no call back then append output to return dict and handle debug
                                output += new
                                if verbose:
                                    #Dont print line by line output if cb is used, let cb handle that 
                                    newdebug += new
                        else:
                            status = self.lastexitcode = chan.recv_exit_status()
                            chan.close()
                            break
                    if newdebug and verbose:
                        self.debug(str(newdebug))
                        newdebug = ''

            if listformat:
                #return output as list of lines
                output = output.splitlines()
                if output is None:
                    output = []
                err = err.splitlines()
                if err is None:
                    err = []
                #add command outcome in return dict.
            if not status:
                status = self.lastexitcode = chan.recv_exit_status()
            ret['cmd'] = cmd
            ret['output'] = output
            ret['error'] = err
            ret['status'] = status
            ret['cbfired'] = cbfired
            ret['elapsed'] = elapsed = int(time.time() - cmdstart)
            if verbose:
                self.debug("done with exec")
        except CommandTimeoutException, cte:
            self.lastexitcode = SshConnection.cmd_timeout_err_code
            elapsed = str(int(time.time() - start))
            self.debug("Command (" + cmd + ") timeout exception after " + str(elapsed) + " seconds\nException")
            raise cte
        return ret

    def refresh_connection(self):
        """
        Attempts to establish a new ssh connection to replace the old 'connection' of this
        ssh obj.
        """
        if self.connection:
            self.connection.close()
        self.connection = self.get_ssh_connection(self.host,
                                                  username=self.username,
                                                  password=self.password,
                                                  keypath=self.keypath,
                                                  proxy_username=self.proxy_username,
                                                  proxy_password=self.proxy_password,
                                                  proxy_keypath=self.proxy_keypath,
                                                  enable_ipv6_dns=self.enable_ipv6_dns,
                                                  timeout=self.timeout,
                                                  retry=self.retry,
                                                  verbose=self.debug_connect)

    def get_ssh_connection(self,
                           hostname,
                           username="root",
                           password=None,
                           keypath=None,
                           proxy=None,
                           proxy_username=None,
                           proxy_password=None,
                           proxy_keypath=None,
                           use_pub_keys=None,
                           enable_ipv6_dns=None,
                           port=22,
                           timeout=60,
                           retry=1,
                           verbose=False):
        """
        Create a paramiko ssh session to hostname. Will attempt to authenticate first with a keypath if provided,
        if the sshkey file path is not provided.  username and password will be used to authenticate. This leaves out
        the case where a password is passed as the password needed to unlock the key file. This 3rd case may need to be
        added but may mask failures in tests for key insertion when using tests who's images have baked in passwords for
        login access(tbd).
        Upon success returns a paramiko sshclient with an established connection.

        :param hostname: - mandatory - hostname or ip to establish ssh connection with
        :param username: - optional - username used to authenticate ssh session
        :param password: - optional - password used to authenticate ssh session
        :param keypath: - optional - full path to sshkey file used to authenticate ssh session
        :param proxy: - optional - host to proxy ssh connection through
        :param proxy_username:  - optional ssh username of proxy host for authentication
        :param proxy_password: - optional ssh password of proxy host for authentication
        :param proxy_keypath: - optional path to ssh key to use for proxy authentication
        :param timeout: - optional - tcp timeout
        :param enable_ipv6_dns: - optional - boolean to avoid ipv6 dns 'AAAA' lookups
        :param retry: - optional - Number of attempts to establish ssh connection for errors outside of authentication
        :param port: - optional - port to connect to, default 22
        :param verbose: - optional - enable verbose debug output
        """
        connected = False
        iplist = []
        ip = None
        if use_pub_keys is None:
            use_pub_keys = self.use_pub_keys
        proxy_ip = None
        if not use_pub_keys and password is None and keypath is None:
            raise Exception("ssh_connect: both password and keypath were set to None")
        if enable_ipv6_dns is None:
            enable_ipv6_dns = self.enable_ipv6_dns
        proxy = proxy or self.proxy

        self.debug("ssh_connect args:\nhostname:" + hostname
                    + "\nusername:" + username
                    + "\npassword:" + str(password)
                    + "\nkeypath:" + str(keypath)
                    + "\nproxy_username:" + str(proxy_username)
                    + "\nproxy_password" + str(proxy_password)
                    + "\nproxy_keypath" + str(proxy_keypath)
                    + "\ntimeout:" + str(timeout)
                    + "\nretry:" + str(retry),verbose=verbose)
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        hostname = str(hostname.strip())
        if not enable_ipv6_dns:
            self.debug('IPV6 DNS lookup disabled, do IPV4 resolution and pass IP to connect()',verbose=verbose)
            # Paramiko uses family type 'AF_UNSPEC' which does both ipv4/ipv6 lookups and can cause some DNS servers
            # to fail in their response(s). Hack to avoid ipv6 lookups...
            # Try ipv4 dns resolution of 'hostname', and pass the ip instead of a hostname to
            # Paramiko's connect to avoid the potential ipv6 'AAAA' lookup...
            iplist = self.get_ipv4_lookup(hostname,verbose=verbose)
        if not iplist:
            iplist = [hostname]
        attempt = 0
        #adjust retry count for debug 'readability' ie 'attempt 1' vs 'attempt 0'
        retry += 1
        while (attempt < retry) and not connected:
            attempt += 1
            proxy_transport = None
            for ip in iplist:
                if self.proxy:
                    if not enable_ipv6_dns:
                        proxy_ip = self.get_ipv4_lookup(self.proxy, verbose=verbose)[0]
                        proxy_transport = self.get_proxy_transport(proxy_host=proxy,
                                                                   dest_host=ip,
                                                                   port=port,
                                                                   proxy_username=proxy_username,
                                                                   proxy_password=proxy_password,
                                                                   proxy_keypath=proxy_keypath)
                if proxy_transport:
                    ssh._transport = proxy_transport
                else:
                    ssh._transport = paramiko.Transport(ip)
                ssh._transport.start_client()
                ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
                try:
                    self.debug("SSH connection attempt(" + str(attempt) +" of " + str(retry) + "), host:'"
                               + username + "@" + hostname + "', using ipv4:" + str(ip) + ", thru proxy:'"
                               + str(proxy_ip) + "'")
                    if keypath is None and password:
                        self.debug("Using username:"+username+" and password:"+str(self.mask_password(password)),
                                   verbose=verbose)
                        ssh._transport.auth_password(username, password)
                        #ssh.connect(ip, username=username, password=password, timeout=timeout)
                        connected = True
                        break
                    elif keypath:
                        self.debug("Using Keypath:" + keypath, verbose=verbose)
                        priv_key = paramiko.RSAKey.from_private_key_file(keypath)
                        ssh._transport.auth_publickey(username,priv_key)
                        #ssh.connect(ip, port=port, username=username, key_filename=keypath, timeout=timeout)
                        connected = True
                        break
                    else:
                        priv_key_files = []
                        if self.priv_key_file and os.path.isfile(self.priv_key_file):
                            priv_key_files.append(self.priv_key_file)
                        self.debug("Using local keys, no keypath/password provided, trying:" + str(self.priv_key_file or ''), verbose=verbose)
                        ssh._auth(username, password,None,priv_key_files, True, True)
                        #ssh.connect(ip, port=port, username=username, key_filename=keypath, timeout=timeout)
                        connected = True

                except paramiko.ssh_exception.SSHException, se:
                    self.debug("Failed to connect to " + hostname + ", retry in 10 seconds. Err:" + str(se))
                    time.sleep(10)
                    pass
            if connected:
                via_string = ''
                if proxy_transport:
                    proxy_host,port = ssh._transport.getpeername()
                    via_string = ' via proxy host:'+str(proxy_host)+':'+str(port)
                self.debug('SSH - Connected to ' + str(ip)+str(via_string))
                break
        if not connected:
            raise Exception(
                'Failed to connect to "' + str(hostname) + '", attempts:' + str(attempt) + ". IPs tried:" + ",".join(
                    iplist))
            #self.debug("Returning ssh connection to: "+ hostname)
        return ssh

    def get_ipv4_lookup(self, hostname, port=22,verbose=False):
        """
        Do an ipv4 lookup of 'hostname' and return list of any resolved ip addresses

        :param hostname: hostname to resolve
        :param port: port to include in lookup, default is ssh port 22
        :param verbose: boolean to print addditional debug
        :return: list of ip addresses (strings in a.b.c.d format)
        """
        get_ipv4_ip = False
        iplist = []
        try:
            if socket.inet_aton(hostname):
                ipcheck = re.compile("^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$")
                if not ipcheck.match(hostname):
                    get_ipv4_ip = True
            self.debug(str(hostname) + ", is already an ip, dont do host lookup...", verbose=verbose)
            # This is already an ip don't look it up (regex might be better here?)
        except socket.error:
            get_ipv4_ip = True
        if get_ipv4_ip:
            try:
                #ipv4 lookup host for ssh connection...
                addrs = socket.getaddrinfo(hostname, port, socket.AF_INET, socket.IPPROTO_IP, socket.IPPROTO_TCP)
                for addr in addrs:
                    iplist.append(str(addr[4][0]))
                self.debug('Resolved hostname:' + str(hostname) + ' to IP(s):' + ",".join(iplist),verbose=verbose)
            except Exception, de:
                self.debug('Error looking up DNS ip for hostname:' + str(hostname) + ", err:" + str(de))
        else:
            #hostname is an ipv4 address...
            iplist = [hostname]
        return iplist

    def mask_password(self,pass_string):
        """
        Replace all but first and last chars with '*' of provided password string.

        :param pass_string: string representing a password to hide/format
        :return: Formatted hidden password
        """
        password = copy.copy(pass_string)
        show = ""
        if not password:
            return password
        if len(password) > 3:
            length = len(password)-2
        else:
            length = len(password)
        for x in xrange(length):
            show += '*'
        if len(password) > 3:
            show = password[0]+show
            show += password[len(password)-1]
        return show

    def sftp_put(self,localfilepath,remotefilepath):
        """
        sftp transfer file from localfilepath to remote system at remotefilepath
        :param localfilepath: path to file on local system
        :param remotefilepath: destination path for put on remote system
        """
        if not self.connection._transport:
            self.refresh_connection()
        transport = self.connection._transport
        sftp = paramiko.SFTPClient.from_transport(transport)
        sftp.put(remotepath=remotefilepath, localpath=localfilepath)
        sftp.close()

    def sftp_get(self, localfilepath, remotefilepath):
        """
        sftp transfer file from remotefilepath to remote system at localfilepath
        :param localfilepath: path where remote file 'get' will place file on local system
        :param remotefilepath: destination path for file to 'get' on remote system
        """
        if not self.connection._transport:
            self.refresh_connection()
        transport = self.connection._transport
        sftp = paramiko.SFTPClient.from_transport(transport)
        sftp.get(remotepath=remotefilepath, localpath=localfilepath)
        sftp.close()


    def close(self):
        self.connection.close()


class CommandExitCodeException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)

class CommandTimeoutException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)

