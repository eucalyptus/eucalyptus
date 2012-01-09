# Makefile for eucalyptus/storage
#
#

include ../Makedefs

STORAGE_LIBS = $(LDFLAGS) -lcurl -lssl -lcrypto -lpthread -pthread
IMAGER_LIBS = ../util/euca_auth.o ../util/misc.o ../util/ipc.o cache.o walrus.o cmd_bundle.o cmd_convert.o cmd_upload.o cmd_prepare.o cmd_fsck.o map.o vmdk_shim.o http.o img.o diskfile.o diskutil.o vbr.o blobstore.o iscsi.o

# will be 32 or 64
ARCH := $(shell getconf LONG_BIT)

# the flags replicate output of `pkg-config --cflags --libs vix-disklib`, which does not work if VDDK is installed in /opt/packages/vddk
VDDK_FLAGS = -I$(VDDK_HOME)/lib/vmware-vix-disklib/include  -L$(VDDK_HOME)/lib/vmware-vix-disklib/lib$(ARCH) -lvixDiskLib -lvixMntapi -UDYNAMIC_LOADING

TESTS=test_vbr test_blobstore test_vmdk_shim
IMAGER=euca_imager
VMDK_WRAPPER=euca_vmdk
VMDK=_euca_vmdk

#EFENCE=-lefence

all: vbr.o backing.o storage-windows.o walrus.o diskutil.o Wclient $(IMAGER) $(VMDK) euca-blobs $(TESTS)
#all: storage-windows.o walrus.o test_blobstore

build: all

$(IMAGER): Makefile imager.c imager.h cmd.h $(IMAGER_LIBS)
	$(CC) $(CFLAGS) $(INCLUDES) imager.c -o $(IMAGER) $(IMAGER_LIBS) $(STORAGE_LIBS) -lrt $(EFENCE)

$(VMDK): vmdk_shim.c vmdk.h vmdk.o diskutil.o http.o ../util/ipc.o ../util/misc.o ../util/euca_auth.o
	$(CC) $(CFLAGS) $(INCLUDES) -DVMDK_CALLEE vmdk_shim.c -o $(VMDK) $(VDDK_FLAGS) vmdk.o diskutil.o http.o ../util/ipc.o ../util/misc.o ../util/euca_auth.o -lcurl -lssl -lcrypto

vmdk_shim.o: vmdk_shim.c vmdk.h _euca_vmdk
	$(CC) $(CFLAGS) $(INCLUDES) -DVMDK_CALLER vmdk_shim.c -c

test_vmdk_shim: vmdk_shim.c vmdk.h vmdk_shim.o
	$(CC) $(CFLAGS) $(INCLUDES) -DVMDK_CALLER -D_UNIT_TEST vmdk_shim.c -o test_vmdk_shim ../util/misc.o diskutil.o ../util/euca_auth.o ../util/ipc.o -lcrypto -lrt

euca-blobs: Makefile blobstore.c blobstore.h
	$(CC) $(CFLAGS) $(INCLUDES) -D_EUCA_BLOBS blobstore.c -o euca-blobs map.o diskutil.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o $(STORAGE_LIBS) $(EFENCE)

Wclient: Makefile Wclient.c ../util/euca_auth.o ../util/misc.o walrus.o http.o
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient ../util/euca_auth.o ../util/misc.o ../util/ipc.o diskutil.o walrus.o http.o $(STORAGE_LIBS)

walrus.o: walrus.c walrus.h
	$(CC) $(CFLAGS) $(INCLUDES) -c walrus.c 

test: test.c blobstore.o storage-windows.o ../util/misc.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o
	$(CC) $(CFLAGS) $(INCLUDES) test.c blobstore.o storage-windows.o ../util/misc.o diskutil.o ../util/data.o ../util/ipc.o ../util/euca_auth.o walrus.o $(STORAGE_LIBS) -o test

test_blobstore: blobstore.o blobstore.c blobstore.h map.o diskutil.o ../util/misc.o
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST blobstore.c -o test_blobstore map.o diskutil.o ../util/misc.o ../util/ipc.o $(STORAGE_LIBS) $(EFENCE) ../util/euca_auth.o

test_vbr: iscsi.o vbr.o blobstore.o diskutil.o walrus.o http.o ../util/misc.o ../util/data.h
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST vbr.c -o test_vbr blobstore.o diskutil.o walrus.o iscsi.o http.o ../util/misc.o ../util/ipc.o $(STORAGE_LIBS) $(EFENCE) ../util/euca_auth.o

test_url: http.c
	$(CC) -D_UNIT_TEST -o test_url http.c

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
	rm -rf *~ *.o Wclient euca-blobs $(IMAGER) $(VMDK) $(TESTS)

distclean:

install: $(VMDK_WRAPPER) $(IMAGER) $(VMDK)
	@$(INSTALL) -m 0755 $(VMDK_WRAPPER) $(DESTDIR)$(usrdir)/lib/eucalyptus
	@$(INSTALL) -m 0755 $(IMAGER) $(DESTDIR)$(usrdir)/lib/eucalyptus
	@$(INSTALL) -m 0755 $(VMDK) $(DESTDIR)$(usrdir)/lib/eucalyptus
deploy:

uninstall:
	@$(RM) -f $(usrdir)/lib/eucalyptus/$(VMDK_WRAPPER)
	@$(RM) -f $(usrdir)/lib/eucalyptus/$(IMAGER)
	@$(RM) -f $(usrdir)/lib/eucalyptus/$(VMDK)

