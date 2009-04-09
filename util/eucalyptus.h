#ifndef INCLUDE_EUCALYPTUS_H
#define INCLUDE_EUCALYPTUS_H

/* environment variable set at startup */
#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

/* file paths relative to $EUCALYPTUS */
#define EUCALYPTUS_CONF_LOCATION   "%s/etc/eucalyptus/eucalyptus.conf"
#define EUCALYPTUS_ADD_KEY         "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/add_key.pl %s/usr/share/eucalyptus/euca_mountwrap"
#define EUCALYPTUS_GEN_LIBVIRT_XML "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/gen_libvirt_xml"
#define EUCALYPTUS_GEN_KVM_LIBVIRT_XML "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/gen_kvm_libvirt_xml"
#define EUCALYPTUS_GET_XEN_INFO    "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_xen_info"
#define EUCALYPTUS_GET_KVM_INFO    "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_sys_info"
#define EUCALYPTUS_DISK_CONVERT    "%s/usr/share/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/partition2disk"
#define NC_NET_PATH_DEFAULT        "%s/var/run/eucalyptus/net"
#define CC_NET_PATH_DEFAULT        "%s/var/run/eucalyptus/net"

/* various defaults */
#define NC_NET_PORT_DEFAULT      1976
#define CC_NET_PORT_DEFAULT      1976

/* names of variables in the configuration file */
#define CONFIG_MAX_MEM   "MAX_MEM"
#define CONFIG_MAX_DISK  "MAX_DISK"
#define CONFIG_MAX_CORES "MAX_CORES"
#define TEMPLATE_PATH    "TEMPLATE_PATH"
#define REGISTERED_PATH  "REGISTERED_PATH"
#define INSTANCE_PATH    "INSTANCE_PATH"
#define CONFIG_VNET_PORT "VNET_PORT"
#define CONFIG_VNET_DHCPDAEMON "VNET_DHCPDAEMON"
#define CONFIG_VNET_PRIVINTERFACE "VNET_PRIVINTERFACE"
#define CONFIG_NC_SERVICE "NC_SERVICE"
#define CONFIG_NC_PORT "NC_PORT"
#define CONFIG_NODES "NODES"
#define CONFIG_HYPERVISOR "HYPERVISOR"
#define CONFIG_NC_CACHE_SIZE "NC_CACHE_SIZE"
#define CONFIG_NC_SWAP_SIZE "SWAP_SIZE"
#define CONFIG_SAVE_INSTANCES "MANUAL_INSTANCES_CLEANUP"

/* name of the administrative user within Eucalyptus */
#define EUCALYPTUS_ADMIN "eucalyptus"

/* system limit defaults */
#define MAXNODES 1024
#define MAXINSTANCES 2048
#define MAXLOGFILESIZE 32768000
#define EUCA_MAX_GROUPS 64
#define EUCA_MAX_VOLUMES 256
#define DEFAULT_NC_CACHE_SIZE 99999 /* in MB */
#define DEFAULT_SWAP_SIZE 512 /* in MB */

#define MEGABYTE 1048576
#define OK 0
#define ERROR 1
#define ERROR_FATAL 1
#define ERROR_RETRY -1

typedef enum instance_states_t {
    /* the first 7 should match libvirt */
    NO_STATE = 0, 
    RUNNING,
    BLOCKED,
    PAUSED,
    SHUTDOWN,
    SHUTOFF,
    CRASHED,

    /* TODO: remove some of these? */
    NEW, 
    WAITING,
    BOOTING,
    TERMINATING,
    AWOL,

    /* the only three states reported to CLC */
    PENDING,  /* staging in data, starting to boot, failed to boot */ 
    EXTANT,   /* guest OS booting, running, shutting down, cleaning up state */
    TEARDOWN, /* a marker for a terminated domain, one not taking up resources */

    TOTAL_STATES
} instance_states;

static char * instance_state_names[] = {
    "Unknown",
    "Running",
    "Running", /* TODO: we need to figure out what to call this. idle? */
    "Paused",
    "Shutdown",
    "Shutoff",
    "Crashed",

    "New",
    "Waiting",
    "Booting",
    "Terminating",
    "AWOL",

    "Pending",
    "Extant",
    "Teardown"
};

#endif

