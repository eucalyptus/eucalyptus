/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
