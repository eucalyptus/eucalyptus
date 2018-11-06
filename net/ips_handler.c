// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

//!
//! @file net/ips_handler.c
//! This file needs a description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <limits.h>

#include <eucalyptus.h>
#include <log.h>
#include <euca_string.h>

#include "ips_handler.h"
#include "eucanetd_util.h"

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

/**
 * Initialize the IP Set handler structure
 *
 * @param ipsh [in] pointer to the IP set handler structure
 * @param cmdprefix [in] a string pointer to the prefix to use to run commands
 *
 * @return 0 on success. 1 on any failure.
 *
 * @pre
 *    - The ipsh pointer should not be NULL
 *     - We should be able to create temporary files on the system
 *     - We should be able to execute ebtables commands.
 *
 * @post
 *     - Temporary files on disk: /tmp/ips_file-XXXXXX
 *     - If cmdprefix was provided, the table's cmdprefix field will be set with it
 *
 * @note
 *     - Once temporary file is initialized the filename will be reused throughout the process
 *       lifetime. The file will be truncated/created on each successive calls to the *_handler_init()
 *       method.
 */
int ips_handler_init(ips_handler *ipsh, const char *cmdprefix) {
    int fd;
    char sTempFileName[EUCA_MAX_PATH] = "";

    if (!ipsh) {
        LOGERROR("invalid input\n");
        return (1);
    }

    if (ipsh->init) {
        snprintf(sTempFileName, EUCA_MAX_PATH, "%s", ipsh->ips_file);
        if (truncate_file(sTempFileName)) {
            return (1);
        }
        LOGDEBUG("Using already allocated temporary filename: %s\n", sTempFileName);
    } else {
        // Initialize new temp filename, only done once. 
        snprintf(sTempFileName, EUCA_MAX_PATH, "/tmp/ips_file-XXXXXX");
        if ((fd = safe_mkstemp(sTempFileName)) < 0) {
            LOGERROR("cannot create tmpfile '%s': check permissions\n", sTempFileName);
            return (1);
        }
        if (chmod(sTempFileName, 0600)) {
            LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", sTempFileName);
        }

        LOGDEBUG("Using Newly created temporary filename: %s\n", sTempFileName);
        close(fd);
    }

    bzero(ipsh, sizeof(ips_handler));

    snprintf(ipsh->ips_file, EUCA_MAX_PATH, "%s", sTempFileName);

    if (cmdprefix) {
        snprintf(ipsh->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        ipsh->cmdprefix[0] = '\0';
    }
    
    // test required shell-outs
    if (euca_execlp_redirect(NULL, NULL, "/dev/null", FALSE, "/dev/null", FALSE, ipsh->cmdprefix, "ipset", "-L", NULL) != EUCA_OK) {
        LOGERROR("could not execute ipset -L. check command/permissions\n");
        return (1);
    }

    ipsh->init = 1;
    return (0);
}

/**
 * Shell-out and execute ipset save
 * @param ipsh [in] pointer to ipset handler
 * @return EUCA_OK on success. EUCA_ERROR on failure
 */
int ips_system_save(ips_handler *ipsh) {
    int rc = EUCA_OK;
    if (euca_execlp_redirect(NULL, NULL, ipsh->ips_file, FALSE, NULL, FALSE, ipsh->cmdprefix, "ipset", "save", NULL) != EUCA_OK) {
        LOGERROR("ipset save failed\n");
        rc = EUCA_ERROR;
    }
    return (rc);
}

/**
 * Shell-out and execute ipset restore
 * @param ipsh [in] pointer to ipset handler
 * @return EUCA_OK on success. EUCA_ERROR on failure
 */
int ips_system_restore(ips_handler *ipsh) {
    int rc = EUCA_OK;
    if (euca_execlp_redirect(NULL, ipsh->ips_file, NULL, FALSE, NULL, FALSE, ipsh->cmdprefix, "ipset", "-!", "restore", NULL) != EUCA_OK) {
        copy_file(ipsh->ips_file, "/tmp/euca_ips_file_failed");
        LOGERROR("ipset restore failed. copying failed input file to '/tmp/euca_ips_file_failed' for manual retry.\n");
        rc = EUCA_ERROR;
    }
    unlink_handler_file(ipsh->ips_file);
    return (rc);
}

/**
 * Retrieves the current ipset state from the system.
 * @param ipsh [in] pointer to ipset handler
 * @return 0 on success. 1 on failure.
 */
int ips_handler_repopulate(ips_handler *ipsh) {
    int rc = 0, nm = 0;
    FILE *FH = NULL;
    char buf[1024] = "";
    char *strptr = NULL;
    char setname[64] = "";
    char ipname[64] = "", *ip = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    if (!ipsh || !ipsh->init) {
        return (1);
    }

    rc = ips_handler_free(ipsh);
    if (rc) {
        LOGERROR("could not reinitialize ips handler.\n");
        return (1);
    }

    rc = ips_system_save(ipsh);
    if (rc) {
        LOGERROR("could not save current IPS rules to file, exiting re-populate\n");
        return (1);
    }

    FH = fopen(ipsh->ips_file, "r");
    if (!FH) {
        LOGERROR("could not open file for read '%s': check permissions\n", ipsh->ips_file);
        return (1);
    }

    while (fgets(buf, 1024, FH)) {
        if ((strptr = strchr(buf, '\n'))) {
            *strptr = '\0';
        }

        if (strlen(buf) < 1) {
            continue;
        }

        while (buf[strlen(buf) - 1] == ' ') {
            buf[strlen(buf) - 1] = '\0';
        }

        if (strstr(buf, "create")) {
            setname[0] = '\0';
            sscanf(buf, "create %s", setname);
            if (strlen(setname)) {
                ips_handler_add_set(ipsh, setname);
            }
        } else if (strstr(buf, "add")) {
            ipname[0] = '\0';
            sscanf(buf, "add %s %[0-9./]", setname, ipname);
            if (strlen(setname) && strlen(ipname)) {
                rc = cidrsplit(ipname, &ip, &nm);
                if (ip && strlen(ip) && nm >= 0 && nm <= 32) {
                    LOGDEBUG("reading in from ipset: adding ip/nm %s/%d to ipset %s\n", SP(ip), nm, SP(setname));
                    ips_set_add_net(ipsh, setname, ip, nm);
                    EUCA_FREE(ip);
                }
            }
        } else {
            LOGWARN("unknown IPS rule on ingress, rule will be thrown out: (%s)\n", buf);
        }
    }
    fclose(FH);

    unlink_handler_file(ipsh->ips_file);
    LOGDEBUG("ips populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
}

/**
 * Applies the ipset configuration in the handler to the system.
 * @param ipsh [in] pointer to the ipset handler
 * @param dodelete [in] set to 1 if we need to flush an empty set or 0 if we ignore
 * @return 0 on success. 1 on failure.
 */
int ips_handler_deploy(ips_handler *ipsh, int dodelete) {
    int i = 0;
    int j = 0;
    FILE *FH = NULL;
    char *strptra = NULL;

    if (!ipsh || !ipsh->init) {
        return (1);
    }

    FH = fopen(ipsh->ips_file, "w");
    if (!FH) {
        LOGERROR("could not open file for write '%s': check permissions\n", ipsh->ips_file);
        return (1);
    }
    for (i = 0; i < ipsh->max_sets; i++) {
        if (ipsh->sets[i].ref_count) {
            fprintf(FH, "create %s hash:net family inet hashsize 2048 maxelem 65536\n", ipsh->sets[i].name);
            fprintf(FH, "flush %s\n", ipsh->sets[i].name);
            for (j = 0; j < ipsh->sets[i].max_member_ips; j++) {
                strptra = hex2dot(ipsh->sets[i].member_ips[j]);
                LOGDEBUG("adding ip/nm %s/%d to ipset %s\n", strptra, ipsh->sets[i].member_nms[j], ipsh->sets[i].name);
                fprintf(FH, "add %s %s/%d\n", ipsh->sets[i].name, strptra, ipsh->sets[i].member_nms[j]);
                EUCA_FREE(strptra);
            }
        } else if ((ipsh->sets[i].ref_count == 0) && dodelete) {
            fprintf(FH, "create %s hash:net family inet hashsize 2048 maxelem 65536\n", ipsh->sets[i].name);
            fprintf(FH, "flush %s\n", ipsh->sets[i].name);
            fprintf(FH, "destroy %s\n", ipsh->sets[i].name);
        }
    }
    fclose(FH);

    return (ips_system_restore(ipsh));
}

/**
 * Adds a new set to the ipset handler
 * @param ipsh [in] pointer to the ipset handler
 * @param setname [in] name of the set to be added
 * @return 0 on success. 1 on failure.
 */
int ips_handler_add_set(ips_handler *ipsh, char *setname) {
    ips_set *set = NULL;

    if (!ipsh || !setname || !ipsh->init) {
        return (1);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        ipsh->sets = realloc(ipsh->sets, sizeof(ips_set) * (ipsh->max_sets + 1));
        if (!ipsh->sets) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        bzero(&(ipsh->sets[ipsh->max_sets]), sizeof(ips_set));
        snprintf(ipsh->sets[ipsh->max_sets].name, 64, "%s", setname);
        ipsh->sets[ipsh->max_sets].ref_count = 1;
        ipsh->max_sets++;
    }
    return (0);
}

/**
 * Searches for the ipset in the argument.
 * @param ipsh [in] pointer to the ipset handler
 * @param findset [in] the ipset name we're looking for
 * @return pointer to the ip_set if found. NULL otherwise.
 */
ips_set *ips_handler_find_set(ips_handler *ipsh, char *findset) {
    int i, setidx = 0, found = 0;
    if (!ipsh || !findset || !ipsh->init) {
        return (NULL);
    }

    found = 0;
    for (i = 0; i < ipsh->max_sets && !found; i++) {
        setidx = i;
        if (!strcmp(ipsh->sets[i].name, findset))
            found++;
    }
    if (!found) {
        return (NULL);
    }
    return (&(ipsh->sets[setidx]));
}

/**
 * Adds an IP address to an ipset
 * @param ipsh [in] pointer to the ipset handler
 * @param setname [in] name of the ipset of interest
 * @param ipname [in] IP address to be added to the ipset of interest
 * @return 0 on success. 1 on failure.
 */
int ips_set_add_ip(ips_handler *ipsh, char *setname, char *ipname) {
    return (ips_set_add_net(ipsh, setname, ipname, 32));
}

/**
 * Adds a CIDR block to an ipset
 * @param ipsh [in] pointer to the ipset handler
 * @param setname [in] name of the ipset of interest
 * @param ipname [in] network address
 * @param nmname [in] network mask
 * @return 0 on success. 1 on failure.
 */
int ips_set_add_net(ips_handler *ipsh, char *setname, char *ipname, int nmname) {
    ips_set *set = NULL;
    u32 *ip = NULL;
    if (!ipsh || !setname || !ipname || !ipsh->init) {
        return (1);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        return (1);
    }

    ip = ips_set_find_net(ipsh, setname, ipname, nmname);
    if (!ip) {
        set->member_ips = realloc(set->member_ips, sizeof(u32) * (set->max_member_ips + 1));
        if (!set->member_ips) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }
        set->member_nms = realloc(set->member_nms, sizeof(int) * (set->max_member_ips + 1));
        if (!set->member_nms) {
            LOGFATAL("out of memory!\n");
            exit(1);
        }

        bzero(&(set->member_ips[set->max_member_ips]), sizeof(u32));
        bzero(&(set->member_nms[set->max_member_ips]), sizeof(int));
        set->member_ips[set->max_member_ips] = dot2hex(ipname);
        set->member_nms[set->max_member_ips] = nmname;
        set->max_member_ips++;
        set->ref_count++;
    }
    return (0);
}

/**
 * Searches for an IP address in an ipset
 * @param ipsh [in] pointer to the ipset handler
 * @param setname [in] name of the ipset of interest
 * @param findipstr [in] IP address to look for
 * @return pointer to an integer representing the IP address if found. NULL otherwise.
 */
u32 *ips_set_find_ip(ips_handler *ipsh, char *setname, char *findipstr) {
    return (ips_set_find_net(ipsh, setname, findipstr, 32));
}

/**
 * Searches for a CIDR block IP in an ipset.
 * @param ipsh [in] pointer to the ipset handler
 * @param setname [in] name of the ipset of interest
 * @param findipstr [in] IP address to look for
 * @param findnm [in] network mask to look for
 * @return pointer to an integer representing the IP address if found. NULL otherwise.
 */
u32 *ips_set_find_net(ips_handler *ipsh, char *setname, char *findipstr, int findnm) {
    int i, found = 0, ipidx = 0;
    ips_set *set = NULL;
    u32 findip;

    if (!ipsh || !setname || !findipstr || !ipsh->init) {
        return (NULL);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        return (NULL);
    }

    findip = dot2hex(findipstr);
    found = 0;
    for (i = 0; i < set->max_member_ips && !found; i++) {
        ipidx = i;
        if (set->member_ips[i] == findip && set->member_nms[i] == findnm)
            found++;
    }

    if (!found) {
        return (NULL);
    }

    return (&(set->member_ips[ipidx]));
}

/**
 * Flushes all entries in the given ipset
 * @param ipsh [in] pointer to the ipset handler
 * @param setname [in] name of the ipset of interest
 * @return 0 on success. 1 on failure.
 */
int ips_set_flush(ips_handler *ipsh, char *setname) {
    ips_set *set = NULL;

    if (!ipsh || !setname || !ipsh->init) {
        return (1);
    }

    set = ips_handler_find_set(ipsh, setname);
    if (!set) {
        return (1);
    }

    EUCA_FREE(set->member_ips);
    EUCA_FREE(set->member_nms);
    set->max_member_ips = set->ref_count = 0;

    return (0);
}

/**
 * Removes ipsets that matches (partial matches accepted) the string in the argument
 * @param ipsh [in] pointer to the ipset handler
 * @param setmatch [in] name of ipset to look for (partial matches are accepted)
 * @return 0 on success. 1 on failure.
 */
int ips_handler_deletesetmatch(ips_handler *ipsh, char *setmatch) {
    int i = 0;
    int found = 0;

    if (!ipsh || !setmatch || !ipsh->init) {
        return (1);
    }

    found = 0;
    for (i = 0; i < ipsh->max_sets && !found; i++) {
        if (strstr(ipsh->sets[i].name, setmatch)) {
            EUCA_FREE(ipsh->sets[i].member_ips);
            EUCA_FREE(ipsh->sets[i].member_nms);
            ipsh->sets[i].max_member_ips = 0;
            ipsh->sets[i].ref_count = 0;
        }
    }

    return (0);
}

/**
 * Release resources of the given ipset handler and reinitializes the handler.
 * @param ipsh [in] pointer to the ipset handler
 * @return 0 on success. 1 on failure.
 */
int ips_handler_free(ips_handler *ipsh) {
    int i = 0;
    char saved_cmdprefix[EUCA_MAX_PATH] = "";

    if (!ipsh || !ipsh->init) {
        return (1);
    }
    snprintf(saved_cmdprefix, EUCA_MAX_PATH, "%s", ipsh->cmdprefix);

    for (i = 0; i < ipsh->max_sets; i++) {
        EUCA_FREE(ipsh->sets[i].member_ips);
        EUCA_FREE(ipsh->sets[i].member_nms);
    }
    EUCA_FREE(ipsh->sets);

    return (ips_handler_init(ipsh, saved_cmdprefix));
}

/**
 * Releases all resources of the given ips_handler.
 * @param ipsh [in] pointer to the ipset handler
 * @return 0 on success. 1 on failure.
 */
int ips_handler_close(ips_handler *ipsh) {
    int i = 0;
    if (!ipsh || !ipsh->init) {
        LOGDEBUG("Invalid argument. NULL or uninitialized ips_handler.\n");
        return (1);
    }
    for (i = 0; i < ipsh->max_sets; i++) {
        EUCA_FREE(ipsh->sets[i].member_ips);
        EUCA_FREE(ipsh->sets[i].member_nms);
    }
    EUCA_FREE(ipsh->sets);

    unlink_handler_file(ipsh->ips_file);
    ipsh->init = 0;
    return (0);
}

/**
 * Logs (TRACE level) the information about the given ipset handler.
 * @param ipsh [in] pointer to the ipset handler
 * @return 0 on success. 1 on failure.
 */
int ips_handler_print(ips_handler *ipsh) {
    int i, j;
    char *strptra = NULL;

    if (!ipsh) {
        return (1);
    }

    if (log_level_get() == EUCA_LOG_TRACE) {
        for (i = 0; i < ipsh->max_sets; i++) {
            LOGTRACE("IPSET NAME: %s\n", ipsh->sets[i].name);
            for (j = 0; j < ipsh->sets[i].max_member_ips; j++) {
                strptra = hex2dot(ipsh->sets[i].member_ips[j]);
                LOGTRACE("\t MEMBER IP: %s/%d\n", strptra, ipsh->sets[i].member_nms[j]);
                EUCA_FREE(strptra);
            }
        }
    }
    return (0);
}
