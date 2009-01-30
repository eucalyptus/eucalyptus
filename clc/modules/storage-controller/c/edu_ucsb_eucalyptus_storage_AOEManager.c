/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the
 * following disclaimer.
 *
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

#include <edu_ucsb_eucalyptus_storage_AOEManager.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

extern jstring run_command(JNIEnv *env, char *cmd, int outfd); 

extern int run_command_and_get_pid(char *cmd, char **args); 

JNIEXPORT jint JNICALL Java_edu_ucsb_eucalyptus_storage_AOEManager_exportVolume
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

    snprintf(rootwrap, 256, "%s/usr/share/eucalyptus/euca_rootwrap", home);

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
    return pid;
}


JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_AOEManager_unexportVolume
  (JNIEnv *env, jobject obj, jint vblade_pid) {
   char command[128];

   snprintf(command, 128, "kill -9 %d", vblade_pid);
   run_command(env, command, 1);
}
