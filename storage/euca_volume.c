// (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
//
// Redistribution and use of this software in source and binary forms,
// with or without modification, are permitted provided that the following
// conditions are met:
//
//   Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
//
//   Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

//!
//! @file util/euca_volume.c
//! C-client for Storage Controller to test/dev operations from the NC to SC.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <unistd.h>                    /* getopt */

#include <libxml/debugXML.h>
#include <libxml/parser.h>
#include <libxslt/xslt.h>
#include <libxml/xpath.h>
#include <libxml/xpathInternals.h>
#include <libxml/tree.h>
#include <libxslt/xsltutils.h>
#include <libxml/xinclude.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <euca_axis.h>
#include <euca_auth.h>

#include "storage-controller.h"
#include "ebs_utils.h"
#include "iscsi.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define BUFSIZE                     1024

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
//! Structure defining NC Volumes
typedef struct eucaVolume_t {
    char id[CHAR_BUFFER_SIZE];   //!< Remote volume identifier string
    char attachment_token[CHAR_BUFFER_SIZE]; //!< Remote device name string, the token reference
    char device[CHAR_BUFFER_SIZE];    //!< Canonical device name (without '/dev/')
    char state[CHAR_BUFFER_SIZE];  //!< Volume state name string
    char connection_string[VERY_BIG_CHAR_BUFFER_SIZE];   //!< Volume Token for attachment/detachment
    char libvirt_XML[VERY_BIG_CHAR_BUFFER_SIZE];  //!< XML for describing the disk to libvirt
    char serial[VERY_BIG_CHAR_BUFFER_SIZE];   //!< serial for device and volume attachment
    char bus[VERY_BIG_CHAR_BUFFER_SIZE];   //!< bus for volume attachment
} eucaVolume;
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

#ifndef NO_COMP
const char *euca_this_component_name = "sc";    //!< Eucalyptus Component Name
const char *euca_client_component_name = "nc";  //!< The client component name
#endif /* ! NO_COMP */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/
static boolean initialized = FALSE;    //!< To determine if the XML library has been initialized
static pthread_mutex_t xml_mutex = PTHREAD_MUTEX_INITIALIZER;   //!< process-global mutex

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
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

#define CHECK_PARAM(par, name) if (par==NULL) { fprintf (stderr, "ERROR: no %s specified (try -h)\n", name); exit (1); }
#define INIT() if (!initialized) init_xml()

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/
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
            fprintf(stderr, "ERROR from XML2/XSLT {%s}\n", buf);
        }

        if (buf[i] == '\0') {
            break;
        }
    }
}

//!
//! Initialize the XML local parameters from the NC state structure.
//!
//! @param[in] nc_state a pointer to the NC state structure to initialize
//!
static void init_xml(void)
{
    pthread_mutex_lock(&xml_mutex);
    {
        if (!initialized) {
            xmlIndentTreeOutput = 1;
            xmlKeepBlanksDefault(0);
            xmlInitParser();
            LIBXML_TEST_VERSION;       // verifies that loaded library matches the compiled library
            xmlSubstituteEntitiesDefault(1);    // substitute entities while parsing
            xmlSetGenericErrorFunc(NULL, error_handler);    // catches errors/warnings that libxml2 writes to stderr
            xsltSetGenericErrorFunc(NULL, error_handler);   // catches errors/warnings that libslt writes to stderr
            initialized = TRUE;
        }
    }
    pthread_mutex_unlock(&xml_mutex);
}

//!
//! Places raw XML result of an xpath query into buf. The query must return
//! only one element.
//!
//! @param[in] xml_path a string containing the path to the XML file to parse
//! @param[in] xpath a string contianing the XPATH expression to evaluate
//! @param[out] buf for the XML string
//! @param[in] buf_len size of the buf
//!
//! @return EUCA_OK or EUCA_ERROR
//!
int get_xpath_xml(const char *xml_path, const char *xpath, char *buf, int buf_len)
{
    int ret = EUCA_ERROR;
    xmlDocPtr doc = NULL;
    xmlXPathContextPtr context = NULL;
    xmlXPathObjectPtr result = NULL;
    xmlNodeSetPtr nodeset = NULL;

    INIT();

    pthread_mutex_lock(&xml_mutex);
    {
        if ((doc = xmlParseFile(xml_path)) != NULL) {
            if ((context = xmlXPathNewContext(doc)) != NULL) {
                if ((result = xmlXPathEvalExpression(((const xmlChar *)xpath), context)) != NULL) {
                    if (!xmlXPathNodeSetIsEmpty(result->nodesetval)) {
                        nodeset = result->nodesetval;
                        if (nodeset->nodeNr > 1) {
                            fprintf(stderr, "multiple matches for '%s' in '%s'\n", xpath, xml_path);
                        } else {
                            xmlNodePtr node = nodeset->nodeTab[0]->xmlChildrenNode;
                            xmlBufferPtr xbuf = xmlBufferCreate();
                            if (xbuf) {
                                int len = xmlNodeDump(xbuf, doc, node, 0, 1);
                                if (len < 0) {
                                    fprintf(stderr, "failed to extract XML from %s\n", xpath);
                                } else if (len > buf_len) {
                                    fprintf(stderr, "insufficient buffer for %s\n", xpath);
                                } else {
                                    char *str = (char *)xmlBufferContent(xbuf);
                                    euca_strncpy(buf, str, buf_len);
                                    ret = EUCA_OK;
                                }
                                xmlBufferFree(xbuf);
                            } else {
                                fprintf(stderr, "failed to allocate XML buffer\n");
                            }
                        }
                    }
                    xmlXPathFreeObject(result);
                } else {
                    fprintf(stderr, "no results for '%s' in '%s'\n", xpath, xml_path);
                }
                xmlXPathFreeContext(context);
            } else {
                fprintf(stderr, "failed to set xpath '%s' context for '%s'\n", xpath, xml_path);
            }
            xmlFreeDoc(doc);
        } else {
            fprintf(stderr, "failed to parse XML in '%s'\n", xml_path);
        }
    }
    pthread_mutex_unlock(&xml_mutex);

    return ret;
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

    pthread_mutex_lock(&xml_mutex);
    {
        if ((doc = xmlParseFile(xml_path)) != NULL) {
            if ((context = xmlXPathNewContext(doc)) != NULL) {
                if ((result = xmlXPathEvalExpression(((const xmlChar *)xpath), context)) != NULL) {
                    if (!xmlXPathNodeSetIsEmpty(result->nodesetval)) {
                        nodeset = result->nodesetval;
                        // We will add one more to have a NULL entry at the end
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
                    fprintf(stderr, "no results for '%s' in '%s'\n", xpath, xml_path);
                }
                xmlXPathFreeContext(context);
            } else {
                fprintf(stderr, "failed to set xpath '%s' context for '%s'\n", xpath, xml_path);
            }
            xmlFreeDoc(doc);
        } else {
            fprintf(stderr, "failed to parse XML in '%s'\n", xml_path);
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


int loop_through_volumes(const char *xml_path, eucaVolume volumes[EUCA_MAX_VOLUMES]){
    char volxpath[128] = "";

#define MKVOLPATH(_suffix)   snprintf(volxpath, sizeof(volxpath), "/instance/volumes/volume[%d]/%s", (i + 1), _suffix);
#define MKLIBVIRTVOLPATH(_suffix)   snprintf(volxpath, sizeof(volxpath), "/instance/volumes/volume[%d]/libvirt/disk/%s", (i + 1), _suffix);
#define XGET_STR_FREE(_XPATH, _dest)                                                 \
{                                                                                    \
    if (get_xpath_content_at(xml_path, (_XPATH), 0, _dest, sizeof(_dest)) == NULL) { \
        fprintf(stderr, "failed to read %s from %s\n", (_XPATH), xml_path);                 \
        for (int z = 0; res_array[z] != NULL; z++)                                   \
            EUCA_FREE(res_array[z]);                                                 \
        EUCA_FREE(res_array);                                                        \
        return (EUCA_ERROR);                                                         \
    }                                                                                \
}
    char **res_array = NULL;

    if ((res_array = get_xpath_content(xml_path, "/instance/volumes/volume")) != NULL) {
        for (int i = 0; (res_array[i] != NULL) && (i < EUCA_MAX_VOLUMES); i++) {
            eucaVolume *v = &volumes[i];

            MKVOLPATH("id");
            XGET_STR_FREE(volxpath, v->id);
            MKVOLPATH("attachmentToken");
            XGET_STR_FREE(volxpath, v->attachment_token);
            MKVOLPATH("stateName");
            XGET_STR_FREE(volxpath, v->state);
            MKVOLPATH("connectionString");
            XGET_STR_FREE(volxpath, v->connection_string);
            MKVOLPATH("devName");
            XGET_STR_FREE(volxpath, v->device);
            MKVOLPATH("libvirt");
            if (strcmp(v->state, VOL_STATE_ATTACHING) && strcmp(v->state, VOL_STATE_ATTACHING_FAILED) && get_xpath_xml(xml_path, volxpath, v->libvirt_XML, sizeof(v->libvirt_XML)) != EUCA_OK) {
                fprintf(stderr, "failed to read '%s' from '%s'\n", volxpath, xml_path);
                for (int z = 0; res_array[z] != NULL; z++)
                    EUCA_FREE(res_array[z]);
                EUCA_FREE(res_array);
                return (EUCA_ERROR);
            }
            MKLIBVIRTVOLPATH("serial");
            XGET_STR_FREE(volxpath, v->serial);
            MKLIBVIRTVOLPATH("target/@bus");
            XGET_STR_FREE(volxpath, v->bus);
        }

        // Free our allocated memory
        for (int i = 0; res_array[i] != NULL; i++)
            EUCA_FREE(res_array[i]);
        EUCA_FREE(res_array);
    }

    return (EUCA_OK);
}

char *find_instance_path(const char* instance_paths, const char *instance_id){
    DIR *insts_dir = NULL;
    char tmp_path[EUCA_MAX_PATH] = "";
    char user_paths[EUCA_MAX_PATH] = "";
    struct dirent *dir_entry = NULL;
    struct stat mystat = { 0 };
    char *instance_path = NULL;;

    // we don't know userId, so we'll look for instanceId in every user's
    // directory (we're assuming that instanceIds are unique in the system)
    snprintf(user_paths, sizeof(user_paths), "%s/work", instance_paths);

    if ((insts_dir = opendir(user_paths)) == NULL) {
        fprintf(stderr, "failed to open %s\n", user_paths);
        return NULL;
    }
    // Scan every path under the user path for one that conaints our instance
    while ((dir_entry = readdir(insts_dir)) != NULL) {
        snprintf(tmp_path, sizeof(tmp_path), "%s/%s/%s", user_paths, dir_entry->d_name, instance_id);
        if (stat(tmp_path, &mystat) == 0) {
            // found it. Now save our user identifier
            instance_path = strdup(tmp_path);
            break;
        }
    }

    // Done with the directory
    closedir(insts_dir);
    insts_dir = NULL;

    return instance_path;
}


char *find_ip_addr(void){
    char hostname[HOSTNAME_SIZE];
    if (gethostname(hostname, sizeof(hostname)) != 0) {
        fprintf(stderr, "failed to find hostname\n");
        return NULL;
    }
    fprintf(stderr, "Searching for IP by hostname %s\n", hostname);

    struct addrinfo hints, *servinfo, *p;
    struct sockaddr_in *h;
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    char ip[BUFSIZE];
    int rv;
    if ((rv = getaddrinfo(hostname, "http", &hints, &servinfo)) != 0) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
        return NULL;
    }
    int found = 0;
    for(p = servinfo; !found && p != NULL; p = p->ai_next) {
        if (!found) {
            h = (struct sockaddr_in *) p->ai_addr;
            euca_strncpy(ip, inet_ntoa(h->sin_addr), sizeof(ip));
            found = 1;
        }
    }
    freeaddrinfo(servinfo);

    return strdup(ip);
}

char *find_local_iqn(void){
    char *ptr = NULL, *iqn = NULL, *tmp = NULL;
    ptr = system_output("cat /etc/iscsi/initiatorname.iscsi");
    if (ptr) {
        iqn = strstr(ptr, "InitiatorName=");
        if (iqn) {
            iqn += strlen("InitiatorName=");
            tmp = strstr(iqn, "\n");
            if (tmp)
                *tmp = '\0';
        }
        ptr = NULL;
    }

    return iqn;
}
//!
//! Prints the command help to stderr
//!
void usage(void)
{
    fprintf(stderr, "usage: SCclient [command] [options]\n"
            "\tcommands:\t\t\trequired options:\n"
            "\t\tConnectVolumes\t\t[-i] [str] [-s] [str]\n"
            "\t\tDisconnectVolumes\t[-i] [str] [-s] [str]\n"
            "\toptions:\n"
            "\t\t-h \t\t- this help information\n"
            "\t\t-s [host:port] \t- SC endpoint\n"
            "\t\t-i [str] \t- instance id to search for\n");
    exit(1);
}

void setup_iscsi(char *euca_home, char *configFile)
{
    char *ceph_user;
    char *ceph_keys;
    char *ceph_conf;
    int rc;

    rc = get_conf_var(configFile, CONFIG_NC_CEPH_USER, &ceph_user);
    rc = get_conf_var(configFile, CONFIG_NC_CEPH_KEYS, &ceph_keys);
    rc = get_conf_var(configFile, CONFIG_NC_CEPH_CONF, &ceph_conf);

    init_iscsi(euca_home,
               (ceph_user == NULL) ? (DEFAULT_CEPH_USER) : (ceph_user),
               (ceph_keys == NULL) ? (DEFAULT_CEPH_KEYRING) : (ceph_keys),
               (ceph_conf == NULL) ? (DEFAULT_CEPH_CONF) : (ceph_conf));
    EUCA_FREE(ceph_user);
    EUCA_FREE(ceph_keys);
    EUCA_FREE(ceph_conf);
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return Always return 0 or exit(1) on failure
//!
int main(int argc, char **argv)
{
    char *sc_hostport = NULL;
    char *command = NULL;
    char *ip = NULL;
    char *instance_id = NULL;
    char *iqn = NULL;
    int ch = 0;

    log_file_set(NULL, NULL);
    log_params_set(EUCA_LOG_ALL, 0, 1);

    while ((ch = getopt(argc, argv, "hn:h:s:i:")) != -1) {
        switch (ch) {
        case 's':
            sc_hostport = optarg;
            break;
        case 'i':
            instance_id = optarg;
            break;
        case 'h':
            usage();                   // will exit
            break;
        case '?':
        default:
            fprintf(stderr, "ERROR: unknown parameter (try -h)\n");
            exit(1);
        }
    }
    argc -= optind;
    argv += optind;

    if (argc > 0) {
        command = argv[0];
        if (argc > 1) {
            fprintf(stderr, "WARNING: too many parameters (%d), using first one as command\n", argc);
            for (int i = 0; i <= argc; i++) {
                if (argv[i])
                    fprintf(stderr, "%d = %s\n", i, argv[i]);
            }
        }
    } else {
        fprintf(stderr, "ERROR: command not specified (try -h)\n");
        exit(1);
    }

    char configFile[BUFSIZE], policyFile[BUFSIZE];
    char *euca_home;
    char sc_url[BUFSIZE];

    euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        euca_home = "";
    }

    snprintf(configFile, BUFSIZE, EUCALYPTUS_CONF_LOCATION, euca_home);
    snprintf(policyFile, BUFSIZE, EUCALYPTUS_POLICIES_DIR "/sc-client-policy.xml", euca_home);
    snprintf(sc_url, BUFSIZE, "http://%s/services/Storage", sc_hostport);

    char *instances_path;
    int rc;

    rc = get_conf_var(configFile, INSTANCE_PATH, &instances_path);

    char *instance_path = find_instance_path(instances_path, instance_id);
    eucaVolume volumes[EUCA_MAX_VOLUMES];
    char instance_xml[BUFSIZE];
    snprintf(instance_xml, BUFSIZE, "%s/instance.xml", instance_path);

    loop_through_volumes(instance_xml, volumes);

    ip = find_ip_addr();
    iqn = find_local_iqn();

    printf("Found local iqn=%s and local ip=%s\n", iqn, ip);

    for(int i = 0; i < EUCA_MAX_VOLUMES; i++){
        if(strlen(volumes[i].state) == 0 || strcmp(volumes[i].state, VOL_STATE_ATTACHED)) continue;

        printf("Performing operation on volume %d\nid=%s\ntoken=%s\ndevice=%s\nconnectionstring=%s\nbus=%s\nserial=%s\n", 
               i, volumes[i].id, volumes[i].attachment_token, volumes[i].device, volumes[i].connection_string, volumes[i].bus, volumes[i].serial);
        /***********************************************************/
        if (!strcmp(command, "ConnectVolumes")) {
            CHECK_PARAM(ip, "ip");
            CHECK_PARAM(iqn, "iqn");
            CHECK_PARAM(sc_hostport, "sc host and port");

            setup_iscsi(euca_home, configFile);
            euca_init_cert();

            char *libvirt_xml = NULL;
            ebs_volume_data *vol_data = NULL;

            if (connect_ebs_volume(volumes[i].id, volumes[i].serial, volumes[i].bus, sc_url, volumes[i].attachment_token, 1, policyFile, ip, iqn, &libvirt_xml, &vol_data) != EUCA_OK) {
                fprintf(stderr, "Error connecting ebs volume %s\n", volumes[i].id);
                exit(1);
            }

            /***********************************************************/
        } else if (!strcmp(command, "DisconnectVolumes")) {
            CHECK_PARAM(ip, "ip");
            CHECK_PARAM(iqn, "iqn");
            CHECK_PARAM(sc_hostport, "sc host and port");

            setup_iscsi(euca_home, configFile);
            euca_init_cert();

            if (disconnect_ebs_volume(sc_url, 1, policyFile, volumes[i].attachment_token, volumes[i].connection_string, ip, iqn) != EUCA_OK) {
                fprintf(stderr, "Error disconnecting ebs volume %s.\n", volumes[i].id);
                exit(1);
            }

            /***********************************************************/
        } else {
            fprintf(stderr, "ERROR: command %s unknown (try -h)\n", command);
            exit(1);
        }
    }

    _exit(0);
}
