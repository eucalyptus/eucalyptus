// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * support for VMware disks and for uploading/cloning them to vSphere/ESX hosts
 */

#ifndef _VMDK_H_
#define _VMDK_H_

#include "misc.h" // boolean
#include "img.h"

int vmdk_init (void);
int gen_vmdk (const char * disk_path, const char * vmdk_path);
int vmdk_upload (const char * file_path, const img_spec * dest);
long long vmdk_get_size (const img_spec * spec);
int vmdk_convert_remote (const img_spec * spec, const char * disk_path);
int vmdk_clone (const char * vmdk_path, const img_spec * spec);
int vmdk_convert_local (const char * disk_path, const char * vmdk_path, boolean overwrite);
int parse_vsphere_path (const char * path, char * vds, const int vds_size, char * vpath, const int vpath_size);

#endif // _VMDK_H_
