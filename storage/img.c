// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdlib.h> // NULL
#include <stdio.h>
#include <string.h> // bzero, memcpy
#include <unistd.h> // getopt, access
#include <ctype.h> // tolower
#include <stdarg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <regex.h>

//#include "eucalyptus.h" // EUCALYPTUS_ENV_VAR_NAME
//#include "ami2vmx.h"
//#include "walrus.h"
#include "misc.h" // logprintfl
//#include "cache.h"
#include "img.h"
//#include "http.h" // http_put
//#include "vmdk.h" // vmdk_*

static void strnsub (char * dest, const char *src, const unsigned int sindex, const unsigned int eindex, const unsigned int size)
{
    strncpy (dest, src+sindex, ((eindex-sindex)>size)?size:eindex-sindex);
}

int parse_img_spec (img_loc * loc, const char * str)
{
#   define _SIZE 512
    char low [_SIZE];
    int i;

    bzero (loc, sizeof (img_loc)); // so all strings are null-terminated

    for (i=0; i<_SIZE && i<strlen(str); i++) {
        low [i] = tolower (str[i]);
    }

    if (!strncmp (low, "http://", 7) || !strncmp (low, "https://", 8)) {
        if (strstr (str, "services/Walrus")) {
            loc->type=WALRUS;
            safe_strncpy (loc->url, str, sizeof(loc->url));

        } else if (strstr (str, "dcPath=")) {
            // EXAMPLE: https://192.168.7.236/folder/i-4DD50852?dcPath=ha-datacenter&dsName=S1
            char * ex = "^[Hh][Tt][Tt][Pp][Ss]?://([^/]+)/([^\\?]+)\\?dcPath=([^&]+)&dsName=(.*)$";
            regmatch_t pmatch[5];
            regex_t re;
            int status;

            if (regcomp (&re, ex, REG_EXTENDED) != 0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to compile regular expression for vSphere URL: %s\n", ex);
                return 1;
            }
            status = regexec (&re, str, (size_t)5, pmatch, 0);
            regfree (&re);
            if (status != 0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to match the syntax of vSphere URL (%s)\nwith regular expression %s\n", str, ex);
                return 1;
            }
            if (re.re_nsub!=4) {
                logprintfl (EUCAERROR, "parse_img_spec: unexpected number of matched elements in %s\n", ex);
                return 1;
            }
            loc->type=VSPHERE;

            char path [SIZE];
            safe_strncpy (loc->url,            str, sizeof (loc->url));
            strnsub (loc->host,           str, pmatch[1].rm_so, pmatch[1].rm_eo, sizeof (loc->host));
            strnsub (path,                str, pmatch[2].rm_so, pmatch[2].rm_eo, sizeof (loc->path));
            strnsub (loc->vsphere_dc,     str, pmatch[3].rm_so, pmatch[3].rm_eo, sizeof (loc->vsphere_dc));
            strnsub (loc->vsphere_ds,     str, pmatch[4].rm_so, pmatch[4].rm_eo, sizeof (loc->vsphere_ds));

            // extract path, split into directory and filename
            loc->dir  [0] = '\0';
            loc->file [0] = '\0';
            loc->path [0] = '\0';
            char * t = strtok (path, "/");
            if (t==NULL || strcmp (t, "folder")!=0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to parse path in URL (must begin with 'folder'): %s...\n", path);
                return 1;
            }
            char * pt = NULL;
            while ((t = strtok (NULL, "/"))!=NULL) {
                if (pt) {
                    if (loc->dir[0]!='\0')
                        strncat (loc->dir, "/", sizeof(loc->dir));
                    strncat (loc->dir, pt, sizeof(loc->dir));
                }
                pt = t;
            }
            if (pt) {
                strncat (loc->file, pt, sizeof(loc->file));
            }
            if (loc->dir[0]=='\0') {
                safe_strncpy (loc->path, loc->file, sizeof(loc->path));
            } else {
                snprintf (loc->path, sizeof(loc->path), "%s/%s", loc->dir, loc->file);
            }

            logprintfl (EUCADEBUG, "parse_img_spec: re_nsub=%d host=%s path='%s' ('%s' + '%s') dc=%s ds=%s\n",
                        re.re_nsub,
                        loc->host,
                        loc->path, loc->dir, loc->file,
                        loc->vsphere_dc,
                        loc->vsphere_ds);

        } else {
            loc->type=HTTP;

            // EXAMPLE: http://192.168.7.236:902/foobar?foo=bar
            char * ex = "^[Hh][Tt][Tt][Pp]([Ss])?://([^/^:]+)(:([0-9]+))?(/[^\\?]+)?(\\?(.*))?$";
            regmatch_t pmatch[8];
            regex_t re;
            int status;

            if (regcomp (&re, ex, REG_EXTENDED) != 0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to compile regular expression for URL: %s\n", ex);
                return 1;
            }
            status = regexec (&re, str, (size_t)8, pmatch, 0);
            regfree (&re);
            if (status != 0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to match the syntax of URL (%s)\nwith regular expression %s\n", str, ex);
                return 1;
            }
            if (re.re_nsub!=7) {
                logprintfl (EUCAERROR, "parse_img_spec: unexpected number of matched elements in %s\n", ex);
                return 1;
            }

            char s[1];
            char port [_SIZE]; bzero (port, sizeof (port));
            strnsub (s,                   str, pmatch[1].rm_so, pmatch[1].rm_eo, sizeof (s));
            if (s[0]=='s' ) loc->type = HTTPS;
            safe_strncpy (loc->url,            str, sizeof (loc->url));
            strnsub (loc->host,           str, pmatch[2].rm_so, pmatch[2].rm_eo, sizeof (loc->host));
            strnsub (port,                str, pmatch[4].rm_so, pmatch[4].rm_eo, sizeof (port) - 1);
            loc->port = atoi (port);
            strnsub (loc->path,           str, pmatch[5].rm_so, pmatch[5].rm_eo, sizeof (loc->path));
            strnsub (loc->params,         str, pmatch[7].rm_so, pmatch[7].rm_eo, sizeof (loc->params));
        }

    } else {
        loc->type=PATH;
        safe_strncpy (loc->path, str, sizeof(loc->path));
    }

    return 0;
}

int img_init_spec (img_spec * spec, const char * id, const char * loc, const img_creds * creds)
{
    bzero (spec, sizeof(*spec));

    if (id && strlen(id)) 
        strncpy (spec->id, id, sizeof(spec->id) - 1); 
    
    else
        return 0; // if no ID is given, this function just zeroes out the struct

    if (parse_img_spec (&(spec->location), loc)) return 1;
    if (creds)
        memcpy (&(spec->location.creds), creds, sizeof(img_creds));
    else
        spec->location.creds.type=NONE;

    return 0;
}

void img_cleanup (void)
{

}

static char * img_spec_type_str (const int type)
{
    switch (type) {
    case PATH: return "path";
    case HTTP: return "http";
    case VSPHERE: return "vsphere";
    case WALRUS: return "walrus";
    case SFTP: return "sftp";
    default: return "unknown";
    }
}

static char * img_creds_type_str (const int type)
{
    switch (type) {
    case NONE: return "none";
    case PASSWORD: return "password";
    case X509CREDS: return "x509";
    case SSHKEY: return "sshkey";
    default: return "unknown";
    }
}

static const char * img_creds_location_str (const img_loc * loc)
{
    switch (loc->type) {
    case PATH: return loc->path;
    case HTTP: return loc->url;
    case VSPHERE: return loc->url;
    case WALRUS: return loc->url;
    case SFTP: return loc->url;
    default: return "";
    }
}

void print_img_spec (const char * name, const img_spec * spec)
{
    if (name==NULL || spec==NULL || strlen(name)==0 || strlen(spec->id)==0) return;
    logprintfl (EUCADEBUG, "\t%s: id=%-12s type=%-7s creds=%-8s %s\n",
                name,
                spec->id,
                img_spec_type_str (spec->location.type),
                img_creds_type_str (spec->location.creds.type),
                img_creds_location_str (&(spec->location)));
}
