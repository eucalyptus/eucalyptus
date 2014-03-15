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
//! @file node/xml.c
//! Implements the XML utilities to create and modify the XML data needed
//! by the NC component.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#define __USE_GNU
#include <string.h>                    // strlen, strcpy
#include <time.h>
#include <sys/types.h>                 // umask
#include <sys/stat.h>                  // umask
#include <pthread.h>
#include <libxml/xmlmemory.h>
#include <libxml/debugXML.h>
#include <libxml/HTMLtree.h>
#include <libxml/xmlIO.h>
#include <libxml/xinclude.h>
#include <libxml/catalog.h>
#include <libxslt/xslt.h>
#include <libxslt/xsltInternals.h>
#include <libxslt/transform.h>
#include <libxslt/xsltutils.h>
#include <errno.h>

#include <eucalyptus.h>
#include <eucalyptus-config.h>
#include <backing.h>                   // umask
#include <data.h>
#include <misc.h>
#include <euca_string.h>

#include "handlers.h"                  // nc_state_t
#include "xml.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static boolean initialized = FALSE;    //!< To determine if the XML library has been initialized
static char nc_home[EUCA_MAX_PATH] = "";    //!< Base of the NC installation ("/" for packages)
static boolean config_use_virtio_root = 0;  //!< Set to TRUE if we are using VIRTIO root
static boolean config_use_virtio_disk = 0;  //!< Set to TRUE if we are using VIRTIO disks
static boolean config_use_virtio_net = 0;   //!< Set to TRUE if we are using VIRTIO network
static char xslt_path[EUCA_MAX_PATH] = "";  //!< Destination path for the XSLT files
static pthread_mutex_t xml_mutex = PTHREAD_MUTEX_INITIALIZER;   //!< process-global mutex

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void init_xml(struct nc_state_t *nc_state);

#if 0
// (unused for now)
static void cleanup(void);
#endif /* 0 */

static int path_check(const char *path, const char *name);
static int write_xml_file(const xmlDocPtr doc, const char *instanceId, const char *path, const char *type);
static void write_vbr_xml(xmlNodePtr vbrs, const virtualBootRecord * vbr);

static void error_handler(void *ctx, const char *fmt, ...) _attribute_format_(2, 3);
static int apply_xslt_stylesheet(const char *xsltStylesheetPath, const char *inputXmlPath, const char *outputXmlPath, char *outputXmlBuffer, int outputXmlBufferSize);

#ifdef __STANDALONE
static void create_dummy_instance(const char *file);
int main(int argc, char **argv);
#endif

#ifdef __STANDALONE2
int main(int argc, char **argv);
#endif
/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef __STANDALONE                    // if compiling as a stand-alone binary (for unit testing)
#define INIT() if (!initialized) init_xml(NULL)
#elif __STANDALONE2
#define INIT() if (!initialized) init_xml(NULL)
#else // if linking against an NC, find nc_state symbol
extern struct nc_state_t nc_state;
#define INIT() if (!initialized) init_xml(&nc_state)
#endif

// macros for making XML construction a bit more readable
#define _NODE(P,N) xmlNewChild((P), NULL, BAD_CAST (N), NULL)
#define _ELEMENT(P,N,V) xmlNewChild((P), NULL, BAD_CAST (N), BAD_CAST (V))
#define _ATTRIBUTE(P,N,V) xmlNewProp((P), BAD_CAST (N), BAD_CAST (V))
#define _BOOL(S) ((S)?("true"):("false"))

#define XGET_STR(_XPATH, _dest)                                                      \
{                                                                                    \
    LOGTRACE("reading up to %lu of " #_dest " from %s\n", sizeof(_dest), xml_path);  \
    if (get_xpath_content_at(xml_path, (_XPATH), 0, _dest, sizeof(_dest)) == NULL) { \
        LOGERROR("failed to read %s from %s\n", (_XPATH), xml_path);                 \
        return (EUCA_ERROR);                                                         \
    }                                                                                \
}

#define XGET_ENUM(_XPATH, _dest, _converter)                                           \
{                                                                                      \
	char __sBuf[32];                                                                   \
	if (get_xpath_content_at(xml_path, (_XPATH), 0, __sBuf, sizeof(__sBuf)) == NULL) { \
		LOGERROR("failed to read %s from %s\n", (_XPATH), xml_path);                   \
		return (EUCA_ERROR);                                                           \
	}                                                                                  \
	(_dest) = _converter(__sBuf);                                                      \
}

#define XGET_BOOL(_XPATH, _dest)                                                       \
{                                                                                      \
	char __sBuf[32];                                                                   \
	if (get_xpath_content_at(xml_path, (_XPATH), 0, __sBuf, sizeof(__sBuf)) == NULL) { \
		LOGERROR("failed to read %s from %s\n", (_XPATH), xml_path);                   \
		return (EUCA_ERROR);                                                           \
	}                                                                                  \
	if (strcmp(__sBuf, "true") == 0) {                                                 \
		(_dest) = 1;                                                                   \
	} else if (strcmp(__sBuf, "false") == 0) {                                         \
		(_dest) = 0;                                                                   \
	} else {                                                                           \
		LOGDEBUG("failed to parse %s as {true|false} in %s\n", (_XPATH), xml_path);    \
		return (EUCA_ERROR);                                                           \
	}                                                                                  \
}

#define XGET_INT(_XPATH, _dest) \
{                                                                                      \
	char __sBuf[32];                                                                   \
	if (get_xpath_content_at(xml_path, (_XPATH), 0, __sBuf, sizeof(__sBuf)) == NULL) { \
		LOGERROR("failed to read %s from %s\n", (_XPATH), xml_path);                   \
		return (EUCA_ERROR);                                                           \
	}                                                                                  \
	errno = 0;                                                                         \
	char *__psEnd = NULL;                                                              \
	long long __v = strtoll(__sBuf, &__psEnd, 10);                                     \
	if ((errno == 0) && ((*__psEnd) == '\0')) {                                        \
		(_dest) = __v;                                                                 \
	} else {                                                                           \
		LOGDEBUG("failed to parse %s as an integer in %s\n", (_XPATH), xml_path);      \
		return (EUCA_ERROR);                                                           \
	}                                                                                  \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Initialize the XML local parameters from the NC state structure.
//!
//! @param[in] nc_state a pointer to the NC state structure to initialize
//!
static void init_xml(struct nc_state_t *nc_state)
{
    pthread_mutex_lock(&xml_mutex);
    {
        if (!initialized) {
            xmlInitParser();
            LIBXML_TEST_VERSION;       // verifies that loaded library matches the compiled library
            xmlSubstituteEntitiesDefault(1);    // substitute entities while parsing
            xmlSetGenericErrorFunc(NULL, error_handler);    // catches errors/warnings that libxml2 writes to stderr
            xsltSetGenericErrorFunc(NULL, error_handler);   // catches errors/warnings that libslt writes to stderr
            if (nc_state != NULL) {
                euca_strncpy(nc_home, nc_state->home, sizeof(nc_home));
                config_use_virtio_root = nc_state->config_use_virtio_root;
                config_use_virtio_disk = nc_state->config_use_virtio_disk;
                config_use_virtio_net = nc_state->config_use_virtio_net;
                euca_strncpy(xslt_path, nc_state->libvirt_xslt_path, sizeof(xslt_path));
            }
            initialized = TRUE;
        }
    }
    pthread_mutex_unlock(&xml_mutex);
}

#if 0
//!
//! Cleanup the LIBXML library (Unused for now)
//!
static void cleanup(void)
{
    xsltCleanupGlobals();
    xmlCleanupParser();                // calls xmlCleanupGlobals()
}
#endif /* 0 */

//!
//! verify that the path for kernel/ramdisk is reasonable
//!
//! @param[in] path the path name to the kernel or ramdisk
//! @param[in] name a string containing "ramdisk" or "kernel" for information reason
//!
//! @return EUCA_OK if the path is valid or EUCA_ERROR if the path is invalid.
//!
static int path_check(const char *path, const char *name)
{
    if (strstr(path, "/dev/") == path) {
        LOGERROR("internal error: path to %s points to a device %s\n", name, path);
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Writes an XML file content to disk.
//!
//! @param[in] doc a pointer to the XML document structure to write to disk
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] path the destination path for the XML file
//! @param[in] type the file type (currently unused and forced to UTF-8).
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
static int write_xml_file(const xmlDocPtr doc, const char *instanceId, const char *path, const char *type)
{
    int ret = 0;
    mode_t old_umask = umask(~BACKING_FILE_PERM);   // ensure the generated XML file has the right perms

    chmod(path, BACKING_FILE_PERM);    // ensure perms in case when XML file exists
    if ((ret = xmlSaveFormatFileEnc(path, doc, "UTF-8", 1)) > 0) {
        LOGTRACE("[%s] wrote %s XML to %s\n", instanceId, type, path);
    } else {
        LOGERROR("[%s] failed to write %s XML to %s\n", instanceId, type, path);
    }
    umask(old_umask);
    return ((ret > 0) ? (EUCA_OK) : (EUCA_ERROR));
}

//!
//! Writes Node Controller state to disk, into an XML file
//!
//! @param[in] nc_state_param pointer to NC's global state struct to be savedd
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure to write the file
//!
int gen_nc_xml(const struct nc_state_t *nc_state_param)
{
    int ret = EUCA_ERROR;
    char *psCloudIp = NULL;
    char path[EUCA_MAX_PATH] = "";
    xmlDocPtr doc = NULL;
    xmlNodePtr nc = NULL;
    xmlNodePtr version = NULL;
    xmlNodePtr enabled = NULL;
    xmlNodePtr cloudIp = NULL;

    INIT();

    pthread_mutex_lock(&xml_mutex);
    {
        doc = xmlNewDoc(BAD_CAST "1.0");
        nc = xmlNewNode(NULL, BAD_CAST "nc");
        xmlDocSetRootElement(doc, nc);

        version = xmlNewChild(nc, NULL, BAD_CAST("version"), BAD_CAST(nc_state_param->version));
        enabled = xmlNewChild(nc, NULL, BAD_CAST("enabled"), BAD_CAST(nc_state_param->is_enabled ? "true" : "false"));

        if ((psCloudIp = hex2dot(nc_state_param->vnetconfig->cloudIp)) != NULL) {
            cloudIp = xmlNewChild(nc, NULL, BAD_CAST("cloudIp"), BAD_CAST(psCloudIp));
            EUCA_FREE(psCloudIp);
        }

        snprintf(path, sizeof(path), EUCALYPTUS_NC_STATE_FILE, nc_home);
        ret = write_xml_file(doc, "global", path, "nc");
        xmlFreeDoc(doc);
    }
    pthread_mutex_unlock(&xml_mutex);

    return (ret);
}

//!
//! Reads Node Controller state from disk
//!
//! @param[in] nc_state_param pointer to NC's global state struct to be updated
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure to read the file
//!
int read_nc_xml(struct nc_state_t *nc_state_param)
{
    char buf[1024] = "";
    char xml_path[EUCA_MAX_PATH] = "";

    INIT();

    snprintf(xml_path, sizeof(xml_path), EUCALYPTUS_NC_STATE_FILE, nc_home);

    XGET_STR("/nc/version", nc_state_param->version);
    XGET_BOOL("/nc/enabled", nc_state_param->is_enabled);
    XGET_STR("/nc/cloudIp", buf);
    nc_state_param->vnetconfig->cloudIp = dot2hex(buf);

    return (EUCA_OK);
}

//!
//! Writes VBR information to xml node
//!
//! @param[in] vbrs pointer to VBRs XML node
//! @param[in] vbr pointer to the VBR
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure to read the file
//!
static void write_vbr_xml(xmlNodePtr vbrs, const virtualBootRecord * vbr)
{
    xmlNodePtr node = _NODE(vbrs, "vbr");
    _ELEMENT(node, "resourceLocation", vbr->resourceLocation);
    _ELEMENT(node, "guestDeviceName", vbr->guestDeviceName);
    {
        char buf[64];
        snprintf(buf, sizeof(buf), "%lld", vbr->sizeBytes);
        _ELEMENT(node, "sizeBytes", buf);
    }
    _ELEMENT(node, "formatName", vbr->formatName);
    _ELEMENT(node, "id", vbr->id);
    _ELEMENT(node, "typeName", vbr->typeName);

    _ELEMENT(node, "type", ncResourceTypeNames[vbr->type]);
    _ELEMENT(node, "locationType", ncResourceLocationTypeNames[vbr->locationType]);
    _ELEMENT(node, "format", ncResourceFormatTypeNames[vbr->format]);
    {
        char buf[2];
        snprintf(buf, sizeof(buf), "%d", vbr->diskNumber);
        _ELEMENT(node, "diskNumber", buf);
    }
    {
        char buf[4];
        snprintf(buf, sizeof(buf), "%d", vbr->partitionNumber);
        _ELEMENT(node, "partitionNumber", buf);
    }
    _ELEMENT(node, "guestDeviceType", libvirtDevTypeNames[vbr->guestDeviceType]);
    _ELEMENT(node, "guestDeviceBus", libvirtBusTypeNames[vbr->guestDeviceBus]);
    _ELEMENT(node, "backingType", libvirtSourceTypeNames[vbr->backingType]);
    _ELEMENT(node, "backingPath", vbr->backingPath);
    _ELEMENT(node, "preparedResourceLocation", vbr->preparedResourceLocation);
}

//!
//! Encodes instance metadata (contained in ncInstance struct) in XML
//! and writes it to file instance->xmlFilePath (/path/to/instance/instance.xml)
//! That file gets processed through tools/libvirt.xsl (/etc/eucalyptus/libvirt.xsl)
//! to produce /path/to/instance/instance-libvirt.xml file that is passed to libvirt.
//!
//! @param[in] instance a pointer to the instance to generate XML from
//!
//! @return EUCA_OK if the operation is successful. Known error code returned include EUCA_ERROR.
//!
//! @see write_xml_file()
//!
int gen_instance_xml(const ncInstance * instance)
{
    int ret = EUCA_ERROR;
    int i = 0;
    int j = 0;
    char *path = NULL;
    char cores_s[10] = "";
    char memory_s[10] = "";
    char disk_s[10] = "";
    char bitness[4] = "";
    char root_uuid[64] = "";
    char devstr[SMALL_CHAR_BUFFER_SIZE] = "";
    xmlNodePtr disk = NULL;
    xmlDocPtr doc = NULL;
    xmlNodePtr instanceNode = NULL;
    xmlNodePtr hypervisor = NULL;
    xmlNodePtr backing = NULL;
    xmlNodePtr root = NULL;
    xmlNodePtr key = NULL;
    xmlNodePtr os = NULL;
    xmlNodePtr groupNames = NULL;
    xmlNodePtr disks = NULL;
    xmlNodePtr vbrs = NULL;
    xmlNodePtr rootNode = NULL;
    xmlNodePtr nics = NULL;
    xmlNodePtr nic = NULL;
    xmlNodePtr vols = NULL;
    const virtualBootRecord *vbr = NULL;

    INIT();

    pthread_mutex_lock(&xml_mutex);
    {
        doc = xmlNewDoc(BAD_CAST "1.0");
        instanceNode = xmlNewNode(NULL, BAD_CAST "instance");
        xmlDocSetRootElement(doc, instanceNode);

        // hypervisor-related specs
        hypervisor = xmlNewChild(instanceNode, NULL, BAD_CAST "hypervisor", NULL);
        _ATTRIBUTE(hypervisor, "type", instance->hypervisorType);
        _ATTRIBUTE(hypervisor, "capability", hypervisorCapabilityTypeNames[instance->hypervisorCapability]);
        snprintf(bitness, 4, "%d", instance->hypervisorBitness);
        _ATTRIBUTE(hypervisor, "bitness", bitness);
        _ATTRIBUTE(hypervisor, "requiresDisk", (instance->combinePartitions ? "true" : "false"));

        //! backing specification (@todo maybe expand this with device maps or whatnot?)
        backing = xmlNewChild(instanceNode, NULL, BAD_CAST "backing", NULL);
        root = xmlNewChild(backing, NULL, BAD_CAST "root", NULL);
        if (instance->params.root != NULL) {
            _ATTRIBUTE(root, "type", ncResourceTypeNames[instance->params.root->type]);
        } else {
            _ATTRIBUTE(root, "type", "unknown");    // for when gen_instance_xml is called with instance struct that hasn't been initialized
        }

        _ELEMENT(instanceNode, "name", instance->instanceId);
        _ELEMENT(instanceNode, "uuid", instance->uuid);
        _ELEMENT(instanceNode, "reservation", instance->reservationId);
        _ELEMENT(instanceNode, "user", instance->userId);
        _ELEMENT(instanceNode, "owner", instance->ownerId);
        _ELEMENT(instanceNode, "account", instance->accountId);
        _ELEMENT(instanceNode, "imageId", instance->imageId);   // may be unused
        _ELEMENT(instanceNode, "kernelId", instance->kernelId); // may be unused
        _ELEMENT(instanceNode, "ramdiskId", instance->ramdiskId);   // may be unused
        _ELEMENT(instanceNode, "dnsName", instance->dnsName);
        _ELEMENT(instanceNode, "privateDnsName", instance->privateDnsName);
        _ELEMENT(instanceNode, "instancePath", instance->instancePath);

        if (instance->params.kernel) {
            path = instance->params.kernel->backingPath;
            if (path_check(path, "kernel"))
                goto free;             // sanity check
            _ELEMENT(instanceNode, "kernel", path);
        }

        if (instance->params.ramdisk) {
            path = instance->params.ramdisk->backingPath;
            if (path_check(path, "ramdisk"))
                goto free;             // sanity check
            _ELEMENT(instanceNode, "ramdisk", path);
        }

        _ELEMENT(instanceNode, "xmlFilePath", instance->xmlFilePath);
        _ELEMENT(instanceNode, "libvirtFilePath", instance->libvirtFilePath);
        _ELEMENT(instanceNode, "consoleLogPath", instance->consoleFilePath);
        _ELEMENT(instanceNode, "userData", instance->userData);
        _ELEMENT(instanceNode, "launchIndex", instance->launchIndex);

        snprintf(cores_s, sizeof(cores_s), "%d", instance->params.cores);
        _ELEMENT(instanceNode, "cores", cores_s);
        snprintf(memory_s, sizeof(memory_s), "%d", instance->params.mem * 1024);
        _ELEMENT(instanceNode, "memoryKB", memory_s);
        snprintf(disk_s, sizeof(disk_s), "%d", instance->params.disk);
        _ELEMENT(instanceNode, "diskGB", disk_s);
        _ELEMENT(instanceNode, "VmType", instance->params.name);
        _ELEMENT(instanceNode, "NicType", libvirtNicTypeNames[instance->params.nicType]);
        _ELEMENT(instanceNode, "NicDevice", instance->params.guestNicDeviceName);

        // SSH-key related
        key = _NODE(instanceNode, "key");
        _ATTRIBUTE(key, "doInjectKey", _BOOL(instance->do_inject_key));
        _ATTRIBUTE(key, "sshKey", instance->keyName);

        // OS-related specs
        os = _NODE(instanceNode, "os");
        _ATTRIBUTE(os, "platform", instance->platform);
        _ATTRIBUTE(os, "virtioRoot", _BOOL(config_use_virtio_root));
        _ATTRIBUTE(os, "virtioDisk", _BOOL(config_use_virtio_disk));
        _ATTRIBUTE(os, "virtioNetwork", _BOOL(config_use_virtio_net));

        // Network groups assigned to the instance
        groupNames = _NODE(instanceNode, "groupNames");
        for (i = 0; i < instance->groupNamesSize; i++) {
            _ELEMENT(groupNames, "name", instance->groupNames[i]);
        }

        // disks specification
        disks = _NODE(instanceNode, "disks");
        _ELEMENT(disks, "floppyPath", instance->floppyFilePath);

        vbrs = _NODE(instanceNode, "vbrs");

        // the first disk should be the root disk (at least for Windows)
        for (j = 1; j >= 0; j--) {
            for (i = 0; ((i < EUCA_MAX_VBRS) && (i < instance->params.virtualBootRecordLen)); i++) {
                vbr = &(instance->params.virtualBootRecord[i]);

                // skip empty entries, if any
                if (vbr == NULL)
                    continue;

                // on the first iteration, write all VBRs into their own section
                if (j)
                    write_vbr_xml(vbrs, vbr);

                // do EMI on the first iteration of the outer loop
                if (j && vbr->type != NC_RESOURCE_IMAGE)
                    continue;

                // ignore EMI on the second iteration of the outer loop
                if (!j && vbr->type == NC_RESOURCE_IMAGE)
                    continue;

                // skip anything without a device on the guest, e.g., kernel and ramdisk
                if (!strcmp("none", vbr->guestDeviceName))
                    continue;

                // for Linux instances on Xen, partitions can be used directly, so disks can be skipped unless booting from EBS
                if (strstr(instance->platform, "linux") && strstr(instance->hypervisorType, "xen")) {
                    if ((vbr->partitionNumber == 0) && (vbr->type == NC_RESOURCE_IMAGE)) {
                        continue;
                    }
                } else {               // on all other os + hypervisor combinations, disks are used, so partitions must be skipped
                    if (vbr->partitionNumber > 0) {
                        continue;
                    }
                }

                disk = _ELEMENT(disks, "diskPath", vbr->backingPath);
                _ATTRIBUTE(disk, "targetDeviceType", libvirtDevTypeNames[vbr->guestDeviceType]);
                _ATTRIBUTE(disk, "targetDeviceName", vbr->guestDeviceName);
                snprintf(devstr, SMALL_CHAR_BUFFER_SIZE, "%s", vbr->guestDeviceName);
                if (config_use_virtio_root) {
                    devstr[0] = 'v';
                    _ATTRIBUTE(disk, "targetDeviceNameVirtio", devstr);
                    _ATTRIBUTE(disk, "targetDeviceBusVirtio", "virtio");
                }
                _ATTRIBUTE(disk, "targetDeviceBus", libvirtBusTypeNames[vbr->guestDeviceBus]);
                _ATTRIBUTE(disk, "sourceType", libvirtSourceTypeNames[vbr->backingType]);

                if (j) {
                    rootNode = _ELEMENT(disks, "root", NULL);
                    _ATTRIBUTE(rootNode, "device", devstr);
                    if (get_blkid(vbr->backingPath, root_uuid, sizeof(root_uuid)) == 0) {
                        assert(strlen(root_uuid));
                        _ATTRIBUTE(rootNode, "uuid", root_uuid);
                    }
                }
            }
        }

        {                              // record volumes
            vols = _NODE(instanceNode, "volumes");

            for (int i = 0; i < EUCA_MAX_VOLUMES; i++) {
                const ncVolume *v = instance->volumes + i;
                if (strlen(v->volumeId) == 0)   // empty slot
                    continue;
                xmlNodePtr vol = _NODE(vols, "volume");
                _ELEMENT(vol, "id", v->volumeId);
                _ELEMENT(vol, "attachmentToken", v->attachmentToken);
                _ELEMENT(vol, "localDev", v->localDev);
                _ELEMENT(vol, "localDevReal", v->localDevReal);
                _ELEMENT(vol, "stateName", v->stateName);
                _ELEMENT(vol, "connectionString", v->connectionString);
            }
        }

        if (instance->params.nicType != NIC_TYPE_NONE) {    // NIC specification
            char str[10];

            nics = _NODE(instanceNode, "nics");
            nic = _NODE(nics, "nic");
            snprintf(str, sizeof(str), "%d", instance->ncnet.vlan);
            _ATTRIBUTE(nic, "vlan", str);
            snprintf(str, sizeof(str), "%d", instance->ncnet.networkIndex);
            _ATTRIBUTE(nic, "networkIndex", str);
            _ATTRIBUTE(nic, "mac", instance->ncnet.privateMac);
            _ATTRIBUTE(nic, "publicIp", instance->ncnet.publicIp);
            _ATTRIBUTE(nic, "privateIp", instance->ncnet.privateIp);
            _ATTRIBUTE(nic, "bridgeDeviceName", instance->params.guestNicDeviceName);
        }

        {                              // set /instance/states
            char str[10];

            xmlNodePtr states = _NODE(instanceNode, "states");
            snprintf(str, sizeof(str), "%d", instance->retries);
            _ELEMENT(states, "retries", str);
            _ELEMENT(states, "stateName", instance->stateName);
            _ELEMENT(states, "bundleTaskStateName", instance->bundleTaskStateName);
            _ELEMENT(states, "createImageTaskStateName", instance->createImageTaskStateName);
            snprintf(str, sizeof(str), "%d", instance->stateCode);
            _ELEMENT(states, "stateCode", str);
            _ELEMENT(states, "state", instance_state_names[instance->state]);
            _ELEMENT(states, "bundleTaskState", bundling_progress_names[instance->bundleTaskState]);
            snprintf(str, sizeof(str), "%d", instance->bundlePid);
            _ELEMENT(states, "bundlePid", str);
            _ELEMENT(states, "bundleBucketExists", (instance->bundleBucketExists) ? ("true") : ("false"));
            _ELEMENT(states, "bundleCanceled", (instance->bundleCanceled) ? ("true") : ("false"));
            _ELEMENT(states, "guestStateName", instance->guestStateName);
            _ELEMENT(states, "isStopRequested", (instance->stop_requested) ? ("true") : ("false"));
            _ELEMENT(states, "createImageTaskState", createImage_progress_names[instance->createImageTaskState]);
            snprintf(str, sizeof(str), "%d", instance->createImagePid);
            _ELEMENT(states, "createImagePid", str);
            _ELEMENT(states, "createImageCanceled", (instance->createImageCanceled) ? ("true") : ("false"));
            _ELEMENT(states, "migrationState", migration_state_names[instance->migration_state]);
            _ELEMENT(states, "migrationSource", instance->migration_src);
            _ELEMENT(states, "migrationDestination", instance->migration_dst);
            _ELEMENT(states, "migrationCredentials", instance->migration_credentials);
        }

        {                              // set /instance/timestamps
            char str[10];

            xmlNodePtr ts = _NODE(instanceNode, "timestamps");
            snprintf(str, sizeof(str), "%d", instance->launchTime);
            _ELEMENT(ts, "launchTime", str);
            snprintf(str, sizeof(str), "%d", instance->expiryTime);
            _ELEMENT(ts, "expiryTime", str);
            snprintf(str, sizeof(str), "%d", instance->bootTime);
            _ELEMENT(ts, "bootTime", str);
            snprintf(str, sizeof(str), "%d", instance->bundlingTime);
            _ELEMENT(ts, "bundlingTime", str);
            snprintf(str, sizeof(str), "%d", instance->createImageTime);
            _ELEMENT(ts, "createImageTime", str);
            snprintf(str, sizeof(str), "%d", instance->terminationTime);
            _ELEMENT(ts, "terminationTime", str);
            snprintf(str, sizeof(str), "%d", instance->migrationTime);
            _ELEMENT(ts, "migrationTime", str);
        }

        ret = write_xml_file(doc, instance->instanceId, instance->xmlFilePath, "instance");

free:
        xmlFreeDoc(doc);
    }
    pthread_mutex_unlock(&xml_mutex);
    return (ret);
}

//!
//! Read instance information from an XML content
//!
//! @param[in] xml_path path to the XML content
//! @param[in] instance pointer to the instance structure to fill
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int read_instance_xml(const char *xml_path, ncInstance * instance)
{
    int ret = EUCA_OK;

    euca_strncpy(instance->xmlFilePath, xml_path, sizeof(instance->xmlFilePath));

    XGET_STR("/instance/hypervisor/@type", instance->hypervisorType);
    XGET_ENUM("/instance/hypervisor/@capability", instance->hypervisorCapability, hypervisorCapabilityType_from_string);
    XGET_INT("/instance/hypervisor/@bitness", instance->hypervisorBitness);
    XGET_BOOL("/instance/hypervisor/@requiresDisk", instance->combinePartitions);
    XGET_STR("/instance/name", instance->instanceId);
    XGET_STR("/instance/uuid", instance->uuid);
    XGET_STR("/instance/reservation", instance->reservationId);
    XGET_STR("/instance/user", instance->userId);
    XGET_STR("/instance/owner", instance->ownerId);
    XGET_STR("/instance/account", instance->accountId);
    XGET_STR("/instance/imageId", instance->imageId);
    XGET_STR("/instance/kernelId", instance->kernelId);
    XGET_STR("/instance/ramdiskId", instance->ramdiskId);
    XGET_STR("/instance/dnsName", instance->dnsName);
    XGET_STR("/instance/privateDnsName", instance->privateDnsName);
    XGET_STR("/instance/instancePath", instance->instancePath);
    XGET_STR("/instance/xmlFilePath", instance->xmlFilePath);
    XGET_STR("/instance/libvirtFilePath", instance->libvirtFilePath);
    XGET_STR("/instance/consoleLogPath", instance->consoleFilePath);
    XGET_STR("/instance/disks/floppyPath", instance->floppyFilePath);
    XGET_STR("/instance/userData", instance->userData);
    XGET_STR("/instance/launchIndex", instance->launchIndex);

    {                                  // pull out groupNames
        char **res_array = NULL;

        if ((res_array = get_xpath_content(xml_path, "/instance/groupNames/name")) != NULL) {
            for (int i = 0; (res_array[i] != NULL) && (i < EUCA_MAX_GROUPS); i++) {
                char *groupName = instance->groupNames[i];
                euca_strncpy(groupName, res_array[i], CHAR_BUFFER_SIZE);
                instance->groupNamesSize++;
                EUCA_FREE(res_array[i]);
            }
            EUCA_FREE(res_array);
        }
    }

    //! @todo combine these into 'params' in XML?
    XGET_INT("/instance/cores", instance->params.cores);
    XGET_INT("/instance/memoryKB", instance->params.mem);
    instance->params.mem /= 1024;      // convert from KB to MB
    XGET_INT("/instance/diskGB", instance->params.disk);
    XGET_STR("/instance/VmType", instance->params.name);
    XGET_ENUM("/instance/NicType", instance->params.nicType, libvirtNicType_from_string);
    XGET_STR("/instance/NicDevice", instance->params.guestNicDeviceName);
    XGET_BOOL("/instance/key/@doInjectKey", instance->do_inject_key);
    XGET_STR("/instance/key/@sshKey", instance->keyName);
    XGET_STR("/instance/os/@platform", instance->platform);
    //! @todo do we want to pull out 'virtio{Root|Disk|Network}' values from the XML?

    //! Various temporary state information
    XGET_INT("/instance/states/retries", instance->retries);
    XGET_STR("/instance/states/stateName", instance->stateName);
    XGET_STR("/instance/states/bundleTaskStateName", instance->bundleTaskStateName);
    XGET_STR("/instance/states/createImageTaskStateName", instance->createImageTaskStateName);
    XGET_INT("/instance/states/stateCode", instance->stateCode);
    XGET_ENUM("/instance/states/state", instance->state, instance_state_from_string);
    XGET_ENUM("/instance/states/bundleTaskState", instance->bundleTaskState, bundling_progress_from_string);
    XGET_INT("/instance/states/bundlePid", instance->bundlePid);
    XGET_BOOL("/instance/states/bundleBucketExists", instance->bundleBucketExists);
    XGET_BOOL("/instance/states/bundleCanceled", instance->bundleCanceled);
    XGET_STR("/instance/states/guestStateName", instance->guestStateName);
    XGET_BOOL("/instance/states/isStopRequested", instance->stop_requested);
    XGET_ENUM("/instance/states/createImageTaskState", instance->createImageTaskState, createImage_progress_from_string);
    XGET_INT("/instance/states/createImagePid", instance->createImagePid);
    XGET_BOOL("/instance/states/createImageCanceled", instance->createImageCanceled);
    XGET_ENUM("/instance/states/migrationState", instance->migration_state, migration_state_from_string);
    XGET_STR("/instance/states/migrationSource", instance->migration_src);
    XGET_STR("/instance/states/migrationDestination", instance->migration_dst);
    XGET_STR("/instance/states/migrationCredentials", instance->migration_credentials);

    // timestamps
    XGET_INT("/instance/timestamps/launchTime", instance->launchTime);
    XGET_INT("/instance/timestamps/expiryTime", instance->expiryTime);
    XGET_INT("/instance/timestamps/bootTime", instance->bootTime);
    XGET_INT("/instance/timestamps/bundlingTime", instance->bundlingTime);
    XGET_INT("/instance/timestamps/createImageTime", instance->createImageTime);
    XGET_INT("/instance/timestamps/terminationTime", instance->terminationTime);
    XGET_INT("/instance/timestamps/migrationTime", instance->migrationTime);

    {                                  // loop through VBRs
        char **res_array = NULL;
        virtualBootRecord *vbr = NULL;

        if ((res_array = get_xpath_content(xml_path, "/instance/vbrs/vbr")) != NULL) {
            for (int i = 0; (res_array[i] != NULL) && (i < EUCA_MAX_VBRS); i++) {
                vbr = &(instance->params.virtualBootRecord[i]);
                char vbrxpath[128];
#define MKVBRPATH(SUFFIX) snprintf(vbrxpath, sizeof(vbrxpath), "/instance/vbrs/vbr[%d]/%s\n", (i + 1), SUFFIX);

                MKVBRPATH("resourceLocation");
                XGET_STR(vbrxpath, vbr->resourceLocation);
                MKVBRPATH("guestDeviceName");
                XGET_STR(vbrxpath, vbr->guestDeviceName);
                MKVBRPATH("sizeBytes");
                XGET_INT(vbrxpath, vbr->sizeBytes);
                MKVBRPATH("formatName");
                XGET_STR(vbrxpath, vbr->formatName);
                MKVBRPATH("id");
                XGET_STR(vbrxpath, vbr->id);
                MKVBRPATH("typeName");
                XGET_STR(vbrxpath, vbr->typeName);

                MKVBRPATH("type");
                XGET_ENUM(vbrxpath, vbr->type, ncResourceType_from_string);
                MKVBRPATH("locationType");
                XGET_ENUM(vbrxpath, vbr->locationType, ncResourceLocationType_from_string);
                MKVBRPATH("format");
                XGET_ENUM(vbrxpath, vbr->format, ncResourceFormatType_from_string);
                MKVBRPATH("diskNumber");
                XGET_INT(vbrxpath, vbr->diskNumber);
                MKVBRPATH("partitionNumber");
                XGET_INT(vbrxpath, vbr->partitionNumber);
                MKVBRPATH("guestDeviceType");
                XGET_ENUM(vbrxpath, vbr->guestDeviceType, libvirtDevType_from_string);
                MKVBRPATH("guestDeviceBus");
                XGET_ENUM(vbrxpath, vbr->guestDeviceBus, libvirtBusType_from_string);
                MKVBRPATH("backingType");
                XGET_ENUM(vbrxpath, vbr->backingType, libvirtSourceType_from_string);
                MKVBRPATH("backingPath");
                XGET_STR(vbrxpath, vbr->backingPath);
                MKVBRPATH("preparedResourceLocation");
                XGET_STR(vbrxpath, vbr->preparedResourceLocation);

                // set pointers in the VBR
                if (strcmp(vbr->typeName, "machine") == 0 || (strcmp(vbr->typeName, "ebs") == 0 && vbr->diskNumber == 0 && vbr->partitionNumber == 0)) {
                    instance->params.root = vbr;
                }
                if (strcmp(vbr->typeName, "kernel") == 0) {
                    instance->params.kernel = vbr;
                }
                if (strcmp(vbr->typeName, "ramdisk") == 0) {
                    instance->params.ramdisk = vbr;
                }
                instance->params.virtualBootRecordLen++;

                LOGTRACE("found vbr '%s' typeName='%s' instance->params.virtualBootRecordLen\n", vbr->resourceLocation, vbr->typeName);
                EUCA_FREE(res_array[i]);
            }
            EUCA_FREE(res_array);
        }
    }

    {                                  // pull out volumes
        char **res_array = NULL;

        if ((res_array = get_xpath_content(xml_path, "/instance/volumes/volume")) != NULL) {
            for (int i = 0; (res_array[i] != NULL) && (i < EUCA_MAX_VOLUMES); i++) {
                ncVolume *v = instance->volumes + i;
                char volxpath[128];
#define MKVOLPATH(SUFFIX) snprintf(volxpath, sizeof(volxpath), "/instance/volumes/volume[%d]/%s\n", (i + 1), SUFFIX);
                MKVOLPATH("id");
                XGET_STR(volxpath, v->volumeId);
                MKVOLPATH("attachmentToken");
                XGET_STR(volxpath, v->attachmentToken);
                MKVOLPATH("localDev");
                XGET_STR(volxpath, v->localDev);
                MKVOLPATH("localDevReal");
                XGET_STR(volxpath, v->localDevReal);
                MKVOLPATH("stateName");
                XGET_STR(volxpath, v->stateName);
                MKVOLPATH("connectionString");
                XGET_STR(volxpath, v->connectionString);
                EUCA_FREE(res_array[i]);
            }
            EUCA_FREE(res_array);
        }
    }

    // get NIC information
    XGET_INT("/instance/nics/nic/@vlan", instance->ncnet.vlan);
    XGET_INT("/instance/nics/nic/@networkIndex", instance->ncnet.networkIndex);
    XGET_STR("/instance/nics/nic/@mac", instance->ncnet.privateMac);
    XGET_STR("/instance/nics/nic/@publicIp", instance->ncnet.publicIp);
    XGET_STR("/instance/nics/nic/@privateIp", instance->ncnet.privateIp);
    XGET_STR("/instance/nics/nic/@bridgeDeviceName", instance->params.guestNicDeviceName);
    if (strcmp(instance->platform, "windows") == 0) {
        instance->params.nicType = NIC_TYPE_WINDOWS;
    } else {
        instance->params.nicType = NIC_TYPE_LINUX;
    }

    if (instance->params.root == NULL) {
        LOGERROR("did not find 'root' among disks in %s\n", xml_path);
        return (EUCA_ERROR);
    }

    /* note that the following XML may not always be present:
     *
     *   /instance/kernel - not set for HVM/Windows instances
     *   /instance/ramdisk - not set for HVM/Windows instances
     *   /instance/backing/root/@type - not set for EBS instances
     *
     * these ones we don't bother reading:
     *
     * /instance/disks - we get same info from volumes[] and vbrs[]
     */

    return (ret);
}

//!
//! Given a file with instance metadata in XML (instance->xmlFilePath)
//! and an XSL-T stylesheet, produces XML document suitable for libvirt
//!
//! @param[in] instance a pointer to the instance structure
//!
//! @return The error code from the call of apply_xslt_stylesheet()
//!
//! @see apply_xslt_stylesheet()
//!
int gen_libvirt_instance_xml(const ncInstance * instance)
{
    int ret = 0;
    INIT();

    pthread_mutex_lock(&xml_mutex);
    {
        ret = apply_xslt_stylesheet(xslt_path, instance->xmlFilePath, instance->libvirtFilePath, NULL, 0);
    }
    pthread_mutex_unlock(&xml_mutex);
    return (ret);
}

//!
//! Gets called from XSLT/XML2 library, possibly several times per error.
//! This handler concatenates the error pieces together and outputs a line,
//! either when a newlines is seen or when the internal buffer is overrun.
//! Needless to say, this function is not thread safe.
//!
//! @param[in] ctx a transparent pointer (UNUSED)
//! @param[in] fmt a format string
//! @param[in] ... the variable argument part of the format string
//!
static void error_handler(void *ctx, const char *fmt, ...)
{
    int i = 0;
    int old_size = 0;
    va_list ap = { {0} };
    static int size = 0;
    static char buf[512] = "";

    old_size = size;

    va_start(ap, fmt);
    size += vsnprintf(buf + size, sizeof(buf) - size, fmt, ap);
    va_end(ap);

    for (i = old_size; i < sizeof(buf); i++) {
        if ((buf[i] == '\n') || (i == (sizeof(buf) - 1))) {
            size = 0;
            buf[i] = '\0';
            LOGTRACE("ERROR from XML2/XSLT {%s}\n", buf);
        }

        if (buf[i] == '\0') {
            break;
        }
    }
}

//!
//! Processes input XML file (e.g., instance metadata) into output XML file or string (e.g., for libvirt)
//! using XSL-T specification file (e.g., libvirt.xsl)
//!
//! @param[in]  xsltStylesheetPath a string containing the path to the XSLT Stylesheet
//! @param[in]  inputXmlPath a string containing the path of the input XML document
//! @param[in]  outputXmlPath a string containing the path of the output XML document
//! @param[out] outputXmlBuffer a string that will contain the output XML data if non NULL and non-0 length.
//! @param[in]  outputXmlBufferSize the length of outputXmlBuffer
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include EUCA_ERROR and EUCA_IO_ERROR.
//!
static int apply_xslt_stylesheet(const char *xsltStylesheetPath, const char *inputXmlPath, const char *outputXmlPath, char *outputXmlBuffer, int outputXmlBufferSize)
{
    int err = EUCA_OK;
    int i = 0;
    int j = 0;
    int bytes = 0;
    int buf_size = 0;
    char c = '\0';
    FILE *fp = NULL;
    xmlChar *buf = NULL;
    boolean applied_ok = FALSE;
    xmlDocPtr doc = NULL;
    xsltStylesheetPtr cur = NULL;
    xsltTransformContextPtr ctxt = NULL;
    xmlDocPtr res = NULL;

    INIT();
    if ((cur = xsltParseStylesheetFile((const xmlChar *)xsltStylesheetPath)) != NULL) {
        if ((doc = xmlParseFile(inputXmlPath)) != NULL) {
            ctxt = xsltNewTransformContext(cur, doc);   // need context to get result
            xsltSetCtxtParseOptions(ctxt, 0);   //! @todo do we want any XSL-T parsing options?

            res = xsltApplyStylesheetUser(cur, doc, NULL, NULL, NULL, ctxt);    // applies XSLT to XML
            applied_ok = ((ctxt->state == XSLT_STATE_OK) ? TRUE : FALSE);   // errors are communicated via ctxt->state
            xsltFreeTransformContext(ctxt);

            if (res && applied_ok) {
                // save to a file, if path was provied
                if (outputXmlPath != NULL) {
                    if ((fp = fopen(outputXmlPath, "w")) != NULL) {
                        if ((bytes = xsltSaveResultToFile(fp, res, cur)) == -1) {
                            LOGERROR("failed to save XML document to %s\n", outputXmlPath);
                            err = EUCA_IO_ERROR;
                        }
                        fclose(fp);
                    } else {
                        LOGERROR("failed to create file %s\n", outputXmlPath);
                        err = EUCA_IO_ERROR;
                    }
                }
                // convert to an ASCII buffer, if such was provided
                if (err == EUCA_OK && outputXmlBuffer != NULL && outputXmlBufferSize > 0) {
                    if (xsltSaveResultToString(&buf, &buf_size, res, cur) == 0) {
                        // success
                        if (buf_size < outputXmlBufferSize) {
                            bzero(outputXmlBuffer, outputXmlBufferSize);
                            for (i = 0, j = 0; i < buf_size; i++) {
                                c = ((char)buf[i]);
                                if (c != '\n')  // remove newlines
                                    outputXmlBuffer[j++] = c;
                            }
                        } else {
                            LOGERROR("XML string buffer is too small (%d > %d)\n", buf_size, outputXmlBufferSize);
                            err = EUCA_ERROR;
                        }
                        xmlFree(buf);
                    } else {
                        LOGERROR("failed to save XML document to a string\n");
                        err = EUCA_ERROR;
                    }
                }
            } else {
                LOGERROR("failed to apply stylesheet %s to %s\n", xsltStylesheetPath, inputXmlPath);
                err = EUCA_ERROR;
            }
            if (res != NULL)
                xmlFreeDoc(res);
            xmlFreeDoc(doc);
        } else {
            LOGERROR("failed to parse XML document %s\n", inputXmlPath);
            err = EUCA_ERROR;
        }
        xsltFreeStylesheet(cur);
    } else {
        LOGERROR("failed to open and parse XSL-T stylesheet file %s\n", xsltStylesheetPath);
        err = EUCA_IO_ERROR;
    }

    return (err);
}

//!
//! Generates the XML content for a given volume
//!
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] instance a pointer to our instance structure
//! @param[in] localDevReal a string containing the target device name
//! @param[in] remoteDev a string containing the disk path.
//!
//! @return The results of calling write_xml_file()
//!
//! @see write_xml_file()
//!
int gen_volume_xml(const char *volumeId, const ncInstance * instance, const char *localDevReal, const char *remoteDev)
{
    int ret = EUCA_ERROR;
    char bitness[4] = "";
    char path[EUCA_MAX_PATH] = "";
    xmlDocPtr doc = NULL;
    xmlNodePtr volumeNode = NULL;
    xmlNodePtr hypervisor = NULL;
    xmlNodePtr os = NULL;
    xmlNodePtr backing = NULL;
    xmlNodePtr root = NULL;
    xmlNodePtr disk = NULL;

    INIT();

    pthread_mutex_lock(&xml_mutex);
    {
        doc = xmlNewDoc(BAD_CAST "1.0");
        volumeNode = xmlNewNode(NULL, BAD_CAST "volume");
        xmlDocSetRootElement(doc, volumeNode);

        // hypervisor-related specs
        hypervisor = xmlNewChild(volumeNode, NULL, BAD_CAST "hypervisor", NULL);
        _ATTRIBUTE(hypervisor, "type", instance->hypervisorType);
        _ATTRIBUTE(hypervisor, "capability", hypervisorCapabilityTypeNames[instance->hypervisorCapability]);
        snprintf(bitness, 4, "%d", instance->hypervisorBitness);
        _ATTRIBUTE(hypervisor, "bitness", bitness);

        _ELEMENT(volumeNode, "id", volumeId);
        _ELEMENT(volumeNode, "user", instance->userId);
        _ELEMENT(volumeNode, "instancePath", instance->instancePath);

        // OS-related specs
        os = _NODE(volumeNode, "os");
        _ATTRIBUTE(os, "platform", instance->platform);
        _ATTRIBUTE(os, "virtioRoot", _BOOL(config_use_virtio_root));
        _ATTRIBUTE(os, "virtioDisk", _BOOL(config_use_virtio_disk));
        _ATTRIBUTE(os, "virtioNetwork", _BOOL(config_use_virtio_net));

        //! backing specification (@todo maybe expand this with device maps or whatnot?)
        backing = xmlNewChild(volumeNode, NULL, BAD_CAST "backing", NULL);
        root = xmlNewChild(backing, NULL, BAD_CAST "root", NULL);
        assert(instance->params.root);
        _ATTRIBUTE(root, "type", ncResourceTypeNames[instance->params.root->type]);

        // volume information
        disk = _ELEMENT(volumeNode, "diskPath", remoteDev);
        _ATTRIBUTE(disk, "targetDeviceType", "disk");
        _ATTRIBUTE(disk, "targetDeviceName", localDevReal);
        _ATTRIBUTE(disk, "targetDeviceBus", "scsi");
        _ATTRIBUTE(disk, "sourceType", "block");
        char serial[64];
        snprintf(serial, sizeof(serial), "%s-dev-%s", volumeId, localDevReal);
        _ATTRIBUTE(disk, "serial", serial);

        snprintf(path, sizeof(path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);
        ret = write_xml_file(doc, instance->instanceId, path, "volume");
        xmlFreeDoc(doc);
    }
    pthread_mutex_unlock(&xml_mutex);
    return (ret);
}

//!
//! Generate volume XML content for LIBVIRT
//!
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] instance a pointer to our instance structure.
//!
//! @return The results of calling apply_xslt_stylesheet()
//!
//! @see apply_xslt_stylesheet()
//!
int gen_libvirt_volume_xml(const char *volumeId, const ncInstance * instance)
{
    int ret = EUCA_OK;
    char path[EUCA_MAX_PATH] = "";
    char lpath[EUCA_MAX_PATH] = "";

    INIT();

    snprintf(path, sizeof(path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);  // vol-XXX.xml
    snprintf(lpath, sizeof(lpath), EUCALYPTUS_VOLUME_LIBVIRT_XML_PATH_FORMAT, instance->instancePath, volumeId);    // vol-XXX-libvirt.xml

    pthread_mutex_lock(&xml_mutex);
    {
        ret = apply_xslt_stylesheet(xslt_path, path, lpath, NULL, 0);
    }
    pthread_mutex_unlock(&xml_mutex);

    return (ret);
}

//!
//! Returns text content of results of an xpath query as a NULL-terminated array of string pointers, which
//! the caller must free. (To be useful, the query should point to an element that does not have any children.)
//!
//! @param[in] xml_path a string containing the path to the XML file to parse
//! @param[in] xpath a string contianing the XPATH expression to evaluate
//!
//! @return a pointer to a list of strings (strings and array must be freed by caller)
//!
char **get_xpath_content(const char *xml_path, const char *xpath)
{
    int i = 0;
    char **res = NULL;
    xmlChar *val = NULL;
    xmlDocPtr doc = NULL;
    xmlXPathContextPtr context = NULL;
    xmlXPathObjectPtr result = NULL;
    xmlNodeSetPtr nodeset = NULL;

    INIT();

    LOGTRACE("searching for '%s' in '%s'\n", xpath, xml_path);
    pthread_mutex_lock(&xml_mutex);
    {
        if ((doc = xmlParseFile(xml_path)) != NULL) {
            if ((context = xmlXPathNewContext(doc)) != NULL) {
                if ((result = xmlXPathEvalExpression(((const xmlChar *)xpath), context)) != NULL) {
                    if (!xmlXPathNodeSetIsEmpty(result->nodesetval)) {
                        nodeset = result->nodesetval;
                        res = EUCA_ZALLOC(nodeset->nodeNr + 1, sizeof(char *));
                        for (i = 0; ((i < nodeset->nodeNr) && (res != NULL)); i++) {
                            if ((nodeset->nodeTab[i]->children != NULL) && (nodeset->nodeTab[i]->children->content != NULL)) {
                                val = nodeset->nodeTab[i]->children->content;
                                res[i] = strdup(((char *)val));
                            } else {
                                res[i] = strdup("");    // when 'children' pointer is NULL, the XML element exists, but is empty
                            }
                        }
                    }
                    xmlXPathFreeObject(result);
                } else {
                    LOGERROR("no results for '%s' in '%s'\n", xpath, xml_path);
                }
                xmlXPathFreeContext(context);
            } else {
                LOGERROR("failed to set xpath '%s' context for '%s'\n", xpath, xml_path);
            }
            xmlFreeDoc(doc);
        } else {
            LOGDEBUG("failed to parse XML in '%s'\n", xml_path);
        }
    }
    pthread_mutex_unlock(&xml_mutex);
    return (res);
}

//!
//! Returns text content of the N-th result of an xpath query.
//!
//! If a buffer was provided, the result is copied into it and the pointer to the
//! buffer is returned. If the buffer is NULL, the result is returned in a string
//! that the caller must free. If there were no values or if index is out of range
//! NULL is returned.
//!
//! To be useful, the query should point to an XML element that does not have any
//! children.
//!
//! @param[in] xml_path a string containing the path to the XML file to parse
//! @param[in] xpath a string contianing the XPATH expression to evaluate
//! @param[in] index of the value to retrieve
//! @param[in] buf string buffer to copy value into (if NULL, a malloced value is returned)
//! @param[in] buf_len length of the buffer (irrelevant if buf is NULL)
//!
//! @return a pointer to a string with the first result (must be freed by caller) or to buf, if specified
//!
char *get_xpath_content_at(const char *xml_path, const char *xpath, int index, char *buf, int buf_len)
{
    char **res_array = NULL;
    char *res = NULL;

    if ((res_array = get_xpath_content(xml_path, xpath)) != NULL) {
        for (int i = 0; res_array[i]; i++) {
            if (i == index) {
                if (buf != NULL) {
                    strncpy(buf, res_array[i], buf_len);
                    EUCA_FREE(res_array[i]);
                    res = buf;
                } else {
                    res = res_array[i]; // caller has to free res_array[i]
                }
            } else {
                EUCA_FREE(res_array[i]);    // we always free all other results
            }
        }
        EUCA_FREE(res_array);
    }

    return (res);
}

#ifdef __STANDALONE
static void add_dummy_vbr(xmlNodePtr parent,
                          char *resourceLocation,
                          char *guestDeviceName,
                          char *sizeBytes,
                          char *formatName,
                          char *id,
                          char *typeName,
                          char *type,
                          char *locationType,
                          char *format,
                          char *diskNumber,
                          char *partitionNumber, char *guestDeviceType, char *guestDeviceBus, char *backingType, char *backingPath, char *preparedResourceLocation)
{
    xmlNodePtr vbr = _NODE(parent, "vbr");
    _ELEMENT(vbr, "resourceLocation", resourceLocation);
    _ELEMENT(vbr, "guestDeviceName", guestDeviceName);
    _ELEMENT(vbr, "sizeBytes", sizeBytes);
    _ELEMENT(vbr, "formatName", formatName);
    _ELEMENT(vbr, "id", id);
    _ELEMENT(vbr, "typeName", typeName);
    _ELEMENT(vbr, "type", type);
    _ELEMENT(vbr, "locationType", locationType);
    _ELEMENT(vbr, "format", format);
    _ELEMENT(vbr, "diskNumber", diskNumber);
    _ELEMENT(vbr, "partitionNumber", partitionNumber);
    _ELEMENT(vbr, "guestDeviceType", guestDeviceType);
    _ELEMENT(vbr, "guestDeviceBus", guestDeviceBus);
    _ELEMENT(vbr, "backingType", backingType);
    _ELEMENT(vbr, "backingPath", backingPath);
    _ELEMENT(vbr, "preparedResourceLocation", preparedResourceLocation);
}

//!
//! Create a dummy instance
//!
//! @param[in] file a string containing the file name for which to save our dummy instances
//!
static void create_dummy_instance(const char *file)
{
    xmlDocPtr doc = xmlNewDoc(BAD_CAST "1.0");
    xmlNodePtr instance = xmlNewNode(NULL, BAD_CAST "instance");
    xmlNodePtr hypervisor = NULL;
    xmlNodePtr os = NULL;
    xmlNodePtr groupNames = NULL;
    xmlNodePtr disks = NULL;
    xmlNodePtr disk1 = NULL;

    xmlDocSetRootElement(doc, instance);

    hypervisor = _NODE(instance, "hypervisor");
    _ATTRIBUTE(hypervisor, "type", "kvm");
    _ATTRIBUTE(hypervisor, "capability", "hw");
    _ATTRIBUTE(hypervisor, "bitness", "64");
    _ATTRIBUTE(hypervisor, "requiresDisk", "false");
    xmlNodePtr backing = _NODE(instance, "backing");
    xmlNodePtr root = _NODE(backing, "root");
    _ATTRIBUTE(root, "type", "image");
    _ELEMENT(instance, "name", "i-12345");
    _ELEMENT(instance, "uuid", "c2eba6c1-9c1c-439c-b6e7-efdaf5de3ccf");
    _ELEMENT(instance, "reservation", "r-46A440E3");
    _ELEMENT(instance, "user", "AID0DOKUKK8RTNPLHD2ZO");
    _ELEMENT(instance, "owner", "instance-owner-X");
    _ELEMENT(instance, "account", "instance-account-Y");
    _ELEMENT(instance, "imageId", "emi-33333");
    _ELEMENT(instance, "kernelId", "eki-22222");
    _ELEMENT(instance, "ramdiskId", "eri-11111");
    _ELEMENT(instance, "dnsName", "");
    _ELEMENT(instance, "privateDnsName", "");
    _ELEMENT(instance, "instancePath", "/var/run/instances/i-123ABC/");
    _ELEMENT(instance, "kernel", "/var/run/instances/i-123ABC/kernel");
    _ELEMENT(instance, "ramdisk", "/var/run/instances/i-123ABC/ramdisk");
    _ELEMENT(instance, "xmlFilePath", "/var/run/instances/i-123ABC/instance.xml");
    _ELEMENT(instance, "libvirtFilePath", "/var/run/instances/i-123ABC/instance-libvirt.xml");
    _ELEMENT(instance, "consoleLogPath", "/var/run/instances/i-123ABC/console.log");
    _ELEMENT(instance, "userData", "");
    _ELEMENT(instance, "launchIndex", "0");
    _ELEMENT(instance, "cores", "1");
    _ELEMENT(instance, "memoryKB", "512000");
    _ELEMENT(instance, "diskGB", "5");
    _ELEMENT(instance, "VmType", "c1.medium");
    _ELEMENT(instance, "NicType", "linux");
    _ELEMENT(instance, "NicDevice", "eth0");

    xmlNodePtr key = _NODE(instance, "key");
    _ATTRIBUTE(key, "doInjectKey", "true");
    _ATTRIBUTE(key, "sshKey", "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCoZzl6SG02DlwTJvq6Bf foo@bar");

    os = _NODE(instance, "os");
    _ATTRIBUTE(os, "platform", "linux");
    _ATTRIBUTE(os, "virtioRoot", "true");
    _ATTRIBUTE(os, "virtioDisk", "false");
    _ATTRIBUTE(os, "virtioNetwork", "true");

    groupNames = _NODE(instance, "groupNames");
    _ELEMENT(groupNames, "name", "mah-group-0");
    _ELEMENT(groupNames, "name", "mah-group-1");
    _ELEMENT(groupNames, "name", "mah-group-2");

    disks = _NODE(instance, "disks");
    disk1 = _ELEMENT(disks, "diskPath", "/var/run/instances/i-213456/root");
    _ATTRIBUTE(disk1, "targetDeviceType", "disk");
    _ATTRIBUTE(disk1, "targetDeviceName", "sda1");
    _ATTRIBUTE(disk1, "targetDeviceBus", "virtio");
    _ATTRIBUTE(disk1, "sourceType", "file");

    disk1 = _ELEMENT(disks, "diskPath", "/var/run/instances/i-213456/swap");
    _ATTRIBUTE(disk1, "targetDeviceType", "disk");
    _ATTRIBUTE(disk1, "targetDeviceName", "sda3");
    _ATTRIBUTE(disk1, "targetDeviceBus", "scsi");
    _ATTRIBUTE(disk1, "sourceType", "file");

    xmlNodePtr rootdisk = _NODE(disks, "root");
    _ATTRIBUTE(rootdisk, "device", "sda1");

    xmlNodePtr vbrs = _NODE(instance, "vbrs");
    add_dummy_vbr(vbrs, "objectstorage://buk1/initrd1", "none", "1111111", "none", "eri-11111", "ramdisk",
                  "ramdisk", "objectstorage", "none", "0", "0", "disk", "ide", "file", "/var/run/instances/i-123ABC/ramdisk", "https://objectstorage1/buk1/initrd1");
    add_dummy_vbr(vbrs, "objectstorage://buk2/kernel1", "none", "22222222", "none", "eki-22222", "kernel",
                  "kernel", "objectstorage", "none", "0", "0", "disk", "ide", "file", "/var/run/instances/i-123ABC/kernel", "https://objectstorage1/buk2/kernel1");
    add_dummy_vbr(vbrs, "objectstorage://buk3/image1", "sda", "3333333333", "ext3", "emi-33333", "machine",
                  "image", "objectstorage", "ext3", "0", "0", "disk", "scsi", "block", "/var/run/instances/i-123ABC/link-to-sda1", "https://objectstorage1/buk3/image1");

    _ELEMENT(disks, "floppyPath", "/var/run/instances/i-213456/instance.floppy");

    xmlNodePtr vols = _NODE(instance, "volumes");
    xmlNodePtr vol = _NODE(vols, "volume");
    _ELEMENT(vol, "id", "vol-123ABC");
    _ELEMENT(vol, "attachmentToken", "alskhfnoacsniusacgnoiausgnoiuascnoiaudfh");
    _ELEMENT(vol, "localDev", "/dev/sde");
    _ELEMENT(vol, "localDevReal", "/dev/sde");
    _ELEMENT(vol, "stateName", "attached");
    _ELEMENT(vol, "connectionString", "kajalksqwuyreoiquwyeroiquwyeroiqwureyroqiweuryoqwiueryioqwry");
    vol = _NODE(vols, "volume");
    _ELEMENT(vol, "id", "vol-ABC123");
    _ELEMENT(vol, "attachmentToken", "allaksjdlfkjiusacgnoiausgnoiuascnoiaudfh");
    _ELEMENT(vol, "localDev", "/dev/sdf");
    _ELEMENT(vol, "localDevReal", "/dev/sdf");
    _ELEMENT(vol, "stateName", "attaching");
    _ELEMENT(vol, "connectionString", "kakjhkjhkjhkjhkjhkyeroiquwyeroiqwureyroqiweuryoqwiueryioqwry");

    xmlNodePtr nics = _NODE(instance, "nics");
    xmlNodePtr nic = _NODE(nics, "nic");
    _ATTRIBUTE(nic, "vlan", "67");
    _ATTRIBUTE(nic, "networkIndex", "9");
    _ATTRIBUTE(nic, "mac", "D0:0D:01:6E:80:64");
    _ATTRIBUTE(nic, "publicIp", "192.168.51.51");
    _ATTRIBUTE(nic, "privateIp", "192.168.98.51");
    _ATTRIBUTE(nic, "bridgeDeviceName", "br0");

    // add dummy state info
    xmlNodePtr states = _NODE(instance, "states");
    _ELEMENT(states, "retries", "3");
    _ELEMENT(states, "stateName", "Extant");
    _ELEMENT(states, "bundleTaskStateName", "None");
    _ELEMENT(states, "createImageTaskStateName", "None");
    _ELEMENT(states, "stateCode", "4");
    _ELEMENT(states, "state", "Extant");
    _ELEMENT(states, "bundleTaskState", "none");
    _ELEMENT(states, "bundlePid", "9876");
    _ELEMENT(states, "bundleBucketExists", "true");
    _ELEMENT(states, "bundleCanceled", "false");
    _ELEMENT(states, "guestStateName", "poweredOn");
    _ELEMENT(states, "isStopRequested", "false");
    _ELEMENT(states, "createImageTaskState", "cancelled");
    _ELEMENT(states, "createImagePid", "58174");
    _ELEMENT(states, "createImageCanceled", "true");
    _ELEMENT(states, "migrationState", "ready");
    _ELEMENT(states, "migrationSource", "192.168.78.12");
    _ELEMENT(states, "migrationDestination", "192.168.78.19");
    _ELEMENT(states, "migrationCredentials", "ABCDEFGHIJKLMNOP");

    // add dummy timestamps
    xmlNodePtr ts = _NODE(instance, "timestamps");
    _ELEMENT(ts, "launchTime", "137879255");
    _ELEMENT(ts, "expiryTime", "137879256");
    _ELEMENT(ts, "bootTime", "137879257");
    _ELEMENT(ts, "bundlingTime", "137879258");
    _ELEMENT(ts, "createImageTime", "137879259");
    _ELEMENT(ts, "terminationTime", "137879260");
    _ELEMENT(ts, "migrationTime", "137879261");

    xmlSaveFormatFileEnc(file, doc, "UTF-8", 1);
    LOGINFO("wrote XML to %s\n", file);
    // cat(file);
    xmlFreeDoc(doc);
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    int err = EUCA_ERROR;
    char *in_path = NULL;
    char *out_path = NULL;
    char xml_buf[2048] = "";

    if (argc != 2) {
        LOGERROR("required parameters are <XSLT stylesheet path>\n");
        return (EUCA_ERROR);
    }

    logfile(NULL, EUCA_LOG_DEBUG, 4);

    euca_strncpy(xslt_path, argv[1], sizeof(xslt_path));
    in_path = tempnam(NULL, "xml-");
    out_path = tempnam(NULL, "xml-");
    char *out_path2 = tempnam(NULL, "xml-");

    create_dummy_instance(in_path);
    LOGINFO("wrote dummy XML to %s\n", in_path);

    ncInstance instance;
    ncInstance instance2;
    bzero(&instance, sizeof(ncInstance));
    bzero(&instance2, sizeof(ncInstance));
    if (read_instance_xml(in_path, &instance) != EUCA_OK) {
        LOGERROR("failed to read instance XML from %s\n", in_path);
        goto out;
    }
    LOGINFO("read dummy XML from %s into 'instance'\n", in_path);

    strncpy(instance.xmlFilePath, out_path2, sizeof(instance.xmlFilePath));
    if (gen_instance_xml(&instance) != EUCA_OK) {
        LOGERROR("failed to create instance XML in %s\n", out_path2);
        goto out;
    }
    LOGINFO("wrote re-generated XML to %s\n", out_path2);

    if (read_instance_xml(out_path2, &instance2) != EUCA_OK) {
        LOGERROR("failed to re-read instance XML from %s\n", out_path2);
        goto out;
    }
    LOGINFO("re-read re-generated XML from %s\n", out_path2);

    instance.params.root = NULL;
    instance.params.kernel = NULL;
    instance.params.ramdisk = NULL;
    instance2.params.root = NULL;
    instance2.params.kernel = NULL;
    instance2.params.ramdisk = NULL;
    LOGINFO("you could try: diff %s %s\n", in_path, out_path2);
    LOGINFO("name=[%s] [%s]\n", instance.params.name, instance2.params.name);
    if (memcmp(&instance, &instance2, sizeof(ncInstance)) != 0) {
        LOGERROR("instance struct saved to %s does not match instance read from %s\n", in_path, out_path2);
        goto out;
    }

    LOGINFO("parsing stylesheet %s\n", xslt_path);
    if ((err = apply_xslt_stylesheet(xslt_path, in_path, out_path, NULL, 0)) != EUCA_OK)
        goto out;

    LOGINFO("parsing stylesheet %s again\n", xslt_path);
    if ((err = apply_xslt_stylesheet(xslt_path, in_path, out_path, xml_buf, sizeof(xml_buf))) != EUCA_OK)
        goto out;

    LOGINFO("wrote XML to %s\n", out_path);
    if (strlen(xml_buf) < 1) {
        err = EUCA_ERROR;
        LOGERROR("failed to see XML in buffer\n");
        goto out;
    }
    cat(out_path);

out:
    remove(out_path);
    remove(in_path);
    EUCA_FREE(in_path);
    EUCA_FREE(out_path);
    return (err);
}
#endif /* __STANDALONE */

#ifdef __STANDALONE2
//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    int j = 0;
    char **devs = NULL;
    char *inputXmlPath = NULL;

    if (argc != 3) {
        LOGERROR("please, supply a path to an XML file and an Xpath\n");
        return (EUCA_ERROR);
    }

    inputXmlPath = argv[1];
    if ((devs = get_xpath_content(argv[1], argv[2])) != NULL) {
        for (j = 0; devs[j]; j++) {
            LOGINFO("devs[%d] = '%s'\n", j, devs[j]);
            EUCA_FREE(devs[j]);
        }
        EUCA_FREE(devs);
    } else {
        LOGERROR("devs[] are empty\n");
    }

    return (EUCA_OK);
}
#endif /* __STANDALONE2 */
