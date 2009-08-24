#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>

#include "vnetwork.h"
#include "data.h"

#define LIBVIRT_QUERY_RETRIES 5
#define MAXDOMS 1024
#define BYTES_PER_DISK_UNIT 1048576 /* disk stats are in Gigs */
#define SWAP_SIZE 512 /* for now, the only possible swap size, in MBs */

/* NC state */
struct nc_state_t {
	struct handlers *H;             // selected handler
	struct handlers *D;             // default  handler
	vnetConfig *vnetconfig;		// network config
	// globals
	int  config_network_port;
	char admin_user_id[CHAR_BUFFER_SIZE];
	int save_instance_files;
	char uri[CHAR_BUFFER_SIZE];
	virConnectPtr conn;
	int convert_to_disk;
	// defined max
	long long config_max_disk;
	long long config_max_mem;
	int config_max_cores;
	// current max
	long long disk_max;
	long long mem_max;
	int cores_max;
	// paths
	char home[CHAR_BUFFER_SIZE];
	char config_network_path [CHAR_BUFFER_SIZE];
	char gen_libvirt_cmd_path[CHAR_BUFFER_SIZE];
	char get_info_cmd_path[CHAR_BUFFER_SIZE];
	char rootwrap_cmd_path[CHAR_BUFFER_SIZE];
	char virsh_cmd_path[CHAR_BUFFER_SIZE];
	char xm_cmd_path[CHAR_BUFFER_SIZE];
	char detach_cmd_path[CHAR_BUFFER_SIZE];
};


struct handlers {
    char name [CHAR_BUFFER_SIZE];
    int (*doInitialize)		(struct nc_state_t *nc);
    int (*doPowerDown)		(struct nc_state_t *nc,
		    		ncMetadata *meta);
    int (*doDescribeInstances)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char **instIds,
				int instIdsLen,
				ncInstance ***outInsts,
				int *outInstsLen);
    int (*doRunInstance)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char *reservationId,
				ncInstParams *params,
				char *imageId,
				char *imageURL,
				char *kernelId,
				char *kernelURL,
				char *ramdiskId,
				char *ramdiskURL,
				char *keyName,
				char *privMac,
				char *pubMac,
				int vlan,
				char *userData,
				char *launchIndex,
				char **groupNames,
				int groupNamesSize,
				ncInstance **outInst);
    int (*doTerminateInstance)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				int *shutdownState,
				int *previousState);
    int (*doRebootInstance)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId);
    int (*doGetConsoleOutput)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char **consoleOutput);
    int (*doDescribeResource)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *resourceType,
			       	ncResource **outRes);
    int (*doStartNetwork)	(struct nc_state_t *nc,
				ncMetadata *ccMeta,
				char **remoteHosts,
				int remoteHostsLen,
				int port,
				int vlan);
    int (*doAttachVolume)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char *volumeId,
				char *remoteDev,
				char *localDev);
    int (*doDetachVolume)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char *volumeId,
				char *remoteDev,
				char *localDev,
				int force);
};

#ifdef HANDLERS_FANOUT // only declare for the fanout code, not the actual handlers
int doPowerDown			(ncMetadata *meta);
int doDescribeInstances		(ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen);
int doRunInstance		(ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInst);
int doTerminateInstance		(ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState);
int doRebootInstance		(ncMetadata *meta, char *instanceId);
int doGetConsoleOutput		(ncMetadata *meta, char *instanceId, char **consoleOutput);
int doDescribeResource		(ncMetadata *meta, char *resourceType, ncResource **outRes);
int doStartNetwork		(ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan);
int doAttachVolume		(ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev);
int doDetachVolume		(ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force);
#endif /* HANDLERS_FANOUT */


/* helper functions used by the low level handlers */
int get_value(			char *s,
				const char *name,
				long long *valp);
int convert_dev_names(		char *localDev,
				char *localDevReal,
				char *localDevTag);
void libvirt_error_handler(	void * userData,
				virErrorPtr error);
void print_running_domains(	void);
virConnectPtr *check_hypervisor_conn();
void change_state(		ncInstance * instance,
				instance_states state);
void adopt_instances();
int get_instance_xml(		const char *gen_libvirt_cmd_path,
				char *userId,
				char *instanceId,
				int ramdisk,
				char *disk_path,
				ncInstParams *params,
				char *privMac,
				char *pubMac,
				char *brname,
				char **xml);
void * monitoring_thread(	void *arg);
void * startup_thread(		void *arg);

#endif /* INCLUDE */

