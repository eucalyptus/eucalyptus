// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file node/client-marshal-fake.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>                     /* For O_* constants */
#include <sys/stat.h>                  /* For mode constants */
#include <semaphore.h>
#include <sys/mman.h>
#include <sys/stat.h>                  /* For mode constants */
#include <fcntl.h>                     /* For O_* constants */

#include <eucalyptus.h>
#define HANDLERS_FANOUT
#include "handlers.h"
#include "client-marshal.h"
#include <euca_auth.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_FAKE_INSTANCES                      4096    //!< Maximum number of fake instances

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct fakeconfig_t fakeconfig;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

enum {
    SHARED_MEM,                        //!< Shared memory
    SHARED_FILE,                       //!< Shared file
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

 //! Fake Client Configuration Structure
struct fakeconfig_t {
    ncInstance global_instances[MAX_FAKE_INSTANCES];    //!< list of instances
    ncResource res;                    //!< NC component resources
    time_t current;
    time_t last;
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

fakeconfig *myconfig = NULL;           //!< fake configuration
sem_t *fakelock = NULL;                //!< fake configuration lock

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
//! Save the NC stuff
//!
void saveNcStuff()
{
    sem_post(fakelock);
}

//!
//! Setup a shared buffer
//!
//! @param[in] buf a pointer to a string buffer
//! @param[in] bufname the name of our buffer
//! @param[in] bytes the size of hte buffer
//! @param[in] lock a pointer to the lock pointer
//! @param[in] lockname the name of the lock
//! @param[in] mode the access mode (SHARED_MEM or SHARED_FILE)
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int setup_shared_buffer_fake(void **buf, char *bufname, size_t bytes, sem_t ** lock, char *lockname, int mode)
{
    int shd, rc, ret;

    // create a lock and grab it
    *lock = sem_open(lockname, O_CREAT, 0644, 1);
    sem_wait(*lock);
    ret = 0;

    if (mode == SHARED_MEM) {
        // set up shared memory segment for config
        shd = shm_open(bufname, O_CREAT | O_RDWR | O_EXCL, 0644);
        if (shd >= 0) {
            // if this is the first process to create the config, init to 0
            rc = ftruncate(shd, bytes);
        } else {
            shd = shm_open(bufname, O_CREAT | O_RDWR, 0644);
        }
        if (shd < 0) {
            fprintf(stderr, "cannot initialize shared memory segment\n");
            sem_post(*lock);
            sem_close(*lock);
            return (1);
        }
        *buf = mmap(0, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, shd, 0);
    } else if (mode == SHARED_FILE) {
        char *tmpstr, path[EUCA_MAX_PATH];
        struct stat mystat;
        int fd;

        tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
        if (!tmpstr) {
            snprintf(path, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/CC/%s", "", bufname);
        } else {
            snprintf(path, EUCA_MAX_PATH, EUCALYPTUS_STATE_DIR "/CC/%s", tmpstr, bufname);
        }
        fd = open(path, O_RDWR | O_CREAT, 0600);
        if (fd < 0) {
            fprintf(stderr, "ERROR: cannot open/create '%s' to set up mmapped buffer\n", path);
            ret = 1;
        } else {
            mystat.st_size = 0;
            rc = fstat(fd, &mystat);
            // this is the check to make sure we're dealing with a valid prior config
            if (mystat.st_size != bytes) {
                rc = ftruncate(fd, bytes);
            }
            *buf = mmap(NULL, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
            if (*buf == NULL) {
                fprintf(stderr, "ERROR: cannot mmap fd\n");
                ret = 1;
            }
            close(fd);
        }
    }
    sem_post(*lock);
    return (ret);
}

#if 0
int setup_shared_buffer_fake(void **buf, char *bufname, size_t bytes, sem_t ** lock, char *lockname, int mode)
{
    int shd = 0;
    int rc = 0;
    int ret = EUCA_OK;
    int fd = 0;
    char *tmpstr = NULL;
    char path[EUCA_MAX_PATH] = "";
    struct stat mystat = { 0 };

    // create a lock and grab it
    *lock = sem_open(lockname, O_CREAT, 0644, 1);
    sem_wait(*lock);

    if (mode == SHARED_MEM) {
        // set up shared memory segment for config
        if ((shd = shm_open(bufname, O_CREAT | O_RDWR | O_EXCL, 0644)) >= 0) {
            // if this is the first process to create the config, init to 0
            rc = ftruncate(shd, bytes);
        } else {
            shd = shm_open(bufname, O_CREAT | O_RDWR, 0644);
        }

        if (shd < 0) {
            fprintf(stderr, "cannot initialize shared memory segment\n");
            sem_post(*lock);
            sem_close(*lock);
            return (EUCA_ERROR);
        }

        *buf = mmap(0, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, shd, 0);
    } else if (mode == SHARED_FILE) {
        if ((tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME)) == NULL) {
            snprintf(path, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/CC/%s", bufname);
        } else {
            snprintf(path, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/CC/%s", tmpstr, bufname);
        }

        if ((fd = open(path, O_RDWR | O_CREAT, 0600)) < 0) {
            fprintf(stderr, "ERROR: cannot open/create '%s' to set up mmapped buffer\n", path);
            ret = EUCA_ERROR;
        } else {
            mystat.st_size = 0;
            rc = fstat(fd, &mystat);
            // this is the check to make sure we're dealing with a valid prior config
            if (mystat.st_size != bytes) {
                rc = ftruncate(fd, bytes);
            }

            if ((*buf = mmap(NULL, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0)) == NULL) {
                fprintf(stderr, "ERROR: cannot mmap fd\n");
                ret = EUCA_ERROR;
            }

            close(fd);
        }
    }
    sem_post(*lock);
    return (ret);
}
#endif

//!
//! Loads NC stuff
//!
void loadNcStuff()
{
    int i = 0;
    int j = 0;
    int count = 0;
    int done = 0;
    int rc = 0;

    rc = setup_shared_buffer_fake((void **)&myconfig, "/eucalyptusCCfakeconfig", sizeof(fakeconfig), &fakelock, "/eucalyptusCCfakelock", SHARED_FILE);
    if (rc) {
        LOGDEBUG("fakeNC:  error setting up shared mem\n");
    }
    sem_wait(fakelock);

    done = 0;
    for (i = 0; i < MAX_FAKE_INSTANCES && !done; i++) {
        if (!strlen(myconfig->global_instances[i].instanceId)) {
            count = i;
            done++;
        }
    }

    if (myconfig->last == 0) {
        myconfig->last = time(NULL);
        myconfig->current = time(NULL);
    } else {
        myconfig->current = time(NULL);
    }

    LOGDEBUG("fakeNC: setup(): last=%ld current=%ld\n", myconfig->last, myconfig->current);
    if ((myconfig->current - myconfig->last) > 30) {
        // do a refresh
        myconfig->last = time(NULL);
        myconfig->current = time(NULL);
        for (i = 0; i < MAX_FAKE_INSTANCES; i++) {
            if (strlen(myconfig->global_instances[i].instanceId)) {
                if (!strcmp(myconfig->global_instances[i].stateName, "Teardown") && ((time(NULL) - myconfig->global_instances[i].launchTime) > 300)) {
                    LOGDEBUG("fakeNC: setup(): invalidating instance %s\n", myconfig->global_instances[i].instanceId);
                    bzero(&(myconfig->global_instances[i]), sizeof(ncInstance));
                } else {
                    for (j = 0; j < EUCA_MAX_VOLUMES; j++) {
                        if (strlen(myconfig->global_instances[i].volumes[j].volumeId)
                            && strcmp(myconfig->global_instances[i].volumes[j].stateName, "attached")) {
                            LOGDEBUG("fakeNC: setup(): invalidating volume %s\n", myconfig->global_instances[i].volumes[j].volumeId);
                            bzero(&(myconfig->global_instances[i].volumes[j]), sizeof(ncVolume));
                        }
                    }
                }
            }
        }
    }
}

//!
//! Creates and initialize an NC stub entry
//!
//! @param[in] endpoint_uri the endpoint URI string
//! @param[in] logfile the log file name string
//! @param[in] homedir the home directory path string
//!
//! @return a pointer to the newly created NC stub structure
//!
ncStub *ncStubCreate(char *endpoint_uri, char *logfile, char *homedir)
{
    char *uri = NULL;
    char *p = NULL;
    char *node_name = NULL;
    axutil_env_t *env = NULL;
    axis2_char_t *client_home = NULL;
    axis2_stub_t *stub = NULL;
    ncStub *st = NULL;

    if (logfile) {
        env = axutil_env_create_all(logfile, AXIS2_LOG_LEVEL_TRACE);
    } else {
        env = axutil_env_create_all(NULL, 0);
    }

    if (homedir) {
        client_home = (axis2_char_t *) homedir;
    } else {
        client_home = AXIS2_GETENV("AXIS2C_HOME");
    }

    if (client_home == NULL) {
        LOGERROR("fakeNC: ERROR: cannot get AXIS2C_HOME");
        return NULL;
    }

    if (endpoint_uri == NULL) {
        LOGERROR("fakeNC: ERROR: empty endpoint_url");
        return NULL;
    }

    uri = endpoint_uri;

    // extract node name from the endpoint
    p = strstr(uri, "://");            // find "http[s]://..."
    if (p == NULL) {
        LOGERROR("fakeNC: ncStubCreate received invalid URI %s\n", uri);
        return NULL;
    }

    node_name = strdup(p + 3);         // copy without the protocol prefix
    if (node_name == NULL) {
        LOGERROR("fakeNC: ncStubCreate is out of memory\n");
        return NULL;
    }

    if ((p = strchr(node_name, ':')) != NULL)
        *p = '\0';                     // cut off the port

    if ((p = strchr(node_name, '/')) != NULL)
        *p = '\0';                     // if there is no port

    LOGDEBUG("fakeNC: DEBUG: requested URI %s\n", uri);

    // see if we should redirect to a local broker
    if (strstr(uri, "EucalyptusBroker")) {
        uri = "http://localhost:8773/services/EucalyptusBroker";
        LOGDEBUG("fakeNC: DEBUG: redirecting request to %s\n", uri);
    }
    //! @todo what if endpoint_uri, home, or env are NULL?
    stub = axis2_stub_create_EucalyptusNC(env, client_home, (axis2_char_t *) uri);

    if (stub && (st = EUCA_ZALLOC(1, sizeof(ncStub)))) {
        st->env = env;
        st->client_home = strdup((char *)client_home);
        st->endpoint_uri = (axis2_char_t *) strdup(endpoint_uri);
        st->node_name = (axis2_char_t *) strdup(node_name);
        st->stub = stub;
        if (st->client_home == NULL || st->endpoint_uri == NULL) {
            LOGWARN("fakeNC: WARNING: out of memory");
        }
    } else {
        LOGWARN("fakeNC: WARNING: out of memory");
    }

    EUCA_FREE(node_name);
    return (st);
}

//!
//! destroy an NC stub structure
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//!
//! @return Always returns EUCA_OK
//!
int ncStubDestroy(ncStub * pStub)
{
    EUCA_FREE(pStub);
    return (EUCA_OK);
}

//! Handles the client broadcast network info rquest
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] networkInfo is a string
//!
//! @return Always return EUCA_OK.
//!
int ncBroadcastNetworkInfoStub(ncStub * pStub, ncMetadata * pMeta, char *networkInfo)
{
    char *xmlbuf = NULL, xmlpath[EUCA_MAX_PATH];
    int ret = EUCA_OK, rc = 0;

    if (networkInfo == NULL) {
        LOGERROR("internal error (bad input parameters to doBroadcastNetworkInfo)\n");
        return (EUCA_INVALID_ERROR);
    }

    LOGTRACE("encoded networkInfo=%s\n", networkInfo);
    snprintf(xmlpath, EUCA_MAX_PATH, "/tmp/global_network_info.xml");
    LOGDEBUG("decoding/writing buffer to (%s)\n", xmlpath);
    xmlbuf = base64_dec((unsigned char *)networkInfo, strlen(networkInfo));
    if (xmlbuf) {
        rc = str2file(xmlbuf, xmlpath, O_CREAT | O_TRUNC | O_WRONLY, 0644, FALSE);
        if (rc) {
            LOGERROR("could not write XML data to file (%s)\n", xmlpath);
            ret = EUCA_ERROR;
        }
        EUCA_FREE(xmlbuf);
    } else {
        LOGERROR("could not b64 decode input buffer\n");
        ret = EUCA_ERROR;
    }

    return (EUCA_OK);
}

//!
//! Handles the Run instance request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  uuid unique user identifier string
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  reservationId the reservation identifier string
//! @param[in]  params a pointer to the virtual machine parameters to use
//! @param[in]  imageId the image identifier string
//! @param[in]  imageURL the image URL address tring
//! @param[in]  kernelId the kernel image identifier (eki-XXXXXXXX)
//! @param[in]  kernelURL the kernel image URL address
//! @param[in]  ramdiskId the ramdisk image identifier (eri-XXXXXXXX)
//! @param[in]  ramdiskURL the ramdisk image URL address
//! @param[in]  ownerId the owner identifier string
//! @param[in]  accountId the account identifier string
//! @param[in]  keyName the key name string
//! @param[in]  netparams a pointer to the network parameters string
//! @param[in]  userData the user data string
//! @param[in]  launchIndex the launch index string
//! @param[in]  platform the platform name string
//! @param[in]  expiryTime the reservation expiration time
//! @param[in]  groupNames a list of group name string
//! @param[in]  groupNamesSize the number of group name in the groupNames list
//! @param[out] outInstPtr the list of instances created by this request
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncRunInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params, char *imageId,
                      char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId,
                      char *keyName, netConfig * netparams, char *userData, char *credential, char *launchIndex, char *platform, int expiryTime, char **groupNames,
                      int groupNamesSize, ncInstance ** outInstPtr)
{
    int i = 0;
    int foundidx = -1;
    ncInstance *instance = NULL;

    LOGDEBUG("fakeNC: runInstance(): params: uuid=%s instanceId=%s reservationId=%s ownerId=%s accountId=%s platform=%s\n", SP(uuid),
             SP(instanceId), SP(reservationId), SP(ownerId), SP(accountId), SP(platform));

    if (!uuid || !instanceId || !reservationId || !ownerId || !accountId || !platform || !pMeta || !netparams) {
        LOGERROR("fakeNC: runInstance(): bad input params\n");
        return (EUCA_ERROR);
    }

    loadNcStuff();

    instance = allocate_instance(uuid, instanceId, reservationId, params, instance_state_names[PENDING], PENDING, pMeta->userId, ownerId, accountId,
                                 netparams, keyName, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize);
    if (instance) {
        instance->launchTime = time(NULL);
        foundidx = -1;
        for (i = 0; i < MAX_FAKE_INSTANCES && (foundidx < 0); i++) {
            if (!strlen(myconfig->global_instances[i].instanceId)) {
                foundidx = i;
            }
        }

        memcpy(&(myconfig->global_instances[foundidx]), instance, sizeof(ncInstance));
        LOGDEBUG("fakeNC: runInstance(): decrementing resource by %d/%d/%d\n", params->cores, params->mem, params->disk);
        myconfig->res.memorySizeAvailable -= params->mem;
        myconfig->res.numberOfCoresAvailable -= params->cores;
        myconfig->res.diskSizeAvailable -= params->disk;

        *outInstPtr = instance;
        LOGDEBUG("fakeNC: runInstance(): allocated and stored instance\n");
    } else {
        LOGERROR("fakeNC: runInstance(): failed to allocate instance\n");
    }

    saveNcStuff();
    return (EUCA_OK);
}

//!
//! Handles the Terminate instance request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in]  force if set to 1 will force the termination of the instance
//! @param[out] shutdownState the instance state code after the call to find_and_terminate_instance() if successful
//! @param[out] previousState the instance state code after the call to find_and_terminate_instance() if successful
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncTerminateInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState)
{
    int i = 0;
    int done = 0;

    LOGDEBUG("fakeNC: terminateInstance(): params: instanceId=%s force=%d\n", SP(instanceId), force);

    if (!instanceId) {
        LOGERROR("fakeNC: termianteInstance(): bad input params\n");
        return (EUCA_ERROR);
    }

    loadNcStuff();

    for (i = 0; i < MAX_FAKE_INSTANCES && !done; i++) {
        if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
            LOGDEBUG("fakeNC: terminateInstance():\tsetting stateName for instance %s at idx %d\n", instanceId, i);
            snprintf(myconfig->global_instances[i].stateName, 10, "Teardown");
            myconfig->res.memorySizeAvailable += myconfig->global_instances[i].params.mem;
            myconfig->res.numberOfCoresAvailable += myconfig->global_instances[i].params.cores;
            myconfig->res.diskSizeAvailable += myconfig->global_instances[i].params.disk;
            done++;
        }
    }

    if (shutdownState && previousState) {
        *shutdownState = *previousState = 0;
    }

    saveNcStuff();
    return (EUCA_OK);
}

//!
//! Handles the client assign address request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] publicIp a string representation of the public IP to assign to the instance
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncAssignAddressStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *publicIp)
{
    int i = 0;
    int done = 0;

    LOGDEBUG("fakeNC: assignAddress(): params: instanceId=%s publicIp=%s\n", SP(instanceId), SP(publicIp));
    if (!instanceId || !publicIp) {
        LOGDEBUG("fakeNC: assignAddress(): bad input params\n");
        return (EUCA_ERROR);
    }

    loadNcStuff();

    for (i = 0; i < MAX_FAKE_INSTANCES && !done; i++) {
        if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
            LOGDEBUG("fakeNC: assignAddress()\tsetting publicIp at idx %d\n", i);
            snprintf(myconfig->global_instances[i].ncnet.publicIp, IP_BUFFER_SIZE, "%s", publicIp);
            done++;
        }
    }

    saveNcStuff();
    return (EUCA_OK);
}

//!
//! Handles the client power down rquest
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//!
//! @return Always return EUCA_OK.
//!
int ncPowerDownStub(ncStub * pStub, ncMetadata * pMeta)
{
    return (EUCA_OK);
}

//!
//! Handles the client describe instance request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a pointer the list of instance identifiers to retrieve data for
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outInsts a pointer the list of instances for which we have data
//! @param[out] outInstsLen the number of instances in the outInsts list.
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncDescribeInstancesStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen)
{
    int i = 0;
    int numinsts = 0;
    ncInstance *newinst = NULL;

    LOGDEBUG("fakeNC: describeInstances(): params: instIdsLen=%d\n", instIdsLen);

    if (instIdsLen < 0) {
        LOGERROR("fakeNC: describeInstances(): bad input params\n");
        return (EUCA_ERROR);
    }

    loadNcStuff();

    //  *outInstsLen = myconfig->instanceidx+1;
    *outInsts = EUCA_ZALLOC(MAX_FAKE_INSTANCES, sizeof(ncInstance *));
    for (i = 0; i < MAX_FAKE_INSTANCES; i++) {
        if (strlen(myconfig->global_instances[i].instanceId)) {
            newinst = EUCA_ZALLOC(1, sizeof(ncInstance));
            if (!strcmp(myconfig->global_instances[i].stateName, "Pending")) {
                snprintf(myconfig->global_instances[i].stateName, 8, "Extant");
                if (!strcmp(myconfig->global_instances[i].ncnet.publicIp, "0.0.0.0")) {
                    snprintf(myconfig->global_instances[i].ncnet.publicIp, IP_BUFFER_SIZE, "%d.%d.%d.%d", rand() % 254 + 1, rand() % 254 + 1, rand() % 254 + 1, rand() % 254 + 1);
                }
                if (!strcmp(myconfig->global_instances[i].ncnet.privateIp, "0.0.0.0")) {
                    snprintf(myconfig->global_instances[i].ncnet.privateIp, IP_BUFFER_SIZE, "%d.%d.%d.%d", rand() % 254 + 1, rand() % 254 + 1, rand() % 254 + 1, rand() % 254 + 1);
                }
            }

            memcpy(newinst, &(myconfig->global_instances[i]), sizeof(ncInstance));
            (*outInsts)[numinsts] = newinst;
            LOGDEBUG("fakeNC: describeInstances(): idx=%d numinsts=%d instanceId=%s stateName=%s\n", i, numinsts, newinst->instanceId, newinst->stateName);
            numinsts++;
        }
    }
    *outInstsLen = numinsts;

    saveNcStuff();
    return (EUCA_OK);
}

//!
//! Handles the client bundle instance request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] bucketName the bucket name string to which the bundle will be saved
//! @param[in] filePrefix the prefix name string of the bundle
//! @param[in] walrusURL the walrus URL address string
//! @param[in] userPublicKey the public key string
//! @param[in] S3Policy the S3 engine policy
//! @param[in] S3PolicySig the S3 engine policy signature
//!
//! @return Always return EUCA_OK
//!
int ncBundleInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL,
                         char *userPublicKey, char *S3Policy, char *S3PolicySig)
{
    return (EUCA_OK);
}

//!
//! Handles the client restart instance request once bundling has completed.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncBundleRestartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

//!
//! Handles the client cancel bundle task request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return Always return EUCA_OK
//!
int ncCancelBundleTaskStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

//!
//! Handles the client describe bundles task request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instIds a list of instance identifier string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[out] outBundleTasks a pointer to the created bundle tasks list
//! @param[out] outBundleTasksLen the number of bundle tasks in the outBundleTasks list
//!
//! @return Always return EUCA_OK
//!
int ncDescribeBundleTasksStub(ncStub * pStub, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen)
{
    return (EUCA_OK);
}

//!
//! Handle the client describe resource request
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  resourceType UNUSED
//! @param[out] outRes a list of resources we retrieved data for
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncDescribeResourceStub(ncStub * pStub, ncMetadata * pMeta, char *resourceType, ncResource ** outRes)
{
    int ret = EUCA_OK;
    ncResource *res = NULL;

    loadNcStuff();

    if (myconfig->res.memorySizeMax <= 0) {
        // not initialized?
        res = allocate_resource("OK", 0, "iqn.1993-08.org.debian:01:736a4e92c588", 1024000, 1024000, 30000000, 30000000, 4096, 4096, "none");
        if (!res) {
            LOGERROR("fakeNC: describeResource(): failed to allocate fake resource\n");
            ret = EUCA_ERROR;
        } else {
            memcpy(&(myconfig->res), res, sizeof(ncResource));
            EUCA_FREE(res);
        }
    }

    if (!ret) {
        snprintf(myconfig->res.nodeStatus, 32, "enabled");
        res = EUCA_ALLOC(1, sizeof(ncResource));
        memcpy(res, &(myconfig->res), sizeof(ncResource));
        *outRes = res;
    } else {
        *outRes = NULL;
    }

    saveNcStuff();
    return (ret);
}

//!
//! Handles the client attach volume request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncAttachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    int i = 0;
    int j = 0;
    int done = 0;
    int vdone = 0;
    int foundidx = -1;

    LOGDEBUG("fakeNC:  attachVolume(): params: instanceId=%s volumeId=%s remoteDev=%s localDev=%s\n", SP(instanceId), SP(volumeId), SP(remoteDev), SP(localDev));
    if (!instanceId || !volumeId || !remoteDev || !localDev) {
        LOGDEBUG("fakeNC:  attachVolume(): bad input params\n");
        return (EUCA_ERROR);
    }

    loadNcStuff();

    for (i = 0; i < MAX_FAKE_INSTANCES && !done; i++) {
        if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
            LOGDEBUG("fakeNC: \tsetting volume info at idx %d\n", i);
            vdone = 0;
            for (j = 0; j < EUCA_MAX_VOLUMES; j++) {
                if (!strlen(myconfig->global_instances[i].volumes[j].volumeId)) {
                    if (foundidx < 0) {
                        foundidx = j;
                    }
                } else if (!strcmp(myconfig->global_instances[i].volumes[j].volumeId, volumeId)) {
                    vdone++;
                }
            }
            if (!vdone && foundidx >= 0) {
                LOGDEBUG("fakeNC: \tfake attaching volume at idx %d\n", foundidx);
                snprintf(myconfig->global_instances[i].volumes[foundidx].volumeId, CHAR_BUFFER_SIZE, "%s", volumeId);
                snprintf(myconfig->global_instances[i].volumes[foundidx].attachmentToken, CHAR_BUFFER_SIZE, "%s", remoteDev);
                snprintf(myconfig->global_instances[i].volumes[foundidx].localDev, CHAR_BUFFER_SIZE, "%s", localDev);
                snprintf(myconfig->global_instances[i].volumes[foundidx].localDevReal, CHAR_BUFFER_SIZE, "%s", localDev);
                snprintf(myconfig->global_instances[i].volumes[foundidx].stateName, CHAR_BUFFER_SIZE, "%s", "attached");
            }
            done++;
        }
    }

    saveNcStuff();
    return (EUCA_OK);
}

//!
//! Handles the client detach volume request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the target device name
//! @param[in] localDev the local device name
//! @param[in] force if set to 1, this will force the volume to detach
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int ncDetachVolumeStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    int i = 0;
    int j = 0;
    int done = 0;
    int vdone = 0;
    int foundidx = -1;

    LOGDEBUG("fakeNC:  detachVolume(): params: instanceId=%s volumeId=%s remoteDev=%s localDev=%s\n", SP(instanceId), SP(volumeId), SP(remoteDev), SP(localDev));
    if (!instanceId || !volumeId || !remoteDev || !localDev) {
        LOGDEBUG("fakeNC:  detachVolume(): bad input params\n");
        return (EUCA_ERROR);
    }

    loadNcStuff();

    for (i = 0; i < MAX_FAKE_INSTANCES && !done; i++) {
        if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
            LOGDEBUG("fakeNC: \tsetting volume info at idx %d\n", i);
            vdone = 0;
            for (j = 0; j < EUCA_MAX_VOLUMES; j++) {
                if (!strcmp(myconfig->global_instances[i].volumes[j].volumeId, volumeId)) {
                    foundidx = j;
                }
            }
            if (foundidx >= 0) {
                LOGDEBUG("fakeNC: \tfake detaching volume at idx %d\n", foundidx);
                snprintf(myconfig->global_instances[i].volumes[foundidx].stateName, CHAR_BUFFER_SIZE, "%s", "detached");
            }
            done++;
        }
    }

    saveNcStuff();
    return (EUCA_OK);
}

//!
//! Handles the client create image request.
//!
//! @param[in] pStub a pointer to the node controller (NC) stub structure
//! @param[in] pMeta a pointer to the node controller (NC) metadata structure
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev the remote device name
//!
//! @return Always return EUCA_OK
//!
int ncCreateImageStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev)
{
    return (EUCA_OK);
}

//!
//! Handles the client describe sensor request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  historySize teh size of the data history to retrieve
//! @param[in]  collectionIntervalTimeMs the data collection interval in milliseconds
//! @param[in]  instIds the list of instance identifiers string
//! @param[in]  instIdsLen the number of instance identifiers in the instIds list
//! @param[in]  sensorIds a list of sensor identifiers string
//! @param[in]  sensorIdsLen the number of sensor identifiers string in the sensorIds list
//! @param[out] outResources a list of sensor resources created by this request
//! @param[out] outResourcesLen the number of sensor resources contained in the outResources list
//!
//! @return Always return EUCA_OK
//!
int ncDescribeSensorsStub(ncStub * pStub, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen,
                          char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen)
{
    return (EUCA_OK);
}

//!
//! Handles the node controller modification request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  stateName the next state for the node controller
//!
//! @return Always returns EUCA_OK
//!
int ncModifyNodeStub(ncStub * pStub, ncMetadata * pMeta, char *stateName)
{
    return (EUCA_OK);
}

//!
//! Marshals the instance migration request, with different behavior on source and destination.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instances metadata for the instance to migrate to destination
//! @param[in]  instancesLen number of instances in the instance list
//! @param[in]  action IP of the destination Node Controller
//! @param[in]  credentials credentials that enable the migration
//!
//! @return Always returns EUCA_OK
//!
//! @see ncMigrateInstances()
//!
int ncMigrateInstancesStub(ncStub * pStub, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials)
{
    return (EUCA_OK);
}

//!
//! Marshals the instance start request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId ID of the instance to control
//!
//! @return Always returns EUCA_OK
//!
//! @see ncStartInstance()
//!
int ncStartInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

//!
//! Marshals the instance shutdown request.
//!
//! @param[in]  pStub a pointer to the node controller (NC) stub structure
//! @param[in]  pMeta a pointer to the node controller (NC) metadata structure
//! @param[in]  instanceId ID of the instance to control
//!
//! @return Always returns EUCA_OK
//!
//! @see ncStopInstance()
//!
int ncStopInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

int ncGetConsoleOutputStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId, char **consoleOutput)
{
    return (EUCA_OK);
}

int ncOPERATIONStub(ncStub * pStub, ncMetadata * pMeta, ...)
{
    return (EUCA_OK);
}

int ncRebootInstanceStub(ncStub * pStub, ncMetadata * pMeta, char *instanceId)
{
    return (EUCA_OK);
}

int ncStartNetworkStub(ncStub * pStub, ncMetadata * pMeta, char *uuid, char **peers, int peersLen, int port, int vlan, char **outStatus)
{
    return (EUCA_OK);
}
