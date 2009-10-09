#ifndef IMG_H
#define IMG_H

#define SIZE 512

typedef struct _img_creds {
    enum { NONE, PASSWORD, X509CREDS, SSHKEY } type;
    char login [SIZE];
    char password [SIZE];
    char pk_path [SIZE];
    char cert_path [SIZE];
    char ssh_key_path [SIZE];
} img_creds;

typedef struct _img_ptr {
    enum { PATH, HTTP, WALRUS, SFTP } type;
    char loc [SIZE];
    img_creds creds;
} img_ptr;

typedef struct _img_spec {
    char id [SIZE];
    img_ptr src;
    img_ptr dst;
} img_spec;
    
typedef struct _img_env {
    char initialized;
    char wdir [SIZE]; int wdir_max_mb;
    char cdir [SIZE]; int cdir_max_mb;
    img_creds default_walrus_creds;
    img_creds default_node_creds;
} img_env;

img_env * img_init (char * wdir, int wdir_max_mb, char * cdir, int cdir_max_mb);
void img_cleanup (void);

int img_init_spec (img_spec * spec, const char * id, 
    const char * src, const img_creds * src_creds, 
    const char * dst, const img_creds * dst_creds);

int img_convert (const char * fmt, const char * unique,
    img_spec * root, img_spec * kernel, img_spec * ramdisk, 
    const char * key, int rsize_mb, int ssize_mb, int esize_mb);
    
#endif