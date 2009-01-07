#ifndef INCLUDE_GLCLIENT_H
#define INCLUDE_GLCLIENT_H

#include <stdio.h>
#include <time.h>
#include "axis2_stub_EucalyptusGL.h"

int gl_getLogs(char *service, char **outCClog, char **outNClog, char **outHlog, char **outAlog, axutil_env_t *env, axis2_stub_t *stub);

int gl_getKeys(char *service, char **outCCCert, char **outNCCert, axutil_env_t *env, axis2_stub_t *stub);

#endif
