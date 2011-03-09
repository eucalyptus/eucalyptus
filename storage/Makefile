# Makefile for eucalyptus/storage
#
#

include ../Makedefs

STORAGE_LIBS = ${LIBS} -lcurl -lssl -lcrypto
IMAGER_LIBS = ../util/euca_auth.o ../util/misc.o ../util/ipc.o cache.o walrus.o cmd_download.o cmd_create.o cmd_inject.o cmd_convert.o cmd_upload.o cmd_bundle.o cmd_fsck.o map.o vmdk.o http.o img.o diskfile.o diskutil.o
VDDK_FLAGS = -L${VDDK_HOME}/lib -lvixDiskLib -lvixMntapi -std=gnu99 -UDYNAMIC_LOADING
IMAGER_WRAPPER=euca_imager
IMAGER=_euca_imager
#EFENCE=-lefence

all: storage.o storage-windows.o walrus.o Wclient $(IMAGER) test_blobstore
#all: storage.o storage-windows.o walrus.o test_blobstore

build: all

$(IMAGER): Makefile imager.c imager.h cmd.h $(IMAGER_LIBS)
	$(CC) $(CFLAGS) $(INCLUDES) imager.c -o $(IMAGER) $(IMAGER_LIBS) $(VDDK_FLAGS) -lcurl -lrt -lssl -lcrypto

Wclient: Makefile Wclient.c ../util/euca_auth.o ../util/misc.o walrus.o http.o
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient ../util/euca_auth.o ../util/misc.o walrus.o http.o $(STORAGE_LIBS)

walrus.o: walrus.c walrus.h
	$(CC) $(CFLAGS) $(INCLUDES) -c walrus.c 

test: test.c storage.h storage.o storage-windows.o ../util/misc.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o
	$(CC) $(CFLAGS) $(INCLUDES) test.c storage.o storage-windows.o ../util/misc.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o $(STORAGE_LIBS) -o test

test_blobstore: blobstore.o blobstore.c blobstore.h map.o diskutil.o ../util/misc.o
	$(CC) -g -rdynamic -std=gnu99 $(CFLAGS) $(INCLUDES) -D_UNIT_TEST blobstore.c -o test_blobstore map.o diskutil.o ../util/misc.o -lpthread $(EFENCE) ../util/euca_auth.o -lssl 

test_url: http.c
	$(CC) -D_UNIT_TEST -o test_url http.c

.c.o:
	$(CC) -c $(CFLAGS) $(INCLUDES) $(VDDK_FLAGS) $<

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
	rm -rf *~ *.o Wclient $(IMAGER) test test_url

distclean:

install: $(IMAGER) $(IMAGER_WRAPPER)
	@$(INSTALL) -m 0755 $(IMAGER_WRAPPER) $(DESTDIR)$(usrdir)/lib/eucalyptus
	@$(INSTALL) -m 0755 $(IMAGER) $(DESTDIR)$(usrdir)/lib/eucalyptus
deploy:

uninstall:
	@$(RM) -f $(usrdir)/lib/eucalyptus/$(IMAGER_WRAPPER)
	@$(RM) -f $(usrdir)/lib/eucalyptus/$(IMAGER)

