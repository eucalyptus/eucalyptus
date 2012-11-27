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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

#include <com_eucalyptus_storage_OverlayManager.h>
#include <storage_native.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <sys/wait.h>
#include <signal.h>

#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

static const char *blockSize = "1M";

int run_command_and_get_pid(char *cmd, char **args)
{
	int pid = -1;
	int fds_to_close[1024];
	int curr_fd = 0;

	if ((pid = fork()) == -1) {
		perror("Could not run command");
		return -1;
	}

	if (pid == 0) {
		DIR *proc_fd_dir;
		struct dirent *fd_dir;
		int fd_to_close;
		char fd_path[128];
		int my_pid = getpid();

		umask(0);
		int sid = setsid();
		if (sid < 0)
			exit(-1);
		char *home = getenv(EUCALYPTUS_ENV_VAR_NAME);
		if (!home) {
			home = strdup("");	/* root by default */
		} else {
			home = strdup(home);
		}
		fprintf(stderr, "command: %s\n", cmd);
		chdir(home);

		//close all open fds
		snprintf(fd_path, 128, "/proc/%d/fd", my_pid);

		if ((proc_fd_dir = opendir(fd_path)) != NULL) {
			curr_fd = 0;
			while ((fd_dir = readdir(proc_fd_dir)) != NULL) {
				if (isdigit(fd_dir->d_name[0])) {
					fds_to_close[curr_fd++] = atoi(fd_dir->d_name);
				}
			}
			int i = 0;
			for (i = 0; i < curr_fd; ++i) {
				close(fds_to_close[i]);
			}
		} else {
			int i = 0;
			for (i = 0; i < 1024; ++i) {
				close(i);
			}
		}

		freopen("/dev/null", "r", stdin);
		freopen("/dev/null", "w", stdout);
		freopen("/dev/null", "w", stderr);

		exit(execvp(cmd, args));
	}
	return pid;
}

void sigchld(int signal)
{
	while (0 < waitpid(-1, NULL, WNOHANG)) ;
}

JNIEXPORT void JNICALL Java_com_eucalyptus_storage_OverlayManager_registerSignals(JNIEnv * env, jobject obj) {
	signal(SIGCHLD, sigchld);
}
