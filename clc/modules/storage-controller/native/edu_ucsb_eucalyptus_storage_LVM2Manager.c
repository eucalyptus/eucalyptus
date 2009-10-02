/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

#include <edu_ucsb_eucalyptus_storage_LVM2Manager.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <sys/wait.h>
#include <signal.h>

#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

static const char* blockSize = "1M";

int run_command_and_get_pid(char *cmd, char **args) {
    int fd[2];
    pipe(fd);
    int pid = -1;
    char lol[128];
    int fds_to_close[1024];
    int curr_fd = 0;

    if ((pid = fork()) == -1) {
        perror("Could not run command");
        return -1;
    }

   if (pid == 0) {
        //daemonize
        DIR *proc_fd_dir;
        struct dirent *fd_dir;
        int fd_to_close;
        char fd_path[128];
        int my_pid = getpid();

        umask(0);
        int sid = setsid();
        if(sid < 0)
            return -1;
        char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
        if (!home) {
            home = strdup (""); /* root by default */
        } else {
        home = strdup (home);
        }
        fprintf(stderr, "command: %s\n", cmd);
        chdir(home);

        //close all open fds
        snprintf(fd_path, 128, "/proc/%d/fd", my_pid);

        if ((proc_fd_dir = opendir(fd_path)) == NULL) {
            perror("ERROR: Cannot opendir\n");
            return -1;
        }

        curr_fd = 0;
        while ((fd_dir = readdir(proc_fd_dir)) != NULL) {
            if (isdigit(fd_dir->d_name[0])) {
                fds_to_close[curr_fd++] =  atoi(fd_dir->d_name);
            }
        }

        int i = 0;
        for(i=0 ; i < curr_fd; ++i) {
        	close(fds_to_close[i]);
        }

        freopen( "/dev/null", "r", stdin);
        freopen( "/dev/null", "w", stdout);
        freopen( "/dev/null", "w", stderr);
        exit(execvp(cmd, args));
   } else {
        close(fd[1]);
   }
   return pid;
}

void sigchld(int signal)
{
 while (0 < waitpid(-1, NULL, WNOHANG));
}

JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_registerSignals
  (JNIEnv *env, jobject obj) {
  signal(SIGCHLD, sigchld);
}

