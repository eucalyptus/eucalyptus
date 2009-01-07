# Makefile for eucalyptus/util
# 
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#

include ../Makedefs

all: misc.o data.o euca_auth.o ipc.o

build: all

test: test.c ipc.o misc.o
	$(CC) $(CFLAGS) $(INCLUDES) -o test test.c ipc.o misc.o -lpthread $(LIBS)

.c.o: $@
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

clean:
	rm -rf *~ *.o test

distclean:

install:

deploy:

