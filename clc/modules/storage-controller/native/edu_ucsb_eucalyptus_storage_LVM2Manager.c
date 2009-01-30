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

#include <edu_ucsb_eucalyptus_storage_LVM2Manager.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

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

    snprintf(absolute_cmd, 256, "%s/usr/share/eucalyptus/euca_rootwrap %s", home, cmd);
    fprintf(stderr, "command: %s\n", absolute_cmd);
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

int run_command_and_get_pid(char *cmd, char **args) {
    int fd[2];
    pipe(fd);
    int pid = -1;

    if ((pid = fork()) == -1) {
        perror("Could not run command");
        return -1;
    }

   if (pid == 0) {
        close(fd[0]);
        dup2(fd[1], 2);
        execvp(cmd, args);
   } else {
        close(fd[1]);
   }
   return pid;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getAoEStatus
  (JNIEnv *env, jobject obj, jstring processId) {
    const jbyte* pid = (*env)->GetStringUTFChars(env, processId, NULL);

    char command[128];
    snprintf(command, 128, "cat /proc/%s/cmdline", pid);                                                
	
    jstring returnValue = run_command(env, command, 1);	
    (*env)->ReleaseStringUTFChars(env, processId, pid);
    return returnValue;
}

JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createSnapshot
(JNIEnv *env, jobject obj, jstring snapshotId) {
	const jbyte *snapshot_id;
	snapshot_id = (*env)->GetStringUTFChars(env, snapshotId, NULL);
	(*env)->ReleaseStringUTFChars(env, snapshotId, snapshot_id);
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_losetup__Ljava_lang_String_2
(JNIEnv *env, jobject obj, jstring fileName) {
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);

	char command[512];
	snprintf(command, 512, "losetup -sf %s", filename);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_losetup__Ljava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jobject obj, jstring fileName, jstring loDevName) {
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);
    const jbyte* lodevname = (*env)->GetStringUTFChars(env, loDevName, NULL);

	char command[512];
	snprintf(command, 512, "losetup %s %s", lodevname, filename);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	(*env)->ReleaseStringUTFChars(env, loDevName, lodevname);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getLoopback
  (JNIEnv *env, jobject obj, jstring loDevName) {
	const jbyte* lodevname = (*env)->GetStringUTFChars(env, loDevName, NULL);

	char command[128];
	snprintf(command, 128, "losetup -s %s", lodevname);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, loDevName, lodevname);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createEmptyFile
(JNIEnv *env, jobject obj, jstring fileName, jint size) {
	char command[256];
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);
    size = size * 1024;
	snprintf(command, 256, "dd if=/dev/zero of=%s count=%d bs=%s", filename, size, blockSize);

	jstring returnValue = run_command(env, command, 2);

	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createPhysicalVolume
(JNIEnv *env, jobject obj, jstring loDevName) {
	const jbyte* dev_name = (*env)->GetStringUTFChars(env, loDevName, NULL);
	char command[128];

	snprintf(command, 128, "pvcreate %s", dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, loDevName, dev_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createVolumeGroup
(JNIEnv *env, jobject obj, jstring pvName, jstring vgName) {
	const jbyte* dev_name = (*env)->GetStringUTFChars(env, pvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "vgcreate %s %s", vg_name, dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, dev_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);    
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createLogicalVolume
(JNIEnv *env, jobject obj, jstring vgName, jstring lvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "lvcreate -n %s -l 100%%FREE %s", lv_name, vg_name);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);

	return returnValue;
}

JNIEXPORT jint JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_aoeExport
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

JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_aoeUnexport
  (JNIEnv *env, jobject obj, jint vblade_pid) {
    //TODO: blind kill. Hope for the best.
   char command[128];

   snprintf(command, 128, "kill %d", vblade_pid);
   run_command(env, command, 1);
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removeLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
    char command[128];

	snprintf(command, 128, "lvremove -f %s", lv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removeVolumeGroup
  (JNIEnv *env, jobject obj, jstring vgName) {
    const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
    char command[128];

	snprintf(command, 128, "vgremove %s", vg_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removePhysicalVolume
  (JNIEnv *env, jobject obj, jstring pvName) {
    const jbyte* pv_name = (*env)->GetStringUTFChars(env, pvName, NULL);
    char command[128];

	snprintf(command, 128, "pvremove %s", pv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, pv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removeLoopback
  (JNIEnv *env, jobject obj, jstring loDevName) {
    const jbyte* lo_dev_name = (*env)->GetStringUTFChars(env, loDevName, NULL);
    char command[128];

	snprintf(command, 128, "losetup -d %s", lo_dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, loDevName, lo_dev_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createSnapshotLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName, jstring snapLvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	const jbyte* snap_lv_name = (*env)->GetStringUTFChars(env, snapLvName, NULL);
	char command[256];

	snprintf(command, 256, "lvcreate -n %s -s -l 100%%FREE %s", snap_lv_name, lv_name);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
	(*env)->ReleaseStringUTFChars(env, snapLvName, snap_lv_name);

	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_extendVolumeGroup
  (JNIEnv *env, jobject obj, jstring pvName, jstring vgName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, pvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "vgextend %s %s", vg_name, dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, dev_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_reduceVolumeGroup
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

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_suspendDevice
  (JNIEnv *env, jobject obj, jstring deviceName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, deviceName, NULL);
	char command[128];

	snprintf(command, 128, "dmsetup -v suspend %s", dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, deviceName, dev_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_resumeDevice
  (JNIEnv *env, jobject obj, jstring deviceName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, deviceName, NULL);
	char command[128];

	snprintf(command, 128, "dmsetup -v resume %s", dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, deviceName, dev_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_duplicateLogicalVolume
  (JNIEnv *env, jobject obj, jstring oldLvName, jstring newLvName) {
    const jbyte* old_lv_name = (*env)->GetStringUTFChars(env, oldLvName, NULL);
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, newLvName, NULL);
	char command[256];

	snprintf(command, 256, "dd if=%s of=%s", old_lv_name, lv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, oldLvName, old_lv_name);
	(*env)->ReleaseStringUTFChars(env, newLvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_enableLogicalVolume
  (JNIEnv *env, jobject obj, jstring absoluteLvName) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, absoluteLvName, NULL);
	char command[256];

	snprintf(command, 256, "lvchange -ay %s", lv_name);
    jstring returnValue = run_command(env, command, 1);

    (*env)->ReleaseStringUTFChars(env, absoluteLvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getLvmVersion
  (JNIEnv *env, jobject obj) {
	char command[256];

   	snprintf(command, 256, "lvdisplay --version");
    jstring returnValue = run_command(env, command, 1);
   
    return returnValue;
}
