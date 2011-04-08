#!/usr/bin/env python
import sys, os, binascii, urllib2, MySQLdb, hashlib, psutil, re, zipfile, paramiko
from M2Crypto import RSA, SSL
from contextlib import contextmanager
from paramiko import SSHClient, SFTPAttributes, SFTPClient

AUTH_SYS_ACCT = "eucalyptus"
AUTH_DEFAULT_ADMIN = "admin"
AUTH_USER_QUERY = """select 
			u.auth_user_token,
			k.auth_access_key_query_id, 
			k.auth_access_key_key
		from (
			auth_access_key k 
			join 
				auth_user u on k.auth_access_key_owning_user=u.id 
			join 
				auth_group_has_users gu on u.id=gu.auth_user_id 
			join
				auth_group g on gu.auth_group_id=g.id 
			join 
				auth_account a on g.auth_group_owning_account=a.id 
			)
	where 
		a.auth_account_name='%s'
		and g.auth_group_name='_%s'
		and k.auth_access_key_active=1;"""

@contextmanager
def ssh_session(host=None, username=None, password=None):
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy()) 
    try:
         sys.stdout.write( "[Connect:%s]"%host )
         ssh.connect(host, username=username, password=password)
         sys.stdout.write( "[Connected]" )
         yield ssh
    finally:
         ssh.close()

def bbbb():
    for i in range(1,10): 
        sys.stdout.write('\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b') 
    return ''

def put_file(host=None,username=None,password=None,srcFile=None,destFile=None):
    with ssh_session(host,username,password) as ssh:
        ftp = ssh.open_sftp()
        try:
            ftp.chdir(os.path.dirname(destFile))
            ftp.put(srcFile,destFile,lambda x,y: 
            sys.stdout.write( "%6.2f %sCopying %s => %s "%((100.0*x)/y,bbbb(),srcFile,destFile,) ))
        except Exception, ex:
            print ex
            sys.exit()

def get_remote_home(host=None,username=None,password=None):
    with ssh_session(host,username,password) as ssh:
        (i,o,e) = ssh.exec_command("ps auxww | awk '/eucalyptus-cloud/{print $2}' | head -n1 | xargs -L1 -I{} cat /proc/{}/cmdline")
        print e.read()
        home = get_home_cmdline( o.read().split('\0') )
        print home

def get_home_cmdline(cmd=None):
    if cmd.__contains__('-h'): 
        return cmd[cmd.index('-h')+1]
    else:
        home = filter(lambda x: x.startswith("--home="), cmd )
        return home.split('=')[1]

def get_home():
    for i in filter(lambda x: psutil.Process(x).name.startswith("eucalyptus-cl") , psutil.get_pid_list() ):
        home = get_home_cmdline( psutil.Process(i).cmdline )
        if home: return home
    print "ERROR: No running eucalyptus-cloud process found." 

def passphrase_callback():
    return "eucalyptus"

def db_pass():
    path = "%s/var/lib/eucalyptus/keys/cloud-pk.pem"%get_home()
    d = hashlib.sha256()
    d.update("eucalyptus")
    pk = RSA.load_key(path,passphrase_callback)
    return binascii.hexlify(pk.sign(d.digest(),algo="sha256"))
    
def db_get(query=None):
    conn = MySQLdb.connect (host = "127.0.0.1",
                                                    user = "eucalyptus",
                                                    passwd = db_pass(),
                                                    db = "eucalyptus_auth",
                                                    port = 8777 )
    cursor = conn.cursor ()
    cursor.execute (query)
    row = cursor.fetchone ()
    result = row[0]
    cursor.close ()
    conn.close ()
    return result

def get_credentials(fileName=None,source=None,account=AUTH_SYS_ACCT,user=AUTH_DEFAULT_ADMIN):
    if source:
        local = "%s.zip"% os.tempnam( os.path.dirname(fileName) if fileName else "/tmp/" )
    else:
        local = os.path.abspath(fileName)
    if os.path.exists(local):
        print "ERROR: file %s already exists." % local
        sys.exit()
    SSL.Connection.clientPostConnectionCheck = None
    url = "https://localhost:8443/getX509?account=%s&user=%s&code=%s"%(account,user,get_token())
    print "Fetching credentials:\n-> %s\n<- %s"%(url,local)
    try:
        inUrl = urllib2.urlopen(url)
        outFile = open(local, 'w')
        outFile.write(inUrl.read())
        outFile.close()
        inUrl.close()
        if source:
            zip = zipfile.ZipFile(local)
            for i in zip.namelist():
                of = open(os.path.join(os.path.dirname(local), i), 'wb')
                obj = zip.read(i) 
                if i == 'eucarc':
                    print obj
                of.write(obj)
                of.close()
            os.remove(local)
    except IOError, ex:
        print ex
        sys.exit()

def get_user_info(account=AUTH_SYS_ACCT,user=AUTH_DEFAULT_ADMIN):
    conn = MySQLdb.connect (host = "127.0.0.1",
                                                    user = "eucalyptus",
                                                    passwd = db_pass(),
                                                    db = "eucalyptus_auth",
                                                    port = 8777 )
    cursor = conn.cursor ()
    query = AUTH_USER_QUERY % (account,user)
    cursor.execute (query)
    row = cursor.fetchone ()
    result = list(row)
    cursor.close ()
    conn.close ()
    return result

def get_query_id():
    (a,b,c)=get_user_info()
    print a, b, c
    return b;

def get_secret_key():
    (a,b,c)=get_user_info()
    print a, b, c
    return c;

def get_token():
    (a,b,c)=get_user_info()
    print a, b, c
    return a;

def main():
    print get_query_id()
    print get_secret_key()
    print get_token()
    
if __name__ == "__main__":
        main()
 
