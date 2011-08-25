// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-                                                                                                     // vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:                                                                                                                                               
/*
  Copyright (c) 2010  Eucalyptus Systems, Inc.	

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by 
  the Free Software Foundation, only version 3 of the License.  
 
  This file is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for more details.  

  You should have received a copy of the GNU General Public License along
  with this program.  If not, see <http://www.gnu.org/licenses/>.
 
  Please contact Eucalyptus Systems, Inc., 130 Castilian
  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
  if you need additional information or have any questions.

  This file may incorporate work covered under the following copyright and
  permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

  Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#define __USE_GNU
#include <string.h>
#include <time.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h> // WEXITSTATUS on Lucid

#include "hooks.h"
#include "misc.h"

static int initialized = 0;
static char euca_path [MAX_PATH];
static char hooks_path [MAX_PATH];

int init_hooks (const char * euca_dir, const char * hooks_dir)
{
    assert (euca_dir);
    assert (hooks_dir);

    safe_strncpy (euca_path, euca_dir, sizeof (euca_path));
    if (check_directory (euca_path))
        return 1;
    safe_strncpy (hooks_path, hooks_dir, sizeof (hooks_path));
    if (check_directory (hooks_path))
        return 1;
    logprintfl (EUCAINFO, "using hooks directory %s\n", hooks_path);

    initialized = 1;
    return 0;
}

int call_hooks (const char * event_name, const char * param1)
{
    assert (event_name);
    if (!initialized) return 0; // return OK if hooks were not initialized
    
    DIR * dir;
    if ((dir=opendir(hooks_path))==NULL) {
        return 1;
    }

    int ret = 0;
    struct dirent * dir_entry;
    while ((dir_entry=readdir(dir))!=NULL) {
        char * entry_name = dir_entry->d_name;

        if (!strcmp(".", entry_name) ||
            !strcmp("..", entry_name))
            continue; // ignore known unrelated files

        // get the path of the directory item
        char entry_path [MAX_PATH];
        snprintf (entry_path, sizeof (entry_path), "%s/%s", hooks_path, entry_name);
        struct stat sb;
        if (stat(entry_path, &sb)==-1)
            continue; // ignore access errors

        // run the hook if... 
        if ((S_ISLNK(sb.st_mode) || S_ISREG(sb.st_mode)) // looks like a file or symlink
            && (sb.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH))) { // is executable 
            char cmd [MAX_PATH];
            snprintf (cmd, sizeof (cmd), "%s %s %s %s", entry_path, event_name, euca_path, param1?param1:"");
            ret = WEXITSTATUS (system (cmd));
            logprintfl (EUCAINFO, "executed hook [%s %s%s%s] which returned %d\n", 
                        entry_name, 
                        event_name, 
                        param1?" ":"", 
                        param1?param1:"", ret);
            if (ret > 0 && ret < 100)
                break; // bail if any hook returns code [1-99] (100+ are reserved for future use)
        }
    }
    closedir(dir);
    
    return ret;
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// code for unit-testing below, to be compiled into a stand-alone binary
///////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef __STANDALONE

int main (int argc, char ** argv)
{
    int status = 0;

    assert (call_hooks ("e1", NULL)!=0);
    assert (call_hooks ("e1", "p1")!=0);
    assert (init_hooks ("/tmp", "/foobar")!=0);
    assert (init_hooks ("/foobar", "/tmp")!=0);
    
    char d [MAX_PATH] = "/tmp/euca-XXXXXX";
    assert (mkdtemp (d)!=NULL);
    assert (init_hooks ("/tmp", d)==0);

    char h1 [MAX_PATH]; snprintf (h1, sizeof (h1), "%s/h1", d); write2file (h1, "#!/bin/bash\necho h1 -$1- -$2- -$3-\n"); chmod (h1, S_IXUSR | S_IRUSR);
    char h3 [MAX_PATH]; snprintf (h3, sizeof (h3), "%s/h3", d); write2file (h3, "#!/bin/bash\necho h3 -$1- -$2- -$3-\n"); chmod (h3, 0); // unreadable hook
    char h4 [MAX_PATH]; snprintf (h4, sizeof (h4), "%s/h4", d); mkdir (h4, 0700); // not a file

    assert (call_hooks ("e1", NULL)==0);
    assert (call_hooks ("e1", "p1")==0);
    char h0 [MAX_PATH]; snprintf (h0, sizeof (h0), "%s/h0", d); write2file (h0, "#!/bin/bash\nexit 99;\n"); chmod (h0, S_IXUSR | S_IRUSR);
    assert (call_hooks ("e1", "p1")==99);

    assert (rmdir (h4)==0);
    assert (unlink (h3)==0);
    assert (unlink (h0)==0);
    assert (unlink (h1)==0);
    assert (rmdir (d)==0);
    printf ("removed directory %s\n", d);

    return status;
}
#endif
