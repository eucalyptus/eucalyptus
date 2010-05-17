/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

#include <com_eucalyptus_storage_DASManager.h>
#include <storage_native.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <sys/wait.h>
#include <signal.h>

JNIEXPORT void JNICALL Java_com_eucalyptus_storage_DASManager_registerSignals
(JNIEnv *env, jobject obj) {
	signal(SIGCHLD, sigchld);
}

