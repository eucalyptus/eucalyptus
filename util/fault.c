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
//! @file util/fault.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <assert.h>
#include <dirent.h>
#include <sys/errno.h>
#include <limits.h>
#include <locale.h>
#include <pthread.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <wchar.h>
#include <sys/stat.h>
#include <ctype.h>

#include <libxml/parser.h>
#include <libxml/tree.h>
#include <eucalyptus-config.h>

#include "eucalyptus.h"
#include "misc.h"
#include "fault.h"
#include "wc.h"
#include "utf8.h"
#include "euca_string.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! These definitions are all easily customized.
//!
//! @todo Make some or all of them configuration-file options?
//! @todo Make paths relative to some configurable base directory?
#define FAULT_LOGFILE_SUFFIX                     "-fault.log"
#define DEFAULT_LOCALIZATION                     "en_US"
#define LOCALIZATION_ENV_VAR                     "LOCALE"
#define XML_SUFFIX                               ".xml"
#define COMMON_PREFIX                            "common"   //!< For .xml file defining common fault labels
#define LOCALIZED_TAG                            "localized"
#define MESSAGE_TAG                              "message"
#define STANDARD_FILESTREAM                      stderr

//! @{
//! @name for ourput formatting

#define STARS                                    "************************************************************************"
#define BARS                                     "" /* I want to remove these little monsters! */

//! @}

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

//! This is the order of priority (highest to lowest) for fault messages wrt customization & localization.
//!
//! Once a fault id has been found and added to the in-memory repository, all further occurrences of that
//! fault in lower-priority faultdirs will be ignored. This is how we set the customization/localization
//! pecking order.
enum faultdir_types {
    CUSTOM_LOCALIZED,
    CUSTOM_DEFAULT_LOCALIZATION,
    DISTRO_LOCALIZED,
    DISTRO_DEFAULT_LOCALIZATION,
    NUM_FAULTDIR_TYPES,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Linked list of faults being deliberately suppressed.
struct suppress_list {
    char *id;                          //!< Fault identifier string
    struct suppress_list *next;        //!< Pointer to the next fault in line
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

//! Global equivalent to argv[0]'s basename.
extern char *program_invocation_short_name;

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

//! Defines the order of labels in fault log entries.
static char *fault_labels[] = { "condition",
    "cause",
    "initiator",
    "location",
    "resolution",
    0,
};

 //! This holds the in-memory model of the fault registry.
static xmlDoc *ef_doc = NULL;

 //! Fault log filehandle.
static FILE *faultlog = NULL;

 //! @todo Thread safety is only half-baked at this point.
static pthread_mutex_t fault_mutex = PTHREAD_MUTEX_INITIALIZER;

 //! base install directory ($EUCALYPTUS) or empty if root
static char euca_base[PATH_MAX] = "";

//! Pointer to the suppressed fault list
static struct suppress_list *suppressed = NULL;

//! Pointer to the redundant fault list
static struct suppress_list *redundant = NULL;

#ifdef EUCA_GENERATE_FAULT
//! Name of this component
const char *euca_this_component_name = "generate-fault";
#endif /* EUCA_GENERATE_FAULT */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static boolean str_end_cmp(const char *str, const char *suffix);
static char *str_trim_suffix(char *str, const char *suffix);
static int scandir_filter(const struct dirent *entry);
#ifndef HAVE_XMLFIRSTELEMENTCHILD
static xmlNodePtr xmlFirstElementChild(xmlNodePtr parent);
#endif /* ! HAVE_XMLFIRSTELEMENTCHILD */
static boolean is_suppressed_eucafault(const char *fault_id);
static boolean check_eucafault_suppression(const char *fault_id, const char *fault_file);
static xmlDoc *read_eucafault(const char *faultdir, const char *fault_id);
static boolean add_eucafault(const xmlDoc * new_doc);
static char *get_fault_id(const xmlNode * node);
static xmlNode *get_common_block(const xmlDoc * doc);
static xmlNode *get_eucafault(const char *fault_id, const xmlDoc * doc);
static boolean initialize_faultlog(const char *fileprefix);
static char *get_common_var(const char *var);
static char *get_fault_var(const char *var, const xmlNode * f_node);
static boolean format_eucafault(const char *fault_id, const char_map ** map);

#ifdef _UNIT_TEST
static void dump_eucafaults_db(void);
static void usage(const char *argv0);
#endif /* _UNIT_TEST */

#ifdef EUCA_GENERATE_FAULT
static void usage(const char *argv0);
#endif /* EUCA_GENERATE_FAULT */

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
//! Compares end of one string to another string (the suffix) for a match.
//!
//! @param[in] str the string serving as a haystack
//! @param[in] suffix the string to compare the end of the haystack
//!
//! @return TRUE if 'suffix' matches the end of 'str'. Otherwise, FALSE is returned.
//!
static boolean str_end_cmp(const char *str, const char *suffix)
{
    size_t lenstr = 0;
    size_t lensuffix = 0;

    if (!str || !suffix)
        return (FALSE);

    lenstr = strlen(str);
    lensuffix = strlen(suffix);

    if (lensuffix > lenstr)
        return (FALSE);

    return (strncmp(str + lenstr - lensuffix, suffix, lensuffix) == 0);
}

//!
//! Trims end of string off if it matches a supplied suffix.
//!
//! @param[in] str the string serving as a haystack
//! @param[in] suffix the string to compare the end of the haystack
//!
//! @return a pointer to str.
//!
static char *str_trim_suffix(char *str, const char *suffix)
{
    int trim = 0;

    if (str && suffix && str_end_cmp(str, suffix)) {
        trim = strlen(str) - strlen(suffix);
        *(str + trim) = '\0';
        LOGTRACE("returning: %s\n", str);
    }
    return (str);
}

//!
//! Used internally by scandir() to match filenames by suffix.
//!
//! @param[in] entry
//!
//! @return the result of the str_end_cmp() call between entry and XML_SUFFIX
//!
//! @see str_end_cmp()
//!
static int scandir_filter(const struct dirent *entry)
{
    return str_end_cmp(entry->d_name, XML_SUFFIX);
}

#ifndef HAVE_XMLFIRSTELEMENTCHILD
//!
//! Needed for older versions of libxml2 (e.g. on RHEL/CentOS 5), which don't contain this function.
//!
//! @param[in] parent a pointer to the element from which we'll retrieve teh first child element
//!
//! @return a pointer to the first child of a node
//!
static xmlNodePtr xmlFirstElementChild(xmlNodePtr parent)
{
    xmlNodePtr child = NULL;

    if (parent == NULL)
        return (NULL);

    for (child = parent->children; child != NULL; child = child->next) {
        if (child->type == XML_ELEMENT_NODE)
            break;
    }

    return (child);
}
#endif /* ! HAVE_XMLFIRSTELEMENTCHILD */

//!
//!
//!
//! @param[in] fault_id
//!
//! @return TRUE if fault_id is being suppressed, FALSE otherwise.
//!
static boolean is_suppressed_eucafault(const char *fault_id)
{
    if (fault_id == NULL) {
        LOGWARN("called with NULL argument...ignoring.\n");
        return FALSE;
    }
    struct suppress_list *suppose = suppressed;

    while (suppose) {
        if (!strcmp(fault_id, suppose->id)) {
            LOGTRACE("returning TRUE for %s.\n", fault_id);
            return TRUE;
        }
        suppose = suppose->next;
    }
    LOGTRACE("returning FALSE for %s.\n", fault_id);
    return FALSE;
}

//!
//! If second argument (fault_file) is non-NULL, checks path specified in arg for a
//! zero-length file. If one is found, the specified fault_id will be added to the
//! suppression list.
//!
//! If second argument is NULL, simply checks for presence of fault_id in suppression
//! list by calling is_suppressed_eucafault().
//!
//! @param[in] fault_id the fault identifier string
//! @param[in] fault_file the suppression list fault file name
//!
//! @return Returns TRUE if fault_id is being suppressed, FALSE otherwise.
//!
//! @see is_suppressed_eucafault()
//!
static boolean check_eucafault_suppression(const char *fault_id, const char *fault_file)
{
    if ((fault_id == NULL) && (fault_file == NULL)) {
        LOGWARN("called with two NULL arguments...ignoring.\n");
        return FALSE;
    }
    if (fault_file == NULL) {
        // Degenerate case.
        return (is_suppressed_eucafault(fault_id));
    } else if (fault_id != NULL) {
        if (is_suppressed_eucafault(fault_id)) {
            LOGTRACE("Detected already-suppressed fault id %s\n", fault_id);
            return TRUE;
        }
        struct stat st;

        if (stat(fault_file, &st) != 0) {
            LOGWARN("stat() problem with %s: %s\n", fault_file, strerror(errno));
            return FALSE;
        }
        if (st.st_size == 0) {
            LOGINFO("Suppressing fault id %s.\n", fault_id);

            struct suppress_list *new_supp = ((struct suppress_list *)EUCA_ZALLOC(1, sizeof(struct suppress_list)));

            if (new_supp == NULL) {
                LOGERROR("struct malloc() failed in check_eucafault_suppression while adding suppressed fault %s.\n", fault_id);
                return FALSE;
            }
            if ((new_supp->id = (char *)strdup(fault_id)) == NULL) {
                LOGERROR("string malloc() failed in check_eucafault_suppression while adding suppressed fault %s.\n", fault_id);
                EUCA_FREE(new_supp);
                return FALSE;
            }
            // Insert at beginning of list.
            new_supp->next = suppressed;
            suppressed = new_supp;
            return TRUE;
        } else {
            return FALSE;
        }
    }
    LOGTRACE("returning FALSE for %s, %s\n", SP(fault_id), SP(fault_file));
    return FALSE;
}

//!
//! Retrieves an XML doc containing fault information for a given fault id.
//!
//! @param[in] faultdir
//! @param[in] fault_id the fault identifier string
//!
//! @return an XML doc containing fault information for a given fault id.
//!
//! @note Assumes--indeed REQUIRES--that fault id matches filename prefix!
//!
static xmlDoc *read_eucafault(const char *faultdir, const char *fault_id)
{
    xmlDoc *my_doc = NULL;
    char fault_file[PATH_MAX];
    static int common_block_exists = 0;

    snprintf(fault_file, PATH_MAX, "%s/%s%s", faultdir, fault_id, XML_SUFFIX);
    LOGTRACE("Attempting to load file %s\n", fault_file);

    if (get_eucafault(fault_id, NULL) != NULL) {
        LOGTRACE("Looks like fault %s already exists.\n", fault_id);
        return NULL;
    }
    if (check_eucafault_suppression(fault_id, fault_file)) {
        LOGTRACE("Looks like fault %s is being deliberately suppressed.\n", fault_id);
        return NULL;
    }
    my_doc = xmlParseFile(fault_file);

    if (my_doc == NULL) {
        LOGWARN("Could not parse file %s\n", fault_file);
        return NULL;
    } else {
        LOGTRACE("Successfully parsed file %s\n", fault_file);
    }
    if (get_eucafault(fault_id, my_doc) != NULL) {
        LOGTRACE("Found fault id %s in %s\n", fault_id, fault_file);
    } else if (get_common_block(my_doc) != NULL) {
        LOGTRACE("Found <%s> block in %s\n", COMMON_PREFIX, fault_file);
        if (common_block_exists++) {
            LOGTRACE("<%s> block already exists--skipping.\n", COMMON_PREFIX);
            xmlFreeDoc(my_doc);
            return NULL;
        }
    } else {
        LOGWARN("Did not find fault id %s in %s -- found fault id %s instead. (Not adding fault.)\n", fault_id, fault_file,
                get_fault_id(xmlFirstElementChild(xmlDocGetRootElement(my_doc))));
        xmlFreeDoc(my_doc);
        return NULL;
    }
    return my_doc;
}

//!
//! Adds XML doc for a fault to the in-memory fault registry (doc). Creates registry if
//! none exists yet.
//!
//! @param[in] new_doc
//!
//! @return TRUE if the operation was successful otherwise false is returned
//!
//! @note Can also add COMMON_PREFIX (\<common\>) block.
//!
static boolean add_eucafault(const xmlDoc * new_doc)
{
    if (xmlDocGetRootElement(ef_doc) == NULL) {
        LOGTRACE("Creating new document.\n");
        ef_doc = xmlCopyDoc((xmlDoc *) new_doc, 1); /* 1 == recursive copy */

        if (ef_doc == NULL) {
            LOGERROR("Problem creating fault registry.\n");
            return FALSE;
        }
    } else {
        LOGTRACE("Appending to existing document.\n");
        if (xmlAddNextSibling(xmlFirstElementChild(xmlDocGetRootElement(ef_doc)), xmlFirstElementChild(xmlDocGetRootElement((xmlDoc *) new_doc))) == NULL) {
            LOGERROR("Problem adding fault to existing registry.\n");
            return FALSE;
        }
    }
    return TRUE;
}

//!
//! Retrieves the fault id found in a fault XML node.
//!
//! @param[in] node a pointer to the XML node to retrieve the fault identifier from
//!
//! @return a pointer to the start of the fault identifier string or NULL if not found
//!
static char *get_fault_id(const xmlNode * node)
{
    if (node == NULL) {
        return NULL;
    }

    /*
     * Does case-insensitive string comparison on the attribute name & value.
     * (These are technically case-sensitive in XML, but I'm being
     * forgiving of minor transgressions in order to have happier users.)
     */
    if ((node->type == XML_ELEMENT_NODE) && !strcasecmp((const char *)node->name, "fault")) {

        for (xmlAttr * attr = node->properties; attr; attr = attr->next) {
            if (!strcasecmp((const char *)attr->name, "id")) {
                return (char *)attr->children->content;
            }
        }
    }
    return NULL;
}

//!
//! Retrieves a common XML node from a given XML document
//!
//! @param[in] doc pointer to an XML document containing the common block
//!
//! @return TRUE if common block exists in doc, FALSE if not.
//!
static xmlNode *get_common_block(const xmlDoc * doc)
{
    if (doc == NULL) {
        return NULL;
    }
    for (xmlNode * node = xmlFirstElementChild(xmlDocGetRootElement((xmlDoc *) doc)); node; node = node->next) {

        if (node->type == XML_ELEMENT_NODE) {
            if (!strcasecmp((const char *)node->name, COMMON_PREFIX)) {
                LOGTRACE("Found <%s> block.\n", COMMON_PREFIX);
                return node;
            }
        }
    }
    return NULL;
}

//!
//! Retrieves fault node if fault id exists in registry, NULL if not. Uses global fault
//! registry unless a non-NULL doc pointer is passed as a parameter, so it can be used
//! either to match a fault id in a single doc or to determine if a fault id already
//! exists in the overall registry.
//!
//! If NULL id supplied, simply returns the first fault node in document.
//!
//! @param[in] fault_id the fault identifier string
//! @param[in] doc pointer to the XML document
//!
//! @return a pointer to the XML fault node or NULL if none found.
//!
static xmlNode *get_eucafault(const char *fault_id, const xmlDoc * doc)
{
    // Uses global registry if no doc supplied.
    if (doc == NULL) {
        doc = ef_doc;
    }
    for (xmlNode * node = xmlFirstElementChild(xmlDocGetRootElement((xmlDoc *) doc)); node; node = node->next) {

        char *this_id = get_fault_id(node);

        if (fault_id == NULL) {
            return node;
        } else if ((this_id != NULL) && !strcasecmp(this_id, fault_id)) {
            return node;
        }
    }
    return NULL;
}

//!
//! Builds up the fault log path from supplied argument and configured directory
//! prefix. If passed NULL, tries to determine process name and uses that for building
//! up the path.
//!
//! @param[in] fileprefix
//!
//! @return TRUE if fault logfile successfully opened, FALSE otherwise (meaning logging
//!         is to console).
//!
static boolean initialize_faultlog(const char *fileprefix)
{
    char faultlogpath[PATH_MAX];
    char *fileprefix_i;

    if (fileprefix == NULL) {
        faultlog = STANDARD_FILESTREAM;
        return TRUE;
    } else if (strlen(fileprefix) == 0) {
        //! @FIXME: program_infocation_short_name is a GNU'ism and is not portable--should wrap with an autoconf check.
        snprintf(faultlogpath, PATH_MAX, EUCALYPTUS_LOG_DIR "/%s" FAULT_LOGFILE_SUFFIX, euca_base, program_invocation_short_name);
    } else {
        // Prune any leading directores from path.
        fileprefix_i = rindex(fileprefix, '/');

        if (fileprefix_i != NULL) {
            fileprefix = fileprefix_i + 1;
        }
        snprintf(faultlogpath, PATH_MAX, EUCALYPTUS_LOG_DIR "/%s" FAULT_LOGFILE_SUFFIX, euca_base, fileprefix);
    }
    LOGTRACE("Initializing faultlog using %s\n", faultlogpath);
    faultlog = fopen(faultlogpath, "a+");

    if (faultlog == NULL) {
        LOGERROR("Cannot open fault log file %s: %s\n", faultlogpath, strerror(errno));
        LOGERROR("Logging faults to the console...\n");
        faultlog = STANDARD_FILESTREAM;
        return FALSE;
    } else {
        return TRUE;
    }
}

//!
//! Builds the localized fault registry from XML files supplied in various directories.
//!
//! @param[in] fileprefix sets the filename prefix (under the configured logfile directory)
//!                       for fault logs from this process. If logfile_name is NULL, tries
//!                       to determine a filename prefix from argv[0] (program_invocation_shortname).
//!
//! @return the number of faults successfully loaded into registry. If the registry was
//!         previously loaded, returns the number of previously loaded faults as a negative
//!         number.
//!
int init_eucafaults(const char *fileprefix)
{
    struct stat dirstat;
    static int faults_loaded = 0;
    char *locale = NULL;
    static char faultdirs[NUM_FAULTDIR_TYPES][PATH_MAX];

    pthread_mutex_lock(&fault_mutex);

    if (faults_loaded) {
        LOGTRACE("Attempt to reinitialize fault registry? Skipping...\n");
        pthread_mutex_unlock(&fault_mutex);
        return -faults_loaded;         // Negative return because already loaded.
    }

    char *euca_env = getenv(EUCALYPTUS_ENV_VAR_NAME);

    if (euca_env) {
        strncpy(euca_base, euca_env, (EUCA_MAX_PATH - 1));
    }

    initialize_faultlog(fileprefix);
    LOGTRACE("Initializing fault registry directories.\n");

    if ((locale = getenv(LOCALIZATION_ENV_VAR)) == NULL) {
        LOGDEBUG("$%s not set, using default value of: %s\n", LOCALIZATION_ENV_VAR, DEFAULT_LOCALIZATION);
    }
    LIBXML_TEST_VERSION;

    /* Cycle through list of faultdirs in priority order, noting any missing. */
    if (locale != NULL) {
        snprintf(faultdirs[CUSTOM_LOCALIZED], PATH_MAX, EUCALYPTUS_CUSTOM_FAULT_DIR "/%s/", euca_base, locale);
    } else {
        faultdirs[CUSTOM_LOCALIZED][0] = 0;
    }
    snprintf(faultdirs[CUSTOM_DEFAULT_LOCALIZATION], PATH_MAX, EUCALYPTUS_CUSTOM_FAULT_DIR "/%s/", euca_base, DEFAULT_LOCALIZATION);
    if (locale != NULL) {
        snprintf(faultdirs[DISTRO_LOCALIZED], PATH_MAX, EUCALYPTUS_FAULT_DIR "/%s/", euca_base, locale);
    } else {
        faultdirs[DISTRO_LOCALIZED][0] = 0;
    }
    snprintf(faultdirs[DISTRO_DEFAULT_LOCALIZATION], PATH_MAX, EUCALYPTUS_FAULT_DIR "/%s/", euca_base, DEFAULT_LOCALIZATION);

    for (int i = 0; i < NUM_FAULTDIR_TYPES; i++) {
        if (faultdirs[i][0]) {
            if (stat(faultdirs[i], &dirstat) != 0) {
                LOGINFO("stat() problem with %s: %s\n", faultdirs[i], strerror(errno));
            } else if (!S_ISDIR(dirstat.st_mode)) {
                LOGINFO("stat() problem with %s: Not a directory. errno=%d(%s)\n", faultdirs[i], errno, strerror(errno));
            } else {
                struct dirent **namelist;
                int numfaults = scandir(faultdirs[i], &namelist, &scandir_filter, alphasort);
                if (numfaults == 0) {
                    LOGDEBUG("No faults found in %s\n", faultdirs[i]);
                } else {
                    LOGDEBUG("Found %d %s files in %s\n", numfaults, XML_SUFFIX, faultdirs[i]);
                    while (numfaults--) {
                        xmlDoc *new_fault = read_eucafault(faultdirs[i], str_trim_suffix(namelist[numfaults]->d_name, XML_SUFFIX));
                        EUCA_FREE(namelist[numfaults]);

                        if (new_fault) {
                            if (add_eucafault(new_fault)) {
                                faults_loaded++;
                            }
                            xmlFreeDoc(new_fault);
                        } else {
                            LOGTRACE("Not adding new fault--mismatch or already exists...?\n");
                        }
                    }
                    EUCA_FREE(namelist);
                }
            }
        }
    }
    pthread_mutex_unlock(&fault_mutex);
    LOGDEBUG("Loaded %d eucafault descriptions into registry.\n", faults_loaded);
    return faults_loaded;
}

//!
//! Retrieves a translated label from \<common\> block.
//!
//! @param[in] var
//!
//! @return the translated label string
//!
//! @note The pointer returned by this function must be free()'d by the  caller
//!
//! @todo Consolidate this with get_fault_id() and make some sort of general-purpose
//!       fetch function? Or make get_fault_id() more general and change this to call
//!       it?
//!
//! @todo Consolidate with get_fault_var()?
//!
static char *get_common_var(const char *var)
{
    xmlNode *c_node = NULL;

    if ((c_node = get_common_block(ef_doc)) == NULL) {
        LOGWARN("Did not find <%s> block\n", COMMON_PREFIX);
        return strdup(var);
    }
    for (xmlNode * node = xmlFirstElementChild(c_node); node; node = node->next) {
        if ((node->type == XML_ELEMENT_NODE) && !strcasecmp((const char *)node->name, "var")) {
            xmlChar *prop = xmlGetProp(node, (const xmlChar *)"name");

            if (!strcasecmp(var, (char *)prop)) {
                xmlChar *value = NULL;

                xmlFree(prop);
                value = xmlGetProp(node, (const xmlChar *)LOCALIZED_TAG);

                if (value == NULL) {
                    value = xmlGetProp(node, (const xmlChar *)"value");
                }
                return (char *)value;
            } else {
                xmlFree(prop);
            }
        }
    }
    // If nothing useful is found, return original variable-name string.
    LOGWARN("Did not find label '%s'\n", var);
    return strdup(var);
}

//!
//! Retrieves a translated label from \<fault\> block.
//!
//! @param[in] var
//! @param[in] f_node
//!
//! @return  a translated label string
//!
//! @note The pointer returned by this function must be free()'d by the caller.
//!
//! @todo Consolidate with get_common_var()?
//!
//! @todo Gosh this looks messy.
//!
static char *get_fault_var(const char *var, const xmlNode * f_node)
{
    if ((f_node == NULL) || (var == NULL)) {
        LOGWARN("called with one or more NULL arguments.\n");
        return NULL;
    }
    // Just in case we're matching the top-level node.
    //! @FIXME Move this into a new tmfunction to eliminate repeated logic below?
    if ((f_node->type == XML_ELEMENT_NODE) && !strcasecmp((const char *)f_node->name, var)) {
        xmlChar *value = xmlGetProp((xmlNode *) f_node,
                                    (const xmlChar *)LOCALIZED_TAG);
        if (value == NULL) {
            value = xmlGetProp((xmlNode *) f_node, (const xmlChar *)MESSAGE_TAG);
        }
        // This is a special (parent) case, so it doesn't handle
        // message/localized text in a child node.
        return (char *)value;
    }
    for (xmlNode * node = xmlFirstElementChild((xmlNode *) f_node); node; node = node->next) {
        if ((node->type == XML_ELEMENT_NODE) && !strcasecmp((const char *)node->name, var)) {
            xmlChar *value = xmlGetProp(node, (const xmlChar *)LOCALIZED_TAG);

            if (value == NULL) {
                value = xmlGetProp(node, (const xmlChar *)MESSAGE_TAG);
            }
            if (value == NULL) {
                // May be a child node, e.g. for "resolution"
                for (xmlNode * subnode = xmlFirstElementChild(node); subnode; subnode = subnode->next) {
                    if ((node->type == XML_ELEMENT_NODE) && !strcasecmp((const char *)subnode->name, LOCALIZED_TAG)) {
                        return (char *)xmlNodeGetContent(subnode);
                    }
                }
                //! @FIXME More elegant method than another list walk?
                for (xmlNode * subnode = xmlFirstElementChild(node); subnode; subnode = subnode->next) {
                    if ((node->type == XML_ELEMENT_NODE) && !strcasecmp((const char *)subnode->name, MESSAGE_TAG)) {
                        return (char *)xmlNodeGetContent(subnode);
                    }
                }
            }
            return (char *)value;
        }
    }
    LOGWARN("Did not find <%s> message in get_fault_var().\n", var);
    return NULL;
}

//!
//! Formats fault-log output and sends to fault log (or stdout/stderr).
//!
//! @param[in] fault_id the fault identifier string
//! @param[in] map
//!
//! @return TRUE if the operation was successful otherwise FALSE is returned.
//!
static boolean format_eucafault(const char *fault_id, const char_map ** map)
{
    static int max_label_len = 0;
    char *fault_var = NULL;
    time_t secs;
    struct tm lt;
    xmlNode *fault_node = get_eucafault(fault_id, NULL);

    if (fault_node == NULL) {
        LOGERROR("Fault %s detected, could not find fault id in registry.\n", fault_id);
        return FALSE;
    }
    // Determine label alignment. (Only needs to be done once.)
    if (!max_label_len) {
        for (int i = 0; fault_labels[i]; i++) {
            int label_len = 0;
            int w_label_len = 0;

            char *label = get_common_var(fault_labels[i]);
            label_len = strlen(label);

            w_label_len = utf8_to_wchar(label, label_len, NULL, 0, 0);
            EUCA_FREE(label);
            if (w_label_len > max_label_len) {
                max_label_len = w_label_len;
            }
        }
    }
    // Top border.
    fprintf(faultlog, "%s\n", STARS);

    // Get time.
    secs = time(NULL);
    if (localtime_r(&secs, &lt) == NULL) {
        // Someone call Dr. Who.
        lt.tm_year = lt.tm_mon = lt.tm_mday = 0;
        lt.tm_hour = lt.tm_min = lt.tm_sec = 0;
    } else {
        // Account for implied offsets in struct.
        lt.tm_year += 1900;
        lt.tm_mon += 1;
    }
    // Construct timestamped fault header.
    fprintf(faultlog, "  ERR-%s %04d-%02d-%02d %02d:%02d:%02dZ ", fault_id, lt.tm_year, lt.tm_mon, lt.tm_mday, lt.tm_hour, lt.tm_min, lt.tm_sec);

    if ((fault_var = get_fault_var("fault", fault_node)) != NULL) {
        char *fault_subbed = NULL;

        if ((fault_subbed = c_varsub(fault_var, map)) != NULL) {
            fprintf(faultlog, "%s\n\n", fault_subbed);
        } else {
            fprintf(faultlog, "%s\n\n", fault_var);
        }
        EUCA_FREE(fault_subbed);
        EUCA_FREE(fault_var);
    } else {
        char *common_var = get_common_var("unknown");
        fprintf(faultlog, "%s\n\n", common_var);
        EUCA_FREE(common_var);
    }

    // Construct fault information lines.
    for (int i = 0; fault_labels[i]; i++) {
        int w_common_var_len = 0;
        int common_var_len = 0;
        int padding = 0;
        char *common_var = get_common_var(fault_labels[i]);

        common_var_len = strlen(common_var);
        w_common_var_len = utf8_to_wchar(common_var, common_var_len, NULL, 0, 0);
        padding = max_label_len - w_common_var_len + 1;
        fprintf(faultlog, "%s%*s %s: ", BARS, padding, " ", common_var);
        EUCA_FREE(common_var);

        if ((fault_var = get_fault_var(fault_labels[i], fault_node)) != NULL) {
            char *fault_subbed = NULL;

            if ((fault_subbed = c_varsub(fault_var, map)) != NULL) {
                fprintf(faultlog, "%s", fault_subbed);
            } else {
                fprintf(faultlog, "%s", fault_var);
            }
            EUCA_FREE(fault_subbed);
            EUCA_FREE(fault_var);
        } else {
            common_var = get_common_var("unknown");
            fprintf(faultlog, "%s", common_var);
            EUCA_FREE(common_var);
        }
        fprintf(faultlog, "\n");
    }
    // Bottom border.
    fprintf(faultlog, "%s\n\n", STARS);
    fflush(faultlog);
    return TRUE;
}

//!
//! Checks if this fault has already been logged with the same set of parameters.
//!
//! @param[in] fault_id the fault identifier string
//! @param[in] vars
//!
//! @return TRUE if successful, FALSE otherwise.
//!
boolean is_redundant_eucafault(const char *fault_id, const char_map ** vars)
{
    // just concatenate everything together: fault_id+key1+val1+key2...
    char *new = strdup(fault_id);
    for (int i = 0; vars && vars[i] != NULL; i++) {
        const char_map *v = vars[i];
        new = euca_strdupcat(new, v->key);
        new = euca_strdupcat(new, v->val);
    }

    //! see if it is already in our linked list (@TODO switch to a more efficient data structure)
    for (struct suppress_list * s = redundant; s != NULL; s = s->next) {
        char *old = s->id;

        // compare strings ourselves (instead of strcmp) so we can bail early
        int i;
        for (i = 0; new[i] != '\0' && old[i] != '\0'; i++)
            if (new[i] != old[i])
                break;                 // not the same string

        if (new[i] == '\0' && old[i] == '\0') { // found the match in the LL
            EUCA_FREE(new);
            return TRUE;
        }
    }

    // was not found, so add it to the list
    struct suppress_list *s = EUCA_ZALLOC(1, sizeof(struct suppress_list));
    s->id = new;
    if (redundant == NULL) {
        redundant = s;
    } else {
        redundant->next = s;
    }

    return FALSE;
}

//!
//! Logs a fault, initializing the fault registry, if necessary. Will call init_eucafaults()
//! internally to ensure fault registry has been loaded.
//!
//! @param[in] fault_id the fault identifier string
//! @param[in] map a set of param/paramText key/value pairs in struct form as defined
//!                in wc.h and assembled using c_varmap_alloc()
//!
//! @return TRUE if fault successfully logged, FALSE otherwise.
//!
//! @see format_eucafault()
//!
boolean log_eucafault_map(const char *fault_id, const char_map ** map)
{
    int load = init_eucafaults(NULL);

    LOGTRACE("init_eucafaults() returned %d\n", load);

    if (is_suppressed_eucafault(fault_id)) {
        LOGDEBUG("Fault %s detected, but it is being actively suppressed.\n", fault_id);
        return (FALSE);
    }

    if (is_redundant_eucafault(fault_id, map)) {
        LOGDEBUG("Fault %s detected, but it has already been logged.\n", fault_id);
        return (FALSE);
    }
    return (format_eucafault(fault_id, map));
}

//!
//! Logs a fault, initializing the fault registry, if necessary. Will call init_eucafaults()
//! internally to ensure fault registry has been loaded.
//!
//! @param[in] fault_id the fault identifier string
//! @param[in] ... the variable argument part of the call
//!
//! @return the number of substitution parameters it was called with, returning it as a
//!         negative number if the underlying log_eucafault_map() call returned FALSE.
//!
//! @see log_eucafault_map()
//! @see init_eucafaults()
//!
//! @note Note that the final NULL argument is very important! (...because va_arg() is stupid.)
//!
int log_eucafault(const char *fault_id, ...)
{
    va_list argv;
    char *token[2] = { NULL };
    char_map **m = NULL;
    int count = 0;
    int load = init_eucafaults(NULL);

    LOGTRACE("init_eucafaults() returned %d\n", load);
    va_start(argv, fault_id);
    {
        while ((token[count % 2] = va_arg(argv, char *)) != NULL) {
            ++count;
            if ((count % 2) == 0)
                m = c_varmap_alloc(m, token[0], token[1]);
        }
    }
    va_end(argv);

    if (count % 2) {
        LOGWARN("called with an odd (unmatched) number of substitution parameters: %d\n", count);
    }

    if (!log_eucafault_map(fault_id, ((const char_map **)m))) {
        LOGTRACE("got FALSE from log_eucafault_map()\n");
        count *= -1;
    }

    c_varmap_free(m);
    return (count);
}

#ifdef _UNIT_TEST
//!
//! Performs blind dump of XML fault registry to stdout.
//!
static void dump_eucafaults_db(void)
{
    printf("\n");
    xmlDocDump(stdout, ef_doc);
    printf("\n");
}

//!
//! Prints simple usage message.
//!
//! @param[in] argv0 a string containing the name of the application
//!
static void usage(const char *argv0)
{
    fprintf(stderr, "Usage: %s [-d] fault-id [param1 param1Value] [param2 param2Value] [...]\n", argv0);
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
    int dump = 0;
    int opt;

    //setlocale (LC_ALL, "en_US.utf-8");

    while ((opt = getopt(argc, argv, "d")) != -1) {
        switch (opt) {
        case 'd':
            dump++;
            break;
        default:
            usage(argv[0]);
            return 1;
        }
    }
    // NULL forces init_eucafaults()'s call to
    // initialize_faultlog() to guess at process name for creating
    // logfile.
    opt = init_eucafaults(NULL);

    LOGDEBUG("init_eucafaults() returned %d\n", opt);

    if (optind < argc) {
        char_map **m = c_varmap_alloc(NULL, "daemon", "Balrog");
        m = c_varmap_alloc(m, "hostIp", "127.0.0.1");
        m = c_varmap_alloc(m, "brokerIp", "127.0.0.2");
        m = c_varmap_alloc(m, "endpointIp", "127.0.0.3");
        LOGTRACE("argv[1st of %d]: %s\n", argc - optind, argv[optind]);
        log_eucafault_map(argv[optind], (const char_map **)m);
        c_varmap_free(m);

        // Now log to stdout for the remainder of the test.
        faultlog = stdout;

        // Reusing & abusing opt. :)
        opt = log_eucafault(argv[optind], "daemon", "Balrog", "hostIp", "127.0.0.1", "brokerIp", "127.0.0.2", "endpointIp", "127.0.0.3", "unmatched!", NULL);
        LOGDEBUG("log_eucafault args returned: %d\n", opt);

        // This allows substitution-argument pairs for unit test to be
        // passed in on command line.
        m = NULL;
        for (opt = optind + 1; opt < argc; opt++) {
            LOGTRACE("argv[opt]: %s\n", argv[opt]);
            if ((opt - optind + 1) % 2) {
                LOGTRACE("...now have two, calling c_varmap_alloc()\n");
                m = c_varmap_alloc(m, argv[opt - 1], argv[opt]);
            }
        }
        if (m != NULL) {
            log_eucafault_map(argv[optind], (const char_map **)m);
            c_varmap_free(m);
        } else {
            log_eucafault_map(argv[optind], NULL);
        }
        log_eucafault(argv[optind], NULL);  // Deliberately call w/NULL.
    }
    if (dump) {
        dump_eucafaults_db();
    }
    if (optind >= argc) {
        usage(argv[0]);
        return 1;
    }
    return 0;
}
#endif /* _UNIT_TEST */

#ifdef EUCA_GENERATE_FAULT
//!
//! Prints simple usage message.
//!
//! @param[in] argv0 a string containing the name of the application
//!
static void usage(const char *argv0)
{
    fprintf(stderr, "Usage: %s [-c component-name] fault-id [param-1 value-1] [param-2 value-1] ...\n", argv0);
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
    drop_privs();                      // become 'eucalyptus' so log file is created with the right privs
    log_params_set(EUCA_LOG_WARN, 0, 0);    // set log level
    log_prefix_set("%L");              // only print log level
    log_file_set(NULL);                // log output goes to STANDARD_FILESTREAM

    char *component = NULL;
    int opt;
    while ((opt = getopt(argc, argv, "c:")) != -1) {
        switch (opt) {
        case 'c':
            component = optarg;
            break;
        case 'h':
        default:
            usage(argv[0]);
            return 1;
        }
    }

    if (argv[optind] == NULL) {
        LOGERROR("no fault ID is specified (try -h for usage)\n");
        return 1;
    }
    int ndigits = 0;
    for (char *c = argv[optind]; *c != '\0'; c++, ndigits++) {
        if (!isdigit(*c)) {
            LOGERROR("invalid fault ID (must be a number)\n");
            return 1;
        }
    }
    if (ndigits < 4) {
        LOGERROR("invalid fault ID (must be a 4-digit number)\n");
        return 1;
    }
    int nfaults = init_eucafaults(component);
    if (nfaults < 1) {
        LOGERROR("failed to locate fault information (is $EUCALYPTUS set?)\n");
        return 1;
    }
    // place variable-and-value pairs from command line into the map
    char_map **m = NULL;
    for (int opt = optind + 1; opt < argc; opt++) {
        LOGDEBUG("argv[opt]: %s\n", argv[opt]);
        if ((opt - optind + 1) % 2) {
            LOGDEBUG("...now have two, calling c_varmap_alloc()\n");
            m = c_varmap_alloc(m, argv[opt - 1], argv[opt]);
        }
    }

    int ret = 0;
    if (log_eucafault_map(argv[optind], (const char_map **)m) == FALSE) {
        ret = 1;
    }
    if (m)
        c_varmap_free(m);

    return ret;
}
#endif /* EUCA_GENERATE_FAULT */
