// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file node/handlers_kvm.c
//! This implements the operations handlers supported by the KVM hypervisor.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU                      /* strnlen */
#include <string.h>                    /* strlen, strcpy */
#include <time.h>
#include <sys/types.h>                 /* fork */
#include <sys/wait.h>                  /* waitpid */
#include <unistd.h>
#include <assert.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>                    /* SIGINT */
#include <sys/stat.h>
#include <fcntl.h>

#include <eucalyptus.h>
#include <ipc.h>
#include <misc.h>
#include <euca_auth.h>
#include <euca_network.h>
#include <backing.h>
#include <diskutil.h>
#include <sensor.h>
#include <euca_string.h>
#include <ebs_utils.h>
#include "handlers.h"
#include "xml.h"
#include "hooks.h"
#include "vbr.h"                       // vbr_parse

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define HYPERVISOR_URI                        "qemu:///system" /**< Defines the Hypervisor URI to use with KVM */

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/
typedef struct rebooting_thread_params_t {
    char instanceId[CHAR_BUFFER_SIZE];
    struct nc_state_t nc;
} rebooting_thread_params;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

// coming from handlers.c
extern sem *inst_sem;
extern sem *hyp_sem;
extern bunchOfInstances *global_instances;
extern int outgoing_migrations_in_progress;
extern int incoming_migrations_in_progress;

// Implemented in handlers_default.c
extern int create_vol_xml(const char *instanceId, const char *volumeId, const char *xml_in, char **xml_out);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int doInitialize(struct nc_state_t *nc);
static void *rebooting_thread(void *arg);
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId);
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput);
static int doMigrateInstances(struct nc_state_t *nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials, char ** resourceLocations, int resourceLocationsLen);
static int generate_migration_keys(char *host, char *credentials, boolean restart, ncInstance * instance);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! KVM LIBVIRT operation handlers
struct handlers kvm_libvirt_handlers = {
    .name = "kvm",
    .doInitialize = doInitialize,
    .doDescribeInstances = NULL,
    .doRunInstance = NULL,
    .doTerminateInstance = NULL,
    .doRebootInstance = doRebootInstance,
    .doGetConsoleOutput = doGetConsoleOutput,
    .doDescribeResource = NULL,
    .doStartNetwork = NULL,
    .doAssignAddress = NULL,
    .doPowerDown = NULL,
    .doAttachVolume = NULL,
    .doDetachVolume = NULL,
    .doAttachNetworkInterface = NULL,
    .doDetachNetworkInterface = NULL,
    .doDescribeSensors = NULL,
    .doModifyNode = NULL,
    .doMigrateInstances = doMigrateInstances,
    .doStartInstance = NULL,
    .doStopInstance = NULL
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Generate migration keys on source and destination host.
//!
//! @param[in] host hostname (IP address) for key
//! @param[in] credentials shared secret for key
//! @param[in] restart TRUE if libvirtd should be restarted after key generation
//! @param[in] instance pointer to instance struct for logging information (optional--can be NULL)
//!
//! @return EUCA_OK, EUCA_INVALID_ERROR, or EUCA_SYSTEM_ERROR
//!

static int generate_migration_keys(char *host, char *credentials, boolean restart, ncInstance * instance)
{
    int rc = EUCA_OK;
    char euca_rootwrap[EUCA_MAX_PATH] = "";
    char generate_keys[EUCA_MAX_PATH] = "";
    char *euca_base = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char *instanceId = instance ? instance->instanceId : "UNSET";
    static char *most_recent_credentials = NULL;
    static char *most_recent_host = NULL;

    if (!host || !credentials) {
        LOGERROR("[%s] called with invalid arguments for host and/or credentials: host=%s, creds=%s\n", SP(instanceId), SP(host), (credentials == NULL) ? "UNSET" : "present");
        return (EUCA_INVALID_ERROR);
    }

    sem_p(hyp_sem);

    if (most_recent_credentials && most_recent_host && !strcmp(most_recent_credentials, credentials) && !strcmp(most_recent_host, host)) {
        LOGDEBUG("[%s] request to generate key using same information (host='%s', creds=%s) as previous request, skipping\n", instanceId, host,
                 (credentials == NULL) ? "UNSET" : "present");
        sem_v(hyp_sem);
        return (EUCA_OK);
    }

    if (!most_recent_credentials) {
        most_recent_credentials = strdup(credentials);
        LOGDEBUG("[%s] first generation of migration credentials\n", instanceId);
    }

    if (!most_recent_host) {
        most_recent_host = strdup(host);
        LOGDEBUG("[%s] first generation of migration host information: %s\n", instanceId, most_recent_host);
    }
    // So, something has changed.
    if (strcmp(most_recent_credentials, credentials)) {
        EUCA_FREE(most_recent_credentials);
        most_recent_credentials = strdup(credentials);
        LOGDEBUG("[%s] regeneration of migration credentials\n", instanceId);
    }

    if (strcmp(most_recent_host, host)) {
        EUCA_FREE(most_recent_host);
        most_recent_host = strdup(host);
        LOGDEBUG("[%s] regeneration of migration host information: %s\n", instanceId, most_recent_host);
    }
    // TO-DO: Add polling around incoming_migrations_in_progress to prevent restarts during migrations?
    snprintf(generate_keys, EUCA_MAX_PATH, EUCALYPTUS_GENERATE_MIGRATION_KEYS, ((euca_base != NULL) ? euca_base : ""));
    snprintf(euca_rootwrap, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, ((euca_base != NULL) ? euca_base : ""));

    LOGDEBUG("[%s] executing migration key-generator: '%s %s %s %s %s'\n", instanceId, euca_rootwrap, generate_keys, host, credentials, ((restart == TRUE) ? "restart" : ""));
    rc = euca_execlp(NULL, euca_rootwrap, generate_keys, host, credentials, ((restart == TRUE) ? "restart" : ""), NULL);

    sem_v(hyp_sem);

    if (rc) {
        LOGERROR("[%s] cmd '%s %s %s %s %s' failed %d\n", instanceId, euca_rootwrap, generate_keys, host, credentials, ((restart == TRUE) ? "restart" : ""), rc);
        return (EUCA_SYSTEM_ERROR);
    }

    LOGDEBUG("[%s] migration key generation succeeded\n", instanceId);
    return (EUCA_OK);
}

//!
//! Initialize the NC state structure for the KVM hypervisor.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//!
//! @return Always return EUCA_OK
//!
static int doInitialize(struct nc_state_t *nc)
{
#define GET_VALUE(_name, _var)                                                           \
{                                                                                        \
	if (get_value (s, (_name), &(_var))) {                                               \
		LOGFATAL("did not find %s in output from %s\n", (_name), nc->get_info_cmd_path); \
		EUCA_FREE(s);                                                                    \
		return (EUCA_FATAL_ERROR);                                                       \
	}                                                                                    \
}

    char *s = NULL;

    // set up paths of Eucalyptus commands NC relies on
    snprintf(nc->get_info_cmd_path, EUCA_MAX_PATH, EUCALYPTUS_GET_KVM_INFO, nc->home, nc->home);
    strcpy(nc->uri, HYPERVISOR_URI);
    nc->convert_to_disk = 1;
    nc->capability = HYPERVISOR_HARDWARE;   //! @todo indicate virtio support?

    s = system_output(nc->get_info_cmd_path);

    GET_VALUE("nr_cores", nc->phy_max_cores);
    GET_VALUE("total_memory", nc->phy_max_mem);
    EUCA_FREE(s);

    // we leave 256M to the host
    nc->mem_max -= 256;
    return (EUCA_OK);
}

//!
//! Defines the thread that does the actual reboot of an instance.
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
static void *rebooting_thread(void *arg)
{
    char *xml = NULL;
    char resourceName[1][MAX_SENSOR_NAME_LEN] = { "" };
    char resourceAlias[1][MAX_SENSOR_NAME_LEN] = { "" };
    ncInstance *instance = NULL;
    struct nc_state_t *nc = NULL;
    virDomainPtr dom = NULL;
    virConnectPtr conn = NULL;
    rebooting_thread_params *params = ((rebooting_thread_params *) arg);
    nc = &(params->nc);
    boolean shutoff = FALSE;

    sem_p(inst_sem);
    {
        instance = find_instance(&global_instances, params->instanceId);
    }
    sem_v(inst_sem);

    if (instance == NULL) {
        LOGERROR("[%s] cannot find instance\n", params->instanceId);
        EUCA_FREE(params)
        return NULL;
    }

    LOGDEBUG("[%s] spawning rebooting thread\n", instance->instanceId);

    if ((conn = lock_hypervisor_conn()) == NULL) {
        LOGERROR("[%s] cannot connect to hypervisor to restart instance, giving up\n", instance->instanceId);
        EUCA_FREE(params);
        return NULL;
    }
    { // hypervisor lock
        dom = virDomainLookupByName(conn, instance->instanceId);
        if (dom == NULL) {
            LOGERROR("[%s] cannot locate instance to reboot, giving up\n", instance->instanceId);
            unlock_hypervisor_conn();
            EUCA_FREE(params);
            return NULL;
        }
        // obtain the most up-to-date XML for domain from libvirt
        xml = virDomainGetXMLDesc(dom, 0);
        if (xml == NULL) {
            LOGERROR("[%s] cannot obtain metadata for instance to reboot, giving up\n", instance->instanceId);
            virDomainFree(dom);            // release libvirt resource
            unlock_hypervisor_conn();
            EUCA_FREE(params);
            return NULL;
        }
        virDomainFree(dom);                // release libvirt resource
    } // end hypervisor lock
    unlock_hypervisor_conn();

    LOGINFO("[%s] shutting down\n", instance->instanceId);
    // try shutdown first, then kill it if uncooperative
    if (shutdown_then_destroy_domain(instance->instanceId, TRUE) != EUCA_OK) {
        LOGERROR("[%s] failed to shutdown and destroy the instance to reboot, giving up\n", instance->instanceId);
        EUCA_FREE(params);
        return NULL;
    }
    // Add a shift to values of three of the metrics: ones that
    // drop back to zero after a reboot. The shift, which is based
    // on the latest value, ensures that values sent upstream do
    // not go backwards .
    sensor_shift_metric(instance->instanceId, "CPUUtilization");
    sensor_shift_metric(instance->instanceId, "NetworkIn");
    sensor_shift_metric(instance->instanceId, "NetworkOut");

    if ((conn = lock_hypervisor_conn()) == NULL) {
        LOGERROR("[%s] cannot connect to hypervisor to restart instance, giving up\n", instance->instanceId);
        EUCA_FREE(params);
        return NULL;
    }
    { // hypervisor lock
        // domain is now shut down, create a new one with the same XML
        LOGINFO("[%s] rebooting\n", instance->instanceId);
        if (!strcmp(nc->pEucaNet->sMode, NETMODE_VPCMIDO)) {
            // need to sleep to allow midolman to update the VM interface
            sleep(10);
        }
        dom = virDomainCreateLinux(conn, xml, 0);
        if (dom == NULL) {
            LOGERROR("[%s] failed to restart instance\n", instance->instanceId);
            shutoff = TRUE;
        } else {
            euca_strncpy(resourceName[0], instance->instanceId, MAX_SENSOR_NAME_LEN);
            sensor_refresh_resources(resourceName, resourceAlias, 1);   // refresh stats so we set base value accurately
            virDomainFree(dom);
        }
    } // end hypervisor lock
    unlock_hypervisor_conn();

    if (shutoff == TRUE) {
        sem_p(inst_sem);
        {
            change_state(instance, SHUTOFF);
        }
        sem_v(inst_sem);
    } else {
        if (!strcmp(nc->pEucaNet->sMode, NETMODE_VPCMIDO)) {
            bridge_instance_interfaces_remove(nc, instance);
        }

        // Fix for EUCA-12608
        if (!strcmp(nc->pEucaNet->sMode, NETMODE_EDGE)) {
            char iface[16];
            snprintf(iface, 16, "vn_%s", instance->instanceId);
            bridge_interface_set_hairpin(nc, instance, iface);
        } 
        sem_p(inst_sem);
        {
            instance->rebootTime = 0; // clear reboot time when running
        }
        sem_v(inst_sem);
    }
    EUCA_FREE(xml);

    unset_corrid(get_corrid());
    EUCA_FREE(params);
    return NULL;
}

//!
//! Handles the reboot request of an instance.
//!
//! @param[in] nc a pointer to the NC state structure to initialize
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include:
//!         EUCA_ERROR, EUCA_NOT_FOUND_ERROR, and EUCA_FATAL_ERROR.
//!
static int doRebootInstance(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId)
{
    pthread_t tcb = { 0 };
    ncInstance *instance = NULL;
    rebooting_thread_params *params = NULL;
    int old_state = 0;
    int old_bootTime = 0;
    sem_p(inst_sem);
    {
        instance = find_instance(&global_instances, instanceId);
        if (instance != NULL) {
            old_state = instance->state;
            if (old_state != BOOTING) {
                old_bootTime = instance->bootTime;
                instance->bootTime = time(NULL);    // otherwise nc_state.booting_cleanup_threshold will kick in
                instance->rebootTime = instance->bootTime;  // reboot time will not be set on initial boot
                change_state(instance, BOOTING);    // not STAGING, since in that mode we don't poll hypervisor for info
                LOGDEBUG("[%s] is set to BOOTING stage\n", instanceId);
            }
        }
    }
    sem_v(inst_sem);

    if (instance == NULL) {
        LOGERROR("[%s] cannot find instance\n", instanceId);
        return (EUCA_NOT_FOUND_ERROR);
    }

    if ((old_state == BOOTING) && (instance->rebootTime > 0)) {
        LOGINFO("[%s] reboot in progress, ignoring reboot request.\n", instanceId);
        return (EUCA_OK);
    }

    params = EUCA_ZALLOC(1, sizeof(rebooting_thread_params));
    memcpy(&(params->instanceId), instanceId, CHAR_BUFFER_SIZE);
    memcpy(&(params->nc), nc, sizeof(struct nc_state_t));
    // since shutdown/restart may take a while, we do them in a thread
    if (pthread_create(&tcb, NULL, rebooting_thread, params)) {
        LOGERROR("[%s] failed to spawn a reboot thread\n", instanceId);
        sem_p(inst_sem);
        {
            instance = find_instance(&global_instances, instanceId);
            // if instance state is still BOOTING set it back to the old one
            if (instance != NULL && instance->state == BOOTING) {
                instance->bootTime = old_bootTime;
                change_state(instance, old_state);
            }
        }
        sem_v(inst_sem);
        return (EUCA_FATAL_ERROR);
    }
    set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tcb);
    if (pthread_detach(tcb)) {
        LOGERROR("[%s] failed to detach the rebooting thread\n", instanceId);
        return (EUCA_FATAL_ERROR);
    }

    return (EUCA_OK);
}

//!
//! Handles the console output retrieval request.
//!
//! @param[in]  nc a pointer to the NC state structure to initialize
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[out] consoleOutput a pointer to the unallocated string that will contain the output
//!
//! @return EUCA_OK on success or EUCA_ERROR and EUCA_NOT_FOUND_ERROR on failure.
//!
static int doGetConsoleOutput(struct nc_state_t *nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    int rc = 0;
    int fd = 0;
    int ret = EUCA_OK;
    int readsize = 0;
    char *console_output = NULL;
    char *console_append = NULL;
    char *console_main = NULL;
    char console_file[EUCA_MAX_PATH] = "";
    char userId[48] = "";
    ncInstance *instance = NULL;
    struct stat statbuf = { 0 };

    *consoleOutput = NULL;
    readsize = 64 * 1024;

    // find the instance record
    sem_p(inst_sem);
    {
        if ((instance = find_instance(&global_instances, instanceId)) != NULL) {
            snprintf(console_file, 1024, "%s/console.append.log", instance->instancePath);
            snprintf(userId, 48, "%s", instance->userId);
        }
    }
    sem_v(inst_sem);

    if (!instance) {
        LOGERROR("[%s] cannot locate instance\n", instanceId);
        return (EUCA_NOT_FOUND_ERROR);
    }
    // read from console.append.log if it exists into dynamically allocated 4K console_append buffer
    if ((rc = stat(console_file, &statbuf)) >= 0) {
        if (diskutil_ch(console_file, nc->admin_user_id, nc->admin_user_id, 0) != EUCA_OK) {
            LOGERROR("[%s] failed to change ownership of %s\n", instanceId, console_file);
            return (EUCA_ERROR);
        }

        if ((fd = open(console_file, O_RDONLY)) >= 0) {
            if ((console_append = EUCA_ZALLOC(4096, sizeof(char))) != NULL) {
                rc = read(fd, console_append, (4096) - 1);
            }
            close(fd);
        }
    }

    sem_p(inst_sem);
    {
        snprintf(console_file, EUCA_MAX_PATH, "%s/console.log", instance->instancePath);
    }
    sem_v(inst_sem);

    // read the last 64K from console.log or the whole file, if smaller, into dynamically allocated 64K console_main buffer
    if ((rc = stat(console_file, &statbuf)) >= 0) {
        if (diskutil_ch(console_file, nc->admin_user_id, nc->admin_user_id, 0) != EUCA_OK) {
            LOGERROR("[%s] failed to change ownership of %s\n", instanceId, console_file);
            EUCA_FREE(console_append);
            return (EUCA_ERROR);
        }

        if ((fd = open(console_file, O_RDONLY)) >= 0) {
            if ((rc = lseek(fd, (off_t) (-1 * readsize), SEEK_END)) < 0) {
                if ((rc = lseek(fd, (off_t) 0, SEEK_SET)) < 0) {
                    LOGERROR("[%s] cannot seek to beginning of file\n", instanceId);
                    if (console_append)
                        EUCA_FREE(console_append);
                    close(fd);
                    return (EUCA_ERROR);
                }
            }

            if ((console_main = EUCA_ZALLOC(readsize, sizeof(char))) != NULL) {
                rc = read(fd, console_main, (readsize) - 1);
            }
            close(fd);
        } else {
            LOGERROR("[%s] cannot open '%s' read-only\n", instanceId, console_file);
        }
    } else {
        LOGWARN("[%s] cannot stat console_output file '%s'\n", instanceId, console_file);
    }

    // concatenate console_append with console_main, base64-encode this, and put into dynamically allocated buffer consoleOutput
    ret = EUCA_ERROR;
    if ((console_output = EUCA_ZALLOC((readsize) + 4096, sizeof(char))) != NULL) {
        if (console_append) {
            strncat(console_output, console_append, 4096);
        }

        if (console_main) {
            strncat(console_output, console_main, readsize);
        }

        if (strlen(console_output) == 0) {
            LOGDEBUG("There is no console output. Setting it to ' '\n");
            snprintf(console_output, 2, " \n");
        }
        *consoleOutput = base64_enc((unsigned char *)console_output, strlen(console_output));
        ret = EUCA_OK;
    }

    EUCA_FREE(console_append);
    EUCA_FREE(console_main);
    EUCA_FREE(console_output);
    return (ret);
}

//!
//! Defines the thread that does the actual migration of an instance off the source.
//!
//! @param[in] arg a transparent pointer to the argument passed to this thread handler
//!
//! @return Always return NULL
//!
static void *migrating_thread(void *arg)
{
    ncInstance *instance = ((ncInstance *) arg);
    virDomainPtr dom = NULL;
    virConnectPtr conn = NULL;
    int migration_error = 0;

    LOGTRACE("invoked for %s\n", instance->instanceId);

    if ((conn = lock_hypervisor_conn()) == NULL) {
        LOGERROR("[%s] cannot migrate instance %s (failed to connect to hypervisor), giving up and rolling back.\n", instance->instanceId, instance->instanceId);
        migration_error++;
        goto out;
    } else {
        LOGTRACE("[%s] connected to hypervisor\n", instance->instanceId);
    }

    dom = virDomainLookupByName(conn, instance->instanceId);
    if (dom == NULL) {
        LOGERROR("[%s] cannot migrate instance %s (failed to find domain), giving up and rolling back.\n", instance->instanceId, instance->instanceId);
        migration_error++;
        goto out;
    }

    char duri[1024];
    snprintf(duri, sizeof(duri), "qemu+tls://%s/system", instance->migration_dst);

    virConnectPtr dconn = NULL;

    LOGDEBUG("[%s] connecting to remote hypervisor at '%s'\n", instance->instanceId, duri);
    dconn = virConnectOpen(duri);
    if (dconn == NULL) {
        LOGWARN("[%s] cannot migrate instance using TLS (failed to connect to remote), retrying using SSH.\n", instance->instanceId);
        snprintf(duri, sizeof(duri), "qemu+ssh://%s/system", instance->migration_dst);
        LOGDEBUG("[%s] connecting to remote hypervisor at '%s'\n", instance->instanceId, duri);
        dconn = virConnectOpen(duri);
        if (dconn == NULL) {
            LOGERROR("[%s] cannot migrate instance using TLS or SSH (failed to connect to remote), giving up and rolling back.\n", instance->instanceId);
            migration_error++;
            goto out;
        }
    }

    LOGINFO("[%s] migrating instance\n", instance->instanceId);
    virDomain *ddom = virDomainMigrate(dom,
                                       dconn,
                                       VIR_MIGRATE_LIVE | VIR_MIGRATE_NON_SHARED_DISK,
                                       NULL,    // new name on destination (optional)
                                       NULL,    // destination URI as seen from source (optional)
                                       0L); // bandwidth limitation (0 => unlimited)
    if (ddom == NULL) {
        LOGERROR("[%s] cannot migrate instance, giving up and rolling back.\n", instance->instanceId);
        migration_error++;
        goto out;
    } else {
        LOGINFO("[%s] instance migrated\n", instance->instanceId);
    }
    virDomainFree(ddom);
    virConnectClose(dconn);

out:
    if (dom)
        virDomainFree(dom);

    if (conn != NULL)
        unlock_hypervisor_conn();

    sem_p(inst_sem);
    LOGDEBUG("%d outgoing migrations still active\n", --outgoing_migrations_in_progress);
    if (migration_error) {
        migration_rollback(instance);
    } else {
        // If this is set to NOT_MIGRATING here, it's briefly possible for
        // both the source and destination nodes to report the same instance
        // as Extant/NOT_MIGRATING, which is confusing!
        instance->migration_state = MIGRATION_CLEANING;
        save_instance_struct(instance);
        copy_instances();
    }
    sem_v(inst_sem);

    LOGDEBUG("done\n");
    unset_corrid(get_corrid());
    return NULL;
}

//!
//! Updates VBR[] of the instance struct (which must be locked by
//! the caller) with new resource locations (URLs of images) if
//! such are present in the new
//!
static void update_resource_locations(virtualMachine *vm, char ** resourceLocations, int resourceLocationsLen)
{
    virtualBootRecord *vbr = NULL;
    char *id_loc = NULL;
    char *loc = NULL;

    for (int i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
        vbr = &(vm->virtualBootRecord[i]);

        //Skip invalid vbr or any entry without an id since the below match will not behave correctly.
        if(vbr == NULL || strlen(vbr->id) <= 0) {
            continue;
        }

        // see if ID in the VBR is among IDs associated with resourceLocations to be updated
        for (int j = 0; j < resourceLocationsLen; j++) {
            id_loc = resourceLocations[j];

            if ((strstr(id_loc, vbr->id) == id_loc) // id_loc begins with ID
                && (strlen(id_loc) > (strlen(vbr->id) + 1))) { // id_loc has more than ID and '='
                loc = id_loc + strlen(vbr->id) + 1; // URL starts after ID and '='
                euca_strncpy(vbr->resourceLocation, loc, sizeof(vbr->resourceLocation)); // update the URL of in-memory struct
            }
        }
    }
}

//!
//! Handles the instance migration request.
//!
//! @param[in]  nc a pointer to the node controller (NC) state
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instances metadata for the instance to migrate to destination
//! @param[in]  instancesLen number of instances in the instance list
//! @param[in]  action IP of the destination Node Controller
//! @param[in]  credentials credentials that enable the migration
//!
//! @return EUCA_OK on success or EUCA_*ERROR on failure
//!
//! @pre
//!
//! @post
static int doMigrateInstances(struct nc_state_t *nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials, char ** resourceLocations, int resourceLocationsLen)
{
    int ret = EUCA_OK;
    int credentials_prepared = 0;
    char *libvirt_xml_modified = NULL;

    if (instancesLen <= 0) {
        LOGERROR("called with invalid instancesLen (%d)\n", instancesLen);
        pMeta->replyString = strdup("internal error (invalid instancesLen)");
        return (EUCA_INVALID_ERROR);
    }

    LOGDEBUG("verifying %d instance[s] for migration...\n", instancesLen);
    for (int inst_idx = 0; inst_idx < instancesLen; inst_idx++) {
        LOGDEBUG("verifying instance # %d...\n", inst_idx);
        if (instances[inst_idx]) {
            ncInstance *instance_idx = instances[inst_idx];
            LOGDEBUG("[%s] proposed migration action '%s' (%s > %s) [creds=%s]\n", SP(instance_idx->instanceId), SP(action), SP(instance_idx->migration_src),
                     SP(instance_idx->migration_dst), (instance_idx->migration_credentials == NULL) ? "UNSET" : "present");
        } else {
            pMeta->replyString = strdup("internal error (instance count mismatch)");
            LOGERROR("Mismatch between migration instance count (%d) and length of instance list\n", instancesLen);
            return (EUCA_ERROR);
        }
    }

    // TO-DO: Optimize the location of this loop, placing it inside various conditionals below?
    for (int inst_idx = 0; inst_idx < instancesLen; inst_idx++) {
        ncInstance *instance_req = instances[inst_idx];
        char *sourceNodeName = instance_req->migration_src;
        char *destNodeName = instance_req->migration_dst;

        LOGDEBUG("[%s] processing instance # %d (%s > %s)\n", instance_req->instanceId, inst_idx, instance_req->migration_src, instance_req->migration_dst);

        // this is a call to the source of migration
        if (!strcmp(pMeta->nodeName, sourceNodeName)) {

            // locate the instance structure
            ncInstance *instance;
            sem_p(inst_sem);
            {
                instance = find_instance(&global_instances, instance_req->instanceId);
            }
            sem_v(inst_sem);
            if (instance == NULL) {
                LOGERROR("[%s] cannot find instance\n", instance_req->instanceId);
                pMeta->replyString = strdup("failed to locate instance to migrate");
                return (EUCA_NOT_FOUND_ERROR);
            }

            if (strcmp(action, "prepare") == 0) {
                sem_p(inst_sem);
                instance->migration_state = MIGRATION_PREPARING;
                euca_strncpy(instance->migration_src, sourceNodeName, HOSTNAME_SIZE);
                euca_strncpy(instance->migration_dst, destNodeName, HOSTNAME_SIZE);
                euca_strncpy(instance->migration_credentials, credentials, CREDENTIAL_SIZE);
                instance->migrationTime = time(NULL);
                update_resource_locations(&(instance->params), resourceLocations, resourceLocationsLen);
                save_instance_struct(instance);
                copy_instances();
                sem_v(inst_sem);

                // Establish migration-credential keys if this is the first instance preparation for this host.
                LOGINFO("[%s] migration source preparing %s > %s [creds=%s]\n", SP(instance->instanceId), SP(instance->migration_src), SP(instance->migration_dst),
                        (instance->migration_credentials == NULL) ? "UNSET" : "present");
                if (!credentials_prepared) {
                    if (generate_migration_keys(sourceNodeName, credentials, TRUE, instance) != EUCA_OK) {
                        pMeta->replyString = strdup("internal error (migration credentials generation failed)");
                        sem_p(inst_sem);
                        instance->migration_state = NOT_MIGRATING;
                        save_instance_struct(instance);
                        copy_instances();
                        sem_v(inst_sem);
                        return (EUCA_SYSTEM_ERROR);
                    } else {
                        credentials_prepared++;
                    }
                }
                sem_p(inst_sem);
                instance->migration_state = MIGRATION_READY;
                save_instance_struct(instance);
                copy_instances();
                sem_v(inst_sem);

            } else if (strcmp(action, "commit") == 0) {

                sem_p(inst_sem);
                if (instance->migration_state == MIGRATION_IN_PROGRESS) {
                    LOGWARN("[%s] duplicate request to migration source to initiate %s > %s (already migrating)\n", instance->instanceId,
                            instance->migration_src, instance->migration_dst);
                    sem_v(inst_sem);
                    return (EUCA_DUPLICATE_ERROR);
                } else if (instance->migration_state != MIGRATION_READY) {
                    LOGERROR("[%s] request to commit migration %s > %s when source migration_state='%s' (not 'ready')\n", instance->instanceId,
                             SP(sourceNodeName), SP(destNodeName), migration_state_names[instance->migration_state]);
                    sem_v(inst_sem);
                    return (EUCA_UNSUPPORTED_ERROR);
                }
                instance->migration_state = MIGRATION_IN_PROGRESS;
                outgoing_migrations_in_progress++;
                LOGINFO("[%s] migration source initiating %s > %s [creds=%s] (1 of %d active outgoing migrations)\n", instance->instanceId, instance->migration_src,
                        instance->migration_dst, (instance->migration_credentials == NULL) ? "UNSET" : "present", outgoing_migrations_in_progress);
                save_instance_struct(instance);
                copy_instances();
                sem_v(inst_sem);

                // since migration may take a while, we do them in a thread
                pthread_t tcb = { 0 };
                if (pthread_create(&tcb, NULL, migrating_thread, (void *)instance)) {
                    LOGERROR("[%s] failed to spawn a migration thread\n", instance->instanceId);
                    return (EUCA_THREAD_ERROR);
                }
                set_corrid_pthread(get_corrid() != NULL ? get_corrid()->correlation_id : NULL, tcb);
                if (pthread_detach(tcb)) {
                    LOGERROR("[%s] failed to detach the migration thread\n", instance->instanceId);
                    return (EUCA_THREAD_ERROR);
                }
            } else if (strcmp(action, "rollback") == 0) {
                if ((instance->migration_state == MIGRATION_READY) || (instance->migration_state == MIGRATION_PREPARING)) {
                    LOGINFO("[%s] rolling back migration (%s > %s) on source\n", instance->instanceId, instance->migration_src, instance->migration_dst);
                    sem_p(inst_sem);
                    migration_rollback(instance);
                    sem_v(inst_sem);
                } else {
                    LOGINFO("[%s] ignoring request to roll back migration on source with instance in state %s(%s) -- duplicate rollback request?\n", instance->instanceId,
                            instance->stateName, migration_state_names[instance->migration_state]);
                }
            } else {
                LOGERROR("[%s] action '%s' is not valid\n", instance->instanceId, action);
                return (EUCA_INVALID_ERROR);
            }

        } else if (!strcmp(pMeta->nodeName, destNodeName)) {    // this is a migrate request to destination

            if (!strcmp(action, "commit")) {
                LOGERROR("[%s] action '%s' for migration (%s > %s) is not valid on destination node\n", instance_req->instanceId, action, SP(sourceNodeName), SP(destNodeName));
                return (EUCA_UNSUPPORTED_ERROR);
            } else if (!strcmp(action, "rollback")) {
                LOGINFO("[%s] rolling back migration (%s > %s) on destination\n", instance_req->instanceId, SP(sourceNodeName), SP(destNodeName));
                sem_p(inst_sem);
                {
                    ncInstance *instance = find_instance(&global_instances, instance_req->instanceId);
                    if (instance != NULL) {
                        LOGDEBUG("[%s] marked for cleanup\n", instance->instanceId);
                        change_state(instance, SHUTOFF);
                        instance->migration_state = MIGRATION_CLEANING;
                        save_instance_struct(instance);
                    }
                }
                sem_v(inst_sem);
                return EUCA_OK;
            } else if (strcmp(action, "prepare") != 0) {
                LOGERROR("[%s] action '%s' is not valid or not implemented\n", instance_req->instanceId, action);
                return (EUCA_INVALID_ERROR);
            }
            // Everything from here on is specific to "prepare" on a destination.

            // allocate a new instance struct
            ncInstance *instance = clone_instance(instance_req);
            if (instance == NULL) {
                LOGERROR("[%s] could not allocate instance struct\n", instance_req->instanceId);
                goto failed_dest;
            }

            sem_p(inst_sem);
            instance->migration_state = MIGRATION_PREPARING;
            instance->migrationTime = time(NULL); //In preparing state, so set migrationTime.
            euca_strncpy(instance->migration_src, sourceNodeName, HOSTNAME_SIZE);
            euca_strncpy(instance->migration_dst, destNodeName, HOSTNAME_SIZE);
            euca_strncpy(instance->migration_credentials, credentials, CREDENTIAL_SIZE);
            update_resource_locations(&(instance->params), resourceLocations, resourceLocationsLen);
            sem_v(inst_sem);

            // Establish migration-credential keys.
            LOGINFO("[%s] migration destination preparing %s > %s [creds=%s]\n", instance->instanceId, SP(instance->migration_src), SP(instance->migration_dst),
                    (instance->migration_credentials == NULL) ? "UNSET" : "present");
            // First, call config-file modification script to authorize source node.
            LOGDEBUG("[%s] authorizing migration source node %s\n", instance->instanceId, instance->migration_src);
            if (authorize_migration_keys(instance->migration_src, instance->migration_credentials, instance, TRUE) != EUCA_OK) {
                goto failed_dest;
            }
            // Then, generate keys and restart libvirtd.
            if (generate_migration_keys(instance->migration_dst, instance->migration_credentials, TRUE, instance) != EUCA_OK) {
                goto failed_dest;
            }
            int error;

            //Fix for EUCA-10433, need instance struct in global_instances prior to doing volume ops
            //The monitor thread will now pick up the instance, so the migrationTime must be set
            sem_p(inst_sem);
            save_instance_struct(instance);
            error = add_instance(&global_instances, instance);
            copy_instances(); 
            sem_v(inst_sem);
            if (error) {
                if (error == EUCA_DUPLICATE_ERROR) {
                    LOGINFO("[%s] instance struct already exists (from previous migration?), deleting and re-adding...\n", instance->instanceId);
                    error = remove_instance(&global_instances, instance);
                    if (error) {
                        LOGERROR("[%s] could not replace (remove) instance struct, failing...\n", instance->instanceId);
                        goto failed_dest;
                    }
                    error = add_instance(&global_instances, instance);
                    if (error) {
                        LOGERROR("[%s] could not replace (add) instance struct, failing...\n", instance->instanceId);
                        goto failed_dest;
                    }
                } else {
                    LOGERROR("[%s] could not add instance struct, failing...\n", instance->instanceId);
                    goto failed_dest;
                }
            }
            
            if (vbr_parse(&(instance->params), pMeta) != EUCA_OK) {
                goto failed_dest;
            }
            // set up networking
            char brname[IF_NAME_LEN] = "";
            snprintf(brname, IF_NAME_LEN, "%s", nc->pEucaNet->sBridgeDevice);
            euca_strncpy(instance->params.guestNicDeviceName, brname, sizeof(instance->params.guestNicDeviceName));
            // TODO: move stuff in startup_thread() into a function?
            
            set_instance_params(instance);

            if ((error = create_instance_backing(instance, TRUE))   // create files that back the disks
                || (error = gen_instance_xml(instance)) // create euca-specific instance XML file
                || (error = gen_libvirt_instance_xml(instance))) {  // transform euca-specific XML into libvirt XML
                LOGERROR("[%s] failed to prepare images for migrating instance (error=%d)\n", instance->instanceId, error);
                goto failed_dest;
            }

            // attach any volumes
            for (int v = 0; v < EUCA_MAX_VOLUMES; v++) {
                ncVolume *volume = &instance->volumes[v];
                if (strcmp(volume->stateName, VOL_STATE_ATTACHED) && strcmp(volume->stateName, VOL_STATE_ATTACHING))
                    continue;          // skip the entry unless attached or attaching
                LOGDEBUG("[%s] volumes [%d] = '%s'\n", instance->instanceId, v, volume->stateName);

                ebs_volume_data *vol_data = NULL;
                char *libvirt_xml = NULL;
                char serial[128];
                char bus[16];
                set_serial_and_bus(volume->volumeId, volume->devName, serial, sizeof(serial), bus, sizeof(bus));

                if ((ret = connect_ebs(volume->devName, serial, bus, nc, instance->instanceId, volume->volumeId, volume->attachmentToken, &libvirt_xml, &vol_data)) != EUCA_OK) {
                    goto unroll;
                }
                // update the volume struct with connection string obtained from SC
                euca_strncpy(volume->connectionString, vol_data->connect_string, sizeof(volume->connectionString));
                // save volume info into vol-XXX-libvirt.xml for future detach
                if (create_vol_xml(instance->instanceId, volume->volumeId, libvirt_xml, &libvirt_xml_modified) != EUCA_OK) {
                    goto unroll;
                }

                continue;
unroll:
                ret = EUCA_ERROR;

                // @TODO: unroll all previous ones
                //  for (int uv = v - 1; uv >= 0; uv--) {
                //    disconnect_ebs(nc, instance->instanceId, volume->volumeId, )
                //  }

                goto failed_dest;
            }

                        // build any secondary network interface xml files
            for (int w=0; w < EUCA_MAX_NICS; w++) {
                if (strlen(instance->secNetCfgs[w].interfaceId) == 0)
                    continue;
                gen_libvirt_nic_xml(instance->instancePath, instance->secNetCfgs[w].interfaceId);
            }

            sem_p(inst_sem);
            instance->migration_state = MIGRATION_READY;
            instance->migrationTime = 0; //Reset the timer, to ensure monitoring thread handles this properly. This is required when setting BOOTING state
            instance->bootTime = time(NULL);    // otherwise nc_state.booting_cleanup_threshold will kick in
            change_state(instance, BOOTING);    // not STAGING, since in that mode we don't poll hypervisor for info
            LOGINFO("[%s] migration destination ready %s > %s\n", instance->instanceId, instance->migration_src, instance->migration_dst);
            save_instance_struct(instance);
            copy_instances();
            sem_v(inst_sem);
            continue;

failed_dest:
            sem_p(inst_sem);
            // Just making sure...
            if (instance != NULL) {
                LOGERROR("[%s] setting instance to Teardown(cleaning) after destination failure to prepare for migration\n", instance->instanceId);
                // Set state to Teardown(cleaning) so source won't wait until timeout to roll back.
                instance->migration_state = MIGRATION_CLEANING;
                instance->terminationTime = time(NULL);
                change_state(instance, TEARDOWN);
                save_instance_struct(instance);
                add_instance(&global_instances, instance);  // OK if this fails--that should mean it's already been added.
                copy_instances();
            }
            // If no remaining incoming or pending migrations, deauthorize all clients.
            // TO-DO: Consolidate with similar sequence in handlers.c into a utility function?
            if (!incoming_migrations_in_progress) {
                int incoming_migrations_pending = 0;
                LOGINFO("[%s] no remaining active incoming migrations -- checking to see if there are any pending migrations\n", instance->instanceId);
                bunchOfInstances *head = NULL;
                for (head = global_instances; head; head = head->next) {
                    if ((head->instance->migration_state == MIGRATION_PREPARING) || (head->instance->migration_state == MIGRATION_READY)) {
                        LOGINFO("[%s] is pending migration, state='%s', deferring deauthorization of migration keys\n", head->instance->instanceId,
                                migration_state_names[head->instance->migration_state]);
                        incoming_migrations_pending++;
                    }
                }
                // TO-DO: Add belt and suspenders?
                if (!incoming_migrations_pending) {
                    LOGINFO("[%s] no remaining incoming or pending migrations -- deauthorizing all migration client keys\n", instance->instanceId);
                    deauthorize_migration_keys(FALSE);
                }
            }
            sem_v(inst_sem);
            // Set to generic EUCA_ERROR unless already set to a more-specific error.
            if (ret == EUCA_OK) {
                ret = EUCA_ERROR;
            }
        } else {
            LOGERROR("unexpected migration request (node %s is neither source nor destination)\n", pMeta->nodeName);
            ret = EUCA_ERROR;
        }
    }
    return ret;
}
