#
#

include ../Makedefs

WSDLPATH=../wsdl
CCWSDLNAME=eucalyptus_cc.wsdl
CCWSDL=$(WSDLPATH)/$(CCWSDLNAME)
NCWSDLNAME=eucalyptus_nc.wsdl
NCWSDL=$(WSDLPATH)/$(NCWSDLNAME)
SERVICE_SO=libEucalyptusCC.so
SERVICE_NAME=EucalyptusCC
CLIENT=CCclient
CLIENTKILLALL=euca_killall
#WSDL2C=${AXIS2C_HOME}/bin/tools/wsdl2c/WSDL2C.sh
NCLIBS=../util/data.o ../node/client-marshal-adb.o ../util/ipc.o
SCLIBS=../storage/storage.o 
VNLIBS=../net/vnetwork.o ../util/misc.o
WSSECLIBS=../util/euca_axis.o ../util/euca_auth.o
CC_LIBS 	= ${LIBS} -lcurl

all: server client

build: all

generated/stubs: $(CCWSDL) $(NCWSDL)
	rm -rf generated
	mkdir -p generated
	@echo Generating client stubs
	sh $(WSDL2C) -uri $(CCWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Generating NCclient stubs
	sh $(WSDL2C) -uri $(NCWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Generating server stubs
	sh $(WSDL2C) -uri $(CCWSDL) -sd -ss -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Adding marshalling calls to server stubs
	../tools/add_marshalling.pl generated/axis2_skel_*.c
	@echo Compiling the stubs
	cd generated; patch -p1 < ../axis2_svc_generated.patch; $(CC) -c $(CFLAGS) $(INCLUDES) *.c
	@echo "this is a cute little makefile marker" >generated/stubs
	@echo Stubs generated.

.c.o:
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

server: $(SERVICE_SO)

$(SERVICE_SO): generated/stubs ../storage/storage.o server-marshal.o handlers.o $(AXIS2C_HOME)/lib/libaxutil.so 
	$(CC) -shared generated/*.o server-marshal.o handlers.o $(SCLIBS) $(NCLIBS) $(VNLIBS) $(WSSECLIBS) $(CC_LIBS) -o $(SERVICE_SO)

client: $(CLIENT)_full $(CLIENTKILLALL)

$(CLIENT)_full: generated/stubs $(CLIENT).c cc-client-marshal-adb.c handlers.o $(AXIS2C_HOME)/lib/libaxutil.so
	$(CC) -o $(CLIENT)_full $(CFLAGS) $(INCLUDES) $(CC_LIBS) generated/adb_*.o generated/axis2_stub_*.o  ../util/misc.o $(WSSECLIBS) $(CLIENT).c cc-client-marshal-adb.c -DMODE=1

$(CLIENTKILLALL): generated/stubs $(CLIENT).c cc-client-marshal-adb.c handlers.o $(AXIS2C_HOME)/lib/libaxutil.so
	$(CC) -o $(CLIENTKILLALL) $(CFLAGS) $(INCLUDES) $(CC_LIBS) generated/adb_*.o generated/axis2_stub_*.o  ../util/misc.o $(WSSECLIBS) $(CLIENT).c cc-client-marshal-adb.c -DMODE=0

deploy:
	$(INSTALL) -d $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
	$(INSTALL) $(SERVICE_SO) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
	$(INSTALL) $(CCWSDL) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
#no ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_SERVICES)/$(SERVICE_NAME)/$(CCWSDLNAME) > $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_noWS.xml

#ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_SERVICES)/$(SERVICE_NAME)/$(CCWSDLNAME) | ../tools/add_service_policy.pl ../tools/service-policy-template.xml | sed "s:CLIENT-CERT:cloud-cert.pem:g" | sed "s:CLIENT-KEY:cloud-pk.pem:g" | sed "s:SERVER-CERT:cluster-cert.pem:g" | sed "s:SERVER-KEY:cluster-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_WS.xml
	cat ../tools/client-policy-template.xml | sed "s:EUCALYPTUS_HOME:$(EUCALYPTUS):g" | sed "s:AXIS2C_HOME:$(AXIS2C_HOME):g" | sed "s:CLIENT-CERT:cloud-cert.pem:g" | sed "s:SERVER-CERT:cluster-cert.pem:g" | sed "s:SERVER-KEY:cluster-pk.pem:g" | sed "s:CLIENT-KEY:cloud-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > cc-client-policy.xml
##	cp ../tools/*.pem $(EUCALYPTUS)/var/lib/eucalyptus/keys/
#	cp cc-client-policy.xml $(EUCALYPTUS)/var/lib/eucalyptus/keys/
#	chmod -R 0600 $(EUCALYPTUS)/var/lib/eucalyptus/keys/*
# ws
	$(INSTALL) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_WS.xml $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services.xml
# no ws
#	cp $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_noWS.xml $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services.xml

clean:
	rm -f $(SERVICE_SO) *.o $(CLIENTKILLALL) $(CLIENT)_full *~* *#*

distclean: clean
	rm -rf generated

install: deploy
	@$(INSTALL) -m 0555 $(CLIENTKILLALL) $(DESTDIR)$(sbindir)
	@$(INSTALL) cc-client-policy.xml $(DESTDIR)$(vardir)/lib/eucalyptus/keys/

uninstall:
	@$(RM) -f $(DESTDIR)$(sbindir)/$(CLIENTKILLALL)
	@$(RM) -f $(DESTDIR)$(vardir)/lib/eucalyptus/keys/cc-client-policy.xml
	@$(RM) -rf $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)
