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
//! @file util/template.c
//! Template source file
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
#include <json/json.h>

#include <eucalyptus.h>
#include <misc.h>
#include <vnetwork.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <ipt_handler.h>
#include <atomic_file.h>

//#include "eucanetd.h"
//#include "config-eucanetd.h"
#include "globalnetwork.h"
#include "midonet-api.h"

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

void mido_free_midoname(midoname *name) {
    EUCA_FREE(name->name);
    EUCA_FREE(name->uuid);
    EUCA_FREE(name->tenant);
    EUCA_FREE(name->jsonbuf);
    EUCA_FREE(name->resource_type);
    EUCA_FREE(name->content_type);
}

int mido_create_tenant(char *name, midoname *outname) {
    return(0);    
}
int mido_read_tenant(midoname *name) {
    return(0);
}

int mido_update_tenant(midoname *name) {
    return(0);
}

int mido_delete_tenant(midoname *name) {
    return(mido_delete_resource("tenants", name));
}

int mido_create_router(char *tenant, char *name, midoname *outname) {
    int rc;
    midoname myname = {0,0,0,0,0};
    /*
    midoname parentname = {0,0,0,0,0};

    parentname.tenant = strdup(tenant);
    parentname.name = strdup(tenant);
    parentname.uuid = strdup(tenant);
    parentname.resource_type = strdup("tenants");
    parentname.content_type = strdup("Tenant");
    parentname.jsonbuf = NULL;
    */
    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("routers");
    myname.content_type = strdup("Router");
    
    rc = mido_create_resource(NULL, &myname, outname, "name", "string", myname.name, NULL);

    mido_free_midoname(&myname);
    //    mido_free_midoname(&parentname);
    return(rc);
}
int mido_read_router(midoname *name) {
    return(mido_read_resource("routers", name));
}
int mido_update_router(midoname *name, ...) {
    va_list al = { {0} };
    int ret=0;
    va_start(al, name);
    ret = mido_update_resource("routers", "Router", name, &al);
    va_end(al);

    return(ret);
}

int mido_print_router(midoname *name) {
    return(mido_print_resource("routers", name));
}

int mido_delete_router(midoname *name) {
    return(mido_delete_resource("routers", name));
}

int mido_create_bridge(char *tenant, char *name, midoname *outname) {
    int rc;
    midoname myname = {0,0,0,0,0};

    /*
      midoname parentname = {0,0,0,0,0};

      parentname.tenant = strdup(tenant);
      parentname.name = strdup(tenant);
      parentname.uuid = strdup(tenant);
      parentname.resource_type = strdup("tenants");
      parentname.content_type = strdup("Tenant");
      parentname.jsonbuf = NULL;
    */

    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("bridges");
    myname.content_type = strdup("Bridge");

    rc = mido_create_resource(NULL, &myname, outname, "name", "string", myname.name, NULL);

    mido_free_midoname(&myname);
    return(rc);
}

int mido_read_bridge(midoname *name) {
    return(mido_read_resource("bridges", name));
}

int mido_update_bridge(midoname *name, ...) {
    va_list al = { {0} };
    int ret=0;
    va_start(al, name);
    ret = mido_update_resource("bridges", "Bridge", name, &al);
    va_end(al);

    return(ret);
}

int mido_print_bridge(midoname *name) {
    return(mido_print_resource("bridges", name));
}

int mido_delete_bridge(midoname *name) {
    return(mido_delete_resource("bridges", name));
}

int mido_create_chain(char *tenant, char *name, midoname *outname) {
    int rc;
    midoname myname = {0,0,0,0,0};

    myname.tenant = strdup(tenant);
    myname.name = strdup(name);
    myname.resource_type = strdup("chains");
    myname.content_type = strdup("Chain");
    
    rc = mido_create_resource(NULL, &myname, outname, "name", "string", myname.name, NULL);
    
    mido_free_midoname(&myname);
    return(rc);
}

int mido_read_chain(midoname *name) {
    return(mido_read_resource("chains", name));
}

int mido_update_chain(midoname *name, ...) {
    va_list al = { {0} };
    int ret=0;
    va_start(al, name);
    ret = mido_update_resource("chains", "Chain", name, &al);
    va_end(al);

    return(ret);
}

int mido_print_chain(midoname *name) {
    return(mido_print_resource("chains", name));
}

int mido_delete_chain(midoname *name) {
    return(mido_delete_resource("chains", name));
}

int mido_create_rule(midoname *chain, char *type, char *src, char *src_slashnet, char *src_ports, char *dst, char *dst_slashnet, char *dst_ports, char *action, char *nat_target, char *nat_port_min, char *nat_port_max, midoname *outname) {
    //int mido_create_port(midoname *devname, char *port_type, midoname *outname) {
    int rc;
    midoname myname = {0,0,0,0,0};
    
    myname.tenant = strdup(chain->tenant);
    myname.resource_type = strdup("rules");
    myname.content_type = strdup("Rule");
    
    /*
      {"fragmentPolicy": "unfragmented", "nwTos": "0", "nwProto": "0", "flowAction": "accept", "nwSrcLength": 32, "nwSrcAddress": "192.168.60.1", "invNwProto": false, "invNwSrc": false, "position": "1", "invNwTos": false, "natTargets": [{"addressTo": "10.111.5.57", "addressFrom": "10.111.5.57", "portTo": 9999, "portFrom": 1}], "type": "snat"}


SNAT rule
{"type": "snat", "nwSrcLength": 32, "nwSrcAddress": "192.168.1.2", "invNwSrc": false, "natTargets": [{"addressTo": "10.111.200.11", "addressFrom": "10.111.200.11", "portTo": 0, "portFrom": 0}], "flowAction": "accept"}

DNAT rule
{"invNwDst": false, "nwDstAddress": "10.111.200.11", "nwDstLength": 32, "type": "dnat", "natTargets": [{"addressTo": "192.168.1.2", "addressFrom": "192.168.1.2", "portTo": 0, "portFrom": 0}], "flowAction": "accept"}


    */
    
    rc = mido_create_resource(chain, &myname, outname, "type", "string", type, "flowAction", "string", action, "nwSrcAddress", "string", src, "nwSrcLength", "string", src_slashnet, "nwDstAddress", "string", dst, "nwDstLength", "string", dst_slashnet, "natTargets", "string", "jsonlist", "natTargets:addressTo", "string", nat_target, "natTargets:addressFrom", "string", nat_target, "natTargets:portTo", "int", nat_port_max, "natTargets:portFrom", "int", nat_port_min, NULL);
    
    mido_free_midoname(&myname);
    return(rc);
}
int mido_read_rule(midoname *name) {
    return(mido_read_resource("ports", name));
}
int mido_update_rule(midoname *name, ...) {
    return(0);
}
int mido_print_rule(midoname *name) {
    return(mido_print_resource("ports", name));
}
int mido_delete_rule(midoname *name) {
    return(mido_delete_resource("ports", name));
}

int mido_create_port(midoname *devname, char *port_type, char *ip, char *nw, char *slashnet, midoname *outname) {
    //int mido_create_port(midoname *devname, char *port_type, midoname *outname) {
    int rc;
    midoname myname = {0,0,0,0,0};

    myname.tenant = strdup(devname->tenant);
    myname.resource_type = strdup("ports");
    myname.content_type = strdup("Port");
    
    //{"type":"InteriorRouter","portAddress":"1.2.3.4","networkAddress":"1.2.3.0","networkLength":"24","tenantId":"euca_tenant_0"}
    if (ip && nw && slashnet) {
        rc = mido_create_resource(devname, &myname, outname, "type", "string", port_type, "portAddress", "string", ip, "networkAddress", "string", nw, "networkLength", "int", slashnet, NULL);
    } else {
        rc = mido_create_resource(devname, &myname, outname, "type", "string", port_type, NULL);
    }
    
    mido_free_midoname(&myname);
    return(rc);
}
int mido_read_port(midoname *name) {
    return(mido_read_resource("ports", name));
}
int mido_update_port(midoname *name, ...) {
    return(0);
}
int mido_print_port(midoname *name) {
    return(mido_print_resource("ports", name));
}
int mido_delete_port(midoname *name) {
    return(mido_delete_resource("ports", name));
}

int mido_unlink_host_port(midoname *host, midoname *port) {
    int rc=0, ret=0;
    char url[EUCA_MAX_PATH];

    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/hosts/%s/ports/%s", host->uuid, port->uuid);
    rc = midonet_http_delete(url);
    if (rc) {
        ret = 1;
    }
    return(ret);
}

int mido_link_host_port(midoname *host, char *interface, midoname *device, midoname *port) {

    int rc;
    midoname myname;
    bzero(&myname, sizeof(midoname));
    /*
      midoname parentname = {0,0,0,0,0};

      parentname.tenant = strdup(tenant);
      parentname.name = strdup(tenant);
      parentname.uuid = strdup(tenant);
      parentname.resource_type = strdup("tenants");
      parentname.content_type = strdup("Tenant");
      parentname.jsonbuf = NULL;
    */

    myname.tenant = strdup(device->tenant);
    myname.name = strdup("port");
    myname.resource_type = strdup("ports");
    myname.content_type = NULL;

    rc = mido_create_resource(host, &myname, NULL, "bridgeId", "string", device->uuid, "portId", "string", port->uuid, "hostId", "string", host->uuid, "interfaceName", "string", interface, NULL);
    
    mido_free_midoname(&myname);

    return(rc);
}

int mido_link_ports(midoname *a, midoname *b) {
    int rc;
    midoname myname;
    bzero(&myname, sizeof(midoname));
    /*
      midoname parentname = {0,0,0,0,0};

      parentname.tenant = strdup(tenant);
      parentname.name = strdup(tenant);
      parentname.uuid = strdup(tenant);
      parentname.resource_type = strdup("tenants");
      parentname.content_type = strdup("Tenant");
      parentname.jsonbuf = NULL;
    */

    myname.tenant = strdup(a->tenant);
    myname.name = strdup("link");
    myname.resource_type = strdup("link");
    myname.content_type = NULL;

    rc = mido_create_resource(a, &myname, NULL, "peerId", "string", b->uuid, NULL);

    mido_free_midoname(&myname);

    return(rc);
}

int mido_update_resource(char *resource_type, char *content_type, midoname *name, va_list *al) {
    char url[EUCA_MAX_PATH];
    int rc=0, ret=0;
    char *key=NULL, *val=NULL;
    struct json_object *jobj=NULL, *el=NULL;
    
    jobj = json_tokener_parse(name->jsonbuf);
    if (jobj) {
        //json_object_object_foreach(jobj, mkey, mval) {
        //printf("\t%s: %s\n", mkey, SP(json_object_get_string(mval)));
        //}
        //        json_object_put(jobj);                
        key = va_arg(*al, char *);
        if (key) val = va_arg(*al, char *);
        while(key && val) {
            el = json_object_object_get(jobj, key);
            if (el) {
                json_object_object_add(jobj, key, json_object_new_string(val));
                json_object_put(el);
            } else {
                json_object_object_add(jobj, key, json_object_new_string(val));
            }
            key = va_arg(*al, char *);
            if (key) val = va_arg(*al, char *);            
        }

        EUCA_FREE(name->jsonbuf);
        name->jsonbuf = strdup(json_object_to_json_string(jobj));
        json_object_put(jobj);
        ret = mido_update_midoname(name);
    } else {
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    }

    // ready to send the http_put
    if (!ret) {
        snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s", resource_type, name->uuid);
        rc = midonet_http_put(url, content_type, name->jsonbuf);
        if (rc) {
            ret = 1;
        }
    }
    return(ret);
}

int mido_read_resource(char *resource_type, midoname *name) {
    char url[EUCA_MAX_PATH], *outhttp=NULL;
    int rc=0, ret=0;

    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s", resource_type, name->uuid);
    rc = midonet_http_get(url, &outhttp);
    if (rc) {
        ret = 1;
    } else {
        EUCA_FREE(name->jsonbuf);
        name->jsonbuf = strdup(outhttp);
        ret = mido_update_midoname(name);
    }
    EUCA_FREE(outhttp);
    return(ret);
}

int mido_print_resource(char *resource_type, midoname *name) {
    int ret=0;
    struct json_object *jobj=NULL;

    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    } else {
        printf("TYPE: %s NAME: %s UUID: %s\n", resource_type, SP(name->name), name->uuid);
        json_object_object_foreach(jobj, key, val) {
            printf("\t%s: %s\n", key, SP(json_object_get_string(val)));
        }
        json_object_put(jobj);                
    }

    return(ret);
}


int mido_create_resource(midoname *parentname, midoname *newname, midoname *outname, ...) {
    int ret=0, rc=0;
    char url[EUCA_MAX_PATH];
    char *outloc=NULL, *outhttp=NULL, *payload=NULL;
    va_list al = { {0} };
    struct json_object *jobj=NULL, *jobj_sublist=NULL, *jarr_sublist=NULL;
    char *key=NULL, *type=NULL, *val=NULL, *listtag=NULL;

    if (outname) { 
        bzero(outname, sizeof(midoname));
    }

    //  construct the payload
    va_start(al, outname);
    
    jobj = json_object_new_object();
    if (!jobj) {
        printf("ERROR: json_object_new_object(...): returned NULL\n");
        ret = 1;
    } else {
        json_object_object_add(jobj, "tenantId", json_object_new_string(newname->tenant));
        key = va_arg(al, char *);
        if (key) type = va_arg(al, char *);
        if (key && type) val = va_arg(al, char *);
        while(key && type && val) {
            if (!strcmp(val, "UNSET")) {
            } else {
                if (!strcmp(val, "jsonlist")) {
                    //                printf("WTF: will create a sublist for element %s\n", key);
                    EUCA_FREE(listtag);
                    listtag = strdup(key);
                    jobj_sublist = json_object_new_object();
                } else if (listtag && strstr(key, listtag)) {
                    char *subkey=NULL;
                    //                printf("WTF: current listtag %s element %s/%s\n", listtag, key, val);
                    subkey = strchr(key, ':');
                    subkey++;
                    if (!strcmp(type, "string")) {
                        json_object_object_add(jobj_sublist, subkey, json_object_new_string(val));
                    } else if (!strcmp(type, "int")) {
                        json_object_object_add(jobj_sublist, subkey, json_object_new_int(atoi(val)));
                    }
                } else {
                    if (listtag) {
                        jarr_sublist = json_object_new_array();
                        json_object_array_add(jarr_sublist, jobj_sublist);
                        json_object_object_add(jobj, listtag, jarr_sublist);
                        EUCA_FREE(listtag);
                        listtag = NULL;
                    }
                    //                printf("WTF: normal key/val: %s/%s\n", key, val);
                    if (!strcmp(type, "string")) {
                        json_object_object_add(jobj, key, json_object_new_string(val));
                    } else if (!strcmp(type, "int")) {
                        json_object_object_add(jobj, key, json_object_new_int(atoi(val)));
                    }
                    //                json_object_object_add(jobj, key, json_object_new_string(val));
                }
            }
            key = va_arg(al, char *);
            if (key) type = va_arg(al, char *);
            if (key && type) val = va_arg(al, char *);
        }
        if (listtag) {
            //            printf("WTF WOTO2: %s/%s\n", listtag, json_object_to_json_string(jobj_sublist));
            jarr_sublist = json_object_new_array();
            json_object_array_add(jarr_sublist, jobj_sublist);
            json_object_object_add(jobj, listtag, jarr_sublist);
            //json_object_put(jarr_sublist);
            //json_object_put(jobj_sublist);
            EUCA_FREE(listtag);
            listtag = NULL;
        }
        //        printf("JSON: %s\n", json_object_to_json_string(jobj));
        payload = strdup(json_object_to_json_string(jobj));
        json_object_put(jobj);
    }
    va_end(al);

    //    snprintf(content, EUCA_MAX_PATH, "{\"name\": \"%s\", \"tenantId\": \"%s\"}", newname->name, newname->tenant);
    //    snprintf(content, EUCA_MAX_PATH, "{\"type\": \"%s\", \"tenantId\": \"%s\"}", "InteriorBridge", parentname->tenant);
    if (!parentname) {
        snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s", newname->resource_type);
    } else {
        snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s/%s", parentname->resource_type, parentname->uuid, newname->resource_type);
    }

    // perform the create
    rc = midonet_http_post(url, newname->content_type, payload, &outloc);
    if (rc) {
        printf("ERROR: midonet_http_post(%s, ...) failed\n", url);
        ret = 1;
    }


    // if all goes well, store the new resource
    if (!ret) {
        if (outloc) {
            rc = midonet_http_get(outloc, &outhttp);
            if (rc) {
                ret = 1;
            } else if (outname) {
                EUCA_FREE(outname->jsonbuf);
                outname->tenant = strdup(newname->tenant);
                outname->jsonbuf = strdup(outhttp);
                outname->resource_type = strdup(newname->resource_type);
                outname->content_type = strdup(newname->content_type);
                ret = mido_update_midoname(outname);
            }
        }
    }

    EUCA_FREE(payload);
    EUCA_FREE(outhttp);
    EUCA_FREE(outloc);
    return(ret);

}

void mido_copy_midoname(midoname *dst, midoname *src) { 
    if (!dst || !src) {
        return;
    }
    bzero(dst, sizeof(midoname));

    if (src->tenant) dst->tenant = strdup(src->tenant);
    if (src->name) dst->name = strdup(src->name);
    if (src->uuid) dst->uuid = strdup(src->uuid);
    if (src->jsonbuf) dst->jsonbuf = strdup(src->jsonbuf);
    if (src->resource_type) dst->resource_type = strdup(src->resource_type);
    if (src->content_type) dst->content_type = strdup(src->content_type);
}

int mido_update_midoname(midoname *name) {
    int ret=0;
    struct json_object *jobj=NULL, *el=NULL;

    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        //        printf("WTF: %s\n", SP(name->jsonbuf));
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    } else {
        el = json_object_object_get(jobj, "id");
        if (el) {
            EUCA_FREE(name->uuid);
            name->uuid = strdup(json_object_get_string(el));
            json_object_put(el);                
        } 
        
        el = json_object_object_get(jobj, "tenantId");
        if (el) {
            EUCA_FREE(name->tenant);
            name->tenant = strdup(json_object_get_string(el));
            json_object_put(el);
        } 
        
        el = json_object_object_get(jobj, "name");
        if (el) {
            EUCA_FREE(name->name);
            name->name = strdup(json_object_get_string(el));
            json_object_put(el);
        } 

        json_object_put(jobj);
    }
    return(ret);
}

int mido_delete_resource(char *resource_type, midoname *name) {
    int rc=0, ret=0;
    char url[EUCA_MAX_PATH];

    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s/%s", resource_type, name->uuid);
    rc = midonet_http_delete(url);
    if (rc) {
        ret = 1;
    }
    return(ret);
}


struct mem_params_t {
    char *mem;
    size_t size;
};

static size_t mem_writer(void *contents, size_t size, size_t nmemb, void *in_params) {
    struct mem_params_t *params = (struct mem_params_t *)in_params;
    
    if (!params->mem) {
        params->mem = malloc(1);
    }
    params->mem = realloc(params->mem, params->size + (size * nmemb) + 1);
    if (params->mem == NULL) {
        return(0);
    }
    memcpy(&(params->mem[params->size]), contents, size * nmemb);
    params->size += size * nmemb;
    params->mem[params->size] = '\0';

    return(size * nmemb);
}

static size_t mem_reader(void *contents, size_t size, size_t nmemb, void *in_params) {
    struct mem_params_t *params = (struct mem_params_t *)in_params;
    size_t bytes_to_copy = 0;

    if (!params->mem || params->size <= 0) {
        return(0);
    }
    
    if (!contents) {
        printf("ERROR: no mem to write into\n");
        params->size = 0;
        return(0);
    }

    bytes_to_copy = (params->size < (size * nmemb)) ? params->size : (size*nmemb);
    
    memcpy(contents, params->mem, bytes_to_copy);
    params->size -= bytes_to_copy;
    params->mem += bytes_to_copy;

    return(bytes_to_copy);
}

int midonet_http_get(char *url, char **out_payload) { 
    CURL *curl=NULL;
    CURLcode curlret;
    struct mem_params_t mem_writer_params = {0,0};
    int ret = 0;
    long httpcode = 0L;

    *out_payload = NULL;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, mem_writer);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&mem_writer_params);
    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if (httpcode != 200L) {
        ret = 1;
    }
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    // convert to payload out
    
    if (!ret) {
        if (mem_writer_params.mem && mem_writer_params.size > 0) {
            *out_payload = malloc(sizeof(char) * mem_writer_params.size + 1);
            memcpy(*out_payload, mem_writer_params.mem, mem_writer_params.size+1);
        } else {
            printf("ERROR: no data to return after successful curl operation\n");
            ret = 1;
        }
    }
    if (mem_writer_params.mem) free(mem_writer_params.mem);
    return(ret);
}

int midonet_http_put(char *url, char *resource_type, char *payload) {
    CURL *curl=NULL;
    CURLcode curlret;
    struct mem_params_t mem_reader_params = {0,0};
    char hbuf[EUCA_MAX_PATH];
    struct curl_slist *headers = NULL;
    int ret = 0;
    long httpcode = 0L;

    mem_reader_params.mem = payload;
    mem_reader_params.size = strlen(payload)+1;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt(curl, CURLOPT_PUT, 1L);
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, mem_reader);
    curl_easy_setopt(curl, CURLOPT_READDATA, (void *)&mem_reader_params);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (long)mem_reader_params.size);
    snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/vnd.org.midonet.%s-v1+json", resource_type);
    headers = curl_slist_append(headers, hbuf);
    snprintf(hbuf, EUCA_MAX_PATH, "Expect:");
    headers = curl_slist_append(headers, hbuf);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
    if (httpcode != 200L) {
        ret = 1;
    }

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    // convert to payload out
    
    /*    if (!ret) {
        if (mem_writer_params.mem && mem_writer_params.size > 0) {
            *out_payload = malloc(sizeof(char) * mem_writer_params.size);
            memcpy(*out_payload, mem_reader_params.mem, mem_reader_params.size);
        } else {
            printf("ERROR: no data to return after successful curl operation\n");
            ret = 1;
        }
    }
    */
    //    if (mem_reader_params.mem) free(mem_reader_params.mem);
    return(ret);    
}


static size_t header_find_location(char *content, size_t size, size_t nmemb, void *params) {
    char *buf=NULL;
    char **loc = (char **)params;
    
    buf = malloc(sizeof(char) * (size * nmemb) + 1 );
    memcpy(buf, content, size * nmemb);
    buf[size*nmemb] = '\0';
    
    if (buf && strstr(buf, "Location: ")) {
        *loc = malloc(sizeof(char) * strlen(buf));
        sscanf(buf, "Location: %s", *loc);
    }
    free(buf);

    return(size * nmemb);
}

int midonet_http_post(char *url, char *resource_type, char *payload, char **out_payload) {
    CURL *curl=NULL;
    CURLcode curlret;
    int ret = 0;
    char *loc=NULL, hbuf[EUCA_MAX_PATH];
    struct curl_slist *headers = NULL;

    *out_payload = NULL;

    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    //    curl_easy_setopt(curl, CURLOPT_HEADER, 1L);
    curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
    curl_easy_setopt(curl, CURLOPT_POST, 1L);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, payload);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, strlen(payload));
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_find_location);
    curl_easy_setopt(curl, CURLOPT_HEADERDATA, &loc);
    if (!resource_type || strlen(resource_type) <= 0) {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/json");
    } else {
        snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/vnd.org.midonet.%s-v1+json", resource_type);
    }
    headers = curl_slist_append(headers, hbuf);
    //    headers = curl_slist_append(headers, "Content-Type: application/vnd.org.midonet");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    if (!ret) {
        if (loc) {
            *out_payload = strdup(loc);
        }
    }
    EUCA_FREE(loc);

    return(ret);
}


int midonet_http_delete(char *url) {
    CURL *curl=NULL;
    CURLcode curlret;
    int ret = 0;
    
    curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");
    curlret = curl_easy_perform(curl);
    if (curlret != CURLE_OK) {
        printf("ERROR: curl_easy_perform(): %s\n", curl_easy_strerror(curlret));
        ret = 1;
    }
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    return(ret);
}

int mido_router_create_route(midoname *router, midoname *rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip) {
    int rc;
    midoname myname;
    bzero(&myname, sizeof(midoname));

    myname.tenant = strdup(router->tenant);
    myname.resource_type = strdup("routes");
    myname.content_type = NULL;
    
    //{"type":"InteriorRouter","portAddress":"1.2.3.4","networkAddress":"1.2.3.0","networkLength":"24","tenantId":"euca_tenant_0"}
    //{"srcNetworkAddr":"0.0.0.0","srcNetworkLength":0,"dstNetworkAddr":"192.168.59.0","dstNetworkLength":24,"type":"Normal","nextHopPort":"e604e4de-b365-400d-ba13-e204483dd936","nextHopGateway":"192.168.59.1","weight":0,"position":2}

    if (next_hop_ip) {
        rc = mido_create_resource(router, &myname, NULL, "srcNetworkAddr", "string", src, "srcNetworkLength", "int", src_slashnet, "dstNetworkAddr", "string", dst, "dstNetworkLength", "int", dst_slashnet, "type", "string", "Normal", "nextHopPort", "string", rport->uuid, "weight", "int", "0", "nextHopGateway", "string", next_hop_ip, NULL);
    } else {
        rc = mido_create_resource(router, &myname, NULL, "srcNetworkAddr", "string", src, "srcNetworkLength", "int", src_slashnet, "dstNetworkAddr", "string", dst, "dstNetworkLength", "int", dst_slashnet, "type", "string", "Normal", "nextHopPort", "string", rport->uuid, "weight", "int", "0", NULL);
    }
    
    mido_free_midoname(&myname);
    return(rc);
}



int mido_get_hosts(midoname **outnames, int *outnames_max) {
    int rc=0, ret=0, i=0, hostup=0;
    char *payload=NULL, url[EUCA_MAX_PATH];

    *outnames = NULL;
    *outnames_max = 0;
        
    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/hosts");
    rc = midonet_http_get(url, &payload);
    if (!rc) {
        //        printf("WTF: %s\n", payload);
        struct json_object *jobj=NULL, *host=NULL, *el=NULL;
        
        jobj = json_tokener_parse(payload);
        if (!jobj) {
            printf("NOU\n");
        } else {
            if (json_object_is_type(jobj, json_type_array)) {
                //                printf("HMM: %s, %d\n", json_object_to_json_string(jobj), json_object_array_length(jobj));
                *outnames_max = 0;
                *outnames = malloc(sizeof(midoname) * json_object_array_length(jobj));
                for (i=0; i<json_object_array_length(jobj); i++) {
                    bzero(&(*outnames)[i], sizeof(midoname));
                    host = json_object_array_get_idx(jobj, i);
                    if (host) {
                        json_object_object_foreach(host, key, val) {
                            printf("\t%s: %s\n", key, SP(json_object_get_string(val)));
                        }

                        el = json_object_object_get(host, "alive");
                        if (el) {
                            if (!strcmp(json_object_get_string(el), "false")) {
                                // host is down, skip
                                hostup = 0;
                            } else {
                                hostup = 1;
                            }
                            json_object_put(el);
                        }

                        if (hostup) {
                            (*outnames)[*outnames_max].jsonbuf = strdup(json_object_to_json_string(host));

                            el = json_object_object_get(host, "id");
                            if (el) {
                                (*outnames)[*outnames_max].uuid = strdup(json_object_get_string(el));
                                json_object_put(el);
                            }
                            
                            el = json_object_object_get(host, "name");
                            if (el) {
                                (*outnames)[*outnames_max].name = strdup(json_object_get_string(el));
                                json_object_put(el);
                            }


                            (*outnames)[*outnames_max].resource_type = strdup("hosts");
                            (*outnames)[*outnames_max].content_type = NULL;
                            *outnames_max = *outnames_max + 1;
                        }
                        json_object_put(host);
                    }
                }
            }
            json_object_put(jobj);
        }

        EUCA_FREE(payload);
    }
    return(ret);
}

#ifdef MIDONET_API_TEST
int main(int argc, char **argv) {
    int rc, i, j;

  /*
    TODO
//     - routing table setup ops
//     - write test setup (eucart, eucabr, vpcrt_X, vpcbr_X, all links)
//     - manually link up and verify VM->rt
//     - host ops
//     - ext vm <-> bridge port linking
//     - ext GW <-> rt port linking
//     - chain ops
//     - el. IP chain/routing setup
     - no pub IP masq chain setup
//     - manually link up and verify pub->priv VM end-to-end
     - DHCP and META taps and links
     - test DHCP and META taps
     - populate operations for re-pop and automation
     - euca integration begin
     - new XML parse and conversion
     - NC interface naming and such on run
     - dhcp/meta tap creation and such
     - end-to-end testing
  */
  for (j=0; j<1000; j++) {
  midoname eucabr, eucart, eucart_brport, eucabr_rtport, eucart_gwport;
  midoname vpcbr, vpcrt, vpcrt_brport, vpcbr_rtport;
  midoname vpcrt_uplink, eucabr_downlink;
  midoname vm_port_a, vm_port_b;
  midoname *names, vmhost, rthost;
  midoname vpcrt_prechain, vpcrt_postchain, vmrule_elip_dnat, vmrule_elip_snat;
  int max;

  { 
      // populate hosts

      for (i=0; i<1; i++) {
          rc = mido_get_hosts(&names, &max);
          
      }
      for (i=0; i<max; i++) {
          printf("I: %d S: %s U: %s\n", i, names[i].name, names[i].uuid);
          if (strstr(names[i].name, "a-12.qa1")) {
              mido_copy_midoname(&vmhost, &(names[i]));
          }
          if (strstr(names[i].name, "a-15.qa1")) {
              mido_copy_midoname(&rthost, &(names[i]));
          }
      }
      //      exit(0);
  }
  printf("FOUND VM HOST: %s/%s\n", vmhost.name, vmhost.uuid);
  printf("FOUND RT HOST: %s/%s\n", rthost.name, rthost.uuid);

  {
      // set up euca routing GW

      rc = mido_create_router("euca_tenant_0", "eucart", &eucart);
      rc = mido_create_bridge("euca_tenant_0", "eucabr", &eucabr);
      rc = mido_create_port(&eucabr, "InteriorBridge", NULL, NULL, NULL, &eucabr_rtport);
      rc = mido_create_port(&eucart, "InteriorRouter", "192.168.254.1", "192.168.254.0", "24", &eucart_brport);
      rc = mido_router_create_route(&eucart, &eucart_brport, "0.0.0.0", "0", "192.168.254.0", "24", NULL);  
      rc = mido_link_ports(&eucart_brport, &eucabr_rtport);

      rc = mido_create_port(&eucart, "ExteriorRouter", "10.111.5.57", "10.111.0.0", "16", &eucart_gwport);
      rc = mido_router_create_route(&eucart, &eucart_gwport, "0.0.0.0", "0", "10.111.0.0", "16", NULL);
      rc = mido_router_create_route(&eucart, &eucart_gwport, "0.0.0.0", "0", "0.0.0.0", "0", "10.111.5.34");
      rc = mido_link_host_port(&rthost, "em1", &eucart, &eucart_gwport);      
  }

  {
      // set up example VPC

      rc = mido_create_router("euca_tenant_0", "vpcrt_12345678", &vpcrt);
      rc = mido_create_bridge("euca_tenant_0", "vpcbr_12345678", &vpcbr);
      rc = mido_create_port(&vpcbr, "InteriorBridge", NULL, NULL, NULL, &vpcbr_rtport);
      rc = mido_create_port(&vpcrt, "InteriorRouter", "192.168.1.1", "192.168.1.0", "24", &vpcrt_brport);
      rc = mido_router_create_route(&vpcrt, &vpcrt_brport, "0.0.0.0", "0", "192.168.1.0", "24", NULL);
      rc = mido_link_ports(&vpcrt_brport, &vpcbr_rtport);
      
  }

  {
      // link the vpc network and euca network

      rc = mido_create_port(&eucabr, "InteriorBridge", NULL, NULL, NULL, &eucabr_downlink);
      rc = mido_create_port(&vpcrt, "InteriorRouter", "192.168.254.2", "192.168.254.0", "24", &vpcrt_uplink);
      rc = mido_router_create_route(&vpcrt, &vpcrt_uplink, "0.0.0.0", "0", "192.168.254.0", "24", NULL);
      rc = mido_router_create_route(&vpcrt, &vpcrt_uplink, "0.0.0.0", "0", "0.0.0.0", "0", "192.168.254.1");
      rc = mido_link_ports(&eucabr_downlink, &vpcrt_uplink);
  }

  {
      // setup VM ports

      // create the Exterior ports for VMs
      rc = mido_create_port(&vpcbr, "ExteriorBridge", NULL, NULL, NULL, &vm_port_a);
      rc = mido_create_port(&vpcbr, "ExteriorBridge", NULL, NULL, NULL, &vm_port_b);

      // link vm host port to vm bridge port
      rc = mido_link_host_port(&vmhost, "vn_abcdefgh", &vpcbr, &vm_port_a);
      
  }

  {
      // create pre/post chains and DNAT/SNAT rules

      // create the chains
      rc = mido_create_chain("euca_tenant_0", "vpc_12345678_pre", &vpcrt_prechain);
      rc = mido_create_chain("euca_tenant_0", "vpc_12345678_post", &vpcrt_postchain);
      
      // create the rules
      rc = mido_create_rule(&vpcrt_prechain, "dnat", "UNSET", "UNSET", "UNSET", "10.111.200.11", "32", "UNSET", "accept", "192.168.1.2", "0", "0", &vmrule_elip_dnat);
      rc = mido_create_rule(&vpcrt_postchain, "snat", "192.168.1.2", "32", "UNSET", "UNSET", "UNSET", "UNSET", "accept", "10.111.200.11", "0", "0", &vmrule_elip_snat);

      // apply the chains to the vpc router
      rc = mido_update_router(&vpcrt, "inboundFilterId", vpcrt_prechain.uuid, NULL);
      rc = mido_update_router(&vpcrt, "outboundFilterId", vpcrt_postchain.uuid, NULL);

      // create the EL ip route in main router
      rc = mido_router_create_route(&eucart, &eucart_brport, "0.0.0.0", "0", "10.111.200.11", "32", "192.168.254.2");
      
  }

  //scanf("%d", &i);

  // note; must unlink before delete
  rc = mido_unlink_host_port(&rthost, &eucart_gwport);  
  rc = mido_delete_router(&eucart);

  // others appear to self cleanup
  rc = mido_delete_bridge(&eucabr);
  rc = mido_delete_router(&vpcrt);
  rc = mido_delete_bridge(&vpcbr);
  rc = mido_delete_chain(&vpcrt_prechain);
  rc = mido_delete_chain(&vpcrt_postchain);
  

  /*
  midoname eucabr, eucart, eucart_brport, eucabr_rtport, eucart_gwport;
  midoname vpcbr, vpcrt, vpcrt_brport, vpcbr_rtport;
  midoname vpcrt_uplink, eucabr_downlink;
  midoname vm_port_a, vm_port_b;
  midoname *names, vmhost, rthost;
  int max;
  */

  mido_free_midoname(&eucabr);
  mido_free_midoname(&eucart);
  mido_free_midoname(&eucart_brport);
  mido_free_midoname(&eucabr_rtport);
  mido_free_midoname(&eucart_gwport);  
  mido_free_midoname(&vpcbr);
  mido_free_midoname(&vpcrt);
  mido_free_midoname(&vpcrt_brport);
  mido_free_midoname(&vpcbr_rtport);
  mido_free_midoname(&vpcrt_uplink);
  mido_free_midoname(&eucabr_downlink);
  mido_free_midoname(&vm_port_a);
  mido_free_midoname(&vm_port_b);
  mido_free_midoname(&vmhost);
  mido_free_midoname(&rthost);
  mido_free_midoname(&vpcrt_prechain);
  mido_free_midoname(&vpcrt_postchain);
  mido_free_midoname(&vmrule_elip_dnat);
  mido_free_midoname(&vmrule_elip_snat);
  for (i=0; i<max; i++) {
      mido_free_midoname(&(names[i]));
  }
  EUCA_FREE(names);

  printf("ITER: %d\n", j);
  }
  exit (0);

  /*

  //  for (i=0; i<10000; i++) {
  for (i=0; i<100; i++) {
      mybridge = malloc(sizeof(midoname));
      myrouter = malloc(sizeof(midoname));
      myport = malloc(sizeof(midoname));
      bzero(mybridge, sizeof(midoname));
      bzero(myrouter, sizeof(midoname));
      bzero(myport, sizeof(midoname));
    
      rc = mido_create_bridge("euca_tenant_0", "testbr", mybridge);
      if (rc) {
          printf("ERROR: mido_create_bridge() failed\n");
          exit(1);
      } 
      
      rc = mido_create_port(mybridge, "InteriorBridge", NULL, NULL, NULL, myport);
      mido_free_midoname(myport);

      rc = mido_create_port(mybridge, "ExteriorBridge", NULL, NULL, NULL, myport);

      
      if (mido_print_bridge(mybridge)) {
          printf("ERROR: print() failed\n");
          exit(1);
      }
      

      rc = mido_update_bridge(mybridge, "name", "goober", NULL);
      if (!rc) {
          printf("ERROR: update(goober) failed\n");
          exit(1);
      }

      rc = mido_update_bridge(mybridge, "crabby", "goober", NULL);
      if (!rc) {
          printf("ERROR: update(crabby) failed\n");
          exit(1);
      }

      rc = mido_update_bridge(mybridge, "name", "goobertygoo", NULL);
      if (!rc) {
          printf("ERROR: update(goobertyfoo) failed\n");
          exit(1);
      }
      
      rc = mido_delete_bridge(mybridge);
      if (rc) {
          printf("ERROR: mido_delete_bridge() failed\n");
          exit(1);
      }

      rc = mido_create_router("euca_tenant_0", "testrt", myrouter);
      if (rc) {
          printf("ERROR: mido_create_router() failed\n");
          exit(1);
      }
      
      
      rc = mido_print_router(myrouter);
      if (rc) {
          printf("ERROR: mido_print_router() failed\n");
          exit (1);
      }  
      
      rc = mido_read_router(myrouter);
      if (rc) {
          printf("ERROR: mido_read_router() failed\n");
          exit (1);
      }
      
      
      rc = mido_print_router(myrouter);
      if (rc) {
          printf("ERROR: mido_print_router() failed\n");
          exit (1);
      }
      
    
      rc = mido_update_router(myrouter, "name", "goober", NULL);
      if (!rc) {
          printf("ERROR: update_router(goober) failed\n");
          exit(1);
      }
      
      rc = mido_delete_router(myrouter);
      if (rc) {
          printf("ERROR: mido_delete_router() failed\n");
      }
      
      rc = mido_read_router(myrouter);
      if (!rc) {
          printf("ERROR: mido_read_router() success but should have failed\n");
          exit (1);
      }
            
      printf("I: %d\n", i);
      mido_free_midoname(mybridge);
      mido_free_midoname(myrouter);
      mido_free_midoname(myport);
      EUCA_FREE(mybridge);
      EUCA_FREE(myrouter);
      EUCA_FREE(myport);
      
  }
*/

  exit (0);
}
#endif
