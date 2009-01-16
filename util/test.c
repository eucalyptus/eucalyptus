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

int main (int argc, char * argv[])
{
    if ( diff ("/etc/motd", "/etc/motd") != 0 ) EXIT
    if ( diff ("/etc/passwd", "/etc/motd") == 0 ) EXIT

    char * s = strdup("jolly old jolly old time...");
    char ** sp = &s;
    if ( strcmp ( replace_string ( sp, "old", "new"), "jolly new jolly new time..." ) ) EXIT
    if ( run ( "ls", "/", "/etc", ">/dev/null", NULL ) ) EXIT

    ncInstance inst;
    bzero (&inst, sizeof (ncInstance));
    ncVolume * v1 = add_volume (&inst, "v1", "r1", "l1"); if (v1==NULL) EXIT
    if (inst.volumesSize!=1) EXIT
    ncVolume * v2 = add_volume (&inst, "v2", "r2", "l2"); if (v2==NULL) EXIT
    
    printf ("all tests passed!\n");
    if (argc==0) return 0;

    /* "visual" testing of the semaphores */
    test_sem_fork ();
    test_sem_pthreads ();

    return 0;
}
