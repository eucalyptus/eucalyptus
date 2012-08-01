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
#define COMMON_PREFIX "common"

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
 * For output formatting
 */
#define STARS "************************************************************************"
#define BARS "|"

/*
 * Defines the order of labels in fault log entries.
 */
static char *fault_labels[] = { "condition",
                                "cause",
                                "initiator",
                                "location",
                                "resolution",
                                0,
};

/*
 * NO USER-SERVICEABLE PARTS BEYOND THIS POINT
 */

/*
 * Shared data
 */

// This holds the in-memory model of the fault database.
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
static xmlNode *get_common_block (xmlDoc *);
static char *get_common_var (const char *);
static void format_eucafault (const char *, ...);

/*
 * Utility functions -- move to misc.c?
 * (Might some of them already be there in one form or another?)
 */

/*
 * Utility function:
 * Compares end of one string to another string (the suffix) for a match.
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
    static int common_block_exists = 0; /* FIXME: I don't like this handling. */

    snprintf (fault_file, PATH_MAX - 1, "%s/%s%s", faultdir, fault_id,
              XML_SUFFIX);
    PRINTF (("Attempting to load file %s\n", fault_file));

    if (fault_id_exists (fault_id, NULL)) {
        PRINTF (("Looks like fault %s already exists.\n", fault_id));
        return NULL;
    }
    my_doc = xmlParseFile (fault_file);

    if (my_doc == NULL) {
        logprintfl (EUCAWARN, "Could not parse file %s in get_eucafault()\n",
                   fault_file);
        return NULL;
    } else {
        PRINTF1 (("Successfully parsed file %s in get_eucafault()\n",
                fault_file));
    }
    if (fault_id_exists (fault_id, my_doc)) {
        PRINTF (("Found fault id %s in %s\n", fault_id, fault_file));
    } else if (get_common_block (my_doc) != NULL) {
        PRINTF (("Found <%s> block in %s\n", COMMON_PREFIX, fault_file));
        if (common_block_exists++) {
            PRINTF (("<%s> block already exists--skipping.\n", COMMON_PREFIX));
            return NULL;
        }
    } else {
        logprintfl (EUCAWARN, "Did not find fault id %s in %s -- found fault id %s instead. (Not adding fault.)\n", fault_id, fault_file, get_fault_id (xmlFirstElementChild (xmlDocGetRootElement (my_doc))));
        return NULL;
    }
    return my_doc;
}

/*
 * Adds XML doc for a fault to the in-memory fault model (doc).
 * Creates model if none exists yet.
 * 
 * Can also add COMMON_PREFIX (<common>) block.
 */
static int
add_eucafault (xmlDoc *new_doc)
{
    if (xmlDocGetRootElement (ef_doc) == NULL) {
        PRINTF1 (("Creating new document.\n"));
        ef_doc = xmlCopyDoc (new_doc, 1); /* 1 == recursive copy */
        // FIXME: Add error check/return here.
    } else {
        PRINTF1 (("Appending to existing document.\n"));
        if (xmlAddNextSibling (xmlFirstElementChild (xmlDocGetRootElement (ef_doc)),
                               xmlFirstElementChild (xmlDocGetRootElement (new_doc))) == NULL) {
            // FIXME: Add more diagnostic information to this error message.
            logprintfl (EUCAERROR, "Problem appending to fault database.\n");
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
    if ((node->type == XML_ELEMENT_NODE) &&
        !strcasecmp ((const char *)node->name, "fault")) {

        for (xmlAttr *attr = node->properties; attr; attr = attr->next) {
            if (!strcasecmp ((const char *)attr->name, "id")) {
                return (char *)attr->children->content;
            }
        }
    }
    return NULL;
}
/*
 * Returns true (1) if common block exists in doc, false (0) if not.
 */
static xmlNode *
get_common_block (xmlDoc *doc)
{
    if (doc == NULL) {
        return 0;
    }
    for (xmlNode *node = xmlFirstElementChild (xmlDocGetRootElement (doc));
         node; node = node->next) {
        if (node->type == XML_ELEMENT_NODE) {
            if (!strcasecmp ((const char *)node->name, COMMON_PREFIX)) {
                PRINTF1 (("Found <%s> block.\n", COMMON_PREFIX));
                return node;
            }
        }
    }
    return NULL;
}


/*
 * Returns true (1) if fault id exists in model, false (0) if not.
 *
 * Uses global fault model unless a non-NULL doc pointer is passed as a
 * parameter, so it can be used either to match a fault id in a single
 * doc or to determine if a fault id already exists in the overall
 * model.
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
    for (xmlNode *node = xmlFirstElementChild (xmlDocGetRootElement (doc));
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
    static int faults_initialized = 0;
    static char faultdirs[NUM_FAULTDIR_TYPES][PATH_MAX];

    pthread_mutex_lock (&fault_mutex);

    if (faults_initialized) {
        PRINTF1 (("Attempt to reinitialize fault registry? Skipping...\n"));
        pthread_mutex_unlock (&fault_mutex);
        return 0; 
    }
    PRINTF (("Initializing fault registry directories.\n"));
    if ((locale = getenv (LOCALIZATION_ENV_VAR)) == NULL) {
        logprintfl (EUCAINFO, 
                   "$LOCALE not set, using default $LOCALE of %s\n",
                   DEFAULT_LOCALIZATION);
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
            if (stat (faultdirs[i], &dirstat) != 0) {
                logprintfl (EUCAWARN, "stat() problem with %s: %s\n",
                           faultdirs[i], strerror (errno));
            } else if (!S_ISDIR (dirstat.st_mode)) {
                logprintfl (EUCAWARN,
                           "stat() problem with %s: Not a directory\n",
                           faultdirs[i], strerror (errno));
            } else {
                struct dirent **namelist;
                int numfaults = scandir (faultdirs[i], &namelist, &scandir_filter,
                                         alphasort);
                if (numfaults == 0) {
                    PRINTF (("No faults found in %s\n", faultdirs[i]));
                } else {
                    PRINTF (("Found %d %s files in %s\n", numfaults, XML_SUFFIX,
                            faultdirs[i]));
                    while (numfaults--) {
                        xmlDoc *new_fault = get_eucafault (faultdirs[i], str_trim_suffix (namelist[numfaults]->d_name, XML_SUFFIX));

                        if (new_fault) {
                            add_eucafault (new_fault);
                            xmlFreeDoc (new_fault);
                        } else {
                            PRINTF1 (("Not adding new fault--mismatch or already exists...?\n"));
                        }
                    }
                }
            }
        }
    }
    faults_initialized++;       /* Not a counter--only a true/false flag */
    pthread_mutex_unlock (&fault_mutex);

    return populate;            /* FIXME: Doesn't yet return population! */
}

/*
 * Retrieves a translated label from <common> block.
 *
 * FIXME: Consolidate this with get_fault_id() and make some sort of
 * general-purpose fetch function? Or make get_fault_id() more general
 * and change this to call it?
 * 
 * FIXME: LEAKS MEMORY!!!
 */
static char *
get_common_var (const char *var)
{
    xmlNode *c_node = NULL;

    if ((c_node = get_common_block (ef_doc)) == NULL) {
        logprintfl (EUCAWARN, "Did not find <%s> block\n", COMMON_PREFIX);
        return (char *)var;
    }
    for (xmlNode *node = xmlFirstElementChild (c_node); node;
         node = node->next) {
        if ((node->type == XML_ELEMENT_NODE) && 
            !strcasecmp ((const char *)node->name, "var")) {
            xmlChar *prop = xmlGetProp (node, (const xmlChar *)"name");

            if (!strcasecmp (var, (char *)prop)) {
                xmlChar *value = NULL;

                xmlFree (prop);
                value = xmlGetProp (node, (const xmlChar *)"localized");

                if (value == NULL) {
                    value = xmlGetProp (node, (const xmlChar *)"value");
                } 
                return (char *)value;
            } else {
                xmlFree (prop);
            }
        }
    }
    // If nothing useful is found, return original variable-name string.
    logprintfl (EUCAWARN, "Did not find label '%s'\n", var);
    return (char *)var;
}

/*
 * Formats fault-log output.
 *
 * FIXME: This walks the common block more than once.
 * FIXME: This doesn't handle wchar output yet--output is misaligned!
 */
static void
format_eucafault (const char *fault_id, ...)
{
    static FILE *logfile = NULL;
    static int max_label_len = 0;

    if (logfile == NULL) {
        logfile = stdout;
    }
    // Determine alignment (but only once)
    if (!max_label_len) {
        for (int i = 0; fault_labels[i]; i++) {
            int this_label_len = strlen (get_common_var (fault_labels[i]));
            if (this_label_len > max_label_len) {
                max_label_len = this_label_len;
            }
        }
    }

    // Now spit it out!
    fprintf (logfile, "%s\n", STARS);

    for (int i = 0; fault_labels[i]; i++) {
        fprintf (logfile, "%s %*s:", BARS, max_label_len,
                 get_common_var (fault_labels[i]));
        fprintf (logfile, "\n");
    }

    fprintf (logfile, "%s\n\n", STARS);
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

    while ((token = va_arg (argv, char *)) != NULL) {
        ++count;
    }
    va_end (argv);

    if (fault_id_exists (fault_id, NULL)) {
        format_eucafault (fault_id);
    } else {
        logprintfl (EUCAERROR,
                    "Fault %s detected, could not find fault id in registry.\n",
                    fault_id);
    }
    return count;               /* FIXME: Just return void instead? */
}

/*
 * Provides a way to log test faults from shell command line.
 */
#ifdef _UNIT_TEST
/*
 * Performs blind dump of XML fault model to stdout.
 */
static void
dump_eucafaults_db (void)
{
    // FIXME: add some stats?
    printf ("\n");
    xmlDocDump (stdout, ef_doc);
    printf ("\n");
}

int main (int argc, char ** argv)
{
    int dump = 0;
    int opt;

    while ((opt = getopt (argc, argv, "d")) != -1) {
        switch (opt) {
        case 'd':
            dump++;
            break;
        default:
            fprintf (stderr, "Usage: %s [-d] [fault id] [param1 param1Value] [...]\n", argv[0]);
            return 1;
        }
    }
    initialize_eucafaults ();

    if (optind < argc) {
        PRINTF1 (("argv[1st]: %s\n", argv[optind]));
        log_eucafault (argv[optind], NULL); /* FIXME: Add passing some parameters. */
    }
    if (dump) {
        dump_eucafaults_db ();
    }
    return 0;
}
#endif // _UNIT_TEST
