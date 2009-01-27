#include <stdio.h>
#include <stdlib.h>
#include <gl-client-marshal.h>
#include <euca_auth.h>

int main(int argc, char **argv) {
  axutil_env_t * env = NULL;
  axis2_char_t * client_home = NULL;
  axis2_char_t endpoint_uri[256], *tmpstr;
  axis2_stub_t * stub = NULL;
  int rc, i;
  char *euca_home;
  
  snprintf(endpoint_uri, 256," http://%s/axis2/services/EucalyptusGL", argv[1]);
  //  env =  axutil_env_create_all("/tmp/GLclient.log", AXIS2_LOG_LEVEL_TRACE);
  env =  axutil_env_create_all("/tmp/fooh", AXIS2_LOG_LEVEL_TRACE);
  client_home = AXIS2_GETENV("AXIS2C_HOME");
  if (!client_home) {
    printf("must have AXIS2C_HOME set\n");
  }
  stub = axis2_stub_create_EucalyptusGL(env, client_home, endpoint_uri);

  if (!strcmp(argv[2], "getLogs")) {
    char *clog, *nlog, *hlog, *alog;
    rc = gl_getLogs(argv[3], &clog, &nlog, &hlog, &alog, env, stub);
    if (!rc) {
      if (clog) printf("CLOG\n----------\n%s\n-----------\n", base64_dec((unsigned char *)clog, strlen(clog)));
      if (nlog) printf("NLOG\n----------\n%s\n-----------\n", base64_dec((unsigned char *)nlog, strlen(nlog)));
      if (hlog) printf("HLOG\n----------\n%s\n-----------\n", base64_dec((unsigned char *)hlog, strlen(hlog)));
      if (alog) printf("ALOG\n----------\n%s\n-----------\n", base64_dec((unsigned char *)alog, strlen(alog)));
    }
  } else if (!strcmp(argv[2], "getKeys")) {
    char *cccert, *nccert;
    rc = gl_getKeys(argv[3], &cccert, &nccert, env, stub);
    if (!rc) {
      if (cccert) printf("CCCERT\n----------\n%s\n-----------\n", base64_dec((unsigned char *)cccert, strlen(cccert)));
      if (nccert) printf("NCCERT\n----------\n%s\n-----------\n", base64_dec((unsigned char *)nccert, strlen(nccert)));
    }
  }
  exit(0);
}
