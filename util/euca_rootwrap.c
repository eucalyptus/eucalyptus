#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char **argv) {
  char **newargv;
  int i;

  if (argc <= 1) {
    exit(1);
  }
  newargv = argv + 1;
  exit(execvp(newargv[0], newargv));
}
