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
