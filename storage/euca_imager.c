#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <unistd.h> // getopt
#include <fcntl.h> // open
#include <ctype.h> // tolower
#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "img.h"

static void usage (const char * msg) 
{ 
    if (msg)
        fprintf (stderr, "error: %s\n\n", msg);
        
    fprintf (stderr, "Usage: euca_imager [parameters]\n"
        "\nRequired parameters:\n"
        "\t-f {xen|kvm}                 output format\n"
        "\t-I [src,dst,id]              disk image SPEC*\n"
        "\t-K [src,dst,id]              kernel image SPEC*\n"
        "\nOptional parameters:\n"
        "\t-R [src,dst,id]              ramdisk image SPEC*\n"
        "\t-S [root,swap,ephemeral]     root partition limit + partition sizes, in MB\n"
        "\t-k [key]                     ssh key file to inject, stdin if '-'\n"
        "\t-W [work dir,max size]       work directory + size in MB, if dst is remote\n"
        "\t-C [cache dir,max size]      cache directory + size in MB, if any\n"
        "\t-l [login] -p [password]     remote dst HTTP auth credentials\n"
        "\t-u [string]                  unique string for this invocation\n"
        "\t-h                           print this message\n"
        "\n* - SPEC consists of source & destination file paths or URLs + unique ID\n"
        "    e.g.: -I http://localhost:8773/services/Walrus/bucket/image.manifest.xml,/tmp/image,emi-12345\n"
        "\n"  
        ); 
    exit (1); 
}

static char * key = NULL;

static void err (const char *format, ...)
{
    va_list ap;
    va_start(ap, format);
    vfprintf(stderr, "error: " format "\n", ap);
    fflush(stderr);
    va_end(ap);
    if (key) free (key);
    img_cleanup ();
    exit (1);
}

// dst is src with all 'old' chars replaced by 'new' chars
static void strnsub (int size, char * dst, const char * src, char old, char new)
{
    if (dst==NULL || src==NULL) return;
    
    int i;
    for (i=0; i<(size-1) && src[i]; i++) {
        if (src[i]==old)
            dst[i]=new;
        else 
            dst[i]=src[i];
    }
    dst [i] = '\0';
}

int main (int argc, char * argv[])
{
    char * fmt = NULL;
    char isrc [256], idst [256], iid [256] = "";
    char ksrc [256], kdst [256], kid [256] = "";
    char rsrc [256], rdst [256], rid [256] = "";
    int rsize = -1; // -1 = unlimited
    int ssize = 0;
    int esize = 0;
    char * key_file = NULL;
    char wdir [256] = ""; int wdir_max = -1; // -1 = unlimited
    char cdir [256] = ""; int cdir_max = -1;
    char * login = NULL; char * password = NULL;
    char s [1024];
    char * unique = NULL;
    
    int ch;
    while ((ch = getopt (argc, argv, "af:I:K:S:R:k:W:C:A:l:p:u:h")) != -1) {
        switch (ch) {
            case 'a':
            for (i=0; i<argc; i++) {
                fprintf (stderr, "%s ", argv[i]);
            }
            fprintf ("\n");
            break;
            
            case 'f':
            fmt = optarg; 
            break;

            case 'I':
            strnsub (sizeof(s), s, optarg, ',', ' ');
            if (sscanf (s, "%255s %255s %255s", isrc, idst, iid)!=3)
                usage ("failed to parse -I parameter");
            break;

            case 'K':
            strnsub (sizeof(s), s, optarg, ',', ' ');
            if (sscanf (s, "%255s %255s %255s", ksrc, kdst, kid)!=3)
                usage ("failed to parse -K parameter");
            break;

            case 'R':
            strnsub (sizeof(s), s, optarg, ',', ' ');
            if (sscanf (s, "%255s %255s %255s", rsrc, rdst, rid)!=3)
                usage ("failed to parse -R parameter");
            break;

            case 'S':
            strnsub (sizeof(s), s, optarg, ',', ' ');
            if (sscanf (s, "%d %d %d", &rsize, &ssize, &esize)!=3)
                usage ("failed to parse -S parameter");
            break;

            case 'k':
            key_file = optarg;
            break;

            case 'W':
            strnsub (sizeof(s), s, optarg, ',', ' ');
            if (sscanf (s, "%255s %d", wdir, &wdir_max)!=2)
                usage ("failed to parse the working dir spec");
            break;

            case 'C':
            strnsub (sizeof(s), s, optarg, ',', ' ');
            if (sscanf (s, "%255s %d", cdir, &cdir_max)!=2)
                usage ("failed to parse the cache dir spec");
            break;
            
            case 'l':
            login = optarg;
            break;
            
            case 'p':
            password = optarg;
            break;

            case 'u':
            unique = optarg;
            break;
            
            case 'h': 
            case '?':
            default:
            usage (NULL);
        }
    }
    argc -= optind;
    argv += optind;

    if (argc>0)
        usage ("unexpected parameter(s)");

    if (fmt==NULL || strlen (iid)<1 )
        usage ("missing required parameter(s)");
    
    logfile (NULL, EUCADEBUG); // so euca libs will log to stdout
    
    // read in the contents of the key file, if any
    if (key_file!=NULL) {
        FILE * fp;
        if (strncmp(key_file, "-", 1)==0) {
            fp = stdin;
        } else {
            fp = fopen (key_file, "r");
        }
        key = fp2str (fp);
        if (key==NULL) {
            err ("failed to read the key file %s", key_file);
        }
    }
    
    // initialize the image converter environment, checking up on certs along the way
    img_env * env;
    if ((env=img_init (wdir, wdir_max, cdir, cdir_max))==NULL)
        err ("failed to initialize image converter");

    // if login & password were specified, use them for destination credentials
    img_creds creds = { type: NONE };
    if (login!=NULL || password!=NULL) {
        if (login==NULL || password==NULL) 
            usage ("both login and password must be specified");
        strncpy (creds.login, login, sizeof(creds.login));
        strncpy (creds.password, password, sizeof(creds.password));
        creds.type=HTTP;
    }
    
    // use the same creds for all sources and same creds for all destinations
    img_creds * src_creds = &(env->default_walrus_creds); // Walrus creds from default location, if found
    img_creds * dst_creds = &creds;
     
    img_spec root; 
    img_spec kernel;
    img_spec ramdisk;
    if (img_init_spec (&root,    iid, isrc, src_creds, idst, dst_creds)) exit (1);
    if (img_init_spec (&kernel,  kid, ksrc, src_creds, kdst, dst_creds)) exit (1); // kid=="" is OK
    if (img_init_spec (&ramdisk, rid, rsrc, src_creds, rdst, dst_creds)) exit (1); // rid=="" is OK
    
    // do all the hard work
    int ret = img_convert (fmt, unique, &root, &kernel, &ramdisk, key, rsize, ssize, esize);
    if (ret) err ("download, conversion, or upload failed");
    
    img_cleanup ();
    free (key);
    return ret;
}
