#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

int doGetLogs(char *service, char **outCCLog, char **outNCLog, char **outHTTPDLog, char **outAxis2Log);
int doGetKeys(char *service, char **outCCCert, char **outNCCert);

#endif
