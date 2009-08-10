#include <stdio.h>
#include <stdlib.h>
#include <string.h> /* strlen, strcpy */
#include <time.h>
#include <limits.h> /* INT_MAX */
#include <sys/types.h> /* fork */
#include <sys/wait.h> /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <errno.h>
#include <sys/stat.h>
#include <pthread.h>
#include <sys/vfs.h> /* statfs */
#include <signal.h> /* SIGINT */

#include "eucalyptus-config.h"
#include "ipc.h"
#include "misc.h"
#define HANDLERS_FANOUT
#include <handlers.h>
#include <storage.h>
#include <eucalyptus.h>

// declarations of available handlers
extern struct handlers xen_libvirt_handlers;
extern struct handlers kvm_libvirt_handlers;

// a NULL-terminated array of available handlers
static struct handlers * available_handlers [] = {
    &xen_libvirt_handlers,
    &kvm_libvirt_handlers,
    NULL
};

// the chosen handlers
static struct handlers * H = NULL;

static int init (void)
{
    static int initialized = 0;
    if (initialized>0) { /* 0 => hasn't run, -1 => failed, 1 => ok */
        return 0;
    } else if (initialized<0) {
        return 1;
    }

    /* read in configuration - this should be first! */
    int do_warn = 0;
    char * home;
    home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
        do_warn = 1;
    } else {
        home = strdup (home);
    }
 
    char config [CHAR_BUFFER_SIZE];
    snprintf(config, CHAR_BUFFER_SIZE, "%s/var/log/eucalyptus/nc.log", home);
    logfile(config, EUCADEBUG); // TODO: right level?
    if (do_warn) 
        logprintfl (EUCAWARN, "env variable %s not set, using /\n", EUCALYPTUS_ENV_VAR_NAME);

    /* from now on we have unrecoverable failure, so no point in retrying
     * to re-init */
    initialized = -1;

    struct stat mystat;
    snprintf(config, CHAR_BUFFER_SIZE, EUCALYPTUS_CONF_LOCATION, home);
    if (stat(config, &mystat)!=0) {
        logprintfl (EUCAFATAL, "could not open configuration file %s\n", config);
        return 1;
    }
    logprintfl (EUCAINFO, "NC is looking for configuration in %s\n", config);
        
    /* determine the hypervisor to use */
    char * hypervisor;
    if (get_conf_var(config, CONFIG_HYPERVISOR, &hypervisor)<1) {
        logprintfl (EUCAFATAL, "value %s is not set in the config file\n", CONFIG_HYPERVISOR);
        return 1;
    }
    struct handlers ** h; 
    for (h = available_handlers; *h; h++ ) {
        if (! strncmp ((*h)->name, hypervisor, CHAR_BUFFER_SIZE) ) { 
            // the name matches!
            H = * h; 
            break;
        }
    }
    if (H==NULL) {
        logprintfl (EUCAFATAL, "requested hypervisor type (%s) is not available\n", hypervisor);
        free (hypervisor);
        return 1;
    }
    free (hypervisor);

    if (H->doInitialize)
        if (H->doInitialize())
            return 1;
    
    initialized = 1;
    return 0;
}

int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    int ret, len;
    char *home, *file_name, *net_mode;
    FILE *f;
#define NC_MONIT_FILENAME "/var/run/eucalyptus/nc-stat"

    if (init()) return 1;
    ret=H->doDescribeInstances (meta, instIds, instIdsLen, outInsts, outInstsLen);

    home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) 
        home = strdup (""); 
    else 
        home = strdup (home);

    if (!home) {
	logprintfl(EUCAERROR, "Out of memory!\n");
	return ret;
    }

    /* allocate enough memory */
    len = (strlen(EUCALYPTUS_CONF_LOCATION) > strlen(NC_MONIT_FILENAME)) ? strlen(EUCALYPTUS_CONF_LOCATION) : strlen(NC_MONIT_FILENAME);
    len += 2 + strlen(home);
    file_name = malloc(sizeof(char) * len);
    if (!file_name) {
	free(home);
	logprintfl(EUCAERROR, "Out of memory!\n");
	return ret;
    }

    if (ret == 0) {
    	sprintf(file_name, EUCALYPTUS_CONF_LOCATION, home);
	net_mode = getConfString(file_name, "VNET_MODE");
	if (!net_mode) {
		free(home);
		logprintfl(EUCAERROR, "Failed to get VNET_MODE!\n");
		return ret;
	}
    
	sprintf(file_name, "%s/%s", home, NC_MONIT_FILENAME);
	if (!strcmp(meta->userId, EUCALYPTUS_ADMIN)) {
		f = fopen(file_name, "w");
		if (f) {
			int i;
			ncInstance * instance;
			char myName[256];

			fprintf(f, "version: %s\n", EUCA_VERSION);
			fprintf(f, "timestamp: %ld\n", time(NULL));
			if (gethostname(myName, 256) == 0) {
				fprintf(f, "node: %s\n", myName);
			}
			fprintf(f, "hypervisor: %s\n", H->name);
			fprintf(f, "network: %s\n", net_mode);

			for (i=0; i < (*outInstsLen); i++) {
				instance = (* outInsts)[i];
				fprintf(f, "id: %s", instance->instanceId);
				fprintf(f, " userId: %s", instance->userId);
				fprintf(f, " state: %s", instance->stateName);
				fprintf(f, " mem: %d", instance->params.memorySize);
				fprintf(f, " disk: %d", instance->params.diskSize);
				fprintf(f, " cores: %d\n", instance->params.numberOfCores);
			}
			fclose(f);
		} else {
			/* file problem */
			logprintfl(EUCAWARN, "Cannot write to nc-stat!\n");
		}
	}
	free(net_mode);
    }
    free(home);
    free(file_name);

    return ret;
}

int doPowerDown(ncMetadata *meta) {
    if (init()) return 1;
    return H->doPowerDown (meta);
}

int doRunInstance (ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInst)
{
    if (init()) return 1;
    return H->doRunInstance (meta, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, privMac, pubMac, vlan, userData, launchIndex, groupNames, groupNamesSize, outInst);
}

int doTerminateInstance (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState)
{
    if (init()) return 1;
    return H->doTerminateInstance (meta, instanceId, shutdownState, previousState);
}

int doRebootInstance (ncMetadata *meta, char *instanceId) 
{
    if (init()) return 1;
    return H->doRebootInstance (meta, instanceId);
}

int doGetConsoleOutput (ncMetadata *meta, char *instanceId, char **consoleOutput) 
{
    if (init()) return 1;
    return H->doGetConsoleOutput (meta, instanceId, consoleOutput);
}

int doDescribeResource (ncMetadata *meta, char *resourceType, ncResource **outRes)
{
    if (init()) return 1;
    return H->doDescribeResource (meta, resourceType, outRes);
}

int doStartNetwork (ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan)
{
    if (init()) return 1;
    return H->doStartNetwork (ccMeta, remoteHosts, remoteHostsLen, port, vlan);
}

int doAttachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    if (init()) return 1;
    return H->doAttachVolume (meta, instanceId, volumeId, remoteDev, localDev);
}

int doDetachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    if (init()) return 1;
    return H->doDetachVolume (meta, instanceId, volumeId, remoteDev, localDev, force);
}
