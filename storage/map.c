// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * a dumb and dangerous map library (uses a linked list, allocates and
 * frees memory for keys, but leaves memory management of values to the
 * user)
 */

#include <stdio.h>
#include <malloc.h>
#include <string.h>
#include "map.h"

map * map_create (int size)
{
    return (map *) calloc(1, sizeof(map));
}

inline static void set (map * m, const char * key, void * val)
{
    m->key = strdup (key);
    m->val = val;
    m->next = NULL;
}

void map_set (map * m, const char * key, void * val)
{
    // empty map
    if (m->key==NULL) {
        set (m, key, val);
        return;
    }

    // map has stuff
    map * mp;
    for (mp=m; mp; mp=mp->next) {

        // key already exists, overwrite it
        if (strcmp (key, mp->key) == 0) {
            mp->val = val;
            return;
        }

        // we are at the end, so add a new entry
        if (mp->next==NULL) {
            mp->next = (map *) calloc(1, sizeof(map));
            mp = mp->next;
            if(mp != NULL) // TODO: need to return an error if calloc failed
                set (mp, key, val);
            return;
        }
    }
}

void * map_get (map * m, char * key)
{
    map * mp;

    for (mp=m; mp!=NULL; mp=mp->next) {
        if (mp->key!=NULL && strcmp (key, mp->key)==0) {
            return mp->val;
        }
    }
    return NULL;
}

// little unit test: compile with gcc -g -D_TEST_MAP map.c

#ifdef _TEST_MAP
#include <assert.h>

int main (int argc, char * argv[])
{
    char * s1 = "string 1";
    char * s2 = "string 2";

    map * m = map_create (10);
    assert (map_get (m, "foo")==NULL);
    map_set (m, "k1", s1);
    assert (map_get (m, "k1")==s1);
    map_set (m, "k2", s2);
    assert (map_get (m, "k2")==s2);
    map_set (m, "k2", s1);
    assert (map_get (m, "k2")==s1);

    return 0;
}
#endif // _TEST_MAP
