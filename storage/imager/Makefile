# Copyright 2014 Eucalyptus Systems, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

#
# Makefile for the imager
#

include ../../Makedefs

IMAGER=euca-imager
VMDK_WRAPPER=euca-vmdk-wrapper
VMDK=euca-vmdk
VMDK_TEST=test-vmdk-shim
#EFENCE=-lefence

STORAGE_LIBS = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread -lrt

LOCAL_IMAGER_OBJS = cmd_bundle.o cmd_convert.o cmd_upload.o cmd_prepare.o cmd_extract.o cmd_fsck.o cache.o img.o diskfile.o vmdk_shim.o
EXTRN_IMAGER_OBJS = $(TOP)/util/euca_auth.o $(TOP)/util/hash.o $(TOP)/util/log.o $(TOP)/util/misc.o $(TOP)/util/euca_string.o $(TOP)/util/euca_file.o $(TOP)/util/ipc.o $(TOP)/storage/objectstorage.o $(TOP)/storage/map.o $(TOP)/storage/http.o $(TOP)/storage/diskutil.o $(TOP)/storage/vbr_no_ebs.o $(TOP)/storage/blobstore.o
IMAGER_OBJS = $(LOCAL_IMAGER_OBJS) $(EXTRN_IMAGER_OBJS)
EXTRN_VMDK_OBJS = $(TOP)/storage/diskutil.o $(TOP)/util/ipc.o $(TOP)/util/log.o $(TOP)/util/misc.o $(TOP)/util/euca_string.o $(TOP)/util/euca_file.o 
EXTRN_SHIM_OBJS = $(TOP)/storage/http.o $(TOP)/util/euca_auth.o $(EXTRN_VMDK_OBJS)

# full list of all external .o files
EXTRN_OBJS = $(EXTRN_IMAGER_OBJS)

# will be 32 or 64
ARCH := $(shell getconf LONG_BIT)

# the flags replicate output of `pkg-config --cflags --libs vix-disklib`, which does not work if VDDK is installed in /opt/packages/vddk
VDDK_FLAGS = -I$(VDDK_HOME)/lib/vmware-vix-disklib/include -L$(VDDK_HOME)/lib/vmware-vix-disklib/lib$(ARCH)
VDDK_LIBS = -lpthread -lvixDiskLib -lvixMntapi -UDYNAMIC_LOADING

ifeq "$(VDDK_HOME)" ""
all: $(IMAGER)
	@echo built euca-imager without VDDK support
else
all: $(IMAGER) $(VMDK)
	@echo built euca-imager with VDDK support
endif

build: all

buildall: build

$(IMAGER): Makefile imager.c imager.h cmd.h $(IMAGER_OBJS)
	@echo compiling and linking $@
	@$(CC) -o $@ $(CPPFLAGS) $(CFLAGS) $(INCLUDES) imager.c $(IMAGER_OBJS) $(STORAGE_LIBS) $(SC_CLIENT_OBJS) $(SC_CLIENT_LIBS) $(EFENCE)

$(VMDK): Makefile vmdk_shim.c vmdk.h vmdk.o $(EXTRN_VMDK_OBJS) $(VMDK_TEST)
	@echo compiling and linking $@
	@$(CC) -o $@ $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(VDDK_FLAGS) -DVMDK_CALLEE vmdk_shim.c vmdk.o $(EXTRN_VMDK_OBJS) $(VDDK_LIBS)

vmdk_shim.o: vmdk_shim.c vmdk.h
	@echo compiling $@
	@$(CC) -o $@ $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -DVMDK_CALLER vmdk_shim.c -c

$(VMDK_TEST): vmdk_shim.c vmdk.h vmdk_shim.o $(EXTRN_SHIM_OBJS)
	@echo compiling and linking $@
	@$(CC) -o $@ $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -DVMDK_CALLER -D_UNIT_TEST vmdk_shim.c $(EXTRN_SHIM_OBJS) $(STORAGE_LIBS)

.c.o:
	@echo compiling $@
	@$(CC) -o $@ -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $<

$(TOP)/storage/vbr_no_ebs.o: $(TOP)/storage/vbr.c
	@echo compiling $@
	@$(CC) -o $@ -c -D_NO_EBS $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $<

indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

clean:
	rm -rf *~ *.o $(IMAGER) $(VMDK) $(VMDK_TEST)

distclean:

ifeq "$(VDDK_HOME)" ""
install: install-imager
	@echo installed euca-imager without VDDK support
else
install: install-imager install-vmdk
	@echo installed euca-imager with VDDK support
endif

install-imager: $(IMAGER)
	mkdir -p $(DESTDIR)$(libexecdir)/eucalyptus/
	$(INSTALL) -m 0755 $(IMAGER) $(DESTDIR)$(libexecdir)/eucalyptus/

install-vmdk: install-imager $(VMDK_WRAPPER) $(VMDK)
	$(INSTALL) -m 0755 $(VMDK_WRAPPER) $(DESTDIR)$(libexecdir)/eucalyptus/
	$(INSTALL) -m 0755 $(VMDK) $(DESTDIR)$(libexecdir)/eucalyptus/

deploy:

uninstall:
	$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/$(VMDK_WRAPPER)
	$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/$(IMAGER)
	$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/$(VMDK)

