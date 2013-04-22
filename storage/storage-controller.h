

#ifndef STORAGE_CONTROLLER_H_
#define STORAGE_CONTROLLER_H_

#define DEFAULT_SC_CALL_TIMEOUT	20 //! 20  second sync call timeout

int scClientCall(char *correlationId, char *userId, int use_ws_sec, char *ws_sec_policy_file_path, int timeout, char *scURL, char *scOp, ...);

#endif /* STORAGE_CONTROLLER_H_ */
