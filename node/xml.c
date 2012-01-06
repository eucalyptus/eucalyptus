// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-                                                                                                     // vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:                                                                                                                                               
/*
  Copyright (c) 2010  Eucalyptus Systems, Inc.	

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by 
  the Free Software Foundation, only version 3 of the License.  
 
  This file is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for more details.  

  You should have received a copy of the GNU General Public License along
  with this program.  If not, see <http://www.gnu.org/licenses/>.
 
  Please contact Eucalyptus Systems, Inc., 130 Castilian
  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
  if you need additional information or have any questions.

  This file may incorporate work covered under the following copyright and
  permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

  Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#define __USE_GNU
#include <string.h> // strlen, strcpy
#include <time.h>
#include <sys/types.h> // umask
#include <sys/stat.h> // umask
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

#include "handlers.h" // nc_state_t
#include "eucalyptus-config.h"
#include "backing.h" // umask
#include "data.h"
#include "misc.h"
#include "xml.h"

static int initialized = 0;
static boolean config_use_virtio_root = 0;
static boolean config_use_virtio_disk = 0;
static boolean config_use_virtio_net = 0;
static char xslt_path [MAX_PATH];

#ifdef __STANDALONE // if compiling as a stand-alone binary (for unit testing)
#define INIT() if (!initialized) init(NULL)
#else // if linking against an NC, find nc_state symbol
extern struct nc_state_t nc_state;
#define INIT() if (!initialized) init(&nc_state)
#endif

static void error_handler (void * ctx, const char * fmt, ...);
static pthread_mutex_t xml_mutex = PTHREAD_MUTEX_INITIALIZER; // process-global mutex

// macros for making XML construction a bit more readable
#define _NODE(P,N) xmlNewChild((P), NULL, BAD_CAST (N), NULL)
#define _ELEMENT(P,N,V) xmlNewChild((P), NULL, BAD_CAST (N), BAD_CAST (V))
#define _ATTRIBUTE(P,N,V) xmlNewProp((P), BAD_CAST (N), BAD_CAST (V))
#define _BOOL(S) ((S)?("true"):("false"))

static void init (struct nc_state_t * nc_state)
{
    pthread_mutex_lock (&xml_mutex);
    if (!initialized) {
        xmlInitParser();
        LIBXML_TEST_VERSION; // verifies that loaded library matches the compiled library
        xmlSubstituteEntitiesDefault (1); // substitute entities while parsing
        xmlSetGenericErrorFunc (NULL, error_handler); // catches errors/warnings that libxml2 writes to stderr
        xsltSetGenericErrorFunc (NULL, error_handler); // catches errors/warnings that libslt writes to stderr
        if (nc_state!=NULL) {
            config_use_virtio_root = nc_state->config_use_virtio_root;
            config_use_virtio_disk = nc_state->config_use_virtio_disk;
            config_use_virtio_net =  nc_state->config_use_virtio_net;
            strncpy (xslt_path, nc_state->libvirt_xslt_path, sizeof (xslt_path));
        }
        initialized = 1;
    }
    pthread_mutex_unlock (&xml_mutex);
}

static void cleanup (void)
{
    xsltCleanupGlobals();
    xmlCleanupParser(); // calls xmlCleanupGlobals()
}

// verify that the path for kernel/ramdisk is reasonable
static int path_check (const char * path, const char * name) // TODO: further checking?
{
    if (strstr (path, "/dev/") == path) {
        logprintfl (EUCAERROR, "internal error: path to %s points to a device %s\n", name, path);
        return 1;
    }
    return 0;
}

static int write_xml_file (const xmlDocPtr doc, const char * instanceId, const char * path, const char * type)
{
    mode_t old_umask = umask (~BACKING_FILE_PERM); // ensure the generated XML file has the right perms
    chmod (path, BACKING_FILE_PERM); // ensure perms in case when XML file exists
    int ret = xmlSaveFormatFileEnc (path, doc, "UTF-8", 1);
    if (ret > 0) {
        logprintfl (EUCAINFO, "[%s] wrote %s XML to %s\n", instanceId, type, path);
    } else {
        logprintfl (EUCAERROR, "[%s] failed to write %s XML to %s\n", instanceId, type, path);
    }
    umask (old_umask);

    return (ret > 0) ? (OK) : (ERROR);
}

// Encodes instance metadata (contained in ncInstance struct) in XML
// and writes it to file instance->xmlFilePath (/path/to/instance/instance.xml)
// That file gets processed through tools/libvirt.xsl (/etc/eucalyptus/libvirt.xsl)
// to produce /path/to/instance/libvirt.xml file that is passed to libvirt create.
int gen_instance_xml (const ncInstance * instance)
{
    INIT();

    int ret = 1;
    pthread_mutex_lock (&xml_mutex);
    xmlDocPtr doc = xmlNewDoc (BAD_CAST "1.0");
    xmlNodePtr instanceNode = xmlNewNode (NULL, BAD_CAST "instance");
    xmlDocSetRootElement (doc, instanceNode);

    { // hypervisor-related specs
        xmlNodePtr hypervisor = xmlNewChild (instanceNode, NULL, BAD_CAST "hypervisor", NULL);
        _ATTRIBUTE(hypervisor, "type", instance->hypervisorType);
        _ATTRIBUTE(hypervisor, "capability", hypervsorCapabilityTypeNames[instance->hypervisorCapability]);
        char bitness[4];
        snprintf(bitness, 4,"%d", instance->hypervisorBitness);
        _ATTRIBUTE(hypervisor, "bitness", bitness);
    }

    { // backing specification (TODO: maybe expand this with device maps or whatnot?)
        xmlNodePtr backing = xmlNewChild (instanceNode, NULL, BAD_CAST "backing", NULL);
        xmlNodePtr root = xmlNewChild (backing, NULL, BAD_CAST "root", NULL);
        assert (instance->params.root);
        _ATTRIBUTE(root, "type", ncResourceTypeName[instance->params.root->type]);
    }

    _ELEMENT(instanceNode, "name", instance->instanceId);
    _ELEMENT(instanceNode, "uuid", instance->uuid);
    _ELEMENT(instanceNode, "reservation", instance->reservationId);
    _ELEMENT(instanceNode, "user", instance->userId);
    _ELEMENT(instanceNode, "dnsName", instance->dnsName);
    _ELEMENT(instanceNode, "privateDnsName", instance->privateDnsName);
    _ELEMENT(instanceNode, "instancePath", instance->instancePath);
    if (instance->params.kernel) {
        char * path = instance->params.kernel->backingPath;
        if (path_check (path, "kernel")) goto free; // sanity check 
        _ELEMENT(instanceNode, "kernel", path);
    }
    if (instance->params.ramdisk) {
        char * path = instance->params.ramdisk->backingPath;
        if (path_check (path, "ramdisk")) goto free; // sanity check
        _ELEMENT(instanceNode, "ramdisk", path);
    }
    _ELEMENT(instanceNode, "consoleLogPath", instance->consoleFilePath);
    _ELEMENT(instanceNode, "userData", instance->userData);
    _ELEMENT(instanceNode, "launchIndex", instance->launchIndex);
    
    char cores_s  [10]; snprintf (cores_s,  sizeof (cores_s),  "%d", instance->params.cores);  _ELEMENT(instanceNode, "cores", cores_s);
    char memory_s [10]; snprintf (memory_s, sizeof (memory_s), "%d", instance->params.mem * 1024); _ELEMENT(instanceNode, "memoryKB", memory_s);

    { // SSH-key related
        xmlNodePtr key = _NODE(instanceNode, "key");
        _ATTRIBUTE(key, "isKeyInjected", _BOOL(instance->do_inject_key));
        _ATTRIBUTE(key, "sshKey", instance->keyName);
    }

    { // OS-related specs
        xmlNodePtr os = _NODE(instanceNode, "os");
        _ATTRIBUTE(os, "platform", instance->platform);
        _ATTRIBUTE(os, "virtioRoot", _BOOL(config_use_virtio_root));
        _ATTRIBUTE(os, "virtioDisk", _BOOL(config_use_virtio_disk));
        _ATTRIBUTE(os, "virtioNetwork", _BOOL(config_use_virtio_net));
    }

    { // disks specification
        xmlNodePtr disks = _NODE(instanceNode, "disks");

        // the first disk should be the root disk (at least for Windows)
        for (int root=1; root>=0; root--){ 
           for (int i=0; i<EUCA_MAX_VBRS && i<instance->params.virtualBootRecordLen; i++) {
               const virtualBootRecord * vbr = &(instance->params.virtualBootRecord[i]); 
               // skip empty entries, if any
               if (vbr==NULL)
                   continue;
               // do EMI on the first iteration of the outer loop
               if (root && vbr->type != NC_RESOURCE_IMAGE) 
                   continue;
               // ignore EMI on the second iteration of the outer loop
               if (!root && vbr->type == NC_RESOURCE_IMAGE)
                   continue;
               // skip anything without a device on the guest, e.g., kernel and ramdisk
               if (!strcmp ("none", vbr->guestDeviceName)) 
                   continue;
               // for Linux instances on Xen, partitions can be used directly, so disks can be skipped unless booting from EBS
               if (strstr (instance->platform, "linux") && strstr (instance->hypervisorType, "xen")) {
                   if (vbr->partitionNumber == 0 && vbr->type == NC_RESOURCE_IMAGE) {
                       continue;
                   }
               } else { // on all other os + hypervisor combinations, disks are used, so partitions must be skipped
                   if (vbr->partitionNumber > 0) {
                       continue;
                   }
               }
            
               xmlNodePtr disk = _ELEMENT(disks, "diskPath", vbr->backingPath);
               _ATTRIBUTE(disk, "targetDeviceType", libvirtDevTypeNames[vbr->guestDeviceType]);
               _ATTRIBUTE(disk, "targetDeviceName", vbr->guestDeviceName);
               char devstr[SMALL_CHAR_BUFFER_SIZE];
               snprintf(devstr, SMALL_CHAR_BUFFER_SIZE, "%s", vbr->guestDeviceName);             
               if (config_use_virtio_root) {
                   devstr[0] = 'v';
                   _ATTRIBUTE(disk, "targetDeviceNameVirtio", devstr);
                   _ATTRIBUTE(disk, "targetDeviceBusVirtio", "virtio");     
               }
               _ATTRIBUTE(disk, "targetDeviceBus", libvirtBusTypeNames[vbr->guestDeviceBus]);
               _ATTRIBUTE(disk, "sourceType", libvirtSourceTypeNames[vbr->backingType]);

               if (root) {
                   xmlNodePtr rootNode = _ELEMENT(disks, "root", NULL);
                   _ATTRIBUTE(rootNode, "device", devstr);
                   char root_uuid[64] = "";
                   if (get_blkid (vbr->backingPath, root_uuid, sizeof(root_uuid)) == 0) {
                       assert (strlen (root_uuid));
                       _ATTRIBUTE(rootNode, "uuid", root_uuid);
                   }
               }
           }
           if (strlen (instance->floppyFilePath)) {
               _ELEMENT(disks, "floppyPath", instance->floppyFilePath);
           }
       }
    }

    if (instance->params.nicType!=NIC_TYPE_NONE) { // NIC specification
        xmlNodePtr nics = _NODE(instanceNode, "nics");
        xmlNodePtr nic =  _NODE(nics, "nic");
        _ATTRIBUTE(nic, "bridgeDeviceName", instance->params.guestNicDeviceName);
        _ATTRIBUTE(nic, "mac", instance->ncnet.privateMac);
    }

    ret = write_xml_file (doc, instance->instanceId, instance->xmlFilePath, "instance");
 free:
    xmlFreeDoc(doc);
    pthread_mutex_unlock (&xml_mutex);

    return ret;
}

static int apply_xslt_stylesheet (const char * xsltStylesheetPath, const char * inputXmlPath, const char * outputXmlPath, char * outputXmlBuffer, int outputXmlBufferSize);

// Given a file with instance metadata in XML (instance->xmlFilePath)
// and an XSL-T stylesheet, produces XML document suitable for libvirt
int gen_libvirt_instance_xml (const ncInstance * instance)
{
        INIT();

        pthread_mutex_lock (&xml_mutex);
        int ret = apply_xslt_stylesheet (xslt_path, instance->xmlFilePath, instance->libvirtFilePath, NULL, 0);
        pthread_mutex_unlock (&xml_mutex);

        return ret;
}

// Gets called from XSLT/XML2 library, possibly several times per error.
// This handler concatenates the error pieces together and outputs a line,
// either when a newlines is seen or when the internal buffer is overrun.
// Needless to say, this function is not thread safe.
static void error_handler (void * ctx, const char * fmt, ...)
{
        int i;
        va_list ap;
        static char buf [512];
        static int size = 0;
        int old_size = size;

        va_start (ap, fmt);
        size += vsnprintf (buf + size, sizeof(buf) - size, fmt, ap);
        va_end (ap);
        
        for (i=old_size; i<sizeof(buf); i++) {
                if (buf[i]=='\n' || i==(sizeof(buf)-1)) {
                        size = 0;
                        buf[i]='\0';
                        logprintfl (EUCADEBUG, "ERROR from XML2/XSLT {%s}\n", buf);
                }                
                if (buf[i]=='\0') {
                        break;
                }
        }
}

// Processes input XML file (e.g., instance metadata) into output XML file or string (e.g., for libvirt)
// using XSL-T specification file (e.g., libvirt.xsl)
static int apply_xslt_stylesheet (const char * xsltStylesheetPath, const char * inputXmlPath, const char * outputXmlPath, char * outputXmlBuffer, int outputXmlBufferSize)
{
        int err = OK;

        INIT();
        xsltStylesheetPtr cur = xsltParseStylesheetFile ((const xmlChar *)xsltStylesheetPath);
        if (cur) {
                xmlDocPtr doc = xmlParseFile (inputXmlPath);
                if (doc) {
                        xsltTransformContextPtr ctxt = xsltNewTransformContext (cur, doc); // need context to get result
                        xsltSetCtxtParseOptions (ctxt, 0); // TODO: do we want any XSL-T parsing options?
                        xmlDocPtr res = xsltApplyStylesheetUser (cur, doc, NULL, NULL, NULL, ctxt); // applies XSLT to XML
                        int applied_ok = ctxt->state==XSLT_STATE_OK; // errors are communicated via ctxt->state
                        xsltFreeTransformContext (ctxt);
                        
                        if (res && applied_ok) {

                            // save to a file, if path was provied
                            if (outputXmlPath!=NULL) {
                                FILE * fp = fopen (outputXmlPath, "w");
                                if (fp) {
                                    int bytes = xsltSaveResultToFile (fp, res, cur);
                                    if (bytes==-1) {
                                        logprintfl (EUCAERROR, "ERROR: failed to save XML document to %s\n", outputXmlPath);
                                        err = ERROR;
                                    }
                                    fclose (fp);
                                } else {
                                    logprintfl (EUCAERROR, "ERROR: failed to create file %s\n", outputXmlPath);
                                    err = ERROR;
                                }                                
                            }

                            // convert to an ASCII buffer, if such was provided
                            if (err==OK && outputXmlBuffer!=NULL && outputXmlBufferSize > 0) {
                                xmlChar * buf;
                                int buf_size;
                                if (xsltSaveResultToString (&buf, &buf_size, res, cur)==0) { // success
                                    if (buf_size < outputXmlBufferSize) {
                                        bzero (outputXmlBuffer, outputXmlBufferSize);
                                        for (int i=0, j=0; i<buf_size; i++) {
                                            char c = (char) buf [i];
                                            if (c != '\n') // remove newlines
                                                outputXmlBuffer [j++] = c;
                                        }
                                    } else {
                                        logprintfl (EUCAERROR, "ERROR: XML string buffer is too small (%d > %d)\n", buf_size, outputXmlBufferSize);
                                        err = ERROR;
                                    }
                                    xmlFree (buf);
                                } else {
                                    logprintfl (EUCAERROR, "ERROR: failed to save XML document to a string\n");
                                    err = ERROR;
                                }
                            }
                        } else {
                            logprintfl (EUCAERROR, "ERROR: failed to apply stylesheet %s to %s\n", xsltStylesheetPath, inputXmlPath);
                            err = ERROR;
                        }
                        if (res!=NULL) xmlFreeDoc(res);
                        xmlFreeDoc(doc);
                } else {
                        logprintfl (EUCAERROR, "ERROR: failed to parse XML document %s\n", inputXmlPath);
                        err = ERROR;
                }
                xsltFreeStylesheet(cur);
        } else {
                logprintfl (EUCAERROR, "ERROR: failed to open and parse XSL-T stylesheet file %s\n", xsltStylesheetPath);
                err = ERROR;
        }

        return err;
}

int gen_libvirt_attach_xml (const char *volumeId, const ncInstance *instance, const char * localDevReal, const char * remoteDev, char * xml, unsigned int xml_size)
{
    INIT();

    int ret = 1;
    pthread_mutex_lock (&xml_mutex);
    xmlDocPtr doc = xmlNewDoc (BAD_CAST "1.0");
    xmlNodePtr volumeNode = xmlNewNode (NULL, BAD_CAST "volume");
    xmlDocSetRootElement (doc, volumeNode);

    { // hypervisor-related specs
        xmlNodePtr hypervisor = xmlNewChild (volumeNode, NULL, BAD_CAST "hypervisor", NULL);
        _ATTRIBUTE(hypervisor, "type", instance->hypervisorType);
        _ATTRIBUTE(hypervisor, "capability", hypervsorCapabilityTypeNames[instance->hypervisorCapability]);
        char bitness[4];
        snprintf(bitness, 4,"%d", instance->hypervisorBitness);
        _ATTRIBUTE(hypervisor, "bitness", bitness);
    }

    _ELEMENT(volumeNode, "id", volumeId);
    _ELEMENT(volumeNode, "user", instance->userId);
    _ELEMENT(volumeNode, "instancePath", instance->instancePath);

    { // OS-related specs
        xmlNodePtr os = _NODE(volumeNode, "os");
        _ATTRIBUTE(os, "platform", instance->platform);
        _ATTRIBUTE(os, "virtioRoot", _BOOL(config_use_virtio_root));
        _ATTRIBUTE(os, "virtioDisk", _BOOL(config_use_virtio_disk));
        _ATTRIBUTE(os, "virtioNetwork", _BOOL(config_use_virtio_net));
    }

    { // backing specification (TODO: maybe expand this with device maps or whatnot?)
        xmlNodePtr backing = xmlNewChild (volumeNode, NULL, BAD_CAST "backing", NULL);
        xmlNodePtr root = xmlNewChild (backing, NULL, BAD_CAST "root", NULL);
        assert (instance->params.root);
        _ATTRIBUTE(root, "type", ncResourceTypeName[instance->params.root->type]);
    }

    { // volume information
        xmlNodePtr disk = _ELEMENT(volumeNode, "diskPath", remoteDev);
        _ATTRIBUTE(disk, "targetDeviceType", "disk");
        _ATTRIBUTE(disk, "targetDeviceName", localDevReal);
        _ATTRIBUTE(disk, "targetDeviceBus", "scsi");
        _ATTRIBUTE(disk, "sourceType", "block");
        
    }

    char path [MAX_PATH];
    snprintf (path, sizeof (path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);
    ret = write_xml_file (doc, instance->instanceId, path, "volume")
        || apply_xslt_stylesheet (xslt_path, path, NULL, xml, xml_size);
    logprintfl (EUCADEBUG2, "XML={%s}\n", xml);

    xmlFreeDoc(doc);
    pthread_mutex_unlock (&xml_mutex);

    return ret;
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// code for unit-testing below, to be compiled into a stand-alone binary
///////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef __STANDALONE

static void create_dummy_instance (const char * file)
{
        xmlDocPtr doc = xmlNewDoc (BAD_CAST "1.0");
        xmlNodePtr instance = xmlNewNode (NULL, BAD_CAST "instance");
        xmlDocSetRootElement (doc, instance);
        {
                xmlNodePtr hypervisor = xmlNewChild (instance, NULL, BAD_CAST "hypervisor", NULL);
                _ATTRIBUTE(hypervisor, "type", "kvm");
                _ATTRIBUTE(hypervisor, "mode", "hvm");
        }
        _ELEMENT(instance, "name", "i-12345");
        _ELEMENT(instance, "kernel", "/var/run/instances/i-213456/kernel");
        _ELEMENT(instance, "ramdisk", "/var/run/instances/i-213456/initrd");
        _ELEMENT(instance, "consoleLogPath", "/var/run/instances/i-213456/console.log");
        _ELEMENT(instance, "cmdline", "ro console=ttyS0");
        _ELEMENT(instance, "cores", "1");
        _ELEMENT(instance, "memoryKB", "512000");
        {
                xmlNodePtr os = _NODE(instance, "os");
                _ATTRIBUTE(os, "platform", "linux");
                _ATTRIBUTE(os, "virtioRoot", "true");
                _ATTRIBUTE(os, "virtioDisk", "false");
                _ATTRIBUTE(os, "virtioNetwork", "false");
        }
        {
                xmlNodePtr features = _NODE(instance, "features");
                _NODE(features, "acpi");
        }
        {
                xmlNodePtr disks = _NODE(instance, "disks");
                {
                        xmlNodePtr disk1 = _ELEMENT(disks, "diskPath", "/var/run/instances/i-213456/root");
                        _ATTRIBUTE(disk1, "targetDeviceType", "disk");
                        _ATTRIBUTE(disk1, "targetDeviceName", "sda1");
                        _ATTRIBUTE(disk1, "targetDeviceBus", "virtio");
                        _ATTRIBUTE(disk1, "sourceType", "file");
                }
                {
                        xmlNodePtr disk1 = _ELEMENT(disks, "diskPath", "/var/run/instances/i-213456/swap");
                        _ATTRIBUTE(disk1, "targetDeviceType", "disk");
                        _ATTRIBUTE(disk1, "targetDeviceName", "sda3");
                        _ATTRIBUTE(disk1, "targetDeviceBus", "scsi");
                        _ATTRIBUTE(disk1, "sourceType", "file");
                }
        }

        xmlSaveFormatFileEnc (file, doc, "UTF-8", 1);
        logprintfl (EUCAINFO, "wrote XML to %s\n", file);
        cat (file);
        xmlFreeDoc(doc);
}

int main (int argc, char ** argv)
{
        if (argc!=2) {
                logprintfl (EUCAERROR, "ERROR: required parameters are <XSLT stylesheet path>\n");
                return 1;
        }
        strncpy (xslt_path, argv[1], sizeof (xslt_path));
        char * in_path = tempnam (NULL, "xml-");
        char * out_path = tempnam (NULL, "xml-");

        create_dummy_instance (in_path);

        logprintfl (EUCAINFO, "parsing stylesheet %s\n", xslt_path);
        int err = apply_xslt_stylesheet (xslt_path, in_path, out_path, NULL, 0);
        if (err!=OK) 
                goto out;
        logprintfl (EUCAINFO, "parsing stylesheet %s again\n", xslt_path);
        char xml_buf [2048];
        err = apply_xslt_stylesheet (xslt_path, in_path, out_path, xml_buf, sizeof (xml_buf));
        if (err!=OK) 
                goto out;
        logprintfl (EUCAINFO, "wrote XML to %s\n", out_path);
        if (strlen (xml_buf) < 1) {
            err = ERROR;
            logprintfl (EUCAERROR, "failed to see XML in buffer\n");
            goto out;
        }
        cat (out_path);
out:
        remove (out_path);
        remove (in_path);
        free (in_path);
        free (out_path);
        return err;
}
#endif
