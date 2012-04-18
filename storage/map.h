// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#ifndef MAP_H
#define MAP_H

typedef struct _map {
    char * key;
    void * val;
    struct _map * next;
} map;

map * map_create (int size);
void map_set (map * m, const char * key, void * val);
void * map_get (map * m, char * key);

#endif // MAP_H
