# Makefile for eucalyptus/storage
#
# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

include ../Makedefs

SUBDIRS=generated . imager
WSDLPATH=../wsdl
SCWSDLNAME=eucalyptus_sc.wsdl
SCWSDL=$(WSDLPATH)/$(SCWSDLNAME)
GFLAGS=$(patsubst -Werror,,$(CFLAGS))
GPPFLAGS=$(patsubst -Werror,,$(CPPFLAGS))
SCCLIENT=SCclient
WSSECLIBS=../util/euca_axis.o ../util/euca_auth.o
SC_LIBS = ${LIBS} ${LDFLAGS} -lcurl -lssl -lcrypto -lrampart
STORAGE_CONTROLLER_OBJS = generated/*.o sc-client-marshal-adb.o iscsi.o ../util/config.o ../util/data.o ../util/fault.o ../util/wc.o ../util/utf8.o diskutil.o ../util/log.o ../util/misc.o ../util/ipc.o ../util/euca_string.o ../util/euca_file.o
EUCA_BLOBS_OBJS =                                     diskutil.o map.o                ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/ipc.o ../util/euca_auth.o
OSGCLIENT_OBJS    =                     objectstorage.o http.o diskutil.o map.o                ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/ipc.o ../util/euca_auth.o
TEST_BLOB_OBJS  =                                     diskutil.o map.o                ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/ipc.o ../util/euca_auth.o
TEST_VBR_OBJS   = iscsi.o blobstore.o objectstorage.o http.o diskutil.o       ../util/hash.o ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/ipc.o ../util/euca_auth.o ebs_utils.o storage-controller.o
TEST_DISKUTIL_OBJS  =                                            map.o                ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/ipc.o

STORAGE_LIBS    = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread
TESTS           = test_vbr test_blobstore test_ebs test_diskutil
#EFENCE          = -lefence

#all: vbr.o backing.o storage-windows.o objectstorage.o diskutil.o map.o Wclient euca-blobs generated/stubs $(SCCLIENT) $(TESTS)
all: generated/stubs
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) buildall) || exit $$? ; done

generated/stubs: $(SCWSDL)
	rm -rf generated
	mkdir -p generated
	@echo Generating SCclient stubs
	sh $(WSDL2C) -uri $(SCWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@cp ../tools/Makefile.gen generated/Makefile
	$(MAKE) -C generated CFLAGS="$(GPFLAGS) $(GFLAGS) -Wno-unused-variable"
	@echo "this is a cute little makefile marker" >generated/stubs
	@echo Stubs generated.

.c.o:
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $<

build: all

buildall: generated/stubs ebs_utils.o storage-controller.o vbr.o vbr_no_ebs.o backing.o storage-windows.o objectstorage.o diskutil.o map.o OSGclient euca-blobs $(SCCLIENT) $(TESTS)

client: $(SCCLIENT) OSGclient

$(SCCLIENT): $(SCCLIENT).c generated/stubs $(WSSECLIBS) storage-controller.o $(STORAGE_CONTROLLER_OBJS)
	$(CC) -o $(SCCLIENT) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(SCCLIENT).c -DMODE=1 storage-controller.o $(STORAGE_CONTROLLER_OBJS) $(WSSECLIBS) $(SC_LIBS)

euca-blobs: Makefile blobstore.c blobstore.h $(EUCA_BLOBS_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -D_EUCA_BLOBS blobstore.c -o euca-blobs $(EUCA_BLOBS_OBJS) $(STORAGE_LIBS) $(EFENCE)

OSGclient: Makefile OSGclient.c $(OSGCLIENT_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) OSGclient.c -o OSGclient $(OSGCLIENT_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_blobstore: blobstore.o $(TEST_BLOB_OBJS)
	$(CC) -rdynamic $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -D_UNIT_TEST blobstore.c -o test_blobstore $(TEST_BLOB_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_vbr: vbr.o $(TEST_VBR_OBJS) generated/stubs $(STORAGE_CONTROLLER_OBJS) ../util/fault.o
	$(CC) -rdynamic $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -D_UNIT_TEST vbr.c -o test_vbr $(TEST_VBR_OBJS) $(STORAGE_LIBS) $(EFENCE) $(SC_LIBS) ../util/euca_axis.o sc-client-marshal-adb.o ../util/fault.o generated/*.o ../util/utf8.o ../util/wc.o

test_url: http.c
	$(CC) -D_UNIT_TEST -o test_url http.c

test_ebs: ebs_utils.c $(STORAGE_CONTROLLER_OBJS) storage-controller.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -D_UNIT_TEST -o test_ebs ebs_utils.c storage-controller.o $(STORAGE_CONTROLLER_OBJS) $(WSSECLIBS) $(SC_LIBS)

test_diskutil: diskutil.c $(TEST_DISKUTIL_OBJS) Makefile
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -g -D_UNIT_TEST diskutil.c -o test_diskutil $(TEST_DISKUTIL_OBJS) -lpthread

vbr_no_ebs.o: vbr.c vbr.h
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -D_NO_EBS -o vbr_no_ebs.o $<

%.o: %.c %.h
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $<	

../util/ipc.o: ../util/ipc.c ../util/ipc.h ../util/eucalyptus.h
	make -C ../util

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/euca_string.o: ../util/euca_string.c ../util/euca_string.h ../util/eucalyptus.h
	make -C ../util

../util/euca_file.o: ../util/euca_file.c ../util/euca_file.h ../util/eucalyptus.h
	make -C ../util

../util/hash.o: ../util/hash.c ../util/hash.h ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/euca_auth.o: ../util/euca_auth.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../util/euca_axis.o:
	make -C ../util

indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

clean:
	@make -C imager clean
	@rm -rf *~ *.o OSGclient euca-blobs $(SCCLIENT) $(TESTS)

distclean:
	@make -C imager distclean
	@rm -rf generated sc-client-policy.xml

install:
	@echo installing euca-imager
	@make -C imager install
	@echo installing sc-client-policy.xml
	@$(INSTALL) sc-client-policy.xml $(DESTDIR)$(vardir)/lib/eucalyptus/keys/

deploy:
	@make -C imager deploy
	cat ../tools/client-policy-template.xml | sed "s:EUCALYPTUS_HOME:$(EUCALYPTUS):g" | sed "s:AXIS2C_HOME:$(AXIS2C_HOME):g" | sed "s:CLIENT-CERT:node-cert.pem:g" | sed "s:SERVER-CERT:cloud-cert.pem:g" | sed "s:SERVER-KEY:node-pk.pem:g" | sed "s:CLIENT-KEY:node-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > sc-client-policy.xml

uninstall:
	@make -C imager uninstall
	@$(RM) -f $(DESTDIR)$(vardir)/lib/eucalyptus/keys/sc-client-policy.xml