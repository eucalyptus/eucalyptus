// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include "imager.h"
#include "diskutil.h"
#include "eucalyptus.h"
#include "vmdk.h"

static char * params [] = {
    NULL
};

char ** fsck_parameters ()
{
    return params;
}

int fsck_validate (imager_request * req)
{
    print_req (req);

    if (diskutil_init(TRUE)) { // euca_imager may need GRUB
        logprintfl (EUCAERROR, "error: failed to initialize diskutil\n");
        return ERROR;
    }

    if (vmdk_init()) {
        logprintfl (EUCAERROR, "error: failed to initialize VMware's VDDK (is LD_LIBRARY_PATH set?)\n");
        return ERROR;
    }
    
    return 0;
}

