// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * header that defines signatures of all commands
 */

#ifndef _CMD_H_
#define _CMD_H_

char ** fsck_parameters ();
int fsck_validate (imager_request *);
int fsck_execute (imager_request *);

char ** download_parameters ();
int download_validate (imager_request *);
int download_requirements (imager_request *);
int download_execute (imager_request *);
int download_cleanup (imager_request *, boolean);

char ** create_parameters ();
int create_validate (imager_request *);
int create_requirements (imager_request *);
int create_execute (imager_request *);
int create_cleanup (imager_request *, boolean);

char ** convert_parameters ();
int convert_validate (imager_request *);
int convert_requirements (imager_request *);
int convert_execute (imager_request *);
int convert_cleanup (imager_request *, boolean);

char ** inject_parameters ();
int inject_validate (imager_request *);
int inject_requirements (imager_request *);
int inject_execute (imager_request *);
int inject_cleanup (imager_request *, boolean);

char ** upload_parameters ();
int upload_validate (imager_request *);
int upload_requirements (imager_request *);
int upload_execute (imager_request *);
int upload_cleanup (imager_request *, boolean);

char ** bundle_parameters ();
int bundle_validate (imager_request *);
int bundle_requirements (imager_request *);
int bundle_execute (imager_request *);
int bundle_cleanup (imager_request *, boolean);

static imager_command known_cmds [] = {
    { "fsck", fsck_parameters, fsck_validate, NULL, fsck_execute, NULL },
    { "download", download_parameters, download_validate, download_requirements, download_execute, download_cleanup },
    { "create", create_parameters, create_validate, create_requirements, create_execute, create_cleanup },
    { "convert", convert_parameters, convert_validate, convert_requirements, convert_execute, convert_cleanup },
    { "inject", inject_parameters, inject_validate, inject_requirements, inject_execute, inject_cleanup },
    { "upload", upload_parameters, upload_validate, upload_requirements, upload_execute, upload_cleanup },
    { "bundle", bundle_parameters, bundle_validate, bundle_requirements, bundle_execute, bundle_cleanup }
};

#endif // _CMD_H_
