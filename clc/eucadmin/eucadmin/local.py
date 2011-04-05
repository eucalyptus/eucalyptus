#!/usr/bin/env python
import sys, os, binascii, urllib2, MySQLdb, hashlib, psutil, re, zipfile, paramiko
from M2Crypto import RSA, SSL
from contextlib import contextmanager
from paramiko import SSHClient, SFTPAttributes, SFTPClient

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
  
def db_get(field=None):
  conn = MySQLdb.connect (host = "127.0.0.1",
                          user = "eucalyptus",
                          passwd = db_pass(),
                          db = "eucalyptus_auth",
                          port = 8777 )
  cursor = conn.cursor ()
  cursor.execute ("select %s from auth_users where auth_user_name='admin';"%field)
  row = cursor.fetchone ()
  result = row[0]
  cursor.close ()
  conn.close ()
  return result

def get_credentials(fileName=None,source=None):
  if source:
    local = "%s.zip"% os.tempnam( os.path.dirname(fileName) if fileName else "/tmp/" )
  else:
    local = os.path.abspath(fileName)
  if os.path.exists(local):
    print "ERROR: file %s already exists." % local
    sys.exit()
  SSL.Connection.clientPostConnectionCheck = None
  url = "https://localhost:8443/getX509?user=admin&code=%s"%get_token()
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

def get_query_id():
  return db_get("auth_user_query_id")

def get_secret_key():
  return db_get("auth_user_secretkey")

def get_token():
  return db_get("auth_user_token")

def main():
  print get_query_id()
  print get_secret_key()
  print get_token()
  
if __name__ == "__main__":
    main()
 
