#
# Makefile for eucalyptus/storage
#

include ../Makedefs

EUCA_BLOBS_OBJS =                                     diskutil.o map.o                ../util/misc.o ../util/ipc.o ../util/euca_auth.o
WCLIENT_OBJS    =                     walrus.o http.o diskutil.o map.o                ../util/misc.o ../util/ipc.o ../util/euca_auth.o
TEST_BLOB_OBJS  =                                     diskutil.o map.o                ../util/misc.o ../util/ipc.o ../util/euca_auth.o
TEST_VBR_OBJS   = iscsi.o blobstore.o walrus.o http.o diskutil.o       ../util/hash.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o

STORAGE_LIBS    = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread
TESTS           = test_vbr test_blobstore
#EFENCE          = -lefence

all: vbr.o backing.o storage-windows.o walrus.o diskutil.o map.o Wclient euca-blobs $(TESTS)

build: all

euca-blobs: Makefile blobstore.c blobstore.h $(EUCA_BLOBS_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) -D_EUCA_BLOBS blobstore.c -o euca-blobs $(EUCA_BLOBS_OBJS) $(STORAGE_LIBS) $(EFENCE)

Wclient: Makefile Wclient.c $(WCLIENT_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient $(WCLIENT_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_blobstore: blobstore.o $(TEST_BLOB_OBJS)
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST blobstore.c -o test_blobstore $(TEST_BLOB_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_vbr: vbr.o $(TEST_VBR_OBJS)
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST vbr.c -o test_vbr $(TEST_VBR_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_url: http.c
	$(CC) -D_UNIT_TEST -o test_url http.c

%.o: %.c %.h
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

