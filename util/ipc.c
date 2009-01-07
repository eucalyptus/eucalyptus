#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <semaphore.h>
#include <sys/stat.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <fcntl.h> /* For O_* */
#include "misc.h" /* logprintfl */
#include "ipc.h"

/* these are wrappers support BOTH SYS V semaphores and the POSIX named
   semaphores depending on whether name was passed to sem_alloc() */

#define DECLARE_ARG union semun { int val; struct semid_ds *buf; ushort *array; } arg

sem * sem_alloc (const int val, const char * name) 
{
    DECLARE_ARG;

    sem * s = malloc (sizeof (sem));
    if (s==NULL) return NULL;
    bzero (s, sizeof (sem));
    s->sysv = -1;

    if (name) { /* named semaphores */
        if ( sem_unlink (name) == 0) { /* clean up in case previous sem holder crashed */
            logprintfl (EUCAINFO, "sem_alloc(): cleaning up old semaphore %s\n", name);
        }
        if ((s->posix = sem_open (name, O_CREAT | O_EXCL, 0644, val))==SEM_FAILED) {
            free (s);
            return NULL;
        }
        s->name = strdup (name);

    } else { /* SYS V IPC semaphores */
        s->sysv = semget (IPC_PRIVATE, /* private to process & children */
                        1, /* only need one */
                        IPC_CREAT | IPC_EXCL | S_IRUSR | S_IWUSR /* user-only */);
        if (s->sysv<0) {
            free (s);
            return NULL;
        }
        
        /* set the value */
        arg.val = val;
        if (semctl(s->sysv, 0, SETVAL, arg) == -1) {
            free (s);
            return NULL;
        }
    }
    
    return s;
}

int sem_p (sem * s)
{
    if (s && s->posix) {
        return sem_wait (s->posix);
    }

    if (s && s->sysv > 0) {
        struct sembuf sb = {0, -1, 0};
        return semop (s->sysv, &sb, 1);
    }

    return -1;
}

int sem_v (sem * s)
{
    if (s && s->posix) {
        return sem_post (s->posix);
    }

    if (s && s->sysv > 0) {
        struct sembuf sb = {0, 1, 0};
        return semop (s->sysv, &sb, 1);
    }
    
    return -1;
}

void sem_free (sem * s)
{
    DECLARE_ARG;
    
    if (s && s->posix) {
        sem_close (s->posix);
        sem_unlink (s->name);
        free (s->name);
    }
    
    if (s && s->sysv > 0) {
        semctl (s->sysv, 0, IPC_RMID, arg); /* TODO: check return */
    }
    
    free (s);
}
