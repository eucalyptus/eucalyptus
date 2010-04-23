# Makefile for eucalyptus/storage
#
#

include ../Makedefs

STORAGE_LIBS = -lcurl
IMAGER_LIBS = ../util/euca_auth.o ../util/misc.o ../util/ipc.o walrus.o img.o cache.o ami2vmx.o do_convert.o http.o

all: storage.o storage-windows.o walrus.o Wclient euca_imager

build: all

euca_imager: Makefile euca_imager.c $(IMAGER_LIBS)
	$(CC) $(CFLAGS) $(INCLUDES) euca_imager.c -o euca_imager $(IMAGER_LIBS) -lcurl -lrt

Wclient: Makefile Wclient.c ../util/euca_auth.o ../util/misc.o walrus.o http.o
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient ../util/euca_auth.o ../util/misc.o walrus.o http.o $(STORAGE_LIBS)

walrus.o: walrus.c walrus.h
	$(CC) $(CFLAGS) $(INCLUDES) -c walrus.c 

test: test.c storage.h storage.o storage-windows.o ../util/misc.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o
	$(CC) $(CFLAGS) $(INCLUDES) test.c storage.o storage-windows.o ../util/misc.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o $(STORAGE_LIBS) -o test

.c.o: 
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

../util/ipc.o: ../util/ipc.c ../util/ipc.h ../util/eucalyptus.h
	make -C ../util

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/euca_auth.o: ../util/euca_auth.c ../util/euca_auth.h
	make -C ../util

clean:
	rm -rf *~ *.o Wclient euca_imager

distclean:

install:
	@$(INSTALL) -m 0755 euca_imager $(DESTDIR)$(usrdir)/lib/eucalyptus
deploy:

uninstall:
	@$(RM) -f $(usrdir)/lib/eucalyptus/euca_imager

