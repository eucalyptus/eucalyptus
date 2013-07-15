#ifndef INCLUDE_CONFIG_CC_H
#define INCLUDE_CONFIG_CC_H

configEntry configKeysRestartCC[] = {
    {"DISABLE_TUNNELING", "N"},
    {"ENABLE_WS_SECURITY", "Y"},
    {"EUCALYPTUS", "/"},
    {"NC_FANOUT", "1"},
    {"NC_PORT", "8775"},
    {"NC_SERVICE", "axis2/services/EucalyptusNC"},
    {"SCHEDPOLICY", "ROUNDROBIN"},
    {"VNET_ADDRSPERNET", NULL},
    {"VNET_BRIDGE", NULL},
    {"VNET_BROADCAST", NULL},
    {"VNET_DHCPDAEMON", "/usr/sbin/dhcpd3"},
    {"VNET_DHCPUSER", "dhcpd"},
    {"VNET_DNS", NULL},
    {"VNET_DOMAINNAME", "eucalyptus.internal"},
    {"VNET_LOCALIP", NULL},
    {"VNET_MACMAP", NULL},
    {"VNET_MODE", "SYSTEM"},
    {"VNET_NETMASK", NULL},
    {"VNET_PRIVINTERFACE", "eth0"},
    {"VNET_PUBINTERFACE", "eth0"},
    {"VNET_PUBLICIPS", NULL},
    {"VNET_PRIVATEIPS", NULL},
    {"VNET_ROUTER", NULL},
    {"VNET_SUBNET", NULL},
    {"VNET_MACPREFIX", "d0:0d"},
    {"POWER_IDLETHRESH", "300"},
    {"POWER_WAKETHRESH", "300"},
    {"CC_IMAGE_PROXY", NULL},
    {"CC_IMAGE_PROXY_CACHE_SIZE", "32768"},
    {"CC_IMAGE_PROXY_PATH", "$EUCALYPTUS" EUCALYPTUS_STATE_DIR "/dynserv/"},
    {NULL, NULL},
};

configEntry configKeysNoRestartCC[] = {
    {"NODES", NULL},
    {"NC_POLLING_FREQUENCY", "6"},
    {"CLC_POLLING_FREQUENCY", "6"},
    {"CC_ARBITRATORS", NULL},
    {"LOGLEVEL", "INFO"},
    {"LOGROLLNUMBER", "10"},
    {"LOGMAXSIZE", "104857600"},
    {"LOGPREFIX", ""},
    {"LOGFACILITY", ""},
    {NULL, NULL},
};

#endif
