// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <assert.h>
#include <dirent.h>
#include <errno.h>
#include <limits.h>
#include <locale.h>
#include <pthread.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <wchar.h>
#include <sys/stat.h>

#include <libxml/parser.h>
#include <libxml/tree.h>

#include <eucalyptus.h>
#include <misc.h>
#include <fault.h>
#include <wc.h>
#include <utf8.h>

/*
 * These definitions are all easily customized.
 *
 * FIXME: Make some or all of them configuration-file options?
 * FIXME: Make paths relative to some configurable base directory?
 */
#define FAULTLOGDIR "/var/log/eucalyptus"
#define DISTRO_FAULTDIR "/usr/share/eucalyptus/faults"
#define CUSTOM_FAULTDIR "/etc/eucalyptus/faults"
#define DEFAULT_LOCALIZATION "en_US"
#define LOCALIZATION_ENV_VAR "LOCALE"
#define XML_SUFFIX ".xml"
#define COMMON_PREFIX "common"  /* For .xml file defining common fault
                                   labels */
#define LOCALIZED_TAG "localized"
#define MESSAGE_TAG "message"

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
#define BARS ""                 /* I want to remove these little monsters! */

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

// This holds the in-memory model of the fault registry.
static xmlDoc *ef_doc = NULL;

// Fault log filehandle.
static FILE *faultlog = NULL;

// Global equivalent to argv[0]'s basename.
extern char *program_invocation_short_name;

// FIXME: Thread safety is only half-baked at this point.
static pthread_mutex_t fault_mutex = PTHREAD_MUTEX_INITIALIZER;

// base install directory ($EUCALYPTUS) or '\0' if root
static char euca_base [PATH_MAX] = "";

/*
 * Function prototypes
 */
static xmlDoc *read_eucafault (const char *, const char *);
static char *get_fault_id (const xmlNode *);
static xmlNode *get_eucafault (const char *, const xmlDoc *);
static int scandir_filter (const struct dirent *);
static boolean str_end_cmp (const char *, const char *);
static char *str_trim_suffix (char *, const char *);
static boolean add_eucafault (const xmlDoc *);
static xmlNode *get_common_block (const xmlDoc *);
static char *get_common_var (const char *);
static char *get_fault_var (const char *, const xmlNode *);
static boolean format_eucafault (const char *, const char_map **);
static boolean initialize_faultlog (const char *);

/*
 * Utility functions -- move to misc.c?
 * (Might some of them already be there in one form or another?)
 */

/*
 * Utility function:
 * Compares end of one string to another string (the suffix) for a match.
 */
static boolean
str_end_cmp (const char *str, const char *suffix)
{
    if (!str || !suffix) {
        return FALSE;
    }
    size_t lenstr = strlen (str);
    size_t lensuffix = strlen (suffix);

    if (lensuffix >  lenstr) {
        return FALSE;
    }
    return strncmp (str + lenstr - lensuffix, suffix, lensuffix) == 0;
}

/*
 * Utility function:
 * Trims end of string off if it matches a supplied suffix.
 *
 * NOTE: The pointer returned by this function must be free()'d by the
 * caller.
 */
static char *
str_trim_suffix (char *str, const char *suffix)
{
    if (!str || !suffix || !str_end_cmp (str, suffix)) {
        return (char *)str;
    } else {
        int trim = strlen (str) - strlen (suffix);
        *(str + trim) = '\0';
        PRINTF1 (("str_trim_suffix() returning: %s\n", str));
    }
    return str;
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
read_eucafault (const char *faultdir, const char *fault_id)
{
    xmlDoc *my_doc = NULL;
    char fault_file[PATH_MAX];
    static int common_block_exists = 0;

    snprintf (fault_file, PATH_MAX, "%s/%s%s", faultdir, fault_id, XML_SUFFIX);
    PRINTF (("Attempting to load file %s\n", fault_file));

    if (get_eucafault (fault_id, NULL) != NULL) {
        PRINTF (("Looks like fault %s already exists.\n", fault_id));
        return NULL;
    }
    my_doc = xmlParseFile (fault_file);

    if (my_doc == NULL) {
        logprintfl (EUCAWARN, "Could not parse file %s in read_eucafault()\n",
                   fault_file);
        return NULL;
    } else {
        PRINTF1 (("Successfully parsed file %s in read_eucafault()\n",
                fault_file));
    }
    if (get_eucafault (fault_id, my_doc) != NULL) {
        PRINTF (("Found fault id %s in %s\n", fault_id, fault_file));
    } else if (get_common_block (my_doc) != NULL) {
        PRINTF (("Found <%s> block in %s\n", COMMON_PREFIX, fault_file));
        if (common_block_exists++) {
            PRINTF (("<%s> block already exists--skipping.\n", COMMON_PREFIX));
            xmlFreeDoc (my_doc);
            return NULL;
        }
    } else {
        logprintfl (EUCAWARN, "Did not find fault id %s in %s -- found fault id %s instead. (Not adding fault.)\n", fault_id, fault_file, get_fault_id (xmlFirstElementChild (xmlDocGetRootElement (my_doc))));
        xmlFreeDoc (my_doc);
        return NULL;
    }
    return my_doc;
}

/*
 * Adds XML doc for a fault to the in-memory fault registry (doc).
 * Creates registry if none exists yet.
 *
 * Can also add COMMON_PREFIX (<common>) block.
 */
static boolean
add_eucafault (const xmlDoc *new_doc)
{
    if (xmlDocGetRootElement (ef_doc) == NULL) {
        PRINTF1 (("Creating new document.\n"));
        ef_doc = xmlCopyDoc ((xmlDoc *)new_doc, 1); /* 1 == recursive copy */

        if (ef_doc == NULL) {
            logprintfl (EUCAERROR, "Problem creating fault registry.\n");
            return FALSE;
        }
    } else {
        PRINTF1 (("Appending to existing document.\n"));
        if (xmlAddNextSibling (xmlFirstElementChild (xmlDocGetRootElement (ef_doc)),
                               xmlFirstElementChild (xmlDocGetRootElement ((xmlDoc *)new_doc))) == NULL) {
            logprintfl (EUCAERROR, "Problem adding fault to existing registry.\n");
            return FALSE;
        }
    }
    return TRUE;
}

/*
 * Returns the fault id found in a fault node.
 */
static char *
get_fault_id (const xmlNode *node)
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
get_common_block (const xmlDoc *doc)
{
    if (doc == NULL) {
        return NULL;
    }
    for (xmlNode *node =
             xmlFirstElementChild (xmlDocGetRootElement ((xmlDoc *)doc));
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
 * Returns fault node if fault id exists in registry, NULL if not.
 *
 * Uses global fault registry unless a non-NULL doc pointer is passed as a
 * parameter, so it can be used either to match a fault id in a single
 * doc or to determine if a fault id already exists in the overall
 * registry.
 *
 * If NULL id supplied, simply returns the first fault node in document.
 */
static xmlNode *
get_eucafault (const char *id, const xmlDoc *doc)
{
    /*
     * Uses global registry if no doc supplied.
     */
    if (doc == NULL) {
        doc = ef_doc;
    }
    for (xmlNode *node =
             xmlFirstElementChild (xmlDocGetRootElement ((xmlDoc *)doc));
         node; node = node->next) {
        char *this_id = get_fault_id (node);

        if (id == NULL) {
            return node;
        } else if ((this_id != NULL) && !strcasecmp (this_id, id)) {
            return node;
        }
    }
    return NULL;
}

/*
 * Builds up the fault log path from supplied argument and configured
 * directory prefix. If passed NULL, tries to determine process name and
 * uses that for building up the path.
 *
 * Returns TRUE if fault logfile successfully opened, FALSE otherwise
 * (meaning logging is to stderr).
 */

static boolean
initialize_faultlog (const char *fileprefix)
{
    char faultlogpath[PATH_MAX];
    char *fileprefix_i;

    if (fileprefix == NULL) {
        // FIXME: program_infocation_short_name is a GNU'ism and is not
        // portable--should wrap with an autoconf check.
        snprintf (faultlogpath, PATH_MAX, "%s%s/%s-faults.log", euca_base, FAULTLOGDIR,
                  program_invocation_short_name);
    } else {
        // Prune any leading directores from path.
        fileprefix_i = rindex (fileprefix, '/');

        if (fileprefix_i != NULL) {
            fileprefix = fileprefix_i + 1;
        }
        snprintf (faultlogpath, PATH_MAX, "%s%s/%s-faults.log", euca_base, FAULTLOGDIR,
                  fileprefix);
    }
    PRINTF (("Initializing faultlog using %s\n", faultlogpath));
    faultlog = fopen (faultlogpath, "a+");

    if (faultlog == NULL) {
        logprintfl (EUCAERROR, "Cannot open fault log file %s: %s\n",
                    faultlogpath, strerror (errno));
        logprintfl (EUCAERROR, "Logging faults to stderr...\n");
        faultlog = stderr;
        return FALSE;
    } else {
        return TRUE;
    }
}

/*
 * EXTERNAL ENTRY POINT
 *
 * Builds the localized fault registry from XML files supplied in
 * various directories.
 *
 * Returns the number of faults successfully loaded into registry. If
 * the registry was previously loaded, returns the number of previously
 * loaded faults as a negative number.
 */
int
init_eucafaults (char *fileprefix)
{
    struct stat dirstat;
    static int faults_loaded = 0;
    char *locale = NULL;
    static char faultdirs[NUM_FAULTDIR_TYPES][PATH_MAX];

    pthread_mutex_lock (&fault_mutex);

    if (faults_loaded) {
        PRINTF1 (("Attempt to reinitialize fault registry? Skipping...\n"));
        pthread_mutex_unlock (&fault_mutex);
        return -faults_loaded;
    }

	char * euca_env = getenv(EUCALYPTUS_ENV_VAR_NAME);
	if (euca_env) {
        strncpy(euca_base, euca_env, MAX_PATH - 1);
    }

    initialize_faultlog (fileprefix);
    PRINTF (("Initializing fault registry directories.\n"));

    if ((locale = getenv (LOCALIZATION_ENV_VAR)) == NULL) {
        logprintfl (EUCAINFO,
                   "$%s not set, using default value of: %s\n",
                    LOCALIZATION_ENV_VAR, DEFAULT_LOCALIZATION);
    }
    LIBXML_TEST_VERSION;

    /* Cycle through list of faultdirs in priority order, noting any missing. */
    if (locale != NULL) {
        snprintf (faultdirs[CUSTOM_LOCALIZED], PATH_MAX, "%s%s/%s/",
                  euca_base, CUSTOM_FAULTDIR, locale);
    } else {
        faultdirs[CUSTOM_LOCALIZED][0] = 0;
    }
    snprintf (faultdirs[CUSTOM_DEFAULT_LOCALIZATION], PATH_MAX, "%s%s/%s/",
              euca_base, CUSTOM_FAULTDIR, DEFAULT_LOCALIZATION);
    if (locale != NULL) {
        snprintf (faultdirs[DISTRO_LOCALIZED], PATH_MAX, "%s%s/%s/",
                  euca_base, DISTRO_FAULTDIR, locale);
    } else {
        faultdirs[DISTRO_LOCALIZED][0] = 0;
    }
    snprintf (faultdirs[DISTRO_DEFAULT_LOCALIZATION], PATH_MAX, "%s%s/%s/",
              euca_base, DISTRO_FAULTDIR, DEFAULT_LOCALIZATION);

    for (int i = 0; i < NUM_FAULTDIR_TYPES; i++) {
        if (faultdirs[i][0]) {
            if (stat (faultdirs[i], &dirstat) != 0) {
                logprintfl (EUCAINFO, "stat() problem with %s: %s\n",
                           faultdirs[i], strerror (errno));
            } else if (!S_ISDIR (dirstat.st_mode)) {
                logprintfl (EUCAINFO,
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
                        xmlDoc *new_fault = read_eucafault (faultdirs[i], str_trim_suffix (namelist[numfaults]->d_name, XML_SUFFIX));
                        free (namelist[numfaults]);

                        if (new_fault) {
                            if (add_eucafault (new_fault)) {
                                faults_loaded++;
                            }
                            xmlFreeDoc (new_fault);
                        } else {
                            PRINTF1 (("Not adding new fault--mismatch or already exists...?\n"));
                        }
                    }
                    free (namelist);
                }
            }
        }
    }
    pthread_mutex_unlock (&fault_mutex);
    logprintfl (EUCAINFO, "Loaded %d fault-logger descriptions into registry.\n",
               faults_loaded);
    return faults_loaded;
}

/*
 * Retrieves a translated label from <common> block.
 *
 * NOTE: The pointer returned by this function must be free()'d by the
 * caller.
 *
 * FIXME: Consolidate this with get_fault_id() and make some sort of
 * general-purpose fetch function? Or make get_fault_id() more general
 * and change this to call it?
 *
 * FIXME: Consolidate with get_fault_var()?
 */
static char *
get_common_var (const char *var)
{
    xmlNode *c_node = NULL;

    if ((c_node = get_common_block (ef_doc)) == NULL) {
        logprintfl (EUCAWARN, "Did not find <%s> block\n", COMMON_PREFIX);
        return strdup (var);
    }
    for (xmlNode *node = xmlFirstElementChild (c_node); node;
         node = node->next) {
        if ((node->type == XML_ELEMENT_NODE) &&
            !strcasecmp ((const char *)node->name, "var")) {
            xmlChar *prop = xmlGetProp (node, (const xmlChar *)"name");

            if (!strcasecmp (var, (char *)prop)) {
                xmlChar *value = NULL;

                xmlFree (prop);
                value = xmlGetProp (node, (const xmlChar *)LOCALIZED_TAG);

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
    return strdup (var);
}

/*
 * Retrieves a translated label from <fault> block.
 *
 * NOTE: The pointer returned by this function must be free()'d by the
 * caller.
 *
 * FIXME: Consolidate with get_common_var()?
 *
 * FIXME: Gosh this looks messy.
 */
static char *
get_fault_var (const char *var, const xmlNode *f_node)
{
    if ((f_node == NULL) || (var == NULL)) {
        logprintfl (EUCAWARN, "get_fault_var() called with one or more NULL arguments.\n");
        return NULL;
    }
    // Just in case we're matching the top-level node.
    // FIXME: Move this into a new tmfunction to eliminate repeated logic below?
    if ((f_node->type == XML_ELEMENT_NODE) &&
        !strcasecmp ((const char *)f_node->name, var)) {
        xmlChar *value = xmlGetProp ((xmlNode *)f_node,
                                     (const xmlChar *)LOCALIZED_TAG);
        if (value == NULL) {
            value = xmlGetProp ((xmlNode *)f_node,
                                (const xmlChar *)MESSAGE_TAG);
        }
        // This is a special (parent) case, so it doesn't handle
        // message/localized text in a child node.
        return (char *)value;
    }
    for (xmlNode *node = xmlFirstElementChild ((xmlNode *)f_node); node;
         node = node->next) {
        if ((node->type == XML_ELEMENT_NODE) &&
            !strcasecmp ((const char *)node->name, var)) {
            xmlChar *value = xmlGetProp (node, (const xmlChar *)LOCALIZED_TAG);

            if (value == NULL) {
                value = xmlGetProp (node, (const xmlChar *)MESSAGE_TAG);
            }
            if (value == NULL) {
                // May be a child node, e.g. for "resolution"
                for (xmlNode *subnode = xmlFirstElementChild (node); subnode;
                     subnode = subnode->next) {
                    if ((node->type == XML_ELEMENT_NODE) &&
                        !strcasecmp ((const char *)subnode->name,
                                     LOCALIZED_TAG)) {
                        return (char *)xmlNodeGetContent (subnode);
                    }
                }
                // FIXME: More elegant method than another list walk?
                for (xmlNode *subnode = xmlFirstElementChild (node); subnode;
                     subnode = subnode->next) {
                    if ((node->type == XML_ELEMENT_NODE) &&
                        !strcasecmp ((const char *)subnode->name,
                                     MESSAGE_TAG)) {
                        return (char *)xmlNodeGetContent (subnode);
                    }
                }
            }
            return (char *)value;
        }
    }
    logprintfl (EUCAWARN, "Did not find <%s> message in get_fault_var().\n",
                var);
    return NULL;
}

/*
 * Formats fault-log output and sends to fault log (or stdout/stderr).
 */
static boolean
format_eucafault (const char *fault_id, const char_map **map)
{
    static int max_label_len = 0;
    char *fault_var = NULL;
    time_t secs;
    struct tm lt;
    xmlNode *fault_node = get_eucafault (fault_id, NULL);

    if (fault_node == NULL) {
        logprintfl (EUCAERROR,
                    "Fault %s detected, could not find fault id in registry.\n",
                    fault_id);
        return FALSE;
    }
    // Determine label alignment. (Only needs to be done once.)
    if (!max_label_len) {
        for (int i = 0; fault_labels[i]; i++) {
            int label_len = 0;
            int w_label_len = 0;

            char *label = get_common_var (fault_labels[i]);
            label_len = strlen (label);

            w_label_len = utf8_to_wchar (label, label_len, NULL, 0, 0);
            free (label);
            if (w_label_len > max_label_len) {
                max_label_len = w_label_len;
            }
        }
    }
    // Top border.
    fprintf (faultlog, "%s\n", STARS);

    // Get time.
    secs = time (NULL);
    if (gmtime_r (&secs, &lt) == NULL) {
        // Someone call Dr. Who.
        lt.tm_year = lt.tm_mon = lt.tm_mday = 0;
        lt.tm_hour = lt.tm_min = lt.tm_sec = 0;
    } else {
        // Account for implied offsets in struct.
        lt.tm_year += 1900;
        lt.tm_mon += 1;
    }
    // Construct timestamped fault header.
    fprintf (faultlog, "  ERR-%s %04d-%02d-%02d %02d:%02d:%02dZ ", fault_id,
             lt.tm_year, lt.tm_mon, lt.tm_mday,
             lt.tm_hour, lt.tm_min, lt.tm_sec);

    if ((fault_var = get_fault_var ("fault", fault_node)) != NULL) {
        char *fault_subbed = NULL;

        if ((fault_subbed = c_varsub (fault_var, map)) != NULL) {
            fprintf (faultlog, "%s\n\n", fault_subbed);
        } else {
            fprintf (faultlog, "%s\n\n", fault_var);
        }
        free (fault_subbed);
        free (fault_var);
    } else {
        char *common_var = get_common_var ("unknown");
        fprintf (faultlog, "%s\n\n", common_var);
        free (common_var);
    }

    // Construct fault information lines.
    for (int i = 0; fault_labels[i]; i++) {
        int w_common_var_len = 0;
        int common_var_len = 0;
        int padding = 0;
        char *common_var = get_common_var (fault_labels[i]);

        common_var_len = strlen (common_var);
        w_common_var_len = utf8_to_wchar (common_var, common_var_len, NULL, 0,
                                          0);
        padding = max_label_len - w_common_var_len + 1;
        fprintf (faultlog, "%s%*s %s: ", BARS, padding, " ", common_var);
        free (common_var);

        if ((fault_var = get_fault_var (fault_labels[i], fault_node)) != NULL) {
            char *fault_subbed = NULL;

            if ((fault_subbed = c_varsub (fault_var, map)) != NULL) {
                fprintf (faultlog, "%s", fault_subbed);
            } else {
                fprintf (faultlog, "%s", fault_var);
            }
            free (fault_subbed);
            free (fault_var);
        } else {
            common_var = get_common_var ("unknown");
            fprintf (faultlog, "%s", common_var);
            free (common_var);
        }
        fprintf (faultlog, "\n");
    }
    // Bottom border.
    fprintf (faultlog, "%s\n\n", STARS);
    return TRUE;
}

/*
 * EXTERNAL ENTRY POINT
 *
 * Logs a fault, initializing the fault registry, if necessary.
 *
 * Returns TRUE if fault successfully logged, FALSE otherwise.
 */
boolean
log_eucafault (const char *fault_id, const char_map **map)
{
    int load = init_eucafaults (NULL);

    PRINTF1 (("init_eucafaults() returned %d\n", load));

    return format_eucafault (fault_id, map);
}

/*
 * EXTERNAL ENTRY POINT
 *
 * Logs a fault, initializing the fault registry, if necessary.
 *
 * Returns the number of substitution parameters it was called with,
 * returning it as a negative number if the underlying log_eucafault()
 * call returned FALSE.
 */
int
log_eucafault_v (const char *fault_id, ...)
{
    va_list argv;
    char *token[2];
    char_map **m = NULL;
    int count = 0;
    int load = init_eucafaults (NULL);

    PRINTF1 (("init_eucafaults() returned %d\n", load));
    va_start (argv, fault_id);

    while ((token[count % 2] = va_arg (argv, char *)) != NULL) {
        ++count;
        if (! (count % 2)) {
            m = c_varmap_alloc (m, token[0], token[1]);
        }
    }
    va_end (argv);

    if (count % 2) {
        logprintfl (EUCAWARN, "log_eucafault_v() called with an odd (unmatched) number of substitution parameters: %d\n", count);
    }
    if (!log_eucafault (fault_id, (const char_map **)m)) {
        PRINTF (("log_eucafault() returned FALSE inside log_eucafault_v()\n"));
        count *= -1;
    }
    c_varmap_free (m);
    return count;
}

/*
 * Provides a way to log test faults from shell command line.
 */
#ifdef _UNIT_TEST

/*
 * Performs blind dump of XML fault registry to stdout.
 */
static void
dump_eucafaults_db (void)
{
    printf ("\n");
    xmlDocDump (stdout, ef_doc);
    printf ("\n");
}

/*
 * Prints simple usage message.
 */
static void
usage (const char *argv0)
{
    fprintf (stderr, "Usage: %s [-d] fault-id [param1 param1Value] [param2 param2Value] [...]\n", argv0);
}

/*
 * I am not an animal.
 */
int
main (int argc, char **argv)
{
    int dump = 0;
    int opt;

    //setlocale (LC_ALL, "en_US.utf-8");

    while ((opt = getopt (argc, argv, "d")) != -1) {
        switch (opt) {
        case 'd':
            dump++;
            break;
        default:
            usage (argv[0]);
            return 1;
        }
    }
    // NULL forces init_eucafaults()'s call to
    // initialize_faultlog() to guess at process name for creating
    // logfile.
    opt = init_eucafaults (NULL);

    PRINTF (("init_eucafaults() returned %d\n", opt));

    if (optind < argc) {
        char_map **m = c_varmap_alloc (NULL, "daemon", "Balrog");
        m = c_varmap_alloc (m, "hostIp", "127.0.0.1");
        m = c_varmap_alloc (m, "brokerIp", "127.0.0.2");
        m = c_varmap_alloc (m, "endpointIp", "127.0.0.3");
        PRINTF1 (("argv[1st of %d]: %s\n", argc - optind, argv[optind]));
        log_eucafault (argv[optind], (const char_map **)m);
        c_varmap_free (m);

        // Reusing & abusing opt. :)
        opt = log_eucafault_v (argv[optind], "daemon", "Balrog",
                               "hostIp", "127.0.0.1",
                               "brokerIp", "127.0.0.2",
                               "endpointIp", "127.0.0.3",
                               "unmatched!", NULL);
        PRINTF (("log_eucafault_v args returned: %d\n", opt));

        // This allows substitution-argument pairs for unit test to be
        // passed in on command line.
        m = NULL;
        for (opt = optind + 1; opt < argc; opt++) {
            PRINTF1 (("argv[opt]: %s\n", argv[opt]));
            if ((opt - optind + 1) % 2) {
                PRINTF1 (("...now have two, calling c_varmap_alloc()\n"));
                m = c_varmap_alloc (m, argv[opt - 1], argv[opt]);
            }
        }
        if (m != NULL) {
            log_eucafault (argv[optind], (const char_map **)m);
            c_varmap_free (m);
        } else {
            log_eucafault (argv[optind], NULL);
        }
        log_eucafault_v (argv[optind]); // Deliberately call with no params.
    }
    if (dump) {
        dump_eucafaults_db ();
    }
    if (optind >= argc) {
        usage (argv[0]);
        return 1;
    }
    return 0;
}
#endif // _UNIT_TEST
