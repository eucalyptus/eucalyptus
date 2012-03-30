#
# Makefile for vmware-broker/imager
#

include ../../Makedefs

IMAGER=euca_imager
VMDK_WRAPPER=euca_vmdk
VMDK=_euca_vmdk
TESTS=test_vmdk_shim
#EFENCE=-lefence

STORAGE_LIBS = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread -lrt

LOCAL_IMAGER_OBJS = cmd_bundle.o cmd_convert.o cmd_upload.o cmd_prepare.o cmd_fsck.o cache.o img.o diskfile.o vmdk_shim.o
EXTRN_IMAGER_OBJS = $(TOP)/util/euca_auth.o $(TOP)/util/hash.o $(TOP)/util/misc.o $(TOP)/util/ipc.o $(TOP)/storage/walrus.o $(TOP)/storage/map.o $(TOP)/storage/http.o $(TOP)/storage/diskutil.o $(TOP)/storage/vbr.o $(TOP)/storage/blobstore.o $(TOP)/storage/iscsi.o
IMAGER_OBJS = $(LOCAL_IMAGER_OBJS) $(EXTRN_IMAGER_OBJS)
EXTRN_VMDK_OBJS = $(TOP)/storage/diskutil.o $(TOP)/util/ipc.o $(TOP)/util/misc.o
EXTRN_SHIM_OBJS = $(TOP)/storage/http.o $(TOP)/util/euca_auth.o $(EXTRN_VMDK_OBJS)

# full list of all external .o files
EXTRN_OBJS = $(EXTRN_IMAGER_OBJS)

# will be 32 or 64
ARCH := $(shell getconf LONG_BIT)

# the flags replicate output of `pkg-config --cflags --libs vix-disklib`, which does not work if VDDK is installed in /opt/packages/vddk
VDDK_FLAGS = -I$(VDDK_HOME)/lib/vmware-vix-disklib/include -L$(VDDK_HOME)/lib/vmware-vix-disklib/lib$(ARCH)
VDDK_LIBS = -lpthread -lvixDiskLib -lvixMntapi -UDYNAMIC_LOADING

all: $(IMAGER) $(VMDK) $(TESTS)

build: all

$(IMAGER): Makefile imager.c imager.h cmd.h $(IMAGER_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) imager.c -o $(IMAGER) $(IMAGER_OBJS) $(STORAGE_LIBS) $(EFENCE)

$(VMDK): Makefile vmdk_shim.c vmdk.h vmdk.o $(EXTRN_VMDK_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) $(VDDK_FLAGS) -DVMDK_CALLEE vmdk_shim.c -o $(VMDK) vmdk.o $(EXTRN_VMDK_OBJS) $(VDDK_LIBS)

vmdk_shim.o: vmdk_shim.c vmdk.h _euca_vmdk
	$(CC) $(CFLAGS) $(INCLUDES) -DVMDK_CALLER vmdk_shim.c -c

test_vmdk_shim: vmdk_shim.c vmdk.h vmdk_shim.o $(EXTRN_SHIM_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) -DVMDK_CALLER -D_UNIT_TEST vmdk_shim.c -o test_vmdk_shim $(EXTRN_SHIM_OBJS) $(STORAGE_LIBS)

.c.o:
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

$(filter %.o,$(EXTRN_OBJS)): %.o:
	$(MAKE) -C $(TOP)/util build
	$(MAKE) -C $(TOP)/storage build

clean:
	rm -rf *~ *.o $(IMAGER) $(VMDK) $(TESTS)

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

