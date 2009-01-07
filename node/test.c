#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "misc.h"
#include "data.h"

void test_command (char * command)
{
    char * result = system_output(command);
    int max = 160;

    if (result && strlen(result)>max) {
        result[max-4] = '.';
        result[max-3] = '.';
        result[max-2] = '.';
        result[max-1] = 0;
    }
    printf("--->%s executed\noutput=[%s]\n\n", command, result);
    free (result);
}

int main (int argc, char **argv) 
{
    printf ("=====> testing misc.c\n");

    test_command("date");
    test_command("ls / -l | sort");
    test_command("/foo");
    {
        char c = 0;
        long l = 0;
        int i = 0;
        long long ll = 0;

        sscanf_lines  ("a1\na\na2\n", "a%d", &i);
        assert (i==1);
        sscanf_lines  ("a\nab3\na   4\na5", "a %d", &i);
        assert (i==4);
        sscanf_lines  ("", "%d", &i);
        sscanf_lines  ("\n\n\n", "%d", &i);
        sscanf_lines  ("abcdefg6", "g%d", &i);
        assert (i!=6);
        sscanf_lines  ("abcdefg", "ab%cdefg", &c);
        assert (c=='c');
        sscanf_lines  ("a\na    7\na\n", "a %ld", &l);
        assert (l==7L);
        sscanf_lines  ("a\n8a\na9\n", "a %lld", &ll);
        assert (ll==9L);
    }

    printf ("=====> testing data.c\n");
    {
#define INSTS 50
        bunchOfInstances * bag = NULL;
        ncInstance * inst = NULL;
        ncInstance * Insts[INSTS];
        int i, n;

        free_instance (NULL);
        free_instance (&inst);
        inst = allocate_instance ("i1", NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL);
        assert(inst!=NULL);
        free_instance (&inst);
        assert(inst==NULL);
        
        n = total_instances (&bag);
        assert(n==0);
        bag=NULL;

        inst = find_instance(&bag, "foo");
        assert(inst==NULL);
        bag=NULL;

        n = remove_instance(&bag, NULL);
        assert(n!=0);
        bag=NULL;

        for (i=0; i<INSTS; i++) {
            char id[10];
            sprintf(id, "i-%d", i);
            inst = Insts[i] = allocate_instance(id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL);
            assert (inst!=NULL);
            n = add_instance(&bag, inst);
            assert (n==0);
        }
        n = total_instances (&bag);
        assert (n==INSTS);
        n = remove_instance(&bag, Insts[0]);
        assert (n==0);
        n = remove_instance(&bag, Insts[INSTS-1]);
        assert (n==0);
        n = total_instances (&bag);
        assert (n==INSTS-2);

        printf ("OK\n");
    }
    return 0;
}
