// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
Copyright (c) 2012  Eucalyptus Systems, Inc.

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
#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <dirent.h>
#include <errno.h>
#include <limits.h>
#include <pthread.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <wchar.h>
#include <sys/stat.h>

#include <libxml/parser.h>
#include <libxml/tree.h>

#include <misc.h>
#include "fault.h"

/*
 * These definitions are all easily customized.
 * FIXME: Make some or all of them configuration-file options?
 */
#define DISTRO_FAULTDIR "/usr/share/eucalyptus/faults"
#define CUSTOM_FAULTDIR "/etc/eucalyptus/faults"
#define DEFAULT_LOCALIZATION "en_US"
#define LOCALIZATION_ENV_VAR "LOCALE"
#define XML_SUFFIX ".xml"

/*
 * This is the order of priority (highest to lowest) for fault messages
 * wrt customization & localization.
 *
 * Once a fault id has been found and added to the in-memory repository,
 * all further occurrences of that fault in lower-priority faultdirs
 * will be ignored. This is how we set the customization/localization
 * pecking order.
 */
enum faultdir_types {
    CUSTOM_LOCALIZED,
    CUSTOM_DEFAULT_LOCALIZATION,
    DISTRO_LOCALIZED,
    DISTRO_DEFAULT_LOCALIZATION,
    NUM_FAULTDIR_TYPES,         /* For iterating. */
};

/*
 * Shared data
 */
static int faults_initialized = 0;
static char faultdirs[NUM_FAULTDIR_TYPES][PATH_MAX];

/*
 * This holds the in-memory model of the fault database.
 */
static xmlDoc *ef_doc = NULL;

// FIXME: Thread safety is only half-baked at this point.
static pthread_mutex_t fault_mutex = PTHREAD_MUTEX_INITIALIZER;

/*
 * Function prototypes
 */
static xmlDoc *get_eucafault (const char *, const char *);
static char *get_fault_id (xmlNode *);
static int fault_id_exists (const char *, xmlDoc *);
static int scandir_filter (const struct dirent *);
static int str_end_cmp (const char *, const char *);
static char *str_trim_suffix (const char *, const char *);
static int add_eucafault (xmlDoc *);
// Can be linked externally for debugging. Use -D_UNIT_TEST
void dump_eucafaults_db (void);

/*
 * Utility functions -- move to misc.c?
 * (Might some of them already be there in one form or another?)
 */

/*
 * Utility function:
 * Compares end of one string to another string (the suffix) for a match.
 *
 */
static int
str_end_cmp (const char *str, const char *suffix)
{
    if (!str || !suffix) {
        return 0;
    }
    size_t lenstr = strlen (str);
    size_t lensuffix = strlen (suffix);

    if (lensuffix >  lenstr) {
        return 0;
    }
    return strncmp (str + lenstr - lensuffix, suffix, lensuffix) == 0;
}

/*
 * Utility function:
 * Trims end of string off if it matches a supplied suffix.
 */
static char *
str_trim_suffix (const char *str, const char *suffix)
{
    if (!str || !suffix || !str_end_cmp (str, suffix)) {
        return (char *)str;
    }
    return strndup (str, strlen (str) - strlen (suffix));
}

/*
 * Utility function:
 * Used internally by scandir() to match filenames by suffix.
 */
static int
scandir_filter (const struct dirent *entry)
{
    return str_end_cmp (entry->d_name, XML_SUFFIX);
}

/*
 * End utility functions.
 */

/*
 * Return an XML doc containing fault information for a given fault id.
 * ASSUMES FAULT ID MATCHES FILENAME!
 */
static xmlDoc *
get_eucafault (const char * faultdir, const char * fault_id)
{
    xmlDoc *my_doc = NULL;
    char fault_file[PATH_MAX];

    PRINTF(("Getting fault %s\n", fault_id));

    if (fault_id_exists(fault_id, NULL)) {
        PRINTF(("(...looks like fault %s already exists?)\n", fault_id));
        return NULL;
    }
    snprintf (fault_file, PATH_MAX - 1, "%s/%s%s", faultdir, fault_id,
              XML_SUFFIX);
    my_doc = xmlParseFile (fault_file);

    if (my_doc == NULL) {
        PRINTF(("Could not parse file %s in get_eucafault()\n",
                fault_file));
        return NULL;
    } else {
        PRINTF(("Successfully parsed file %s in get_eucafault()\n",
                fault_file));
    }
    if (fault_id_exists (fault_id, my_doc)) {
        PRINTF(("Found fault id %s in %s\n", fault_id, fault_file));
    } else {
        PRINTF(("Did NOT find fault id %s in %s\n", fault_id, fault_file));
        PRINTF(("Found fault id %s instead.\n",
                get_fault_id (xmlFirstElementChild(xmlDocGetRootElement(my_doc)))));
        PRINTF(("NOT ADDING FAULT!\n"));
        return NULL;
    }
    return my_doc;
}

/*
 * Adds XML doc for a fault to the in-memory fault model (doc).
 * Creates model if none exists yet.
 */
static int
add_eucafault (xmlDoc *new_doc)
{
    if (xmlDocGetRootElement (ef_doc) == NULL) {
        PRINTF(("Creating new document.\n"));
        ef_doc = xmlCopyDoc (new_doc, 1); /* 1 == recursive copy */
        // FIXME: Add error check/return here.
    } else {
        PRINTF(("Appending to existing document.\n"));
        if (xmlAddNextSibling (xmlFirstElementChild (xmlDocGetRootElement (ef_doc)),
                               xmlFirstElementChild (xmlDocGetRootElement (new_doc))) == NULL) {
            PRINTF(("*** Problem appending!"));
            return -1;
        }
    }
    return 0;
}

/* 
 * Returns the fault id found in a fault node.
 */
static char *
get_fault_id (xmlNode *node)
{
    if (node == NULL) {
        return NULL;
    }
    /*
     * Does case-insensitive string comparison on the attribute name & value.
     * (These are technically case-sensitive in XML, but I'm being
     * forgiving of minor transgressions in order to have happier users.)
     */
    if (node->type == XML_ELEMENT_NODE) {
        // FIXME: Double-check that this is a <fault> tag?
        for (xmlAttr *attr = node->properties; attr; attr = attr->next) {
            if (!strcasecmp((const char *)attr->name, "id")) {
                return (char *)attr->children->content;
            }
        }
    }
    return NULL;
}
    
/*
 * Returns true (1) if fault id already exists in model, false (0) if
 * not.
 */
static int
fault_id_exists (const char *id, xmlDoc *doc)
{
    /* 
     * Uses global model if no doc supplied.
     */
    if (doc == NULL) {
        doc = ef_doc;
    }
    for (xmlNode *node = xmlFirstElementChild(xmlDocGetRootElement(doc));
         node; node = node->next) {
        char *this_id = get_fault_id (node);

        if ((this_id != NULL) && !strcasecmp (this_id, id)) {
            return 1;
        }
    }
    return 0;
}

/*
 * EXTERNAL ENTRY POINT
 * (Though disabled in fault.h unless _UNIT_TEST is #define'd)
 *
 * Performs blind dump of XML fault model to stdout.
 */
void
dump_eucafaults_db (void)
{
    // FIXME: add some stats?
    printf("\n");
    xmlDocDump(stdout, ef_doc);
    printf("\n");
}

/*
 * EXTERNAL ENTRY POINT
 *
 * Builds the localized fault database from XML files supplied in
 * various directories.
 */
int
initialize_eucafaults (void)
{
    struct stat dirstat;
    int populate = 0;           /* FIXME: Use or delete. */
    char *locale = NULL;

    pthread_mutex_lock(&fault_mutex);

    if (faults_initialized) {
        PRINTF(("Attempt to reinitialize fault registry? Skipping...\n"));
        pthread_mutex_unlock(&fault_mutex);
        return 0;
    }
    PRINTF(("Initializing fault registry directories.\n"));
    if ((locale = getenv (LOCALIZATION_ENV_VAR)) == NULL) {
        PRINTF(("$LOCALE not set, using only default LOCALE of %s\n",
                DEFAULT_LOCALIZATION));
    }
    LIBXML_TEST_VERSION;

    /* Cycle through list of faultdirs in priority order, noting any missing. */
    if (locale != NULL) {
        snprintf (faultdirs[CUSTOM_LOCALIZED], PATH_MAX - 1, "%s/%s/",
                  CUSTOM_FAULTDIR, locale);
    } else {
        faultdirs[CUSTOM_LOCALIZED][0] = 0;
    }
    snprintf (faultdirs[CUSTOM_DEFAULT_LOCALIZATION], PATH_MAX - 1, "%s/%s/",
              CUSTOM_FAULTDIR, DEFAULT_LOCALIZATION);
    if (locale != NULL) {
        snprintf (faultdirs[DISTRO_LOCALIZED], PATH_MAX - 1, "%s/%s/",
                  DISTRO_FAULTDIR, locale);
    } else {
        faultdirs[DISTRO_LOCALIZED][0] = 0;
    }
    snprintf (faultdirs[DISTRO_DEFAULT_LOCALIZATION], PATH_MAX - 1, "%s/%s/",
              DISTRO_FAULTDIR, DEFAULT_LOCALIZATION);

    /* Not really sure how useful this is or will be. */
    for (int i = 0; i < NUM_FAULTDIR_TYPES; i++) {
        if (faultdirs[i][0]) {
            if (stat(faultdirs[i], &dirstat) != 0) {
                PRINTF(("*** Problem with %s:\n", faultdirs[i]));
                PRINTF(("    stat(): %s\n", strerror (errno)));
            } else if (!S_ISDIR(dirstat.st_mode)) {
                PRINTF(("*** Problem with %s:\n", faultdirs[i]));
                PRINTF(("    Not a directory\n"));
            } else {
                struct dirent **namelist;
                int numfaults = scandir (faultdirs[i], &namelist, &scandir_filter,
                                         alphasort);
                if (numfaults == 0) {
                    PRINTF(("*** No faults found in %s\n", faultdirs[i]));
                } else {
                    PRINTF(("... found %d faults in %s\n", numfaults, faultdirs[i]));

                    while (numfaults--) {
                        xmlDoc *new_fault = get_eucafault (faultdirs[i], str_trim_suffix (namelist[numfaults]->d_name, XML_SUFFIX));

                        if (new_fault) {
                            add_eucafault (new_fault);
                            xmlFreeDoc(new_fault);
                        } else {
                            PRINTF(("(...not adding new fault--mismatch or already exists?)\n"));
                        }
                    }
                }
            }
        }
    }
    faults_initialized++;       /* Not a counter--only a true/false flag */
    pthread_mutex_unlock(&fault_mutex);

    return populate;            /* FIXME: Doesn't yet return population! */
}

/*
 * EXTERNAL ENTRY POINT
 *
 * Logs a fault, initializing the fault model, if necessary.
 */
int
log_eucafault (char *fault_id, ...)
{
    va_list argv;
    char *token;
    int count = 0;

    initialize_eucafaults ();

    va_start (argv, fault_id);

    while((token = va_arg (argv, char *)) != NULL) {
        ++count;
    }
    va_end(argv);

    if (fault_id_exists(fault_id, NULL)) {
        printf ("\nFound fault %s.\n", fault_id);
    } else {
        printf ("\nCould not find fault %s.\n", fault_id);
    }
    return count;               /* FIXME: Just return void instead? */
}
