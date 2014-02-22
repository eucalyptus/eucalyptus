// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2014 Eucalyptus Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 ************************************************************************/

//!
//! @file storage/imager/img.c
//!
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdlib.h>                    // NULL
#include <stdio.h>
#include <string.h>                    // bzero, memcpy
#include <unistd.h>                    // getopt, access
#include <ctype.h>                     // tolower
#include <stdarg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <regex.h>

#include <eucalyptus.h>
#include <misc.h>                      // logprintfl
#include <euca_string.h>

#include "img.h"

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void strnsub(char *dest, const char *src, const size_t sindex, const size_t eindex, const size_t size);
static const char *img_spec_type_str(const int type);
static const char *img_creds_type_str(const int type);
static const char *img_creds_location_str(const img_loc * loc);

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
//!
//!
//! @param[in,out] dest
//! @param[in]     src
//! @param[in]     sindex
//! @param[in]     eindex
//! @param[in]     size
//!
//! @pre
//!
//! @post
//!
static void strnsub(char *dest, const char *src, const size_t sindex, const size_t eindex, const size_t size)
{
    strncpy(dest, src + sindex, (((eindex - sindex) > size) ? size : eindex - sindex));
}

//!
//!
//!
//! @param[in] loc
//! @param[in] str
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int parse_img_spec(img_loc * loc, const char *str)
{
#define _SIZE        512

    int i = 0;
    int status = 0;
    int bufLen = 0;
    char *t = NULL;
    char *ex = NULL;
    char *pt = NULL;
    char s[1] = "";
    char port[_SIZE] = "";
    char low[_SIZE] = "";
    char path[SIZE] = "";
    regex_t re = { 0 };
    regmatch_t pmatch[8] = { {0} };

    bzero(loc, sizeof(img_loc));       // so all strings are null-terminated

    for (i = 0; i < _SIZE && i < strlen(str); i++) {
        low[i] = tolower(str[i]);
    }

    if (!strncmp(low, "http://", 7) || !strncmp(low, "https://", 8)) {
        if (strstr(str, "services/objectstorage")) {
            loc->type = OBJECTSTORAGE;
            euca_strncpy(loc->url, str, sizeof(loc->url));
        } else if (strstr(str, "dcPath=")) {
            // EXAMPLE: https://192.168.7.236/folder/i-4DD50852?dcPath=ha-datacenter&dsName=S1
            ex = "^[Hh][Tt][Tt][Pp][Ss]?://([^/]+)/([^\\?]+)\\?dcPath=([^&]+)&dsName=(.*)$";
            if (regcomp(&re, ex, REG_EXTENDED) != 0) {
                LOGERROR("failed to compile regular expression for vSphere URL: %s\n", ex);
                return EUCA_ERROR;
            }

            status = regexec(&re, str, (size_t) 5, pmatch, 0);
            regfree(&re);
            if (status != 0) {
                LOGERROR("failed to match the syntax of vSphere URL (%s)\nwith regular expression %s\n", str, ex);
                return EUCA_ERROR;
            }

            if (re.re_nsub != 4) {
                LOGERROR("unexpected number of matched elements in %s\n", ex);
                return EUCA_ERROR;
            }
            loc->type = VSPHERE;

            euca_strncpy(loc->url, str, sizeof(loc->url));
            strnsub(loc->host, str, pmatch[1].rm_so, pmatch[1].rm_eo, sizeof(loc->host));
            strnsub(path, str, pmatch[2].rm_so, pmatch[2].rm_eo, sizeof(loc->path));
            strnsub(loc->vsphere_dc, str, pmatch[3].rm_so, pmatch[3].rm_eo, sizeof(loc->vsphere_dc));
            strnsub(loc->vsphere_ds, str, pmatch[4].rm_so, pmatch[4].rm_eo, sizeof(loc->vsphere_ds));

            // extract path, split into directory and filename
            loc->dir[0] = '\0';
            loc->file[0] = '\0';
            loc->path[0] = '\0';
            t = strtok(path, "/");
            if (t == NULL || strcmp(t, "folder") != 0) {
                LOGERROR("failed to parse path in URL (must begin with 'folder'): %s...\n", path);
                return EUCA_ERROR;
            }

            bufLen = sizeof(loc->dir) - 1;
            while ((t = strtok(NULL, "/")) != NULL) {
                if (pt) {
                    if (loc->dir[0] != '\0') {
                        strncat(loc->dir, "/", (sizeof(loc->dir) - strlen(loc->dir) - 1));
                    }
                    strncat(loc->dir, pt, (sizeof(loc->dir) - strlen(loc->dir) - 1));
                }
                pt = t;
            }

            if (pt) {
                strncat(loc->file, pt, (sizeof(loc->file) - strlen(loc->file) - 1));
            }

            if (loc->dir[0] == '\0') {
                euca_strncpy(loc->path, loc->file, sizeof(loc->path));
            } else {
                snprintf(loc->path, sizeof(loc->path), "%s/%s", loc->dir, loc->file);
            }

            LOGDEBUG("re_nsub=%ld host=%s path='%s' ('%s' + '%s') dc=%s ds=%s\n", re.re_nsub, loc->host, loc->path, loc->dir, loc->file, loc->vsphere_dc, loc->vsphere_ds);
        } else {
            loc->type = HTTP;

            // EXAMPLE: http://192.168.7.236:902/foobar?foo=bar
            ex = "^[Hh][Tt][Tt][Pp]([Ss])?://([^/^:]+)(:([0-9]+))?(/[^\\?]+)?(\\?(.*))?$";
            if (regcomp(&re, ex, REG_EXTENDED) != 0) {
                LOGERROR("failed to compile regular expression for URL: %s\n", ex);
                return EUCA_ERROR;
            }

            status = regexec(&re, str, (size_t) 8, pmatch, 0);
            regfree(&re);
            if (status != 0) {
                LOGERROR("failed to match the syntax of URL (%s)\nwith regular expression %s\n", str, ex);
                return EUCA_ERROR;
            }

            if (re.re_nsub != 7) {
                LOGERROR("unexpected number of matched elements in %s\n", ex);
                return EUCA_ERROR;
            }

            bzero(port, sizeof(port));
            strnsub(s, str, pmatch[1].rm_so, pmatch[1].rm_eo, sizeof(s));
            if (s[0] == 's')
                loc->type = HTTPS;

            euca_strncpy(loc->url, str, sizeof(loc->url));
            strnsub(loc->host, str, pmatch[2].rm_so, pmatch[2].rm_eo, sizeof(loc->host));
            strnsub(port, str, pmatch[4].rm_so, pmatch[4].rm_eo, sizeof(port) - 1);

            loc->port = atoi(port);
            strnsub(loc->path, str, pmatch[5].rm_so, pmatch[5].rm_eo, sizeof(loc->path));
            strnsub(loc->params, str, pmatch[7].rm_so, pmatch[7].rm_eo, sizeof(loc->params));
        }
    } else {
        loc->type = PATH;
        euca_strncpy(loc->path, str, sizeof(loc->path));
    }

    return EUCA_OK;

#undef _SIZE
}

//!
//!
//!
//! @param[in] spec
//! @param[in] id
//! @param[in] loc
//! @param[in] creds
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int img_init_spec(img_spec * spec, const char *id, const char *loc, const img_creds * creds)
{
    bzero(spec, sizeof(*spec));

    if (id && strlen(id))
        strncpy(spec->id, id, sizeof(spec->id) - 1);
    else {
        // if no ID is given, this function just zeroes out the struct
        return EUCA_OK;
    }

    if (parse_img_spec(&(spec->location), loc))
        return EUCA_ERROR;

    if (creds)
        memcpy(&(spec->location.creds), creds, sizeof(img_creds));
    else
        spec->location.creds.type = NONE;

    return EUCA_OK;
}

//!
//!
//!
void img_cleanup(void)
{

}

//!
//!
//!
//! @param[in] type
//!
//! @return The string matching the type value
//!
static const char *img_spec_type_str(const int type)
{
    switch (type) {
    case PATH:
        return "path";
    case HTTP:
        return "http";
    case VSPHERE:
        return "vsphere";
    case OBJECTSTORAGE:
        return "objectstorage";
    case SFTP:
        return "sftp";
    default:
        return "unknown";
    }
}

//!
//!
//!
//! @param[in] type
//!
//! @return The string matching the type value
//!
static const char *img_creds_type_str(const int type)
{
    switch (type) {
    case NONE:
        return "none";
    case PASSWORD:
        return "password";
    case X509CREDS:
        return "x509";
    case SSHKEY:
        return "sshkey";
    default:
        return "unknown";
    }
}

//!
//!
//!
//! @param[in] loc
//!
//! @return The location to the image credentials
//!
//! @pre
//!
//! @post
//!
static const char *img_creds_location_str(const img_loc * loc)
{
    switch (loc->type) {
    case PATH:
        return loc->path;
    case HTTP:
        return loc->url;
    case VSPHERE:
        return loc->url;
    case OBJECTSTORAGE:
        return loc->url;
    case SFTP:
        return loc->url;
    default:
        return "";
    }
}

//!
//!
//!
//! @param[in] name
//! @param[in] spec
//!
void print_img_spec(const char *name, const img_spec * spec)
{
    if (name == NULL || spec == NULL || strlen(name) == 0 || strlen(spec->id) == 0)
        return;

    LOGDEBUG("\t%s: id=%-12s type=%-7s creds=%-8s %s\n",
             name, spec->id, img_spec_type_str(spec->location.type), img_creds_type_str(spec->location.creds.type), img_creds_location_str(&(spec->location)));
}
