#ifndef INCLUDE_ATOMIC_FILE_H
#define INCLUDE_ATOMIC_FILE_H

typedef struct atomic_file_t {
    char tmpfile[MAX_PATH];
    char tmpfilebase[MAX_PATH];
    char dest[MAX_PATH];
    char source[MAX_PATH];
    char *lasthash, *currhash;    
} atomic_file;

int atomic_file_init(atomic_file *file, char *source, char *dest);
int atomic_file_set_source(atomic_file *file, char *newsource);
int atomic_file_get(atomic_file *file, int *file_updated);
int atomic_file_free(atomic_file *file);

#endif
