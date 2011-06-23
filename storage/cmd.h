// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * header that defines signatures of all commands
 */

#ifndef _CMD_H_
#define _CMD_H_

char ** fsck_parameters ();
int fsck_validate (imager_request *);

char ** prepare_parameters ();
int prepare_validate (imager_request *);
artifact * prepare_requirements (imager_request *, artifact *);
int prepare_cleanup (imager_request *, boolean);

char ** convert_parameters ();
int convert_validate (imager_request *);
artifact * convert_requirements (imager_request *, artifact *);
int convert_cleanup (imager_request *, boolean);

char ** upload_parameters ();
int upload_validate (imager_request *);
artifact * upload_requirements (imager_request *, artifact *);
int upload_cleanup (imager_request *, boolean);

char ** bundle_parameters ();
int bundle_validate (imager_request *);
artifact * bundle_requirements (imager_request *, artifact *);
int bundle_cleanup (imager_request *, boolean);

static imager_command known_cmds [] = {
    { "fsck", fsck_parameters, fsck_validate, NULL, NULL },
    { "prepare", prepare_parameters, prepare_validate, prepare_requirements, prepare_cleanup },
    { "convert", convert_parameters, convert_validate, convert_requirements, convert_cleanup },
    { "upload", upload_parameters, upload_validate, upload_requirements, upload_cleanup },
    { "bundle", bundle_parameters, bundle_validate, bundle_requirements, bundle_cleanup }
};

#endif // _CMD_H_
