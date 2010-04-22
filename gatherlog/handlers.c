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
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>
#include <linux/limits.h>
#ifndef MAX_PATH
#define MAX_PATH 4096
#endif

#include <gl-client-marshal.h>
#include <handlers.h>
#include <euca_auth.h>



int doGetLogs(char *service, char **outCCLog, char **outNCLog, char **outHTTPDLog, char **outAxis2Log) {
  char *home, file[MAX_PATH], *buf;
  int fd, rc, bufsize;
  
  *outCCLog = *outNCLog = *outHTTPDLog = *outAxis2Log = NULL;
  if (!service) return(1);
  
  bufsize = 1000 * 1024;
  buf = malloc(bufsize);
  if (!buf) {
      printf("Out of memory!\n");
      return 1;
  }
  
  if (!strcmp(service, "self")) {
    char *tmp;
    home = NULL;
    tmp = getenv("EUCALYPTUS");
    if (tmp) home = strdup(tmp);
    if (!home) {
       home = strdup("");
    }
    if (!home) {
      printf("Out of memory!\n");
      free(buf);
      return 1;
    }

    
    snprintf(file, MAX_PATH, "%s/var/log/eucalyptus/cc.log", home);
    fd = open(file, O_RDONLY);
    if (fd >= 0) {
      bzero(buf, bufsize);
      lseek(fd, -1 * bufsize, SEEK_END);
      rc = read(fd, buf, bufsize);
      if (rc > 0) {
	*outCCLog = base64_enc((unsigned char *)buf, strlen(buf));
      }
      close(fd);
    } else {
      *outCCLog = NULL;
    }
    
    snprintf(file, MAX_PATH, "%s/var/log/eucalyptus/nc.log", home);
    fd = open(file, O_RDONLY);
    if (fd >= 0) {
      bzero(buf, bufsize);
      lseek(fd, -1 * bufsize, SEEK_END);
      rc = read(fd, buf, bufsize);
      if (rc > 0) {
	*outNCLog = base64_enc((unsigned char *)buf, strlen(buf));
      }
      close(fd);
    } else {
      *outNCLog = NULL;
    }
    
    bzero(buf, bufsize);
    snprintf(file, MAX_PATH, "%s/var/log/eucalyptus/httpd-nc_error_log", home);
    fd = open(file, O_RDONLY);
    if (fd < 0) {
      snprintf(file, MAX_PATH, "%s/var/log/eucalyptus/httpd-cc_error_log", home);
      fd = open(file, O_RDONLY);
    }
    if (fd >= 0) {
      bzero(buf, bufsize);
      rc = read(fd, buf, bufsize);
      if (rc > 0) {
	*outHTTPDLog = base64_enc((unsigned char *)buf, strlen(buf));
      }
      close(fd);
    } else {
      *outHTTPDLog = NULL;
    }
    
    bzero(buf, bufsize);
    snprintf(file, MAX_PATH, "%s/var/log/eucalyptus/axis2c.log", home);
    fd = open(file, O_RDONLY);
    if (fd >= 0) {
      bzero(buf, bufsize);
      rc = read(fd, buf, bufsize);
      if (rc > 0) {
	*outAxis2Log = base64_enc((unsigned char *)buf, strlen(buf));
      }
      close(fd);
    } else {
      *outAxis2Log = NULL;
    }
    if (home) free(home);
  } else {
    int pid, filedes[2], status;
    
    pipe(filedes);
    pid = fork();
    if (pid == 0) {
      axutil_env_t * env = NULL;
      axis2_char_t * client_home = NULL;
      axis2_char_t endpoint_uri[256], *tmpstr;
      axis2_stub_t * stub = NULL;
      char *clog, *hlog, *alog, *nlog;
      
      close(filedes[0]);
      
      //      env =  axutil_env_create_all("/tmp/GLclient.log", AXIS2_LOG_LEVEL_TRACE);
      env =  axutil_env_create_all(NULL, 0);
      client_home = AXIS2_GETENV("AXIS2C_HOME");
      if (!client_home) {
	exit(1);
      } else {
	stub = axis2_stub_create_EucalyptusGL(env, client_home, service);
	clog = nlog = hlog = alog = NULL;
	rc = gl_getLogs("self", &clog, &nlog, &hlog, &alog, env, stub);
	if (rc) {
	} else {
	  bzero(buf, bufsize);
	  if (clog) snprintf(buf, bufsize, "%s", clog);
	  rc = write(filedes[1], buf, bufsize);

	  bzero(buf, bufsize);
	  if (nlog) snprintf(buf, bufsize, "%s", nlog);
	  rc = write(filedes[1], buf, bufsize);
	  
	  bzero(buf, bufsize);
	  if (hlog) snprintf(buf, bufsize, "%s", hlog);
	  rc = write(filedes[1], buf, bufsize);
	  
	  bzero(buf, bufsize);
	  if (alog) snprintf(buf, bufsize, "%s", alog);
	  rc = write(filedes[1], buf, bufsize);
	}
      }
      close(filedes[1]);
      exit(0);

    } else {
      close(filedes[1]);

      rc = read(filedes[0], buf, bufsize);
      if (rc && buf[0] != '\0') {
	*outCCLog = strdup(buf);
      }

      rc = read(filedes[0], buf, bufsize);
      if (rc && buf[0] != '\0') {
	*outNCLog = strdup(buf);
      }

      rc = read(filedes[0], buf, bufsize);
      if (rc && buf[0] != '\0') {
	*outHTTPDLog = strdup(buf);
      }

      rc = read(filedes[0], buf, bufsize);
      if (rc && buf[0] != '\0') {
	*outAxis2Log = strdup(buf);
      }
      close(filedes[0]);
      wait(&status);
    }
  }
  
  if (buf) free(buf);
  
  return(0);
}

int doGetKeys(char *service, char **outCCCert, char **outNCCert) {
  char *home, file[MAX_PATH], *buf;
  int fd, rc, bufsize;

  *outCCCert = *outNCCert = NULL;
  if (!service) return(1);
 
  
  bufsize = 1000 * 1024;
  buf = malloc(bufsize);
  if (!buf) {
     printf("Out of memory!\n");
     return 1;
  }
  
  if (!strcmp(service, "self")) {
    char *tmp;
    home = NULL;
    tmp = getenv("EUCALYPTUS");
    if (tmp) home = strdup(tmp);
    if (!home) {
      home = strdup("");
    }
    if (!home) {
      printf("Out of memory!\n");
      free(buf);
      return 1;
    }
    
    snprintf(file, MAX_PATH, "%s/var/lib/eucalyptus/keys/cluster-cert.pem", home);
    fd = open(file, O_RDONLY);
    if (fd >= 0) {
      bzero(buf, bufsize);
      lseek(fd, -1 * bufsize, SEEK_END);
      rc = read(fd, buf, bufsize);
      if (rc > 0) {
	*outCCCert = base64_enc((unsigned char *)buf, strlen(buf));
      }
      close(fd);
    } else {
      *outCCCert = NULL;
    }
    
    bzero(buf, bufsize);
    snprintf(file, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", home);
    fd = open(file, O_RDONLY);
    if (fd >= 0) {
      bzero(buf, bufsize);
      lseek(fd, -1 * bufsize, SEEK_END);
      rc = read(fd, buf, bufsize);
      if (rc > 0) {
	*outNCCert = base64_enc((unsigned char *)buf, strlen(buf));
      }
      close(fd);
    } else {
      *outNCCert = NULL;
    }
    
    if (home) free(home);
  } else {
    int pid, filedes[2], status;
    
    pipe(filedes);
    pid = fork();
    if (pid == 0) {
      axutil_env_t * env = NULL;
      axis2_char_t * client_home = NULL;
      axis2_char_t endpoint_uri[256], *tmpstr;
      axis2_stub_t * stub = NULL;
      char *ccert, *ncert;
      
      close(filedes[0]);
      
      //      env =  axutil_env_create_all("/tmp/GLclient.log", AXIS2_LOG_LEVEL_TRACE);
      env =  axutil_env_create_all(NULL, 0);
      client_home = AXIS2_GETENV("AXIS2C_HOME");
      if (!client_home) {
	exit(1);
      } else {
	stub = axis2_stub_create_EucalyptusGL(env, client_home, service);
	ccert = ncert = NULL;
	rc = gl_getKeys("self", &ccert, &ncert, env, stub);
	if (rc) {
	} else {
	  bzero(buf, bufsize);
	  if (ccert) snprintf(buf, bufsize, "%s", ccert);
	  rc = write(filedes[1], buf, bufsize);
	  
	  bzero(buf, bufsize);
	  if (ncert) snprintf(buf, bufsize, "%s", ncert);
	  rc = write(filedes[1], buf, bufsize);
	}
      }
      close(filedes[1]);
      exit(0);
      
    } else {
      close(filedes[1]);
      
      rc = read(filedes[0], buf, bufsize);
      if (rc) {
	*outCCCert = strdup(buf);
      }
      
      rc = read(filedes[0], buf, bufsize);
      if (rc) {
	*outNCCert = strdup(buf);
      }
      
      close(filedes[0]);
      wait(&status);
    }
  }
  
  if (buf) free(buf);
  
  return(0);
}
