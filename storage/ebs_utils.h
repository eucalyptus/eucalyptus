/*
 * ebs_utils.h
 *
 *  Created on: Apr 4, 2013
 *      Author: zhill
 */

//! EBS utils handles the EBS abstraction part of EBS volumes. Specific implementations of those
//! abstractions are found in other files such as iscsi.c/h

#ifndef EBS_UTILS_H_
#define EBS_UTILS_H_

#include <eucalyptus.h>
#include <ipc.h>

 //Max length of token string
#define EBS_TOKEN_MAX_LENGTH	512
#define EBS_CONNECT_STRING_MAX_LENGTH	4096
#define VOLUME_STRING_PREFIX	"sc://"

//! Represents an ebs volume and the metadata required to connect/disconnect
typedef struct ebs_volume_data {
	char volumeId[128];
	char token[EBS_TOKEN_MAX_LENGTH];
	//char scUrl[EUCA_MAX_PATH]; //! Remove this eventually, should be orthogonal to volume info
	char connect_string[EBS_CONNECT_STRING_MAX_LENGTH]; //! Remove this?
} ebs_volume_data;


//! Initialize ebs utility structures like semaphores
int init_ebs_utils();

//! Get the local device for the remote device
char *get_volume_local_device(const char *connection_string);

//! Convert a volume data string into a ebs_volume_data struct
int deserialize_volume(char *volume_string, ebs_volume_data **dest);

//! Serialize ebs_volume_data structure into a string
int serialize_volume(ebs_volume_data *vol_data, char **dest);

//! New version, uses external sc url...likely derived from service info
int connect_ebs_volume(char *sc_url, char *attachment_token, int use_ws_sec, char *ws_sec_policy_file, char *local_ip, char *local_iqn, char **result_device, ebs_volume_data **vol_data);

//! Do a full detach including the local session disconnect and token de-auth on SC via call
int disconnect_ebs_volume(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, char *volume_string, char *connect_string, char *local_ip, char *local_iqn);

//! Do disconnect with ebs_volume_data rather than string
int disconnect_ebs_volume_with_struct(char *sc_url, int use_ws_sec, char *ws_sec_policy_file, ebs_volume_data *vol_data, char *local_ip, char *local_iqn);

#endif /* EBS_UTILS_H_ */
