#!/usr/bin/python

from uiproxyclient import UIProxyClient
from datetime import datetime, timedelta

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')
    print "=== Getting Zones, again and again ==="
    iter = 0
    sec_iter = 0
    start = datetime.now()
    while True:
        zones = client.get_zones()
        iter = iter+1
        sec_iter = sec_iter+1
        if (datetime.now() - start) > timedelta(seconds=1):
          print "req/sec: "+str(sec_iter)
          sec_iter = 0
          start = datetime.now()
