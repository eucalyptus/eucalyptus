// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * imager header file
 */

#ifndef _IMAGER_H_
#define _IMAGER_H_

#include <sys/stat.h> // mode_t
#include "misc.h"
#include "map.h"
#include "vbr.h"

struct _imager_command;

typedef struct _imager_param {
    char * key;
    char * val;
} imager_param;

typedef struct _imager_request {
    struct _imager_command * cmd;
    imager_param * params;
    int index; // of this command in a sequence
    void * internal;
} imager_request;

typedef struct _imager_command {
    char * name;
    char ** (* parameters) (); // returns valid parameter names and info for each
    int (* validate) (imager_request *); // verifies parameters, returning 0 if all is well
    artifact * (* requirements) (imager_request *, artifact * prev_art); // checks on inputs, records outputs
    int (* cleanup) (imager_request *, boolean);
} imager_command;

// common functions used by commands

void err (const char *format, ...);
void print_req (imager_request * req);
char parse_boolean (const char * s);
char * strduplc (const char * s);
char * parse_loginpassword (const char * s);
long long parse_bytes (const char * s);
int verify_readability (const char * path);
char * get_euca_home (void);
map * get_artifacts_map (void);
int ensure_path_exists (const char * path, mode_t mode);
int ensure_dir_exists (const char * path, mode_t mode);
artifact * skip_sentinels (artifact * a);

#endif // _IMAGER_H_
