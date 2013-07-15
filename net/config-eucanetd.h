#ifndef INCLUDE_CONFIG_EUCANETD_H
#define INCLUDE_CONFIG_EUCANETD_H

#include <config.h>

configEntry configKeysRestartEUCANETD[] = {
  {"EUCALYPTUS", "/"},
  {"VNET_BRIDGE", NULL},
  {"VNET_BROADCAST", NULL},
  {"VNET_DHCPDAEMON", "/usr/sbin/dhcpd3"},
  {"VNET_DHCPUSER", "dhcpd"},
  {"VNET_DNS", NULL},
  {"VNET_DOMAINNAME", "eucalyptus.internal"},
  {"VNET_MODE", "SYSTEM"},
  {"VNET_NETMASK", NULL},
  {"VNET_PRIVINTERFACE", "eth0"},
  {"VNET_PUBINTERFACE", "eth0"},
  {"VNET_PUBLICIPS", NULL},
  {"VNET_PRIVATEIPS", NULL},
  {"VNET_ROUTER", NULL},
  {"VNET_SUBNET", NULL},
  {"VNET_MACPREFIX", "d0:0d"},
  {NULL, NULL},
};

configEntry configKeysNoRestartEUCANETD[] = {
  {"CC_POLLING_FREQUENCY", "1"},
  {"LOGLEVEL", "INFO"},
  {"LOGROLLNUMBER", "10"},
  {"LOGMAXSIZE", "104857600"},
  {"LOGPREFIX", ""},
  {"LOGFACILITY", ""},
  {NULL, NULL},
};

#endif
