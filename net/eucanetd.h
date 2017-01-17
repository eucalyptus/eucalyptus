// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/

#ifndef _INCLUDE_EUCANETD_H_
#define _INCLUDE_EUCANETD_H_

//!
//! @file net/eucanetd.h
//! Definition of the service management layer
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <data.h>
#include <atomic_file.h>
#include <config.h>
#include <data.h>
#include <ipt_handler.h>
#include <ips_handler.h>
#include <ebt_handler.h>
#include <atomic_file.h>
#include <euca_gni.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name The bitmask indicating which of the API needs to be executed after a successful system scrub

#define EUCANETD_RUN_NO_API                      0x00000000 //!< Bassically says don't do anything
#define EUCANETD_RUN_NETWORK_API                 0x00000001 //!< If set, this will trigger the core to run the implement_network() driver API
#define EUCANETD_RUN_SECURITY_GROUP_API          0x00000002 //!< If set, this will trigger the core to run the implement_sg() driver API
#define EUCANETD_RUN_ADDRESSING_API              0x00000004 //!< If set, this will trigger the core to run the implement_addressing() driver API
#define EUCANETD_RUN_ALL_API                     (EUCANETD_RUN_NETWORK_API | EUCANETD_RUN_SECURITY_GROUP_API | EUCANETD_RUN_ADDRESSING_API)
#define EUCANETD_RUN_ERROR_API                   0x80000000 //!< This is to indicate an error case
#define EUCANETD_VPCMIDO_IFERROR                 0xC0000000 //!< Error implementing interface(s) in VPCMIDO

//! @}

// eucanetd error codes
#define EUCANETD_ERR_VPCMIDO_API                 20
#define EUCANETD_ERR_VPCMIDO_CORE                30
#define EUCANETD_ERR_VPCMIDO_MD                  40
#define EUCANETD_ERR_VPCMIDO_TZ                  50
#define EUCANETD_ERR_VPCMIDO_POPULATE            60
#define EUCANETD_ERR_VPCMIDO_PASS1               70
#define EUCANETD_ERR_VPCMIDO_PASS2               80
#define EUCANETD_ERR_VPCMIDO_VPCS                90
#define EUCANETD_ERR_VPCMIDO_SGS                100
#define EUCANETD_ERR_VPCMIDO_ENIS               110
#define EUCANETD_ERR_VPCMIDO_OTHER              255

//! @{
//! @name Various known network IP and usual bitmask

#define INADDR_METADATA                          0xA9FEA9FE //!< Metadata IP address
#define IN_HOST_NET                              0xFFFFFFFF //!< 32 bit host network (not defined in in.h)

//! @}

#define NUM_EUCANETD_CONFIG                      1

/* Defines the bitmask for the flush mode */
#define EUCANETD_FLUSH_AND_RUN_MASK              0x01  //!< Will only flush and continue running
#define EUCANETD_FLUSH_ONLY_MASK                 0x02  //!< Will flush and stop running the daemon
#define EUCANETD_FLUSH_MASK                      0xFF  //!< Mask to see if we need to flush

#define EUCANETD_DUMMY_UDP_PORT                  63822

#define EUCANETD_DHCPD_UNIT                      "eucanetd-dhcpd@%s.service"
#define EUCANETD_NGINX_UNIT                      "eucanetd-nginx.service"

#define SYSCTL_BRIDGE_PATH                       "/proc/sys/net/bridge"
#define SYSCTL_BRIDGE_CALLIPTABLES               "/proc/sys/net/bridge/bridge-nf-call-iptables"
#define SYSCTL_IP_FORWARD                        "/proc/sys/net/ipv4/ip_forward"

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

// Enumeration of configuration constants found in eucalyptus.conf
enum {
    EUCANETD_CVAL_PUBINTERFACE,
    EUCANETD_CVAL_PRIVINTERFACE,
    EUCANETD_CVAL_BRIDGE,
    EUCANETD_CVAL_EUCAHOME,
    EUCANETD_CVAL_MODE,
    EUCANETD_CVAL_DHCPDAEMON,
    EUCANETD_CVAL_DHCPUSER,
    EUCANETD_CVAL_SYSTEMCTL,
    EUCANETD_CVAL_USE_SYSTEMCTL,
    EUCANETD_CVAL_POLLING_FREQUENCY,
    EUCANETD_CVAL_DISABLE_L2_ISOLATION,
    EUCANETD_CVAL_NC_PROXY,
    EUCANETD_CVAL_NC_ROUTER,
    EUCANETD_CVAL_NC_ROUTER_IP,
    EUCANETD_CVAL_METADATA_USE_VM_PRIVATE,
    EUCANETD_CVAL_METADATA_IP,
    EUCANETD_CVAL_ADDRSPERNET,
    EUCANETD_CVAL_EUCA_USER,
    EUCANETD_CVAL_LOGLEVEL,
    EUCANETD_CVAL_LOGROLLNUMBER,
    EUCANETD_CVAL_LOGMAXSIZE,
    EUCANETD_CVAL_MIDO_INTRTCIDR,
    EUCANETD_CVAL_MIDO_INTMDCIDR,
    EUCANETD_CVAL_MIDO_EXTMDCIDR,
    EUCANETD_CVAL_MIDO_MDCIDR,
    EUCANETD_CVAL_MIDO_MAX_RTID,
    EUCANETD_CVAL_MIDO_MAX_ENIID,
    EUCANETD_CVAL_MIDO_ENABLE_MIDOMD,
    EUCANETD_CVAL_MIDO_API_URIBASE,
    EUCANETD_CVAL_MIDO_MD_VETH_USE_NETNS,
    EUCANETD_CVAL_MIDO_MD_254_EGRESS,
    EUCANETD_CVAL_MIDO_MD_253_EGRESS,
    EUCANETD_CVAL_MIDO_VALIDATE_MIDOCONFIG,
    EUCANETD_CVAL_LOCALIP,
    EUCANETD_CVAL_LAST,
};

enum {
    FLUSH_NONE,
    FLUSH_ALL,
    FLUSH_DYNAMIC,
    FLUSH_MIDO_ALL,
    FLUSH_MIDO_DYNAMIC,
    FLUSH_MIDO_CHECKDUPS,
    FLUSH_MIDO_DUPS,
    FLUSH_MIDO_CHECKVPC,
    FLUSH_MIDO_CHECKUNCONNECTED,
    FLUSH_MIDO_UNCONNECTED,
    FLUSH_MIDO_VPC,
    FLUSH_MIDO_TZONE,
    FLUSH_MIDO_LISTVPC,
    FLUSH_MIDO_LISTGATEWAYS,
    FLUSH_MIDO_TEST,
};

//! Enumeration of the peer component type running alongside this eucanetd service
typedef enum eucanetd_peer_t {
    PEER_INVALID = 0,                  //!< This is an invalid peer, this is used to detect initialization failures (forget to initialize)
    PEER_CLC = 1,                      //!< This indicates we are currently working with a CLC component
    PEER_CC = 2,                       //!< This indicates we are currently working with a CC component
    PEER_NC = 3,                       //!< This indicates we are currently working with an NC component
    PEER_NONE = 4,                     //!< This indicates we are currently running on a non-euca host
    PEER_MAX = 5,                      //!< This is an invalid role use to detect initialization errors (couldn't set the role)
} eucanetd_peer;

enum eucanetd_debug_level_t {
    EUCANETD_DEBUG_NONE = 0,
    EUCANETD_DEBUG_TRACE = 1,
    EUCANETD_DEBUG_DEBUG = 2,
    EUCANETD_DEBUG_INFO = 3,
    EUCANETD_DEBUG_WARN = 4,
    EUCANETD_DEBUG_ERROR = 5,
    EUCANETD_DEBUG_FATAL = 6,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Structure defining the core EUCANETD configuration
typedef struct eucanetdConfig_t {
    ipt_handler *ipt;                  //!< Pointer to the IP Tables Handler
    ips_handler *ips;                  //!< Pointer to the IP Sets Handler
    ebt_handler *ebt;                  //!< Pointer to the EB Tables Handler

    char netMode[NETMODE_LEN];         //!< Network mode name string
    euca_netmode nmCode;               //!< Network mode integer code
    char *eucahome;                    //!< Pointer to the string containing the eucalyptus area home path
    char *eucauser;                    //!< Pointer to the string containing the eucalyptus system user name
    char cmdprefix[EUCA_MAX_PATH];
    char configFiles[NUM_EUCANETD_CONFIG][EUCA_MAX_PATH];
    u32 vmGatewayIP;
    u32 clcMetadataIP;
    char ncRouterIP[INET_ADDR_LEN];
    char metadataIP[INET_ADDR_LEN];

    char pubInterface[IF_NAME_LEN];    //!< The configured public interface device to use for networking (VNET_PUBINTERFACE)
    char privInterface[IF_NAME_LEN];   //!< The configured private interface device to use for networking (VNET_PRIVINTERFACE)
    char bridgeDev[IF_NAME_LEN];       //!< The configured bridge device to use for networking (VNET_BRIDGE)

    char dhcpDaemon[EUCA_MAX_PATH];    //!< The path to the ISC DHCP server executable to use. (VNET_DHCPDAEMON)
    
    char systemctl[EUCA_MAX_PATH];     //!< systemctl executable to use. (VNET_SYSTEMCTL)

    char mido_intrtcidr[NETWORK_ADDR_LEN];
    char mido_intmdcidr[NETWORK_ADDR_LEN];
    char mido_extmdcidr[NETWORK_ADDR_LEN];
    char mido_mdcidr[NETWORK_ADDR_LEN];
    char mido_md_254_egress[256];
    char mido_md_253_egress[256];
    char mido_api_uribase[URI_LEN];
    int mido_max_rtid;
    int mido_max_eniid;

    atomic_file global_network_info_file;
    char lastAppliedVersion[32];

    // these are flags that can be set by values in eucalyptus.conf
    int polling_frequency;
    int disable_l2_isolation;
    int nc_router_ip;
    int nc_router;
    int metadata_use_vm_private;
    int metadata_ip;

    in_addr_t localIp;                 //!< Local address to use for this system
    u32 *my_ips;
    int max_my_ips;

    int eucanetd_err;
    boolean eucanetd_first_update;
    
    boolean nc_proxy;                //!< Set to TRUE to indicate we're using the NC proxy feature
    boolean use_systemctl;
    boolean enable_mido_md;
    boolean mido_md_veth_use_netns;
    boolean mido_md_config_changed;
    boolean mido_md_egress_rules_changed;
    boolean populate_mido_md;
    boolean validate_mido_config;

    int debug;
    int flushmode;
    char *flushmodearg;
    boolean multieucanetd_safe;
    int udpsock;
    boolean init;
    u32 euca_version;
    char *euca_version_str;
} eucanetdConfig;

//! Network Driver API
typedef struct driver_handler_t {
    char name[CHAR_BUFFER_SIZE];                                                         //!< The name of the given network driver (e.g. EDGE, VPCMIDO, etc.)
    int (*init) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);                      //!< The driver initialization interface
    int (*upgrade) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);                   //!< This is optional when upgrade tasks are required.
    int (*cleanup) (eucanetdConfig *pConfig, globalNetworkInfo *pGni, boolean doFlush);  //!< The driver cleanup interface
    int (*system_flush) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);              //!< Responsible for the flushing of all euca networking artifacts
    int (*system_maint) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);              //!< Maintenance actions when eucanetd is idle (e.g., no GNI changes)
    u32 (*system_scrub) (eucanetdConfig *pConfig, globalNetworkInfo *pGni,
            globalNetworkInfo *pGniApplied);                                             //!< Works on detecting what is changing
    int (*implement_network) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);         //!< Takes care of network devices, tunnels, etc.
    int (*implement_sg) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);              //!< Takes care of security group implementations and membership
    int (*implement_addressing) (eucanetdConfig *pConfig, globalNetworkInfo *pGni);      //!< Takes care of IP addressing, Elastic IPs, etc.
    int (*handle_signal) (eucanetdConfig *pConfig, globalNetworkInfo *pGni, int signal); //!< Forward signals (USR1 and USR2) to driver
} driver_handler;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Configuration Keys from eucalyptus.conf
extern configEntry configKeysRestartEUCANETD[];
extern configEntry configKeysNoRestartEUCANETD[];
//! @}

//! @{
//! @name Network Driver Interfaces (NDIs)
extern struct driver_handler_t edgeDriverHandler;           //!< EDGE network driver callback instance
extern struct driver_handler_t midoVpcDriverHandler;        //!< MIDONET VPC network driver callback instance
//! @}

//! Global Network Information structure pointer.
//extern globalNetworkInfo *globalnetworkinfo;

//! Role of the component running alongside this eucanetd service
extern eucanetd_peer eucanetdPeer;

//! Array of peer type strings
extern const char *asPeerRoleName[];

extern int sig_rcvd;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/
int eucanetd_dummy_udpsock(void);
int eucanetd_dummy_udpsock_close(void);
void eucanetd_emulate_sigusr2(void);

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

//! Macro to check whether or not a given peer value is valid
#define PEER_IS_VALID(_peer)             (((_peer) > PEER_INVALID) && ((_peer) < PEER_MAX))

//! Macro to determine if we are on a CLC
#define PEER_IS_CLC(_peer)               ((_peer) == PEER_CLC)

//! Macro to determine if we are on a CC
#define PEER_IS_CC(_peer)                ((_peer) == PEER_CC)

//! Macro to determine if we are on a NC
#define PEER_IS_NC(_peer)                ((_peer) == PEER_NC)

//! Macro to determine if we are not either NC, CC, and CLC
#define PEER_IS_NONE(_peer)                ((_peer) == PEER_NONE)

//! Macro to convert a peer enumeration to a string representation
#define PEER2STR(_peer)                  ((((unsigned)(_peer)) > PEER_MAX) ? asPeerRoleName[PEER_MAX] : asPeerRoleName[(_peer)])

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCANETD_H_ */
