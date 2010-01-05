# Makefile for eucalyptus/util
# 
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
	rm -rf config.h

install:
	@$(INSTALL) -m 0755 euca_rootwrap $(DESTDIR)$(usrdir)/lib/eucalyptus/
	@$(INSTALL) -m 0755 euca_mountwrap $(DESTDIR)$(usrdir)/lib/eucalyptus/

deploy:

uninstall:
	@$(RM) -f $(usrdir)/lib/eucalyptus/euca_rootwrap
	@$(RM) -f $(usrdir)/lib/eucalyptus/euca_mountwrap

