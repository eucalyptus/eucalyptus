#!/bin/bash
TCPDUMP="tcpdump -Alnt -s0 ${@} and (((ip[2:2] - ((ip[0]&0xf)<<2)) - ((tcp[12]&0xf0)>>2)) != 0)"
echo ${TCPDUMP}
sudo ${TCPDUMP} \
| egrep -v '^.{0,2}"F|^$|^\.\.+$|\.\.$|^E\.\.|^IP .{1,3}\.' 
