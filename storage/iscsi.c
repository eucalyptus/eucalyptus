// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
  Copyright (c) 2009  Eucalyptus Systems, Inc.

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

#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h> /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h> /* open */
#include <signal.h>
#include "eucalyptus.h"
#include <sys/types.h>
#include <sys/wait.h>
#include "misc.h"
#include "ipc.h"

static char home [MAX_PATH] = "";
static char connect_storage_cmd_path [MAX_PATH];
static char disconnect_storage_cmd_path [MAX_PATH];
static char get_storage_cmd_path [MAX_PATH];
static sem * iscsi_sem; // for serializing attach and detach invocations

void init_iscsi (const char * euca_home)
{
    const char * tmp;
    if (euca_home) {
        tmp = euca_home;
    } else {
        tmp = getenv(EUCALYPTUS_ENV_VAR_NAME);
        if (!tmp) {
            tmp = "/opt/eucalyptus";
        }
    } 
    safe_strncpy (home, tmp, sizeof (home));
    snprintf (connect_storage_cmd_path, MAX_PATH, EUCALYPTUS_CONNECT_ISCSI, home, home);
    snprintf (disconnect_storage_cmd_path, MAX_PATH, EUCALYPTUS_DISCONNECT_ISCSI, home, home);
    snprintf (get_storage_cmd_path, MAX_PATH, EUCALYPTUS_GET_ISCSI, home, home);
	iscsi_sem = sem_alloc (1, "mutex");
}

char * connect_iscsi_target (const char *dev_string) 
{
    char buf [MAX_PATH];
    char *retval=NULL;
    int pid, status, rc, len, rbytes, filedes[2];
 
    assert (strlen (home));
    snprintf (buf, MAX_PATH, "%s %s,%s", connect_storage_cmd_path, home, dev_string);
    logprintfl (EUCAINFO, "connect_iscsi_target invoked (dev_string=%s)\n", dev_string);
    
    rc = pipe(filedes);
    if (rc) {
        logprintfl(EUCAERROR, "connect_iscsi_target: cannot create pipe\n");
        return(NULL);
    }
    
    sem_p (iscsi_sem);
    pid = fork();
    if (!pid) {
        close (filedes[0]);
        
        if (strlen(buf)) logprintfl(EUCADEBUG, "connect_iscsi_target(): running command: %s\n", buf);
        if ((retval = system_output(buf)) == NULL) {
            logprintfl (EUCAERROR, "ERROR: connect_iscsi_target failed\n");
            len = 0;
        } else {
            logprintfl (EUCAINFO, "connect_iscsi_target(): attached host device name: %s\n", retval);
            len = strlen(retval);
        } 
        rc = write(filedes[1], &len, sizeof(int));
        if (retval) {
            rc = write(filedes[1], retval, sizeof(char) * len);
        }
        close (filedes[1]);

        if (rc == len) {
            exit(0);
        }
        exit(1);
        
    } else {
        close (filedes[1]);
        
        rbytes = timeread(filedes[0], &len, sizeof(int), 90);
        if (rbytes <= 0) {
            kill(pid, SIGKILL);
        } else {
            retval = malloc(sizeof(char) * (len+1));
            bzero(retval, len+1);
            rbytes = timeread(filedes[0], retval, len, 90);
            if (rbytes <= 0) {
                kill(pid, SIGKILL);
            }
        }
        close (filedes[0]);

        rc = timewait(pid, &status, 90);
        if (rc) {
            rc = WEXITSTATUS(status);
        } else {
            kill(pid, SIGKILL);
        }
    }
    sem_v (iscsi_sem);

    return retval;
}

int disconnect_iscsi_target (const char *dev_string) 
{
    int pid, retval, status;
    assert (strlen (home));

    logprintfl (EUCAINFO, "disconnect_iscsi_target invoked (dev_string=%s)\n", dev_string);

    sem_p (iscsi_sem);
    pid = fork();
    if (!pid) {
        if ( dev_string && strlen(dev_string) ) logprintfl(EUCADEBUG, "disconnect_iscsi_target(): running command: %s %s,%s\n", disconnect_storage_cmd_path, home, dev_string);
        if (vrun("%s %s,%s", disconnect_storage_cmd_path, home, dev_string) != 0) {
            logprintfl (EUCAERROR, "ERROR: disconnect_iscsi_target failed\n");
            exit(1);
        }
        exit(0);
    } else {
        retval = timewait(pid, &status, 90);
        if (retval) {
            retval = WEXITSTATUS(status);
        } else {
            kill(pid, SIGKILL);
            retval = -1;
        }
    }
    sem_v (iscsi_sem);

    return retval;
}

char * get_iscsi_target (const char *dev_string) 
{
    char buf [MAX_PATH];
    char *retval=NULL;
    int pid, status, rc, len, rbytes, filedes[2];
    assert (strlen (home));
    
    snprintf (buf, MAX_PATH, "%s %s,%s", get_storage_cmd_path, home, dev_string);
    logprintfl (EUCAINFO, "get_iscsi_target invoked (dev_string=%s)\n", dev_string);
    
    rc = pipe(filedes);
    if (rc) {
        logprintfl(EUCAERROR, "get_iscsi_target: cannot create pipe\n");
        return(NULL);
    }
    
    sem_p (iscsi_sem);
    pid = fork();
    if (!pid) {
        close(filedes[0]);

        if (strlen(buf)) logprintfl(EUCADEBUG, "get_iscsi_target(): running command: %s\n", buf);

        if ((retval = system_output(buf)) == NULL) {
            logprintfl (EUCAERROR, "ERROR: get_iscsi_target failed\n");
            len = 0;
        } else {
            logprintfl (EUCAINFO, "Device: %s\n", retval);
            len = strlen(retval);
        } 
        rc = write(filedes[1], &len, sizeof(int));
        if (retval) {
            rc = write(filedes[1], retval, sizeof(char) * len);
        }
        close(filedes[1]);

        if (rc == len) {
            exit(0);
        }
        exit(1);
        
    } else {
        close(filedes[1]);
        
        rbytes = timeread(filedes[0], &len, sizeof(int), 90);
        if (rbytes <= 0) {
            kill(pid, SIGKILL);
        } else {
            retval = malloc(sizeof(char) * (len+1));
            bzero(retval, len+1);
            rbytes = timeread(filedes[0], retval, len, 90);
            if (rbytes <= 0) {
                kill(pid, SIGKILL);
            }
        }
        close(filedes[0]);

        rc = timewait(pid, &status, 90);
        if (rc) {
            rc = WEXITSTATUS(status);
        } else {
            kill(pid, SIGKILL);
        }
    }
    sem_v (iscsi_sem);

    return retval;
}

int check_iscsi (const char* dev_string) 
{
    if (strchr (dev_string, ',') == NULL)
        return 0;
    return 1;
}
