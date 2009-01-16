# Makefile for eucalyptus/util
# 
# $Id: Makefile,v 1.6 2009-01-06 21:42:52 nurmi Exp $
#

include ../Makedefs

all: misc.o data.o euca_auth.o ipc.o euca_rootwrap euca_mountwrap

build: all 

euca_rootwrap:
	$(CC) $(CFLAGS) $(INCLUDES) -o euca_rootwrap euca_rootwrap.c

euca_mountwrap:
	$(CC) $(CFLAGS) $(INCLUDES) -o euca_mountwrap euca_mountwrap.c

test: test.c ipc.o misc.o
	$(CC) $(CFLAGS) $(INCLUDES) -o test test.c ipc.o misc.o data.o -lpthread $(LIBS)

.c.o: $@
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

clean:
	rm -rf *~ *.o test euca_rootwrap euca_mountwrap

distclean:

install:
	@$(INSTALL) -m 0755 euca_rootwrap $(datarootdir)/eucalyptus
	@$(INSTALL) -m 0755 euca_mountwrap $(datarootdir)/eucalyptus

deploy:

