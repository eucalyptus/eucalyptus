
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

#include <edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

static const char* blockSize = "1M";

jstring run_command(JNIEnv *env, char *cmd, int outfd) {
	FILE* fd;
	int pid;
	char readbuffer[256];
    char absolute_cmd[256];

    char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
    } else {
        home = strdup (home);
    }

    snprintf(absolute_cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap %s", home, cmd);
    fprintf(stdout, "cmd: %s\n", absolute_cmd);

	bzero(readbuffer, 256);
	fd = popen(absolute_cmd, "r");
	if(fgets(readbuffer, 256, fd)) {
	    char* ptr = strchr(readbuffer, '\n');
	    if(ptr != NULL) {
		    *ptr = '\0';
	    }
	}
	fclose(fd);
	return (*env)->NewStringUTF(env, readbuffer);
}

JNIEXPORT jint JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_losetup
  (JNIEnv *env, jobject obj, jstring fileName, jstring loDevName) {
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);
    const jbyte* lodevname = (*env)->GetStringUTFChars(env, loDevName, NULL);

	char command[512];
	snprintf(command, 512, "losetup %s %s", lodevname, filename);
	int returnValue = run_command_and_get_status(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	(*env)->ReleaseStringUTFChars(env, loDevName, lodevname);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_findFreeLoopback
  (JNIEnv *env, jobject obj) {
	char command[64];
	snprintf(command, 64, "losetup -f");
	jstring returnValue = run_command(env, command, 1);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_removeLogicalVolume 
  (JNIEnv *env, jobject obj, jstring lvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
    char command[128];

	snprintf(command, 128, "lvremove -f %s", lv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_removePhysicalVolume
  (JNIEnv *env, jobject obj, jstring pvName) {
    const jbyte* pv_name = (*env)->GetStringUTFChars(env, pvName, NULL);
    char command[128];

	snprintf(command, 128, "pvremove %s", pv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, pv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_removeLoopback
  (JNIEnv *env, jobject obj, jstring loDevName) {
    const jbyte* lo_dev_name = (*env)->GetStringUTFChars(env, loDevName, NULL);
    char command[128];

	snprintf(command, 128, "losetup -d %s", lo_dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, loDevName, lo_dev_name);
    return returnValue;
}

JNIEXPORT jstring Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_reduceVolumeGroup 
  (JNIEnv *env, jobject obj, jstring vgName, jstring pvName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, pvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "vgreduce %s %s", vg_name, dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, dev_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_enableLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	char command[256];

	snprintf(command, 256, "lvchange -ay %s", lv_name);
    jstring returnValue = run_command(env, command, 1);

    (*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_disableLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	char command[256];

	snprintf(command, 256, "lvchange -an %s", lv_name);
    jstring returnValue = run_command(env, command, 1);

    (*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_createVolumeFromLv
  (JNIEnv *env, jobject obj, jstring lvName, jstring volumePath) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
    const jbyte* volume_path = (*env)->GetStringUTFChars(env, volumePath, NULL);
	char command[256];

	snprintf(command, 256, "dd if=%s of=%s bs=%s", lv_name, volume_path, blockSize);
	jstring returnValue = run_command(env, command, 1);

    (*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    (*env)->ReleaseStringUTFChars(env, volumePath, volume_path);    
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_getLvmVersion
  (JNIEnv *env, jobject obj) {
	char command[256];

    jstring returnValue = run_command(env, "lvm version", 1);

    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_fs_FileSystemStorageManager_removeVolumeGroup
  (JNIEnv *env, jobject obj, jstring vgName) {
    const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
    char command[128];

	snprintf(command, 128, "vgremove %s", vg_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
    return returnValue;
}
