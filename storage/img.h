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

typedef struct _img_loc {
    enum { PATH, HTTP, VSPHERE, WALRUS, SFTP } type;
    char url [SIZE];
    char path [SIZE]; // dir/file
	char dir [SIZE]; 
	char file [SIZE];
    char vsphere_host [SIZE];
    char vsphere_dcPath [SIZE];
    char vsphere_dsName [SIZE];
    img_creds creds;
} img_loc;

typedef struct _img_spec {
    enum { EMI, EKI, ERI, DISK, VDDK } type;
    char id [SIZE];
    img_loc location;
    int size;
} img_spec;
    
typedef struct _img_env {
    char initialized;
    img_loc wloc;
    int wloc_max_mb;
    img_loc cloc;
    int cloc_max_mb;
    img_creds default_walrus_creds;
    img_creds default_node_creds;
} img_env;

img_env * img_init (char * wdir, int wdir_max_mb, char * cdir, int cdir_max_mb);
void img_cleanup (void);
int img_init_spec (img_spec * spec, const char * id, const char * loc, const img_creds * creds);
int img_convert (img_spec * root, img_spec * kernel, img_spec * ramdisk, img_spec * dest, const char * key, int rsize_mb, int ssize_mb, int esize_mb);
    
#endif
