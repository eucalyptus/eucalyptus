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

STORAGE_LIBS = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread -lrt

LOCAL_IMAGER_OBJS = cmd_bundle.o cmd_prepare.o cmd_extract.o cmd_fsck.o cache.o img.o diskfile.o
EXTRN_IMAGER_OBJS = $(TOP)/util/euca_auth.o $(TOP)/util/hash.o $(TOP)/util/log.o $(TOP)/util/misc.o $(TOP)/util/euca_string.o $(TOP)/util/euca_file.o $(TOP)/util/ipc.o $(TOP)/storage/objectstorage.o $(TOP)/storage/map.o $(TOP)/storage/http.o $(TOP)/storage/diskutil.o $(TOP)/storage/vbr_no_ebs.o $(TOP)/storage/blobstore.o
IMAGER_OBJS = $(LOCAL_IMAGER_OBJS) $(EXTRN_IMAGER_OBJS)

# full list of all external .o files
EXTRN_OBJS = $(EXTRN_IMAGER_OBJS)

# will be 32 or 64
ARCH := $(shell getconf LONG_BIT)

all: $(IMAGER)
	@echo built euca-imager

build: all

buildall: build

$(IMAGER): Makefile imager.c imager.h cmd.h $(IMAGER_OBJS)
	@echo compiling and linking $@
	@$(CC) -o $@ $(CPPFLAGS) $(CFLAGS) $(INCLUDES) imager.c $(IMAGER_OBJS) $(STORAGE_LIBS) $(SC_CLIENT_OBJS) $(SC_CLIENT_LIBS) $(EFENCE)

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
	rm -rf *~ *.o $(IMAGER)

distclean:

install: install-imager
	@echo installed euca-imager

install-imager: $(IMAGER)
	mkdir -p $(DESTDIR)$(libexecdir)/eucalyptus/
	$(INSTALL) -m 0755 $(IMAGER) $(DESTDIR)$(libexecdir)/eucalyptus/

deploy:

uninstall:
	$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/$(IMAGER)
