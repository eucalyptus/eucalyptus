#ifndef IPC_H
#define IPC_H

#include <sys/ipc.h>
#include <sys/sem.h>
#include <semaphore.h>

typedef struct sem_struct {
    int sysv;
    sem_t * posix;
    char * name;
} sem;

sem * sem_alloc (const int val, const char * name);
int   sem_p (sem * s);
int   sem_v (sem * s);
void  sem_free (sem * s);

#endif /* IPC_H */
