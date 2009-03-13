#ifndef WALRUS_H
#define WALRUS_H
int walrus_object_by_url (const char * url, const char * outfile, const int do_compress);
int walrus_object_by_path (const char * path, const char * outfile, const int do_compress);
int walrus_image_by_manifest_url (const char * url, const char * outfile, const int do_compress);
int walrus_image_by_manifest_path (const char * manifest_path, const char * outfile, const int do_compress);
int walrus_verify_digest (const char * url, const char * digest_path);
#endif /* WALRUS_H */
