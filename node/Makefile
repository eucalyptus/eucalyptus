#
# $Id: Makefile,v 1.6 2008-12-24 18:28:07 nurmi Exp $
#

include ../Makedefs

WSDL=../wsdl/eucalyptus_nc.wsdl
NCWSDLNAME=eucalyptus_nc.wsdl
SERVICE_SO=libEucalyptusNC.so
SERVICE_NAME=EucalyptusNC
CLIENT=NCclient
WSDL2C=$(AXIS2C_HOME)/bin/tools/wsdl2c/WSDL2C.sh
NC_LIBS 		= $(LIBS) -lvirt -lcurl
NC_HANDLERS=handlers_xen.o

all: server client clientlib test_nc test

build: all

generated/stubs: $(WSDL)
	@echo Generating server stubs
	rm -rf generated
	mkdir -p generated
	sh $(WSDL2C) -uri $(WSDL) -sd -ss -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Adding marshalling calls to server stubs
	../tools/add_marshalling.pl generated/axis2_skel_*.c
	@echo Generating client stubs
	sh $(WSDL2C) -uri $(WSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Compiling the stubs
	cd generated; patch -p1 < ../axis2_svc_generated.patch; $(CC) -c $(CFLAGS) $(INCLUDES) *.c
	@echo "this is a cute little makefile marker" >generated/stubs
	@echo Stubs generated.

.c.o:
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

server: $(SERVICE_SO)

../storage/storage.o: ../storage/storage.c ../util/misc.o ../util/data.o
	make -C ../storage

../storage/walrus.o: ../storage/walrus.c ../util/misc.o ../util/data.o
	make -C ../storage

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../util/euca_auth.o: ../util/euca_auth.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

$(SERVICE_SO): generated/stubs ../storage/storage.o ../net/vnetwork.o ../util/misc.o ../util/data.o server-marshal.o handlers.o $(NC_HANDLERS) $(AXIS2C_HOME)/lib/libaxutil.so ../util/eucalyptus.h ../util/euca_auth.o
	$(CC) -o $(SERVICE_SO) -shared $(NC_LIBS) generated/adb_*.o generated/axis2*_skel_*.o server-marshal.o handlers.o $(NC_HANDLERS) ../storage/*.o ../net/*.o ../util/*.o 

clientlib: generated/stubs ../util/data.o client-marshal-adb.o

client: $(CLIENT) $(CLIENT)_local

$(CLIENT): generated/stubs ../util/misc.o ../util/data.o client-marshal-adb.o client-marshal-local.o $(AXIS2C_HOME)/lib/libaxutil.so $(CLIENT).c
	$(CC) -o $(CLIENT) $(CFLAGS) $(INCLUDES) $(NC_LIBS) generated/adb_*.o generated/axis2_stub_*.o client-marshal-adb.o ../util/*.o ../storage/*.o ../net/*.o $(CLIENT).c 

$(CLIENT)_local: generated/stubs ../util/misc.o ../util/data.o client-marshal-adb.o client-marshal-local.o handlers.o $(NC_HANDLERS) $(CLIENT).c ../util/euca_auth.o
	$(CC) -o $(CLIENT)_local $(INCLUDES) $(CFLAGS) client-marshal-local.o ../util/*.o ../storage/*.o ../net/*.o handlers.o $(NC_HANDLERS) $(CLIENT).c $(NC_LIBS)

test: test.c ../util/misc.o ../util/data.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test test.c ../util/misc.o ../util/data.o

test_nc: test_nc.c ../util/misc.o
	$(CC) $(CFLAGS) $(INCLUDES) $(NC_LIBS) -o test_nc test_nc.c ../util/misc.o

deploy:
	$(INSTALL) -d $(AXIS2C_HOME)/services/$(SERVICE_NAME)/
	$(INSTALL) $(SERVICE_SO) $(AXIS2C_HOME)/services/$(SERVICE_NAME)/

# no ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_HOME)/services/$(SERVICE_NAME)/$(NCWSDLNAME) > $(AXIS2C_HOME)/services/$(SERVICE_NAME)/services_noWS.xml
# ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_HOME)/services/$(SERVICE_NAME)/$(NCWSDLNAME) | ../tools/add_service_policy.pl ../tools/service-policy-template.xml | sed "s:CLIENT-CERT:cluster-cert.pem:g" | sed "s:CLIENT-KEY:cluster-pk.pem:g" | sed "s:SERVER-CERT:node-cert.pem:g" | sed "s:SERVER-KEY:node-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > $(AXIS2C_HOME)/services/$(SERVICE_NAME)/services_WS.xml
	cat ../tools/client-policy-template.xml | sed "s:EUCALYPTUS_HOME:$(EUCALYPTUS):g" | sed "s:AXIS2C_HOME:$(AXIS2C_HOME):g"  | sed "s:CLIENT-CERT:cluster-cert.pem:g" | sed "s:CLIENT-KEY:cluster-pk.pem:g" | sed "s:SERVER-CERT:node-cert.pem:g" | sed "s:SERVER-KEY:node-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > nc-client-policy.xml
#	cp nc-client-policy.xml $(EUCALYPTUS)/var/eucalyptus/keys/
##	cp ../tools/*.pem $(EUCALYPTUS)/var/eucalyptus/keys/
#	chmod -R 0600 $(EUCALYPTUS)/var/eucalyptus/keys/*
# ws
	$(INSTALL) $(AXIS2C_HOME)/services/$(SERVICE_NAME)/services_WS.xml $(AXIS2C_HOME)/services/$(SERVICE_NAME)/services.xml
# no ws
#	cp $(AXIS2C_HOME)/services/$(SERVICE_NAME)/services_noWS.xml $(AXIS2C_HOME)/services/$(SERVICE_NAME)/services.xml

clean:
	rm -rf $(SERVICE_SO) *.o $(CLIENT) $(CLIENT)_local *~* *#*

distclean:
	rm -rf generated $(SERVICE_SO) *.o $(CLIENT) $(CLIENT)_local *~* *#*

install: deploy
	$(INSTALL) -m 0600 nc-client-policy.xml $(vardir)/eucalyptus/keys/
	$(INSTALL) -m 0755 test_nc $(sbindir)/euca_test_nc
