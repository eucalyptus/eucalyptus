/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

#include <com_eucalyptus_storage_AOEManager.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

extern int run_command_and_get_pid(char *cmd, char **args); 

JNIEXPORT jint JNICALL Java_com_eucalyptus_storage_AOEManager_exportVolume
(JNIEnv *env, jobject obj, jstring iface, jstring lvName, jint major, jint minor) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
    const jbyte* if_name = (*env)->GetStringUTFChars(env, iface, NULL);
    char major_str[4];
    char minor_str[4];
    char *args[7];
    
    char rootwrap[256];
    char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
    } else {
        home = strdup (home);
    }

    snprintf(rootwrap, 256, "%s/usr/lib/eucalyptus/euca_rootwrap", home);

    snprintf(major_str, 4, "%d", major);
    snprintf(minor_str, 4, "%d", minor);

    args[0] = rootwrap;
    args[1] = "vblade";
    args[2] = major_str;
    args[3] = minor_str;
    args[4] = (char *) if_name;
    args[5] = (char *) lv_name;
    args[6] = (char *) NULL;

    int pid = run_command_and_get_pid(rootwrap, args);
    (*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    (*env)->ReleaseStringUTFChars(env, iface, if_name);
    if (home) free(home);
    return pid;
}

