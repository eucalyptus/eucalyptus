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
SERVICE_SO=libEucalyptusNC.so
SERVICE_NAME=EucalyptusNC
CLIENT=NCclient
#WSDL2C=$(AXIS2C_HOME)/bin/tools/wsdl2c/WSDL2C.sh
NC_LIBS	= $(LIBS) $(LDFLAGS) -lvirt -lcurl -lssl -lcrypto -lrampart `xslt-config --libs`
AXIOM_LIBS = -lrampart -laxis2_http_sender -laxis2_http_receiver -laxis2_http_common -laxis2_engine -laxis2_axiom -laxutil -lneethi
OPENSSL_LIBS = -lssl -lcrypto
NC_HANDLERS=handlers_xen.o handlers_kvm.o handlers_default.o xml.o hooks.o
STORAGE_OBJS=../storage/backing.o ../storage/diskutil.o ../storage/blobstore.o ../storage/walrus.o ../storage/vbr.o ../storage/iscsi.o
BUILD_ID=-DEUCA_COMPILE_TIMESTAMP=\""[built `date --rfc-3339='sec'`]"\"
#EFENCE=-lefence

all: generated/stubs test_hooks
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) buildall) || exit $$? ; done

build: all

buildall: server client clientlib test_nc test

generated/stubs: $(NCWSDL)
	@echo Generating server stubs
	rm -rf generated
	mkdir -p generated
	sh $(WSDL2C) -uri $(NCWSDL) -sd -ss -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Adding marshalling calls to server stubs
#	../tools/add_marshalling.pl generated/axis2_skel_*.c generated/axis2_svc_skel_*.c
	../tools/add_marshalling.pl generated/axis2_skel_*.c
	@echo Generating client stubs
	sh $(WSDL2C) -uri $(NCWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Compiling the stubs
	patch --directory=generated -p1 <axis2_svc_generated.patch
	@cp ../tools/Makefile.gen generated/Makefile
	$(MAKE) -C generated
	cd ..
	@echo "this is a cute little makefile marker" >generated/stubs
	@echo Stubs generated.

.c.o:
	$(CC) $(BUILD_ID) -c $(CFLAGS) `xslt-config --cflags` $(INCLUDES) $<

server: $(SERVICE_SO)

../storage/backing.o: ../storage/backing.c ../util/log.o ../util/misc.o ../util/data.o
	make -C ../storage

../storage/diskutil.o: ../storage/diskutil.c ../util/log.o ../util/misc.o ../util/data.o
	make -C ../storage

../storage/blobstore.o: ../storage/blobstore.c ../util/log.o ../util/misc.o ../util/data.o
	make -C ../storage

../storage/walrus.o: ../storage/walrus.c ../util/log.o ../util/misc.o ../util/data.o
	make -C ../storage

../storage/iscsi.o: ../storage/iscsi.c
	make -C ../storage

../storage/http.o: ../storage/http.c ../util/log.o ../util/misc.o ../util/data.o
	make -C ../storage

../storage/vbr.o: ../storage/vbr.c ../util/data.o
	make -C ../storage

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/log.o: ../util/log.c ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../util/euca_auth.o: ../util/euca_auth.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../util/sensor.o: ../util/sensor.c
	make -C ../util

../util/windows-bundle.o: ../util/windows-bundle.c ../util/windows-bundle.h
	make -C ../util

$(SERVICE_SO): generated/stubs $(STORAGE_OBJS) ../net/vnetwork.o ../util/log.o ../util/misc.o ../util/sensor.o ../util/hash.o ../util/data.o server-marshal.o handlers.o $(NC_HANDLERS) ../util/eucalyptus.h ../util/euca_auth.o ../storage/http.o ../util/windows-bundle.o
	$(CC) -o $(SERVICE_SO) -shared generated/adb_*.o generated/axis2*_skel_*.o server-marshal.o handlers.o $(NC_HANDLERS) $(STORAGE_OBJS) ../net/*.o ../util/*.o ../storage/http.o ../storage/storage-windows.o $(NC_LIBS) $(EFENCE)

clientlib: generated/stubs ../util/data.o client-marshal-adb.o

client: $(CLIENT) $(CLIENT)_local

fake: $(CLIENT)_fake

$(CLIENT): generated/stubs $(STORAGE_OBJS) ../util/log.o ../util/misc.o ../util/sensor.o ../util/data.o client-marshal-adb.o client-marshal-local.o $(CLIENT).c ../storage/vbr.o
	$(CC) -o $(CLIENT) $(CFLAGS) $(INCLUDES) generated/adb_*.o generated/axis2_stub_*.o $(AXIOM_LIBS) client-marshal-adb.o ../util/*.o $(AXIOM_LIBS) $(OPENSSL_LIBS) ../storage/diskutil.o ../net/*.o -lm $(CLIENT).c ../storage/http.o -lcurl ../storage/storage-windows.o $(NC_LIBS)

$(CLIENT)_fake: generated/stubs $(STORAGE_OBJS) ../util/log.o ../util/misc.o ../util/sensor.o ../util/data.o client-marshal-adb.o client-marshal-local.o client-marshal-fake.o $(CLIENT).c ../storage/vbr.o

$(CLIENT)_local: generated/stubs $(STORAGE_OBJS) ../util/log.o ../util/misc.o ../util/sensor.o ../util/data.o client-marshal-adb.o client-marshal-local.o handlers.o $(NC_HANDLERS) $(CLIENT).c ../util/euca_auth.o ../storage/vbr.o
	$(CC) -o $(CLIENT)_local -DNO_COMP $(INCLUDES) $(CFLAGS) client-marshal-local.o ../util/*.o $(STORAGE_OBJS) ../net/*.o handlers.o $(NC_HANDLERS) $(CLIENT).c $(NC_LIBS) ../storage/http.o ../storage/storage-windows.o

test: test.c ../util/log.o ../util/misc.o ../util/sensor.o ../util/data.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test test.c ../util/log.o ../util/misc.o ../util/sensor.o ../storage/diskutil.o ../util/data.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_nc: test_nc.c ../util/log.o ../util/misc.o ../util/sensor.o ../storage/diskutil.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test_nc -lvirt test_nc.c -lvirt ../util/log.o ../util/misc.o ../util/sensor.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_hooks: hooks.c ../util/log.o ../util/misc.o ../util/sensor.o ../storage/diskutil.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test_hooks -D__STANDALONE hooks.c ../util/log.o ../util/misc.o ../util/sensor.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_xml: xml.c ../util/log.o ../util/misc.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) `xslt-config --cflags` -o test_xml -D__STANDALONE xml.c ../util/log.o ../util/misc.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

test_xml2: xml.c ../util/log.o ../util/misc.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) `xslt-config --cflags` -o test_xml2 -D__STANDALONE2 xml.c ../util/log.o ../util/misc.o ../storage/diskutil.o ../util/euca_auth.o $(OPENSSL_LIBS) ../util/ipc.o

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

clean:
	rm -rf $(SERVICE_SO) *.o $(CLIENT) $(CLIENT)_local *~* *#* test_xml

distclean:
	rm -rf generated $(SERVICE_SO) *.o $(CLIENT) $(CLIENT)_local nc-client-policy.xml test test_nc test_hooks *~* *#*

install: deploy
	$(INSTALL) nc-client-policy.xml $(DESTDIR)$(vardir)/lib/eucalyptus/keys/
	$(INSTALL) -m 0755 test_nc $(DESTDIR)$(sbindir)/euca_test_nc

uninstall:
	$(RM) -f $(DESTDIR)$(vardir)/lib/eucalyptus/keys/nc-client-policy.xml
	$(RM) -f $(DESTDIR)$(sbindir)/euca_test_nc
	$(RM) -rf $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)
