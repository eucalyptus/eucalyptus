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
#include <string.h>             // strlen, strcpy
#include <time.h>
#include <sys/types.h>          // umask
#include <sys/stat.h>           // umask
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

#include <eucalyptus.h>
#include <eucalyptus-config.h>
#include <backing.h>            // umask
#include <data.h>
#include <misc.h>
#include <euca_string.h>

#include "handlers.h"           // nc_state_t
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

static boolean initialized = FALSE; //!< To determine if the XML library has been initialized
static boolean config_use_virtio_root = 0;  //!< Set to TRUE if we are using VIRTIO root
static boolean config_use_virtio_disk = 0;  //!< Set to TRUE if we are using VIRTIO disks
static boolean config_use_virtio_net = 0;   //!< Set to TRUE if we are using VIRTIO network
static char xslt_path[MAX_PATH];    //!< Destination path for the XSLT files
static pthread_mutex_t xml_mutex = PTHREAD_MUTEX_INITIALIZER;   //!< process-global mutex

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int gen_instance_xml(const ncInstance * instance);
int gen_libvirt_instance_xml(const ncInstance * instance);
int gen_volume_xml(const char *volumeId, const ncInstance * instance, const char *localDevReal, const char *remoteDev);
int gen_libvirt_volume_xml(const char *volumeId, const ncInstance * instance);
char **get_xpath_content(const char *xml_path, const char *xpath);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void init(struct nc_state_t *nc_state);
#if 0
// (unused for now)
static void cleanup(void);
#endif /* 0 */
static int path_check(const char *path, const char *name);
static int write_xml_file(const xmlDocPtr doc, const char *instanceId, const char *path, const char *type);

static void error_handler(void *ctx, const char *fmt, ...) __attribute__ ((__format__(__printf__, 2, 3)));;
static int apply_xslt_stylesheet(const char *xsltStylesheetPath, const char *inputXmlPath, const char *outputXmlPath, char *outputXmlBuffer,
                                 int outputXmlBufferSize);

#ifdef __STANDALONE
static void create_dummy_instance(const char *file);
int main(int argc, char **argv);
#endif /* __STANDALONE */

#ifdef __STANDALONE2
int main(int argc, char **argv)
#endif                          /* __STANDALONE2 */
/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#ifdef __STANDALONE             // if compiling as a stand-alone binary (for unit testing)
#define INIT() if (!initialized) init(NULL)
#elif __STANDALONE2
#define INIT() if (!initialized) init(NULL)
#else                           // if linking against an NC, find nc_state symbol
extern struct nc_state_t nc_state;
#define INIT() if (!initialized) init(&nc_state)
#endif

// macros for making XML construction a bit more readable
#define _NODE(P,N) xmlNewChild((P), NULL, BAD_CAST (N), NULL)
#define _ELEMENT(P,N,V) xmlNewChild((P), NULL, BAD_CAST (N), BAD_CAST (V))
#define _ATTRIBUTE(P,N,V) xmlNewProp((P), BAD_CAST (N), BAD_CAST (V))
#define _BOOL(S) ((S)?("true"):("false"))

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
static void init(struct nc_state_t *nc_state)
{
    pthread_mutex_lock(&xml_mutex);
    {
        if (!initialized) {
            xmlInitParser();
            LIBXML_TEST_VERSION;    // verifies that loaded library matches the compiled library
            xmlSubstituteEntitiesDefault(1);    // substitute entities while parsing
            xmlSetGenericErrorFunc(NULL, error_handler);    // catches errors/warnings that libxml2 writes to stderr
            xsltSetGenericErrorFunc(NULL, error_handler);   // catches errors/warnings that libslt writes to stderr
            if (nc_state != NULL) {
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
    xmlCleanupParser();         // calls xmlCleanupGlobals()
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
        logprintfl(EUCAERROR, "internal error: path to %s points to a device %s\n", name, path);
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

    chmod(path, BACKING_FILE_PERM); // ensure perms in case when XML file exists
    if ((ret = xmlSaveFormatFileEnc(path, doc, "UTF-8", 1)) > 0) {
        logprintfl(EUCADEBUG, "[%s] wrote %s XML to %s\n", instanceId, type, path);
    } else {
        logprintfl(EUCAERROR, "[%s] failed to write %s XML to %s\n", instanceId, type, path);
    }
    umask(old_umask);
    return ((ret > 0) ? (EUCA_OK) : (EUCA_ERROR));
}

//!
//! Encodes instance metadata (contained in ncInstance struct) in XML
//! and writes it to file instance->xmlFilePath (/path/to/instance/instance.xml)
//! That file gets processed through tools/libvirt.xsl (/etc/eucalyptus/libvirt.xsl)
//! to produce /path/to/instance/libvirt.xml file that is passed to libvirt create.
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
    xmlNodePtr disks = NULL;
    xmlNodePtr rootNode = NULL;
    xmlNodePtr nics = NULL;
    xmlNodePtr nic = NULL;
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
        _ATTRIBUTE(hypervisor, "capability", hypervsorCapabilityTypeNames[instance->hypervisorCapability]);
        snprintf(bitness, 4, "%d", instance->hypervisorBitness);
        _ATTRIBUTE(hypervisor, "bitness", bitness);

        //! backing specification (@todo maybe expand this with device maps or whatnot?)
        backing = xmlNewChild(instanceNode, NULL, BAD_CAST "backing", NULL);
        root = xmlNewChild(backing, NULL, BAD_CAST "root", NULL);
        assert(instance->params.root);
        _ATTRIBUTE(root, "type", ncResourceTypeName[instance->params.root->type]);

        _ELEMENT(instanceNode, "name", instance->instanceId);
        _ELEMENT(instanceNode, "uuid", instance->uuid);
        _ELEMENT(instanceNode, "reservation", instance->reservationId);
        _ELEMENT(instanceNode, "user", instance->userId);
        _ELEMENT(instanceNode, "dnsName", instance->dnsName);
        _ELEMENT(instanceNode, "privateDnsName", instance->privateDnsName);
        _ELEMENT(instanceNode, "instancePath", instance->instancePath);

        if (instance->params.kernel) {
            path = instance->params.kernel->backingPath;
            if (path_check(path, "kernel"))
                goto free;      // sanity check
            _ELEMENT(instanceNode, "kernel", path);
        }

        if (instance->params.ramdisk) {
            path = instance->params.ramdisk->backingPath;
            if (path_check(path, "ramdisk"))
                goto free;      // sanity check
            _ELEMENT(instanceNode, "ramdisk", path);
        }

        _ELEMENT(instanceNode, "consoleLogPath", instance->consoleFilePath);
        _ELEMENT(instanceNode, "userData", instance->userData);
        _ELEMENT(instanceNode, "launchIndex", instance->launchIndex);

        snprintf(cores_s, sizeof(cores_s), "%d", instance->params.cores);
        _ELEMENT(instanceNode, "cores", cores_s);
        snprintf(memory_s, sizeof(memory_s), "%d", instance->params.mem * 1024);
        _ELEMENT(instanceNode, "memoryKB", memory_s);

        // SSH-key related
        key = _NODE(instanceNode, "key");
        _ATTRIBUTE(key, "isKeyInjected", _BOOL(instance->do_inject_key));
        _ATTRIBUTE(key, "sshKey", instance->keyName);

        // OS-related specs
        os = _NODE(instanceNode, "os");
        _ATTRIBUTE(os, "platform", instance->platform);
        _ATTRIBUTE(os, "virtioRoot", _BOOL(config_use_virtio_root));
        _ATTRIBUTE(os, "virtioDisk", _BOOL(config_use_virtio_disk));
        _ATTRIBUTE(os, "virtioNetwork", _BOOL(config_use_virtio_net));

        // disks specification
        disks = _NODE(instanceNode, "disks");

        // the first disk should be the root disk (at least for Windows)
        for (j = 1; j >= 0; j--) {
            for (i = 0; ((i < EUCA_MAX_VBRS) && (i < instance->params.virtualBootRecordLen)); i++) {
                vbr = &(instance->params.virtualBootRecord[i]);

                // skip empty entries, if any
                if (vbr == NULL)
                    continue;

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
                } else {        // on all other os + hypervisor combinations, disks are used, so partitions must be skipped
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

            if (strlen(instance->floppyFilePath)) {
                _ELEMENT(disks, "floppyPath", instance->floppyFilePath);
            }
        }

        if (instance->params.nicType != NIC_TYPE_NONE) {    // NIC specification
            nics = _NODE(instanceNode, "nics");
            nic = _NODE(nics, "nic");
            _ATTRIBUTE(nic, "bridgeDeviceName", instance->params.guestNicDeviceName);
            _ATTRIBUTE(nic, "mac", instance->ncnet.privateMac);
        }

        ret = write_xml_file(doc, instance->instanceId, instance->xmlFilePath, "instance");

free:
        xmlFreeDoc(doc);
    }
    pthread_mutex_unlock(&xml_mutex);
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
            logprintfl(EUCATRACE, "ERROR from XML2/XSLT {%s}\n", buf);
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
static int apply_xslt_stylesheet(const char *xsltStylesheetPath, const char *inputXmlPath, const char *outputXmlPath, char *outputXmlBuffer,
                                 int outputXmlBufferSize)
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
                            logprintfl(EUCAERROR, "failed to save XML document to %s\n", outputXmlPath);
                            err = EUCA_IO_ERROR;
                        }
                        fclose(fp);
                    } else {
                        logprintfl(EUCAERROR, "failed to create file %s\n", outputXmlPath);
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
                            logprintfl(EUCAERROR, "XML string buffer is too small (%d > %d)\n", buf_size, outputXmlBufferSize);
                            err = EUCA_ERROR;
                        }
                        xmlFree(buf);
                    } else {
                        logprintfl(EUCAERROR, "failed to save XML document to a string\n");
                        err = EUCA_ERROR;
                    }
                }
            } else {
                logprintfl(EUCAERROR, "failed to apply stylesheet %s to %s\n", xsltStylesheetPath, inputXmlPath);
                err = EUCA_ERROR;
            }
            if (res != NULL)
                xmlFreeDoc(res);
            xmlFreeDoc(doc);
        } else {
            logprintfl(EUCAERROR, "failed to parse XML document %s\n", inputXmlPath);
            err = EUCA_ERROR;
        }
        xsltFreeStylesheet(cur);
    } else {
        logprintfl(EUCAERROR, "failed to open and parse XSL-T stylesheet file %s\n", xsltStylesheetPath);
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
    char path[MAX_PATH] = "";
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
        _ATTRIBUTE(hypervisor, "capability", hypervsorCapabilityTypeNames[instance->hypervisorCapability]);
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
        _ATTRIBUTE(root, "type", ncResourceTypeName[instance->params.root->type]);

        // volume information
        disk = _ELEMENT(volumeNode, "diskPath", remoteDev);
        _ATTRIBUTE(disk, "targetDeviceType", "disk");
        _ATTRIBUTE(disk, "targetDeviceName", localDevReal);
        _ATTRIBUTE(disk, "targetDeviceBus", "scsi");
        _ATTRIBUTE(disk, "sourceType", "block");

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
    char path[MAX_PATH] = "";
    char lpath[MAX_PATH] = "";

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
//! @return a pointer to a list of strings
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

    logprintfl(EUCATRACE, "searching for '%s' in '%s'\n", xpath, xml_path);
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
                            }
                        }
                    }
                    xmlXPathFreeObject(result);
                } else {
                    logprintfl(EUCAERROR, "no results for '%s' in '%s'\n", xpath, xml_path);
                }
                xmlXPathFreeContext(context);
            } else {
                logprintfl(EUCAERROR, "failed to set xpath '%s' context for '%s'\n", xpath, xml_path);
            }
            xmlFreeDoc(doc);
        } else {
            logprintfl(EUCADEBUG, "failed to parse XML in '%s'\n", xml_path);
        }
    }
    pthread_mutex_unlock(&xml_mutex);
    return (res);
}

#ifdef __STANDALONE
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
    xmlNodePtr features = NULL;
    xmlNodePtr disks = NULL;
    xmlNodePtr disk1 = NULL;

    xmlDocSetRootElement(doc, instance);

    hypervisor = xmlNewChild(instance, NULL, BAD_CAST "hypervisor", NULL);
    _ATTRIBUTE(hypervisor, "type", "kvm");
    _ATTRIBUTE(hypervisor, "mode", "hvm");
    _ELEMENT(instance, "name", "i-12345");
    _ELEMENT(instance, "kernel", "/var/run/instances/i-213456/kernel");
    _ELEMENT(instance, "ramdisk", "/var/run/instances/i-213456/initrd");
    _ELEMENT(instance, "consoleLogPath", "/var/run/instances/i-213456/console.log");
    _ELEMENT(instance, "cmdline", "ro console=ttyS0");
    _ELEMENT(instance, "cores", "1");
    _ELEMENT(instance, "memoryKB", "512000");

    os = _NODE(instance, "os");
    _ATTRIBUTE(os, "platform", "linux");
    _ATTRIBUTE(os, "virtioRoot", "true");
    _ATTRIBUTE(os, "virtioDisk", "false");
    _ATTRIBUTE(os, "virtioNetwork", "false");

    features = _NODE(instance, "features");
    _NODE(features, "acpi");

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

    xmlSaveFormatFileEnc(file, doc, "UTF-8", 1);
    logprintfl(EUCAINFO, "wrote XML to %s\n", file);
    cat(file);
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
        logprintfl(EUCAERROR, "required parameters are <XSLT stylesheet path>\n");
        return (EUCA_ERROR);
    }

    euca_strncpy(xslt_path, argv[1], sizeof(xslt_path));
    in_path = tempnam(NULL, "xml-");
    out_path = tempnam(NULL, "xml-");

    create_dummy_instance(in_path);

    logprintfl(EUCAINFO, "parsing stylesheet %s\n", xslt_path);
    if ((err = apply_xslt_stylesheet(xslt_path, in_path, out_path, NULL, 0)) != EUCA_OK)
        goto out;

    logprintfl(EUCAINFO, "parsing stylesheet %s again\n", xslt_path);
    if ((err = apply_xslt_stylesheet(xslt_path, in_path, out_path, xml_buf, sizeof(xml_buf))) != EUCA_OK)
        goto out;

    logprintfl(EUCAINFO, "wrote XML to %s\n", out_path);
    if (strlen(xml_buf) < 1) {
        err = EUCA_ERROR;
        logprintfl(EUCAERROR, "failed to see XML in buffer\n");
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
        logprintfl(EUCAERROR, "please, supply a path to an XML file and an Xpath\n");
        return (EUCA_ERROR);
    }

    inputXmlPath = argv[1];
    if ((devs = get_xpath_content(argv[1], argv[2])) != NULL) {
        for (j = 0; devs[j]; j++) {
            logprintfl(EUCAINFO, "devs[%d] = '%s'\n", j, devs[j]);
            EUCA_FREE(devs[j]);
        }
        EUCA_FREE(devs);
    } else {
        logprintfl(EUCAERROR, "devs[] are empty\n");
    }

    return (EUCA_OK);
}
#endif /* __STANDALONE2 */
