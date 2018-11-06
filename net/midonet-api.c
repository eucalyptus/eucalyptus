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
//! @file net/midonet-api.c
//! Need description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#include <dirent.h>
#include <errno.h>
#include <curl/curl.h>
#include <json-c/json.h>

#include <pthread.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <atomic_file.h>
#include <log.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "euca_gni.h"
#include "midonet-api.h"
#include "euca-to-mido.h"
#include "eucalyptus-config.h"

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

struct mem_params_t {
    char *mem;
    size_t size;
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */
extern int midocache_invalid;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

midonet_api_cache *midocache = NULL;
midoname_list *midocache_midos = NULL;

int midonet_api_system_changed = 0;
int http_gets = 0;
int http_posts = 0;
int http_puts = 0;
int http_deletes = 0;

double http_gets_time = 0.0;
double http_posts_time = 0.0;
double http_puts_time = 0.0;
double http_deletes_time = 0.0;

static int http_gets_prev = 0;
static int http_posts_prev = 0;
static int http_puts_prev = 0;
static int http_deletes_prev = 0;

static char midonet_api_uribase[URI_LEN] = {0};
char midonet_api_version[16] = {0};
static char midonet_api_mtypes[APPLICATION_MAX_INDEX][MIDO_MTYPE_MAX_LEN];

static int mido_libcurl_initialized = 0;
static mido_libcurl_handles libcurl_handles;
static pthread_mutex_t libcurl_handles_mutex;
static pthread_mutex_t mido_buffer_mutex;
static pthread_mutex_t mido_cache_ports_mutex;

static size_t header_find_location(char *content, size_t size, size_t nmemb, void *params);
static size_t mem_writer(void *contents, size_t size, size_t nmemb, void *in_params);
static size_t mem_reader(void *contents, size_t size, size_t nmemb, void *in_params);

/**
 * Prepares an array of mido_cache_thread_params structures: divides ntasks to
 * nthreads blocks, and sets the start and end indices appropriately.
 * @param ntasks [in] number of tasks.
 * @param nthreads [in] number of threads.
 * @return pointer to an array of mido_cache_thread_param structures. Caller is
 * responsible to release the allocated memory.
 */
mido_cache_worker_thread_params *prep_thread_params(int ntasks, int nthreads) {
    mido_cache_worker_thread_params *tparams = EUCA_ZALLOC_C(nthreads, sizeof (mido_cache_worker_thread_params));
    if (ntasks > 100) {
        int step = (ntasks + nthreads - 1) / nthreads;
        tparams[0].start = 0;
        tparams[0].end = step;
        for (int i = 1; i < (nthreads - 1); i++) {
            tparams[i].start = tparams[i - 1].end;
            tparams[i].end = (i + 1) * step;
        }
        tparams[nthreads - 1].start = tparams[nthreads - 2].end;
        tparams[nthreads - 1].end = ntasks;
    }
    return (tparams);
}

/**
 * Converts a list of comma separated IP address strings into an array of strings,
 * containing 1 IP address per entry.
 * @param iplist [in] a string containing a list of IP addresses
 * @param outiparr [out] array of strings, each string representing an IP address.
 * @param max_outiparr [out] number of entries in the array.
 * @return 0 on success. 1 otherwise.
 * @note Caller responsible to release memory allocated for outiparr.
 */
int iplist_split(char *iplist, char ***outiparr, int *max_outiparr) {
    char *list = NULL;
    char *list_iter = NULL;
    char *tok = NULL;
    char **arr = NULL;
    int max_arr = 0;
    if (iplist == NULL) {
        return (1);
    }
    if (!outiparr || !max_outiparr || !strlen(iplist)) {
        return (1);
    }
    
    list = strdup(iplist);
    list_iter = list;
    while ((tok = strchr(list_iter, ','))) {
        *tok = '\0';
        if (strlen(list_iter)) {
            arr = EUCA_REALLOC_C(arr, max_arr + 1, sizeof (char *));
            arr[max_arr] = strdup(list_iter);
            max_arr++;
        }
        list_iter = tok + 1;
    }
    if (strlen(list_iter)) {
        arr = EUCA_REALLOC_C(arr, max_arr + 1, sizeof (char *));
        arr[max_arr] = strdup(list_iter);
        max_arr++;
    }
    *outiparr = arr;
    *max_outiparr = max_arr;
    EUCA_FREE(list);
    return (0);
}

/**
 * Releases memory allocated by iplist_split().
 * @param iparr array of strings.
 * @param max_iparr number of strings in the array.
 * @return always 0.
 */
int iplist_arr_free(char **iparr, int max_iparr) {
    if (!iparr || !max_iparr) {
        return (0);
    }
    for (int i = 0; i < max_iparr; i++) {
        EUCA_FREE(iparr[i]);
    }
    EUCA_FREE(iparr);
    return (0);
}

/**
 * Splits eucanetd VPCMIDO router names (in the form of name_rtid) into name and
 * rtid parts.
 * @param routername [in] name of eucanetd VPCMIDO router of interest
 * @param name [out] string that represents the name of name_rtid. Memory allocated
 * for name should be released by the caller.
 * @param id [out] integer that represents the rtid of name_rtid
 * @return 0 on success. 1 on failure.
 */
int routername_split(char *routername, char **name, int *id) {
    if (!routername || !name || !id) {
        return (1);
    }
    char *instr = strdup(routername);
    char *rtid = NULL;
    if (!instr) {
        return (1);
    }
    for (int i = strlen(instr) - 2; i >= 0; i--) {
        if (instr[i] == '_') {
            instr[i] = '\0';
            rtid = &(instr[i + 1]);
            break;
        }
    }
    if (rtid && strlen(instr) && strlen(rtid)) {
        *name = strdup(instr);
        *id = atoi(rtid);
    }
    EUCA_FREE(instr);
    return (0);
}

/**
 * Logs the information in the given midoname data structure.
 * @param name [in] midoname data structure of interest.
 */
void mido_print_midoname(midoname *name) {
    if (name == NULL) {
        LOGWARN("Invalid argument: NULL midoname\n");
    } else {
        LOGTRACE("init=%d tenant=%s name=%s uuid=%s resource_type=%s media_type=%s\n",
                name->init, SP(name->tenant), SP(name->name), SP(name->uuid), SP(name->resource_type),
                SP(name->media_type));
    }
}

/**
 * Logs MidoNet-API information
 */
void mido_info_midonetapi(void) {
    if (strlen(midonet_api_version) && strlen(midonet_api_uribase)) {
        LOGINFO("\nmido API %s at %s\n", midonet_api_version, midonet_api_uribase);
    }
}

/**
 * Logs MIDOCACHE information.
 */
void mido_info_midocache(void) {
    int routers = 0;
    int rtports = 0;
    int rtroutes = 0;
    int bridges = 0;
    int brports = 0;
    int brdhcps = 0;
    int brdhcphosts = 0;
    int chains = 0;
    int chrules = 0;
    int ipaddrgroups = 0;
    int ipagips = 0;
    int hosts = 0;
    int haddrs = 0;
    int portgroups = 0;
    int pgports = 0;
    int tunnelzones = 0;
    int tzhosts = 0;

    if (midocache == NULL) {
        return;
    }
    for (int i = 0; i < midocache->max_routers; i++) {
        if (midocache->routers[i]) {
            routers++;
            for (int j = 0; j < midocache->routers[i]->max_ports; j++) {
                if (midocache->routers[i]->ports[j]) {
                    rtports++;
                }
            }
            for (int j = 0; j < midocache->routers[i]->max_routes; j++) {
                if (midocache->routers[i]->routes[j]) {
                    rtroutes++;
                }
            }
        }
    }
    for (int i = 0; i < midocache->max_bridges; i++) {
        if (midocache->bridges[i]) {
            bridges++;
            for (int j = 0; j < midocache->bridges[i]->max_ports; j++) {
                if (midocache->bridges[i]->ports[j]) {
                    brports++;
                }
            }
            for (int j = 0; j < midocache->bridges[i]->max_dhcps; j++) {
                if (midocache->bridges[i]->dhcps[j]) {
                    brdhcps++;
                    for (int k = 0; k < midocache->bridges[i]->dhcps[j]->max_dhcphosts; k++) {
                        if (midocache->bridges[i]->dhcps[j]->dhcphosts[k]) {
                            brdhcphosts++;
                        }
                    }
                }
            }
        }
    }
    for (int i = 0; i < midocache->max_chains; i++) {
        if (midocache->chains[i]) {
            chains++;
            for (int j = 0; j < midocache->chains[i]->max_rules; j++) {
                if (midocache->chains[i]->rules[j]) {
                    chrules++;
                }
            }
        }
    }
    for (int i = 0; i < midocache->max_ipaddrgroups; i++) {
        if (midocache->ipaddrgroups[i]) {
            ipaddrgroups++;
            for (int j = 0; j < midocache->ipaddrgroups[i]->max_ips; j++) {
                if (midocache->ipaddrgroups[i]->ips[j]) {
                    ipagips++;
                }
            }
        }
    }
    for (int i = 0; i < midocache->max_hosts; i++) {
        if (midocache->hosts[i]) {
            hosts++;
            if (midocache->hosts[i]->max_addresses) {
                haddrs += midocache->hosts[i]->max_addresses;
            }
        }
    }
    for (int i = 0; i < midocache->max_portgroups; i++) {
        if (midocache->portgroups[i]) {
            portgroups++;
            for (int j = 0; j < midocache->portgroups[i]->max_ports; j++) {
                if (midocache->portgroups[i]->ports[j]) {
                    pgports++;
                }
            }
        }
    }
    for (int i = 0; i < midocache->max_tunnelzones; i++) {
        if (midocache->tunnelzones[i]) {
            tunnelzones++;
            for (int j = 0; j < midocache->tunnelzones[i]->max_hosts; j++) {
                if (midocache->tunnelzones[i]->hosts[j]) {
                    tzhosts++;
                }
            }
        }
    }

    LOGINFO("MIDOCACHE: mnbuffer %d allocated / %d released\n", midocache_midos->size, midocache_midos->released);
    LOGINFO("\t%d routers (%d ports, %d routes)\n", routers, rtports, rtroutes);
    LOGINFO("\t%d bridges (%d ports, %d dhcps, %d dhcphosts)\n", bridges, brports, brdhcps, brdhcphosts);
    LOGINFO("\t%d chains (%d rules)\n", chains, chrules);
    LOGINFO("\t%d ipags (%d ips)\n", ipaddrgroups, ipagips);
    LOGINFO("\t%d hosts (%d addrs), %d portgroups (%d ports)\n", hosts, haddrs, portgroups, pgports);
    LOGINFO("\t%d tunnelzones (%d hosts)\n", tunnelzones, tzhosts);
}

/**
 * Logs the API HTTP request counts (diff from a previous call)
 *
 * @see mido_info_http_count_total() for cumulative count.
 */
void mido_info_http_count(void) {
    LOGINFO("MidoNet API requests: %d gets, %d puts, %d posts, %d deletes\n", 
            http_gets - http_gets_prev, http_puts - http_puts_prev,
            http_posts - http_posts_prev, http_deletes - http_deletes_prev);
    http_gets_prev = http_gets;
    http_puts_prev = http_puts;
    http_posts_prev = http_posts;
    http_deletes_prev = http_deletes;
}

/**
 * Logs the API HTTP request counts (cumulative count)
 *
 * @see mido_info_http_count() for counts between calls.
 */
void mido_info_http_count_total(void)
{
    LOGINFO("Total mido requests: %d gets, %d puts, %d posts, %d deletes\n", 
            http_gets, http_puts, http_posts, http_deletes);
    long int getdiv = http_gets ? http_gets : (http_gets + 1);
    long int putdiv = http_puts ? http_puts : (http_puts + 1);
    long int postdiv = http_posts ? http_posts : (http_posts + 1);
    long int deldiv = http_deletes ? http_deletes : (http_deletes + 1);
    LOGINFO("\t%.2f ms/get, %.2f ms/put, %.2f ms/post, %.2f ms/del\n", 
            http_gets_time / getdiv / 1000.0, http_puts_time / putdiv / 1000.0,
            http_posts_time / postdiv / 1000.0, http_deletes_time / deldiv / 1000.0);
}

/**
 * Clears the mido_parsed_route structure in the argument. Allocated memory is released.
 *
 * @param route [in] parsed route entry of interest
 */
void mido_free_mido_parsed_route(mido_parsed_route *route) {
    if (!route) {
        return;
    }
    mido_free_midoname(&(route->router));
    mido_free_midoname(&(route->rport));
    EUCA_FREE(route->src_net);
    EUCA_FREE(route->src_length);
    EUCA_FREE(route->dst_net);
    EUCA_FREE(route->dst_length);
    EUCA_FREE(route->next_hop_ip);
    EUCA_FREE(route->weight);
    bzero(route, sizeof(mido_parsed_route));
}

/**
 * Clears the list of mido_parsed_route structures in the argument.
 *
 * @param routes [in] array of mido_parsed_route structures
 * @param max_routes [in] number of array elements
 */
void mido_free_mido_parsed_route_list(mido_parsed_route *routes, int max_routes) {
    int i = 0;
    if (!routes) return;
    for (i = 0; i < max_routes; i++) {
        mido_free_mido_parsed_route(&(routes[i]));
    }
}

/**
 * Releases memory allocated for the given array of midoname data structures
 *
 * @param name [in] array of midoname data structures of interest
 * @param max_name [in] entries in the array
 */
void mido_free_midoname_list(midoname *name, int max_name) {
    for (int i = 0; i < max_name; i++) {
        mido_free_midoname(&(name[i]));
    }
}

/**
 * Releases memory allocated for the given midoname data structure.
 * @param name [in] pointer to the midoname data structure of interest.
 */
void mido_free_midoname(midoname *name) {
    if (!name) {
        return;
    }
    EUCA_FREE(name->name);
    EUCA_FREE(name->uuid);
    EUCA_FREE(name->tenant);
    EUCA_FREE(name->jsonbuf);
    EUCA_FREE(name->resource_type);
    EUCA_FREE(name->media_type);
    EUCA_FREE(name->uri);
    if (name->ipagip) {
        EUCA_FREE(name->ipagip->ip);
        EUCA_FREE(name->ipagip);
    }
    if (name->rule) {
        EUCA_FREE(name->rule->type);
        EUCA_FREE(name->rule->nwdstaddress);
        EUCA_FREE(name->rule->nwdstlength);
        EUCA_FREE(name->rule->nwsrcaddress);
        EUCA_FREE(name->rule->nwsrclength);
        EUCA_FREE(name->rule->nattarget);
        EUCA_FREE(name->rule->jumpchainid);
        EUCA_FREE(name->rule);
    }
    if (name->port) {
        EUCA_FREE(name->port->type);
        EUCA_FREE(name->port->hostid);
        EUCA_FREE(name->port->peerid);
        EUCA_FREE(name->port->ifname);
        EUCA_FREE(name->port->netaddr);
        EUCA_FREE(name->port->netlen);
        EUCA_FREE(name->port->portaddr);
        EUCA_FREE(name->port->portmac);
        EUCA_FREE(name->port);
    }
    if (name->route) {
        EUCA_FREE(name->route->srcnet);
        EUCA_FREE(name->route->srclen);
        EUCA_FREE(name->route->dstnet);
        EUCA_FREE(name->route->dstlen);
        EUCA_FREE(name->route->type);
        EUCA_FREE(name->route->nexthopport);
        EUCA_FREE(name->route->nexthopgateway);
        EUCA_FREE(name->route->weight);
        EUCA_FREE(name->route);
    }
    bzero(name, sizeof(midoname));
}

/**
 * Retrieves an element that corresponds to the given key from name.
 *
 * @param name [in] midoname data structure from where the information will be extracted
 * @param key [in] key of interest
 * @param val [out] value of interest
 *
 * @return 0 on success. 1 on failure.
 */
int mido_getel_midoname(midoname *name, char *key, char **val) {
    int ret = 0;
    json_object *jobj = NULL;

    if (!name || !key || !val || (!name->init)) {
        return (1);
    }

    *val = NULL;
    jobj = json_tokener_parse(name->jsonbuf);
    if (jobj) {
        json_object_object_foreach(jobj, elkey, elval) {
            if (!*val && elkey && elval) {
                if (!strcmp(elkey, key)) {
                    *val = strdup(SP(json_object_get_string(elval)));
                }
            }
        }
        json_object_put(jobj);
    }

    if (*val == NULL) {
        ret = 1;
    }

    return (ret);
}

/**
 * Parses an json array that is a value of the given key, and returns as an array
 * of strings.
 *
 * @param name [in] midoname containing the jsonbuf of interest.
 * @param key [in] json key of interest.
 * @param values [out] array of parsed strings.
 * @param max_values [out] number of elements in the returning array.
 *
 * @return 0 on success. 1 on failure.
 *
 * @note caller is responsible for releasing memory allocated for results.
 */
int mido_getarr_midoname(midoname *name, char *key, char ***values, int *max_values) {
    int ret = 0;
    json_object *jobj = NULL;
    json_object *jarr = NULL;
    json_object *jarrel = NULL;
    int jarr_len = 0;
    char **res;

    if (!name || !key || !values || !max_values || (!name->init)) {
        LOGWARN("Invalid argument: NULL pointer.\n");
        return (1);
    }
    LOGEXTREME("searching for %s", key);

    *values = NULL;
    *max_values = 0;
    jobj = json_tokener_parse(name->jsonbuf);
    if (jobj) {
        json_object_object_get_ex(jobj, key, &jarr);
        if ((jarr == NULL) || (!json_object_is_type(jarr, json_type_array))) {
            ret = 1;
        } else {
            jarr_len = json_object_array_length(jarr);
            LOGEXTREME("\tfound %d\n", jarr_len);
            if (jarr_len > 0) {
                res = EUCA_ZALLOC_C(jarr_len, sizeof (char *));
                for (int i = 0; i < jarr_len; i++) {
                    jarrel = json_object_array_get_idx(jarr, i);
                    res[i] = strdup(json_object_get_string(jarrel));
                    LOGEXTREME("\t%d %s\n", i, res[i]);
                }
                *values = res;
                *max_values = jarr_len;
            }
        }
        json_object_put(jobj);
    }

    return (ret);
}

/**
 * Creates a tunnel-zone named name in MidoNet.
 * @param name [in] name of the tunnel-zone to be created.
 * @param type [in] type type of the tunnel-zone (gre|vxlan)
 * @param outname [i/o] pointer to an extant MidoNet tunnel-zone (parameters will be checked
 * to avoid duplicate resource creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created resource
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
midonet_api_tunnelzone *mido_create_tunnelzone(char *name, char *type, midoname **outname) {
    int rc;
    midoname myname;
    midoname *out = NULL;
    midonet_api_tunnelzone *tz = NULL;
    
    if (outname && *outname) {
        out = *outname;
    }
    if (out && out->init) {
        if (!strcmp(name, out->name)) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            return (midonet_api_cache_lookup_tunnelzone(out));
        }
        out = NULL;
    } else {
        midoname tmp;
        tmp.name = strdup(name);
        tz = midonet_api_cache_lookup_tunnelzone(&tmp);
        EUCA_FREE(tmp.name);
        if (tz) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            if (outname) {
                *outname = tz->obj;
            }
            return (tz);
        }
    }

    memset(&myname, 0, sizeof(midoname));
    myname.tenant = strdup(VPCMIDO_TENANT);
    myname.name = strdup(name);
    myname.resource_type = strdup("tunnel_zones");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_TUNNEL_ZONE_JSON]);

    tz = NULL;
    rc = mido_create_resource(NULL, 0, &myname, &out, "name", myname.name, "type", type, NULL);
    if (rc == 0) {
        // cache newly created tunnelzone
        tz = midonet_api_cache_add_tunnelzone(out);
    }
    if (outname) {
        *outname = out;
    }
    mido_free_midoname(&myname);
    return (tz);
}

/**
 * Retrieves an array of pointers to midonet object representing tunnel-zone.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing tunnel-zone, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_tunnelzones(char *tenant, midoname ***outnames, int *outnames_max) {
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_tunnelzone **tzones = midocache->tunnelzones;
        if ((tzones != NULL) && (midocache->max_tunnelzones > 0)) {
            *outnames_max = midocache->max_tunnelzones;
            *outnames = EUCA_ZALLOC_C(*outnames_max, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < *outnames_max; i++) {
                names[i] = tzones[i]->obj;
            }
        }
        return (0);
    }
    return (mido_get_resources(NULL, 0, tenant, "tunnel_zones",
            midonet_api_mtypes[APPLICATION_COLLECTION_TUNNEL_ZONE_JSON],
            midonet_api_mtypes[APPLICATION_TUNNEL_ZONE_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves an array of pointers to midonet object representing GRE tunnel-zone hosts.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing GRE tunnel-zone host, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_tunnelzone_hosts(midoname *tzone, midoname ***outnames, int *outnames_max) {
    if (midocache != NULL) {
        midonet_api_tunnelzone *tunnelzone = midonet_api_cache_lookup_tunnelzone(tzone);
        if (tunnelzone == NULL) {
            LOGWARN("Unable to find %s in midocache\n", tzone->name);
            return (1);
        }

        *outnames_max = tunnelzone->max_hosts;
        *outnames = EUCA_ZALLOC_C(*outnames_max, sizeof (midoname *));
        midoname **names = *outnames;
        for (int i = 0; i < *outnames_max; i++) {
            names[i] = tunnelzone->hosts[i];
        }
        return (0);
    }
    return (mido_get_resources(tzone, 1, tzone->tenant, "hosts",
            midonet_api_mtypes[APPLICATION_COLLECTION_TUNNEL_ZONE_HOST_JSON],
            midonet_api_mtypes[APPLICATION_TUNNEL_ZONE_HOST_JSON],
            outnames, outnames_max));
}

/**
 * Creates a new tunnel-zone member as specified in the argument. 
 *
 * @param tz [in] midonet_api_tunnelzone structure of interest. This parameter
 * has priority over tunnelzone. If tz is NULL, tzmn is used to search
 * midocache.
 * @param tzmn [in] tunnel-zone of interest.
 * @param host [in] host that will be added to the tunnel-zone.
 * @param ip [in] IP address of the host to be used in the tunnel-zone.
 * @param outname [i/o] pointer to an extant MidoNet member host (parameters will be checked
 * to avoid duplicate host creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created host will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the host is successfully created/found. 1 otherwise.
 */
int mido_create_tunnelzone_member(midonet_api_tunnelzone *tz, midoname *tzmn,
        midoname *host, char *ip, midoname **outname) {
    int rc = 0, ret = 0, max_hosts = 0, found = 0;
    midoname **hosts = NULL;
    midoname myname = { 0 };
    midoname *out = NULL;
    char *hostId = NULL;
    char *ipAddress = NULL;

    if (!tz && !tzmn) {
        LOGWARN("Invalid argument: cannot create member in a NULL tunnelzone.\n");
        return (1);
    }

    midonet_api_tunnelzone *ctz = NULL;
    if (tz != NULL) {
        ctz = tz;
    } else {
        ctz = midonet_api_cache_lookup_tunnelzone(tzmn);
    }
    if (ctz == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", tzmn->name);
        return (1);
    } else {
        hosts = ctz->hosts;
        max_hosts = ctz->max_hosts;
    }
    if (outname && *outname) {
        out = *outname;
    }

    if (out && out->init) {
        mido_getel_midoname(out, "hostId", &hostId);
        mido_getel_midoname(out, "ipAddress", &ipAddress);
        if (hostId && ipAddress) {
            LOGEXTREME("found %s %s\n", hostId, ipAddress);
            if (!strcmp(hostId, host->uuid) && !strcmp(ipAddress, ip)) {
                found = 1;
                LOGTRACE("tz member already in mido - abort create\n");
            }
        }
        EUCA_FREE(hostId);
        EUCA_FREE(ipAddress);
    }
    if (!found) {
        for (int i = 0; i < max_hosts && !found; i++) {
            mido_getel_midoname(hosts[i], "hostId", &hostId);
            mido_getel_midoname(hosts[i], "ipAddress", &ipAddress);
            if (hostId && ipAddress) {
                if (!strcmp(hostId, host->uuid) && !strcmp(ipAddress, ip)) {
                    found = 1;
                    if (outname) {
                        *outname = hosts[i];
                    }
                    LOGTRACE("tz member already in mido - abort create\n");
                }
            }
            EUCA_FREE(hostId);
            EUCA_FREE(ipAddress);
        }
    }

    if (!found) {
        memset(&myname, 0, sizeof (midoname));
        myname.tenant = strdup(ctz->obj->tenant);
        myname.resource_type = strdup("hosts");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_TUNNEL_ZONE_HOST_JSON]);

        LOGTRACE("\tadding %s/%s to %s\n", host->name, ip, ctz->obj->name);
        rc = mido_create_resource(ctz->obj, 1, &myname, &out, "ipAddress", ip, "hostId", host->uuid, NULL);
        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            midonet_api_cache_add_tunnelzone_host(ctz, out);
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }
    return (ret);
}

/**
 * Creates a router in MidoNet.
 * @param tenant [in] name of the MidoNet tenant.
 * @param name [in] name of the router to be created.
 * @param outname [i/o] pointer to an extant MidoNet router (parameters will be checked
 * to avoid duplicate router creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created router
 * will not be returned.
 * @return Pointer to the newly created router. NULL otherwise.
 */
midonet_api_router *mido_create_router(char *tenant, char *name, midoname **outname) {
    int rc = 0;
    midoname myname;
    midoname *out = NULL;
    midonet_api_router *rt = NULL;
    
    if (outname && *outname) {
        out = *outname;
    }
    if (out && out->init) {
        if (!strcmp(name, out->name)) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            return (midonet_api_cache_lookup_router(out, NULL));
        }
        out = NULL;
    } else {
        midoname tmp;
        tmp.name = strdup(name);
        rt = midonet_api_cache_lookup_router(&tmp, NULL);
        EUCA_FREE(tmp.name);
        if (rt) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            if (outname) {
                *outname = rt->obj;
            }
            return (rt);
        }
    }

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("routers");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_ROUTER_JSON]);

    rt = NULL;
    rc = mido_create_resource(NULL, 0, &myname, &out, "name", myname.name, NULL);
    if (rc == 0) {
        // cache newly created router
        rt = midonet_api_cache_add_router(out);
    }
    if (outname) {
        *outname = out;
    }
    mido_free_midoname(&myname);
    return (rt);
}

/**
 * Updates information about a router with the parameters in the variable argument section.
 * @param [in] name midoname structure of the router of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_router(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Logs the information about a router represented by name.
 * @param name [in] MN router of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_router(midoname *name) {
    return (mido_print_resource("routers", name));
}

/**
 * Deletes the given router from MidoNet.
 * @param name [in] router (assumed without check) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_router(midoname *name) {
    int rc = 0;
    if (!name || !name->name) {
        return (1);
    }
    midonet_api_cache_del_router(name);
    rc = mido_delete_resource(NULL, name);

    return (rc);
}

/**
 * Creates a bridge in MidoNet.
 * @param tenant [in] name of the MidoNet tenant.
 * @param name [in] name of the bridge to be created.
 * @param outname [i/o] pointer to an extant MidoNet bridge (parameters will be checked
 * to avoid duplicate bridge creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created bridge
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
midonet_api_bridge *mido_create_bridge(char *tenant, char *name, midoname **outname) {
    int rc;
    midoname myname;
    midoname *out = NULL;
    midonet_api_bridge *br = NULL;
    
    if (outname && *outname) {
        out = *outname;
    }
    if (out && out->init) {
        if (!strcmp(name, out->name)) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            return (midonet_api_cache_lookup_bridge(out, NULL));
        }
        out = NULL;
    } else {
        midoname tmp;
        tmp.name = strdup(name);
        br = midonet_api_cache_lookup_bridge(&tmp, NULL);
        EUCA_FREE(tmp.name);
        if (br) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            if (outname) {
                *outname = br->obj;
            }
            return (br);
        }
    }

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("bridges");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_BRIDGE_JSON]);

    br = NULL;
    rc = mido_create_resource(NULL, 0, &myname, &out, "name", myname.name, NULL);
    if (rc == 0) {
        // cache newly created bridge
        br = midonet_api_cache_add_bridge(out);
    }
    if (outname) {
        *outname = out;
    }
    mido_free_midoname(&myname);
    return (br);
}

/**
 * Updates information about a bridge with the parameters in the variable argument section.
 * @param [in] name midoname structure of the bridge of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_bridge(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Logs the information about a bridge represented by name.
 * @param name [in] MN bridge of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_bridge(midoname *name) {
    return (mido_print_resource("bridges", name));
}

/**
 * Deletes the given bridge from MidoNet.
 * @param name [in] bridge of interest (no check is performed - caller responsible to
 * make sure it is a bridge).
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_bridge(midoname *name) {
    int rc = 0;
    if (!name || !name->name) {
        return (1);
    }
    midonet_api_cache_del_bridge(name);
    rc = mido_delete_resource(NULL, name);
    return (rc);
}

/**
 * Creates a router in MidoNet.
 * @param tenant [in] of the MidoNet tenant.
 * @param name [in] name of the port-group to be created.
 * @param outname [i/o] pointer to an extant MidoNet port-group (parameters will be checked
 * to avoid duplicate creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created port-group
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
int mido_create_portgroup(char *tenant, char *name, midoname **outname) {
    int rc = 0;
    midoname myname;
    midoname *out = NULL;
    midonet_api_portgroup *pg = NULL;
    
    if (outname && *outname) {
        out = *outname;
    }
    if (out && out->init) {
        if (!strcmp(name, out->name)) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            return (0);
        }
    } else {
        midoname tmp;
        tmp.name = strdup(name);
        pg = midonet_api_cache_lookup_portgroup(&tmp, NULL);
        EUCA_FREE(tmp.name);
        if (pg) {
            LOGEXTREME("%s already in mido - abort create.\n", name);
            if (outname) {
                *outname = pg->obj;
            }
            return (0);
        }
        out = NULL;
    }

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("port_groups");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_PORT_GROUP_JSON]);

    rc = mido_create_resource(NULL, 0, &myname, &out, "name", myname.name, "stateful", "true", NULL);
    if (rc == 0) {
        // cache newly created port-group
        rc = midonet_api_cache_add_portgroup(out);
    }
    if (outname) {
        *outname = out;
    }
    mido_free_midoname(&myname);
    return (rc);
}

/**
 * Updates information about a port-group with the parameters in the variable argument section.
 * @param [in] name midoname structure of the port-group of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_portgroup(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Deletes the given port-group from MidoNet.
 * @param name [in] port-group (assumed without check) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_portgroup(midoname *name) {
    int rc = 0;
    if (!name || !name->name) {
        return (1);
    }
    midonet_api_cache_del_portgroup(name);
    rc = mido_delete_resource(NULL, name);

    return (rc);
}

/**
 * Logs the information about a port-group represented by name.
 * @param name [in] MN port-group of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_portgroup(midoname *name) {
    return (mido_print_resource("port_groups", name));
}

/**
 * Retrieves an array of pointers to midonet object representing port-group.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing port-group, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_portgroups(char *tenant, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_portgroup **pgroups = midocache->portgroups;
        if ((pgroups != NULL) && (midocache->max_portgroups > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_portgroups, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_portgroups; i++) {
                if (pgroups[i] == NULL) {
                    continue;
                }
                names[count] = pgroups[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(NULL, 0, tenant, "port_groups",
            midonet_api_mtypes[APPLICATION_COLLECTION_PORT_GROUP_JSON],
            midonet_api_mtypes[APPLICATION_PORT_GROUP_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves a midonet object that represents the portgroup in the argument from midocache.
 * @param name [in] name of the MidoNet portgroup of interest.
 * @return pointer to the data structure that represents the portgroup, when found. NULL otherwise.
 */
midonet_api_portgroup *mido_get_portgroup(char *name) {
    midoname tmp;
    midonet_api_portgroup *res = NULL;
    if (midocache != NULL) {
        tmp.name = strdup(name);
        res = midonet_api_cache_lookup_portgroup(&tmp, NULL);
        EUCA_FREE(tmp.name);
        return (res);
    }
    return (NULL);
}

/**
 * Adds the router port in the argument to the port-group in the argument.
 * @param portgroup [in] port-group (not checked) of interest.
 * @param port [in] port (not checked) to be added to the port-group.
 * @param outname [i/o] pointer to an extant MidoNet port-group port (parameters will be checked
 * to avoid duplicate creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created port
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
int mido_create_portgroup_port(midoname *portgroup, midoname *port, midoname **outname) {
    int rc = 0, max_ports = 0;
    midoname myname, **ports = NULL;
    midoname *out = NULL;

    if (portgroup == NULL) {
        LOGWARN("Invalid argument: cannot create port-group port for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    // get the port-group information from midocache
    midonet_api_portgroup *pg = midonet_api_cache_lookup_portgroup(portgroup, NULL);
    if (pg == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", portgroup->name);
        return (1);
    } else {
        ports = pg->ports;
        max_ports = pg->max_ports;
    }
    if (out && out->init) {
        for (int i = 0; i < max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (ports[i] == out) {
                LOGEXTREME("port %s already a member of %s - abort create\n", out->uuid, portgroup->name);
                return (0);
            }
        }
        out = NULL;
    }
    for (int i = 0; i < max_ports; i++) {
        if (strstr(ports[i]->uuid, port->uuid)) {
            if (outname) {
                *outname = ports[i];
            }
            LOGEXTREME("port %s already a member of %s - abort create.\n", ports[i]->uuid, portgroup->name);
            return (0);
        }
    }

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(portgroup->tenant);
    myname.resource_type = strdup("ports");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_PORT_GROUP_PORT_JSON]);

    rc = mido_create_resource(portgroup, 1, &myname, &out, "portId", port->uuid, NULL);
    if (rc == 0) {
        midonet_api_cache_add_portgroup_port(pg, out);
    }
    if (outname) {
        *outname = out;
    }

    mido_free_midoname(&myname);
    return (rc);
}

/**
 * Deletes the given port-group port from MidoNet.
 * @param portgroup [in] port-group (no checks) of interest.
 * @param port [in] port-group port (no checks) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_portgroup_port(midoname *portgroup, midoname *port) {
    if (!portgroup || !portgroup->name || !port || !port->uuid) {
        return (1);
    }
    midonet_api_portgroup *pg = midonet_api_cache_lookup_portgroup(portgroup, NULL);
    if (pg == NULL) {
        LOGWARN("Unable to find %s in midocache\n", portgroup->name);
        return (1);
    }
    midonet_api_cache_del_portgroup_port(pg, port);
    return (mido_delete_resource(NULL, port));
}

/**
 * Retrieves an array of pointers to midonet object representing port-group.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing port-group, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_portgroup_ports(midoname *portgroup, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_portgroup *pg = midonet_api_cache_lookup_portgroup(portgroup, NULL);
        if (pg == NULL) {
            LOGWARN("MIDOCACHE: %s not found\n", portgroup->name);
        } else {
            if (pg->max_ports > 0) {
                *outnames = EUCA_ZALLOC_C(pg->max_ports, sizeof (midoname *));
            }
            midoname **names = *outnames;
            for (int i = 0; i < pg->max_ports; i++) {
                if (pg->ports[i] == NULL) {
                    continue;
                }
                names[count] = pg->ports[i];
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(portgroup, 1, portgroup->tenant, "ports",
            midonet_api_mtypes[APPLICATION_COLLECTION_PORT_GROUP_PORT_JSON],
            midonet_api_mtypes[APPLICATION_PORT_GROUP_PORT_JSON],
            outnames, outnames_max));
}

/**
 * Creates a ipaddrgroup in MidoNet.
 * @param tenant [in] name of the MidoNet tenant.
 * @param name [in] name of the ipaddrgroup to be created.
 * @param outname [i/o] pointer to an extant MidoNet ipaddrgroup (parameters will be checked
 * to avoid duplicate ipaddrgroup creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created ipaddrgroup
 * will not be returned.
 * @return Pointer to the newly created ip-address-group. NULL on any error.
 */
midonet_api_ipaddrgroup *mido_create_ipaddrgroup(char *tenant, char *name, midoname **outname) {
    int rc;
    midoname myname;
    midoname *out = NULL;
    midonet_api_ipaddrgroup *ig = NULL;
    
    if (outname && *outname) {
        out = *outname;
    }
    if (out && out->init) {
        if (!strcmp(name, out->name)) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            return (midonet_api_cache_lookup_ipaddrgroup(out, NULL));
        }
        out = NULL;
    } else {
        midoname tmp;
        tmp.name = strdup(name);
        ig = midonet_api_cache_lookup_ipaddrgroup(&tmp, NULL);
        EUCA_FREE(tmp.name);
        if (ig) {
            LOGEXTREME("%s already in mido - abort create.\n", name);
            if (outname) {
                *outname = ig->obj;
            }
            return (ig);
        }
    }
    
    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("ip_addr_groups");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_IP_ADDR_GROUP_JSON]);

    ig = NULL;
    rc = mido_create_resource(NULL, 0, &myname, &out, "name", myname.name, NULL);
    if (rc == 0) {
        // cache newly created ipaddrgroup
        ig = midonet_api_cache_add_ipaddrgroup(out);
    }
    if (outname) {
        *outname = out;
    }
    mido_free_midoname(&myname);
    return (ig);
}

/**
 * Updates information about an ip-addr-group with the parameters in the variable argument section.
 * @param [in] name midoname structure of the ip-addr-group of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_ipaddrgroup(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Deletes the given ipaddrgroup from MidoNet.
 * @param name [in] ipaddrgroup of interest (no check is performed - caller responsible to
 * make sure it is an ipaddrgroup).
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_ipaddrgroup(midoname *name) {
    int rc = 0;
    if (!name || !name->name) {
        return (1);
    }
    midonet_api_cache_del_ipaddrgroup(name);
    rc = mido_delete_resource(NULL, name);
    return (rc);
}

/**
 * Logs the information about an ip-address-group represented by name.
 * @param name [in] MN port-group of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_ipaddrgroup(midoname *name) {
    return (mido_print_resource("ip_addr_groups", name));
}

/**
 * Retrieves an array of pointers to midonet object representing ip-address-groups.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing ip-address-groups, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_ipaddrgroups(char *tenant, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_ipaddrgroup **ipaddrgroups = midocache->ipaddrgroups;
        if ((ipaddrgroups != NULL) && (midocache->max_ipaddrgroups > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_ipaddrgroups, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_ipaddrgroups; i++) {
                if (ipaddrgroups[i] == NULL) {
                    continue;
                }
                names[count] = ipaddrgroups[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(NULL, 0, tenant, "ip_addr_groups",
            midonet_api_mtypes[APPLICATION_COLLECTION_IP_ADDR_GROUP_JSON],
            midonet_api_mtypes[APPLICATION_IP_ADDR_GROUP_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves a midonet object that represents the ip-address-group in the argument from midocache.
 * @param name [in] name of the MidoNet ip-address-group of interest.
 * @return pointer to the data structure that represents the ipaddrgroup, when found. NULL otherwise.
 */
midonet_api_ipaddrgroup *mido_get_ipaddrgroup(char *name) {
    if (midocache != NULL) {
        if (midocache->sorted_ipaddrgroups == 0) {
            qsort(midocache->ipaddrgroups, midocache->max_ipaddrgroups,
                    sizeof (midonet_api_ipaddrgroup *), compare_midonet_api_ipaddrgroup);
            midocache->sorted_ipaddrgroups = 1;
        }
        midoname tmp;
        tmp.name = strdup(name);
        midonet_api_ipaddrgroup ipag;
        ipag.obj = &tmp;
        midonet_api_ipaddrgroup *pipag = &ipag;
        midonet_api_ipaddrgroup **res = (midonet_api_ipaddrgroup **) bsearch(&pipag,
                midocache->ipaddrgroups, midocache->max_ipaddrgroups,
                sizeof (midonet_api_ipaddrgroup *), compare_midonet_api_ipaddrgroup);
        EUCA_FREE(tmp.name);
        if (res) {
            return (*res);
        }
    }
    return (NULL);
}

/**
 * Searches a matching dhcp in the given array of dhcps.
 * @param dhcps [in] pointer to an array of pointers to midoname structures that represent dhcps.
 * @param max_dhcps number of elements in dhcps array.
 * @param subnet [in] dhcp network address.
 * @param slashnet [in] dhcp network prefix length.
 * @param gw [in] gateway IP address of the dhcp subnet.
 * @param dnsServers [in] DNS servers to be offered to dhcp clients.
 * @param max_dnsServers [in] number of DNS servers to be offered to dhcp clients.
 * @param foundidx index to the array of the matching dhcp, if found.
 * @return 0 if a matching dhcp is found. 1 otherwise.
 */
int mido_find_dhcp_from_list(midoname **dhcps, int max_dhcps, char *subnet, char *slashnet,
        char *gw, u32 *dnsServers, int max_dnsServers, int *foundidx) {
    int rc = 0, found = 0, ret = 0;
    int i = 0;

    if ((dhcps == NULL) || (max_dhcps == 0)) {
        return (1);
    }

    // Parse DNS Servers
    char *dnslist = EUCA_ZALLOC_C(max_dnsServers * 16, sizeof (char));
    char *dlpos = dnslist;
    for (i = 0; i < max_dnsServers; i++) {
        char *tmpsrv = hex2dot(dnsServers[i]);
        snprintf(dlpos, 17, "%s,", tmpsrv);
        EUCA_FREE(tmpsrv);
        dlpos = dnslist + strlen(dnslist);
    }
    if (strlen(dnslist) != 0) {
        dlpos--;
        dlpos = '\0';
    } else {
        snprintf(dnslist, 16, "8.8.8.8");
    }
    char *dnsjson = NULL;
    dnsjson = mido_get_json(NULL, "dnsServerAddrs", "jsonarr",
            "dnsServerAddrs:LIST", dnslist, "dnsServerAddrs:END", "END", NULL);
    char *dnssrvs = strchr(dnsjson, ']');
    if (dnssrvs == NULL) {
        LOGWARN("possible bug in dns servers json\n");
    } else {
        dnssrvs[1] = '\0';
        dnssrvs = strchr(dnsjson, '[');
        if (dnssrvs == NULL) {
            LOGWARN("possible bug in dns servers json.\n");
        } else {
            found = 0;
            for (i = 0; i < max_dhcps && !found; i++) {
                if ((dhcps[i] == NULL) || (dhcps[i]->init == 0)) {
                    continue;
                }
                rc = mido_cmp_midoname_to_input(dhcps[i], "subnetPrefix", subnet, "subnetLength", slashnet,
                        "defaultGateway", gw, "dnsServerAddrs", dnssrvs, NULL);
                if (!rc) {
                    found = 1;
                    if (foundidx) {
                        *foundidx = i;
                    }
                }
            }
        }
    }

    if (found) {
        ret = 0;
    } else {
        ret = 1;
    }
    EUCA_FREE(dnslist);
    EUCA_FREE(dnsjson);
    return (ret);
}

/**
 * Creates a dhcp on the specified bridge in MidoNet.
 * @param br [in] bridge of interest. br has priority over devname. If br is NULL,
 * midocache lookup is performed based on devname.
 * @param devname [in] bridge of interest.
 * @param subnet [in] dhcp network address.
 * @param slashnet [in] dhcp network prefix length.
 * @param gw [in] gateway IP address of the dhcp subnet.
 * @param dnsServers [in] DNS servers to be offered to dhcp clients.
 * @param max_dnsServers [in] number of DNS servers to be offered to dhcp clients.
 * @param outname [i/o] pointer to an extant MidoNet bridge dhcp (parameters will be checked
 * to avoid duplicate dhcp creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created bridge dhcp
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
int mido_create_dhcp(midonet_api_bridge *br, midoname *devname, char *subnet, char *slashnet, char *gw,
        u32 *dnsServers, int max_dnsServers, midoname **outname) {
    int rc = 0;
    int ret = 0;
    midoname myname;
    int found = 0;
    int foundidx = 0;
    midonet_api_dhcp *founddhcp = NULL;

    midoname *out = NULL;
    
    if ((br == NULL) && (devname == NULL)) {
        LOGWARN("Invalid argument: cannot create dhcp for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    if (br == NULL) {
        br = midonet_api_cache_lookup_bridge(devname, NULL);
    }
    if (br == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", devname->name);
        return (1);
    }
    if (out && out->init) {
        founddhcp = midonet_api_cache_lookup_dhcp(br, out, NULL);
        if (founddhcp) {
            found = 1;
            LOGEXTREME("dhcp already in mido - abort create\n");
            return (0);
        }
    }
    founddhcp = midonet_api_cache_lookup_dhcp_byparam(br, subnet, slashnet, gw, dnsServers, max_dnsServers, &foundidx);
    if (founddhcp != NULL) {
        found = 1;
        LOGEXTREME("dhcp already in mido - abort create.\n");
        if (outname) {
            *outname = founddhcp->obj;
        }
    }

    if (!found) {
        bzero(&myname, sizeof (midoname));
        myname.tenant = strdup(br->obj->tenant);
        myname.resource_type = strdup("dhcp");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_DHCP_SUBNET_JSON]);

        // Parse DNS Servers
        char *dnslist = EUCA_ZALLOC_C(max_dnsServers * 16, sizeof (char));
        char *dlpos = dnslist;
        for (int i = 0; i < max_dnsServers; i++) {
            char *tmpsrv = hex2dot(dnsServers[i]);
            snprintf(dlpos, 17, "%s,", tmpsrv);
            EUCA_FREE(tmpsrv);
            dlpos = dnslist + strlen(dnslist);
        }
        if (strlen(dnslist) != 0) {
            dlpos--;
            dlpos = '\0';
        } else {
            snprintf(dnslist, 16, "8.8.8.8");
        }

        rc = mido_create_resource(br->obj, 1, &myname, &out, "subnetPrefix", subnet,
                "subnetLength", slashnet, "defaultGateway", gw, "dnsServerAddrs", "jsonarr",
                "dnsServerAddrs:LIST", dnslist, "dnsServerAddrs:END", "END", NULL);
        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            midonet_api_cache_add_dhcp(br, out);
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        EUCA_FREE(dnslist);
        mido_free_midoname(&myname);
    }
    return (ret);
}

/**
 * Updates information about a dhcp with the parameters in the variable argument section.
 * @param [in] name midoname structure of the dhcp of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_dhcp(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Logs the information about a dhcp represented by name.
 * @param name [in] MN port-group of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_dhcp(midoname *name) {
    return (mido_print_resource("dhcp", name));
}

/**
 * Deletes a dhcp from MidoNet.
 * @param devname [in] bridge (no checks performed) of interest.
 * @param name [in] dhcp (no checks performed) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_dhcp(midonet_api_bridge *devname, midoname *name) {
    int rc = 0;
    if (!devname || !devname->obj || !name || !name->uuid) {
        return (1);
    }
    midonet_api_cache_del_dhcp(devname, name);
    rc = mido_delete_resource(NULL, name);

    return (rc);
}

/**
 * Retrieves an array of pointers to midonet object representing bridge dhcps.
 * @param devname [in] bridge of interest.
 * @param outnames [out] an array of pointers to midonet objects representing bridge dhcps, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_dhcps(midoname *devname, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_bridge *br = midonet_api_cache_lookup_bridge(devname, NULL);
        if (br == NULL) {
            LOGWARN("MIDOCACHE: %s not found\n", devname->name);
        } else {
            if (br->max_dhcps > 0) {
                *outnames = EUCA_ZALLOC_C(br->max_dhcps, sizeof (midoname *));
            }
            midoname **dhcps = *outnames;
            for (int i = 0; i < br->max_dhcps; i++) {
                if (br->dhcps[i] == NULL) {
                    continue;
                }
                dhcps[count] = br->dhcps[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(devname, 1, devname->tenant, "dhcp",
            midonet_api_mtypes[APPLICATION_COLLECTION_DHCP_SUBNET_JSON],
            midonet_api_mtypes[APPLICATION_DHCP_SUBNET_JSON],
            outnames, outnames_max));
}

/**
 * Searches a matching dhcphost in the given array of dhcphosts.
 * @param dhcphosts [in] pointer to an array of pointers to midoname structures that represent dhcphosts.
 * @param max_dhcphosts [in] number of elements in dhcphosts array.
 * @param name [in] name of the dhcphost (i-xxxxxxxx or eni-xxxxxxxx)
 * @param mac [in] Ethernet MAC address of the dhcphost.
 * @param ip [in] IP address of the dhcphost.
 * @param dns_domain [in] optional DNS domain to be sent with DHCP responses.
 * @param foundidx [out] index to dhcp.dhcphosts array, if found.
 * @return 0 on success, 1 otherwise.
 */
int mido_find_dhcphost_from_list(midoname **dhcphosts, int max_dhcphosts, char *name,
        char *mac, char *ip, char *dns_domain, int *foundidx) {
    int rc = 0, found = 0, ret = 0;
    int i = 0;

    if ((dhcphosts == NULL) || (max_dhcphosts == 0)) {
        return (1);
    }

    found = 0;
    for (i = 0; i < max_dhcphosts && !found; i++) {
        if ((dhcphosts[i] == NULL) || (dhcphosts[i]->init == 0)) {
            continue;
        }
        if (dns_domain) {
            rc = mido_cmp_midoname_to_input(dhcphosts[i], "name", name,
                    "macAddr", mac, "ipAddr", ip, "extraDhcpOpts", "jsonlist",
                    "extraDhcpOpts:optName", "domain_search", "extraDhcpOpts:optValue",
                    dns_domain, "extraDhcpOpts:END", "END", NULL);
        } else {
            rc = mido_cmp_midoname_to_input(dhcphosts[i], "name", name,
                    "macAddr", mac, "ipAddr", ip, NULL);
        }
        if (!rc) {
            found = 1;
            if (foundidx) {
                *foundidx = i;
            }
        }
    }
    if (found) {
        ret = 0;
    } else {
        ret = 1;
    }
    return (ret);    
}

/**
 * Retrieves an array of pointers to midonet object representing bridge dhcp hosts.
 * @param devname [in] bridge of interest.
 * @param dhcp [in] dhcp of the bridge of interest.
 * @param outnames [out] an array of pointers to midonet objects representing bridge dhcp hosts, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_dhcphosts(midoname *devname, midoname *dhcp, midoname ***outnames, int *outnames_max) {
    int rc = 0;
    midoname parents[2];
    bzero(parents, 2 * sizeof (midoname));

    int dhhcount = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_bridge *br = midonet_api_cache_lookup_bridge(devname, NULL);
        if (br == NULL) {
            LOGWARN("MIDOCACHE: %s not found\n", devname->name);
        } else {
            midonet_api_dhcp *dh = midonet_api_cache_lookup_dhcp(br, dhcp, NULL);
            if (dh == NULL) {
                LOGWARN("MIDOCACHE: %s not found in %s\n", dhcp->name, devname->name);
                return (1);
            }
            if (dh->max_dhcphosts > 0) {
                *outnames = EUCA_ZALLOC_C(dh->max_dhcphosts, sizeof (midoname *));
            }
            midoname **names = *outnames;
            for (int i = 0; i < dh->max_dhcphosts; i++) {
                if (dh->dhcphosts[i] == NULL) {
                    continue;
                }
                names[dhhcount] = dh->dhcphosts[i];
                dhhcount++;
            }
            *outnames_max = dhhcount;
        }
        return (0);
    }

    mido_copy_midoname(&(parents[0]), devname);
    mido_copy_midoname(&(parents[1]), dhcp);
    rc = mido_get_resources(parents, 2, devname->tenant, "hosts",
            midonet_api_mtypes[APPLICATION_COLLECTION_DHCP_HOST_JSON],
            midonet_api_mtypes[APPLICATION_DHCP_HOST_JSON],
            outnames, outnames_max);
    mido_free_midoname_list(parents, 2);

    return (rc);
}

/**
 * Retrieves a midonet object that represents the dhcphost in the argument from midocache.
 * @param dhcp [in] the bridge dhcp where the dhcphost of interest will be searched.
 * @param dhcphostname [in] dhcphost of interest.
 * @return pointer to the data structure that represents the dhcphost, when found. NULL otherwise.
 */
midoname *mido_get_dhcphost(midonet_api_dhcp *dhcp, char *dhcphostname) {
    if (!dhcphostname) {
        LOGWARN("Invalid argument: cannot retrieve NULL dhcphost.\n");
        return (NULL);
    }
    if (midocache != NULL) {
        if (dhcp->sorted_dhcphosts == 0) {
            qsort(dhcp->dhcphosts, dhcp->max_dhcphosts, sizeof (midoname *), compare_midoname_name);
            dhcp->sorted_dhcphosts = 1;
        }
        midoname dhn;
        dhn.name = strdup(dhcphostname);
        midoname *pdhn = &dhn;
        midoname **res = (midoname **) bsearch(&pdhn, dhcp->dhcphosts, dhcp->max_dhcphosts,
                sizeof (midoname *), compare_midoname_name);
        EUCA_FREE(dhn.name);
        if (res) {
            return (*res);
        }
    }
    return (NULL);
}

/**
 * Creates a dhcp host in the specified bridge/dhcp in MidoNet.
 * @param bridge [in] bridge (not checked) of interest.
 * @param dhcp [in] dhcp (not checked) of interest.
 * @param name [in] name of the dhcphost (i-xxxxxxxx or eni-xxxxxxxx)
 * @param mac [in] Ethernet MAC address of the dhcphost.
 * @param ip [in] IP address of the dhcphost.
 * @param dns_domain [in] optional DNS domain to be sent with DHCP responses.
 * @param outname [i/o] pointer to an extant MidoNet bridge dhcp host (parameters will be checked
 * to avoid duplicate router creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created dhcp host
 * will not be returned.
 * @return 0 on success, 1 otherwise.
 */
int mido_create_dhcphost(midonet_api_bridge *bridge, midoname *dhcp, char *name, char *mac,
        char *ip, char *dns_domain, midoname **outname) {
    int rc = 0;
    int ret = 0;
    midoname myname;
    int found = 0;
    int foundidx = 0;

    midoname *out = NULL;
    midoname *founddhh = NULL;
    
    if ((bridge == NULL) || (bridge->obj == NULL) || (dhcp == NULL)) {
        LOGWARN("Invalid argument: cannot create dhcphost for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    // get the bridge information from midocache
    midonet_api_dhcp *dh = NULL;
    dh = midonet_api_cache_lookup_dhcp(bridge, dhcp, NULL);
    if (dh == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", dhcp->name);
        return (1);
    }
    if (out && out->init) {
        founddhh = midonet_api_cache_lookup_dhcp_host(dh, out, NULL);
        if (founddhh) {
            found = 1;
            LOGEXTREME("dhcp host already in mido - abort create\n");
            return (0);
        }
    }
    founddhh = midonet_api_cache_lookup_dhcp_host_byparam(dh, name, mac, ip, dns_domain, &foundidx);
    if (founddhh != NULL) {
        found = 1;
        LOGEXTREME("dhcp host already in mido - abort create.\n");
        if (outname) {
            *outname = founddhh;
        }
    }

    if (!found) {
        midoname *parents;
        bzero(&myname, sizeof (midoname));
        myname.name = strdup(name);
        myname.tenant = strdup(bridge->obj->tenant);
        myname.resource_type = strdup("hosts");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_DHCP_HOST_JSON]);

        parents = EUCA_ZALLOC_C(2, sizeof (midoname));

        mido_copy_midoname(&(parents[0]), bridge->obj);
        mido_copy_midoname(&(parents[1]), dhcp);

        if (!found) {
            if (dns_domain) {
                rc = mido_create_resource(parents, 2, &myname, &out, "name",
                        myname.name, "macAddr", mac, "ipAddr", ip, "extraDhcpOpts",
                        "jsonlist", "extraDhcpOpts:optName", "domain_search",
                        "extraDhcpOpts:optValue", dns_domain, "extraDhcpOpts:END", "END", NULL);
            } else {
                rc = mido_create_resource(parents, 2, &myname, &out, "name",
                        myname.name, "macAddr", mac, "ipAddr", ip, NULL);
            }

            if (rc == 0) {
                if (outname) {
                    *outname = out;
                }
                midonet_api_cache_add_dhcp_host(dh, out);
                ret = 0;
            } else if (rc < 0) {
                ret = 0;
            } else {
                ret = 1;
            }
        }

        mido_free_midoname(&(parents[0]));
        mido_free_midoname(&(parents[1]));
        mido_free_midoname(&myname);
        EUCA_FREE(parents);
    }
    return (ret);
}

/**
 * Deletes a dhcp host from MidoNet.
 * @param bridge [in] bridge (not checked) of interest.
 * @param dhcp [in] dhcp (not checked) of interest.
 * @param name [in] dhcphost (no checks performed) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_dhcphost(midoname *bridge, midoname *dhcp, midoname *name) {
    int rc = 0;
    if (!bridge || !bridge->name || !dhcp || !dhcp->uuid || !name || !name->uuid) {
        return (1);
    }
    midonet_api_dhcp *dh = NULL;
    midonet_api_bridge *br = midonet_api_cache_lookup_bridge(bridge, NULL);
    if (br == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", bridge->name);
        return (1);
    } else {
        dh = midonet_api_cache_lookup_dhcp(br, dhcp, NULL);
        if (dh == NULL) {
            LOGWARN("Unable to find %s in midocache.\n", dhcp->name);
            return (1);
        } else {
            midonet_api_cache_del_dhcp_host(dh, name);
            rc = mido_delete_resource(NULL, name);
        }
    }

    return (rc);
}

/**
 * Creates a chain in MidoNet.
 * @param tenant [in] name of the MidoNet tenant.
 * @param name [in] name of the chain to be created.
 * @param outname [i/o] pointer to an extant MidoNet chain (parameters will be checked
 * to avoid duplicate chain creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created chain
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
midonet_api_chain *mido_create_chain(char *tenant, char *name, midoname **outname) {
    int rc;
    midoname myname;
    midoname *out = NULL;
    midonet_api_chain *ch = NULL;
    
    if (outname && *outname) {
        out = *outname;
    }
    if (out && out->init) {
        if (!strcmp(name, out->name)) {
            LOGEXTREME("%s already in mido - abort create\n", name);
            return (midonet_api_cache_lookup_chain(out, NULL));
        }
        out = NULL;
    } else {
        midoname tmp;
        tmp.name = strdup(name);
        ch = midonet_api_cache_lookup_chain(&tmp, NULL);
        EUCA_FREE(tmp.name);
        if (ch) {
            LOGEXTREME("%s already in mido - abort create.\n", name);
            if (outname) {
                *outname = ch->obj;
            }
            return (ch);
        }
    }

    bzero(&myname, sizeof(midoname));
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("chains");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_CHAIN_JSON]);

    ch = NULL;
    rc = mido_create_resource(NULL, 0, &myname, &out, "name", myname.name, NULL);
    if (rc == 0) {
        // cache newly created chain
        ch = midonet_api_cache_add_chain(out);
    }
    if (outname) {
        *outname = out;
    }
    mido_free_midoname(&myname);
    return (ch);
}

/**
 * Updates information about a chain with the parameters in the variable argument section.
 * @param [in] name midoname structure of the chain of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_chain(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Logs the information about a chain represented by name.
 * @param name [in] MN chain of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_chain(midoname *name) {
    return (mido_print_resource("chains", name));
}

/**
 * Deletes the given chain from MidoNet.
 * @param name [in] chain of interest (no check is performed - caller responsible to
 * make sure it is a chain).
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_chain(midoname *name) {
    int rc = 0;
    if (!name || !name->name) {
        return (1);
    }
    midonet_api_cache_del_chain(name);
    rc = mido_delete_resource(NULL, name);
    return (rc);
}

/**
 * Creates a new ip-address-group ip as specified in the argument. 
 *
 * @param ipag [in] midonet_api_ipaddrgroup structure of interest. This parameter
 * has priority over ipaddrgroup. If ipag is NULL, ipaddrgroup is used to search
 * midocache.
 * @param ipaddrgroup [in] ip-address-group of interest.
 * @param ip [in] ip address of interest.
 * @param outname [i/o] pointer to an extant MidoNet ip (parameters will be checked
 * to avoid duplicate ip creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created ip will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the ip is successfully created/found. 1 otherwise.
 */
int mido_create_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipag, midoname *ipaddrgroup, char *ip, midoname **outname) {
    int rc = 0, ret = 0, max_ips = 0, found = 0;
    midoname myname, **ips = NULL;
    midoname *out = NULL;
    midoname *foundip = NULL;

    if (!ipaddrgroup && !ipag) {
        LOGWARN("Invalid argument: cannot create ip in a NULL ipaddrgroup.\n");
        return (1);
    }

    midonet_api_ipaddrgroup *ig = NULL;
    if (ipag != NULL) {
        ig = ipag;
    } else {
        ig = midonet_api_cache_lookup_ipaddrgroup(ipaddrgroup, NULL);
    }
    if (ig == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", ipaddrgroup->name);
        return (1);
    } else {
        ips = ig->ips;
        max_ips = ig->max_ips;
    }
    if (outname && *outname) {
        out = *outname;
    }

    if (out && out->init) {
        rc = mido_find_ipaddrgroup_ip_from_list(outname, 1, ip, &foundip);
        if (foundip) {
            found = 1;
            LOGEXTREME("ip already in mido - abort create\n");
        }
    }
    if (!found) {
        rc = mido_find_ipaddrgroup_ip_from_list(ips, max_ips, ip, &foundip);
        if (foundip) {
            found = 1;
            LOGEXTREME("ip already in mido - abort create.\n");
            if (outname) {
                *outname = foundip;
            }
        }
    }

    if (!found) {
        bzero(&myname, sizeof (midoname));
        myname.tenant = strdup(ig->obj->tenant);
        myname.resource_type = strdup("ip_addrs");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_IP_ADDR_GROUP_ADDR_JSON]);

        LOGTRACE("\tadding %s to %s\n", ip, ig->obj->name);
        rc = mido_create_resource(ig->obj, 1, &myname, &out, "addr", ip, "version", "4", NULL);
        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            midonet_api_cache_add_ipaddrgroup_ip(ig, out, dot2hex(ip));
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }
    LOGTRACE("\t %s %d IPs\n", ig->obj->name, ig->max_ips);
    return (ret);
}

/**
 * Searches a list of ip-address-group ips in the argument for a matching ip.
 *
 * @param ips [in] pointer to a list of midoname structures containing ipaddrgroup ips.
 * @param max_ips [in] number of ipaddrgroup ips in the list.
 * @param ip [in] IP address of interest.
 * @param outip [out] pointer of the matching ip, if found.
 * to be started (but not accessed) before the call.
 *
 * @return 0 if the search is successful. 1 otherwise.
 */
int mido_find_ipaddrgroup_ip_from_list(midoname **ips, int max_ips, char *ip, midoname **outip) {
    int ret = 0, found = 0, i = 0;

    if (!outip) {
        LOGWARN("Invalid argument: outip cannot be NULL\n");
        return (1);
    }
    if (!ips) {
        *outip = NULL;
        return (1);
    }

    found = 0;
    for (i = 0; i < max_ips && !found; i++) {
        if ((ips[i] == NULL) || (ips[i]->init == 0)) {
            continue;
        }
        if (ips[i]->ipagip && ips[i]->ipagip->ip && !strcmp(ips[i]->ipagip->ip, ip)) {
            *outip = ips[i];
            found = 1;
        }
    }
    if (!found) {
        *outip = NULL;
    }

    return (ret);
}

/**
 * Deletes an ip-address-group ip from MidoNet.
 * @param ipaddrgroup [in] ip-address-group (no checks performed) of interest.
 * @param ipaddrgroup_ip [in] ip (no checks performed) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, midoname *ipaddrgroup_ip) {
    int rc = 0;
    if (!ipaddrgroup || !ipaddrgroup->obj || !ipaddrgroup_ip || !ipaddrgroup_ip->name) {
        return (1);
    }
    midonet_api_cache_del_ipaddrgroup_ip(ipaddrgroup, ipaddrgroup_ip);
    rc = mido_delete_resource(ipaddrgroup->obj, ipaddrgroup_ip);

    return (rc);
}

/**
 * Retrieves an array of pointers to midonet object representing ip-address-group ip.
 * @param ipaddrgroup [in] ip-address-groups of interest.
 * @param outnames [out] an array of pointers to midonet objects representing chain rule, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_ipaddrgroup_ips(midoname *ipaddrgroup, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_ipaddrgroup *ig = midonet_api_cache_lookup_ipaddrgroup(ipaddrgroup, NULL);
        if (ig == NULL) {
            LOGWARN("MIDOCACHE: %s not found\n", ipaddrgroup->name);
        } else {
            if (ig->max_ips > 0) {
                *outnames = EUCA_ZALLOC_C(ig->max_ips, sizeof (midoname *));
            }
            midoname **names = *outnames;
            for (int i = 0; i < ig->max_ips; i++) {
                if (ig->ips[i] == NULL) {
                    continue;
                }
                names[count] = ig->ips[i];
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(ipaddrgroup, 1, ipaddrgroup->tenant, "ip_addrs",
            midonet_api_mtypes[APPLICATION_COLLECTION_IP_ADDR_GROUP_ADDR_JSON],
            midonet_api_mtypes[APPLICATION_IP_ADDR_GROUP_ADDR_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves the ip entry in the position pos.
 * @param ipaddrgroup [in] ip-address-group of interest.
 * @param pos [in] position of the IP address of interest (starts at 0).
 * @return pointer to midoname data structure that represents the IP of interest.
 */
midoname *mido_get_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, int pos) {
    int count = 0;
    if (midocache != NULL) {
        if (ipaddrgroup == NULL) {
            LOGWARN("MIDOCACHE: cannot get IP from NULL ip-address-group\n");
        } else {
            for (int i = 0; i < ipaddrgroup->max_ips; i++) {
                if (ipaddrgroup->ips[i] == NULL) {
                    continue;
                } else {
                    if (count == pos) {
                        return (ipaddrgroup->ips[i]);
                    } else {
                        count++;
                    }
                }
            }
        }
    }
    return (NULL);
}

/**
 * Searches a list of chain rules in the argument for a rule matching the fields
 * specified in the variable argument section.
 *
 * @param rules [in] pointer to a list of midoname structures containing chain rules.
 * @param max_rules [in] number of chain rules in the list.
 * @param outrule [out] pointer of the matching rule, if found.
 * @param ... variable argument section
 *
 * @return 0 if the search is successful. 1 otherwise.
 */
int mido_find_rule_from_list(midoname **rules, int max_rules, midoname **outrule, ...) {
    int ret = 0;
    va_list al;
    va_start(al, outrule);
    ret = mido_find_rule_from_list_v(rules, max_rules, outrule, &al);
    va_end(al);
    return (ret);
}

/**
 * Searches a list of chain rules in the argument for a rule matching the fields
 * specified in the variable argument section.
 *
 * @param rules [in] pointer to a list of midoname structures containing chain rules.
 * @param max_rules [in] number of chain rules in the list.
 * @param outrule [out] pointer of the matching rule, if found.
 * @param al variable argument list that specifies a rule. This list is assumed
 * to be started (but not accessed) before the call.
 *
 * @return 0 if the search is successful. 1 otherwise.
 */
int mido_find_rule_from_list_v(midoname **rules, int max_rules, midoname **outrule, va_list *al) {
    int rc = 0, ret = 0, found = 0, i = 0;

    if (!outrule) {
        LOGWARN("Invalid argument: outrule cannot be NULL\n");
        return (1);
    }
    if (!rules) {
        *outrule = NULL;
        return (1);
    }

    found = 0;
    for (i = 0; i < max_rules && rules && !found; i++) {
        if ((rules[i] == NULL) || (rules[i]->init == 0)) {
            continue;
        }
        rc = mido_cmp_midoname_to_input_json_v(rules[i], al);
        if (!rc) {
            *outrule = rules[i];
            found = 1;
        }
    }
    if (!found) {
        *outrule = NULL;
    }

    return (ret);
}

/**
 * Creates a new chain rule as specified in the argument. 
 * @param ch [in] chain of interest. ch has priority over chain. If ch is NULL,
 * midocache lookup is performed based on chain.
 * @param chain [in] chain of interest.
 * @param outname [i/o] pointer to an extant MidoNet rule (parameters will be checked
 * to avoid duplicate rule creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created rule will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @param next_position [out] pointer to an integer, where the next position for
 * a new chain rule would be stored in future rule creations.
 * @param ... variable argument, specifies a new chain rule.
 * @return 0 if the rule is successfully created/found. 1 otherwise.
 */
int mido_create_rule(midonet_api_chain *ch, midoname *chain, midoname **outname, int *next_position, ...) {
    int rc = 0, ret = 0, max_rules = 0, found = 0;
    midoname myname;
    midoname *out = NULL;
    midoname *foundrule = NULL;
    midoname **rules = NULL;
    va_list ap = {{0}};

    if ((ch == NULL) && (chain == NULL)) {
        LOGWARN("Invalid argument: cannot create rule for NULL\n");
        return (1);
    }
    // get the chain information from midocache
    if (ch == NULL) {
        ch = midonet_api_cache_lookup_chain(chain, NULL);
    }
    if (ch == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", chain->name);
        return (1);
    } else {
        rules = ch->rules;
        max_rules = ch->max_rules;
    }
    if (outname && *outname) {
        out = *outname;
    }

    va_start(ap, next_position);

    if (out && out->init) {
        rc = mido_find_rule_from_list_v(outname, 1, &foundrule, &ap);
        if (foundrule) {
            found = 1;
            LOGEXTREME("rule already in mido - abort create\n");
        }
    }
    if (!found) {
        rc = mido_find_rule_from_list_v(rules, max_rules, &foundrule, &ap);
        if (foundrule) {
            found = 1;
            LOGEXTREME("rule already in mido - abort create.\n");
            if (outname) {
                *outname = foundrule;
            }
        }
    }

    if (!found) {
        bzero(&myname, sizeof (midoname));
        myname.tenant = strdup(ch->obj->tenant);
        myname.resource_type = strdup("rules");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_RULE_JSON]);

        rc = mido_create_resource_v(ch->obj, 1, &myname, &out, &ap);
        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            midonet_api_cache_add_chain_rule(ch, out);
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }

    va_end(ap);
    LOGTRACE("%s pos rule create max_rules %d count %d\n", ch->obj->name, ch->max_rules, ch->rules_count);
    if (next_position) *next_position = ch->rules_count + 1;

    return (ret);
}

/**
 * Logs the information about a port represented by name.
 * @param name [in] MN port of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_rule(midoname *name) {
    return (mido_print_resource("ports", name));
}

/**
 * Deletes a chain rule from MidoNet.
 * @param chain [in] chain (no checks performed) of interest.
 * @param name [in] rule (no checks performed) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_rule(midonet_api_chain *chain, midoname *name) {
    int rc = 0;
    if (!chain || !chain->max_rules || !name || !name->uri) {
        return (1);
    }
    midonet_api_cache_del_chain_rule(chain, name);
    rc = mido_delete_resource(NULL, name);

    return (rc);
}

/**
 * Creates a port in MidoNet.
 * @param devname [in] midoname structure representing a router or bridge.
 * @param port_type [in] type of the port to be created. (Bridge||Router)
 * @param ip [in] for a router port, the IP address to be assigned to the port.
 * @param nw [in] for a router port, the network address to be assigned to the port.
 * @param slashnet [in] for a router port, the network prefix length to be assigned to the port.
 * @param mac [in] for a router port, the optional Ethernet MAC address to be assigned to the port.
 * @param outname [i/o] pointer to an extant MidoNet port (parameters will be checked
 * to avoid duplicate bridge creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created port
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
int mido_create_port(midoname *devname, char *port_type, char *ip, char *nw,
        char *slashnet, char *mac, midoname **outname) {
    int rc;
    midoname myname;

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(devname->tenant);
    myname.resource_type = strdup("ports");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_PORT_JSON]);

    if (ip && nw && slashnet) {
        if (mac) {
            rc = mido_create_resource(devname, 1, &myname, outname, "type", port_type,
                    "portAddress", ip, "networkAddress", nw, "networkLength", slashnet,
                    "portMac", mac, NULL);
        } else {
            rc = mido_create_resource(devname, 1, &myname, outname, "type", port_type,
                    "portAddress", ip, "networkAddress", nw, "networkLength", slashnet, NULL);
        }
    } else {
        rc = mido_create_resource(devname, 1, &myname, outname, "type", port_type, NULL);
    }

    mido_free_midoname(&myname);
    return (rc);
}

/**
 * Creates a bridge port in MidoNet.
 * @param br [in] bridge of interest. br has priority over devname. If br is NULL,
 * midocache lookup is performed based on devname.
 * @param devname [in] midoname structure representing a bridge.
 * @param outname [i/o] pointer to an extant MidoNet port (parameters will be checked
 * to avoid duplicate port creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created port
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
int mido_create_bridge_port(midonet_api_bridge *br, midoname *devname, midoname **outname) {
    midoname *out = NULL;
    midoname **ports = NULL;
    int max_ports = 0;
    int rc = 0;
    int ret = 0;

    if ((br == NULL) && (devname == NULL)) {
        LOGWARN("Invalid argument: cannot create bridge port for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    // get the bridge information from midocache
    if (br == NULL) {
        br = midonet_api_cache_lookup_bridge(devname, NULL);
    }
    if (br == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", devname->name);
        return (1);
    } else {
        ports = br->ports;
        max_ports = br->max_ports;
    }
    if (out && out->init) {
        for (int i = 0; i < max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (ports[i] == out) {
                LOGEXTREME("port %s already in mido - abort create\n", out->name);
                return (0);
            }
            if (!strcmp(ports[i]->uuid, out->uuid)) {
                LOGEXTREME("port %s already in mido - abort create.\n", out->name);
                return (0);
            }
        }
        out = NULL;
    }
    rc = mido_create_port(br->obj, "Bridge", NULL, NULL, NULL, NULL, &out);
    if (rc == 0) {
        if (outname) {
            *outname = out;
        }
        midonet_api_cache_add_bridge_port(br, out);
        ret = 0;
    } else {
        ret = 1;
    }
    return (ret);
}

/**
 * Creates a router port in MidoNet.
 * @param rt [in] router of interest. rt has priority over devname. If
 * rt is NULL, midocache lookup is performed based on devname.
 * @param devname [in] midoname structure representing a router.
 * @param ip [in] for a router port, the IP address to be assigned to the port.
 * @param nw [in] for a router port, the network address to be assigned to the port.
 * @param slashnet [in] for a router port, the network prefix length to be assigned to the port.
 * @param mac [in] for a router port, the optional Ethernet MAC address to be assigned to the port.
 * @param outname [i/o] pointer to an extant MidoNet port (parameters will be checked
 * to avoid duplicate router port creation. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created port
 * will not be returned.
 * @return 0 on success. 1 otherwise.
 */
int mido_create_router_port(midonet_api_router *rt, midoname *devname, char *ip, char *nw, char *slashnet, char *mac, midoname **outname) {
    midoname *out = NULL;
    midoname **ports = NULL;
    int max_ports = 0;
    int found = 0;
    int foundidx = 0;
    int rc = 0;
    int ret = 0;

    if ((rt == NULL) && (devname == NULL)) {
        LOGWARN("Invalid argument: cannot create router port for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    // get the router information from midocache
    if (rt == NULL) {
        rt = midonet_api_cache_lookup_router(devname, NULL);
    }
    if (rt == NULL) {
        LOGWARN("Unable to find %s in midocache.\n", devname->name);
        return (1);
    } else {
        ports = rt->ports;
        max_ports = rt->max_ports;
    }
    if (out && out->init) {
        for (int i = 0; i < max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (ports[i] == out) {
                LOGEXTREME("port %s already in mido - abort create\n", out->name);
                return (0);
            }
            if (!strcmp(ports[i]->uuid, out->uuid)) {
                LOGEXTREME("port %s already in mido - abort create.\n", out->name);
                return (0);
            }
        }
        out = NULL;
    }
    rc = mido_find_port_from_list(ports, max_ports, ip, nw, slashnet, mac, &foundidx);
    if (rc == 0) {
        found = 1;
        LOGEXTREME("port already in mido - abort create.\n");
        if (outname) {
            *outname = ports[foundidx];
        }
    }

    if (!found) {
        rc = mido_create_port(rt->obj, "Router", ip, nw, slashnet, mac, &out);
        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            midonet_api_cache_add_router_port(rt, out);
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
    }
    return (ret);
}

/**
 * Searches a port specified in the arguments from a list (also specified in the arguments). 
 * @param ports [in] pointer to an array of pointers to midoname structures representing ports.
 * @param max_ports [in] number of ports in the array.
 * @param ip [in] for a router port, the IP address of the port.
 * @param nw [in] for a router port, the network address of the port.
 * @param slashnet [in] for a router port, the network prefix length of the port.
 * @param mac [in] for a router port, the optional Ethernet MAC address of the port.
 * @param foundidx [out] index to the array of the searching port, if found.
 * @return 0 if the port is found. 1 otherwise.
 */
int mido_find_port_from_list(midoname **ports, int max_ports, char *ip, char *nw, char *slashnet, char *mac, int *foundidx) {
    int rc = 0, found = 0, ret = 0;
    int i = 0;
    char *sip;
    char *snw;
    char *ssn;
    char *smac;
    
    if ((ports == NULL) || (max_ports == 0)) {
        return (1);
    }

    sip = (ip == NULL) ? strdup("UNSET") : strdup(ip);
    snw = (nw == NULL) ? strdup("UNSET") : strdup(nw);
    ssn = (slashnet == NULL) ? strdup("UNSET") : strdup(slashnet);
    smac = (mac == NULL) ? strdup("UNSET") : strdup(mac);

    found = 0;
    for (i = 0; i < max_ports && !found; i++) {
        if ((ports[i] == NULL) || (ports[i]->init == 0)) {
            continue;
        }
        rc = 1;
        if (ports[i]->port) {
            int ipok = 0;
            int nwok = 0;
            int snok = 0;
            int macok = 0;
            if ((!ports[i]->port->portaddr && !ip) ||
                    (ports[i]->port->portaddr && !strcmp(sip, ports[i]->port->portaddr))) {
                ipok = 1;
            }
            if (!mac ||
                    (ports[i]->port->portmac && !strcmp(smac, ports[i]->port->portmac))) {
                macok = 1;
            }
            if ((!ports[i]->port->netaddr && !nw) ||
                    (ports[i]->port->netaddr && !strcmp(snw, ports[i]->port->netaddr))) {
                nwok = 1;
            }
            if ((!ports[i]->port->netlen && !slashnet) ||
                    (ports[i]->port->netlen && !strcmp(ssn, ports[i]->port->netlen))) {
                snok = 1;
            }
            if (ipok && macok && nwok && snok) {
                rc = 0;
            }
        }
        if (rc == 0) {
            found = 1;
            if (foundidx) {
                *foundidx = i;
            }
        }
    }
    EUCA_FREE(sip);
    EUCA_FREE(snw);
    EUCA_FREE(ssn);
    EUCA_FREE(smac);
    if (found) {
        ret = 0;
    } else {
        ret = 1;
    }
    return (ret);
}

/**
 * Updates information about a port with the parameters in the variable argument section.
 * @param [in] name midoname structure of a port of interest (checks are not performed)
 * @param [in] ... variable argument section (key-value pairs)
 * @return  0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_port(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;
    va_start(al, name);
    ret = mido_update_resource(name, &al);
    va_end(al);

    return (ret);
}

/**
 * Logs the information about a port represented by name.
 * @param name [in] MN port of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_port(midoname *name) {
    return (mido_print_resource("ports", name));
}

/**
 * Deletes the given bridge port from MidoNet.
 * @param bridge [in] bridge of interest (no check is performed - caller responsible to
 * make sure it is a bridge).
 * @param port [in] bridge port of interest (no check is performed - caller responsible
 * to make sure that the port belongs to the bridge.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_bridge_port(midonet_api_bridge *bridge, midoname *port) {
    if (!bridge || !bridge->obj || !port || !port->uuid) {
        return (1);
    }
    midonet_api_cache_del_bridge_port(bridge, port);
    return (mido_delete_resource(NULL, port));
}

/**
 * Deletes the given router port from MidoNet.
 * @param router [in] router of interest (no check is performed - caller responsible to
 * make sure it is a router).
 * @param port [in] router port of interest (no check is performed - caller responsible
 * to make sure that the port belongs to the router.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_router_port(midonet_api_router *router, midoname *port) {
    if (!router || !router->obj || !port || !port->uuid) {
        return (1);
    }
    midonet_api_cache_del_router_port(router, port);
    return (mido_delete_resource(NULL, port));
}

/**
 * Retrieves an array of pointers to midonet object representing bridge and router ports.
 * @param devname [in] optional device (router or bridge)
 * @param outnames [out] an array of pointers to midonet objects representing ports, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_ports(midoname *devname, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        midoname **ports = midocache->ports;
        int max_ports = midocache->max_ports;
        *outnames = NULL;
        *outnames_max = 0;

        if (devname) {
            if (!strcmp(devname->resource_type, "bridges")) {
                midonet_api_bridge *br = mido_get_bridge(devname->name);
                if (br) {
                    *outnames = EUCA_ZALLOC_C(br->max_ports, sizeof (midoname *));
                    midoname **names = *outnames;
                    for (int i = 0; i < br->max_ports; i++) {
                        if (br->ports[i] == NULL) {
                            continue;
                        }
                        names[count] = br->ports[i];
                        count++;
                    }
                    *outnames_max = count;
                }
            }
            if (!strcmp(devname->resource_type, "routers")) {
                midonet_api_router *rt = mido_get_router(devname->name);
                if (rt) {
                    *outnames = EUCA_ZALLOC_C(rt->max_ports, sizeof (midoname *));
                    midoname **names = *outnames;
                    for (int i = 0; i < rt->max_ports; i++) {
                        if (rt->ports[i] == NULL) {
                            continue;
                        }
                        names[count] = rt->ports[i];
                        count++;
                    }
                    *outnames_max = count;
                }
            }
            //return(mido_get_device_ports(ports, max_ports, devname, outnames, outnames_max));
            return (0);
        }

        if ((ports != NULL) && (max_ports > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_ports, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_ports; i++) {
                if (ports[i] == NULL) {
                    continue;
                }
                names[count] = ports[i];
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }

    if (devname) {
        return (mido_get_resources(devname, 1, devname->tenant, "ports",
                midonet_api_mtypes[APPLICATION_COLLECTION_PORT_JSON],
                midonet_api_mtypes[APPLICATION_PORT_JSON],
                outnames, outnames_max));
    } else {
        return (mido_get_resources(NULL, 0, VPCMIDO_TENANT, "ports",
                midonet_api_mtypes[APPLICATION_COLLECTION_PORT_JSON],
                midonet_api_mtypes[APPLICATION_PORT_JSON],
                outnames, outnames_max));
    }
}

/**
 * Retrieves a port from MidoNet-API to refresh the in-memory data.
 * @param port [in] port of interest.
 * @return 0 on success. Positive number otherwise.
 */
int mido_refresh_port(midoname *port) {
    return (mido_refresh_resource(port));
}

/**
 * Searches the given list of MidoNet ports for ports that belongs to the device
 * specified in the argument. (Deprecated 2016-05 - use mido_get_ports)
 * @param ports [in] an array of pointers to MidoNet ports.
 * @param max_ports [in] number of ports in the array.
 * @param device [in] device of interest.
 * @param outports [out] pointer to an array of midoname data structure references
 * of the ports that belong to the given device. Memory is allocated.
 * Caller should release once done.
 * @param outports_max [out] number of ports that belong to the device of interest.
 * @return 0 if port(s) that belong(s) to the given device is/are found. 1 otherwise.
 */
int mido_get_device_ports(midoname **ports, int max_ports, midoname *device, midoname ***outports, int *outports_max) {
    int i;
    int rc;
    char *devuuid = NULL;
    midoname *port = NULL;
    midoname **retports = NULL;

    if (!ports || !max_ports || !outports || !outports_max || !device) {
        return (1);
    }
    if (device->init == 0) {
        return (1);
    }
    *outports_max = 0;
    for (i = 0; i < max_ports; i++) {
        port = ports[i];
        if (port->init == 0) {
            continue;
        }
        rc = mido_getel_midoname(port, "deviceId", &devuuid);
        if ((rc == 0) && (!strcmp(devuuid, device->uuid))) {
            retports = EUCA_REALLOC_C(retports, *outports_max + 1, sizeof (midoname *));
            retports[*outports_max] = port;
            (*outports_max)++;
        }
        EUCA_FREE(devuuid);
        devuuid = NULL;
    }
    *outports = retports;
    return (0);
}

/**
 * Searches the given list of MidoNet ports for ports that belongs to the host
 * specified in the argument.
 * @param ports [in] an array of pointers to MidoNet ports.
 * @param max_ports [in] number of ports in the array.
 * @param host [in] host of interest.
 * @param outports [out] pointer to an array of midoname data structure references
 * of the ports that belong to the given device. Memory is allocated.
 * Caller should release once done.
 * @param outports_max [out] number of ports that belong to the device of interest.
 * @return 0 if port(s) that belong(s) to the given device is/are found. 1 otherwise.
 */
int mido_get_host_ports(midoname **ports, int max_ports, midoname *host, midoname ***outports, int *outports_max) {
    int i;
    int rc;
    char *hostuuid = NULL;
    midoname *port = NULL;
    midoname **retports = NULL;

    if (!ports || !max_ports || !outports || !outports_max || !host) {
        return (1);
    }
    if (host->init == 0) {
        return (1);
    }
    *outports_max = 0;
    for (i = 0; i < max_ports; i++) {
        port = ports[i];
        if (port->init == 0) {
            continue;
        }
        rc = mido_getel_midoname(port, "hostId", &hostuuid);
        if ((rc == 0) && (!strcmp(hostuuid, host->uuid))) {
            retports = EUCA_REALLOC_C(retports, *outports_max + 1, sizeof (midoname *));
            retports[*outports_max] = port;
            (*outports_max)++;
        }
        EUCA_FREE(hostuuid);
        hostuuid = NULL;
    }
    *outports = retports;
    return (0);
}

/**
 * Retrieves an array of pointers to midonet object representing chain rule.
 * @param chainname [in] chain of interest.
 * @param outnames [out] an array of pointers to midonet objects representing chain rule, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_rules(midoname *chainname, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_chain *ch = midonet_api_cache_lookup_chain(chainname, NULL);
        if (ch == NULL) {
            LOGWARN("MIDOCACHE: %s not found\n", chainname->name);
        } else {
            if (ch->max_rules > 0) {
                *outnames = EUCA_ZALLOC_C(ch->max_rules, sizeof (midoname *));
            }
            midoname **names = *outnames;
            for (int i = 0; i < ch->max_rules; i++) {
                if (ch->rules[i] == NULL) {
                    continue;
                }
                names[count] = ch->rules[i];
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(chainname, 1, chainname->tenant, "rules",
            midonet_api_mtypes[APPLICATION_COLLECTION_RULE_JSON],
            midonet_api_mtypes[APPLICATION_RULE_JSON],
            outnames, outnames_max));
}

/**
 * Reloads chain rules from mido.
 * @param chain [in] chain of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_reload_rules(midonet_api_chain *chain) {
    if ((!chain) || (!chain->obj) || (!chain->obj->tenant)) {
        LOGERROR("Invalid argument: cannot retrieve NULL chains/rules\n");
        return (1);
    }
    if (chain->rules) {
        EUCA_FREE(chain->rules);
    }
    midoname ***outnames = &(chain->rules);
    int *outnames_max = &(chain->max_rules);
    chain->rules_count = 0;
    int rc = mido_get_resources(chain->obj, 1, chain->obj->tenant, "rules",
            midonet_api_mtypes[APPLICATION_COLLECTION_RULE_JSON],
            midonet_api_mtypes[APPLICATION_RULE_JSON],
            outnames, outnames_max);
    if (rc) {
        LOGWARN("Failed to reload %s rules\n", chain->obj->name);
        return (1);
    }
    chain->rules_count = chain->max_rules;
    return (0);
}

/**
 * Retrieves an array of pointers to midonet object representing chain rule that implement jumps.
 * @param chain [in] chain of interest.
 * @param outnames [out] an array of pointers to midonet objects representing chain rule, to be returned.
 * @param outnames_max [out] number of elements in the outnames array.
 * @param jumptargets [out] an array of pointers to strings of target UUIDs of chain rules.
 * @param max_jumptargets [out] number of elements in jumptargets array.
 * @return 0 on success. 1 otherwise.
 */
int mido_get_jump_rules(midonet_api_chain *chain, midoname ***outnames, int *outnames_max,
        char ***jumptargets, int *jumptargets_max) {
    int count = 0;
    midoname **names = NULL;
    char **jpts = NULL;
    if (midocache != NULL) {
        if (outnames) *outnames = NULL;
        if (outnames_max) *outnames_max = 0;
        if (jumptargets) *jumptargets = NULL;
        if (jumptargets_max) *jumptargets_max = 0;
        if (chain == NULL) {
            LOGWARN("MIDOCACHE: cannot search jump rules for NULL\n");
            return (1);
        } else {
            mido_reload_rules(chain);
            if (chain->max_rules > 0) {
                if (outnames) {
                    *outnames = EUCA_ZALLOC_C(chain->max_rules, sizeof (midoname *));
                    names = *outnames;
                }
                if (jumptargets) {
                    *jumptargets = EUCA_ZALLOC_C(chain->max_rules, sizeof (char *));
                    jpts = *jumptargets;
                }
            }
            for (int i = 0; i < chain->max_rules; i++) {
                if (chain->rules[i] == NULL) {
                    continue;
                }
                char *type = NULL;
                if (chain->rules[i]->rule && chain->rules[i]->rule->type) {
                    type = chain->rules[i]->rule->type;
                }
                //mido_getel_midoname(chain->rules[i], "type", &type);
                if (type && !strcmp(type, "jump")) {
                    if (names) names[count] = chain->rules[i];
                    if (jpts) jpts[count] = chain->rules[i]->rule->jumpchainid;
                    //if (jpts) mido_getel_midoname(chain->rules[i], "jumpChainId", &(jpts[count]));
                    count++;
                }
                //EUCA_FREE(type);
            }
            if (outnames_max) *outnames_max = count;
            if (jumptargets_max) *jumptargets_max = count;
        }
        return (0);
    }
    return (1);
}

/**
 * Deletes all rules of a chain from MidoNet.
 * @param chain [in] chain of interest.
 * @return 0 on success. Positive number otherwise.
 */
int mido_clear_rules(midonet_api_chain *chain) {
    int rc = 0;
    if (!midocache || !chain || !chain->obj) {
        return (1);
    }
    for (int i = 0; i < chain->max_rules; i++) {
        if (chain->rules[i] == NULL) {
            continue;
        }
        rc += mido_delete_resource(NULL, chain->rules[i]);
        chain->rules[i] = NULL;
        (chain->rules_count)--;
    }
    if (chain->rules_count != 0) {
        LOGWARN("Inconsistent rule count (%d after clear) in %s.\n", chain->rules_count, chain->obj->name);
    }
    EUCA_FREE(chain->rules);
    chain->rules = NULL;
    chain->max_rules = 0;
    chain->rules_count = 0;
    return (0);
}

/**
 * Removes the host interface - port binding from the port in the argument.
 * @param host [in] MidoNet host of interest.
 * @param port [in] Midonet bridge or router port of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_unlink_host_port(midoname *host, midoname *port) {
    int rc = 0, ret = 0;
    char url[EUCA_MAX_PATH];
    
    if (!host || !port) {
        LOGWARN("Invalid argument: NULL host or port - cannot unlink host-port.\n");
        return (1);
    }

    snprintf(url, EUCA_MAX_PATH, "%s/hosts/%s/ports/%s", midonet_api_uribase, host->uuid, port->uuid);
    rc = midonet_http_delete(url);
    if (rc) {
        ret = 1;
        LOGERROR("Failed to unlink %s %s\n", host->name, port->name);
    } else {
        rc += mido_refresh_port(port);
        if (rc) {
            LOGWARN("failed to refresh linked ports data.\n");
        }
    }
    return (ret);
}

/**
 * Link MidoNet host port to a MidoNet bridge.
 * @param host [in] MidoNet host (no checks) of interest.
 * @param interface [in] name of the interface of interest.
 * @param device [in] MidoNet bridge of interest.
 * @param port [in] MidoNet bridge port of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_link_host_port(midoname *host, char *interface, midoname *device, midoname *port) {

    int rc = 0, ret = 0, found = 0;
    midoname myname;
    char *hinterface = NULL;

    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(device->tenant);
    myname.name = strdup("port");
    myname.resource_type = strdup("ports");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_HOST_INTERFACE_PORT]);

    // check to see if the port is already mapped
    rc = mido_getel_midoname(port, "hostInterfacePort", &hinterface);
    if (!rc) {
        LOGTRACE("Port %s already mapped to interface %s\n", port->name, hinterface);
        found = 1;
    }
    EUCA_FREE(hinterface);

    if (!found) {
        rc = mido_create_resource(host, 1, &myname, NULL, "deviceId", device->uuid,
                "portId", port->uuid, "hostId", host->uuid, "interfaceName", interface, NULL);
        if (rc) {
            LOGWARN("Failed to link port %s to device %s\n", interface, device->name);
            ret = 1;
        } else {
            rc += mido_refresh_port(port);
            if (rc) {
                LOGWARN("failed to refresh bridge port linked to host.\n");
            }
        }
    }

    mido_free_midoname(&myname);

    return (ret);
}

/**
 * Link MidoNet ports a and b.
 * @param a [in] bridge or router port of interest.
 * @param b [in] bridge or router port of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_link_ports(midoname *a, midoname *b) {
    int rc = 0, found = 0, ret = 0;
    midoname myname;
    //char *asideval = NULL, *bsideval = NULL;
    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(a->tenant);
    myname.name = strdup("link");
    myname.resource_type = strdup("link");
    myname.media_type = strdup(midonet_api_mtypes[APPLICATION_PORT_LINK]);

    // check to see if link already exists before making new link
    if (a->port && b->port && a->port->peerid && b->port->peerid) {
        if (!strcmp(a->port->peerid, b->uuid) && !strcmp(b->port->peerid, a->uuid)) {
            LOGTRACE("ALREADY EXISTS: link from port %s to port %s\n", SP(a->uuid), SP(b->uuid));
            found = 1;
        }
    }
    if (!found) {
        rc = mido_create_resource(a, 1, &myname, NULL, "peerId", b->uuid, NULL);
        if (rc) {
            ret = 1;
        } else {
            rc += mido_refresh_port(a);
            rc += mido_refresh_port(b);
            if (rc) {
                LOGWARN("failed to refresh linked ports data.\n");
            }
        }
    }

    mido_free_midoname(&myname);

    return (ret);
}

/**
 * Update a MidoNet resource with the parameters in the list of variable arguments.
 * @param name [in] midoname structure with the MidoNet resource of interest.
 * @param al [in] list of variable arguments.
 * @return 0 on success. -1 if update is not needed (all parameters are already in
 * place). 1 on failure.
 */
int mido_update_resource(midoname *name, va_list *al) {
    int rc = 0, ret = 0;
    char *payload=NULL;
    va_list ala = { {0} }, alb = { {0} };

    // check to see if resource needs updating
    va_copy(alb, *al);
    rc = mido_cmp_midoname_to_input_json_v(name, &alb);
    LOGTRACE("update_resource cmp return: %d\n", rc);
    va_end(alb);

    if (!rc) {
        LOGTRACE("resource to update matches in place resource - skipping update\n");
        return(-1);
    }

    va_copy(ala, *al);
    payload = mido_jsonize(name->tenant, &ala);
    va_end(ala);
       
    if (payload) {
        rc = midonet_http_put(name->uri, name->media_type, payload);
        if (rc) {
            ret = 1;
        }
        EUCA_FREE(payload);
    }
    if (!ret) {
        rc = midonet_http_get(name->uri, name->media_type, &payload);
        if (rc) {
            LOGWARN("Failed to retrieve new resource from %s\n", name->uri);
            ret = 1;
        } else {
            EUCA_FREE(name->jsonbuf);
            name->jsonbuf = strdup(payload);
            ret = mido_update_midoname(name);
        }
        EUCA_FREE(payload);
    }
    return(ret);
}

/**
 * Logs the information about a resource_type represented by name.
 * @param resource_type [in] resource type as described in MN
 * @param name [in] MN bridge of interest
 * @return 0 on success. 1 on failure.
 */
int mido_print_resource(char *resource_type, midoname *name) {
    int ret = 0;
    struct json_object *jobj = NULL;

    if (!name->jsonbuf) {
        return (1);
    }
    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        LOGERROR("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    } else {
        LOGTRACE("TYPE: %s NAME: %s UUID: %s\n", resource_type, SP(name->name), name->uuid);
        json_object_object_foreach(jobj, key, val) {
            LOGTRACE("\t%s: %s\n", key, SP(json_object_get_string(val)));
        }
        json_object_put(jobj);
    }

    return (ret);
}

/**
 * Wrapper to the mido_jsonize() function.
 * @param tenant name of the MidoNet tenant.
 * @param ... list of strings to be jsonized.
 * @return json representation of the input arguments.
 */
char *mido_get_json(char *tenant, ...) {
    char *ret = NULL;
    va_list al;
    va_start(al, tenant);
    ret = mido_jsonize(tenant, &al);
    va_end(al);
    return (ret);
}

/**
 * Converts a null terminated variable list of key-value pairs to a valid JSON string
 * @param tenant [in] optional tenant ID to be included in the generated JSON string
 * @param al [in] variable argument section
 * @return the input key-value pairs JSONized into a string
 */
char *mido_jsonize(char *tenant, va_list *al) {
    char *payload = NULL;
    struct json_object *jobj = NULL, *jobj_sublist = NULL, *jarr_sublist = NULL;
    char *key = NULL, *val = NULL, *listobjtag = NULL, *listarrtag = NULL, *listjsontag = NULL;
    int listobjtag_count = 0, listarrtag_count = 0, listjsontag_count = 0;

    jobj = json_object_new_object();
    if (!jobj) {
        LOGERROR("ERROR: json_object_new_object(...): returned NULL\n");
        payload = NULL;
    } else {
        if (tenant) {
            json_object_object_add(jobj, "tenantId", json_object_new_string(tenant));
        }
        key = va_arg(*al, char *);
        if (key)
            val = va_arg(*al, char *);
        while (key && val) {
            //            LOGTRACE("HERE: %s/%s\n", key, val);
            if (!strcmp(val, "UNSET")) {
            } else {
                if (!strcmp(val, "jsonlist")) {
                    EUCA_FREE(listobjtag);
                    EUCA_FREE(listarrtag);
                    EUCA_FREE(listjsontag);
                    listobjtag = strdup(key);
                    listobjtag_count = 0;
                    jobj_sublist = json_object_new_object();
                } else if (!strcmp(val, "jsonjson")) {
                    EUCA_FREE(listobjtag);
                    EUCA_FREE(listarrtag);
                    EUCA_FREE(listjsontag);
                    listjsontag = strdup(key);
                    listjsontag_count = 0;
                    jobj_sublist = json_object_new_object();
                } else if (!strcmp(val, "jsonarr")) {
                    EUCA_FREE(listobjtag);
                    EUCA_FREE(listarrtag);
                    EUCA_FREE(listjsontag);
                    listarrtag = strdup(key);
                    listarrtag_count = 0;
                    jobj_sublist = json_object_new_array();
                } else if ((listobjtag && strstr(key, listobjtag)) || (listjsontag && strstr(key, listjsontag))) {
                    char *subkey = NULL;
                    subkey = strchr(key, ':');
                    subkey++;
                    if (!strcmp(val, "END")) {
                        // add the thing
                        if (listjsontag) {
                            if (listjsontag_count) {
                                json_object_object_add(jobj, listjsontag, jobj_sublist);
                            }
                            EUCA_FREE(listjsontag);
                            listjsontag = NULL;
                        } else if (listobjtag) {
                            if (listobjtag_count) {
                                jarr_sublist = json_object_new_array();
                                json_object_array_add(jarr_sublist, jobj_sublist);
                                json_object_object_add(jobj, listobjtag, jarr_sublist);
                            }
                            EUCA_FREE(listobjtag);
                            listobjtag = NULL;
                        }
                    } else {
                        listjsontag_count++;
                        listobjtag_count++;
                        json_object_object_add(jobj_sublist, subkey, json_object_new_string(val));
                    }
                } else if (listarrtag && strstr(key, listarrtag)) {
                    char *subkey = NULL;
                    subkey = strchr(key, ':');
                    subkey++;
                    if (!strcmp(val, "END")) {
                        if (listarrtag_count) {
                            json_object_object_add(jobj, listarrtag, jobj_sublist);
                        }
                        EUCA_FREE(listarrtag);
                        listarrtag = NULL;
                    } else if (!strcmp(subkey, "LIST")) {
                        char **slist = NULL;
                        int max_slist = 0;
                        iplist_split(val, &slist, &max_slist);
                        for (int i = 0; i < max_slist; i++) {
                            listarrtag_count++;
                            json_object_array_add(jobj_sublist, json_object_new_string(slist[i]));
                        }
                        iplist_arr_free(slist, max_slist);
                    } else {
                        listarrtag_count++;
                        json_object_array_add(jobj_sublist, json_object_new_string(val));
                    }
                } else {
                    if (listobjtag) {
                        jarr_sublist = json_object_new_array();
                        json_object_array_add(jarr_sublist, jobj_sublist);
                        json_object_object_add(jobj, listobjtag, jarr_sublist);
                        EUCA_FREE(listobjtag);
                        listobjtag = NULL;
                    } else if (listjsontag) {
                        json_object_object_add(jobj, listjsontag, jobj_sublist);
                        EUCA_FREE(listjsontag);
                        listjsontag = NULL;
                    } else if (listarrtag) {
                        json_object_object_add(jobj, listarrtag, jobj_sublist);
                        EUCA_FREE(listarrtag);
                        listarrtag = NULL;
                    }
                    json_object_object_add(jobj, key, json_object_new_string(val));
                }
            }
            key = va_arg(*al, char *);
            if (key)
                val = va_arg(*al, char *);
        }
        if (listobjtag) {
            jarr_sublist = json_object_new_array();
            json_object_array_add(jarr_sublist, jobj_sublist);
            json_object_object_add(jobj, listobjtag, jarr_sublist);
            EUCA_FREE(listobjtag);
            listobjtag = NULL;
        } else if (listjsontag) {
            json_object_object_add(jobj, listjsontag, jobj_sublist);
            EUCA_FREE(listjsontag);
            listjsontag = NULL;
        } else if (listarrtag) {
            json_object_object_add(jobj, listarrtag, jobj_sublist);
            EUCA_FREE(listarrtag);
            listarrtag = NULL;
        }
        //        printf("JSON: %s\n", json_object_to_json_string(jobj));
        payload = strdup(json_object_to_json_string(jobj));
        //        LOGTRACE("PAYLOAD: %s\n", payload);
        json_object_put(jobj);
    }
    return (payload);
}

/**
 * Creates a new object in MidoNet.
 * @param parents [in] MidoNet parent objects (e.g., router for routes/ports, chain for rules, etc)
 * @param max_parents [in] Number of parents.
 * @param newname [in] pointer to midoname structure describing the object to be created.
 * @param outname [i/o] pointer to a MidoNet object. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created object
 * will not be returned. Check for duplicates must be performed prior to this call
 * in order to avoid duplicates.
 * @param ... variable parameters.
 * @return 0 on success (resource created). Negative number if extant mido object
 * was updated (new object was not created). Positive number on any failure.
 */
int mido_create_resource(midoname *parents, int max_parents, midoname *newname, midoname **outname, ...) {
    int ret = 0;
    va_list al;
    va_start(al, outname);
    ret = mido_create_resource_v(parents, max_parents, newname, outname, &al);
    va_end(al);
    return (ret);
}

/**
 * Creates a new object in MidoNet.
 * @param parents [in] MidoNet parent objects (e.g., router for routes/ports, chain for rules, etc)
 * @param max_parents [in] Number of parents.
 * @param newname [in] pointer to midoname structure describing the object to be created.
 * @param outname [i/o] pointer to a MidoNet object. If outname points to NULL, a newly allocated
 * midoname structure will be returned. If outname is NULL, the newly created object
 * will not be returned. Check for duplicates must be performed prior to this call
 * in order to avoid duplicates.
 * @param al [in] list of variable arguments.
 * @return 0 on success (resource created). Negative number if extant mido object
 * was updated (new object was not created). Positive number on any failure.
 */
int mido_create_resource_v(midoname *parents, int max_parents, midoname *newname, midoname **outname, va_list * al) {
    int ret = 0, rc = 0;
    char url[EUCA_MAX_PATH];
    char *outloc = NULL, *outhttp = NULL, *payload = NULL;
    char tmpbuf[EUCA_MAX_PATH];
    int i;

    midoname *outmn = NULL;
    
    if (outname) {
        outmn = *outname;
        if (outmn) {
            if (outmn->init) {
                if (outmn->jsonbuf) {
                    rc = mido_cmp_midoname_to_input_json_v(outmn, al);
                    if (rc) {
                        LOGINFO("\t create_resource_v() applying changes to %s/%s\n", outmn->name, outmn->jsonbuf);
                        mido_update_resource(outmn, al);
                        ret = -2;
                    } else {
                        LOGINFO("\t create_resource_v() object already in mido %s\n", outmn->name);
                        ret = -1;
                    }
                } else {
                    LOGERROR("Unable to check for duplicates: %s abort creation.\n", outmn->name);
                    ret = 1;
                }
                return (ret);
            } else {
                bzero(outmn, sizeof (midoname));
            }
        } else {
            outmn = midoname_list_get_midoname(midocache_midos);
        }
        *outname = outmn;
    } else {
        LOGEXTREME("\t New mido object will not be returned.\n");
    }

    //  construct the payload
    payload = mido_jsonize(newname->tenant, al);

    if (payload) {
        if (!parents) {
            snprintf(url, EUCA_MAX_PATH, "%s/%s", midonet_api_uribase, newname->resource_type);
        } else {
            snprintf(url, EUCA_MAX_PATH, "%s/", midonet_api_uribase);
            for (i = 0; i < max_parents; i++) {
                tmpbuf[0] = '\0';
                snprintf(tmpbuf, EUCA_MAX_PATH, "%s/%s/", parents[i].resource_type, parents[i].uuid);
                strcat(url, tmpbuf);
            }
            tmpbuf[0] = '\0';
            snprintf(tmpbuf, EUCA_MAX_PATH, "%s", newname->resource_type);
            strcat(url, tmpbuf);
        }

        // perform the create
        rc = midonet_http_post(url, newname->media_type, payload, &outloc);
        if (rc) {
            ret = 1;
        }
    } else {
        LOGERROR("could not generate payload\n");
        ret = 1;
    }

    // if all goes well, store the new resource
    if (!ret) {
        if (outmn && outloc) {
            rc = midonet_http_get(outloc, newname->media_type, &outhttp);
            if (rc) {
                LOGWARN("Failed to retrieve new resource from %s\n", outloc);
                ret = 1;
            } else {
                mido_copy_midoname(outmn, newname);
                if (outhttp) {
                    if (outmn->jsonbuf) {
                        EUCA_FREE(outmn->jsonbuf);
                    }
                    outmn->jsonbuf = strdup(outhttp);
                }

                outmn->init = 1;
                ret = mido_update_midoname(outmn);
            }
        }
    }

    EUCA_FREE(payload);
    EUCA_FREE(outhttp);
    EUCA_FREE(outloc);
    return (ret);
}

/**
 * Compares midoname structures a and b.
 * @param a midoname structure of interest.
 * @param b midoname structure of interest.
 * @return 
 */
int mido_cmp_midoname(midoname *a, midoname *b) {
    int ret=0;

    if (!a || !b) {
        return(1);
    }

    if (!a->init || !b->init) {
        return(1);
    }

    ret = strcmp(a->tenant, b->tenant);
    if (!ret) ret = strcmp(a->name, b->name);
    if (!ret) ret = strcmp(a->uuid, b->uuid);
    if (a->jsonbuf && b->jsonbuf) {
        if (!ret) ret = strcmp(a->jsonbuf, b->jsonbuf);
    }
    if (!ret) ret = strcmp(a->resource_type, b->resource_type);
    if (!ret) ret = strcmp(a->media_type, b->media_type);
    if (!ret) ret = strcmp(a->uri, b->uri);
    
    if (ret) {
        return(1);
    }
    return(0);
}

/**
 * Duplicates data from src midoname structure to dst midoname structure. NULL fields
 * are ignored. dst is initialized before processing.
 * @param [in] dst midoname structure
 * @param [in] src midoname structure
 */
void mido_copy_midoname(midoname *dst, midoname *src) {
    if (!dst || !src) {
        return;
    }
    if (dst == src) {
        LOGINFO("mido_copy_midoname: %s %s dst == src skipping\n", dst->resource_type, dst->name);
        return;
    }
    if (dst->init) {
        mido_free_midoname(dst);
    }

    bzero(dst, sizeof(midoname));
    if (src->tenant)
        dst->tenant = strdup(src->tenant);
    if (src->name)
        dst->name = strdup(src->name);
    if (src->uuid)
        dst->uuid = strdup(src->uuid);
    if (src->jsonbuf)
        dst->jsonbuf = strdup(src->jsonbuf);
    if (src->resource_type)
        dst->resource_type = strdup(src->resource_type);
    if (src->media_type)
        dst->media_type = strdup(src->media_type);
    if (src->uri)
        dst->uri = strdup(src->uri);

    dst->init = src->init;
}

/**
 * Create a midoname datastructure with the parameters in the argument.
 * @param [in] tenant MN tenant ID.
 * @param [in] name name of the MN object.
 * @param [in] uuid UUID of the MN object.
 * @param [in] resource_type MN resource type.
 * @param [in] media_type MN media type.
 * @param [in] uri MN URI.
 * @param [in] jsonbuf buffer that holds the full MN object information.
 * @param [out] outname pointer to midoname data structure that will be filled
 * with the information in the argument.
 * @return 0 on success.
 */
int mido_create_midoname(char *tenant, char *name, char *uuid, char *resource_type, char *media_type, char *uri, char *jsonbuf, midoname * outname) {
    if (!outname) {
        return (1);
    }

    bzero(outname, sizeof(midoname));
    if (tenant)
        outname->tenant = strdup(tenant);
    if (name)
        outname->name = strdup(name);
    if (uuid)
        outname->uuid = strdup(uuid);
    if (resource_type)
        outname->resource_type = strdup(resource_type);
    if (media_type)
        outname->media_type = strdup(media_type);
    if (jsonbuf)
        outname->jsonbuf = strdup(jsonbuf);
    if (uri)
        outname->uri = strdup(uri);

    outname->init = 1;

    return (0);
}

/**
 * Parses the JSON object in name->json
 * @param name [in] pointer to a midoname data structure
 * @return 0 on success. Positive integer otherwise.
 */
int mido_update_midoname(midoname *name) {
    int ret = 0;
    struct json_object *jobj = NULL, *el = NULL;
    char special_uuid[EUCA_MAX_PATH];

    if (!name || (!name->init)) {
        return (1);
    }
    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        LOGERROR("failed to parse %s\n", name->jsonbuf ? name->jsonbuf : "NULL");
        ret = 1;
    } else {
        json_object_object_get_ex(jobj, "id", &el);
        if (el) {
            EUCA_FREE(name->uuid);
            name->uuid = strdup(json_object_get_string(el));
        }

        json_object_object_get_ex(jobj, "tenantId", &el);
        if (el) {
            EUCA_FREE(name->tenant);
            name->tenant = strdup(json_object_get_string(el));
        }

        json_object_object_get_ex(jobj, "name", &el);
        if (el) {
            EUCA_FREE(name->name);
            name->name = strdup(json_object_get_string(el));
        }

        json_object_object_get_ex(jobj, "uri", &el);
        if (el) {
            EUCA_FREE(name->uri);
            name->uri = strdup(json_object_get_string(el));
        }

        // special cases
        if (!strcmp(name->resource_type, "dhcp")) {
            char *subnet = NULL, *slashnet = NULL;
            EUCA_FREE(name->uuid);
            EUCA_FREE(name->name);

            json_object_object_get_ex(jobj, "subnetPrefix", &el);
            if (el) {
                subnet = strdup(json_object_get_string(el));
            }

            json_object_object_get_ex(jobj, "subnetLength", &el);
            if (el) {
                slashnet = strdup(json_object_get_string(el));
            }

            if (subnet && slashnet) {
                snprintf(special_uuid, EUCA_MAX_PATH, "%s_%s", subnet, slashnet);
                name->uuid = strdup(special_uuid);
            }
            EUCA_FREE(subnet);
            EUCA_FREE(slashnet);

        } else if (!strcmp(name->resource_type, "ip_addrs")) {
            char *ip = NULL;
            EUCA_FREE(name->uuid);
            EUCA_FREE(name->name);
            json_object_object_get_ex(jobj, "addr", &el);
            if (el) {
                ip = strdup(json_object_get_string(el));
            }
            
            if (ip) {
                if (name->ipagip) {
                    EUCA_FREE(name->ipagip->ip);
                } else {
                    name->ipagip = EUCA_ZALLOC_C(1, sizeof (midoname_ipagip_extras));
                }
                name->ipagip->ip = strdup(ip);
                snprintf(special_uuid, EUCA_MAX_PATH, "versions/6/ip_addrs/%s", ip);
                name->uuid = strdup(special_uuid);
            }
            EUCA_FREE(ip);

        } else if (!strcmp(name->resource_type, "rules")) {
            if (name->rule) {
                EUCA_FREE(name->rule->type);
                EUCA_FREE(name->rule->nwdstaddress);
                EUCA_FREE(name->rule->nwdstlength);
                EUCA_FREE(name->rule->nwsrcaddress);
                EUCA_FREE(name->rule->nwsrclength);
                EUCA_FREE(name->rule->nattarget);
                EUCA_FREE(name->rule->jumpchainid);
            } else {
                name->rule = EUCA_ZALLOC_C(1, sizeof (midoname_rule_extras));
            }
            json_object_object_get_ex(jobj, "type", &el);
            if (el) {
                name->rule->type = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "nwDstAddress", &el);
            if (el) {
                name->rule->nwdstaddress = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "nwDstLength", &el);
            if (el) {
                name->rule->nwsrclength = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "nwSrcAddress", &el);
            if (el) {
                name->rule->nwsrcaddress = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "nwSrcLength", &el);
            if (el) {
                name->rule->nwdstlength = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "natTargets", &el);
            if (el) {
                name->rule->nattarget = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "jumpChainId", &el);
            if (el) {
                name->rule->jumpchainid = strdup(json_object_get_string(el));
            }

        } else if (!strcmp(name->resource_type, "ports")) {
            if (name->port) {
                EUCA_FREE(name->port->type);
                EUCA_FREE(name->port->hostid);
                EUCA_FREE(name->port->peerid);
                EUCA_FREE(name->port->ifname);
                EUCA_FREE(name->port->netaddr);
                EUCA_FREE(name->port->netlen);
                EUCA_FREE(name->port->portaddr);
                EUCA_FREE(name->port->portmac);
            } else {
                name->port = EUCA_ZALLOC_C(1, sizeof (midoname_port_extras));
            }
            json_object_object_get_ex(jobj, "type", &el);
            if (el) {
                name->port->type = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "hostId", &el);
            if (el) {
                name->port->hostid = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "peerId", &el);
            if (el) {
                name->port->peerid = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "interfaceName", &el);
            if (el) {
                name->port->ifname = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "networkAddress", &el);
            if (el) {
                name->port->netaddr = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "networkLength", &el);
            if (el) {
                name->port->netlen = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "portAddress", &el);
            if (el) {
                name->port->portaddr = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "portMac", &el);
            if (el) {
                name->port->portmac = strdup(json_object_get_string(el));
            }
        } else if (!strcmp(name->resource_type, "routes")) {
            if (name->route) {
                EUCA_FREE(name->route->srcnet);
                EUCA_FREE(name->route->srclen);
                EUCA_FREE(name->route->dstnet);
                EUCA_FREE(name->route->dstlen);
                EUCA_FREE(name->route->type);
                EUCA_FREE(name->route->nexthopport);
                EUCA_FREE(name->route->nexthopgateway);
                EUCA_FREE(name->route->weight);
            } else {
                name->route = EUCA_ZALLOC_C(1, sizeof (midoname_route_extras));
            }
            json_object_object_get_ex(jobj, "srcNetworkAddr", &el);
            if (el) {
                name->route->srcnet = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "srcNetworkLength", &el);
            if (el) {
                name->route->srclen = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "dstNetworkAddr", &el);
            if (el) {
                name->route->dstnet = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "dstNetworkLength", &el);
            if (el) {
                name->route->dstlen = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "type", &el);
            if (el) {
                name->route->type = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "nextHopPort", &el);
            if (el) {
                name->route->nexthopport = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "nextHopGateway", &el);
            if (el) {
                name->route->nexthopgateway = strdup(json_object_get_string(el));
            }
            json_object_object_get_ex(jobj, "weight", &el);
            if (el) {
                name->route->weight = strdup(json_object_get_string(el));
            }
        }
    
        if (!name->uuid || !strlen(name->uuid)) {
            if (name->uri && strlen(name->uri)) {
                name->uuid = strdup(name->uri);
            }
        }
        if (!name->name || (strlen(name->name) <= 0)) {
            name->name = strdup(name->uuid);
        }

        json_object_put(jobj);
    }

    return (ret);
}

/**
 * Deletes a MN construct
 * @param parentname [in] optional parent MN construct
 * @param name [in] MN construct to be removed
 * @return 0 on success. 1 on failure.
 */
int mido_delete_resource(midoname *parentname, midoname *name) {
    int rc = 0, ret = 0;
    char url[EUCA_MAX_PATH];

    if (!name || !name->init) {
        return (0);
    }

    url[0] = '\0';

    if (name->uri && strlen(name->uri)) {
        snprintf(url, EUCA_MAX_PATH, "%s", name->uri);
    }

    if (!strlen(url)) {
        if (parentname) {
            snprintf(url, EUCA_MAX_PATH, "%s/%s/%s/%s/%s", midonet_api_uribase, parentname->resource_type, parentname->uuid, name->resource_type, name->uuid);
        } else {
            snprintf(url, EUCA_MAX_PATH, "%s/%s/%s", midonet_api_uribase, name->resource_type, name->uuid);
        }
    }

    LOGTRACE("resource to delete: %s/%s url to delete: %s\n", SP(name->name), SP(name->uuid), url);

    rc = midonet_http_delete(url);
    if (rc) {
        ret = 1;
    }

    if (!ret) {
        mido_free_midoname(name);
    }
    return (ret);
}

/**
 * Memory writer call back function to be used with libcurl
 * @param contents [in] contents to be copied
 * @param size [in] size of each unit in contents
 * @param nmemb [in] number of units in contents
 * @param in_params [in/out] midonet-api parameters 
 * @return the number of bytes written.
 */
static size_t mem_writer(void *contents, size_t size, size_t nmemb, void *in_params) {
    struct mem_params_t *params = (struct mem_params_t *)in_params;

    if (!params->mem) {
        params->mem = calloc(1, 1);
    }
    params->mem = realloc(params->mem, params->size + (size * nmemb) + 1);
    if (params->mem == NULL) {
        return (0);
    }
    memcpy(&(params->mem[params->size]), contents, size * nmemb);
    params->size += size * nmemb;
    params->mem[params->size] = '\0';

    return (size * nmemb);
}

/**
 * Memory reader call back to be used with libcurl
 * @param contents [out] destination of the data to be read
 * @param size [in] size of each unit in contents
 * @param nmemb [in] number of units in contents
 * @param in_params [in/out] midonet-api parameters 
 * @return number of bytes read.
 */
static size_t mem_reader(void *contents, size_t size, size_t nmemb, void *in_params) {
    struct mem_params_t *params = (struct mem_params_t *)in_params;
    size_t bytes_to_copy = 0;

    if (!params->mem || params->size <= 0) {
        return (0);
    }

    if (!contents) {
        LOGERROR("ERROR: no mem to write into\n");
        params->size = 0;
        return (0);
    }

    bytes_to_copy = (params->size < (size * nmemb)) ? params->size : (size * nmemb);

    memcpy(contents, params->mem, bytes_to_copy);
    params->size -= bytes_to_copy;
    params->mem += bytes_to_copy;

    return (bytes_to_copy);
}

/**
 * Initializes resources to be used in midonet-api calls
 */
void midonet_api_init(void) {
    // Initialize media_types array
    for (int i = 0; i < APPLICATION_MAX_INDEX; i++) {
        midonet_api_mtypes[i][0] = '\0';
    }
    snprintf(midonet_api_mtypes[APPLICATION_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_JSON_V5);
    snprintf(midonet_api_mtypes[APPLICATION_HOST_INTERFACE_PORT], MIDO_MTYPE_MAX_LEN, APPLICATION_HOST_INTERFACE_PORT_V1);
    snprintf(midonet_api_mtypes[APPLICATION_PORT_LINK], MIDO_MTYPE_MAX_LEN, APPLICATION_PORT_LINK_V1);
    snprintf(midonet_api_mtypes[APPLICATION_SYSTEM_STATE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_SYSTEM_STATE_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_BGP_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_BGP_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_BGP_PEER_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_BGP_PEER_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_AD_ROUTE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_AD_ROUTE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_BGP_NETWORK_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_BGP_NETWORK_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_BRIDGE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_BRIDGE_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_DHCP_SUBNET_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_DHCP_SUBNET_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_DHCP_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_DHCP_HOST_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_PORT_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_PORT_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_CHAIN_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_CHAIN_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_RULE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_RULE_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_HOST_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_INTERFACE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_INTERFACE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_IP_ADDR_GROUP_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_IP_ADDR_GROUP_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_IP_ADDR_GROUP_ADDR_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_IP_ADDR_GROUP_ADDR_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_PORT_GROUP_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_PORT_GROUP_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_PORT_GROUP_PORT_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_PORT_GROUP_PORT_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_ROUTER_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_ROUTER_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_ROUTE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_ROUTE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_TUNNEL_ZONE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_TUNNEL_ZONE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_TUNNEL_ZONE_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_TUNNEL_ZONE_HOST_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_SYSTEM_STATE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_SYSTEM_STATE_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_BGP_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_BGP_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_BGP_PEER_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_BGP_PEER_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_AD_ROUTE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_AD_ROUTE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_BGP_NETWORK_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_BGP_NETWORK_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_BRIDGE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_BRIDGE_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_DHCP_SUBNET_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_DHCP_SUBNET_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_DHCP_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_DHCP_HOST_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_PORT_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_PORT_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_CHAIN_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_CHAIN_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_RULE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_RULE_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_HOST_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_INTERFACE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_INTERFACE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_IP_ADDR_GROUP_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_IP_ADDR_GROUP_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_IP_ADDR_GROUP_ADDR_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_IP_ADDR_GROUP_ADDR_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_PORT_GROUP_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_PORT_GROUP_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_PORT_GROUP_PORT_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_PORT_GROUP_PORT_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_ROUTER_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_ROUTER_JSON_V2);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_ROUTE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_ROUTE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_TUNNEL_ZONE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_TUNNEL_ZONE_JSON_V1);
    snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_TUNNEL_ZONE_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_TUNNEL_ZONE_HOST_JSON_V1);

    mido_libcurl_init(&libcurl_handles);
    mido_initialize_apiuribase();
    
    // Check MidoNet API version - only API version v5.0 supported
    if (strcmp(midonet_api_version, "v5.0")) {
        LOGWARN("Unsupported MidoNet API version (%s) detected.\n", midonet_api_version);
        LOGINFO("Note:\n"
                "Eucalyptus (%s) has been validated with MEM v5.2, and partially with open source v5.2.\n"
                "Please update MidoNet to a compatible version (MEM v5.2 recommended).\n",
                EUCA_VERSION);
    }
    
    if (!strncmp(midonet_api_version, "v5.0", 16)) {
        snprintf(midonet_api_mtypes[APPLICATION_PORT_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_PORT_JSON_V3);
        snprintf(midonet_api_mtypes[APPLICATION_BRIDGE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_BRIDGE_JSON_V4);
        snprintf(midonet_api_mtypes[APPLICATION_ROUTER_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_ROUTER_JSON_V3);
        snprintf(midonet_api_mtypes[APPLICATION_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_HOST_JSON_V3);
        snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_PORT_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_PORT_JSON_V3);
        snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_BRIDGE_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_BRIDGE_JSON_V4);
        snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_ROUTER_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_ROUTER_JSON_V3);
        snprintf(midonet_api_mtypes[APPLICATION_COLLECTION_HOST_JSON], MIDO_MTYPE_MAX_LEN, APPLICATION_COLLECTION_HOST_JSON_V3);
    }
}

/**
 * Cleanup resources used in midonet-api calls
 */
void midonet_api_cleanup(void) {
    mido_libcurl_cleanup(&libcurl_handles);
}

/**
 * Gets the MidoNet API version.
 * @param [out] optional pointer to a string to hold a string representation of
 * detected MidoNet API version. Caller is responsible to release the memory allocated.
 * @return pointer to a statically allocated string with representation of
 * detected MidoNet API version.
 */
char *midonet_api_get_version(char **version) {
    if (version) {
        if (!strlen(midonet_api_version)) {
            *version = strdup("unknown");
        } else {
            *version = strdup(midonet_api_version);
        }
    }
    return (midonet_api_version);
}

/**
 * Check if MidoNet API matches version "v1.9"
 * @return TRUE if detected MidoNet API version is "v1.9". FALSE otherwise.
 */
extern inline boolean is_midonet_api_v1(void);

/**
 * Check if MidoNet API matches version "v5.0"
 * @return TRUE if detected MidoNet API version is "v5.0". FALSE otherwise.
 */
extern inline boolean is_midonet_api_v5(void);

/**
 * Gets the MidoNet API uribase.
 * @param [out] optional pointer to a string to hold a string representation of
 * detected MidoNet API uribase. Caller is responsible to release the memory allocated.
 * @return pointer to a statically allocated string with representation of
 * detected MidoNet API uribase.
 */
char *midonet_api_get_uribase(char **uribase) {
    if (uribase) {
        if (!strlen(midonet_api_uribase)) {
            *uribase = strdup("unknown");
        } else {
            *uribase = strdup(midonet_api_uribase);
        }
    }
    return (midonet_api_uribase);
}

/**
 * Cleanup possibly open libcurl easy_handles used by midonet-api
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @return 0 on success. Positive integer otherwise.
 */
int mido_libcurl_cleanup_handles(mido_libcurl_handles *handles) {
    pthread_mutex_lock(&libcurl_handles_mutex);
    if (handles) {
        for (int i = 0; i < handles->max_handles; i++) {
            curl_easy_cleanup(handles->handles[i]);
        }
        EUCA_FREE(handles->handles);
        for (int i = 0; i < handles->max_gethandles; i++) {
            curl_easy_cleanup(handles->gethandles[i]);
        }
        EUCA_FREE(handles->gethandles);
        bzero(handles, sizeof (mido_libcurl_handles));
    }
    pthread_mutex_unlock(&libcurl_handles_mutex);
    return (0);
}

/**
 * Initializes libcurl to be used for midonet-api.
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @return 0 on success. Positive integer on failure.
 */
int mido_libcurl_init(mido_libcurl_handles *handles) {
    if (mido_libcurl_initialized == 1) {
        return (0);
    }
    if (handles == NULL) {
        LOGWARN("Cannot initialize libcurl with NULL mido_libcurl_handles\n");
        return (1);
    }
    curl_global_init(CURL_GLOBAL_DEFAULT);
    pthread_mutex_init(&libcurl_handles_mutex, NULL);
    pthread_mutex_init(&mido_buffer_mutex, NULL);
    pthread_mutex_init(&mido_cache_ports_mutex, NULL);
    mido_libcurl_initialized = 1;
    mido_libcurl_cleanup_handles(handles);
    return (0);
}

/**
 * Cleanup libcurl resources used by midonet-api
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @return 0 on success. Positive integer otherwise.
 */
int mido_libcurl_cleanup(mido_libcurl_handles *handles) {
    mido_libcurl_cleanup_handles(handles);
    curl_global_cleanup();
    pthread_mutex_destroy(&libcurl_handles_mutex);
    pthread_mutex_destroy(&mido_buffer_mutex);
    pthread_mutex_destroy(&mido_cache_ports_mutex);
    mido_libcurl_initialized = 0;
    return (0);
}

/**
 * Retrieve a libcurl easy_handle
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @return pointer to an easy_handle of interest
 */
CURL *mido_libcurl_get_handle(mido_libcurl_handles *handles) {
    if (handles == NULL) {
        LOGWARN("Invalid argument: cannot retrieve easy_handle from NULL\n");
        return (NULL);
    }
    CURL *res = NULL;
    pthread_mutex_lock(&libcurl_handles_mutex);
    if (handles->max_handles > 0) {
        res = handles->handles[handles->max_handles - 1];
        handles->max_handles--;
        curl_easy_reset(res);
    } else {
        res = curl_easy_init();
        if (!res) {
            LOGERROR("Unable to get libcurl easy_handle\n");
        } else {
            curl_easy_setopt(res, CURLOPT_NOSIGNAL, 1L);
        }
    }
    pthread_mutex_unlock(&libcurl_handles_mutex);
    return (res);
}

/**
 * Retrieve a libcurl easy_handle tailored for GET operations.
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @return pointer to an easy_handle of interest
 */
CURL *mido_libcurl_get_gethandle(mido_libcurl_handles *handles) {
    if (handles == NULL) {
        LOGWARN("Invalid argument: cannot retrieve get easy_handle from NULL\n");
        return (NULL);
    }
    CURL *res = NULL;
    pthread_mutex_lock(&libcurl_handles_mutex);
    if (handles->max_gethandles > 0) {
        res = handles->gethandles[handles->max_gethandles - 1];
        handles->max_gethandles--;
        curl_easy_reset(res);
    } else {
        res = curl_easy_init();
        if (!res) {
            LOGERROR("Unable to get libcurl easy_handle\n");
        }
    }
    pthread_mutex_unlock(&libcurl_handles_mutex);
    if (res) {
        curl_easy_setopt(res, CURLOPT_HTTPGET, 1L);
        curl_easy_setopt(res, CURLOPT_WRITEFUNCTION, mem_writer);
        curl_easy_setopt(res, CURLOPT_NOSIGNAL, 1L);
        //curl_easy_setopt(res, CURLOPT_TCP_KEEPALIVE, 1L);
        //curl_easy_setopt(res, CURLOPT_TCP_KEEPINTVL, 30L);
        //curl_easy_setopt(res, CURLOPT_TCP_KEEPIDLE, 600L);
    }
    return (res);
}

/**
 * Releases the libcurl easy_handle in the argument for other threads to use.
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @param handle [in] libcurl easy_handle to release. The handle is not closed, just
 * returned to a pool.
 * @return 0 on success. 1 on any error.
 */
int mido_libcurl_release_handle(mido_libcurl_handles *handles, CURL *handle) {
    if (!handles || !handle) {
        LOGWARN("Invalid argument: cannot release NULL handle\n");
        return (1);
    }
    pthread_mutex_lock(&libcurl_handles_mutex);
    handles->handles = EUCA_APPEND_PTRARR(handles->handles, &(handles->max_handles), handle);
    pthread_mutex_unlock(&libcurl_handles_mutex);
    return (0);
}

/**
 * Releases the libcurl easy_handle in the argument for other threads to use.
 * @param handles [in] pointer to mido_libcurl_handles structure
 * @param handle [in] libcurl easy_handle to release. The handle is not closed, just
 * returned to a pool.
 * @return 0 on success. 1 on any error.
 */
int mido_libcurl_release_gethandle(mido_libcurl_handles *handles, CURL *handle) {
    if (!handles || !handle) {
        LOGWARN("Invalid argument: cannot release NULL GET handle\n");
        return (1);
    }
    pthread_mutex_lock(&libcurl_handles_mutex);
    handles->gethandles = EUCA_APPEND_PTRARR(handles->gethandles, &(handles->max_gethandles), handle);
    pthread_mutex_unlock(&libcurl_handles_mutex);
    return (0);
}

/**
 * Performs http GET operation using libcurl
 * @param url [in] the http url of interest.
 * @param apistr [in] optional API string, to be used in "accept: ____" http header
 * @param out_payload [out] pointer to a string where the result is stored. Caller
 * is responsible to release the memory allocated.
 * @return 0 on success. Positive integer on any failure.
 */
int midonet_http_get(char *url, char *apistr, char **out_payload) {

    CURL *curl = NULL;
    CURLcode curlret;
    struct mem_params_t mem_writer_params = { 0, 0 };
    int ret = 0;
    long httpcode = 0L;
    struct curl_slist *headers = NULL;
    char hbuf[EUCA_MAX_PATH];
    long int httptime;

    struct timeval tv;
    eucanetd_timer_usec(&tv);
    *out_payload = NULL;

    curl = mido_libcurl_get_gethandle(&libcurl_handles);
    if (!curl) {
        LOGWARN("failed to get a libcurl handle - unable to perform http GET\n");
        return (1);
    }

    curl_easy_setopt(curl, CURLOPT_URL, url);

    //curl = curl_easy_init();
    //curl_easy_setopt(curl, CURLOPT_URL, url);
    //curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);
    //curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, mem_writer);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &mem_writer_params);

    if (apistr && strlen(apistr)) {
        snprintf(hbuf, EUCA_MAX_PATH, "accept: %s", apistr);
        headers = curl_slist_append(headers, hbuf);
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    } else {
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, NULL);
    }

    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        LOGERROR("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if (httpcode != 200L) {
        LOGWARN("curl get http code: %ld\nurl: %s\napistr: %s\n", httpcode, url, apistr);
        ret = 1;
    }
    curl_slist_free_all(headers);
    mido_libcurl_release_gethandle(&libcurl_handles, curl);
    //curl_easy_cleanup(curl);
    //curl_global_cleanup();

    // convert to payload out

    if (!ret) {
        if (mem_writer_params.mem && mem_writer_params.size > 0) {
            *out_payload = EUCA_ZALLOC_C(mem_writer_params.size + 1, sizeof(char));
            memcpy(*out_payload, mem_writer_params.mem, mem_writer_params.size + 1);
        } else {
            LOGERROR("ERROR: no data to return after successful curl operation\n");
            ret = 1;
        }
    }
    if (mem_writer_params.mem)
        free(mem_writer_params.mem);

    httptime = eucanetd_timer_usec(&tv);
    LOGTRACE("total time for http GET operation: %ld us\n", httptime);
    http_gets++;
    http_gets_time += httptime;
    return (ret);
}

/**
 * Performs http PUT operation using libcurl
 * @param url [in] the http url of interest.
 * @param apistr [in] API string (media type), to be used in "Content-Type: ____" http header
 * @param payload [in] a JSON string to be used as the payload of this operation.
 * @return 0 on success. Positive integer on any failure.
 */
int midonet_http_put(char *url, char *apistr, char *payload) {
    CURL *curl = NULL;
    CURLcode curlret;
    struct mem_params_t mem_reader_params = { 0, 0 };
    char hbuf[EUCA_MAX_PATH];
    struct curl_slist *headers = NULL;
    int ret = 0;
    long httpcode = 0L;
    struct timeval tv;
    long int httptime;

    eucanetd_timer_usec(&tv);

    mido_check_state();

    mem_reader_params.mem = payload;
    mem_reader_params.size = strlen(payload) + 1;

    curl = mido_libcurl_get_handle(&libcurl_handles);
    //curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt(curl, CURLOPT_PUT, 1L);
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, mem_reader);
    curl_easy_setopt(curl, CURLOPT_READDATA, (void *)&mem_reader_params);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (long)mem_reader_params.size);

    if (apistr && strlen(apistr)) {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: %s", apistr);
    } else {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/json");
    }
    headers = curl_slist_append(headers, hbuf);
    snprintf(hbuf, EUCA_MAX_PATH, "Expect:");
    headers = curl_slist_append(headers, hbuf);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    LOGTRACE("PUT PAYLOAD: %s\n", SP(payload));
    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        LOGERROR("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if (httpcode == 200L || httpcode == 204L) {
        ret = 0;
        midonet_api_system_changed = 1;
    } else {
        LOGWARN("curl put http code: %ld\n", httpcode);
        LOGINFO("\turl %s payload %s\n", url, SP(payload));
        ret = 1;
    }

    curl_slist_free_all(headers);
    mido_libcurl_release_handle(&libcurl_handles, curl);
    //curl_easy_cleanup(curl);
    //curl_global_cleanup();

    httptime = eucanetd_timer_usec(&tv);
    LOGTRACE("total time for http PUT operation: %ld us\n", httptime);
    http_puts++;
    http_puts_time += httptime;
    return (ret);
}

/**
 * Callback function to read header data used with libcurl.
 * @param content [in] header data from libcurl
 * @param size [in] size of elements in content
 * @param nmemb [in] number of elements in content
 * @param params [out] midonet-api parameters
 * @return number of bytes read.
 */
static size_t header_find_location(char *content, size_t size, size_t nmemb, void *params) {
    char *buf = NULL;
    char **loc = (char **)params;

    buf = EUCA_ZALLOC_C((size * nmemb) + 1, sizeof(char));
    memcpy(buf, content, size * nmemb);
    buf[size * nmemb] = '\0';

    if (buf && strstr(buf, "Location: ")) {
        *loc = EUCA_ZALLOC_C(strlen(buf), sizeof(char));
        sscanf(buf, "Location: %s", *loc);
    }
    free(buf);

    return (size * nmemb);
}

/**
 * Performs http POST operation using libcurl
 * @param url [in] the http url of interest.
 * @param apistr [in] API string (media type), to be used in "Content-Type: ____" http header
 * @param payload [in] a JSON string to be used as the payload of this operation.
 * @param out_payload [out] optional pointer to a string where the results of this
 * POST operation will be stored.
 * @return 0 on success. Positive integer on any failure.
 */
int midonet_http_post(char *url, char *apistr, char *payload, char **out_payload) {
    CURL *curl = NULL;
    CURLcode curlret;
    int ret = 0;
    char *loc = NULL, hbuf[EUCA_MAX_PATH];
    struct curl_slist *headers = NULL;
    long httpcode = 0L;
    struct timeval tv;
    long int httptime;

    eucanetd_timer_usec(&tv);

    mido_check_state();

    *out_payload = NULL;

    curl = mido_libcurl_get_handle(&libcurl_handles);
    //curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    //    curl_easy_setopt(curl, CURLOPT_HEADER, 1L);
    curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
    curl_easy_setopt(curl, CURLOPT_POST, 1L);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, payload);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, strlen(payload));
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_find_location);
    curl_easy_setopt(curl, CURLOPT_HEADERDATA, &loc);
    if (apistr && strlen(apistr)) {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: %s", apistr);
    } else {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/json");
    }
    headers = curl_slist_append(headers, hbuf);
    //    headers = curl_slist_append(headers, "Content-Type: application/vnd.org.midonet");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    LOGTRACE("POST PAYLOAD: %s\n", SP(payload));
    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        LOGERROR("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    } else {
        midonet_api_system_changed = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if ((httpcode != 200L) && (httpcode != 201L)) {
        LOGWARN("curl post http code: %ld\n", httpcode);
        LOGINFO("\turl %s payload %s\n", url, payload);
        ret = 1;
    }
    curl_slist_free_all(headers);
    mido_libcurl_release_handle(&libcurl_handles, curl);
    //curl_easy_cleanup(curl);
    //curl_global_cleanup();

    if (!ret) {
        if (loc) {
            *out_payload = strdup(loc);
        }
    }
    EUCA_FREE(loc);

    httptime = eucanetd_timer_usec(&tv);
    LOGTRACE("total time for http POST operation: %ld us\n", httptime);
    http_posts++;
    http_posts_time += httptime;
    return (ret);
}

/**
 * Performs http DELETE operation using libcurl
 * @param url [in] the http url of interest.
 * @return 0 on success. Positive integer on any failure.
 */
int midonet_http_delete(char *url) {
    CURL *curl = NULL;
    CURLcode curlret;
    int ret = 0;
    long httpcode = 0L;
    struct timeval tv;
    long int httptime;

    eucanetd_timer_usec(&tv);

    mido_check_state();
        
    curl = mido_libcurl_get_handle(&libcurl_handles);
    //curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");

    LOGTRACE("DELETE PAYLOAD: %s\n", SP(url));
    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        LOGERROR("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    } else {
        midonet_api_system_changed = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if ((httpcode != 200L) && (httpcode != 204L)) {
        LOGWARN("curl delete http code: %ld\n", httpcode);
        LOGINFO("\turl %s\n", SP(url));
        ret = 1;
    }

    mido_libcurl_release_handle(&libcurl_handles, curl);
    //curl_easy_cleanup(curl);
    //curl_global_cleanup();

    httptime = eucanetd_timer_usec(&tv);
    LOGTRACE("total time for http DELETE operation: %ld us\n", httptime);
    http_deletes++;
    http_deletes_time += httptime;
    return (ret);
}

/**
 * Searches for a mido router route specified in the arguments from a list (also
 * specified in the arguments). 
 *
 * @param routes [in] list of routes to look for a matching route entry.
 * @param max_routes [in] number of routes in the list.
 * @param rport [in] router port to be routed.
 * @param src [in] source subnet.
 * @param src_slashnet [in] source slash net.
 * @param dst [in] destination subnet.
 * @param dst_slashnet [in] destination slash net.
 * @param next_hop_ip [in] next hop.
 * @param weight [in] route weight.
 * @param foundidx [out] index at which the route was found. -1 if the route was not found.
 * @return 0 if the route was found. 1 otherwise.
 */
int mido_find_route_from_list(midoname **routes, int max_routes, midoname *rport,
        char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip,
        char *weight, int *foundidx) {
    int rc = 0, found = 0, ret = 0;
    int i = 0;

    if ((routes == NULL) || (max_routes == 0)) {
        return (1);
    }

    found = 0;
    for (i = 0; i < max_routes && !found; i++) {
        if ((routes[i] == NULL) || (routes[i]->init == 0)) {
            continue;
        }
        if (!next_hop_ip || strcmp(next_hop_ip, "UNSET")) {
            rc = 1;
            if (routes[i]->route) {
                if ((src && routes[i]->route->srcnet && !strcmp(src, routes[i]->route->srcnet)) &&
                        (src_slashnet && routes[i]->route->srclen && !strcmp(src_slashnet, routes[i]->route->srclen)) &&
                        (dst && routes[i]->route->dstnet && !strcmp(dst, routes[i]->route->dstnet)) &&
                        (dst_slashnet && routes[i]->route->dstlen && !strcmp(dst_slashnet, routes[i]->route->dstlen)) &&
                        (!strcmp("Normal", routes[i]->route->type)) &&
                        (rport->uuid && routes[i]->route->nexthopport && !strcmp(rport->uuid, routes[i]->route->nexthopport)) &&
                        (weight && routes[i]->route->weight && !strcmp(weight, routes[i]->route->weight)) &&
                        (next_hop_ip && routes[i]->route->nexthopgateway && !strcmp(next_hop_ip, routes[i]->route->nexthopgateway))) {
                    rc = 0;
                }
            }
            if (!rc) {
                found = 1;
                if (foundidx) {
                    *foundidx = i;
                }
            }
        } else {
            rc = mido_cmp_midoname_to_input(routes[i], "srcNetworkAddr", src, "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength", dst_slashnet,
                    "type", "Normal", "nextHopPort", rport->uuid, "weight", weight, NULL);
            rc = 1;
            if (routes[i]->route) {
                if ((src && routes[i]->route->srcnet && !strcmp(src, routes[i]->route->srcnet)) &&
                        (src_slashnet && routes[i]->route->srclen && !strcmp(src_slashnet, routes[i]->route->srclen)) &&
                        (dst && routes[i]->route->dstnet && !strcmp(dst, routes[i]->route->dstnet)) &&
                        (dst_slashnet && routes[i]->route->dstlen && !strcmp(dst_slashnet, routes[i]->route->dstlen)) &&
                        (!strcmp("Normal", routes[i]->route->type)) &&
                        (rport->uuid && routes[i]->route->nexthopport && !strcmp(rport->uuid, routes[i]->route->nexthopport)) &&
                        (weight && routes[i]->route->weight && !strcmp(weight, routes[i]->route->weight))) {
                    rc = 0;
                }
            }
            if (!rc) {
                found = 1;
                if (foundidx) {
                    *foundidx = i;
                }
            }
        }
    }
    if (found) {
        ret = 0;
    } else {
        ret = 1;
    }
    return (ret);
}

/**
 * Creates a new router route as specified in the argument. 
 *
 * @param rt [in] router or interest. rt has priority over router. If rt is NULL,
 * midocache lookup is performed based on router.
 * @param router [in] router of interest.
 * @param rport [in] router port where matching packets will be routed.
 * @param src [in] source subnet.
 * @param src_slashnet [in] source slash net.
 * @param dst [in] destination subnet.
 * @param dst_slashnet [in] destination slash net.
 * @param next_hop_ip [in] next hop.
 * @param weight [in] route weight.
 * @param outname [i/o] pointer to an extant MidoNet route (parameters will be checked
 * to avoid duplicate route creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created route will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_route(midonet_api_router *rt, midoname *router, midoname *rport, char *src, char *src_slashnet,
        char *dst, char *dst_slashnet, char *next_hop_ip, char *weight, midoname **outname) {
    int rc = 0, found = 0, ret = 0;
    midoname myname;
    midoname **routes = NULL;
    int max_routes = 0;
    int foundidx = 0;

    midoname *out = NULL;
    
    if ((router == NULL) && (rt == NULL)) {
        LOGWARN("Invalid argument: cannot create route for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    if (rt == NULL) {
        rt = midonet_api_cache_lookup_router(router, NULL);
    }
    if (out && out->init) {
        rc = mido_find_route_from_list(outname, 1, rport, src, src_slashnet,
                dst, dst_slashnet, next_hop_ip, weight, &foundidx);
        if (rc == 0) {
            found = 1;
            LOGEXTREME("route already in mido - abort create\n");
            return (0);
        }
    }
    // get the router information from midocache
    if (rt == NULL) {
        LOGWARN("Unable to create route for NULL router.\n");
        return (1);
    } else {
        routes = rt->routes;
        max_routes = rt->max_routes;
    }
    rc = mido_find_route_from_list(routes, max_routes, rport, src, src_slashnet,
            dst, dst_slashnet, next_hop_ip, weight, &foundidx);
    if (rc == 0) {
        found = 1;
        LOGEXTREME("route already in mido - abort create.\n");
        if (outname) {
            *outname = routes[foundidx];
        }
    }

    if (!found) {
        bzero(&myname, sizeof (midoname));
        myname.tenant = strdup(rt->obj->tenant);
        myname.resource_type = strdup("routes");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_ROUTE_JSON]);

        if (strcmp(next_hop_ip, "UNSET")) {
            rc = mido_create_resource(rt->obj, 1, &myname, &out, "srcNetworkAddr", src,
                    "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength",
                    dst_slashnet, "type", "Normal", "nextHopPort", rport->uuid,
                    "weight", weight, "nextHopGateway", next_hop_ip, NULL);
        } else {
            rc = mido_create_resource(rt->obj, 1, &myname, &out, "srcNetworkAddr", src,
                    "srcNetworkLength", src_slashnet, "dstNetworkAddr", dst, "dstNetworkLength",
                    dst_slashnet, "type", "Normal", "nextHopPort", rport->uuid, "weight", weight, NULL);
        }
        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            midonet_api_cache_add_router_route(rt, out);
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }

    return (ret);
}

/**
 * Deletes a router route from MidoNet.
 * @param router [in] router (no checks performed) of interest.
 * @param name [in] route (no checks performed) of interest.
 * @return 0 on success. 1 otherwise.
 */
int mido_delete_route(midonet_api_router *router, midoname *name) {
    int rc = 0;
    if (!router || !router->obj || !name || !name->jsonbuf) {
        return (1);
    }
    rc = midonet_api_cache_del_router_route(router, name);
    rc = mido_delete_resource(NULL, name);

    return (rc);
}

/**
 * Retrieves an array of pointers to midonet object representing router routes.
 * @param router [in] router of interest.
 * @param outnames [out] an array of pointers to midonet objects representing router routes, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_routes(midoname *router, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_router *rt = midonet_api_cache_lookup_router(router, NULL);
        if (rt == NULL) {
            LOGWARN("MIDOCACHE: %s not found\n", router->name);
        } else {
            if (rt->max_routes > 0) {
                *outnames = EUCA_ZALLOC_C(rt->max_routes, sizeof (midoname *));
            }
            midoname **names = *outnames;
            for (int i = 0; i < rt->max_routes; i++) {
                if (rt->routes[i] == NULL) {
                    continue;
                }
                names[count] = rt->routes[i];
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(router, 1, router->tenant, "routes",
            midonet_api_mtypes[APPLICATION_COLLECTION_ROUTE_JSON],
            midonet_api_mtypes[APPLICATION_ROUTE_JSON],
            outnames, outnames_max));
}

/**
 * Creates a bgp end-point on the device (port MN v1.9, or router MN v5.0) dev using
 * the parameters in the argument.
 * @param dev [in] device to create the bgp end-point
 * @param localAS [in] the local ASN
 * @param peerAS [in] the remote/peer ASN
 * @param peerAddr [in] the remote/peer BGP router IP
 * @param outname [i/o] pointer to an extant MidoNet object (parameters will be checked
 * to avoid duplicate bgp creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created bgp will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_bgp(midoname *dev, u32 localAS, u32 peerAS, char *peerAddr, midoname **outname) {
    if (is_midonet_api_v1()) {
        return (mido_create_bgp_v1(dev, localAS, peerAS, peerAddr, outname));
    } else if (is_midonet_api_v5()) {
        return (mido_create_bgp_v5(dev, localAS, peerAS, peerAddr, outname));
    } else {
        return (1);
    }
}

/**
 * Creates a bgp end-point on port using the parameters in the argument (MN1.9).
 * @param port [in] device to create the bgp end-point
 * @param localAS [in] the local ASN
 * @param peerAS [in] the remote/peer ASN
 * @param peerAddr [in] the remote/peer BGP router IP
 * @param outname [i/o] pointer to an extant MidoNet object (parameters will be checked
 * to avoid duplicate bgp creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created bgp will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_bgp_v1(midoname *port, u32 localAS, u32 peerAS, char *peerAddr, midoname **outname) {
    int rc = 0, found = 0, ret = 0;
    midoname myname;

    midoname *out = NULL;
    
    if (port == NULL)  {
        LOGWARN("Invalid argument: cannot create bgp for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    midoname **bgps = NULL;
    int max_bgps = 0;
    mido_get_bgps(port, &bgps, &max_bgps);
    found = 0;
    for (int i = 0; i < max_bgps && !found; i++) {
        char *las = NULL;
        char *pas = NULL;
        char *paddr = NULL;
        mido_getel_midoname(bgps[i], "localAS", &las);
        mido_getel_midoname(bgps[i], "peerAddr", &paddr);
        mido_getel_midoname(bgps[i], "peerAS", &pas);
        if (las && pas && paddr) {
            if ((localAS == atoi(las)) && (peerAS == atoi(pas)) && !strcmp(peerAddr, paddr)) {
                found = 1;
                out = bgps[i];
            }
        }
        EUCA_FREE(las);
        EUCA_FREE(pas);
        EUCA_FREE(paddr);
    }
    EUCA_FREE(bgps);
    if (found) {
        LOGTRACE("bgp already in mido - abort create\n");
        if (outname) {
            *outname = out;
        }
        return (0);
    }

    if (!found) {
        memset(&myname, 0, sizeof (midoname));
        myname.tenant = strdup(VPCMIDO_TENANT);
        myname.resource_type = strdup("bgps");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_BGP_JSON]);

        char sLocalAS[16];
        char sPeerAS[16];
        snprintf(sLocalAS, 16, "%u", localAS);
        snprintf(sPeerAS, 16, "%u", peerAS);
        rc = mido_create_resource(port, 1, &myname, &out, "localAS", sLocalAS,
                "peerAS", sPeerAS, "peerAddr", peerAddr, NULL);

        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }

    return (ret);
}

/**
 * Creates a bgp end-point on router using the parameters in the argument (MN5.0).
 * @param router [in] router to create the bgp end-point
 * @param localAS [in] the local ASN - will overwrite router's ASN
 * @param peerAS [in] the remote/peer ASN
 * @param peerAddr [in] the remote/peer BGP router IP
 * @param outname [i/o] pointer to an extant MidoNet object (parameters will be checked
 * to avoid duplicate bgp creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created bgp will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_bgp_v5(midoname *router, u32 localAS, u32 peerAS, char *peerAddr, midoname **outname) {
    int rc = 0, found = 0, ret = 0;
    midoname myname;
    boolean do_bgp_peer = TRUE;
    boolean do_router_asn = TRUE;

    midoname *out = NULL;
    
    if (router == NULL)  {
        LOGWARN("Invalid argument: cannot create bgp for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    midoname **bgps = NULL;
    int max_bgps = 0;
    mido_get_bgps(router, &bgps, &max_bgps);
    found = 0;
    for (int i = 0; i < max_bgps && !found; i++) {
        char *pas = NULL;
        char *paddr = NULL;
        mido_getel_midoname(bgps[i], "address", &paddr);
        mido_getel_midoname(bgps[i], "asNumber", &pas);
        if (pas && paddr) {
            if ((peerAS == atoi(pas)) && !strcmp(peerAddr, paddr)) {
                do_bgp_peer = FALSE;
                found = 1;
                out = bgps[i];
            }
        }
        EUCA_FREE(pas);
        EUCA_FREE(paddr);
    }
    EUCA_FREE(bgps);
    if (found) {
        char *las = NULL;
        mido_getel_midoname(router, "asNumber", &las);
        if (localAS == atoi(las)) {
            do_router_asn = FALSE;
            LOGTRACE("bgp already in mido - abort create\n");
        } else {
            found = 0;
        }
        EUCA_FREE(las);
    }
    if (!do_router_asn && !do_bgp_peer) {
        if (outname) {
            *outname = out;
        }
        return (0);
    }

    if (!found) {
        if (do_router_asn) {
            char sLocalAS[16];
            snprintf(sLocalAS, 16, "%u", localAS);
            rc = mido_update_router(router, "id", router->uuid, "name", router->name,
                    "tenantId", router->tenant, "asNumber", sLocalAS, NULL);
            if ((rc != 0) && (rc != -1)) {
                LOGERROR("failed to update router asNumber\n");
                ret++;
            }
        }
        
        memset(&myname, 0, sizeof (midoname));
        myname.tenant = strdup(VPCMIDO_TENANT);
        myname.resource_type = strdup("bgp_peers");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_BGP_PEER_JSON]);

        char sPeerAS[16];
        snprintf(sPeerAS, 16, "%u", peerAS);
        rc = mido_create_resource(router, 1, &myname, &out, "asNumber", sPeerAS,
                "address", peerAddr, NULL);

        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            ret += 0;
        } else if (rc < 0) {
            ret += 0;
        } else {
            ret += 1;
        }
        mido_free_midoname(&myname);
    }

    return (ret);
}

/**
 * Retrieves an array of pointers to midonet object representing bgps.
 * This call does not update and/or access midocache.
 * @param dev [in] device (router port MN1.9 or router MN5) of interest.
 * @param outnames [out] an array of pointers to midonet objects representing bgps, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bgps(midoname *dev, midoname ***outnames, int *outnames_max) {
    if (is_midonet_api_v1()) {
        return (mido_get_bgps_v1(dev, outnames, outnames_max));
    } else if (is_midonet_api_v5()) {
        return (mido_get_bgps_v5(dev, outnames, outnames_max));
    } else {
        return (1);
    }
}

/**
 * Retrieves an array of pointers to midonet object representing bgps (MN1.9).
 * This call does not update and/or access midocache.
 * @param port [in] router port of interest.
 * @param outnames [out] an array of pointers to midonet objects representing router port bgps, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bgps_v1(midoname *port, midoname ***outnames, int *outnames_max) {
    return (mido_get_resources(port, 1, VPCMIDO_TENANT, "bgps",
            midonet_api_mtypes[APPLICATION_COLLECTION_BGP_JSON],
            midonet_api_mtypes[APPLICATION_BGP_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves an array of pointers to midonet object representing bgps (MN5).
 * This call does not update and/or access midocache.
 * @param router [in] router of interest.
 * @param outnames [out] an array of pointers to midonet objects representing router port bgps, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bgps_v5(midoname *router, midoname ***outnames, int *outnames_max) {
    return (mido_get_resources(router, 1, VPCMIDO_TENANT, "bgp_peers",
            midonet_api_mtypes[APPLICATION_COLLECTION_BGP_PEER_JSON],
            midonet_api_mtypes[APPLICATION_BGP_PEER_JSON],
            outnames, outnames_max));
}

/**
 * Creates a bgp route on dev (bgp MN v1.9, or router MN v5.0) using
 * the parameters in the argument.
 * @param dev [in] device to create the bgp route
 * @param nwPrefix [in] the network address to advertise
 * @param prefixLength [in] the length of the network to advertise
 * @param outname [i/o] pointer to an extant MidoNet object (parameters will be checked
 * to avoid duplicate bgp route creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created bgp route will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_bgp_route(midoname *dev, char *nwPrefix, char *prefixLength, midoname **outname) {
    if (is_midonet_api_v1()) {
        return (mido_create_bgp_route_v1(dev, nwPrefix, prefixLength, outname));
    } else if (is_midonet_api_v5()) {
        return (mido_create_bgp_route_v5(dev, nwPrefix, prefixLength, outname));
    } else {
        return (1);
    }
}

/**
 * Creates a bgp route on bgp using the parameters in the argument (MN1.9).
 * @param bgp [in] bgp to create the bgp route
 * @param nwPrefix [in] the network address to advertise
 * @param prefixLength [in] the length of the network to advertise
 * @param outname [i/o] pointer to an extant MidoNet object (parameters will be checked
 * to avoid duplicate bgp route creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created bgp route will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_bgp_route_v1(midoname *bgp, char *nwPrefix, char *prefixLength, midoname **outname) {
    int rc = 0, found = 0, ret = 0;
    midoname myname;

    midoname *out = NULL;
    
    if (bgp == NULL)  {
        LOGWARN("Invalid argument: cannot create bgp route for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    midoname **bgp_routes = NULL;
    int max_bgp_routes = 0;
    mido_get_bgp_routes(bgp, &bgp_routes, &max_bgp_routes);
    found = 0;
    for (int i = 0; i < max_bgp_routes && !found; i++) {
        char *nw = NULL;
        char *len = NULL;
        mido_getel_midoname(bgp_routes[i], "nwPrefix", &nw);
        mido_getel_midoname(bgp_routes[i], "prefixLength", &len);
        if (nw && len) {
            if (!strcmp(nw, nwPrefix) && !strcmp(len, prefixLength)) {
                found = 1;
                out = bgp_routes[i];
            }
        }
        EUCA_FREE(nw);
        EUCA_FREE(len);
    }
    EUCA_FREE(bgp_routes);
    if (found) {
        LOGTRACE("bgp route already in mido - abort create\n");
        if (outname) {
            *outname = out;
        }
        return (0);
    }

    if (!found) {
        memset(&myname, 0, sizeof (midoname));
        myname.tenant = strdup(VPCMIDO_TENANT);
        myname.resource_type = strdup("ad_routes");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_AD_ROUTE_JSON]);

        rc = mido_create_resource(bgp, 1, &myname, &out, "nwPrefix", nwPrefix,
                "prefixLength", prefixLength, NULL);

        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }

    return (ret);
}

/**
 * Creates a bgp route on router using the parameters in the argument (MN5.0).
 * @param router [in] router to create the bgp route
 * @param nwPrefix [in] the network address to advertise
 * @param prefixLength [in] the length of the network to advertise
 * @param outname [i/o] pointer to an extant MidoNet object (parameters will be checked
 * to avoid duplicate bgp route creation. If outname points to NULL, a newly allocated
 * midoname structure representing the newly created bgp route will be returned.
 * If outname is NULL, the newly created object will not be returned.
 * @return 0 if the route is successfully created/found. 1 otherwise.
 */
int mido_create_bgp_route_v5(midoname *router, char *nwPrefix, char *prefixLength, midoname **outname) {
    int rc = 0, found = 0, ret = 0;
    midoname myname;

    midoname *out = NULL;
    
    if (router == NULL)  {
        LOGWARN("Invalid argument: cannot create bgp network for NULL\n");
        return (1);
    }
    if (outname && *outname) {
        out = *outname;
    }

    midoname **bgp_routes = NULL;
    int max_bgp_routes = 0;
    mido_get_bgp_routes(router, &bgp_routes, &max_bgp_routes);
    found = 0;
    for (int i = 0; i < max_bgp_routes && !found; i++) {
        char *nw = NULL;
        char *len = NULL;
        mido_getel_midoname(bgp_routes[i], "subnetAddress", &nw);
        mido_getel_midoname(bgp_routes[i], "subnetLength", &len);
        if (nw && len) {
            if (!strcmp(nw, nwPrefix) && !strcmp(len, prefixLength)) {
                found = 1;
                out = bgp_routes[i];
            }
        }
        EUCA_FREE(nw);
        EUCA_FREE(len);
    }
    EUCA_FREE(bgp_routes);
    if (found) {
        LOGTRACE("bgp network already in mido - abort create\n");
        if (outname) {
            *outname = out;
        }
        return (0);
    }

    if (!found) {
        memset(&myname, 0, sizeof (midoname));
        myname.tenant = strdup(VPCMIDO_TENANT);
        myname.resource_type = strdup("bgp_networks");
        myname.media_type = strdup(midonet_api_mtypes[APPLICATION_BGP_NETWORK_JSON]);

        rc = mido_create_resource(router, 1, &myname, &out, "subnetAddress", nwPrefix,
                "subnetLength", prefixLength, NULL);

        if (rc == 0) {
            if (outname) {
                *outname = out;
            }
            ret = 0;
        } else if (rc < 0) {
            ret = 0;
        } else {
            ret = 1;
        }
        mido_free_midoname(&myname);
    }

    return (ret);
}

/**
 * Retrieves an array of pointers to midonet object representing routes of a bgp.
 * This call does not update and/or access midocache.
 * @param dev [in] device (bgp MN1.9, or router MN5) of interest.
 * @param outnames [out] an array of pointers to midonet objects representing routes of a bgp, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bgp_routes(midoname *dev, midoname ***outnames, int *outnames_max) {
    if (is_midonet_api_v1()) {
        return (mido_get_bgp_routes_v1(dev, outnames, outnames_max));
    } else if (is_midonet_api_v5()) {
        return (mido_get_bgp_routes_v5(dev, outnames, outnames_max));
    } else {
        return (1);
    }
}

/**
 * Retrieves an array of pointers to midonet object representing routes of a bgp (MN1.9).
 * This call does not update and/or access midocache.
 * @param bgp [in] bgp of interest.
 * @param outnames [out] an array of pointers to midonet objects representing routes of a bgp, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bgp_routes_v1(midoname *bgp, midoname ***outnames, int *outnames_max) {
    return (mido_get_resources(bgp, 1, VPCMIDO_TENANT, "ad_routes",
            midonet_api_mtypes[APPLICATION_COLLECTION_AD_ROUTE_JSON],
            midonet_api_mtypes[APPLICATION_AD_ROUTE_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves an array of pointers to midonet object representing routes of a bgp (MN5).
 * This call does not update and/or access midocache.
 * @param router [in] router of interest.
 * @param outnames [out] an array of pointers to midonet objects representing routes of a bgp, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bgp_routes_v5(midoname *router, midoname ***outnames, int *outnames_max) {
    return (mido_get_resources(router, 1, VPCMIDO_TENANT, "bgp_networks",
            midonet_api_mtypes[APPLICATION_COLLECTION_BGP_NETWORK_JSON],
            midonet_api_mtypes[APPLICATION_BGP_NETWORK_JSON],
            outnames, outnames_max));
}

/**
 * Compares json_object one and two. Specifically, checks whether all elements in
 * one are present (and match) in two. 
 * @param one json_object of interest
 * @param two json_object of interest
 * @return 0 if all elements of one have a corresponding element in two. 1 otherwise.
 */
int json_object_cmp(json_object *one, json_object *two) {
    int onetype = 0, twotype = 0, onesubtype = 0, twosubtype = 0, rc = 0, ret = 0, i = 0;
    char *oneel = NULL, *twoel = NULL;
    json_object *twoval = NULL;

    if (!one && !two) {
        LOGTRACE("both are null\n");
        return (0);
    } else if ((one && !two) || (!one && two)) {
        LOGTRACE("one is null\n");
        return (1);
    }

    LOGTRACE("one=%s\n", json_object_to_json_string(one));
    LOGTRACE("two=%s\n", json_object_to_json_string(two));
    onetype = json_object_get_type(one);
    twotype = json_object_get_type(two);
    if (onetype != twotype) {
        LOGTRACE("types differ\n");
        return(1);
    }

    if (onetype == json_type_object) {
        json_object_object_foreach(one, onekey, oneval) {
            LOGTRACE("evaluating key %s\n", onekey);
            onesubtype = json_object_get_type(oneval);
            //            twoval = json_object_object_get(two, onekey);
            json_object_object_get_ex(two, onekey, &twoval);
            twosubtype = json_object_get_type(twoval);
            if ((onesubtype != twosubtype) && ((onesubtype == json_type_object) ||
                    (twosubtype == json_type_object) || (onesubtype == json_type_array) ||
                    (twosubtype == json_type_array))) {
                rc = 1;
            } else if (onesubtype == json_type_object) {
                // recurse
                rc = json_object_cmp(oneval, twoval);
            } else if (onesubtype == json_type_array) {
                for (i = 0; i < json_object_array_length(oneval) && !rc; i++) {
                    rc = json_object_cmp(json_object_array_get_idx(oneval, i), json_object_array_get_idx(twoval, i));
                }
            } else {
                oneel = strdup(SP(json_object_get_string(oneval)));
                twoel = strdup(SP(json_object_get_string(twoval)));
                LOGTRACE("strcmp: %s/%s\n", oneel, twoel);
                rc = strcmp(oneel, twoel);
                EUCA_FREE(oneel);
                EUCA_FREE(twoel);
                if (rc != 0) {
                    ret = 1;
                    break;
                }
            }
            //            json_object_put(twoval);
            if (rc) {
                ret = 1;
            }
        }
    } else {
        LOGTRACE("comparing strings: one=%s two=%s\n", json_object_to_json_string(one), json_object_to_json_string(two));
        ret = strcmp(json_object_to_json_string(one), json_object_to_json_string(two));
    }

    LOGTRACE("result of cmp: %d\n", ret);
    return (ret);
}

/**
 * Checks if a NULL terminated list of variable arguments (consisting of key-value
 * pairs) have corresponding entries in midoname data structure (jsonbuf).
 * @param name midoname data structure of interest.
 * @param ... NULL terminated variable argument section
 * @return 0 if all key-value pairs are found in name. 1 otherwise.
 */
int mido_cmp_midoname_to_input_json(midoname *name, ...) {
    va_list al = { {0} };
    int ret = 0;

    va_start(al, name);
    ret = mido_cmp_midoname_to_input_json_v(name, &al);
    va_end(al);
    return (ret);
}

/**
 * Checks if a NULL terminated list of variable arguments (consisting of key-value
 * pairs) have corresponding entries in midoname data structure (jsonbuf).
 * @param name midoname data structure of interest.
 * @param al variable argument list that specifies a rule. This list is copied in
 * this function.
 * @return 0 if all key-value pairs are found in name. 1 otherwise.
 */
 int mido_cmp_midoname_to_input_json_v(midoname *name, va_list *al) {
    va_list ala = { {0} };
    char *jsonbuf = NULL;
    int ret = 0;

    va_copy(ala, *al);
    jsonbuf = mido_jsonize(NULL, &ala);
    va_end(ala);

    LOGTRACE("new=%s\n", SP(jsonbuf));
    LOGTRACE("old=%s\n", SP(name->jsonbuf));

    if (jsonbuf && name->jsonbuf) {
        ret = mido_cmp_jsons(jsonbuf, name->jsonbuf, name->resource_type);
    } else {
        ret = 1;
    }

    EUCA_FREE(jsonbuf);
    return (ret);
}

/**
 * Compares 2 json strings that represent mido objects. All jsonsrc elements needs
 * a matching element in jsondst (jsondst may have more elements).
 * @param jsonsrc a string containing a json that represents a mido object.
 * @param jsondst a string containing a json that represents another mido object.
 * @param type type of mido object.
 * @return 0 if all elements in jsonsrc has a matching element in jsondst. 1 otherwise.
 */
int mido_cmp_jsons(char *jsonsrc, char *jsondst, char *type) {
    json_object *srcjobj = NULL;
    json_object *dstjobj = NULL;
    int ret = 0;

    if (!jsonsrc && !jsondst) {
        return (0);
    }
    if (!jsonsrc || !jsondst) {
        return (1);
    }

    dstjobj = json_tokener_parse(jsondst);
    srcjobj = json_tokener_parse(jsonsrc);

    // special case el removal
    if (!strcmp(type, "rules")) {
        // for chain rules, remove the position element
        json_object_object_del(srcjobj, "position");
        json_object_object_del(dstjobj, "position");
    }

    if (json_object_cmp(srcjobj, dstjobj)) {
        ret = 1;
    } else {
        ret = 0;
    }

    if (srcjobj) {
        json_object_put(srcjobj);
    }
    if (dstjobj) {
        json_object_put(dstjobj);
    }
    return (ret);
}

/**
 * Compares midoname data structures a and b. The comparison are based on jsonbuf
 * of a and b. jsonbuf elements position, id, uri are ignored in the comparison (since
 * they should be always disfferent).
 * @param a midoname structure of interest.
 * @param b midoname structute of interest.
 * @return 0 if all elements in a->jsonbuf has a matching element in b->jsonbuf. 1 otherwise.
 * The elements position, id, and uri are ignored in the comparison. 0 is also returned if
 * both a and b are NULL.
 */
int mido_cmp_midoname_jsonbuf(midoname *a, midoname *b) {
    int ret = 0;
    if (a == b) {
        return 0;
    }
    if ((a == NULL) || (b == NULL)) {
        return 1;
    }

    json_object *srcjobj = json_tokener_parse(a->jsonbuf);
    json_object *dstjobj = json_tokener_parse(b->jsonbuf);

    json_object_object_del(srcjobj, "position");
    json_object_object_del(srcjobj, "id");
    json_object_object_del(srcjobj, "uri");

    if (json_object_cmp(srcjobj, dstjobj)) {
        ret = 1;
    } else {
        ret = 0;
    }

    if (srcjobj) {
        json_object_put(srcjobj);
    }
    if (dstjobj) {
        json_object_put(dstjobj);
    }
    return (ret);
}

/**
 * Checks if a NULL terminated list of variable arguments (consisting of key-value
 * pairs) have corresponding entries in midoname data structure (jsonbuf).
 * @param name midoname data structure of interest.
 * @param ... NULL terminated variable argument section
 * @return 0 if all key-value pairs are found in name. 1 otherwise.
 */
int mido_cmp_midoname_to_input(midoname *name, ...) {
    va_list al = { {0} };
    int rc = 0;
    char *key = NULL, *dstval = NULL, *srcval = NULL;

    va_start(al, name);

    key = va_arg(al, char *);
    if (key)
        dstval = va_arg(al, char *);
    while (key && dstval) {
        rc = mido_getel_midoname(name, key, &srcval);
        if (!rc) {
            if (strcmp(dstval, srcval)) {
                EUCA_FREE(srcval);
                va_end(al);
                return (1);
            }
            EUCA_FREE(srcval);
        } else if (rc && !strcmp(dstval, "UNSET")) {
            // skip
        } else {
            EUCA_FREE(srcval);
            va_end(al);
            return (1);
        }
        key = va_arg(al, char *);
        if (key)
            dstval = va_arg(al, char *);
    }

    va_end(al);
    LOGTRACE("RESOURCE ALREADY IN PLACE: %s\n", SP(name->uuid));
    return (0);
}

/**
 * Retrieves an array of pointers to midonet object representing routers.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing routers, to be returned
 * @param outnames_max [out] number of retrieved routers.
 * @return 0 on success. 1 otherwise.
 */
int mido_get_routers(char *tenant, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_router **routers = midocache->routers;
        if ((routers != NULL) && (midocache->max_routers > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_routers, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_routers; i++) {
                if (routers[i] == NULL) {
                    continue;
                }
                names[count] = routers[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(NULL, 0, tenant, "routers",
            midonet_api_mtypes[APPLICATION_COLLECTION_ROUTER_JSON],
            midonet_api_mtypes[APPLICATION_ROUTER_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves a midonet object that represents the router in the argument from midocache.
 * @param name [in] name of the MidoNet router of interest.
 * @return pointer to the data structure that represents the router, when found. NULL otherwise.
 */
midonet_api_router *mido_get_router(char *name) {
    if (midocache != NULL) {
        if (midocache->sorted_routers == 0) {
            qsort(midocache->routers, midocache->max_routers,
                    sizeof (midonet_api_router *), compare_midonet_api_router);
            midocache->sorted_routers = 1;
        }
        midoname tmp;
        tmp.name = strdup(name);
        midonet_api_router router;
        router.obj = &tmp;
        midonet_api_router *prouter = &router;
        midonet_api_router **res = (midonet_api_router **) bsearch(&prouter,
                midocache->routers, midocache->max_routers,
                sizeof (midonet_api_router *), compare_midonet_api_router);
        EUCA_FREE(tmp.name);
        if (res) {
            return (*res);
        }
    }
    return (NULL);
}

/**
 * Retrieves an array of pointers to midonet object representing bridges.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing bridges, to be returned
 * @param outnames_max [out] number of retrieved bridges.
 * @return 0 on success. 1 otherwise.
 */
int mido_get_bridges(char *tenant, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_bridge **bridges = midocache->bridges;
        if ((bridges != NULL) && (midocache->max_bridges > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_bridges, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_bridges; i++) {
                if (bridges[i] == NULL) {
                    continue;
                }
                names[count] = bridges[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(NULL, 0, tenant, "bridges",
            midonet_api_mtypes[APPLICATION_COLLECTION_BRIDGE_JSON],
            midonet_api_mtypes[APPLICATION_BRIDGE_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves a midonet object that represents the bridge in the argument from midocache.
 * @param name [in] name of the MidoNet bridge of interest.
 * @return pointer to the data structure that represents the bridge, when found. NULL otherwise.
 */
midonet_api_bridge *mido_get_bridge(char *name) {
    if (midocache != NULL) {
        if (midocache->sorted_bridges == 0) {
            qsort(midocache->bridges, midocache->max_bridges,
                    sizeof (midonet_api_bridge *), compare_midonet_api_bridge);
            midocache->sorted_bridges = 1;
        }
        midoname tmp;
        tmp.name = strdup(name);
        midonet_api_bridge bridge;
        bridge.obj = &tmp;
        midonet_api_bridge *pbridge = &bridge;
        midonet_api_bridge **res = (midonet_api_bridge **) bsearch(&pbridge,
                midocache->bridges, midocache->max_bridges,
                sizeof (midonet_api_bridge *), compare_midonet_api_bridge);
        EUCA_FREE(tmp.name);
        if (res) {
            return (*res);
        }
    }

    return (NULL);
}

/**
 * Retrieves an array of pointers to midonet object representing chains.
 * @param tenant [in] name of the MidoNet tenant.
 * @param outnames [out] an array of pointers to midonet objects representing chains, to be returned
 * @param outnames_max [out] number of retrieved chains.
 * @return 0 on success. 1 otherwise.
 */
int mido_get_chains(char *tenant, midoname ***outnames, int *outnames_max) {
    int count = 0;
    if (midocache != NULL) {
        *outnames = NULL;
        *outnames_max = 0;
        midonet_api_chain **chains = midocache->chains;
        if ((chains != NULL) && (midocache->max_chains > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_chains, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_chains; i++) {
                if (chains[i] == NULL) {
                    continue;
                }
                names[count] = chains[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }
    return (mido_get_resources(NULL, 0, tenant, "chains",
            midonet_api_mtypes[APPLICATION_COLLECTION_CHAIN_JSON],
            midonet_api_mtypes[APPLICATION_CHAIN_JSON],
            outnames, outnames_max));
}

/**
 * Retrieves a midonet object that represents the chain in the argument from midocache.
 * @param name [in] name of the MidoNet chain of interest.
 * @return pointer to the data structure that represents the chain, when found. NULL otherwise.
 */
midonet_api_chain *mido_get_chain(char *name) {
    if (midocache != NULL) {
        if (midocache->sorted_chains == 0) {
            qsort(midocache->chains, midocache->max_chains,
                    sizeof (midonet_api_chain *), compare_midonet_api_chain);
            midocache->sorted_chains = 1;
        }
        midoname tmp;
        tmp.name = strdup(name);
        midonet_api_chain ch;
        ch.obj = &tmp;
        midonet_api_chain *pch = &ch;
        midonet_api_chain **res = (midonet_api_chain **) bsearch(&pch,
                midocache->chains, midocache->max_chains,
                sizeof (midonet_api_chain *), compare_midonet_api_chain);
        EUCA_FREE(tmp.name);
        if (res) {
            return (*res);
        }
    }
    return (NULL);
}

/**
 * Retrieves objects from MidoNet-API.
 * @param parents [in] pointer to an optional array of parent MidoNet objects.
 * @param max_parents [in] number of parents.
 * @param tenant [in] MidoNet tenant string.
 * @param resource_type [in] type of the resource of interest (e.g., routers, bridges, etc)
 * @param apistr [in] string to be appended on REST call header (typically the MidoNet collections media type)
 * @param mtype [in] media type of expected individual mido objects (MidoNet media type).
 * @param outnames [out] array of pointers to midoname structures of the retrieved objects.
 * @param outnames_max [out] number of retrieved objects.
 * @return 0 on success, 1 otherwise.
 */
int mido_get_resources(midoname *parents, int max_parents, char *tenant, char *resource_type,
        char *apistr, char *mtype, midoname ***outnames, int *outnames_max) {
    int rc = 0, ret = 0, i = 0;
    char *payload = NULL, url[EUCA_MAX_PATH], tmpbuf[EUCA_MAX_PATH];
    midoname **names = NULL;
    int names_max = 0;

    *outnames = NULL;
    *outnames_max = 0;

    bzero(url, EUCA_MAX_PATH);
    if (!parents) {
        snprintf(url, EUCA_MAX_PATH, "%s/%s?tenant_id=%s", midonet_api_uribase, resource_type, tenant);
    } else {
        snprintf(url, EUCA_MAX_PATH, "%s/", midonet_api_uribase);
        for (i = 0; i < max_parents; i++) {
            bzero(tmpbuf, EUCA_MAX_PATH);
            snprintf(tmpbuf, EUCA_MAX_PATH, "%s/%s/", parents[i].resource_type, parents[i].uuid);
            strcat(url, tmpbuf);
        }
        bzero(tmpbuf, EUCA_MAX_PATH);
        snprintf(tmpbuf, EUCA_MAX_PATH, "%s?tenant_id=%s", resource_type, tenant);
        strcat(url, tmpbuf);
    }

    rc = midonet_http_get(url, apistr, &payload);
    if (!rc) {
        struct json_object *jobj = NULL, *resource = NULL;

        jobj = json_tokener_parse(payload);
        if (!jobj) {
            LOGWARN("cannot tokenize midonet response: check midonet health\n");
        } else {
            if (json_object_is_type(jobj, json_type_array)) {
                names_max = 0;
                midoname_list_get_midonames(midocache_midos, &names, json_object_array_length(jobj));

                for (i = 0; i < json_object_array_length(jobj); i++) {
                    resource = json_object_array_get_idx(jobj, i);
                    if (resource) {
                        names[names_max]->tenant = strdup(tenant);
                        names[names_max]->jsonbuf = strdup(json_object_to_json_string(resource));
                        names[names_max]->resource_type = strdup(resource_type);
                        names[names_max]->init = 1;
                        if (mtype && strlen(mtype)) {
                            names[names_max]->media_type = strdup(mtype);
                        }
                        mido_update_midoname(names[names_max]);
                        names_max++;
                    }
                }
            }
            json_object_put(jobj);
        }
        EUCA_FREE(payload);
    }

    if (names && (names_max > 0)) {
        *outnames = names;
        *outnames_max = names_max;
    }
    return (ret);
}

/**
 * Retrieves an MidoNet object from MidoNet-API to refresh the in-memory data.
 * @param resc [in] pointer to a MidoNet object to be refreshed.
 * @return 0 on success, 1 otherwise.
 */
int mido_refresh_resource(midoname *resc) {
    int rc=0;
    char hbuf[EUCA_MAX_PATH];
    
    if (!resc) {
        LOGWARN("Invalid argument: cannot refresh NULL object.\n");
        return (1);
    }
    
    char *payload = NULL;
    char *url = NULL;
    url = resc->uri;
    
    hbuf[0] = '\0';
    if (!resc->media_type || strlen(resc->media_type) <= 0) {
        snprintf(hbuf, EUCA_MAX_PATH, "application/json");
    } else {
        snprintf(hbuf, EUCA_MAX_PATH, "%s", resc->media_type);
    }
    LOGTRACE("\t refreshing %s %s\n", url, hbuf);

    rc = midonet_http_get(url, hbuf, &payload);
    if (!rc) {
        if (payload && (strlen(payload))) {
            EUCA_FREE(resc->jsonbuf);
            resc->jsonbuf = strdup(payload);
            resc->init = 1;
            rc = mido_update_midoname(resc);
        }
    } else {
        LOGWARN("failed to refresh %s\n", resc->name);
    }
    EUCA_FREE(payload);

    return (rc);
}

/**
 * Retrieves an array of pointers to midonet object representing hosts.
 * @param outnames [out] an array of pointers to midonet objects representing hosts, to be returned
 * @param outnames_max [out] number of elements in the outnames array
 * @return 0 on success. 1 otherwise.
 */
int mido_get_hosts(midoname ***outnames, int *outnames_max) {
    int rc = 0, ret = 0, i = 0, hostup = 0;
    char *payload = NULL, url[EUCA_MAX_PATH];
    int count = 0;
    midoname **names = NULL;
    int names_max = 0;

    *outnames = NULL;
    *outnames_max = 0;

    if (midocache != NULL) {
        midonet_api_host **hosts = midocache->hosts;
        if ((hosts != NULL) && (midocache->max_hosts > 0)) {
            *outnames = EUCA_ZALLOC_C(midocache->max_hosts, sizeof (midoname *));
            midoname **names = *outnames;
            for (int i = 0; i < midocache->max_hosts; i++) {
                if (hosts[i] == NULL) {
                    continue;
                }
                names[count] = hosts[i]->obj;
                count++;
            }
            *outnames_max = count;
        }
        return (0);
    }

    bzero(url, EUCA_MAX_PATH);
    snprintf(url, EUCA_MAX_PATH, "%s/hosts", midonet_api_uribase);
    rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_COLLECTION_HOST_JSON], &payload);
    if (!rc) {
        struct json_object *jobj = NULL, *host = NULL, *el = NULL;

        jobj = json_tokener_parse(payload);
        if (!jobj) {
            LOGWARN("cannot tokenize midonet response: check midonet health\n");
        } else {
            if (json_object_is_type(jobj, json_type_array)) {
                names_max = 0;
                midoname_list_get_midonames(midocache_midos, &names, json_object_array_length(jobj));
                for (i = 0; i < json_object_array_length(jobj); i++) {
                    host = json_object_array_get_idx(jobj, i);
                    if (host) {
                        json_object_object_get_ex(host, "alive", &el);
                        if (el) {
                            if (!strcmp(json_object_get_string(el), "false")) {
                                // host is down, skip
                                hostup = 0;
                            } else {
                                hostup = 1;
                            }
                        }
                        if (hostup) {
                            names[names_max]->jsonbuf = strdup(json_object_to_json_string(host));
                            json_object_object_get_ex(host, "id", &el);
                            if (el) {
                                names[names_max]->uuid = strdup(json_object_get_string(el));
                            }
                            json_object_object_get_ex(host, "name", &el);
                            if (el) {
                                names[names_max]->name = strdup(json_object_get_string(el));
                            }

                            names[names_max]->resource_type = strdup("hosts");
                            names[names_max]->media_type = strdup(midonet_api_mtypes[APPLICATION_HOST_JSON]);
                            names[names_max]->init = 1;
                            names_max++;
                        }
                    }
                }
            }
            json_object_put(jobj);
        }
        EUCA_FREE(payload);
    }

    if (names && (names_max > 0)) {
        *outnames = names;
        *outnames_max = names_max;
    }
    return (ret);
}

/**
 * Retrieves a midonet object that represents the host in the argument from midocache.
 * The search prioritizes name over uuid. name or uuid can be NULL, but not both.
 * @param name [in] name of the MidoNet host of interest.
 * @param uuid [in] uuid of the MidoNet host of interest.
 * @return pointer to the data structure that represents the host, when found. NULL otherwise.
 */
midonet_api_host *mido_get_host(char *name, char *uuid) {
    midoname tmp;
    midonet_api_host *res = NULL;
    bzero(&tmp, sizeof (midoname));
    if (midocache != NULL) {
        if (name) {
            tmp.name = strdup(name);
        }
        if (uuid) {
            tmp.uuid = strdup(uuid);
        }
        res = midonet_api_cache_lookup_host(&tmp);
        EUCA_FREE(tmp.name);
        EUCA_FREE(tmp.uuid);
        return (res);
    }
    return (NULL);
}

/**
 * Retrieves a midonet object that represents the host in the argument from midocache.
 * @param hostname [in] hostname of the MidoNet host of interest.
 * @return pointer to the data structure that represents the host, when found. NULL otherwise.
 */
midonet_api_host *mido_get_host_byname(char *hostname) {
    char *ip = NULL;
    midonet_api_host *res = NULL;
    if (euca_getaddr(hostname, &ip)) {
        res = mido_get_host_byip(ip);
    }
    EUCA_FREE(ip);
    return (res);
}

/**
 * Retrieves a midonet object that represents the host in the argument from midocache.
 * @param ip [in] IP address of the MidoNet host of interest.
 * @return pointer to the data structure that represents the host, when found. NULL otherwise.
 */
midonet_api_host *mido_get_host_byip(char *ip) {
    u32 int_ip;

    if (!ip) {
        LOGWARN("Invalid argument: cannot retrieve NULL host.\n");
        return (NULL);
    }
    int_ip = dot2hex(ip);
    if (midocache != NULL) {
        midonet_api_iphostmap *iphm = &(midocache->iphostmap);
        if (iphm->sorted == 0) {
            qsort(iphm->entries, iphm->max_entries, sizeof (midonet_api_iphostmap_entry), compare_midonet_api_iphostmap_entry);
            iphm->sorted = 1;
        }
        midonet_api_iphostmap_entry key;
        key.host = NULL;
        key.ip = int_ip;
        midonet_api_iphostmap_entry *res = NULL;
        res = (midonet_api_iphostmap_entry *) bsearch(&key, iphm->entries, iphm->max_entries,
                sizeof (midonet_api_iphostmap_entry), compare_midonet_api_iphostmap_entry);
        if (res) {
            return (res->host);
        }
    }
    return (NULL);
}

/**
 * Retrieves interfaces of the specified host as detected by midolman.
 *
 * @param host [in] host of interest.
 * @param iftype [in] interface type to search and return.
 * @param outnames [out] array of interfaces of the host.
 * @param outnames_max [out] number of interfaces.
 *
 * @return 0 on success. 1 otherwise.
 *
 * @note memory allocated for outnames must be released by the caller. The caller
 *       is also required to release prior memory allocations to outnames before
 *       calling this function.
 */
int mido_get_interfaces(midoname *host, u32 iftype, u32 ifendpoint, midoname **outnames, int *outnames_max) {
    int rc = 0, ret = 0, i = 0, getif = 0;
    char *payload = NULL, url[EUCA_MAX_PATH];
    midoname *names = NULL;
    int names_max = 0;

    if ((host == NULL) || (host->init == 0) || (host->uuid == NULL)) {
        LOGWARN("Invalid argument: invalid MidoNet host.\n");
        return (1);
    }
    *outnames = NULL;
    *outnames_max = 0;

    bzero(url, EUCA_MAX_PATH);
    snprintf(url, EUCA_MAX_PATH, "%s/hosts/%s/interfaces", midonet_api_uribase, host->uuid);
    rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_COLLECTION_INTERFACE_JSON], &payload);
    if (!rc) {
        struct json_object *jobj = NULL, *interface = NULL, *el = NULL;

        jobj = json_tokener_parse(payload);
        if (!jobj) {
            LOGWARN("cannot tokenize midonet response: check midonet health\n");
        } else {
            if (json_object_is_type(jobj, json_type_array)) {
                names_max = 0;
                names = EUCA_ZALLOC_C(json_object_array_length(jobj), sizeof (midoname));
                for (i = 0; i < json_object_array_length(jobj); i++) {
                    interface = json_object_array_get_idx(jobj, i);
                    if (interface) {
                        if (iftype == MIDO_HOST_INTERFACE_ALL) {
                            getif = 1;
                        } else {
                            getif = 0;
                        }
                        json_object_object_get_ex(interface, "type", &el);
                        if (el) {
                            const char *tmp = NULL;
                            tmp = json_object_get_string(el);
                            if ((!getif) && (iftype & MIDO_HOST_INTERFACE_PHYSICAL) && (!strcmp(tmp, "Physical"))) {
                                getif = 1;
                            }
                            if ((!getif) && (iftype & MIDO_HOST_INTERFACE_VIRTUAL) && (!strcmp(tmp, "Virtual"))) {
                                getif = 1;
                            }
                            if ((!getif) && (iftype & MIDO_HOST_INTERFACE_TUNNEL) && (!strcmp(tmp, "Tunnel"))) {
                                getif = 1;
                            }
                            if ((!getif) && (iftype & MIDO_HOST_INTERFACE_UNKNOWN) && (!strcmp(tmp, "Unknown"))) {
                                getif = 1;
                            }
                        }
                        if (getif) {
                            if (ifendpoint == MIDO_HOST_INTERFACE_ENDPOINT_ALL) {
                                getif = 1;
                            } else {
                                getif = 0;
                            }
                            json_object_object_get_ex(interface, "endpoint", &el);
                            if (el) {
                                const char *tmp = json_object_get_string(el);
                                if ((!getif) && (ifendpoint & MIDO_HOST_INTERFACE_ENDPOINT_PHYSICAL) && (!strcmp(tmp, "PHYSICAL"))) {
                                    getif = 1;
                                }
                                if ((!getif) && (ifendpoint & MIDO_HOST_INTERFACE_ENDPOINT_DATAPAH) && (!strcmp(tmp, "DATAPATH"))) {
                                    getif = 1;
                                }
                                if ((!getif) && (ifendpoint & MIDO_HOST_INTERFACE_ENDPOINT_LOCALHOST) && (!strcmp(tmp, "LOCALHOST"))) {
                                    getif = 1;
                                }
                                if ((!getif) && (ifendpoint & MIDO_HOST_INTERFACE_ENDPOINT_UNKNOWN) && (!strcmp(tmp, "UNKNOWN"))) {
                                    getif = 1;
                                }
                            }
                        }

                        if (getif) {
                            names[names_max].jsonbuf = strdup(json_object_to_json_string(interface));
                            json_object_object_get_ex(interface, "id", &el);
                            if (el) {
                                names[names_max].uuid = strdup(json_object_get_string(el));
                            }
                            json_object_object_get_ex(interface, "name", &el);
                            if (el) {
                                names[names_max].name = strdup(json_object_get_string(el));
                            }
                            names[names_max].resource_type = strdup("interfaces");
                            names[names_max].media_type = strdup(midonet_api_mtypes[APPLICATION_INTERFACE_JSON]);
                            names[names_max].init = 1;
                            names_max++;
                        }
                    }
                }
            }
            json_object_put(jobj);
        }
        EUCA_FREE(payload);
    }

    if (names && (names_max > 0)) {
        *outnames = EUCA_ZALLOC_C(names_max, sizeof(midoname));
        memcpy(*outnames, names, sizeof(midoname) *names_max);
        *outnames_max = names_max;
    }
    EUCA_FREE(names);

    return (ret);
}

/**
 * Retrieves the interface dev of the specified host as detected by midolman.
 *
 * @param host [in] host of interest.
 * @param dev [in] interface of interest.
 * @param outname [out] information about the interface if found.
 * @return 0 on success. 1 otherwise.
 */
int mido_get_interface(midoname *host, char *dev, midoname *outname) {
    int rc = 0, ret = 0;
    char *payload = NULL, url[EUCA_MAX_PATH];

    if ((host == NULL) || (host->init == 0) || (host->uuid == NULL) || (!outname)) {
        LOGWARN("Invalid argument: cannot get interface from NULL host.\n");
        return (1);
    }

    memset(url, 0, EUCA_MAX_PATH);
    memset(outname, 0, sizeof(midoname));
    snprintf(url, EUCA_MAX_PATH, "%s/hosts/%s/interfaces/%s", midonet_api_uribase, host->uuid, dev);
    rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_INTERFACE_JSON], &payload);
    if (!rc) {
        struct json_object *jobj = NULL, *el = NULL;

        jobj = json_tokener_parse(payload);
        if (!jobj) {
            LOGWARN("cannot tokenize midonet response: check midonet health\n");
        } else {
            if (json_object_is_type(jobj, json_type_object)) {
                outname->jsonbuf = strdup(payload);
                json_object_object_get_ex(jobj, "uri", &el);
                if (el) {
                    outname->uuid = strdup(json_object_get_string(el));
                }
                json_object_object_get_ex(jobj, "name", &el);
                if (el) {
                    outname->name = strdup(json_object_get_string(el));
                }
                outname->resource_type = strdup("interfaces");
                outname->media_type = strdup(midonet_api_mtypes[APPLICATION_INTERFACE_JSON]);
                outname->init = 1;
            }
            json_object_put(jobj);
        }
        EUCA_FREE(payload);
    } else {
        ret++;
    }

    return (ret);
}

/**
 * Retrieves the IPv4 addresses of the specified host as detected by midolman.
 *
 * @param host [in] host of interest.
 * @param outnames [out] array of IPv4 addresses of the host.
 * @param outnames_max [out] number of addresses.
 *
 * @return 0 on success. 1 otherwise.
 *
 * @note memory allocated for outnames must be released by the caller. The caller
 *       is also required to release prior memory allocations to outnames before
 *       calling this function.
 */
int mido_get_addresses(midoname *host, u32 **outnames, int *outnames_max) {
    int rc = 0, ret = 0;
    int i = 0;
    midoname *names = NULL;
    int names_max = 0;
    char **addrs;
    int max_addrs;
    u32 *hips = NULL;

    if ((host == NULL) || (host->init == 0) || (host->uuid == NULL)) {
        LOGWARN("Invalid argument: invalid MidoNet host.\n");
        return (1);
    }
    LOGTRACE("retrieving IPv4 addresses of %s\n", host->name);
    rc = mido_get_interfaces(host, MIDO_HOST_INTERFACE_ALL,
            (MIDO_HOST_INTERFACE_ENDPOINT_PHYSICAL | MIDO_HOST_INTERFACE_ENDPOINT_UNKNOWN |
            MIDO_HOST_INTERFACE_ENDPOINT_DATAPAH), &names, &names_max);

    if ((rc == 0) && (names != NULL) && (names_max > 0)) {
        *outnames_max = 0;
        for (i = 0; i < names_max; i++) {
            rc = mido_getarr_midoname(&(names[i]), "addresses", &addrs, &max_addrs);
            LOGTRACE("%s %s - max_addrs: %d\n", host->name, names[i].name, max_addrs);
            if ((rc == 0) && (max_addrs > 0)) {
                hips = EUCA_REALLOC_C(hips, *outnames_max + max_addrs, sizeof (u32));
                bzero(&(hips[*outnames_max]), max_addrs * sizeof (u32));
                for (int j = 0; j < max_addrs; j++) {
                    if (strlen(addrs[j]) > 16) {
                        LOGTRACE("\tskipping %s - not an IPv4 address.\n", addrs[j]);
                    } else {
                        hips[*outnames_max] = dot2hex(addrs[j]);
                        if ((hips[*outnames_max] & 0xa9fe0000) == 0xa9fe0000) {
                            LOGTRACE("\tskipping %s - link local address\n", addrs[j]);
                            hips[*outnames_max] = 0;
                        } else {
                            LOGTRACE("\tFound %s\n", addrs[j]);
                            (*outnames_max)++;
                        }
                    }
                    EUCA_FREE(addrs[j]);
                }
                EUCA_FREE(addrs);
            }
        }
        *outnames = hips;
    }

    mido_free_midoname_list(names, names_max);
    EUCA_FREE(names);

    return (ret);
}

/**
 * Retrieves the IPv4 address of device dev of the specified host as detected by midolman.
 *
 * @param host [in] host of interest.
 * @param outnames [out] array of IPv4 addresses of the host.
 * @param outnames_max [out] number of addresses.
 *
 * @return 0 on success. 1 otherwise.
 *
 * @note memory allocated for outnames must be released by the caller. The caller
 *       is also required to release prior memory allocations to outnames before
 *       calling this function.
 */
int mido_get_address(midoname *host, char *dev, u32 *outaddress) {
    int rc = 0, ret = 0;
    char **addrs;
    int max_addrs = 0;
    midoname devmn;

    if ((host == NULL) || (host->init == 0) || (host->uuid == NULL)) {
        LOGWARN("Invalid argument: unable to retrieve address from NULL host.\n");
        return (1);
    }
    *outaddress = 0;
    rc = mido_get_interface(host, dev, &devmn);
    if (rc == 0) {
        rc = mido_getarr_midoname(&devmn, "addresses", &addrs, &max_addrs);
        LOGTRACE("%s %s - max_addrs: %d\n", host->name, devmn.name, max_addrs);
        if ((rc == 0) && (max_addrs > 0)) {
            for (int i = 0; i < max_addrs; i++) {
                if (strlen(addrs[i]) > 16) {
                    LOGTRACE("\tskipping %s - not an IPv4 address.\n", addrs[i]);
                } else {
                    *outaddress = dot2hex(addrs[i]);
                }
                EUCA_FREE(addrs[i]);
            }
        }
        EUCA_FREE(addrs);
    } else {
        ret++;
    }

    mido_free_midoname(&devmn);

    return (ret);
}

/**
 * Invokes the MidoNet system-state API to make sure that eucanetd can access MidoNet APIs.
 * @return 0 on success. 1 otherwise.
 */
int mido_check_state(void) {
    int rc = 0;
    char url[EUCA_MAX_PATH];
    char *outbuf = NULL;
    
    bzero(url, EUCA_MAX_PATH);
    snprintf(url, EUCA_MAX_PATH, "%s/system_state", midonet_api_uribase);

    rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_SYSTEM_STATE_JSON], &outbuf);
    EUCA_FREE(outbuf);
    return (rc);
}

/**
 * Retrieves the current MidoNet API base URL.
 * @return the current MidoNet API base URL.
 */
char *mido_get_apiuribase(void) {
    return (midonet_api_uribase);
}

/**
 * Copies the contents of apiuribase to eucanetd VPCMIDO driver configuration.
 * @param apiuribase [in] the MidoNet API base URL to be set.
 */
void mido_set_apiuribase(const char *apiuribase) {
    if (apiuribase) {
        euca_strncpy(midonet_api_uribase, apiuribase, URI_LEN);
    }
}

/**
 * Invokes the MidoNet system-state API and extracts the MidoNet-API base uri, and
 * MidoNet-API version..
 * 3 base urls are attempted: user provided apiruibase configuration, the default
 * MN1.9 baseuri (localhost:8080), and the default MN5 baseuri (localhost:8181).
 * @return 0 on success. 1 otherwise.
 */
int mido_initialize_apiuribase(void) {
    int rc = 1;
    char url[URI_LEN] = { 0 };
    char *outbuf = NULL;
    int max_att = 100;

    if (strlen(midonet_api_uribase)) {
        snprintf(url, URI_LEN, "%s/", midonet_api_uribase);
        midonet_api_uribase[0] = '\0';
        for (int i = 0; i < max_att && rc; i++) {
            rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_JSON], &outbuf);
            if (rc) {
                sleep(5);
            }
        }
    } else {
        midonet_api_uribase[0] = '\0';
        for (int i = 0; i < max_att && rc; i++) {
            snprintf(url, URI_LEN, "%s/", MIDONET_API_BASE_URL_8080);
            rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_JSON], &outbuf);
            if (rc) {
                snprintf(url, URI_LEN, "%s/", MIDONET_API_BASE_URL_8181);
                rc = midonet_http_get(url, midonet_api_mtypes[APPLICATION_JSON], &outbuf);
                if (rc) {
                    sleep(5);
                }
            }
        }
    }

    if (!rc && outbuf && strlen(outbuf)) {
        json_object *el = NULL;
        json_object *jobj = json_tokener_parse(outbuf);
        if (jobj) {
            json_object_object_get_ex(jobj, "uri", &el);
            if (el) {
                snprintf(midonet_api_uribase, URI_LEN, "%s", json_object_get_string(el));
            }
            el = NULL;
            json_object_object_get_ex(jobj, "version", &el);
            if (el) {
                snprintf(midonet_api_version, 16, "%s", json_object_get_string(el));
            }
            json_object_put(jobj);
        }
    }

    int uribaselen = strlen(midonet_api_uribase);
    if (uribaselen) {
        if (midonet_api_uribase[uribaselen - 1] == '/') {
            midonet_api_uribase[uribaselen - 1] = '\0';
        }
    } else {
        rc++;
    }
    if (!strlen(midonet_api_version)) {
        snprintf(midonet_api_version, 16, "unknown");
    }
    EUCA_FREE(outbuf);
    return (rc);
}

/**
 * Creates an empty midoname_list structure.
 *
 * @return pointer to the newly created midoname_list structure or NULL in case of failure.
 */
midoname_list *midoname_list_new(void) {
    midoname_list *res = NULL;

    LOGTRACE("Creating a new midoname_list.\n");
    res = EUCA_ZALLOC_C(1, sizeof (midoname_list));
    return (res);
}

/**
 * Releases resources used by the midoname_list structure in the argument.
 *
 * @param midoname_list [in] structure of interest.
 * @return 0 on success. 1 otherwise.
 * 
 * @note The list is assumed to be created using midoname_list_new()
 */
int midoname_list_free(midoname_list *list) {
    int j = 0;
    LOGTRACE("Releasing midoname_list at %p\n", list);
    if (list == NULL) {
        return (0);
    }
    for (int i = 0; i < list->size; i++) {
        mido_free_midoname(list->mnames[i]);
    }
    while (j < list->size) {
        EUCA_FREE(list->mnames[j]);
        j = j + MIDONAME_LIST_CAPACITY_STEP;
    }
    EUCA_FREE(list->mnames);
    EUCA_FREE(list);
    return (0);
}

/**
 * Returns a free midoname element from the midoname_list in the argument.
 *
 * @param midoname_list [in] structure of interest.
 * @return pointer to an unused midoname structure. NULL on any failure.
 *
 * @note The list is assumed to be created using midoname_list_new()
 */
midoname *midoname_list_get_midoname(midoname_list *list) {
    midoname *res = NULL;
    midoname *mnarr = NULL;
    pthread_mutex_lock(&mido_buffer_mutex);
    LOGTRACE("Retrieving an unused midoname element %d/%d\n", list->size, list->capacity);
    if (list->size == list->capacity) {
        list->mnames = EUCA_REALLOC_C(list->mnames, list->capacity + MIDONAME_LIST_CAPACITY_STEP, sizeof (midoname *));
        mnarr = EUCA_ZALLOC_C(MIDONAME_LIST_CAPACITY_STEP, sizeof (midoname));
        list->capacity += MIDONAME_LIST_CAPACITY_STEP;
        for (int i = 0; i < MIDONAME_LIST_CAPACITY_STEP; i++) {
            list->mnames[list->size + i] = &(mnarr[i]);
        }
    }
    res = list->mnames[list->size];
    (list->size)++;
    pthread_mutex_unlock(&mido_buffer_mutex);
    return res;
}

/**
 * Retrieve a list of free midoname elements from the midoname_list in the argument.
 * 
 * @param list [in] midoname_list structure of interest.
 * @param outnames [out] pointer to an array of midoname pointers. Each pointer points
 * to a valid and free midoname data structure.
 * @param max_outnames [in] number of requested elements.
 * @return 0 when successful. 1 otherwise. 
 */
int midoname_list_get_midonames(midoname_list *list, midoname ***outnames, int max_outnames) {
    if (outnames == NULL) {
        LOGWARN("Invalid argument: cannot return midoname list to a NULL pointer.\n");
        return (1);
    }
    if (max_outnames <= 0) {
        // Nothing to do
        return (1);
    }
    *outnames = EUCA_ZALLOC_C(max_outnames, sizeof (midoname *));
    for (int i = 0; i < max_outnames; i++) {
        midoname **res = *outnames;
        res[i] = midoname_list_get_midoname(list);
        if (res[i] == NULL) {
            LOGWARN("Failed to retrieve a free midoname.\n");
            return (1);
        }
    }
    return (0);
}

/**
 * Initializes the midonet-api cache data structure.
 * 
 * @return pointer to a newly created and initialized midonet_api_cache data structure.
 * NULL if unable to allocate resources.
 */
midonet_api_cache *midonet_api_cache_init(void) {
    midonet_api_cache *cache = EUCA_ZALLOC_C(1, sizeof (midonet_api_cache));
    midonet_api_cache_midos_init();
    return (cache);
}

/**
 * Initializes the midonet-api cache midos list.
 * @return pointer to the midonet-api cache midos list.
 */
midoname_list *midonet_api_cache_midos_init(void) {
    if (midocache_midos == NULL) {
        midocache_midos = midoname_list_new();
    }
    return (midocache_midos);
}

/**
 * Gets the pointer to the midonet-api cache midos list.
 * @return pointer to the midonet-api cache midos list.
 */
midoname_list *midonet_api_cache_midos_get(void) {
    return (midocache_midos);
}

/**
 * Releases resources allocated for the midonet_api_cache cache data structure.
 * 
 * @param [in] cache midonet_api_cache data structure of interest.
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_flush(midonet_api_cache *cache) {
    int i;

    if (cache == NULL) {
        if (midocache != NULL) {
            cache = midocache;
        } else {
            return (0);
        }
    }
    
    EUCA_FREE(cache->ports);

    for (i = 0; i < cache->max_routers; i++) {
        midonet_api_router_free(cache->routers[i]);
    }
    EUCA_FREE(cache->routers);

    for (i = 0; i < cache->max_bridges; i++) {
        midonet_api_bridge_free(cache->bridges[i]);
    }
    EUCA_FREE(cache->bridges);

    for (i = 0; i < cache->max_chains; i++) {
        midonet_api_chain_free(cache->chains[i]);
    }
    EUCA_FREE(cache->chains);

    for (i = 0; i < cache->max_hosts; i++) {
        midonet_api_host_free(cache->hosts[i]);
    }
    EUCA_FREE(cache->hosts);

    for (i = 0; i < cache->max_ipaddrgroups; i++) {
        midonet_api_ipaddrgroup_free(cache->ipaddrgroups[i]);
    }
    EUCA_FREE(cache->ipaddrgroups);

    for (i = 0; i < cache->max_portgroups; i++) {
        midonet_api_portgroup_free(cache->portgroups[i]);
    }
    EUCA_FREE(cache->portgroups);

    for (i = 0; i < cache->max_tunnelzones; i++) {
        midonet_api_tunnelzone_free(cache->tunnelzones[i]);
    }
    EUCA_FREE(cache->tunnelzones);
    
    if (cache->iphostmap.entries) {
        EUCA_FREE(cache->iphostmap.entries);
    }

    if (midocache == cache) {
        midocache = NULL;
        midoname_list_free(midocache_midos);
        midocache_midos = NULL;
        midonet_api_cache_midos_init();
    }
    EUCA_FREE(cache);
    
    return (0);
}

/**
 * Returns a pointer to the current MIDOCACHE.
 * 
 * @return a pointer to the current MIDOCACHE.
 */
midonet_api_cache *midonet_api_cache_get(void) {
    return (midocache);
}

/**
 * Raises the midocache invalid flag if the number of midocache_midos releases reaches
 * a threshold.
 * 
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_check(void) {
    if ((midocache_midos != NULL) && (midocache_midos->released > MIDONAME_LIST_RELEASES_B4INVALIDATE)) {
        midocache_invalid = 1;
    } else {
        // System seems idle - release libcurl handles
        midonet_api_cleanup();
    }
    return (0);
}

/**
 * Populates the midonet_api_cache data structure. If midocache is already populated,
 * this is a no-op.
 * 
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_populate(void) {
    if ((midocache_midos != NULL) && (midocache_midos->released > MIDONAME_LIST_RELEASES_B4INVALIDATE)) {
        midonet_api_cache_flush(NULL);
    }
    if (midocache == NULL) {
        return (midonet_api_cache_refresh());
    }
    return (0);
}

/**
 * Clear the current midocache and populates the midonet_api_cache data structure.
 * 
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_refresh(void) {
    return (midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_ALL));
}

/**
 * Clear the current midocache and populates the midonet_api_cache data structure.
 * @param refreshmode [in] specify whether to populate hosts (MIDO_CACHE_REFRESH_ALL)
 * or not (MIDO_CACHE_REFRESH_NOHOSTS).
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_refresh_v(enum mido_cache_refresh_mode_t refreshmode) {
    int rc = 0;
    int ret = 0;
    int i = 0;
    int j = 0;
    int k = 0;
    midonet_api_cache *cache = NULL;
    //midoname ***names = NULL;
    //int *max_names = NULL;
    midoname **l1names = NULL;
    int max_l1names = 0;
    midoname **l2names = NULL;
    int max_l2names = 0;
    struct timeval tv = {0};

    // Disable global midonet_api cache
    if (midocache != NULL) {
        midonet_api_cache_flush(NULL);
        EUCA_FREE(midocache);
        midocache = NULL;
    }
    cache = midonet_api_cache_init();

    int mnapiok = 0;
    while (!mnapiok) {
        rc = mido_check_state();
        if (rc) {
            if (sig_rcvd) {
                sig_rcvd = 0;
                break;
            } else {
                sleep(1);
            }
        } else {
            mnapiok = 1;
        }
    }
    if (!mnapiok) {
        LOGERROR("Unable to access midonet-api.\n");
        EUCA_FREE(cache);
        return (1);
    }

    eucanetd_timer_usec(&tv);

    // get all routers
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_routers(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->routers = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_router *));
        midonet_api_router *router = NULL;
        for (i = 0; i < max_l1names; i++) {
            router = EUCA_ZALLOC_C(1, sizeof (midonet_api_router));
            cache->routers[i] = router;
            router->obj = l1names[i];
            LOGEXTREME("Cached router %s\n", router->obj->name);
            rc = mido_get_ports(router->obj, &(router->ports), &(router->max_ports));
            if (!rc) {
                cache->ports = EUCA_REALLOC_C(cache->ports, cache->max_ports + router->max_ports, sizeof (midoname *));
                for (j = 0; j < router->max_ports; j++) {
                    cache->ports[cache->max_ports + j] = router->ports[j];
                }
                cache->max_ports += router->max_ports;
            }
            for (j = 0; j < router->max_ports && !rc; j++) {
                LOGEXTREME("\tCached port %s\n", router->ports[j]->uuid);
            }

            rc = mido_get_routes(router->obj, &(router->routes), &(router->max_routes));
            for (j = 0; j < router->max_routes && !rc; j++) {
                LOGEXTREME("\tCached route %s\n", router->routes[j]->uuid);
            }
        }
        cache->max_routers = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido routers\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\trouters in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    // get all bridges
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_bridges(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->bridges = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_bridge *));
        midonet_api_bridge *bridge = NULL;
        for (i = 0; i < max_l1names; i++) {
            bridge = EUCA_ZALLOC_C(1, sizeof (midonet_api_bridge));
            cache->bridges[i] = bridge;
            bridge->obj = l1names[i];
            LOGEXTREME("Cached bridge %s\n", bridge->obj->name);
            rc = mido_get_ports(bridge->obj, &(bridge->ports), &(bridge->max_ports));
            if (!rc) {
                cache->ports = EUCA_REALLOC_C(cache->ports, cache->max_ports + bridge->max_ports, sizeof (midoname *));
                for (j = 0; j < bridge->max_ports; j++) {
                    cache->ports[cache->max_ports + j] = bridge->ports[j];
                }
                cache->max_ports += bridge->max_ports;
            }
            for (j = 0; j < bridge->max_ports && !rc; j++) {
                LOGEXTREME("\tCached port %s\n", bridge->ports[j]->uuid);
            }
            
            l2names = NULL;
            max_l2names = 0;
            rc = mido_get_dhcps(bridge->obj, &l2names, &max_l2names);
            if (!rc && l2names) {
                bridge->dhcps = EUCA_ZALLOC_C(max_l2names, sizeof (midonet_api_dhcp *));
                midonet_api_dhcp *dhcp = NULL;
                for (j = 0; j < max_l2names; j++) {
                    dhcp = EUCA_ZALLOC_C(1, sizeof (midonet_api_dhcp));
                    bridge->dhcps[j] = dhcp;
                    dhcp->obj = l2names[j];
                    LOGEXTREME("\tCached bridge %s dhcp %s\n", bridge->obj->name, dhcp->obj->uuid);
                    rc = mido_get_dhcphosts(bridge->obj, dhcp->obj,
                            &(dhcp->dhcphosts), &(dhcp->max_dhcphosts));
                    for (k = 0; k < dhcp->max_dhcphosts && !rc; k++) {
                        LOGEXTREME("\t\tCached dhcphost %s\n", dhcp->dhcphosts[k]->uuid);
                    }
                }
                bridge->max_dhcps = max_l2names;
                EUCA_FREE(l2names);
            }
        }
        cache->max_bridges = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido bridges\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tbridges in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    LOGTRACE("\t%d ports\n", cache->max_ports);

    // get all chains
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_chains(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->chains = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_chain *));
        midonet_api_chain *chain = NULL;
        for (i = 0; i < max_l1names; i++) {
            chain = EUCA_ZALLOC_C(1, sizeof (midonet_api_chain));
            cache->chains[i] = chain;
            chain->obj = l1names[i];
            LOGEXTREME("Cached chain %s\n", chain->obj->name);
            rc = mido_get_rules(chain->obj, &(chain->rules), &(chain->max_rules));
            chain->rules_count = chain->max_rules;
            for (j = 0; j < chain->max_rules && !rc; j++) {
                LOGEXTREME("\tCached rule %s\n", chain->rules[j]->uuid);
            }
        }
        cache->max_chains = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido chains\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tchains in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    // get all hosts
    if (refreshmode == MIDO_CACHE_REFRESH_ALL) {
        rc = midonet_api_cache_iphostmap_populate(cache);
        if (rc) {
            LOGWARN("failed to populate mido ip-host map\n");
        }
    }
    LOGTRACE("\tiphostmap in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);
    
    // get all IP address groups
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_ipaddrgroups(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->ipaddrgroups = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_ipaddrgroup *));
        midonet_api_ipaddrgroup *ipaddrgroup = NULL;
        for (i = 0; i < max_l1names; i++) {
            ipaddrgroup = EUCA_ZALLOC_C(1, sizeof (midonet_api_ipaddrgroup));
            cache->ipaddrgroups[i] = ipaddrgroup;
            ipaddrgroup->obj = l1names[i];
            LOGEXTREME("Cached ip-address-group %s\n", ipaddrgroup->obj->name);
            rc = mido_get_ipaddrgroup_ips(ipaddrgroup->obj, &(ipaddrgroup->ips), &(ipaddrgroup->max_ips));
            ipaddrgroup->ips_count = ipaddrgroup->max_ips;
            ipaddrgroup->hexips = EUCA_REALLOC_C(ipaddrgroup->hexips, ipaddrgroup->max_ips, sizeof (u32));
            for (j = 0; j < ipaddrgroup->max_ips && !rc; j++) {
                LOGEXTREME("\tCached IP %s\n", ipaddrgroup->ips[j]->uuid);
                if (!ipaddrgroup->ips[j]->ipagip || !ipaddrgroup->ips[j]->ipagip->ip || !strlen(ipaddrgroup->ips[j]->ipagip->ip)) {
                    LOGWARN("failed to retrieve IP address for %s\n", ipaddrgroup->obj->name);
                } else {
                    ipaddrgroup->hexips[j] = dot2hex(ipaddrgroup->ips[j]->ipagip->ip);
                }
            }
        }
        cache->max_ipaddrgroups = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido ip-address-groups\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tipag in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);
    
    // get all port-groups
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_portgroups(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->portgroups = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_portgroup *));
        midonet_api_portgroup *portgroup = NULL;
        for (i = 0; i < max_l1names; i++) {
            portgroup = EUCA_ZALLOC_C(1, sizeof (midonet_api_portgroup));
            cache->portgroups[i] = portgroup;
            portgroup->obj = l1names[i];
            LOGEXTREME("Cached port-group %s\n", portgroup->obj->name);
            rc = mido_get_portgroup_ports(portgroup->obj, &(portgroup->ports), &(portgroup->max_ports));
            for (j = 0; j < portgroup->max_ports && !rc; j++) {
                LOGEXTREME("\tCached port %s\n", portgroup->ports[j]->uuid);
            }
        }
        cache->max_portgroups = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido port-groups\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tpgs in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    // get all tunnel-zones
    midonet_api_cache_refresh_tunnelzones(cache);
    LOGTRACE("\ttzs in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    // Enable midocache
    midocache = cache;

    return (ret);
}

/**
 * Reloads router routes from MidoNet.
 * @param cache [in] midonet_api_cache of interest
 * @param start [in] start index of interest.
 * @param end [in] end index of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int midonet_api_cache_refresh_routerroutes(midonet_api_cache *cache, int start, int end) {
    int rc = 0;
    if (cache == NULL) {
        return (1);
    }
    if ((start < 0) || (end > cache->max_routers)) {
        return (1);
    }
    for (int i = start; i < end; i++) {
        midonet_api_router *router = cache->routers[i];
        if (router == NULL) {
            continue;
        }

        rc = mido_get_ports(router->obj, &(router->ports), &(router->max_ports));
        if (!rc) {
            pthread_mutex_lock(&mido_cache_ports_mutex);
            cache->ports = EUCA_REALLOC_C(cache->ports, cache->max_ports + router->max_ports, sizeof (midoname *));
            for (int j = 0; j < router->max_ports; j++) {
                cache->ports[cache->max_ports + j] = router->ports[j];
            }
            cache->max_ports += router->max_ports;
            pthread_mutex_unlock(&mido_cache_ports_mutex);
            for (int j = 0; j < router->max_ports && !rc; j++) {
                LOGEXTREME("\tCached port %s\n", router->ports[j]->uuid);
            }
        } else {
            LOGWARN("\tFailed to retrieve %s ports\n", router->obj->name);
        }

        rc = mido_get_routes(router->obj, &(router->routes), &(router->max_routes));
        if (!rc) {
            for (int j = 0; j < router->max_routes && !rc; j++) {
                LOGEXTREME("\tCached route %s\n", router->routes[j]->uuid);
            }
        } else {
            LOGWARN("\tFailed to retrieve %s routes\n", router->obj->name);
        }
    }
    return (0);
}

/**
 * Reloads bridge dhcps from MidoNet.
 * @param cache [in] midonet_api_cache of interest
 * @param start [in] start index of interest.
 * @param end [in] end index of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int midonet_api_cache_refresh_bridgedhcps(midonet_api_cache *cache, int start, int end) {
    int rc = 0;
    if (cache == NULL) {
        return (1);
    }
    if ((start < 0) || (end > cache->max_bridges)) {
        return (1);
    }
    for (int i = start; i < end; i++) {
        midonet_api_bridge *bridge = cache->bridges[i];
        if (bridge == NULL) {
            continue;
        }

        rc = mido_get_ports(bridge->obj, &(bridge->ports), &(bridge->max_ports));
        if (!rc) {
            pthread_mutex_lock(&mido_cache_ports_mutex);
            cache->ports = EUCA_REALLOC_C(cache->ports, cache->max_ports + bridge->max_ports, sizeof (midoname *));
            for (int j = 0; j < bridge->max_ports; j++) {
                cache->ports[cache->max_ports + j] = bridge->ports[j];
            }
            cache->max_ports += bridge->max_ports;
            pthread_mutex_unlock(&mido_cache_ports_mutex);
            for (int j = 0; j < bridge->max_ports && !rc; j++) {
                LOGEXTREME("\tCached port %s\n", bridge->ports[j]->uuid);
            }
        } else {
            LOGWARN("\tFailed to retrieve %s ports\n", bridge->obj->name);
        }

        midoname **l2names = NULL;
        int max_l2names = 0;
        rc = mido_get_dhcps(bridge->obj, &l2names, &max_l2names);
        if (!rc && l2names) {
            bridge->dhcps = EUCA_ZALLOC_C(max_l2names, sizeof (midonet_api_dhcp *));
            midonet_api_dhcp *dhcp = NULL;
            for (int j = 0; j < max_l2names; j++) {
                dhcp = EUCA_ZALLOC_C(1, sizeof (midonet_api_dhcp));
                bridge->dhcps[j] = dhcp;
                dhcp->obj = l2names[j];
                LOGEXTREME("\tCached bridge %s dhcp %s\n", bridge->obj->name, dhcp->obj->uuid);
                rc = mido_get_dhcphosts(bridge->obj, dhcp->obj,
                        &(dhcp->dhcphosts), &(dhcp->max_dhcphosts));
                if (!rc) {
                    for (int k = 0; k < dhcp->max_dhcphosts && !rc; k++) {
                        LOGEXTREME("\t\tCached dhcphost %s\n", dhcp->dhcphosts[k]->uuid);
                    }
                } else {
                    LOGWARN("\t\tFailed to retrieve %s dhcphosts\n", dhcp->obj->name);
                }
            }
            bridge->max_dhcps = max_l2names;
            EUCA_FREE(l2names);
        } else {
            if (rc) {
                LOGWARN("\tFailed to retrieve %s dhcps\n", bridge->obj->name);
            }
        }
    }
    return (0);
}

/**
 * Reloads chain rules from MidoNet.
 * @param cache [in] midonet_api_cache of interest
 * @param start [in] start index of interest.
 * @param end [in] end index of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int midonet_api_cache_refresh_chainrules(midonet_api_cache *cache, int start, int end) {
    if (cache == NULL) {
        return (1);
    }
    if ((start < 0) || (end > cache->max_chains)) {
        return (1);
    }
    for (int i = start; i < end; i++) {
        midonet_api_chain *chain = cache->chains[i];
        if (chain == NULL) {
            continue;
        }
        int rc = mido_get_rules(chain->obj, &(chain->rules), &(chain->max_rules));
        if (!rc) {
            chain->rules_count = chain->max_rules;
            for (int j = 0; j < chain->max_rules && !rc; j++) {
                LOGEXTREME("\tCached rule %s\n", chain->rules[j]->uuid);
            }
        } else {
            LOGWARN("\tFailed to retrieve %s rules\n", chain->obj->name);
        }
    }
    return (0);
}

/**
 * Reloads ip-address-group ips from MidoNet.
 * @param cache [in] midonet_api_cache of interest
 * @param start [in] start index of interest.
 * @param end [in] end index of interest.
 * @return 0 on success. Positive integer otherwise.
 */
int midonet_api_cache_refresh_ipagips(midonet_api_cache *cache, int start, int end) {
    if (cache == NULL) {
        return (1);
    }
    if ((start < 0) || (end > cache->max_ipaddrgroups)) {
        return (1);
    }

    for (int i = start; i < end; i++) {
        midonet_api_ipaddrgroup *ipaddrgroup = cache->ipaddrgroups[i];
        if (ipaddrgroup == NULL) {
            continue;
        }
        int rc = mido_get_ipaddrgroup_ips(ipaddrgroup->obj, &(ipaddrgroup->ips), &(ipaddrgroup->max_ips));
        if (!rc) {
            ipaddrgroup->ips_count = ipaddrgroup->max_ips;
            ipaddrgroup->hexips = EUCA_REALLOC_C(ipaddrgroup->hexips, ipaddrgroup->max_ips, sizeof (u32));
            for (int j = 0; j < ipaddrgroup->max_ips && !rc; j++) {
                LOGEXTREME("\tCached IP %s\n", ipaddrgroup->ips[j]->uuid);
                if (!ipaddrgroup->ips[j]->ipagip || !ipaddrgroup->ips[j]->ipagip->ip || !strlen(ipaddrgroup->ips[j]->ipagip->ip)) {
                    LOGWARN("failed to retrieve IP address for %s\n", ipaddrgroup->obj->name);
                } else {
                    ipaddrgroup->hexips[j] = dot2hex(ipaddrgroup->ips[j]->ipagip->ip);
                }
            }
        } else {
            LOGWARN("\tFailed to retrieve %s ips\n", ipaddrgroup->obj->name);
        }
    }
    return (0);
}

/**
 * Thread to reload chain rules from MidoNet. MIDOCACHE chains are assumed to be pre-populated.
 * Affected chains starts at index tparam->start and ends at index tparam->end.
 * @param tparam [in] parameters to be used.
 * @return NULL
 */
void *midonet_api_cache_refresh_objects_worker_thread(void *worker_param) {
    struct timeval tv;
    eucanetd_timer_usec(&tv);
    if (worker_param == NULL) {
        pthread_exit(NULL);
    }
    mido_cache_worker_thread_params *tp = (mido_cache_worker_thread_params *) worker_param;
    midonet_api_cache *cache = tp->cache;
    if (!cache) {
        tp->rc = 1;
    } else {
        if (!tp->get_from_mido) {
            tp->rc = 1;
        } else {
            tp->rc = tp->get_from_mido(tp->cache, tp->start, tp->end);
        }
    }
    LOGTRACE("\t\t%s worker thread - %.2f ms\n", tp->name, eucanetd_timer_usec(&tv) / 1000.0);
    pthread_exit((void *) &(tp->rc));
}

/**
 * Main thread responsible to load objects from MidoNet.
 * @param main_params [in] mido_cache_main_thread_params structure, which specifies what to do.
 * @return NULL
 */
void *midonet_api_cache_refresh_objects_main_thread(void *main_param) {
    struct timeval tv;
    eucanetd_timer_usec(&tv);
    if (main_param == NULL) {
        pthread_exit(NULL);
    }
    int rc = 0;
    mido_cache_main_thread_params *param = (mido_cache_main_thread_params *) main_param;
    int n = param->n;
    if (!param->cache || !param->get_from_mido) {
        LOGWARN("invalid argument: cannot start NULL threads\n");
        param->rc = 1;
        pthread_exit((void *) &(param->rc));
    }

    if (n > MIDONET_API_USE_THREADS_THRESHOLD) {
        mido_cache_worker_thread_params *tparams = prep_thread_params(n, MIDONET_API_RELOAD_THREADS);
        for (int i = 0; i < MIDONET_API_RELOAD_THREADS; i++) {
            tparams[i].cache = param->cache;
            tparams[i].get_from_mido = param->get_from_mido;
            snprintf(tparams[i].name, MIDO_CACHE_THREAD_NAME_LEN, "%s", param->name);
        }
        pthread_t cthreads[MIDONET_API_RELOAD_THREADS - 1];
        pthread_attr_t ptattr;
        pthread_attr_init(&ptattr);
        pthread_attr_setdetachstate(&ptattr, PTHREAD_CREATE_JOINABLE);
        for (int i = 0; i < (MIDONET_API_RELOAD_THREADS - 1); i++) {
            rc += pthread_create(&cthreads[i], &ptattr, midonet_api_cache_refresh_objects_worker_thread, (void *) &(tparams[i]));
        }
        rc += param->get_from_mido(tparams[MIDONET_API_RELOAD_THREADS - 1].cache,
                tparams[MIDONET_API_RELOAD_THREADS - 1].start, tparams[MIDONET_API_RELOAD_THREADS - 1].end);
        for (int i = 0; i < (MIDONET_API_RELOAD_THREADS - 1); i++) {
            rc += pthread_join(cthreads[i], NULL);
        }

        EUCA_FREE(tparams);
        pthread_attr_destroy(&ptattr);
    } else {
        rc += param->get_from_mido(param->cache, 0, n);
    }

    LOGTRACE("\t\t%s main thread - %.2f ms\n", param->name, eucanetd_timer_usec(&tv) / 1000.0);
    param->rc = 0;
    pthread_exit((void *) &(param->rc));
}

/**
 * Clears the current midocache tunnelzone entries and reloads tunnelzones from MidoNet.
 * @param cache [in] midonet_api_cache of interest (where tunnelzones will be [re]populated)
 * @return 0 on success. 1 on any error.
 */
int midonet_api_cache_refresh_tunnelzones(midonet_api_cache *cache) {
    int rc = 0;
    midoname **l1names = NULL;
    int max_l1names = 0;
    midonet_api_cache *midocache_bak;

    if (!cache) {
        cache = midonet_api_cache_get();
    }

    // Flush cache->hosts
    for (int i = 0; i < cache->max_tunnelzones; i++) {
        midonet_api_tunnelzone_free(cache->tunnelzones[i]);
    }
    EUCA_FREE(cache->tunnelzones);

    // disable midocache (load all tunnelzones from MidoNet)
    midocache_bak = midocache;
    midocache = NULL;

    rc = mido_get_tunnelzones(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->tunnelzones = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_tunnelzone *));
        midonet_api_tunnelzone *tunnelzone = NULL;
        for (int i = 0; i < max_l1names; i++) {
            tunnelzone = EUCA_ZALLOC_C(1, sizeof (midonet_api_tunnelzone));
            cache->tunnelzones[i] = tunnelzone;
            tunnelzone->obj = l1names[i];
            LOGEXTREME("Cached tunnel-zone %s\n", tunnelzone->obj->name);
            rc = mido_get_tunnelzone_hosts(tunnelzone->obj, &(tunnelzone->hosts), &(tunnelzone->max_hosts));
            for (int j = 0; j < tunnelzone->max_hosts && !rc; j++) {
                LOGEXTREME("\tCached tz host %s\n", tunnelzone->hosts[j]->uuid);
            }
        }
        cache->max_tunnelzones = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido tunnel-zones\n");
        }
    }
    EUCA_FREE(l1names);

    // recover midocache state
    midocache = midocache_bak;
    return (0);
}

/**
 * Clears the current midocache host entries and reloads hosts from MidoNet.
 * @param cache [in] midonet_api_cache of interest (where hosts will be [re]populated)
 * @return 0 on success. 1 on any error.
 */
int midonet_api_cache_refresh_hosts(midonet_api_cache *cache) {
    int rc = 0;
    midoname **l1names = NULL;
    int max_l1names = 0;
    midonet_api_cache *midocache_bak;
    
    if (!cache) {
        cache = midonet_api_cache_get();
    }

    // Flush cache->hosts
    for (int i = 0; i < cache->max_hosts; i++) {
        midonet_api_host_free(cache->hosts[i]);
    }
    EUCA_FREE(cache->hosts);

    // get all hosts
    // disable midocache (load all hosts from MidoNet
    midocache_bak = midocache;
    midocache = NULL;

    rc = mido_get_hosts(&l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->hosts = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_host *));
        midonet_api_host *host = NULL;
        for (int i = 0; i < max_l1names; i++) {
            host = EUCA_ZALLOC_C(1, sizeof (midonet_api_host));
            cache->hosts[i] = host;
            host->obj = l1names[i];
            LOGEXTREME("Cached host %s\n", host->obj->name);
            rc = mido_get_addresses(host->obj, &(host->addresses), &(host->max_addresses));
            if (!rc) {
                for (int j = 0; j < host->max_addresses && !rc; j++) {
                    LOGEXTREME("\tCached address %u\n", host->addresses[j]);
                }
            }
        }
        cache->max_hosts = max_l1names;
    }
    EUCA_FREE(l1names);
    // recover midocache state
    midocache = midocache_bak;
    return (0);
}

/**
 * Clear the current midocache and populates the midonet_api_cache data structure.
 * @param refreshmode [in] specify whether to populate hosts (MIDO_CACHE_REFRESH_ALL)
 * or not (MIDO_CACHE_REFRESH_NOHOSTS).
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_refresh_v_threads(enum mido_cache_refresh_mode_t refreshmode) {
    int rc = 0;
    int ret = 0;
    int i = 0;
    int j = 0;
    midonet_api_cache *cache = NULL;
    midoname **l1names = NULL;
    int max_l1names = 0;
    struct timeval tv = {0};
    mido_cache_main_thread_params param[MIDO_CACHE_THREAD_END];
    pthread_t pt[MIDO_CACHE_THREAD_END];
    pthread_attr_t ptattr;

    midonet_api_cleanup();
    midonet_api_init();

    pthread_attr_init(&ptattr);
    pthread_attr_setdetachstate(&ptattr, PTHREAD_CREATE_JOINABLE);

    // Disable global midonet_api cache
    if (midocache != NULL) {
        midonet_api_cache_flush(NULL);
        EUCA_FREE(midocache);
        midocache = NULL;
    }
    cache = midonet_api_cache_init();

    int mnapiok = 0;
    while (!mnapiok) {
        rc = mido_check_state();
        if (rc) {
            if (sig_rcvd) {
                sig_rcvd = 0;
                break;
            } else {
                sleep(1);
            }
        } else {
            mnapiok = 1;
        }
    }
    if (!mnapiok) {
        LOGERROR("Unable to access midonet-api.\n");
        EUCA_FREE(cache);
        return (1);
    }

    eucanetd_timer_usec(&tv);

    // get all routers
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_routers(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->routers = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_router *));
        midonet_api_router *router = NULL;
        for (i = 0; i < max_l1names; i++) {
            router = EUCA_ZALLOC_C(1, sizeof (midonet_api_router));
            cache->routers[i] = router;
            router->obj = l1names[i];
            LOGEXTREME("Cached router %s\n", router->obj->name);
        }
        cache->max_routers = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido routers\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\trouters in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    snprintf(param[MIDO_CACHE_THREAD_ROUTER].name, MIDO_CACHE_THREAD_NAME_LEN, "router");
    param[MIDO_CACHE_THREAD_ROUTER].n = cache->max_routers;
    param[MIDO_CACHE_THREAD_ROUTER].get_from_mido = midonet_api_cache_refresh_routerroutes;
    param[MIDO_CACHE_THREAD_ROUTER].cache = cache;
    rc = pthread_create(&pt[MIDO_CACHE_THREAD_ROUTER], &ptattr, midonet_api_cache_refresh_objects_main_thread,
            (void *) &param[MIDO_CACHE_THREAD_ROUTER]);
    //pthread_join(pt[MIDO_CACHE_THREAD_ROUTER], NULL);

    // get all bridges
    eucanetd_timer_usec(&tv);
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_bridges(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->bridges = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_bridge *));
        midonet_api_bridge *bridge = NULL;
        for (i = 0; i < max_l1names; i++) {
            bridge = EUCA_ZALLOC_C(1, sizeof (midonet_api_bridge));
            cache->bridges[i] = bridge;
            bridge->obj = l1names[i];
            LOGEXTREME("Cached bridge %s\n", bridge->obj->name);
        }
        cache->max_bridges = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido bridges\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tbridges in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    snprintf(param[MIDO_CACHE_THREAD_BRIDGE].name, MIDO_CACHE_THREAD_NAME_LEN, "bridge");
    param[MIDO_CACHE_THREAD_BRIDGE].n = cache->max_bridges;
    param[MIDO_CACHE_THREAD_BRIDGE].get_from_mido = midonet_api_cache_refresh_bridgedhcps;
    param[MIDO_CACHE_THREAD_BRIDGE].cache = cache;
    rc = pthread_create(&pt[MIDO_CACHE_THREAD_BRIDGE], &ptattr, midonet_api_cache_refresh_objects_main_thread,
            (void *) &param[MIDO_CACHE_THREAD_BRIDGE]);
    //pthread_join(pt[MIDO_CACHE_THREAD_BRIDGE], NULL);

    // get all chains
    eucanetd_timer_usec(&tv);
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_chains(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->chains = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_chain *));
        midonet_api_chain *chain = NULL;
        for (i = 0; i < max_l1names; i++) {
            chain = EUCA_ZALLOC_C(1, sizeof (midonet_api_chain));
            cache->chains[i] = chain;
            chain->obj = l1names[i];
            LOGEXTREME("Cached chain %s\n", chain->obj->name);
        }
        cache->max_chains = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido chains\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tchains in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);
    
    snprintf(param[MIDO_CACHE_THREAD_CHAIN].name, MIDO_CACHE_THREAD_NAME_LEN, "chain");
    param[MIDO_CACHE_THREAD_CHAIN].n = cache->max_chains;
    param[MIDO_CACHE_THREAD_CHAIN].get_from_mido = midonet_api_cache_refresh_chainrules;
    param[MIDO_CACHE_THREAD_CHAIN].cache = cache;
    rc = pthread_create(&pt[MIDO_CACHE_THREAD_CHAIN], &ptattr, midonet_api_cache_refresh_objects_main_thread,
            (void *) &param[MIDO_CACHE_THREAD_CHAIN]);
    //pthread_join(pt[MIDO_CACHE_THREAD_CHAIN], NULL);

    // get all IP address groups
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_ipaddrgroups(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->ipaddrgroups = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_ipaddrgroup *));
        midonet_api_ipaddrgroup *ipaddrgroup = NULL;
        for (i = 0; i < max_l1names; i++) {
            ipaddrgroup = EUCA_ZALLOC_C(1, sizeof (midonet_api_ipaddrgroup));
            cache->ipaddrgroups[i] = ipaddrgroup;
            ipaddrgroup->obj = l1names[i];
            LOGEXTREME("Cached ip-address-group %s\n", ipaddrgroup->obj->name);
        }
        cache->max_ipaddrgroups = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido ip-address-groups\n");
        }
    }
    EUCA_FREE(l1names);
    LOGTRACE("\tipag in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);
    
    snprintf(param[MIDO_CACHE_THREAD_IPAG].name, MIDO_CACHE_THREAD_NAME_LEN, "ipag");
    param[MIDO_CACHE_THREAD_IPAG].n = cache->max_ipaddrgroups;
    param[MIDO_CACHE_THREAD_IPAG].get_from_mido = midonet_api_cache_refresh_ipagips;
    param[MIDO_CACHE_THREAD_IPAG].cache = cache;
    rc = pthread_create(&pt[MIDO_CACHE_THREAD_IPAG], &ptattr, midonet_api_cache_refresh_objects_main_thread, (void *) &param[MIDO_CACHE_THREAD_IPAG]);

    // get all port-groups
    eucanetd_timer_usec(&tv);
    l1names = NULL;
    max_l1names = 0;
    rc = mido_get_portgroups(VPCMIDO_TENANT, &l1names, &max_l1names);
    if (!rc && max_l1names) {
        cache->portgroups = EUCA_ZALLOC_C(max_l1names, sizeof (midonet_api_portgroup *));
        midonet_api_portgroup *portgroup = NULL;
        for (i = 0; i < max_l1names; i++) {
            portgroup = EUCA_ZALLOC_C(1, sizeof (midonet_api_portgroup));
            cache->portgroups[i] = portgroup;
            portgroup->obj = l1names[i];
            LOGEXTREME("Cached port-group %s\n", portgroup->obj->name);
            rc = mido_get_portgroup_ports(portgroup->obj, &(portgroup->ports), &(portgroup->max_ports));
            for (j = 0; j < portgroup->max_ports && !rc; j++) {
                LOGEXTREME("\tCached port %s\n", portgroup->ports[j]->uuid);
            }
        }
        cache->max_portgroups = max_l1names;
    } else {
        if (rc) {
            LOGWARN("Failed to retrieve mido port-groups\n");
        }
    }
    EUCA_FREE(l1names);

    // get all tunnel-zones
    midonet_api_cache_refresh_tunnelzones(cache);
    LOGTRACE("\tetc in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);

    pthread_join(pt[MIDO_CACHE_THREAD_ROUTER], NULL);
    pthread_join(pt[MIDO_CACHE_THREAD_BRIDGE], NULL);
    // get all hosts
    eucanetd_timer_usec(&tv);
    if (refreshmode == MIDO_CACHE_REFRESH_ALL) {
        rc = midonet_api_cache_iphostmap_populate(cache);
        if (rc) {
            LOGWARN("failed to populate mido ip-host map\n");
        }
    }
    LOGTRACE("\tiphostmap in %.2f\n", eucanetd_timer_usec(&tv) / 1000.0);
    
    pthread_join(pt[MIDO_CACHE_THREAD_CHAIN], NULL);
    pthread_join(pt[MIDO_CACHE_THREAD_IPAG], NULL);

    pthread_attr_destroy(&ptattr);

    // Enable midocache
    midocache = cache;
    //mido_info_midocache();
    return (ret);
}

/**
 * Populates the midonet_api_cache iphostmap table. Existing iphostmap is flushed.
 * The list of hosts is always loaded from MidoNet (regardless of midocache state).
 * @param cache [in] midonet_api_cache structure that holds the iphostmap of interest.
 * @return 0 on success. 1 on any failure.
 */
int midonet_api_cache_iphostmap_populate(midonet_api_cache *cache) {
    int rc = 0;
    midonet_api_cache *midocache_bak = NULL;
    
    if (!cache) {
        cache = midonet_api_cache_get();
    }

    // Disable midocache
    midocache_bak = midocache;
    midocache = NULL;
    
    rc = midonet_api_cache_refresh_hosts(cache);
    if (rc != 0) {
        LOGWARN("Unable to load hosts information from MidoNet.\n");
        return (1);
    }
    midonet_api_iphostmap *iphm = &(cache->iphostmap);
    if (iphm->entries) {
        EUCA_FREE(iphm->entries);
        iphm->entries = NULL;
    }
    iphm->max_entries = 0;
    LOGDEBUG("\tpopulating ip-to-midohost map table.\n");
    
    for (int i = 0; i < cache->max_hosts; i++) {
        iphm->entries = EUCA_REALLOC_C(iphm->entries, iphm->max_entries + cache->hosts[i]->max_addresses,
                sizeof (midonet_api_iphostmap_entry));
        if (iphm->entries) {
            bzero(&(iphm->entries[iphm->max_entries]), cache->hosts[i]->max_addresses * sizeof (midonet_api_iphostmap_entry));
        }
        for (int j = 0; j < cache->hosts[i]->max_addresses; j++) {
            iphm->entries[iphm->max_entries].ip = cache->hosts[i]->addresses[j];
            iphm->entries[iphm->max_entries].host = cache->hosts[i];
            (iphm->max_entries)++;
        }
    }
    iphm->sorted = 0;

    // recover midocache state
    midocache = midocache_bak;
    return (0);
}

/**
 * Gets the number of hosts found in cache.
 * @param cache [in] midonet_api_cache structure. If NULL, the system midonet_api_cache is used.
 * @return the number of hosts found in cache.
 */
int midonet_api_cache_get_nhosts(midonet_api_cache *cache) {
    if (!cache) {
        cache = midocache;
    }
    if (cache) {
        return (cache->max_hosts);
    }
    return (0);
}

/**
 * Gets the number of Eucalyptus tunnel-zone hosts found in cache.
 * @param cache [in] midonet_api_cache structure. If NULL, the system midonet_api_cache is used.
 * @return the number of hosts found in cache.
 */
int midonet_api_cache_get_ntzhosts(midonet_api_cache *cache) {
    if (!cache) {
        cache = midocache;
    }
    if (cache) {
        for (int i = 0; i < cache->max_tunnelzones; i++) {
            midonet_api_tunnelzone *tz = cache->tunnelzones[i];
            if (strstr(VPCMIDO_TUNNELZONE, tz->obj->name)) {
                return (tz->max_hosts);
            }
        }
        return (cache->max_hosts);
    }
    return (0);
}

/**
 * Searches midocache for the tunnel-zone in the argument.
 * @param tzone [in] tunnel-zone of interest.
 * @return pointer to the tunnel-zone data structure if found. NULL otherwise.
 */
midonet_api_tunnelzone *midonet_api_cache_lookup_tunnelzone(midoname *tzone) {
    if (midocache != NULL) {
        midonet_api_tunnelzone **tzones = midocache->tunnelzones;
        for (int i = 0; i < midocache->max_tunnelzones; i++) {
            if (tzones[i] == NULL) {
                continue;
            }
            if ((tzone == tzones[i]->obj) || (!strcmp(tzone->name, tzones[i]->obj->name))) {
                return tzones[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a tunnelzone entry in midocache.
 * @param tunnelzone [in] tunnelzone (not checked) of interest
 * @return Pointer to the newly created tunnelzone cache entry. NULL otherwise.
 */
midonet_api_tunnelzone *midonet_api_cache_add_tunnelzone(midoname *tunnelzone) {
    midonet_api_tunnelzone *newtz = NULL;
    newtz = EUCA_ZALLOC_C(1, sizeof (midonet_api_tunnelzone));
    newtz->obj = tunnelzone;
    midocache->tunnelzones = EUCA_APPEND_PTRARR(midocache->tunnelzones, &(midocache->max_tunnelzones), newtz);
    return (newtz);
}

/**
 * Adds a tunnel-zone host entry in midocache.
 * @param tunnelzone [in] tunnelzone (not checked) of interest
 * @param host [in] host (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_tunnelzone_host(midonet_api_tunnelzone *tzone, midoname *host) {
    tzone->hosts = EUCA_APPEND_PTRARR(tzone->hosts, &(tzone->max_hosts), host);
    return (0);
}

/**
 * Searches midocache for the host in the argument.
 * @param host [in] host of interest.
 * @return pointer to the host data structure if found. NULL otherwise.
 */
midonet_api_host *midonet_api_cache_lookup_host(midoname *name) {
    if (midocache != NULL) {
        midonet_api_host **hosts = midocache->hosts;
        for (int i = 0; i < midocache->max_hosts; i++) {
            if (hosts[i] == NULL) {
                continue;
            }
            if (name == hosts[i]->obj) {
                return hosts[i];
            }
            if (name->name && (!strcmp(name->name, hosts[i]->obj->name))) {
                return hosts[i];
            }
            if (name->uuid && (!strcmp(name->uuid, hosts[i]->obj->uuid))) {
                return hosts[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a port entry in midocache.
 * @param port [in] port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_port(midoname *port) {
    midocache->ports = EUCA_APPEND_PTRARR(midocache->ports, &(midocache->max_ports), port);
    return (0);
}

/**
 * Adds a bridge port entry in midocache. Both bridge ports and ports lists are updated.
 * @param bridge [in] bridge (not checked) of interest
 * @param port [in] port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_bridge_port(midonet_api_bridge *bridge, midoname *port) {
    bridge->ports = EUCA_APPEND_PTRARR(bridge->ports, &(bridge->max_ports), port);
    midocache->ports = EUCA_APPEND_PTRARR(midocache->ports, &(midocache->max_ports), port);
    return (0);
}

/**
 * Adds a router port entry in midocache. Both router ports and ports lists are updated.
 * @param router [in] router (not checked) of interest
 * @param port [in] port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_router_port(midonet_api_router *router, midoname *port) {
    router->ports = EUCA_APPEND_PTRARR(router->ports, &(router->max_ports), port);
    midocache->ports = EUCA_APPEND_PTRARR(midocache->ports, &(midocache->max_ports), port);
    return (0);
}

/**
 * Deletes a port entry from midocache. Caller responsible to remove corresponding
 * router or bridge port entries from midocache.
 * @param port [in] port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_port(midoname *port) {
    int idx = 0;
    midoname *todel = NULL;
    todel = midonet_api_cache_lookup_port(port, &idx);
    if (todel) {
        // midoname data structure should be released with midocache_midos
        midocache->ports[idx] = NULL;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Deletes a bridge port entry from midocache. Both bridge ports and ports lists are updated.
 * @param bridge [in] bridge (not checked) of interest
 * @param port [in] port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_bridge_port(midonet_api_bridge *bridge, midoname *port) {
    int found = 0;
    midoname **bports = bridge->ports;
    int max_bports = bridge->max_ports;
    
    for (int i = 0; i < max_bports && !found; i++) {
        if (bports[i] == port) {
            // midoname data structure should be released with midocache_midos
            found = 1;
            bports[i] = NULL;
        }
    }
    if (!found) {
        LOGWARN("port %s not found in %s\n", port->name, bridge->obj->name);
    }
    
    return (midonet_api_cache_del_port(port));
}

/**
 * Deletes a router port entry from midocache. Both router ports and ports lists are updated.
 * @param router [in] router (not checked) of interest
 * @param port [in] port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_router_port(midonet_api_router *router, midoname *port) {
    int found = 0;
    midoname **rports = router->ports;
    int max_rports = router->max_ports;
    
    for (int i = 0; i < max_rports && !found; i++) {
        if (rports[i] == port) {
            // midoname data structure should be released with midocache_midos
            found = 1;
            rports[i] = NULL;
        }
    }
    if (!found) {
        LOGWARN("port %s not found in %s\n", port->name, router->obj->name);
    }
    
    return (midonet_api_cache_del_port(port));
}

/**
 * Searches midocache for the port in the argument.
 * @param port [in] port of interest.
 * @param idx [out] index of the port in midocache, if found.
 * @return pointer to the port midoname structure if found. NULL otherwise.
 */
midoname *midonet_api_cache_lookup_port(midoname *port, int *idx) {
    if (midocache != NULL) {
        midoname **ports = midocache->ports;
        for (int i = 0; i < midocache->max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (ports[i] == port) {
                if (idx) {
                    *idx = i;
                }
                return ports[i];
            }
        }
        for (int i = 0; i < midocache->max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (!strcmp(port->uuid, ports[i]->uuid)) {
                LOGEXTREME("%s found with uuid cmp.\n", port->name);
                if (idx) {
                    *idx = i;
                }
                return ports[i];
            }
        }
    }
    return (NULL);
}

/**
 * Retrieves an array of pointers to midonet_api_bridge objects.
 * @param bridges [out] an array of pointers to midonet_api_bridge objects, to be returned
 * @param max_bridges [out] number of midonet_api_bridge objects.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_get_bridges(midonet_api_bridge ***bridges, int *max_bridges) {
    int count = 0;
    if (midocache != NULL) {
        *bridges = NULL;
        *max_bridges = 0;
        midonet_api_bridge **bds = midocache->bridges;
        if ((bds != NULL) && (midocache->max_bridges > 0)) {
            *bridges = EUCA_ZALLOC_C(midocache->max_bridges, sizeof (midonet_api_bridge *));
            midonet_api_bridge **res = *bridges;
            for (int i = 0; i < midocache->max_bridges; i++) {
                if (bds[i] == NULL) {
                    continue;
                }
                res[count] = bds[i];
                count++;
            }
            *max_bridges = count;
        }
        return (0);
    } else {
        return (1);
    }
}

/**
 * Adds a bridge entry in midocache.
 * @param bridge [in] bridge (not checked) of interest
 * @return Pointer to the newly created bridge cache entry. NULL otherwise.
 */
midonet_api_bridge *midonet_api_cache_add_bridge(midoname *bridge) {
    midonet_api_bridge *newbr = NULL;
    newbr = EUCA_ZALLOC_C(1, sizeof (midonet_api_bridge));
    newbr->obj = bridge;
    midocache->bridges = EUCA_APPEND_PTRARR(midocache->bridges, &(midocache->max_bridges), newbr);
    midocache->sorted_bridges = 0;
    return (newbr);
}

/**
 * Deletes a bridge entry from midocache.
 * @param bridge [in] bridge (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_bridge(midoname *bridge) {
    int idx = 0;
    midonet_api_bridge *todel = NULL;
    todel = midonet_api_cache_lookup_bridge(bridge, &idx);
    if (todel) {
        for (int i = 0; i < todel->max_ports; i++) {
            if (todel->ports[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_port(todel->ports[i]);
        }
        for (int i = 0; i < todel->max_dhcps; i++) {
            if (todel->dhcps[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_dhcp(todel, todel->dhcps[i]->obj);
        }
        midonet_api_bridge_free(todel);
        midocache->bridges[idx] = NULL;
        midocache->sorted_bridges = 0;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the bridge in the argument.
 * @param bridge [in] bridge of interest.
 * @param idx [out] index of the bridge in midocache, if found.
 * @return pointer to the bridge data structure if found. NULL otherwise.
 */
midonet_api_bridge *midonet_api_cache_lookup_bridge(midoname *bridge, int *idx) {
    if (midocache != NULL) {
        midonet_api_bridge **bridges = midocache->bridges;
        for (int i = 0; i < midocache->max_bridges; i++) {
            if (bridges[i] == NULL) {
                continue;
            }
            if (bridges[i]->obj == bridge) {
                if (idx) {
                    *idx = i;
                }
                return bridges[i];
            }
        }
        for (int i = 0; i < midocache->max_bridges; i++) {
            if (bridges[i] == NULL) {
                continue;
            }
            if (strstr(bridges[i]->obj->name, bridge->name)) {
                LOGEXTREME("%s found with name cmp.\n", bridge->name);
                if (idx) {
                    *idx = i;
                }
                return bridges[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a dhcp entry in midocache.
 * @param bridge [in] bridge (not checked) of interest
 * @param dhcp [in] dhcp (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_dhcp(midonet_api_bridge *bridge, midoname *dhcp) {
    midonet_api_dhcp *newdhcp = NULL;
    newdhcp = EUCA_ZALLOC_C(1, sizeof (midonet_api_dhcp));
    newdhcp->obj = dhcp;
    bridge->dhcps = EUCA_APPEND_PTRARR(bridge->dhcps, &(bridge->max_dhcps), newdhcp);
    return (0);
}

/**
 * Deletes a bridge dhcp entry from midocache.
 * @param bridge [in] bridge (not checked) of interest
 * @param dhcp [in] dhcp (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_dhcp(midonet_api_bridge *bridge, midoname *dhcp) {
    int idx = 0;
    midonet_api_dhcp *todel = NULL;
    todel = midonet_api_cache_lookup_dhcp(bridge, dhcp, &idx);
    if (todel) {
        for (int i = 0; i < todel->max_dhcphosts; i++) {
            if (todel->dhcphosts[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_dhcp_host(todel, todel->dhcphosts[i]);
        }
        midonet_api_dhcp_free(todel);
        bridge->dhcps[idx] = NULL;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the dhcp in the argument.
 * @param bridge [in] bridge (not checked) of interest
 * @param dhcp [in] dhcp of interest.
 * @param idx [out] index of the dhcp in midocache, if found.
 * @return pointer to the dhcp data structure if found. NULL otherwise.
 */
midonet_api_dhcp *midonet_api_cache_lookup_dhcp(midonet_api_bridge *bridge, midoname *dhcp, int *idx) {
    if (midocache != NULL) {
        midonet_api_dhcp **dhcps = bridge->dhcps;
        for (int i = 0; i < bridge->max_dhcps; i++) {
            if (dhcps[i] == NULL) {
                continue;
            }
            if (dhcps[i]->obj == dhcp) {
                if (idx) {
                    *idx = i;
                }
                return dhcps[i];
            }
        }
        for (int i = 0; i < bridge->max_dhcps; i++) {
            if (dhcps[i] == NULL) {
                continue;
            }
            if (!strcmp(dhcp->uuid, dhcps[i]->obj->uuid)) {
                LOGEXTREME("%s found with uuid cmp.\n", dhcp->name);
                if (idx) {
                    *idx = i;
                }
                return dhcps[i];
            }
        }
    }
    return (NULL);
}

/**
 * Searches midocache for the DHCP as specified in the arguments.
 * @param bridge [in] bridge of interest.
 * @param subnet [in] dhcp network address.
 * @param slashnet [in] dhcp network prefix length.
 * @param gw [in] gateway IP address of the dhcp subnet.
 * @param dnsServers [in] DNS servers to be offered to dhcp clients.
 * @param max_dnsServers [in] number of DNS servers to be offered to dhcp clients.
 * @param idx [out] index to bridge.dhcps array of the dhcp, if found.
 * @return pointer to the dhcp of interest, if found. NULL otherwise.
 */
midonet_api_dhcp *midonet_api_cache_lookup_dhcp_byparam(midonet_api_bridge *bridge,
        char *subnet, char *slashnet, char *gw, u32 *dnsServers, int max_dnsServers,
        int *idx) {
    int rc = 0;
    if (midocache != NULL) {
        for (int i = 0; i < bridge->max_dhcps; i++) {
            midonet_api_dhcp *dhcp = bridge->dhcps[i];
            if ((dhcp == NULL) || (dhcp->obj == NULL) || (dhcp->obj->init == 0)) {
                continue;
            }
            rc = mido_find_dhcp_from_list(&(dhcp->obj), 1, subnet, slashnet, gw, dnsServers, max_dnsServers, NULL);
            if (rc == 0) {
                if (idx) {
                    *idx = i;
                }
                return (dhcp);
            }
        }
    }
    return (NULL);
}

/**
 * Adds a dhcp host entry in midocache.
 * @param dhcp [in] dhcp (not checked) of interest
 * @param dhcphost [in] dhcphost (not checked) of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_dhcp_host(midonet_api_dhcp *dhcp, midoname *dhcphost) {
    dhcp->dhcphosts = EUCA_APPEND_PTRARR(dhcp->dhcphosts, &(dhcp->max_dhcphosts), dhcphost);
    dhcp->sorted_dhcphosts = 0;
    return (0);
}

/**
 * Deletes a dhcp host entry from midocache.
 * @param dhcp [in] dhcp (not checked) of interest
 * @param dhcphost [in] dhcphost (not checked) of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_dhcp_host(midonet_api_dhcp *dhcp, midoname *dhcphost) {
    int idx = 0;
    midoname *todel = NULL;
    todel = midonet_api_cache_lookup_dhcp_host(dhcp, dhcphost, &idx);
    if (todel) {
        // midoname data structure should be released with midocache_midos
        dhcp->dhcphosts[idx] = NULL;
        dhcp->sorted_dhcphosts = 0;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the dhcp host in the argument.
 * @param dhcp [in] dhcp of interest.
 * @param dhcphost [in] dhcp host of interest.
 * @param idx [out] index of the dhcp host in midocache, if found.
 * @return pointer to the dhcp host data structure if found. NULL otherwise.
 */
midoname *midonet_api_cache_lookup_dhcp_host(midonet_api_dhcp *dhcp, midoname *dhcphost, int *idx) {
    if (midocache != NULL) {
        midoname **dhcphosts = dhcp->dhcphosts;
        for (int i = 0; i < dhcp->max_dhcphosts; i++) {
            if (dhcphosts[i] == NULL) {
                continue;
            }
            if (dhcphosts[i] == dhcphost) {
                if (idx) {
                    *idx = i;
                }
                return dhcphosts[i];
            }
        }
        for (int i = 0; i < dhcp->max_dhcphosts; i++) {
            if (dhcphosts[i] == NULL) {
                continue;
            }
            if (!strcmp(dhcphost->uuid, dhcphosts[i]->uuid)) {
                LOGEXTREME("%s found with uuid cmp.\n", dhcphost->name);
                if (idx) {
                    *idx = i;
                }
                return (dhcphosts[i]);
            }
        }
    }
    return (NULL);    
}

/**
 * Searches midocache for the dhcp host in the argument.
 * @param dhcp [in] dhcp of interest.
 * @param dhcphost [in] dhcp host of interest.
 * @param name [in] name of the dhcphost (i-xxxxxxxx or eni-xxxxxxxx)
 * @param mac [in] Ethernet MAC address of the dhcphost.
 * @param ip [in] IP address of the dhcphost.
 * @param dns_domain [in] optional DNS domain to be sent with DHCP responses.
 * @param idx [out] index of the dhcp host in midocache, if found.
 * @return pointer to the dhcp host data structure if found. NULL otherwise.
 */
midoname *midonet_api_cache_lookup_dhcp_host_byparam(midonet_api_dhcp *dhcp,
        char *name, char *mac, char *ip, char *dns_domain, int *idx) {
    int rc = 0;
    int foundidx = 0;
    if (midocache != NULL) {
        rc = mido_find_dhcphost_from_list(dhcp->dhcphosts, dhcp->max_dhcphosts,
                name, mac, ip, dns_domain, &foundidx);
        if (rc == 0) {
            if (idx) {
                *idx = foundidx;
            }
            return (dhcp->dhcphosts[foundidx]);
        }
    }
    return (NULL);
    
}

/**
 * Retrieves an array of pointers to midonet_api_router objects.
 * @param routers [out] an array of pointers to midonet_api_router objects, to be returned
 * @param max_routers [out] number of retrieved routers.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_get_routers(midonet_api_router ***routers, int *max_routers) {
    int count = 0;
    if (midocache != NULL) {
        *routers = NULL;
        *max_routers = 0;
        midonet_api_router **rs = midocache->routers;
        if ((rs != NULL) && (midocache->max_routers > 0)) {
            *routers = EUCA_ZALLOC_C(midocache->max_routers, sizeof (midonet_api_router *));
            midonet_api_router **res = *routers;
            for (int i = 0; i < midocache->max_routers; i++) {
                if (rs[i] == NULL) {
                    continue;
                }
                res[count] = rs[i];
                count++;
            }
            *max_routers = count;
        }
        return (0);
    }
    return (1);
}

/**
 * Retrieves an array of pointers to midonet_api_router objects (for NAT gateways).
 * @param routers [out] an array of pointers to midonet_api_router objects (for NAT gateways)
 * @param max_routers [out] number of retrieved routers.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_get_natg_routers(midonet_api_router ***routers, int *max_routers) {
    if (midocache != NULL) {
        *routers = NULL;
        *max_routers = 0;
        midonet_api_router **rs = midocache->routers;
        if ((rs != NULL) && (midocache->max_routers > 0)) {
            midonet_api_router **res = NULL;
            for (int i = 0; i < midocache->max_routers; i++) {
                if (rs[i] == NULL) {
                    continue;
                }
                if (strstr(rs[i]->obj->name, "natr_nat")) {
                    res = EUCA_APPEND_PTRARR(res, max_routers, rs[i]);
                }
            }
            *routers = res;
        }
        return (0);
    }
    return (1);
}

/**
 * Adds a router entry in midocache.
 * @param router [in] router (not checked) of interest
 * @return Pointer to the newly created router cache entry. NULL otherwise.
 */
midonet_api_router *midonet_api_cache_add_router(midoname *router) {
    midonet_api_router *newrt = NULL;
    newrt = EUCA_ZALLOC_C(1, sizeof (midonet_api_router));
    newrt->obj = router;
    midocache->routers = EUCA_APPEND_PTRARR(midocache->routers, &(midocache->max_routers), newrt);
    midocache->sorted_routers = 0;
    return (newrt);
}

/**
 * Deletes a router entry from midocache.
 * @param router [in] router (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_router(midoname *router) {
    int idx = 0;
    midonet_api_router *todel = NULL;
    todel = midonet_api_cache_lookup_router(router, &idx);
    if (todel) {
        for (int i = 0; i < todel->max_ports; i++) {
            if (todel->ports[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_port(todel->ports[i]);
        }
        for (int i = 0; i < todel->max_routes; i++) {
            if (todel->routes[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_router_route(todel, todel->routes[i]);
        }
        midonet_api_router_free(todel);
        midocache->routers[idx] = NULL;
        midocache->sorted_routers = 0;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the router in the argument.
 * @param router [in] router of interest.
 * @param idx [out] index of the router in midocache, if found.
 * @return pointer to the router data structure if found. NULL otherwise.
 */
midonet_api_router *midonet_api_cache_lookup_router(midoname *router, int *idx) {
    if (midocache != NULL) {
        midonet_api_router **routers = midocache->routers;
        for (int i = 0; i < midocache->max_routers; i++) {
            if (routers[i] == NULL) {
                continue;
            }
            if (routers[i]->obj == router) {
                if (idx) {
                    *idx = i;
                }
                return routers[i];
            }
        }
        for (int i = 0; i < midocache->max_routers; i++) {
            if (routers[i] == NULL) {
                continue;
            }
            if (strstr(routers[i]->obj->name, router->name)) {
                LOGEXTREME("%s found with name cmp.\n", router->name);
                if (idx) {
                    *idx = i;
                }
                return routers[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a router route entry in midocache.
 * @param router [in] router (not checked) of interest
 * @param route [in] route (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_router_route(midonet_api_router *router, midoname *route) {
    router->routes = EUCA_APPEND_PTRARR(router->routes, &(router->max_routes), route);
    return (0);
}

/**
 * Deletes a router route entry from midocache.
 * @param router [in] router (not checked) of interest
 * @param route [in] route (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_router_route(midonet_api_router *router, midoname *route) {
    for (int i = 0; i < router->max_routes; i++) {
        if (router->routes[i] == NULL) {
            continue;
        }
        if (router->routes[i] == route) {
            router->routes[i] = NULL;
            (midocache_midos->released)++;
            return (0);
        }
    }
    return (1);
}

/**
 * Adds a portgroup entry in midocache.
 * @param pgroup [in] port-group (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_portgroup(midoname *pgroup) {
    midonet_api_portgroup *newpg = NULL;
    newpg = EUCA_ZALLOC_C(1, sizeof (midonet_api_portgroup));
    newpg->obj = pgroup;
    midocache->portgroups = EUCA_APPEND_PTRARR(midocache->portgroups, &(midocache->max_portgroups), newpg);
    return (0);
}

/**
 * Deletes a portgroup entry from midocache.
 * @param pgroup [in] port-group (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_portgroup(midoname *pgroup) {
    int idx = 0;
    midonet_api_portgroup *todel = NULL;
    todel = midonet_api_cache_lookup_portgroup(pgroup, &idx);
    if (todel) {
        for (int i = 0; i < todel->max_ports; i++) {
            if (todel->ports[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_portgroup_port(todel, todel->ports[i]);
        }
        midonet_api_portgroup_free(todel);
        midocache->portgroups[idx] = NULL;
        (midocache_midos->released)++;
        
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the port-group in the argument.
 * @param pgroup [in] port-group of interest.
 * @param idx [out] index of the port-group in midocache, if found.
 * @return pointer to the port-group data structure if found. NULL otherwise.
 */
midonet_api_portgroup *midonet_api_cache_lookup_portgroup(midoname *pgroup, int *idx) {
    if (midocache != NULL) {
        midonet_api_portgroup **pgs = midocache->portgroups;
        for (int i = 0; i < midocache->max_portgroups; i++) {
            if (pgs[i] == NULL) {
                continue;
            }
            if (pgs[i]->obj == pgroup) {
                if (idx) {
                    *idx = i;
                }
                return pgs[i];
            }
        }
        for (int i = 0; i < midocache->max_portgroups; i++) {
            if (pgs[i] == NULL) {
                continue;
            }
            if (!strcmp(pgroup->name, pgs[i]->obj->name)) {
                LOGEXTREME("%s found with name cmp.\n", pgroup->name);
                if (idx) {
                    *idx = i;
                }
                return pgs[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a port-group port in midocache.
 * @param pgroup [in] port-group (not checked) of interest
 * @param port [in] port-group port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_portgroup_port(midonet_api_portgroup *pgroup, midoname *port) {
    pgroup->ports = EUCA_APPEND_PTRARR(pgroup->ports, &(pgroup->max_ports), port);
    return (0);
}

/**
 * Deletes a port-group port from midocache.
 * @param pgroup [in] port-group (not checked) of interest
 * @param port [in] port-group port (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_portgroup_port(midonet_api_portgroup *pgroup, midoname *port) {
    int idx = 0;
    midoname *todel = NULL;
    todel = midonet_api_cache_lookup_portgroup_port(pgroup, port, &idx);
    if (todel) {
        pgroup->ports[idx] = NULL;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the port-group port in the argument.
 * @param pgroup [in] port-group port (not checked) of interest
 * @param port [in] port (not checked) of interest.
 * @param idx [out] index to the portgroup.ports array in midocache, if found.
 * @return pointer to the port midoname data structure if found. NULL otherwise.
 */
midoname *midonet_api_cache_lookup_portgroup_port(midonet_api_portgroup *pgroup, midoname *port, int *idx) {
    if (midocache != NULL) {
        midoname **ports = pgroup->ports;
        for (int i = 0; i < pgroup->max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (ports[i] == port) {
                if (idx) {
                    *idx = i;
                }
                return ports[i];
            }
        }
        for (int i = 0; i < pgroup->max_ports; i++) {
            if (ports[i] == NULL) {
                continue;
            }
            if (!strcmp(port->uuid, ports[i]->uuid)) {
                LOGEXTREME("%s port found with uuid cmp.\n", pgroup->obj->name);
                if (idx) {
                    *idx = i;
                }
                return ports[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a chain entry in midocache.
 * @param chain [in] chain (not checked) of interest
 * @return Pointer to the newly created chain object.
 */
midonet_api_chain *midonet_api_cache_add_chain(midoname *chain) {
    midonet_api_chain *newchain = NULL;
    newchain = EUCA_ZALLOC_C(1, sizeof (midonet_api_chain));
    newchain->obj = chain;
    midocache->chains = EUCA_APPEND_PTRARR(midocache->chains, &(midocache->max_chains), newchain);
    midocache->sorted_chains = 0;
    return (newchain);
}

/**
 * Deletes a chain entry from midocache.
 * @param chain [in] chain (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_chain(midoname *chain) {
    int idx = 0;
    midonet_api_chain *todel = NULL;
    todel = midonet_api_cache_lookup_chain(chain, &idx);
    if (todel) {
        for (int i = 0; i < todel->max_rules; i++) {
            if (todel->rules[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_chain_rule(todel, todel->rules[i]);
        }
        midonet_api_chain_free(todel);
        midocache->chains[idx] = NULL;
        midocache->sorted_chains = 0;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the chain in the argument.
 * @param chain [in] chain of interest.
 * @param idx [out] index of the chain in midocache, if found.
 * @return pointer to the chain data structure if found. NULL otherwise.
 */
midonet_api_chain *midonet_api_cache_lookup_chain(midoname *chain, int *idx) {
    if (midocache != NULL) {
        midonet_api_chain **chains = midocache->chains;
        for (int i = 0; i < midocache->max_chains; i++) {
            if (chains[i] == NULL) {
                continue;
            }
            if (chains[i]->obj == chain) {
                if (idx) {
                    *idx = i;
                }
                return chains[i];
            }
        }
        for (int i = 0; i < midocache->max_chains; i++) {
            if (chains[i] == NULL) {
                continue;
            }
            if (!strcmp(chain->name, chains[i]->obj->name)) {
                LOGEXTREME("%s found with name cmp.\n", chain->name);
                if (idx) {
                    *idx = i;
                }
                return chains[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds a chain rule entry in midocache.
 * @param chain [in] chain (not checked) of interest
 * @param rule [in] rule (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_chain_rule(midonet_api_chain *chain, midoname *rule) {
    chain->rules = EUCA_APPEND_PTRARR(chain->rules, &(chain->max_rules), rule);
    (chain->rules_count)++;
    return (0);
}

/**
 * Deletes a chain rule entry from midocache.
 * @param chain [in] chain (not checked) of interest
 * @param rule [in] rule (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_chain_rule(midonet_api_chain *chain, midoname *rule) {
    for (int i = 0; i < chain->max_rules; i++) {
        if (chain->rules[i] == NULL) {
            continue;
        }
        if (chain->rules[i] == rule) {
            chain->rules[i] = NULL;
            (chain->rules_count)--;
            (midocache_midos->released)++;
            return (0);
        }
    }
    return (1);
}

/**
 * Adds a ipaddrgroup entry in midocache.
 * @param ipaddrgroup [in] ipaddrgroup (not checked) of interest
 * @return Pointer to the newly added ip-address-group cache entry. NULL otherwise.
 */
midonet_api_ipaddrgroup *midonet_api_cache_add_ipaddrgroup(midoname *ipaddrgroup) {
    midonet_api_ipaddrgroup *newipaddrgroup = NULL;
    newipaddrgroup = EUCA_ZALLOC_C(1, sizeof (midonet_api_ipaddrgroup));
    newipaddrgroup->obj = ipaddrgroup;
    midocache->ipaddrgroups = EUCA_APPEND_PTRARR(midocache->ipaddrgroups, &(midocache->max_ipaddrgroups), newipaddrgroup);
    midocache->sorted_ipaddrgroups = 0;
    return (newipaddrgroup);
}

/**
 * Deletes a ipaddrgroup entry from midocache.
 * @param ipaddrgroup [in] ipaddrgroup (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_ipaddrgroup(midoname *ipaddrgroup) {
    int idx = 0;
    midonet_api_ipaddrgroup *todel = NULL;
    todel = midonet_api_cache_lookup_ipaddrgroup(ipaddrgroup, &idx);
    if (todel) {
        for (int i = 0; i < todel->max_ips; i++) {
            if (todel->ips[i] == NULL) {
                continue;
            }
            midonet_api_cache_del_ipaddrgroup_ip(todel, todel->ips[i]);
        }
        midonet_api_ipaddrgroup_free(todel);
        midocache->ipaddrgroups[idx] = NULL;
        midocache->sorted_ipaddrgroups = 0;
        (midocache_midos->released)++;
        return (0);
    }
    return (1);
}

/**
 * Searches midocache for the ipaddrgroup in the argument.
 * @param ipaddrgroup [in] ipaddrgroup of interest.
 * @param idx [out] index of the ipaddrgroup in midocache, if found.
 * @return pointer to the ipaddrgroup data structure if found. NULL otherwise.
 */
midonet_api_ipaddrgroup *midonet_api_cache_lookup_ipaddrgroup(midoname *ipaddrgroup, int *idx) {
    if (midocache != NULL) {
        midonet_api_ipaddrgroup **ipaddrgroups = midocache->ipaddrgroups;
        for (int i = 0; i < midocache->max_ipaddrgroups; i++) {
            if (ipaddrgroups[i] == NULL) {
                continue;
            }
            if (ipaddrgroups[i]->obj == ipaddrgroup) {
                if (idx) {
                    *idx = i;
                }
                return ipaddrgroups[i];
            }
        }
        for (int i = 0; i < midocache->max_ipaddrgroups; i++) {
            if (ipaddrgroups[i] == NULL) {
                continue;
            }
            if (!strcmp(ipaddrgroup->name, ipaddrgroups[i]->obj->name)) {
                LOGEXTREME("%s found with name cmp.\n", ipaddrgroup->name);
                if (idx) {
                    *idx = i;
                }
                return ipaddrgroups[i];
            }
        }
    }
    return (NULL);
}

/**
 * Adds an ip-address-group ip entry in midocache.
 * @param ipaddrgroup [in] ip-address-group (not checked) of interest
 * @param ip [in] ip (not checked) of interest
 * @param hexip [in] ip address in integer representation (not checked)
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_add_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, midoname *ip, u32 hexip) {
    ipaddrgroup->hexips = EUCA_REALLOC_C(ipaddrgroup->hexips, ipaddrgroup->max_ips + 1, sizeof (u32));
    ipaddrgroup->hexips[ipaddrgroup->max_ips] = hexip;
    ipaddrgroup->ips = EUCA_APPEND_PTRARR(ipaddrgroup->ips, &(ipaddrgroup->max_ips), ip);
    (ipaddrgroup->ips_count)++;
    return (0);
}

/**
 * Deletes a ip-address-group ip entry from midocache.
 * @param ipaddrgroup [in] ip-address-group (not checked) of interest
 * @param ip [in] ip (not checked) of interest
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_cache_del_ipaddrgroup_ip(midonet_api_ipaddrgroup *ipaddrgroup, midoname *ip) {
    for (int i = 0; i < ipaddrgroup->max_ips; i++) {
        if (ipaddrgroup->ips[i] == NULL) {
            continue;
        }
        if (ipaddrgroup->ips[i] == ip) {
            ipaddrgroup->hexips[i] = 0;
            ipaddrgroup->ips[i] = NULL;
            (ipaddrgroup->ips_count)--;
            (midocache_midos->released)++;
            if (ipaddrgroup->ips_count == 0) {
                EUCA_FREE(ipaddrgroup->ips);
                EUCA_FREE(ipaddrgroup->hexips);
                ipaddrgroup->ips = NULL;
                ipaddrgroup->hexips = NULL;
                ipaddrgroup->max_ips = 0;
            }
            return (0);
        }
    }
    return (1);
}

/**
 * Releases resources allocated for a midonet_api_router data structure.
 * @param router [in] midonet_api_router structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_router_free(midonet_api_router *router) {
    if (router == NULL) {
        return (1);
    }
    EUCA_FREE(router->ports);
    EUCA_FREE(router->routes);
    bzero(router, sizeof (midonet_api_router));
    EUCA_FREE(router);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_dhcp data structure.
 * @param dhcp [in] midonet_api_dhcp structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_dhcp_free(midonet_api_dhcp *dhcp) {
    if (dhcp == NULL) {
        return (1);
    }
    EUCA_FREE(dhcp->dhcphosts);
    bzero(dhcp, sizeof (midonet_api_dhcp));
    EUCA_FREE(dhcp);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_bridge data structure.
 * @param bridge [in] midonet_api_bridge structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_bridge_free(midonet_api_bridge *bridge) {
    if (bridge == NULL) {
        return (1);
    }
    for (int i = 0; i < bridge->max_dhcps; i++) {
        midonet_api_dhcp_free(bridge->dhcps[i]);
    }
    EUCA_FREE(bridge->dhcps);
    EUCA_FREE(bridge->ports);
    bzero(bridge, sizeof (midonet_api_bridge));
    EUCA_FREE(bridge);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_chain data structure.
 * @param chain [in] midonet_api_chain structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_chain_free(midonet_api_chain *chain) {
    if (chain == NULL) {
        return (1);
    }
    EUCA_FREE(chain->rules);
    bzero(chain, sizeof (midonet_api_chain));
    EUCA_FREE(chain);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_host data structure.
 * @param host [in] midonet_api_host structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_host_free(midonet_api_host *host) {
    if (host == NULL) {
        return (1);
    }
    EUCA_FREE(host->addresses);
    bzero(host, sizeof (midonet_api_host));
    EUCA_FREE(host);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_ipaddrgroup data structure.
 * @param ipaddrgroup [in] midonet_api_ipaddrgroup structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_ipaddrgroup_free(midonet_api_ipaddrgroup *ipaddrgroup) {
    if (ipaddrgroup == NULL) {
        return (1);
    }
    EUCA_FREE(ipaddrgroup->ips);
    EUCA_FREE(ipaddrgroup->hexips);
    bzero(ipaddrgroup, sizeof (midonet_api_ipaddrgroup));
    EUCA_FREE(ipaddrgroup);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_portgroup data structure.
 * @param ipaddrgroup [in] midonet_api_portgroup structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_portgroup_free(midonet_api_portgroup *portgroup) {
    if (portgroup == NULL) {
        return (1);
    }
    EUCA_FREE(portgroup->ports);
    bzero(portgroup, sizeof (midonet_api_portgroup));
    EUCA_FREE(portgroup);
    return (0);
}

/**
 * Releases resources allocated for a midonet_api_tunnelzone data structure.
 * @param tunnelzone [in] midonet_api_tunnelzone structure of interest.
 * @return 0 on success. 1 otherwise.
 */
int midonet_api_tunnelzone_free(midonet_api_tunnelzone *tunnelzone) {
    if (tunnelzone == NULL) {
        return (1);
    }
    EUCA_FREE(tunnelzone->hosts);
    bzero(tunnelzone, sizeof (midonet_api_tunnelzone));
    EUCA_FREE(tunnelzone);
    return (0);
}

/**
 * Delete all bridges, routers, ip-address-groups, and chains with names consistent
 * with eucanetd VPCMIDO naming scheme.
 * @return always 0.
 */
int midonet_api_delete_all(void) {
    // Refresh midocache
    int rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_NOHOSTS);
    if (rc) {
        LOGERROR("cannot populate midocache prior to cleanup: check midonet health\n");
        return (1);
    }

    // Delete all ip-address-groups
    for (int i = 0; i < midocache->max_ipaddrgroups; i++) {
        if (strstr(midocache->ipaddrgroups[i]->obj->name, "sg_") ||
                strstr(midocache->ipaddrgroups[i]->obj->name, "elip_")) {
            LOGINFO("\t\t%s del\n", midocache->ipaddrgroups[i]->obj->name);
            mido_delete_resource(NULL, midocache->ipaddrgroups[i]->obj);
        }
    }
    
    // Delete all chains
    for (int i = 0; i < midocache->max_chains; i++) {
        if (strstr(midocache->chains[i]->obj->name, "sg_") ||
                strstr(midocache->chains[i]->obj->name, "ic_") ||
                strstr(midocache->chains[i]->obj->name, "vc_") ||
                strstr(midocache->chains[i]->obj->name, "natc_")) {
            LOGINFO("\t\t%s del\n", midocache->chains[i]->obj->name);
            mido_delete_resource(NULL, midocache->chains[i]->obj);
        }
    }

    // Delete all ports that are not connected
    for (int i = 0; i < midocache->max_ports; i++) {
        char *peerId = NULL;
        char *interfaceName = NULL;
        if (midocache->ports[i]->port) {
            peerId = midocache->ports[i]->port->peerid;
            interfaceName = midocache->ports[i]->port->ifname;
        }
        //mido_getel_midoname(midocache->ports[i], "peerId", &peerId);
        //mido_getel_midoname(midocache->ports[i], "interfaceName", &interfaceName);
        
        if (!peerId && !interfaceName) {
            LOGINFO("\t\tport %s del\n", midocache->ports[i]->name);
            mido_delete_resource(NULL, midocache->ports[i]);
        }
        //EUCA_FREE(peerId);
        //EUCA_FREE(interfaceName);
    }

    // Delete all bridges
    for (int i = 0; i < midocache->max_bridges; i++) {
        if (strstr(midocache->bridges[i]->obj->name, "vb_vpc")) {
            LOGINFO("\t\t%s del\n", midocache->bridges[i]->obj->name);
            mido_delete_resource(NULL, midocache->bridges[i]->obj);
        }
    }

    // Delete all routers
    for (int i = 0; i < midocache->max_routers; i++) {
        if (strstr(midocache->routers[i]->obj->name, "vr_vpc") ||
                strstr(midocache->routers[i]->obj->name, "natr_nat")) {
            LOGINFO("\t\t%s del\n", midocache->routers[i]->obj->name);
            mido_delete_resource(NULL, midocache->routers[i]->obj);
        }
    }

    return (0);
}

/**
 * Iterate through all bridges, routers, ip-address-groups, and chains and detect/
 * delete duplicate objects.
 * @param checkonly [in] if true, only performs check. If false, duplicate objects
 * are deleted.
 * @return always 0.
 */
int midonet_api_delete_dups(boolean checkonly) {
    boolean deleted = FALSE;
    boolean gDeleted = FALSE;
    boolean gDupDetected = FALSE;
    midoname *a = NULL;
    midoname *b = NULL;
    int aidx = 0;
    int bidx = 0;
    // Refresh midocache
    LOGINFO("Loading objects from MidoNet.\n");
    int rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_NOHOSTS);
    if (rc) {
        LOGERROR("cannot populate midocache: check midonet health\n");
        return (1);
    }

    // Check ip-address-groups
    mido_get_ipaddrgroup("metadata_ip");
    LOGINFO("\tchecking ip-address-groups\n");
    deleted = FALSE;
    a = NULL;
    aidx = 0;
    b = NULL;
    bidx = 0;
    for (int i = 1; i < midocache->max_ipaddrgroups; i++) {
        if (strstr(midocache->ipaddrgroups[i]->obj->name, "sg_") ||
                strstr(midocache->ipaddrgroups[i]->obj->name, "elip_") ||
                strstr(midocache->ipaddrgroups[i]->obj->name, "metadata_")) {
            if (!deleted) {
                a = midocache->ipaddrgroups[i - 1]->obj;
                aidx = i - 1;
            }
            b = midocache->ipaddrgroups[i]->obj;
            bidx = i;
            if (!strcmp(a->name, b->name)) {
                LOGINFO("\t\t%s duplicate at idx %d\n", a->name, i);
                gDupDetected = TRUE;
                if (!checkonly) {
                    if (midocache->ipaddrgroups[aidx]->ips_count > midocache->ipaddrgroups[bidx]->ips_count) {
                        mido_delete_resource(NULL, midocache->ipaddrgroups[bidx]->obj);
                        deleted = TRUE;
                    } else {
                        mido_delete_resource(NULL, midocache->ipaddrgroups[aidx]->obj);
                        deleted = FALSE;
                    }
                    gDeleted = TRUE;
                }
            } else {
                deleted = FALSE;
            }
        }
    }
    
    // Check all chains
    mido_get_chain("eucabr_infilter");
    LOGINFO("\tchecking chains\n");
    deleted = FALSE;
    a = NULL;
    aidx = 0;
    b = NULL;
    bidx = 0;
    for (int i = 1; i < midocache->max_chains; i++) {
        if (strstr(midocache->chains[i]->obj->name, "sg_") ||
                strstr(midocache->chains[i]->obj->name, "ic_") ||
                strstr(midocache->chains[i]->obj->name, "vc_") ||
                strstr(midocache->chains[i]->obj->name, "natc_") ||
                strstr(midocache->chains[i]->obj->name, "eucabr_")) {
            if (!deleted) {
                a = midocache->chains[i - 1]->obj;
                aidx = i - 1;
            }
            b = midocache->chains[i]->obj;
            bidx = i;
            if (!strcmp(a->name, b->name)) {
                LOGINFO("\t\t%s duplicate at idx %d\n", a->name, i);
                gDupDetected = TRUE;
                if (!checkonly) {
                    if (midocache->chains[aidx]->rules_count > midocache->chains[bidx]->rules_count) {
                        mido_delete_resource(NULL, midocache->chains[bidx]->obj);
                        deleted = TRUE;
                    } else {
                        mido_delete_resource(NULL, midocache->chains[aidx]->obj);
                        deleted = FALSE;
                    }
                    gDeleted = TRUE;
                }
            } else {
                deleted = FALSE;
            }
        }
    }
    // Check chain rules
    LOGINFO("\tchecking chain rules\n");
    for (int i = 0; i < midocache->max_chains; i++) {
        if ((!midocache->chains[i]->obj) || (!midocache->chains[i]->obj->init)) continue;
        if (strstr(midocache->chains[i]->obj->name, "sg_") ||
                strstr(midocache->chains[i]->obj->name, "ic_") ||
                strstr(midocache->chains[i]->obj->name, "vc_") ||
                strstr(midocache->chains[i]->obj->name, "natc_") ||
                strstr(midocache->chains[i]->obj->name, "eucabr_")) {
            if (midonet_api_delete_dups_rules(midocache->chains[i], checkonly)) {
                LOGINFO("\t\tfor chain %s\n", midocache->chains[i]->obj->name);
                gDupDetected = TRUE;
            }
        }
    }

    // Delete all bridges
    mido_get_bridge("eucabr");
    LOGINFO("\tchecking bridges\n");
    deleted = FALSE;
    a = NULL;
    aidx = 0;
    b = NULL;
    bidx = 0;
    for (int i = 1; i < midocache->max_bridges; i++) {
        if (strstr(midocache->bridges[i]->obj->name, "vb_vpc") ||
                strstr(midocache->bridges[i]->obj->name, "eucabr")) {
            if (!deleted) {
                a = midocache->bridges[i - 1]->obj;
                aidx = i - 1;
            }
            b = midocache->bridges[i]->obj;
            bidx = i;
            if (!strcmp(a->name, b->name)) {
                LOGINFO("\t\t%s duplicate at idx %d\n", a->name, i);
                gDupDetected = TRUE;
                if (!checkonly) {
                    if (midocache->bridges[aidx]->max_ports >  midocache->bridges[bidx]->max_ports) {
                        mido_delete_resource(NULL, midocache->bridges[bidx]->obj);
                        deleted = TRUE;
                    } else {
                        mido_delete_resource(NULL, midocache->bridges[aidx]->obj);
                        deleted = FALSE;
                    }
                    gDeleted = TRUE;
                }
            } else {
                deleted = FALSE;
            }
        }
    }

    // Delete all routers
    LOGINFO("\tchecking routers\n");
    mido_get_router("eucart");
    deleted = FALSE;
    a = NULL;
    aidx = 0;
    b = NULL;
    bidx = 0;
    char *aname = NULL;
    char *bname = NULL;
    aidx = 0;
    int aid = 0;
    bidx = 0;
    int bid = 0;
    for (int i = 1; i < midocache->max_routers; i++) {
        if (strstr(midocache->routers[i]->obj->name, "vr_vpc") ||
                strstr(midocache->routers[i]->obj->name, "natr_nat") ||
                strstr(midocache->routers[i]->obj->name, "eucart")) {
            if (!deleted) {
                a = midocache->routers[i - 1]->obj;
                aidx = i - 1;
                EUCA_FREE(aname);
                routername_split(a->name, &aname, &aid);
            }
            b = midocache->routers[i]->obj;
            bidx = i;
            EUCA_FREE(bname);
            routername_split(b->name, &bname, &bid);
            if (aname && bname && !strcmp(aname, bname)) {
                LOGINFO("\t\t%s duplicate at idx %d\n", a->name, i);
                gDupDetected = TRUE;
                if (!checkonly) {
                    if (aid == bid) {
                        if (midocache->routers[aidx]->max_routes > midocache->routers[bidx]->max_routes) {
                            mido_delete_resource(NULL, midocache->routers[bidx]->obj);
                            deleted = TRUE;
                        } else {
                            mido_delete_resource(NULL, midocache->routers[aidx]->obj);
                            deleted = FALSE;
                        }
                    } else if (aid < bid) {
                        mido_delete_resource(NULL, midocache->routers[bidx]->obj);
                        deleted = TRUE;
                    } else {
                        mido_delete_resource(NULL, midocache->routers[aidx]->obj);;
                        deleted = FALSE;
                    }
                    gDeleted = TRUE;
                }
            } else {
                if (!strcmp(a->name, b->name) && strstr(a->name, "eucart")) {
                    LOGINFO("\t\t%s duplicate at idx %d\n", a->name, i);
                    gDupDetected = TRUE;
                    if (!checkonly) {
                        if (midocache->routers[aidx]->max_routes > midocache->routers[bidx]->max_routes) {
                            mido_delete_resource(NULL, midocache->routers[bidx]->obj);
                            deleted = TRUE;
                        } else {
                            mido_delete_resource(NULL, midocache->routers[aidx]->obj);
                            deleted = FALSE;
                        }
                    }
                } else {
                    deleted = FALSE;
                }
            }
        }
    }
    EUCA_FREE(aname);
    EUCA_FREE(bname);

    if (gDeleted) {
        // Refresh midocache
        LOGINFO("(Re)Loading objects from MidoNet.\n");
        int rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_NOHOSTS);
        if (rc) {
            LOGERROR("cannot populate midocache: check midonet health\n");
            return (1);
        }
        gDeleted = FALSE;
    }
    
/*
    midonet_api_chain *eucabrin = mido_get_chain("eucabr_infilter");
    if (eucabrin) {
        LOGINFO("\tchecking eucabr infilter\n");
        if (midonet_api_delete_dups_rules(eucabrin, checkonly)) {
            gDupDetected = TRUE;
        }
    }
*/

    midonet_api_router *eucart = mido_get_router(VPCMIDO_CORERT);
    if (eucart) {
        LOGINFO("\tchecking eucart routes\n");
        if (midonet_api_delete_dups_routes(eucart, checkonly)) {
            gDupDetected = TRUE;
        }
    }

    if (!gDupDetected) {
        LOGINFO("=== System checked for duplicates. Looks CLEAN ===\n");
    } else {
        LOGWARN("=== DUPLICATE(S) DETECTED ===\n");
    }
    return (0);
}

/**
 * Iterate through the list of ports and detect/delete unconnected ports.
 * @param ports [in] pointer to an array of pointers to midoname structures representing ports..
 * @param max_ports [in] number of entries in the array
 * @param checkonly [in] if true, only performs check. If false, unconnected ports are
 * are deleted.
 * @return number of detected (unconnected) ports.
 */
int midonet_api_delete_unconnected_ports(midoname **ports, int max_ports, boolean checkonly) {
    int ret = 0;
    for (int i = 0; i < max_ports; i++) {
        char *peerId = NULL;
        char *interfaceName = NULL;
        if (ports[i]->port) {
            peerId = ports[i]->port->peerid;
            interfaceName = ports[i]->port->ifname;
        }
        
        if (!peerId && !interfaceName) {
            LOGINFO("\t\tunconnected port %s\n", ports[i]->name);
            ret++;
            if (!checkonly) {
                mido_delete_resource(NULL, midocache->ports[i]);
            }
        }
    }    
    return (ret);
}

/**
 * Iterate through rules of the given chain and detect/delete duplicate rules.
 * @param chain [in] chain of interest.
 * @param checkonly [in] if true, only performs check. If false, duplicate objects
 * are deleted.
 * @return number of detected duplicates.
 */
int midonet_api_delete_dups_rules(midonet_api_chain *chain, boolean checkonly) {
    int ret = 0;
    if (!chain) {
        return (ret);
    }
    for (int i = 0; i < chain->max_rules; i++) {
        if ((!chain->rules[i]) || (!chain->rules[i]->init)) continue;
        for (int j = i + 1; j < chain->max_rules; j++) {
            if ((!chain->rules[j]) || (!chain->rules[j]->init)) continue;
            if (!mido_cmp_midoname_jsonbuf(chain->rules[i], chain->rules[j])) {
                LOGINFO("\t\tduplicate rule detected at idx %d\n", i);
                ret++;
                if (!checkonly) {
                    mido_delete_resource(NULL, chain->rules[i]);
                }
                break;
            }
        }
    }
    return (ret);
}

/**
 * Iterate through route of the given router and detect/delete duplicate routes.
 * @param router [in] router of interest.
 * @param checkonly [in] if true, only performs check. If false, duplicate objects
 * are deleted.
 * @return number of detected duplicates.
 */
int midonet_api_delete_dups_routes(midonet_api_router *router, boolean checkonly) {
    int ret = 0;
    if (!router) {
        return (ret);
    }
    for (int i = 0; i < router->max_routes; i++) {
        if ((!router->routes[i]) || (!router->routes[i]->init)) continue;
        for (int j = i + 1; j < router->max_routes; j++) {
            if ((!router->routes[j]) || (!router->routes[j]->init)) continue;
            if (!mido_cmp_midoname_jsonbuf(router->routes[i], router->routes[j])) {
                LOGINFO("\t\tduplicate route detected at idx %d\n", i);
                ret++;
                if (!checkonly) {
                    mido_delete_resource(NULL, router->routes[i]);
                }
                break;
            }
        }
    }
    return (ret);
}

/**
 * Comparator function for midonet_api_iphostmap_entry structures.
 * @param p1 [in] pointer to midonet_api_iphostmap_entry 1.
 * @param p2 [in] pointer to midonet_api_iphostmap_entry 2.
 * @return 0 iff p1->ip == p2->ip. -1 iff p1->ip < p2->ip. 1 iff p1->ip > p2->ip.
 */
int compare_midonet_api_iphostmap_entry(const void *p1, const void *p2) {
    midonet_api_iphostmap_entry *e1 = (midonet_api_iphostmap_entry *) p1;
    midonet_api_iphostmap_entry *e2 = (midonet_api_iphostmap_entry *) p2;

    if (e1->ip == e2->ip) {
        return 0;
    }
    if (e1->ip < e2->ip) {
        return -1;
    } else {
        return 1;
    }
}

/**
 * Comparator function for midoname structures. Comparison is base on name property.
 * @param p1 [in] pointer to midoname pointer 1.
 * @param p2 [in] pointer to midoname pointer 2.
 * @return 0 iff p1->name == p2->name. -1 iff p1->name < p2->name. 1 iff p1->name > p2->name.
 * NULL is considered larger than a non-NULL string.
 */
int compare_midoname_name(const void *p1, const void *p2) {
    midoname **pp1 = NULL;
    midoname **pp2 = NULL;
    midoname *e1 = NULL;
    midoname *e2 = NULL;
    char *name1 = NULL;
    char *name2 = NULL;

    if ((p1 == NULL) || (p2 == NULL)) {
        LOGWARN("Invalid argument: cannot compare NULL midoname\n");
        return (0);
    }
    pp1 = (midoname **) p1;
    pp2 = (midoname **) p2;
    e1 = *pp1;
    e2 = *pp2;
    if (e1 && e1->name && strlen(e1->name)) {
        name1 = e1->name;
    }
    if (e2 && e2->name && strlen(e2->name)) {
        name2 = e2->name;
    }
    if (name1 == name2) {
        return (0);
    }
    if (name1 == NULL) {
        return (1);
    }
    if (name2 == NULL) {
        return (-1);
    }
    return (strcmp(name1, name2));
}

/**
 * Comparator function for midonet_api_bridge structures. Comparison is base on name property.
 * @param p1 [in] pointer to midonet_api_bridge pointer 1.
 * @param p2 [in] pointer to midonet_api_bridge pointer 2.
 * @return 0 iff p1->.->obj->name == p2->.->obj->name. -1 iff p1->.->obj->name < p2->.->obj->name.
 * 1 iff p1->.->obj->name > p2->.->obj->name.
 * NULL is considered larger than a non-NULL string.
 */
int compare_midonet_api_bridge(const void *p1, const void *p2) {
    midonet_api_bridge **pp1 = NULL;
    midonet_api_bridge **pp2 = NULL;
    midonet_api_bridge *e1 = NULL;
    midonet_api_bridge *e2 = NULL;
    char *name1 = NULL;
    char *name2 = NULL;

    if ((p1 == NULL) || (p2 == NULL)) {
        LOGWARN("Invalid argument: cannot compare NULL midonet_api_bridge\n");
        return (0);
    }
    pp1 = (midonet_api_bridge **) p1;
    pp2 = (midonet_api_bridge **) p2;
    e1 = *pp1;
    e2 = *pp2;
    if (e1 && e1->obj && e1->obj->name && strlen(e1->obj->name)) {
        name1 = e1->obj->name;
    }
    if (e2 && e2->obj && e2->obj->name && strlen(e2->obj->name)) {
        name2 = e2->obj->name;
    }
    if (name1 == name2) {
        return (0);
    }
    if (name1 == NULL) {
        return (1);
    }
    if (name2 == NULL) {
        return (-1);
    }
    return (strcmp(name1, name2));
}

/**
 * Comparator function for midonet_api_router structures. Comparison is base on name property.
 * @param p1 [in] pointer to midonet_api_router pointer 1.
 * @param p2 [in] pointer to midonet_api_router pointer 2.
 * @return 0 iff p1->.->obj->name == p2->.->obj->name. -1 iff p1->.->obj->name < p2->.->obj->name.
 * 1 iff p1->.->obj->name > p2->.->obj->name.
 * NULL is considered larger than a non-NULL string.
 */
int compare_midonet_api_router(const void *p1, const void *p2) {
    midonet_api_router **pp1 = NULL;
    midonet_api_router **pp2 = NULL;
    midonet_api_router *e1 = NULL;
    midonet_api_router *e2 = NULL;
    char *name1 = NULL;
    char *name2 = NULL;

    if ((p1 == NULL) || (p2 == NULL)) {
        LOGWARN("Invalid argument: cannot compare NULL midonet_api_router\n");
        return (0);
    }
    pp1 = (midonet_api_router **) p1;
    pp2 = (midonet_api_router **) p2;
    e1 = *pp1;
    e2 = *pp2;
    if (e1 && e1->obj && e1->obj->name && strlen(e1->obj->name)) {
        name1 = e1->obj->name;
    }
    if (e2 && e2->obj && e2->obj->name && strlen(e2->obj->name)) {
        name2 = e2->obj->name;
    }
    if (name1 == name2) {
        return (0);
    }
    if (name1 == NULL) {
        return (1);
    }
    if (name2 == NULL) {
        return (-1);
    }
    return (strcmp(name1, name2));
}

/**
 * Comparator function for midonet_api_ipaddrgroup structures. Comparison is base on name property.
 * @param p1 [in] pointer to midonet_api_ipaddrgroup pointer 1.
 * @param p2 [in] pointer to midonet_api_ipaddrgroup pointer 2.
 * @return 0 iff p1->.->obj->name == p2->.->obj->name. -1 iff p1->.->obj->name < p2->.->obj->name.
 * 1 iff p1->.->obj->name > p2->.->obj->name.
 * NULL is considered larger than a non-NULL string.
 */
int compare_midonet_api_ipaddrgroup(const void *p1, const void *p2) {
    midonet_api_ipaddrgroup **pp1 = NULL;
    midonet_api_ipaddrgroup **pp2 = NULL;
    midonet_api_ipaddrgroup *e1 = NULL;
    midonet_api_ipaddrgroup *e2 = NULL;
    char *name1 = NULL;
    char *name2 = NULL;

    if ((p1 == NULL) || (p2 == NULL)) {
        LOGWARN("Invalid argument: cannot compare NULL midonet_api_ipaddrgroup\n");
        return (0);
    }
    pp1 = (midonet_api_ipaddrgroup **) p1;
    pp2 = (midonet_api_ipaddrgroup **) p2;
    e1 = *pp1;
    e2 = *pp2;
    if (e1 && e1->obj && e1->obj->name && strlen(e1->obj->name)) {
        name1 = e1->obj->name;
    }
    if (e2 && e2->obj && e2->obj->name && strlen(e2->obj->name)) {
        name2 = e2->obj->name;
    }
    if (name1 == name2) {
        return (0);
    }
    if (name1 == NULL) {
        return (1);
    }
    if (name2 == NULL) {
        return (-1);
    }
    return (strcmp(name1, name2));
}

/**
 * Comparator function for midonet_api_chain structures. Comparison is base on name property.
 * @param p1 [in] pointer to midonet_api_chain pointer 1.
 * @param p2 [in] pointer to midonet_api_chain pointer 2.
 * @return 0 iff p1->.->obj->name == p2->.->obj->name. -1 iff p1->.->obj->name < p2->.->obj->name.
 * 1 iff p1->.->obj->name > p2->.->obj->name.
 * NULL is considered larger than a non-NULL string.
 */
int compare_midonet_api_chain(const void *p1, const void *p2) {
    midonet_api_chain **pp1 = NULL;
    midonet_api_chain **pp2 = NULL;
    midonet_api_chain *e1 = NULL;
    midonet_api_chain *e2 = NULL;
    char *name1 = NULL;
    char *name2 = NULL;

    if ((p1 == NULL) || (p2 == NULL)) {
        LOGWARN("Invalid argument: cannot compare NULL midonet_api_chain\n");
        return (0);
    }
    pp1 = (midonet_api_chain **) p1;
    pp2 = (midonet_api_chain **) p2;
    e1 = *pp1;
    e2 = *pp2;
    if (e1 && e1->obj && e1->obj->name && strlen(e1->obj->name)) {
        name1 = e1->obj->name;
    }
    if (e2 && e2->obj && e2->obj->name && strlen(e2->obj->name)) {
        name2 = e2->obj->name;
    }
    if (name1 == name2) {
        return (0);
    }
    if (name1 == NULL) {
        return (1);
    }
    if (name2 == NULL) {
        return (-1);
    }
    return (strcmp(name1, name2));
}

#ifdef MIDONET_API_TEST
/**
 *
 *
 * @param argc [in]
 * @param argv [in]
 *
 * @return
 *
 * @note
 */
int main(int argc, char **argv) {
    exit(0);
}
#endif
