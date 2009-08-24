#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>

#include <gl-client-marshal.h>
#include <handlers.h>
#include <euca_auth.h>

int doGetLogs(char *service, char **outCCLog, char **outNCLog, char **outHTTPDLog, char **outAxis2Log) {
  char *home, file[1024], *buf;
  int fd, rc, bufsize;
  
  *outCCLog = *outNCLog = *outHTTPDLog = *outAxis2Log = NULL;
  if (!service) return(1);
  
  bufsize = 1000 * 1024;
  buf = malloc(bufsize);
  
  if (!strcmp(service, "self")) {
    home = strdup(getenv("EUCALYPTUS"));
    if (!home) {
      home = strdup("");
    }

    
    snprintf(file, 1024, "%s/var/log/eucalyptus/cc.log", home);
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
    
    snprintf(file, 1024, "%s/var/log/eucalyptus/nc.log", home);
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
    snprintf(file, 1024, "%s/var/log/eucalyptus/httpd-nc_error_log", home);
    fd = open(file, O_RDONLY);
    if (fd < 0) {
      snprintf(file, 1024, "%s/var/log/eucalyptus/httpd-cc_error_log", home);
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
    snprintf(file, 1024, "%s/var/log/eucalyptus/axis2c.log", home);
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
  char *home, file[1024], *buf;
  int fd, rc, bufsize;

  *outCCCert = *outNCCert = NULL;
  if (!service) return(1);
 
  
  bufsize = 1000 * 1024;
  buf = malloc(bufsize);
  
  if (!strcmp(service, "self")) {
    home = strdup(getenv("EUCALYPTUS"));
    if (!home) {
      home = strdup("");
    }
    
    snprintf(file, 1024, "%s/var/lib/eucalyptus/keys/cluster-cert.pem", home);
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
    snprintf(file, 1024, "%s/var/lib/eucalyptus/keys/node-cert.pem", home);
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
