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

#define _GNU_SOURCE

#include <dirent.h>
#include <errno.h>
#include <limits.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <wchar.h>
#include <sys/stat.h>

#include <libxml/parser.h>
#include <libxml/tree.h>

#include "fault.h"

#define DISTRO_FAULTDIR "faults/default"
#define CUSTOM_FAULTDIR "faults/customized"
#define ENGLISH "en_US"
#define DEFAULT_LOCALIZATION "ru" // Russian. :)
#define THISFILE DISTRO_FAULTDIR "/" ENGLISH "/1234.xml"
#define XML_SUFFIX ".xml"

/*
 * This is the order of priority (highest to lowest) for fault messages wrt
 * customization & localization.
 *
 * Once a fault id has been found and added to the in-memory repository,
 * all further occurrences of that fault in lower-priority faultdirs will
 * be ignored.
 */
enum faultdir_types {
    CUSTOM_LOCAL,
    CUSTOM_ENGLISH,
    DISTRO_LOCAL,
    DISTRO_ENGLISH,
    NUM_FAULTDIR_TYPES,         /* For keeping score */
};

/*
 * Shared data
 */
static int faults_initialized = 0;
static char faultdirs[NUM_FAULTDIR_TYPES][PATH_MAX];
static xmlDoc *ef_doc = NULL;
static pthread_mutex_t fault_mutex = PTHREAD_MUTEX_INITIALIZER;

/*
 * Function prototypes
 */
static int initialize_faultdb (void);
static xmlDoc *get_eucafault (const char *, const char *);
static int get_eucafaults_doc (void);
static void print_element_names (void);
static int fault_id_exists (const char *);
static int scandir_filter (const struct dirent *);
static int str_end_cmp (const char *, const char *);
static char *str_trim_suffix (const char *, const char *);
static int add_eucafault (xmlDoc *);

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

static char *
str_trim_suffix (const char *str, const char *suffix)
{
    if (!str || !suffix || !str_end_cmp (str, suffix)) {
        return (char *)str;
    }
    return strndup (str, strlen (str) - strlen (suffix));
}

static int
scandir_filter (const struct dirent *entry)
{
    return str_end_cmp (entry->d_name, XML_SUFFIX);
}

static int
initialize_faultdb (void)
{
    struct stat dirstat;
    int populate = 0;           /* FIXME: Use or delete. */

    pthread_mutex_lock(&fault_mutex);
    if (faults_initialized) {
        pthread_mutex_unlock(&fault_mutex);
        return 0;
    }
    printf("--> Initializing fault registry directories.\n");
    LIBXML_TEST_VERSION;
    /* Cycle through list of faultdirs in priority order, noting any missing. */
    snprintf (faultdirs[CUSTOM_LOCAL], PATH_MAX - 1, "%s/%s/",
              CUSTOM_FAULTDIR, DEFAULT_LOCALIZATION);
    snprintf (faultdirs[CUSTOM_ENGLISH], PATH_MAX - 1, "%s/%s/",
              CUSTOM_FAULTDIR, ENGLISH);
    snprintf (faultdirs[DISTRO_LOCAL], PATH_MAX - 1, "%s/%s/",
              DISTRO_FAULTDIR, DEFAULT_LOCALIZATION);
    snprintf (faultdirs[DISTRO_ENGLISH], PATH_MAX - 1, "%s/%s/",
              DISTRO_FAULTDIR, ENGLISH);

    /* Not really sure how useful this is or will be. */
    for(int i = 0; i < NUM_FAULTDIR_TYPES; i++){
        if (stat(faultdirs[i], &dirstat) != 0) {
            printf ("*** Problem with %s:\n", faultdirs[i]);
            perror ("    stat()");
        } else if (!S_ISDIR(dirstat.st_mode)) {
            printf ("*** Problem with %s:\n", faultdirs[i]);
            printf ("    Not a directory\n");
        } else {
            struct dirent **namelist;
            int numfaults = scandir (faultdirs[i], &namelist, &scandir_filter,
                                     alphasort);
            if (numfaults == 0) {
                printf ("*** No faults found in %s\n", faultdirs[i]);
            } else {
                printf ("... found %d faults in %s\n", numfaults, faultdirs[i]);
                while (numfaults--) {
                    xmlDoc *new_fault = get_eucafault (faultdirs[i], str_trim_suffix (namelist[numfaults]->d_name, XML_SUFFIX));
                    if (new_fault) {
                        add_eucafault (new_fault);
                        xmlFreeDoc(new_fault);
                    } else {
                        printf ("... not adding new fault--already exists?\n");
                    }
                }
            }
                    
        }
    }
    //populate = get_eucafaults_doc();
/*     populate = populate_faultdb (); */
    faults_initialized++;
    pthread_mutex_unlock(&fault_mutex);

    return populate;
}

static xmlDoc *
get_eucafault (const char * faultdir, const char * fault_id)
{
    xmlDoc *my_doc = NULL;
    char faultfile[PATH_MAX];

    printf ("Getting fault %s\n", fault_id);
    if (fault_id_exists(fault_id)) {
        printf ("(...looks like fault %s already exists?)\n", fault_id);
        return NULL;
    }
    snprintf (faultfile, PATH_MAX - 1, "%s/%s%s", faultdir, fault_id,
              XML_SUFFIX);
    
    my_doc = xmlParseFile (faultfile);
    // FIXME: Should sanity check that fault id in file matches filename?
    //        ...or should we expect filenames to diverge from fault id #s?

    if (my_doc == NULL) {
        printf ("Could not parse file %s in get_eucafault()\n", 
                faultfile);
        return NULL;
    } else {
        printf ("Successfully parsed file %s in get_eucafault()\n",
                faultfile);
    }
    return my_doc;
}

static int
add_eucafault (xmlDoc *new_doc)
{
    if (xmlDocGetRootElement (ef_doc) == NULL) {
        printf ("Creating new document.\n");
        ef_doc = xmlCopyDoc (new_doc, 1); /* 1 == recursive copy */
        // FIXME: Add error check/return here.
    } else {
        printf ("Appending to existing document.\n");
        if (xmlAddNextSibling (xmlFirstElementChild (xmlDocGetRootElement (ef_doc)),
                               xmlFirstElementChild (xmlDocGetRootElement (new_doc))) == NULL) {
            printf ("*** Problem appending!");
            return -1;
        }
    }
    return 0;
}

static int
get_eucafaults_doc (void)
{
    xmlDoc *new_doc = NULL;
    if (ef_doc != NULL) {
        printf ("get_eucafaults_doc() called twice? Returning...\n");
        return -1;
    }
    new_doc = get_eucafault(faultdirs[DISTRO_ENGLISH], "1234");

    if (xmlDocGetRootElement(ef_doc) == NULL) {
        printf ("Creating new document.\n");
        ef_doc = xmlCopyDoc(new_doc, 1); /* 1 means recursive copy */
    }
    xmlFreeDoc(new_doc);

    new_doc = get_eucafault(faultdirs[DISTRO_ENGLISH], "1235");
    if (xmlDocGetRootElement(ef_doc) == NULL) {
        printf ("Creating new document.\n");
        ef_doc = xmlCopyDoc(new_doc, 1);
    } else {
        printf ("Appending to existing document.\n");
        if (xmlAddNextSibling(xmlFirstElementChild(xmlDocGetRootElement(ef_doc)),
                              xmlFirstElementChild(xmlDocGetRootElement(new_doc))) == NULL) {
            printf ("*** Problem appending!");
        }
    } 
    xmlFreeDoc(new_doc);

    new_doc = get_eucafault(faultdirs[DISTRO_ENGLISH], "1236");
    if (xmlDocGetRootElement(ef_doc) == NULL) {
        printf ("Creating new document.\n");
        ef_doc = xmlCopyDoc(new_doc, 1);
    } else {
        printf ("Appending to existing document.\n");
        if (xmlAddNextSibling(xmlFirstElementChild(xmlDocGetRootElement(ef_doc)),
                              xmlFirstElementChild(xmlDocGetRootElement(new_doc))) == NULL) {
            printf ("*** Problem appending!");
        }
    } 
    xmlFreeDoc(new_doc);

    new_doc = get_eucafault(faultdirs[DISTRO_LOCAL], "1236");
    if (xmlDocGetRootElement(ef_doc) == NULL) {
        printf ("Creating new document.\n");
        ef_doc = xmlCopyDoc(new_doc, 1);
    } else {
        printf ("Appending to existing document.\n");
        if (xmlAddNextSibling(xmlFirstElementChild(xmlDocGetRootElement(ef_doc)),
                              xmlFirstElementChild(xmlDocGetRootElement(new_doc))) == NULL) {
            printf ("*** Problem appending!");
        }
    } 
    xmlFreeDoc(new_doc);
    return 0;
}

static int
fault_id_exists (const char *id)
{
    /*
     * Does case-insensitive string comparison on the attribute name & value.
     * (These are technically case-sensitive in XML, but I believe in being
     * forgiving of minor transgressions--makes for happier users.
     */
    for (xmlNode *node = xmlFirstElementChild(xmlDocGetRootElement(ef_doc));
         node; node = node->next) {
        if (node->type == XML_ELEMENT_NODE) {
            for (xmlAttr *attr = node->properties; attr; attr = attr->next) {
                if (!strcasecmp((const char *)attr->name, "id")) {
                    if (!strcasecmp((const char *)attr->children->content,
                                    id)) {
                        return 1;
                    }
                }
            }
        }
    }
    return 0;
}

static void
print_element_names(void)
{
    //    xmlNode *cur_node = NULL;
    //    for (cur_node = a_node; cur_node; cur_node = cur_node->next) {
    //        if (cur_node->type == XML_ELEMENT_NODE) {
            //            printf("node type: Element, name:  %s\n", cur_node->name);
/*             printf("(%06d)              value: %ls\n", depth, */
/*                    (wchar_t *)cur_node->content); */
/*     xmlElemDump(stdout, a_doc, xmlDocGetRootElement(a_doc)); */
    printf("\n");
    xmlDocDump(stdout, ef_doc);
    printf("\n");

    /*
    for (xmlNode *cur_node = xmlFirstElementChild(xmlDocGetRootElement(ef_doc));
         cur_node; cur_node = cur_node->next) {

        if (cur_node->type == XML_ELEMENT_NODE) {
            printf("Node name: %s\n", cur_node->name);
            for (xmlAttr *cur_attr = cur_node->properties; cur_attr; cur_attr = cur_attr->next) {
                printf("     attr: %s\n", cur_attr->name);
                printf("      val: %s\n", cur_attr->children->content);
            }
            
        }
    }
    */
            //        }
        //print_element_names(cur_node->children);
            //    }
}

int
log_fault (char *fault_id, ...)
{
    va_list argv;
    char *token;
    int count = 0;

    initialize_faultdb();

    va_start (argv, fault_id);
    while((token = va_arg (argv, char *)) != NULL) {
        ++count;
    }
    va_end(argv);
    if (fault_id_exists(fault_id)) {
        printf ("FOUND FAULT %s WOO!\n", fault_id);
    } else {
        printf ("No such fault %s found :(\n", fault_id);
    }
    return count;               // FIXME: Just return void?
}

#ifdef _UNIT_TEST
int main (int argc, char ** argv)
{
    
    print_element_names ();
    if (argc > 1) {
        log_fault(argv[1], NULL); /* FIXME: Add passing argv parameters. */
    }
    print_element_names ();

    return 0;
}
#endif // _UNIT_TEST
