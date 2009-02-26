#include <stdio.h>
#include <stdlib.h>
#include <unistd.h> /* usleep */
#include <pthread.h>
#include "ipc.h"
#include "misc.h"
#include "data.h"

#define ITER 160*4

void test_sem_fork (void) 
{
    sem * s = sem_alloc (1, "eucalyptus-util-test");
    
    printf ("---> testing semaphores between processes\n");
    
    if (fork()) {
        char c = 'A';
        int i;
        sleep (1);
        printf ("A trying sem...\n");
        sem_p (s);
        printf ("A got sem!\n");
        sleep (1);
        printf ("A releasing sem...\n");
        sem_v (s);
        sleep (1);
        
        for (i=0; i<ITER; i++) {
            sem_p(s);
            if (i%16==0) printf ("\n");
            write (1, &c, 1);
            usleep (100);
            write (1, &c, 1);
            sem_v(s);
        }
        
    } else {
        char c = 'B';
        int i;
        
        printf ("B trying sem...\n");
        sem_p (s);
        printf ("B got sem!\n");
        sleep (2);
        printf ("B releasing sem...\n");
        sem_v (s);
        sleep (2);
        
        for (i=0; i<ITER; i++) {
            sem_p(s);
            write (1, &c, 1);
            usleep (100);
            write (1, &c, 1);
            sem_v(s);
        }
        exit (0); /* child quits */
    }

    sem_free (s);
    printf ("\n");
}

void * thread_a (void * arg)
{
    sem * s = (sem *)arg;
    char c = 'T';
    int i;
    sleep (1);
    printf ("T trying sem...\n");
    sem_p (s);
    printf ("T got sem!\n");
    sleep (1);
    printf ("T releasing sem...\n");
    sem_v (s);
    sleep (1);
    
    for (i=0; i<ITER; i++) {
        sem_p(s);
        if (i%16==0) printf ("\n");
        write (1, &c, 1);
        usleep (100);
        write (1, &c, 1);
        sem_v(s);
    }
    return NULL;
}

void * thread_b (void * arg)
{
    sem * s = (sem *)arg;
    char c = 'U';
    int i;
    
    printf ("U trying sem...\n");
    sem_p (s);
    printf ("U got sem!\n");
    sleep (2);
    printf ("U releasing sem...\n");
    sem_v (s);
    sleep (2);
    
    for (i=0; i<ITER; i++) {
        sem_p(s);
        write (1, &c, 1);
        usleep (100);
        write (1, &c, 1);
        sem_v(s);
    }
    return NULL;
}

void test_sem_pthreads (void) 
{
    sem * s = sem_alloc (1, "eucalyptus-util-test2");
    pthread_t ta, tb;
    void * status;

    printf ("---> testing semaphores between threads\n");
    pthread_create (&ta, NULL, thread_a, s);
    pthread_create (&tb, NULL, thread_b, s);
    pthread_join (ta, &status);
    pthread_join (tb, &status);
    printf ("\n");

    sem_free (s);
}

#define EXIT { fprintf (stderr, "error on line %d\n", __LINE__); exit (1); }

static void test_volumes (void)
{
    int i, j;
    char id [100];
    ncInstance inst;
    ncVolume * vols [EUCA_MAX_VOLUMES+1];

    for (j=0; j<10; j++) {
        int pivot = random()%EUCA_MAX_VOLUMES;
        printf ("testing volumes iteration=%d pivot=%d\n", j, pivot);
        bzero (&inst, sizeof (ncInstance));
        for (i=0; i<EUCA_MAX_VOLUMES; i++) {
            snprintf (id, 100, "v%06d", i);
            vols [i] = add_volume (&inst, id, "remote", "local");
            if (vols [i] == NULL) {
                fprintf (stderr, "error on add iteration %i-%i\n", i, j);
                EXIT;
            }
            if (inst.volumesSize!=i+1) {
                fprintf (stderr, "error on add iteration %i-%i\n", i, j);
                EXIT;
            }
        }

        snprintf (id, 100, "v%06d", i);
        vols [i] = add_volume (&inst, id, "remote", "local");
        if (vols [i] != NULL) EXIT;
        if (inst.volumesSize!=EUCA_MAX_VOLUMES) EXIT;
        
        ncVolume * v = vols [pivot];
        strncpy (id, v->volumeId, 100);
        ncVolume * v2 = free_volume (&inst, id, "remote", "local");
        if (v2 != v) EXIT;
        if (inst.volumesSize!=EUCA_MAX_VOLUMES-1) EXIT;
        v = add_volume (&inst, id, "remote", "local");
        if (v == NULL) EXIT;
        if (inst.volumesSize!=EUCA_MAX_VOLUMES) EXIT;
        v2 = free_volume (&inst, id, "remote", "local");
        if (v2 != v) EXIT;
        if (inst.volumesSize!=EUCA_MAX_VOLUMES-1) EXIT;
        v = add_volume (&inst, id, "remote", "local");
        if (v == NULL) EXIT;
        if (inst.volumesSize!=EUCA_MAX_VOLUMES) EXIT;

        for (i=0; i<EUCA_MAX_VOLUMES; i++) {
            snprintf (id, 100, "v%06d", i);
            v = free_volume (&inst, id, "remote", "local");
            if (v == NULL) {
                fprintf (stderr, "error on free iteration %i-%i\n", i, j);
                EXIT;
            }
            if (inst.volumesSize!=EUCA_MAX_VOLUMES-i-1) {
                fprintf (stderr, "error on free iteration %i-%i\n", i, j);
                EXIT;
            }
        }
    }
}

int main (int argc, char * argv[])
{
    if ( diff ("/etc/motd", "/etc/motd") != 0 ) EXIT;
    if ( diff ("/etc/passwd", "/etc/motd") == 0 ) EXIT;

    char * s = strdup("jolly old jolly old time...");
    char ** sp = &s;
    if ( strcmp ( replace_string ( sp, "old", "new"), "jolly new jolly new time..." ) ) EXIT;
    if ( vrun ( "ls / /etc >/dev/null" ) ) EXIT;

    test_volumes ();

    printf ("all tests passed!\n");
    if (argc==1) return 0;

    /* "visual" testing of the semaphores */
    test_sem_fork ();
    test_sem_pthreads ();

    return 0;
}
