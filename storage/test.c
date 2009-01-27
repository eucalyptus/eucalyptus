#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "storage.h"

int main (int argc, char **argv) 
{
    printf ("=====> testing storage.c\n");
    int err = test_cache ();
    printf ("  error=%d\n", err);
    return err;
}
