#ifndef CACHE_H
#define CACHE_H
long long init_cache (const char * cache_path);
void add_to_cache (const char * cached_path, const long long file_size_bytes);
void print_cache (void);
int ok_to_cache (const char * cached_path, const long long file_size_bytes);
int test_cache (void);
#endif // CACHE_H

