// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#ifndef IMG_H
#define IMG_H

#define SIZE 512

typedef struct _img_creds {
    enum { NONE, PASSWORD, X509CREDS, SSHKEY } type;
    char login [SIZE];
    char password [SIZE];
    char pk_path [SIZE];
    char cert_path [SIZE];
    char ssh_key_path [SIZE];
} img_creds;

typedef struct _img_loc {
    enum { PATH, HTTP, HTTPS, VSPHERE, WALRUS, SFTP } type;
    char url [SIZE];
    char path [SIZE]; // dir/file
    char dir [SIZE];
    char file [SIZE];
    char host [SIZE];
    char params [SIZE];
    int port;
    char vsphere_dc [SIZE];
    char vsphere_ds [SIZE];
    char vsphere_vmx_ds [SIZE];
    char vsphere_vmx_path [SIZE];
    img_creds creds;
} img_loc;

typedef struct _img_spec {
    //    enum { EMI, EKI, ERI, DISK, VDDK } type;
    char id [SIZE];
    img_loc location;
    int size;
} img_spec;

int parse_img_spec (img_loc * loc, const char * str);
void print_img_spec (const char * name, const img_spec * spec);

#endif
