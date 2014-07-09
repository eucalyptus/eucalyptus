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
    midoname myname = {0,0,0};

    myname.tenant = strdup(tenant);
    myname.name = strdup(name);

    rc = mido_create_resource("routers", "Router", &myname, outname);

    mido_free_midoname(&myname);
    return(rc);
}
int mido_read_router(midoname *name) {
    return(mido_read_resource("routers", name));
}
//int mido_update_router(midoname *name, ...);
int mido_print_router(midoname *name) {
    return(mido_print_resource("routers", name));
}
int mido_delete_router(midoname *name) {
    return(mido_delete_resource("routers", name));
}

int mido_create_bridge(char *tenant, char *name, midoname *outname) {
    int rc;
    midoname myname = {0,0,0};

    myname.tenant = strdup(tenant);
    myname.name = strdup(name);

    rc = mido_create_resource("bridges", "Bridge", &myname, outname);

    mido_free_midoname(&myname);
    return(rc);
}

int mido_read_bridge(midoname *name) {
    return(mido_read_resource("bridges", name));
}
//int mido_update_bridge(midoname *name, ...);
int mido_print_bridge(midoname *name) {
    return(mido_print_resource("bridges", name));
}
int mido_delete_bridge(midoname *name) {
    return(mido_delete_resource("bridges", name));
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
        printf("TYPE: %s NAME: %s UUID: %s\n", resource_type, name->name, name->uuid);
        json_object_object_foreach(jobj, key, val) {
            printf("\t%s: %s\n", key, SP(json_object_get_string(val)));
        }
        json_object_put(jobj);                
    }

    return(ret);
}


int mido_create_resource(char *resource_type, char *content_type, midoname *name, midoname *outname) {
    int ret=0, rc=0;
    char url[EUCA_MAX_PATH], content[EUCA_MAX_PATH];
    char *outloc=NULL, *outhttp=NULL;

    bzero(outname, sizeof(midoname));

    snprintf(content, EUCA_MAX_PATH, "{\"name\": \"%s\", \"tenantId\": \"%s\"}", name->name, name->tenant);
    snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/%s", resource_type);
    rc = midonet_http_post(url, content_type, content, &outloc);
    if (rc) {
        printf("ERROR: midonet_http_post(%s, ...) failed\n", url);
        ret = 1;
    }

    if (!ret) {
        if (outloc) {
            rc = midonet_http_get(outloc, &outhttp);
            if (rc) {
                ret = 1;
            } else {
                outname->jsonbuf = strdup(outhttp);
                ret = mido_update_midoname(outname);
            }
        }
    }
    return(ret);

}

int mido_update_midoname(midoname *name) {
    int ret=0;
    struct json_object *jobj=NULL, *el=NULL;

    jobj = json_tokener_parse(name->jsonbuf);
    if (!jobj) {
        printf("ERROR: json_tokener_parse(...): returned NULL\n");
        ret = 1;
    } else {
        el = json_object_object_get(jobj, "id");
        if (el) {
            name->uuid = strdup(json_object_get_string(el));
            json_object_put(el);                
        } 
        
        el = json_object_object_get(jobj, "tenantId");
        if (el) {
            name->tenant = strdup(json_object_get_string(el));
            json_object_put(el);
        } 
        
        el = json_object_object_get(jobj, "name");
        if (el) {
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


struct mem_writer_params_t {
    char *mem;
    size_t size;
};

static size_t mem_writer(void *contents, size_t size, size_t nmemb, void *in_params) {
    struct mem_writer_params_t *params = (struct mem_writer_params_t *)in_params;
    
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

int midonet_http_get(char *url, char **out_payload) { 
    CURL *curl=NULL;
    CURLcode curlret;
    struct mem_writer_params_t mem_writer_params = {0,0};
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
            *out_payload = malloc(sizeof(char) * mem_writer_params.size);
            memcpy(*out_payload, mem_writer_params.mem, mem_writer_params.size);
        } else {
            printf("ERROR: no data to return after successful curl operation\n");
            ret = 1;
        }
    }
    if (mem_writer_params.mem) free(mem_writer_params.mem);
    return(ret);
}

int midonet_http_put(char *url, char *payload) {
    return(0);
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
    snprintf(hbuf, EUCA_MAX_PATH, "Content-Type: application/vnd.org.midonet.%s-v1+json", resource_type);
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

#ifdef MIDONET_API_TEST
int main(int argc, char **argv) {
    ///midonet-api/bridges/6f93a7f4-2f61-46ff-9004-01c3ad153e97
  char url[EUCA_MAX_PATH];
  char *httpout=NULL;
  int rc;
  struct json_object *jobj=NULL;

  midoname mybridge, myrouter;
  
  rc = mido_create_bridge("euca_tenant_0", "testbr", &mybridge);
  if (rc) {
      printf("ERROR: mido_create_bridge() failed\n");
      exit(1);
  } 

  mido_print_bridge(&mybridge);
  
  rc = mido_delete_bridge(&mybridge);
  if (rc) {
      printf("ERROR: mido_delete_bridge() failed\n");
  }

  rc = mido_create_router("euca_tenant_0", "testrt", &myrouter);
  if (rc) {
      printf("ERROR: mido_create_router() failed\n");
      exit(1);
  }

  rc = mido_print_router(&myrouter);
  if (rc) {
      printf("ERROR: mido_print_router() failed\n");
      exit (1);
  }  

  rc = mido_read_router(&myrouter);
  if (rc) {
      printf("ERROR: mido_read_router() failed\n");
      exit (1);
  }

  rc = mido_print_router(&myrouter);
  if (rc) {
      printf("ERROR: mido_print_router() failed\n");
      exit (1);
  }

  rc = mido_delete_router(&myrouter);
  if (rc) {
      printf("ERROR: mido_delete_router() failed\n");
  }

  rc = mido_read_router(&myrouter);
  if (!rc) {
      printf("ERROR: mido_read_router() success but should have failed\n");
      exit (1);
  }

  exit (0);
  snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/bridges");
  rc = midonet_http_post(url, "Bridge", "{\"name\": \"foobar\", \"tenantId\": \"e2d78dd164ee4a81bd1428b4f48a097f\"}", &httpout);
  if (rc) {
      printf("ERROR: midonet_http_post(%s, ...) failed\n", url);
      exit(1);
  }
  printf("LOC: %s\n", httpout);
  if (!httpout) {
      exit(1);
  }

  snprintf(url, EUCA_MAX_PATH, httpout);
  rc = midonet_http_delete(url);
  if (rc) {
      printf("ERROR: midonet_http_delete(%s, ...) failed\n", url);
      exit(1);
  }
  exit(0);

  snprintf(url, EUCA_MAX_PATH, "http://localhost:8080/midonet-api/");
  rc = midonet_http_get(url, &httpout);
  if (rc) {
      printf("ERROR: midonet_http_get(%s, ...) failed\n", url);
      exit(1);
  }

  //  printf("url: %s httpout: %s\n", url, );

  //  for (int i=0; i<40000; i++) {
  jobj = json_tokener_parse(httpout);
  //  }
  //  sleep(100);

  if (!jobj) {
      printf("ERROR: json_tokener_parse(...): returned NULL\n");
      exit(1);
  }
  json_object_object_foreach(jobj, key, val) {
      printf("\t%s: %s\n", key, json_object_get_string(val));
  }
  json_object_put(jobj);





  

  exit(0);
}
#endif
