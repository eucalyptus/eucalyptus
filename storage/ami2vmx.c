#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <getopt.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <ami2vmx.h>

char *helpers[LASTHELPER] = {"losetup", "mount", "kvm-img,qemu-img", "grub", "parted", "mv", "dd", "sync", "mkdir", "cp", "rsync", "umount", "euca_rootwrap", "euca_mountwrap"};
char *helpers_path[LASTHELPER];

#ifdef AMI2VMX

int main(int argc, char **argv) {
  int opt_idx=0, i, rc;
  int help=0, verbose=0, dopause=0, swap=128, ephemeral=128, force=0, bits=64;
  char *infile=NULL, *outfile=NULL, *output=NULL,
    *kernel=NULL, *ramdisk=NULL, *modules=NULL;
  static struct option long_options[] =
    {
      {"verbose", no_argument, 0, 'v'},
      {"help", no_argument, 0, 'h'},
      {"force", no_argument, 0, 'f'},
      {"infile", required_argument, 0, 'i'},
      {"outfile", required_argument, 0, 'o'},
      {"kernel", required_argument, 0, 'k'},
      {"ramdisk", required_argument, 0, 'r'},
      {"modules", required_argument, 0, 'm'},
      {"pause", no_argument, 0, 'p'},
      {"swap", required_argument, 0, 's'},
      {"ephemeral", required_argument, 0, 'e'},
      {"bits", required_argument, 0, 'b'},
      {0,0,0,0}
    };

  
  while(1) {
    i = 0;
    i = getopt_long (argc, argv, "hfvpi:o:k:r:m:s:e:b:", long_options, &opt_idx);
    if (i == -1) {
      break;
    }
    switch (i) {
    case 0:
      if (long_options[opt_idx].flag != 0) {
	break;
      }
      break;
    case 'h':
      help=1;
      break;
    case 'f':
      force=1;
      break;
    case 'v':
      verbose=1;
      break;
    case 'p':
      dopause=1;
      break;
    case 'i':
      infile = strdup(optarg);
      break;
    case 'o':
      outfile = strdup(optarg);
      break;
    case 'k':
      kernel = strdup(optarg);
      break;
    case 'r':
      ramdisk = strdup(optarg);
      break;
    case 'm':
      modules = strdup(optarg);
      break;
    case 's':
      sscanf(optarg, "%d", &swap);
      break;
    case 'e':
      sscanf(optarg, "%d", &ephemeral);
      break;
    case 'b':
      sscanf(optarg, "%d", &bits);
      break;
    default:
      break;
    }
  }
  if (help) {
    usage();
    exit(0);
  }
  if (!infile || !outfile) {
    printf("ERROR: must specify both an infile (-i) and outfile (-o)\n");
    usage();
    exit(1);
  } else if (!kernel || !modules) {
    printf("ERROR: must specify at least a kernel (-k) and a directory containing kernel modules (-m)\n");
    usage();
    exit (1);
  } else if (geteuid() != 0) {
    printf("ERROR: you must be root to run this program\n");
    usage();
    exit(1);
  } else if (bits != 32 && bits != 64) {
    printf("ERROR: invalid bits argument, must be '32' or '64'\n");
    usage();
    exit(1);
  }


  rc = verify_ami2vmx_helpers(force);
  if (rc) {
    printf("ERROR: cannot find needed dependencies\n");
    exit (1);
  }

  rc = verify_input(infile, kernel, ramdisk, modules, force);
  if (rc) {
    printf("ERROR: input not correct\n");
    exit (1);
  }
  
  rc = do_convert(infile, outfile, kernel, ramdisk, modules, swap, ephemeral, bits, dopause, force);
  if (rc) {
    printf("ERROR: conversion failed, infile=%s outfile=%s\n", infile, outfile);
    exit(1);
  }
  exit (0);
}

void usage(void) {
  printf("Usage: ami2vmx [OPTIONS]\n\n");
  printf("Mandatory arguments:\n");
  printf("  -i, --infile\t\tPath to input AMI image file\n");
  printf("  -o, --outfile\t\tPath to output VMX image file\n");
  printf("  -k, --kernel\t\tPath to Linux kernel to use\n");
  printf("  -m, --modules\t\tPath to kernel modules (should match kernel specified by -k)\n");
  printf("\n");
  printf("Optional arguments:\n");
  printf("  -r, --ramdisk\t\tPath to optional ramdisk/initrd\n");
  printf("  -p, --pause\t\tPause operation after mounting input image to allow manual configuration\n");
  printf("  -s, --swap\t\tSize of swap partition in MB (default=128)\n");
  printf("  -e, --ephemeral\tSize of ephemeral disk in MB (default=128)\n");
  printf("  -b, --bits\t\tSpecify 32 or 64 bit architecture (default=64)\n");
  printf("  -f, --force\t\tForce conversion, do not verify inputs (default=0)\n");
  printf("\n");
  printf("Note that you must be root to run this program.\n");
}

#endif

int verify_ami2vmx_helpers(int force, char * extra_path) {
  int i, done, rc, j;
  char *tok, *toka, *path, *helper, file[1024], *save, *savea;
  struct stat statbuf;

  for (i=0; i<LASTHELPER; i++) {
    if (getenv("PATH")==NULL) {
      return(1);
    }
    path = malloc (strlen(getenv("PATH"))+((extra_path==NULL)?(0):(strlen(extra_path)))+2);
    if (path==NULL) {
        return 1;
    }
    sprintf (path, "%s:%s", extra_path, getenv("PATH"));
    
    tok = strtok_r(path, ":", &save);
    done=0;
    while(tok && !done) {
      helper = strdup(helpers[i]);
      toka = strtok_r(helper, ",", &savea);
      while(toka && !done) {
	snprintf(file, 1024, "%s/%s", tok, toka);
	//	printf("statting: %s\n", file);
	rc = stat(file, &statbuf);
	if (rc) {
	} else {
	  if (S_ISREG(statbuf.st_mode)) {
	    done++;
	  }
	}
	toka = strtok_r(NULL, ":", &savea);
      }
      tok = strtok_r(NULL, ":", &save);
    }
    if (!done) {
      printf("ERROR: cannot find helper '%s' in your path\n", helpers[i]);
      return(1);
    }
    
    helpers_path[i] = strdup(file);
    free(path);
  }
  
  
  return(0);
}

int verify_input(char *file, char *kernel, char *ramdisk, char *modules, int force) {
  char *output=NULL, cmd[1024];
  int rc, ret=0;
  struct stat statbuf;

  output = pruntf("file %s", file);
  if (!output) {
    if (!force) {
      return(1);
    }
  }
  if (strstr(output, "ext2 filesystem") || strstr(output, "ext3 filesystem") || strstr(output, "ext4 filesystem")) {
  } else {
    printf("ERROR: supplied image is not an ext2, ext3 or ext4 filesystem '%s'\n", file);
    if (!force) {
      return(1);
    }
  }
  free(output);

  output = pruntf("file %s", kernel);
  if (!output) {
    if (!force) {
      return(1);
    }
  }
  if (strstr(output, "Linux kernel")) {
  } else {
    printf("ERROR: supplied kernel is not a Linux kernel '%s'\n", kernel);
    if (!force) return(1);
  }
  free(output);

  if (ramdisk) {
    rc = stat(ramdisk, &statbuf);
    if (rc) {
      printf("ERROR: cannot stat ramdisk '%s'\n", ramdisk);
      return(1);
    }
  }

/*
  rc = stat(modules, &statbuf);
  if (rc) {
    printf("ERROR: cannot stat modules directory '%s'\n", modules);
    return(1);
  } else if (!S_ISDIR(statbuf.st_mode)) {
    printf("ERROR: supplied modules parameter is not a directory '%s'", modules);
    return(1);
  }
  */

  return(0);
}

char *pruntf(char *format, ...) {
  va_list ap;
  FILE *IF=NULL;
  char cmd[1024], *ptr;
  size_t bytes=0;
  int outsize=1025, rc;
  char *output=NULL;
  
  va_start(ap, format);
  vsnprintf(cmd, 1024, format, ap);
  
  strncat(cmd, " 2>&1", 1024);
  output = NULL;
  
  //  printf("CMD: %s\n", cmd);
  IF=popen(cmd, "r");
  if (!IF) {
    printf("ERROR: cannot popen() cmd '%s' for read\n", cmd);
    return(NULL);
  }
  
  output = malloc(sizeof(char) * outsize);
  while((bytes = fread(output+(outsize-1025), 1, 1024, IF)) > 0) {
    output[(outsize-1025)+bytes] = '\0';
    outsize += 1024;
    output = realloc(output, outsize);
  }
  rc = pclose(IF);
  if (rc) {
    if (output) free(output);
    output = NULL;
    printf("ERROR: bad return code from cmd '%s'\n", cmd);
  } 
  //  printf("OUTPUT: '%s'\n", output);
  return(output);
}

char *pruntff(char *format, ...) {
  va_list ap;
  FILE *IF=NULL;
  char cmd[1024], *ptr;
  size_t bytes=0;
  int outsize=1025, rc;
  char *output;

  va_start(ap, format);
  vsnprintf(cmd, 1024, format, ap);

  output = NULL;
  IF=popen(cmd, "r");
  if (!IF) {
    printf("ERROR: cannot popen() cmd '%s' for read\n", cmd);
    return(NULL);
  }
  
  output = malloc(sizeof(char) * outsize);
  while((bytes = fread(output+(outsize-1025), 1, 1024, IF)) > 0) {
    output[(outsize-1025)+bytes] = '\0';
    outsize += 1024;
    output = realloc(output, outsize);
  }
  rc = pclose(IF);
  if (rc) {
    if (output) free(output);
    output = NULL;
  }
  return(output);
}
