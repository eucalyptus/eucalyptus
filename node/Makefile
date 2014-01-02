# Makefile for Eucalyptus Node Controller
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

SUBDIRS=generated .
WSDLPATH=../wsdl/
CCWSDLNAME=eucalyptus_cc.wsdl
CCWSDL=$(WSDLPATH)/$(CCWSDLNAME)
NCWSDLNAME=eucalyptus_nc.wsdl
NCWSDL=$(WSDLPATH)/$(NCWSDLNAME)
SCWSDLNAME=eucalyptus_sc.wsdl
SCWSDL=$(WSDLPATH)/$(SCWSDLNAME)
SERVICE_SO=libEucalyptusNC.so
SERVICE_SO_FAKE=libEucalyptusNCFake.so
SERVICE_NAME=EucalyptusNC
CLIENT=NCclient
GFLAGS=$(patsubst -Werror,,$(CFLAGS))
GPPFLAGS=$(patsubst -Werror,,$(CPPFLAGS))
#WSDL2C=$(AXIS2C_HOME)/bin/tools/wsdl2c/WSDL2C.sh
NC_LIBS	= $(LIBS) $(LDFLAGS) -lvirt -lcurl -lssl -lcrypto -lrampart `xslt-config --libs`
AXIOM_LIBS = -lrampart -laxis2_http_sender -laxis2_http_receiver -laxis2_http_common -laxis2_engine -laxis2_axiom -laxutil -lneethi
OPENSSL_LIBS = -lssl -lcrypto
NC_HANDLERS=handlers_xen.o handlers_kvm.o handlers_default.o xml.o hooks.o
STORAGE_OBJS=../storage/backing.o ../storage/diskutil.o ../storage/blobstore.o ../storage/objectstorage.o ../storage/vbr.o ../storage/iscsi.o ../storage/ebs_utils.o ../storage/sc-client-marshal-adb.o ../storage/storage-controller.o

BUILD_ID=-DEUCA_COMPILE_TIMESTAMP=\""[built `date --rfc-3339='sec'`]"\"
#EFENCE=-lefence

all: generated/stubs test_hooks
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) buildall) || exit $$? ; done

build: all

buildall: server client clientlib test_misc test_nc test_hooks test_xml test_xml2

generated/stubs: $(NCWSDL) $(SCWSDL) 
	@echo Generating server stubs
	rm -rf generated
	mkdir -p generated
	sh $(WSDL2C) -uri $(NCWSDL) -sd -ss -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Adding marshalling calls to server stubs
#	../tools/add_marshalling.pl generated/axis2_skel_*.c generated/axis2_svc_skel_*.c
	../tools/add_marshalling.pl generated/axis2_skel_*.c
	@echo Generating SCclient stubs
	sh $(WSDL2C) -uri $(SCWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Generating client stubs
	sh $(WSDL2C) -uri $(NCWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Compiling the stubs
	patch --directory=generated -p1 <axis2_svc_generated.patch
	@cp ../tools/Makefile.gen generated/Makefile
	$(MAKE) -C generated CFLAGS="$(GPPFLAGS) $(GFLAGS) -Wno-unused-variable"
	cd ..
	@echo "this is a cute little makefile marker" >generated/stubs
	@echo Stubs generated.

.c.o:
	$(CC) -c $(BUILD_ID) $(CPPFLAGS) $(CFLAGS) `xslt-config --cflags` $(INCLUDES) $<

server: $(SCLIBS) $(SERVICE_SO)

../storage/backing.o: ../storage/backing.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/data.o
	make -C ../storage

../storage/diskutil.o: ../storage/diskutil.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/data.o
	make -C ../storage

../storage/blobstore.o: ../storage/blobstore.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/data.o
	make -C ../storage

../storage/objectstorage.o: ../storage/objectstorage.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/data.o
	make -C ../storage

../storage/iscsi.o: ../storage/iscsi.c
	make -C ../storage

../storage/http.o: ../storage/http.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/data.o
	make -C ../storage

../storage/vbr.o: ../storage/vbr.c ../util/data.o
	make -C ../storage

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/euca_string.o: ../util/euca_string.c ../util/euca_string.h ../util/eucalyptus.h
	make -C ../util

../util/euca_file.o: ../util/euca_file.c ../util/euca_file.h ../util/eucalyptus.h
	make -C ../util

../util/log.o: ../util/log.c ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../util/euca_auth.o: ../util/euca_auth.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../util/sensor.o: ../util/sensor.c
	make -C ../util

../storage/ebs_utils.o: ../storage/ebs_utils.c
	make -C ../storage

../storage/sc-client-marshal-adb.o: ../storage/sc-client-marshal-adb.c
	make -C ../storage

../storage/storage-controller.o: ../storage/storage-controller.c ../storage/sc-client-marshal-adb.o
	make -C ../storage

$(SERVICE_SO): generated/stubs $(STORAGE_OBJS) ../net/vnetwork.o ../util/log.o ../util/misc.o ../util/euca_file.o ../util/euca_file.o ../util/sensor.o ../util/hash.o ../util/data.o server-marshal.o handlers.o $(NC_HANDLERS) ../util/eucalyptus.h ../util/euca_auth.o ../storage/http.o
	$(CC) -shared generated/*.o server-marshal.o handlers.o $(NC_HANDLERS) $(STORAGE_OBJS) ../net/vnetwork.o ../util/*.o ../storage/http.o ../storage/storage-windows.o $(SCLIBS) $(NC_LIBS) $(EFENCE) -o $(SERVICE_SO)

clientlib: generated/stubs ../util/data.o $(SCLIBS) client-marshal-adb.o

client: $(CLIENT) $(CLIENT)_local

fake: $(CLIENT)_fake

$(CLIENT): generated/stubs $(CLIENT).c $(STORAGE_OBJS) ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../util/data.o client-marshal-adb.o client-marshal-local.o $(CLIENT).c ../storage/vbr.o
	$(CC) -o $(CLIENT) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(CLIENT).c client-marshal-adb.c generated/adb_*.o generated/axis2_stub_*.o ../util/*.o ../storage/diskutil.o ../net/vnetwork.o ../storage/http.o -lcurl ../storage/storage-windows.o -lm $(AXIOM_LIBS) $(OPENSSL_LIBS) $(NC_LIBS)

$(CLIENT)_fake: generated/stubs $(STORAGE_OBJS) ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../util/data.o client-marshal-adb.o client-marshal-local.o client-marshal-fake.o $(CLIENT).c ../storage/vbr.o

$(CLIENT)_local: generated/stubs $(STORAGE_OBJS) ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../util/data.o client-marshal-adb.o client-marshal-local.o handlers.o $(NC_HANDLERS) $(CLIENT).c ../util/euca_auth.o ../storage/vbr.o
	$(CC) -o $(CLIENT)_local -DNO_COMP $(INCLUDES) $(CPPFLAGS) $(CFLAGS) -shared client-marshal-local.o ../util/*.o $(STORAGE_OBJS) ../net/vnetwork.o handlers.o $(NC_HANDLERS) $(CLIENT).c $(NC_LIBS) ../storage/http.o ../storage/storage-windows.o $(SCLIBS)

test_misc: test.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../util/data.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test_misc test.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../storage/diskutil.o ../util/data.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_nc: test_nc.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../storage/diskutil.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test_nc -lvirt test_nc.c -lvirt ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_hooks: hooks.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../storage/diskutil.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test_hooks -D__STANDALONE hooks.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../util/sensor.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_xml: xml.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o ../util/data.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(NC_LIBS) `xslt-config --cflags` -o test_xml -D__STANDALONE xml.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o ../util/data.o

test_xml2: xml.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o ../util/data.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(NC_LIBS) `xslt-config --cflags` -o test_xml2 -D__STANDALONE2 xml.c ../util/log.o ../util/misc.o ../util/euca_string.o ../util/euca_file.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o ../util/data.o

libvirt_tortura: libvirt_tortura.c
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -o libvirt_tortura libvirt_tortura.c -lvirt

deploy:
	$(INSTALL) -d $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
	$(INSTALL) $(SERVICE_SO) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/

# no ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_SERVICES)/$(SERVICE_NAME)/$(NCWSDLNAME) > $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_noWS.xml
# ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_SERVICES)/$(SERVICE_NAME)/$(NCWSDLNAME) | ../tools/add_service_policy.pl ../tools/service-policy-template.xml | sed "s:CLIENT-CERT:cluster-cert.pem:g" | sed "s:CLIENT-KEY:cluster-pk.pem:g" | sed "s:SERVER-CERT:node-cert.pem:g" | sed "s:SERVER-KEY:node-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_WS.xml
	cat ../tools/client-policy-template.xml | sed "s:EUCALYPTUS_HOME:$(EUCALYPTUS):g" | sed "s:AXIS2C_HOME:$(AXIS2C_HOME):g"  | sed "s:CLIENT-CERT:cluster-cert.pem:g" | sed "s:CLIENT-KEY:cluster-pk.pem:g" | sed "s:SERVER-CERT:node-cert.pem:g" | sed "s:SERVER-KEY:node-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > nc-client-policy.xml
#	cp nc-client-policy.xml $(EUCALYPTUS)/var/lib/eucalyptus/keys/
##	cp ../tools/*.pem $(EUCALYPTUS)/var/lib/eucalyptus/keys/
#	chmod -R 0600 $(EUCALYPTUS)/var/lib/eucalyptus/keys/*
# ws
	$(INSTALL) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_WS.xml $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services.xml
# no ws
#	cp $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_noWS.xml $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services.xml

indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

test: test_nc test_misc test_hooks test_xml test_xml2
	./test_xml ../tools/libvirt.xsl

clean:
	rm -rf $(SERVICE_SO) *.o $(CLIENT) $(CLIENT)_local *~* *#* test_nc test_misc test_xml test_xml2

distclean:
	rm -rf generated $(SERVICE_SO) *.o $(CLIENT) $(CLIENT)_local nc-client-policy.xml test test_nc test_hooks *~* *#*

install: deploy
	$(INSTALL) nc-client-policy.xml $(DESTDIR)$(vardir)/lib/eucalyptus/keys/
	$(INSTALL) -m 0755 test_nc $(DESTDIR)$(sbindir)/euca_test_nc

uninstall:
	$(RM) -f $(DESTDIR)$(vardir)/lib/eucalyptus/keys/nc-client-policy.xml
	$(RM) -f $(DESTDIR)$(sbindir)/euca_test_nc
	$(RM) -rf $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)
