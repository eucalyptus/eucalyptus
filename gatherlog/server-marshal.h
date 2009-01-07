#ifndef SERVER_MARSHAL_H
#define SERVER_MARSHAL_H

#include "axis2_skel_EucalyptusGL.h"
#include <handlers.h>

adb_GetLogsResponse_t *GetLogsMarshal(adb_GetLogs_t *getLogs, const axutil_env_t *env);
adb_GetKeysResponse_t *GetKeysMarshal(adb_GetKeys_t *getKeys, const axutil_env_t *env);

#endif
