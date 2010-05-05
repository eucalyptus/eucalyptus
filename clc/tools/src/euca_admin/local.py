#!/usr/bin/env python
import sys, os, binascii, urllib2, MySQLdb, hashlib
from M2Crypto import RSA, SSL

def passphrase_callback():
  return "eucalyptus"

def db_pass():
  path = "%s/var/lib/eucalyptus/keys/cloud-pk.pem"%os.getenv('EUCALYPTUS')
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

def get_credentials(fileName=None):
  local = os.path.abspath(fileName)
  if os.path.exists(local):
    print "ERROR: file %s already exists." % local
    sys.exit()
  SSL.Connection.clientPostConnectionCheck = None
  url = "https://localhost:8443/getX509?user=admin&code=%s"%get_token()
  print "Fetching %s"%url
  try:
    inUrl = urllib2.urlopen(url)
    outFile = open(local, 'w')
    outFile.write(inUrl.read())
    outFile.close()
    inUrl.close()
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
 
