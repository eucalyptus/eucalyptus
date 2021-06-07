// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

#ifndef _INCLUDE_MIDONET_API_H_
#define _INCLUDE_MIDONET_API_H_

#include <pthread.h>
#include <curl/curl.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "eucanetd.h"
#include "eucanetd_util.h"

//!
//! @file net/midonet-api.h
//! Need definition
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#define VPCMIDO_TENANT     "euca_tenant_1"
#define VPCMIDO_TUNNELZONE "mido-tz midotz euca-tz eucatz"
#define VPCMIDO_DEFAULT_TZ "mido-tz"
#define VPCMIDO_EUCAVER    "euca_version"
#define VPCMIDO_CORERT     "eucart"
#define VPCMIDO_COREBR     "eucabr"
#define VPCMIDO_CORERTPG   "eucapg"
#define VPCMIDO_MDRT       "eucamdrt"
#define VPCMIDO_MDBR       "eucamdbr"
#define VPCMIDO_MD_VETH_H  "eucamd_host"
#define VPCMIDO_MD_VETH_M  "eucamd_mido"
#define VPCMIDO_MD_NETNS   "eucamd"

// Internal Router IDs that conflict with 169.254.169.248 through 169.254.169.255
#define RTID_169_255 10751
#define RTID_169_248 10744

#define MIDO_HOST_INTERFACE_PHYSICAL           0x00000001
#define MIDO_HOST_INTERFACE_VIRTUAL            0x00000002
#define MIDO_HOST_INTERFACE_TUNNEL             0x00000004
#define MIDO_HOST_INTERFACE_UNKNOWN            0x00000008
#define MIDO_HOST_INTERFACE_ENDPOINT_PHYSICAL  0x00000100
#define MIDO_HOST_INTERFACE_ENDPOINT_LOCALHOST 0x00000200
#define MIDO_HOST_INTERFACE_ENDPOINT_DATAPAH   0x00000400
#define MIDO_HOST_INTERFACE_ENDPOINT_UNKNOWN   0x00000800
#define MIDO_HOST_INTERFACE_ENDPOINT_ALL       0xFFFFFFFF
#define MIDO_HOST_INTERFACE_ALL                0xFFFFFFFF

#define MIDONAME_LIST_CAPACITY_STEP            1000
#define MIDONAME_LIST_RELEASES_B4INVALIDATE    36000

#define MIDONET_API_RELOAD_THREADS             6
#define MIDONET_API_USE_THREADS_THRESHOLD      100

#define MIDO_CACHE_THREAD_NAME_LEN             8

#define MIDONET_API_BASE_URL_8080              "http://127.0.0.1:8080/midonet-api"
#define MIDONET_API_BASE_URL_8181              "http://127.0.0.1:8181/midonet-api"

#define MIDONET_API_V19                        "v1.9"
#define MIDONET_API_V50                        "v5.0"

#define APPLICATION_JSON_V5                               "application/vnd.org.midonet.Application-v5+json"
#define APPLICATION_HOST_INTERFACE_PORT_V1                "application/vnd.org.midonet.HostInterfacePort-v1+json"
#define APPLICATION_PORT_LINK_V1                          "application/vnd.org.midonet.PortLink-v1+json"
#define APPLICATION_BGP_JSON_V1                           "application/vnd.org.midonet.Bgp-v1+json"
#define APPLICATION_BGP_PEER_JSON_V1                      "application/vnd.org.midonet.BgpPeer-v1+json"
#define APPLICATION_AD_ROUTE_JSON_V1                      "application/vnd.org.midonet.AdRoute-v1+json"
#define APPLICATION_BGP_NETWORK_JSON_V1                   "application/vnd.org.midonet.BgpNetwork-v1+json"
#define APPLICATION_BRIDGE_JSON_V2                        "application/vnd.org.midonet.Bridge-v2+json"
#define APPLICATION_BRIDGE_JSON_V4                        "application/vnd.org.midonet.Bridge-v4+json"
#define APPLICATION_IP4MAC_JSON_V1                        "application/vnd.org.midonet.IP4Mac-v1+json"
#define APPLICATION_MACPORT_JSON_V2                       "application/vnd.org.midonet.MacPort-v2+json"
#define APPLICATION_DHCP_SUBNET_JSON_V2                   "application/vnd.org.midonet.DhcpSubnet-v2+json"
#define APPLICATION_DHCP_HOST_JSON_V2                     "application/vnd.org.midonet.DhcpHost-v2+json"
#define APPLICATION_PORT_JSON_V2                          "application/vnd.org.midonet.Port-v2+json"
#define APPLICATION_PORT_JSON_V3                          "application/vnd.org.midonet.Port-v3+json"
#define APPLICATION_CHAIN_JSON_V1                         "application/vnd.org.midonet.Chain-v1+json"
#define APPLICATION_RULE_JSON_V2                          "application/vnd.org.midonet.Rule-v2+json"
#define APPLICATION_HOST_JSON_V2                          "application/vnd.org.midonet.Host-v2+json"
#define APPLICATION_HOST_JSON_V3                          "application/vnd.org.midonet.Host-v3+json"
#define APPLICATION_INTERFACE_JSON_V1                     "application/vnd.org.midonet.Interface-v1+json"
#define APPLICATION_IP_ADDR_GROUP_JSON_V1                 "application/vnd.org.midonet.IpAddrGroup-v1+json"
#define APPLICATION_IP_ADDR_GROUP_ADDR_JSON_V1            "application/vnd.org.midonet.IpAddrGroupAddr-v1+json"
#define APPLICATION_PORT_GROUP_JSON_V1                    "application/vnd.org.midonet.PortGroup-v1+json"
#define APPLICATION_PORT_GROUP_PORT_JSON_V1               "application/vnd.org.midonet.PortGroupPort-v1+json"
#define APPLICATION_ROUTER_JSON_V2                        "application/vnd.org.midonet.Router-v2+json"
#define APPLICATION_ROUTER_JSON_V3                        "application/vnd.org.midonet.Router-v3+json"
#define APPLICATION_ROUTE_JSON_V1                         "application/vnd.org.midonet.Route-v1+json"
#define APPLICATION_TUNNEL_ZONE_JSON_V1                   "application/vnd.org.midonet.TunnelZone-v1+json"
#define APPLICATION_TUNNEL_ZONE_HOST_JSON_V1              "application/vnd.org.midonet.TunnelZoneHost-v1+json"
#define APPLICATION_SYSTEM_STATE_JSON_V2                  "application/vnd.org.midonet.SystemState-v2+json"
#define APPLICATION_COLLECTION_BGP_JSON_V1                "application/vnd.org.midonet.collection.Bgp-v1+json"
#define APPLICATION_COLLECTION_BGP_PEER_JSON_V1           "application/vnd.org.midonet.collection.BgpPeer-v1+json"
#define APPLICATION_COLLECTION_AD_ROUTE_JSON_V1           "application/vnd.org.midonet.collection.AdRoute-v1+json"
#define APPLICATION_COLLECTION_BGP_NETWORK_JSON_V1        "application/vnd.org.midonet.collection.BgpNetwork-v1+json"
#define APPLICATION_COLLECTION_BRIDGE_JSON_V2             "application/vnd.org.midonet.collection.Bridge-v2+json"
#define APPLICATION_COLLECTION_BRIDGE_JSON_V4             "application/vnd.org.midonet.collection.Bridge-v4+json"
#define APPLICATION_COLLECTION_IP4MAC_JSON_V1             "application/vnd.org.midonet.collection.IP4Mac-v1+json"
#define APPLICATION_COLLECTION_MACPORT_JSON_V2            "application/vnd.org.midonet.collection.MacPort-v2+json"
#define APPLICATION_COLLECTION_DHCP_SUBNET_JSON_V2        "application/vnd.org.midonet.collection.DhcpSubnet-v2+json"
#define APPLICATION_COLLECTION_DHCP_HOST_JSON_V2          "application/vnd.org.midonet.collection.DhcpHost-v2+json"
#define APPLICATION_COLLECTION_PORT_JSON_V2               "application/vnd.org.midonet.collection.Port-v2+json"
#define APPLICATION_COLLECTION_PORT_JSON_V3               "application/vnd.org.midonet.collection.Port-v3+json"
#define APPLICATION_COLLECTION_CHAIN_JSON_V1              "application/vnd.org.midonet.collection.Chain-v1+json"
#define APPLICATION_COLLECTION_RULE_JSON_V2               "application/vnd.org.midonet.collection.Rule-v2+json"
#define APPLICATION_COLLECTION_HOST_JSON_V2               "application/vnd.org.midonet.collection.Host-v2+json"
#define APPLICATION_COLLECTION_HOST_JSON_V3               "application/vnd.org.midonet.collection.Host-v3+json"
#define APPLICATION_COLLECTION_INTERFACE_JSON_V1          "application/vnd.org.midonet.collection.Interface-v1+json"
#define APPLICATION_COLLECTION_IP_ADDR_GROUP_JSON_V1      "application/vnd.org.midonet.collection.IpAddrGroup-v1+json"
#define APPLICATION_COLLECTION_IP_ADDR_GROUP_ADDR_JSON_V1 "application/vnd.org.midonet.collection.IpAddrGroupAddr-v1+json"
#define APPLICATION_COLLECTION_PORT_GROUP_JSON_V1         "application/vnd.org.midonet.collection.PortGroup-v1+json"
#define APPLICATION_COLLECTION_PORT_GROUP_PORT_JSON_V1    "application/vnd.org.midonet.collection.PortGroupPort-v1+json"
#define APPLICATION_COLLECTION_ROUTER_JSON_V2             "application/vnd.org.midonet.collection.Router-v2+json"
#define APPLICATION_COLLECTION_ROUTER_JSON_V3             "application/vnd.org.midonet.collection.Router-v3+json"
#define APPLICATION_COLLECTION_ROUTE_JSON_V1              "application/vnd.org.midonet.collection.Route-v1+json"
#define APPLICATION_COLLECTION_TUNNEL_ZONE_JSON_V1        "application/vnd.org.midonet.collection.TunnelZone-v1+json"
#define APPLICATION_COLLECTION_TUNNEL_ZONE_HOST_JSON_V1   "application/vnd.org.midonet.collection.TunnelZoneHost-v1+json"

#define MIDO_MTYPE_MAX_LEN 128

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

enum mido_chain_rule_elements_t {
    MIDO_CRULE_TYPE,
    MIDO_CRULE_PROTO,
    MIDO_CRULE_NW,
    MIDO_CRULE_NWLEN,
    MIDO_CRULE_TPS,
    MIDO_CRULE_TPS_S,
    MIDO_CRULE_TPS_E,
    MIDO_CRULE_TPS_INV,
    MIDO_CRULE_TPS_END,
    MIDO_CRULE_TPD,
    MIDO_CRULE_TPD_S,
    MIDO_CRULE_TPD_E,
    MIDO_CRULE_TPD_INV,
    MIDO_CRULE_TPD_END,
    MIDO_CRULE_GRPUUID,
    MIDO_CRULE_END
};

enum mido_cache_refresh_mode_t {
    MIDO_CACHE_REFRESH_ALL,
    MIDO_CACHE_REFRESH_NOHOSTS,
    MIDO_CACHE_REFRESH_NONE
};

enum mido_cache_pthreads_t {
    MIDO_CACHE_THREAD_ROUTER,
    MIDO_CACHE_THREAD_BRIDGE,
    MIDO_CACHE_THREAD_CHAIN,
    MIDO_CACHE_THREAD_IPAG,
    MIDO_CACHE_THREAD_END
};

enum mido_mtype_index_t {
    APPLICATION_JSON,
    APPLICATION_HOST_INTERFACE_PORT,
    APPLICATION_PORT_LINK,
    APPLICATION_BGP_JSON,
    APPLICATION_BGP_PEER_JSON,
    APPLICATION_AD_ROUTE_JSON,
    APPLICATION_BGP_NETWORK_JSON,
    APPLICATION_BRIDGE_JSON,
    APPLICATION_IP4MAC_JSON,
    APPLICATION_MACPORT_JSON,
    APPLICATION_DHCP_SUBNET_JSON,
    APPLICATION_DHCP_HOST_JSON,
    APPLICATION_PORT_JSON,
    APPLICATION_CHAIN_JSON,
    APPLICATION_RULE_JSON,
    APPLICATION_HOST_JSON,
    APPLICATION_INTERFACE_JSON,
    APPLICATION_IP_ADDR_GROUP_JSON,
    APPLICATION_IP_ADDR_GROUP_ADDR_JSON,
    APPLICATION_PORT_GROUP_JSON,
    APPLICATION_PORT_GROUP_PORT_JSON,
    APPLICATION_ROUTER_JSON,
    APPLICATION_ROUTE_JSON,
    APPLICATION_TUNNEL_ZONE_JSON,
    APPLICATION_TUNNEL_ZONE_HOST_JSON,
    APPLICATION_SYSTEM_STATE_JSON,
    APPLICATION_COLLECTION_BGP_JSON,
    APPLICATION_COLLECTION_BGP_PEER_JSON,
    APPLICATION_COLLECTION_AD_ROUTE_JSON,
    APPLICATION_COLLECTION_BGP_NETWORK_JSON,
    APPLICATION_COLLECTION_BRIDGE_JSON,
    APPLICATION_COLLECTION_IP4MAC_JSON,
    APPLICATION_COLLECTION_MACPORT_JSON,
    APPLICATION_COLLECTION_DHCP_SUBNET_JSON,
    APPLICATION_COLLECTION_DHCP_HOST_JSON,
    APPLICATION_COLLECTION_PORT_JSON,
    APPLICATION_COLLECTION_CHAIN_JSON,
    APPLICATION_COLLECTION_RULE_JSON,
    APPLICATION_COLLECTION_HOST_JSON,
    APPLICATION_COLLECTION_INTERFACE_JSON,
    APPLICATION_COLLECTION_IP_ADDR_GROUP_JSON,
    APPLICATION_COLLECTION_IP_ADDR_GROUP_ADDR_JSON,
    APPLICATION_COLLECTION_PORT_GROUP_JSON,
    APPLICATION_COLLECTION_PORT_GROUP_PORT_JSON,
    APPLICATION_COLLECTION_ROUTER_JSON,
    APPLICATION_COLLECTION_ROUTE_JSON,
    APPLICATION_COLLECTION_TUNNEL_ZONE_JSON,
    APPLICATION_COLLECTION_TUNNEL_ZONE_HOST_JSON,
    APPLICATION_MAX_INDEX
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct midoname_ipagip_extras_t {
    char *ip;
} midoname_ipagip_extras;

typedef struct midoname_rule_extras_t {
    char *type;
    char *nwdstaddress;
    char *nwdstlength;
    char *nwsrcaddress;
    char *nwsrclength;
    char *nattarget;
    char *jumpchainid;
} midoname_rule_extras;

typedef struct midoname_port_extras_t {
    char *type;
    char *hostid;
    char *peerid;
    char *ifname;
    char *netaddr;
    char *netlen;
    char *portaddr;
    char *portmac;
} midoname_port_extras;

typedef struct midoname_route_extras_t {
    char *srcnet;
    char *srclen;
    char *dstnet;
    char *dstlen;
    char *type;
    char *nexthopport;
    char *nexthopgateway;
    char *weight;
} midoname_route_extras;

typedef struct midoname_ip4mac_extras_t {
    char *ip;
    char *mac;
} midoname_ip4mac_extras;

typedef struct midoname_macport_extras_t {
    char *macAddr;
    char *portId;
} midoname_macport_extras;

typedef struct midoname_t {
    char *tenant;
    char *name;
    char *uuid;
    char *jsonbuf;
    char *resource_type;
    char *media_type;
    char *uri;
    midoname_ipagip_extras *ipagip;
    midoname_rule_extras *rule;
    midoname_port_extras *port;
    midoname_route_extras *route;
    midoname_ip4mac_extras *ip4mac;
    midoname_macport_extras *macport;
    int tag;
    int init;
} midoname;

typedef struct midoname_list_t {
    midoname **mnames;
    int size;
    int capacity;
    int released;
} midoname_list;

typedef struct midonet_api_router_t {
    midoname *obj;
    midoname **ports;
    int max_ports;
    midoname **routes;
    int max_routes;
} midonet_api_router;

typedef struct midonet_api_dhcp_t {
    midoname *obj;
    midoname **dhcphosts;
    int max_dhcphosts;
    int sorted_dhcphosts;
} midonet_api_dhcp;

typedef struct midonet_api_bridge_t {
    midoname *obj;
    midoname **ports;
    int max_ports;
    midoname **ip4mac_pairs;
    int max_ip4mac_pairs;
    midoname **macport_pairs;
    int max_macport_pairs;
    midonet_api_dhcp **dhcps;
    int max_dhcps;
} midonet_api_bridge;

typedef struct midonet_api_chain_t {
    midoname *obj;
    midoname **rules;
    int max_rules;
    int rules_count;
} midonet_api_chain;

typedef struct midonet_api_host_t {
    midoname *obj;
    //midoname **ports;
    //int max_ports;
    u32 *addresses;
    int max_addresses;
} midonet_api_host;

typedef struct midonet_api_ipaddrgroup_t {
    midoname *obj;
    midoname **ips;
    u32 *hexips;
    int max_ips;
    int ips_count;
} midonet_api_ipaddrgroup;

typedef struct midonet_api_portgroup_t {
    midoname *obj;
    midoname **ports;
    int max_ports;
} midonet_api_portgroup;

typedef struct midonet_api_tunnelzone_t {
    midoname *obj;
    midoname **hosts;
    int max_hosts;
} midonet_api_tunnelzone;

typedef struct midonet_api_iphostmap_entry_t {
    u32 ip;
    midonet_api_host *host;
} midonet_api_iphostmap_entry;

typedef struct midonet_api_iphostmap_t {
    midonet_api_iphostmap_entry *entries;
    int max_entries;
    int sorted;
} midonet_api_iphostmap;

typedef struct midonet_api_cache_t {
    midoname **ports;
    int max_ports;
    midonet_api_router **routers;
    int max_routers;
    int sorted_routers;
    midonet_api_bridge **bridges;
    int max_bridges;
    int sorted_bridges;
    midonet_api_chain **chains;
    int max_chains;
    int sorted_chains;
    midonet_api_host **hosts;
    int max_hosts;
    midonet_api_ipaddrgroup **ipaddrgroups;
    int max_ipaddrgroups;
    int sorted_ipaddrgroups;
    midonet_api_portgroup **portgroups;
    int max_portgroups;
    midonet_api_tunnelzone **tunnelzones;
    int max_tunnelzones;
    midonet_api_iphostmap iphostmap;
} midonet_api_cache;

typedef struct mido_parsed_route_t {
    midoname router;
    midoname rport;
    char *src_net;
    char *src_length;
    char *dst_net;
    char *dst_length;
    char *next_hop_ip;
    char *weight;
    int mido_present;
} mido_parsed_route;

typedef struct mido_parsed_chain_rule_t {
    char jsonel[MIDO_CRULE_END][64];
} mido_parsed_chain_rule;

typedef int (*loadobj) (midonet_api_cache *cache, int start, int end);

typedef struct mido_cache_worker_thread_params_t {
    int start;
    int end;
    int rc;
    char name[8];
    midonet_api_cache *cache;
    loadobj get_from_mido;
} mido_cache_worker_thread_params;

typedef struct mido_cache_main_thread_params_t {
    int rc;
    int n;
    char name[8];
    midonet_api_cache *cache;
    loadobj get_from_mido;
} mido_cache_main_thread_params;

typedef struct mido_libcurl_handles_t {
    int max_handles;
    int max_gethandles;
    CURL **handles;
    CURL **gethandles;
} mido_libcurl_handles;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/
extern int http_gets;
extern int http_posts;
extern int http_puts;
extern int http_deletes;

extern char midonet_api_version[16];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//int mido_allocate_midorule(char *position, char *type, char *action, char *protocol, char *srcIAGuuid, char *src_port_min, char *src_port_max,  char *dstIAGuuid, char *dst_port_min, char *dst_port_max, char *matchForwardFlow, char *matchReturnFlow, char *nat_target, char *nat_port_min, char *nat_port_max, midorule *outrule);

mido_cache_worker_thread_params *prep_thread_params(int ntasks, int nthreads);

int iplist_split(char *iplist, char ***outiparr, int *max_outiparr);
int iplist_arr_free(char **iparr, int max_iparr);

int routername_split(char *routername, char **name, int *id);

void mido_info_midonetapi(void);
void mido_info_midocache(void);

void mido_info_http_count(void);
void mido_info_http_count_total(void);

void mido_free_mido_parsed_route(mido_parsed_route *route);
void mido_free_mido_parsed_route_list(mido_parsed_route *routes, int max_routes);

int mido_create_midoname(char *tenant, char *name, char *uuid, char *resource_type, char *media_type, char *uri, char *jsonbuf, midoname * outname);
void mido_free_midoname(midoname *name);
void mido_free_midoname_list(midoname *name, int max_name);
int mido_update_midoname(midoname *name);
void mido_copy_midoname(midoname * dst, midoname * src);
int mido_getel_midoname(midoname *name, char *key, char **val);
int mido_getarr_midoname(midoname *name, char *key, char ***values, int *max_values);
void mido_print_midoname(midoname *name);
int mido_cmp_midoname(midoname *a, midoname *b);
int mido_merge_midoname_lists(midoname *lista, int lista_max, midoname *listb, int listb_max, midoname **addmidos, int addmidos_max, midoname **delmidos, int delmidos_max);

int mido_check_state(void);
char *mido_get_apiuribase(void);
void mido_set_apiuribase(const char *apiuribase);
int mido_initialize_apiuribase(void);

midonet_api_tunnelzone *mido_create_tunnelzone(char *name, char *type, midoname **outname);
int mido_get_tunnelzones(char *tenant, midoname ***outnames, int *outnames_max);
int mido_get_tunnelzone_hosts(midoname *tzone, midoname ***outnames, int *outnames_max);
int mido_create_tunnelzone_member(midonet_api_tunnelzone *tz, midoname *tzmn, midoname *host, char *ip, midoname **outname);

midonet_api_bridge *mido_create_bridge(char *tenant, char *name, midoname **outname);
int mido_update_bridge(midoname *name, ...);
int mido_print_bridge(midoname *name);
int mido_delete_bridge(midoname *name);
int mido_get_bridges(char *tenant, midoname ***outnames, int *outnames_max);
midonet_api_bridge *mido_get_bridge(char *name);

midonet_api_router *mido_create_router(char *tenant, char *name, midoname **outname);
int mido_update_router(midoname *name, ...);
int mido_delete_router(midoname *name);
int mido_print_router(midoname *name);
int mido_get_routers(char *tenant, midoname ***outnames, int *outnames_max);
midonet_api_router *mido_get_router(char *name);

int mido_find_route_from_list(midoname **routes, int max_routes, midoname *rport,
        char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip,
        char *weight, int *foundidx);
int mido_create_route(midonet_api_router *rt, midoname *router, midoname *rport, char *src, char *src_slashnet,
        char *dst, char *dst_slashnet, char *next_hop_ip, char *weight, midoname **outname);
int mido_delete_route(midonet_api_router *router, midoname *name);
int mido_get_routes(midoname *router, midoname ***outnames, int *outnames_max);

int mido_create_bgp(midoname *dev, u32 localAS, u32 peerAS, char *peerAddr, midoname **outname);
int mido_create_bgp_v1(midoname *port, u32 localAS, u32 peerAS, char *peerAddr, midoname **outname);
int mido_create_bgp_v5(midoname *router, u32 localAS, u32 peerAS, char *peerAddr, midoname **outname);
int mido_get_bgps(midoname *dev, midoname ***outnames, int *outnames_max);
int mido_get_bgps_v1(midoname *port, midoname ***outnames, int *outnames_max);
int mido_get_bgps_v5(midoname *router, midoname ***outnames, int *outnames_max);
int mido_create_bgp_route(midoname *dev, char *nwPrefix, char *prefixLength, midoname **outname);
int mido_create_bgp_route_v1(midoname *bgp, char *nwPrefix, char *prefixLength, midoname **outname);
int mido_create_bgp_route_v5(midoname *router, char *nwPrefix, char *prefixLength, midoname **outname);
int mido_get_bgp_routes(midoname *dev, midoname ***outnames, int *outnames_max);
int mido_get_bgp_routes_v1(midoname *bgp, midoname ***outnames, int *outnames_max);
int mido_get_bgp_routes_v5(midoname *router, midoname ***outnames, int *outnames_max);

int mido_create_ip4mac(midonet_api_bridge *br, midoname *devname, char *ip, char *mac, midoname **outname);
int mido_delete_ip4mac(midonet_api_bridge *bridge, midoname *ip4mac);
int mido_delete_ip4mac_byip(midonet_api_bridge *bridge, char *ip);
int mido_get_ip4mac_pairs(midoname *bridgename, midoname ***outnames, int *outnames_max);

int mido_create_macport(midonet_api_bridge *br, midoname *devname, char *macAddr, char *portId, midoname **outname);
int mido_delete_macport(midonet_api_bridge *bridge, midoname *macport);
int mido_delete_macport_bymac(midonet_api_bridge *bridge, char *mac);
int mido_delete_macport_byport(midonet_api_bridge *bridge, char *portId);
int mido_get_macport_pairs(midoname *bridgename, midoname ***outnames, int *outnames_max);

int mido_find_dhcp_from_list(midoname **dhcps, int max_dhcps, char *subnet, char *slashnet,
        char *gw, u32 *dnsServers, int max_dnsServers, int *foundidx);
int mido_create_dhcp(midonet_api_bridge *br, midoname *devname, char *subnet, char *slashnet, char *gw, u32 *dnsServers, int max_dnsServers, midoname **outname);
int mido_update_dhcp(midoname *name, ...);
int mido_print_dhcp(midoname *name);
int mido_delete_dhcp(midonet_api_bridge *devname, midoname *name);
int mido_get_dhcps(midoname *devname, midoname ***outnames, int *outnames_max);

int mido_find_dhcphost_from_list(midoname **dhcphosts, int max_dhcphosts, char *name,
        char *mac, char *ip, char *dns_domain, int *foundidx);
int mido_create_dhcphost(midonet_api_bridge *bridge, midoname *dhcp, char *name, char *mac, char *ip, char *dns_domain, midoname **outname);
int mido_delete_dhcphost(midoname *bridge, midoname *dhcp, midoname *name);
int mido_get_dhcphosts(midoname *devname, midoname *dhcp, midoname ***outnames, int *outnames_max);
midoname *mido_get_dhcphost(midonet_api_dhcp *dhcp, char *dhcphostname);

int mido_create_portgroup(char *tenant, char *name, midoname **outname);
int mido_update_portgroup(midoname *name, ...);
int mido_delete_portgroup(midoname *name);
int mido_print_portgroup(midoname *name);
int mido_get_portgroups(char *tenant, midoname ***outnames, int *outnames_max);
midonet_api_portgroup *mido_get_portgroup(char *name);

int mido_create_portgroup_port(midoname *portgroup, midoname *port, midoname **outname);
int mido_delete_portgroup_port(midoname *portgroup, midoname *port);
int mido_get_portgroup_ports(midoname *portgroup, midoname ***outnames, int *outnames_max);

int mido_create_port(midoname *devname, char *port_type, char *ip, char *nw, char *slashnet, char *mac, midoname **outname);
int mido_create_bridge_port(midonet_api_bridge *br, midoname *devname, midoname **outname);
int mido_create_router_port(midonet_api_router *rt, midoname *devname, char *ip, char *nw, char *slashnet, char *mac, midoname **outname);
int mido_find_port_from_list(midoname **ports, int max_ports, char *ip, char *nw, char *slashnet, char *mac, int *foundidx);
int mido_update_port(midoname *name, ...);
int mido_print_port(midoname *name);
int mido_delete_bridge_port(midonet_api_bridge *bridge, midoname *port);
int mido_delete_router_port(midonet_api_router *router, midoname *port);
int mido_get_ports(midoname *devname, midoname ***outnames, int *outnames_max);
int mido_refresh_port(midoname *port);

int mido_get_device_ports(midoname **ports, int max_ports, midoname *device, midoname ***outports, int *outports_max);
int mido_get_host_ports(midoname **ports, int max_ports, midoname *host, midoname ***outports, int *outports_max);

int mido_link_ports(midoname *a, midoname *b);

int mido_link_host_port(midoname *host, char *interface, midoname *device, midoname *port);
int mido_unlink_host_port(midoname *host, midoname *port);

int mido_get_hosts(midoname ***outnames, int *outnames_max);
midonet_api_host *mido_get_host(char *name, char *uuid);
midonet_api_host *mido_get_host_byname(char *hostname);
midonet_api_host *mido_get_host_byip(char *ip);
int mido_get_interfaces(midoname *host, u32 iftype, u32 ifendpoint, midoname **outnames, int *outnames_max);
int mido_get_interface(midoname *host, char *dev, midoname *outname);
int mido_get_addresses(midoname *host, u32 **outnames, int *outnames_max);
int mido_get_address(midoname *host, char *dev, u32 *outaddress);

midonet_api_chain *mido_create_chain(char *tenant, char *name, midoname **outname);
int mido_update_chain(midoname *name, ...);
int mido_print_chain(midoname *name);
int mido_delete_chain(midoname *name);
int mido_get_chains(char *tenant, midoname ***outnames, int *outnames_max);
int mido_get_chains_with_prefix(char *tenant, char *prefix, midoname ***outnames, int *outnames_max);
midonet_api_chain *mido_get_chain(char *name);

int mido_create_rule(midonet_api_chain *ch, midoname *chain, midoname **outname, int *next_position, ...);
int mido_find_rule_from_list(midoname **rules, int max_rules, midoname **outrule, ...);
int mido_find_rule_from_list_v(midoname **rules, int max_rules, midoname **outrule, va_list *al);
int mido_print_rule(midoname *name);
int mido_delete_rule(midonet_api_chain *chain, midoname *rule);
int mido_get_rules(midoname *chainname, midoname ***outnames, int *outnames_max);
int mido_reload_rules(midonet_api_chain *chain);
int mido_get_jump_rules(midonet_api_chain *chain, midoname ***outnames, int *outnames_max,
        char ***jumptargets, int *jumptargets_max);
int mido_clear_rules(midonet_api_chain *chain);

midonet_api_ipaddrgroup *mido_create_ipaddrgroup(char *tenant, char *name, midoname **outname);
int mido_update_ipaddrgroup(midoname *name, ...);
int mido_delete_ipaddrgroup(midoname *name);
int mido_print_ipaddrgroup(midoname *name);
int mido_get_ipaddrgroups(char *tenant, midoname ***outnames, int *outnames_max);
midonet_api_ipaddrgroup *mido_get_ipaddrgroup(char *name);

int mido_create_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipag, midoname *ipaddrgroup, char *ip, midoname **outname);
int mido_find_ipaddrgroup_ip_from_list(midoname **ips, int max_ips, char *ip, midoname **outip);
int mido_delete_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, midoname *ipaddrgroup_ip);
int mido_get_ipaddrgroup_ips(midoname *ipaddrgroup, midoname ***outnames, int *outnames_max);
midoname *mido_get_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, int pos);

int mido_create_resource(midoname *parents, int max_parents, midoname *newname, midoname **outname, ...);
int mido_create_resource_v(midoname *parents, int max_parents, midoname *newname, midoname **outname, va_list * al);
int mido_update_resource(midoname *name, va_list *al);
int mido_print_resource(char *resource_type, midoname *name);
int mido_delete_resource(midoname * parentname, midoname *name);
int mido_get_resources(midoname * parents, int max_parents, char *tenant, char *resource_type, char *apistr, char *mtype, midoname ***outnames, int *outnames_max);
int mido_refresh_resource(midoname *resc);

int mido_cmp_midoname_to_input(midoname *name, ...);
int mido_cmp_midoname_to_input_json(midoname *name, ...);
int mido_cmp_midoname_to_input_json_v(midoname *name, va_list * al);
int mido_cmp_jsons(char *jsonsrc, char *jsondst, char *type);
int mido_cmp_midoname_jsonbuf(midoname *a, midoname *b);
char *mido_get_json(char *tenant, ...);
char *mido_jsonize(char *tenant, va_list * al);

void midonet_api_init(void);
void midonet_api_cleanup(void);
char *midonet_api_get_version(char **version);

char *midonet_api_get_uribase(char **uribase);
int mido_libcurl_cleanup_handles(mido_libcurl_handles *handles);
int mido_libcurl_init(mido_libcurl_handles *handles);
int mido_libcurl_cleanup(mido_libcurl_handles *handles);
CURL *mido_libcurl_get_handle(mido_libcurl_handles *handles);
CURL *mido_libcurl_get_gethandle(mido_libcurl_handles *handles);
int mido_libcurl_release_handle(mido_libcurl_handles *handles, CURL *handle);
int mido_libcurl_release_gethandle(mido_libcurl_handles *handles, CURL *handle);

int midonet_http_get(char *url, char *apistr, char **out_payload);
int midonet_http_put(char *url, char *apistr, char *payload);
int midonet_http_post(char *url, char *apistr, char *payload, char **out_payload);
int midonet_http_delete(char *url);

midoname_list *midoname_list_new(void);
int midoname_list_free(midoname_list *list);
midoname *midoname_list_get_midoname(midoname_list *list);
int midoname_list_get_midonames(midoname_list *list, midoname ***outnames, int max_outnames);

midonet_api_cache *midonet_api_cache_init(void);
midoname_list *midonet_api_cache_midos_init(void);
midoname_list *midonet_api_cache_midos_get(void);

midonet_api_cache *midonet_api_cache_get(void);
int midonet_api_cache_check(void);
int midonet_api_cache_flush(midonet_api_cache *cache);
int midonet_api_cache_populate(void);
int midonet_api_cache_refresh(void);
int midonet_api_cache_refresh_v(enum mido_cache_refresh_mode_t refreshmode);
int midonet_api_cache_refresh_v_threads(enum mido_cache_refresh_mode_t refreshmode);

int midonet_api_cache_refresh_routerroutes(midonet_api_cache *cache, int start, int end);
int midonet_api_cache_refresh_bridgeobjects(midonet_api_cache *cache, int start, int end);
int midonet_api_cache_refresh_chainrules(midonet_api_cache *cache, int start, int end);
int midonet_api_cache_refresh_ipagips(midonet_api_cache *cache, int start, int end);

void *midonet_api_cache_refresh_objects_worker_thread(void *worker_param);
void *midonet_api_cache_refresh_objects_main_thread(void *main_params);

int midonet_api_cache_refresh_tunnelzones(midonet_api_cache *cache);
int midonet_api_cache_refresh_hosts(midonet_api_cache *cache);
int midonet_api_cache_iphostmap_populate(midonet_api_cache *cache);
int midonet_api_cache_get_nhosts(midonet_api_cache *cache);
int midonet_api_cache_get_ntzhosts(midonet_api_cache *cache);

midonet_api_tunnelzone *midonet_api_cache_lookup_tunnelzone(midoname *tzone);
midonet_api_tunnelzone *midonet_api_cache_add_tunnelzone(midoname *tunnelzone);
int midonet_api_cache_add_tunnelzone_host(midonet_api_tunnelzone *tzone, midoname *host);

midonet_api_host *midonet_api_cache_lookup_host(midoname *name);

int midonet_api_cache_add_port(midoname *port);
int midonet_api_cache_add_bridge_port(midonet_api_bridge *bridge, midoname *port);
int midonet_api_cache_add_router_port(midonet_api_router *router, midoname *port);
int midonet_api_cache_del_port(midoname *port);
int midonet_api_cache_del_bridge_port(midonet_api_bridge *bridge, midoname *port);
int midonet_api_cache_del_router_port(midonet_api_router *router, midoname *port);
midoname *midonet_api_cache_lookup_port(midoname *port, int *idx);

int midonet_api_cache_get_bridges(midonet_api_bridge ***bridges, int *max_bridges);
midonet_api_bridge *midonet_api_cache_add_bridge(midoname *bridge);
int midonet_api_cache_del_bridge(midoname *bridge);
midonet_api_bridge *midonet_api_cache_lookup_bridge(midoname *bridge, int *idx);

int midonet_api_cache_add_ip4mac(midonet_api_bridge *bridge, midoname *ip4mac);
int midonet_api_cache_del_ip4mac(midonet_api_bridge *bridge, midoname *ip4mac);
midoname *midonet_api_cache_lookup_ip4mac(midonet_api_bridge *bridge, midoname *ip4mac, int *idx);
midoname *midonet_api_cache_lookup_ip4mac_byip(midonet_api_bridge *bridge, char *ip, int *idx);
midoname *midonet_api_cache_lookup_ip4mac_bymac(midonet_api_bridge *bridge, char *mac, int *idx);

int midonet_api_cache_add_macport(midonet_api_bridge *bridge, midoname *macport);
int midonet_api_cache_del_macport(midonet_api_bridge *bridge, midoname *macport);
midoname *midonet_api_cache_lookup_macport(midonet_api_bridge *bridge, midoname *macport, int *idx);
midoname *midonet_api_cache_lookup_macport_bymac(midonet_api_bridge *bridge, char *mac, int *idx);
midoname *midonet_api_cache_lookup_macport_byport(midonet_api_bridge *bridge, char *portId, int *idx);

int midonet_api_cache_add_dhcp(midonet_api_bridge *bridge, midoname *dhcp);
int midonet_api_cache_del_dhcp(midonet_api_bridge *bridge, midoname *dhcp);
midonet_api_dhcp *midonet_api_cache_lookup_dhcp(midonet_api_bridge *bridge, midoname *dhcp, int *idx);
midonet_api_dhcp *midonet_api_cache_lookup_dhcp_byparam(midonet_api_bridge *bridge,
        char *subnet, char *slashnet, char *gw, u32 *dnsServers, int max_dnsServers,
        int *idx);

int midonet_api_cache_add_dhcp_host(midonet_api_dhcp *dhcp, midoname *dhcphost);
int midonet_api_cache_del_dhcp_host(midonet_api_dhcp *dhcp, midoname *dhcphost);
midoname *midonet_api_cache_lookup_dhcp_host(midonet_api_dhcp *dhcp, midoname *dhcphost, int *idx);
midoname *midonet_api_cache_lookup_dhcp_host_byparam(midonet_api_dhcp *dhcp,
        char *name, char *mac, char *ip, char *dns_domain, int *idx);

int midonet_api_cache_get_routers(midonet_api_router ***routers, int *max_routers);
int midonet_api_cache_get_natg_routers(midonet_api_router ***routers, int *max_routers);
midonet_api_router *midonet_api_cache_add_router(midoname *router);
int midonet_api_cache_del_router(midoname *router);
midonet_api_router *midonet_api_cache_lookup_router(midoname *router, int *idx);
int midonet_api_cache_add_router_route(midonet_api_router *router, midoname *route);
int midonet_api_cache_del_router_route(midonet_api_router *router, midoname *route);

int midonet_api_cache_add_portgroup(midoname *pgroup);
int midonet_api_cache_del_portgroup(midoname *pgroup);
midonet_api_portgroup *midonet_api_cache_lookup_portgroup(midoname *pgroup, int *idx);
int midonet_api_cache_add_portgroup_port(midonet_api_portgroup *pgroup, midoname *port);
int midonet_api_cache_del_portgroup_port(midonet_api_portgroup *pgroup, midoname *port);
midoname *midonet_api_cache_lookup_portgroup_port(midonet_api_portgroup *pgroup, midoname *port, int *idx);

midonet_api_chain *midonet_api_cache_add_chain(midoname *chain);
int midonet_api_cache_del_chain(midoname *chain);
midonet_api_chain *midonet_api_cache_lookup_chain(midoname *chain, int *idx);
int midonet_api_cache_add_chain_rule(midonet_api_chain *chain, midoname *rule);
int midonet_api_cache_del_chain_rule(midonet_api_chain *chain, midoname *rule);

midonet_api_ipaddrgroup *midonet_api_cache_add_ipaddrgroup(midoname *ipaddrgroup);
int midonet_api_cache_del_ipaddrgroup(midoname *ipaddrgroup);
midonet_api_ipaddrgroup *midonet_api_cache_lookup_ipaddrgroup(midoname *ipaddrgroup, int *idx);
int midonet_api_cache_add_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, midoname *ip, u32 hexip);
int midonet_api_cache_del_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, midoname *ip);

int midonet_api_router_free(midonet_api_router *router);
int midonet_api_dhcp_free(midonet_api_dhcp *dhcp);
int midonet_api_bridge_free(midonet_api_bridge *bridge);
int midonet_api_chain_free(midonet_api_chain *chain);
int midonet_api_host_free(midonet_api_host *host);
int midonet_api_ipaddrgroup_free(midonet_api_ipaddrgroup *ipaddrgroup);
int midonet_api_portgroup_free(midonet_api_portgroup *portgroup);
int midonet_api_tunnelzone_free(midonet_api_tunnelzone *tunnelzone);

int midonet_api_delete_all(void);
int midonet_api_delete_dups(boolean checkonly);

int midonet_api_delete_unconnected_ports(midoname **ports, int max_ports, boolean checkonly);
int midonet_api_delete_dups_rules(midonet_api_chain *chain, boolean checkonly);
int midonet_api_delete_dups_routes(midonet_api_router *router, boolean checkonly);

int compare_midonet_api_iphostmap_entry(const void *p1, const void *p2);
int compare_midoname_name(const void *p1, const void *p2);
int compare_midonet_api_bridge(const void *p1, const void *p2);
int compare_midonet_api_router(const void *p1, const void *p2);
int compare_midonet_api_ipaddrgroup(const void *p1, const void *p2);
int compare_midonet_api_chain(const void *p1, const void *p2);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             INLINE IMPLEMENTATION                          |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/**
 * Check if MidoNet API matches version "v1.9"
 * @return TRUE if detected MidoNet API version is "v1.9". FALSE otherwise.
 */
inline boolean is_midonet_api_v1(void) {
    if (!strcmp(midonet_api_version, MIDONET_API_V19)) {
        return (TRUE);
    }
    return (FALSE);
}

/**
 * Check if MidoNet API matches version "v5.0"
 * @return TRUE if detected MidoNet API version is "v5.0". FALSE otherwise.
 */
inline boolean is_midonet_api_v5(void) {
    if (!strcmp(midonet_api_version, MIDONET_API_V50)) {
        return (TRUE);
    }
    return (FALSE);
}

#endif /* ! _INCLUDE_MIDONET_API_H_ */
