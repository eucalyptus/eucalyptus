#
# Makefile for eucalyptus/storage
#

include ../Makedefs

STORAGE_LIBS = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread
TESTS=test_vbr test_blobstore
#EFENCE=-lefence

all: vbr.o backing.o storage-windows.o walrus.o diskutil.o map.o Wclient euca-blobs $(TESTS)
#all: storage-windows.o walrus.o test_blobstore

build: all

euca-blobs: Makefile blobstore.c blobstore.h map.o
	$(CC) $(CFLAGS) $(INCLUDES) -D_EUCA_BLOBS blobstore.c -o euca-blobs map.o diskutil.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o $(STORAGE_LIBS) $(EFENCE)

Wclient: Makefile Wclient.c ../util/euca_auth.o ../util/misc.o walrus.o http.o
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient ../util/euca_auth.o ../util/misc.o ../util/ipc.o diskutil.o walrus.o http.o $(STORAGE_LIBS)

walrus.o: walrus.c walrus.h
	$(CC) $(CFLAGS) $(INCLUDES) -c walrus.c 

test: test.c blobstore.o storage-windows.o ../util/misc.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o
	$(CC) $(CFLAGS) $(INCLUDES) test.c blobstore.o storage-windows.o ../util/misc.o diskutil.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o $(STORAGE_LIBS) -o test

test_blobstore: blobstore.o blobstore.c blobstore.h map.o diskutil.o ../util/misc.o
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST blobstore.c -o test_blobstore map.o diskutil.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o $(STORAGE_LIBS) $(EFENCE)

test_vbr: iscsi.o vbr.o blobstore.o diskutil.o walrus.o http.o ../util/misc.o ../util/data.h
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST vbr.c -o test_vbr blobstore.o diskutil.o walrus.o iscsi.o http.o ../util/hash.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o $(STORAGE_LIBS) $(EFENCE)

test_url: http.c
	$(CC) -D_UNIT_TEST -o test_url http.c

.c.o:
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

../util/ipc.o: ../util/ipc.c ../util/ipc.h ../util/eucalyptus.h
	make -C ../util

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/hash.o: ../util/hash.c ../util/hash.h ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/euca_auth.o: ../util/euca_auth.c ../util/euca_auth.h
	make -C ../util

clean:
	rm -rf *~ *.o Wclient euca-blobs $(TESTS)

distclean:

install:

deploy:

uninstall:

