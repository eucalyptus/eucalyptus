#
#

include ../Makedefs

WSDLPATH=../wsdl/
GLWSDLNAME=eucalyptus_gl.wsdl
GLWSDL=$(WSDLPATH)/$(GLWSDLNAME)
SERVICE_SO=libEucalyptusGL.so
SERVICE_NAME=EucalyptusGL
CLIENT=GLclient
#WSDL2C=$(AXIS2C_HOME)/bin/tools/wsdl2c/WSDL2C.sh

all: server client 

build: all

generated/stubs: $(GLWSDL)
	rm -rf generated
	mkdir -p generated
	@echo Generating client stubs
	sh $(WSDL2C) -uri $(GLWSDL) -g -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Generating server stubs
	sh $(WSDL2C) -uri $(GLWSDL) -sd -ss -d adb -u -uw -f -o generated | grep -v 'the classpath'
	@echo Adding marshalling calls to server stubs
	../tools/add_marshalling.pl generated/axis2_skel_*.c
	@echo Compiling the stubs
	cd generated; $(CC) -c $(CFLAGS) $(INCLUDES) -I../../util *.c; cd ..
	@echo "this is a cute little makefile marker" >generated/stubs
	@echo Stubs generated.

.c.o:
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

server: $(SERVICE_SO)

$(SERVICE_SO): generated/stubs gl-client-marshal-adb.o server-marshal.o handlers.o $(AXIS2C_HOME)/lib/libaxutil.so 
	$(CC) -shared generated/*.o server-marshal.o handlers.o $(WSSECLIBS) -L$(AXIS2C_HOME)/lib $(LIBS) ../util/misc.o ../util/euca_auth.o ./gl-client-marshal-adb.o -o $(SERVICE_SO)

client: $(CLIENT)

$(CLIENT): generated/stubs $(CLIENT).c gl-client-marshal-adb.c handlers.o $(AXIS2C_HOME)/lib/libaxutil.so
	$(CC) -o $(CLIENT) $(CFLAGS) $(INCLUDES) $(LIBS) generated/adb_*.o generated/axis2_stub_*.o  ../util/misc.o ../util/euca_auth.o $(CLIENT).c gl-client-marshal-adb.c -DMODE=1

deploy:
	$(INSTALL) -d $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
	$(INSTALL) $(SERVICE_SO) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
	$(INSTALL) $(GLWSDL) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/
#no ws-sec
	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_SERVICES)/$(SERVICE_NAME)/$(GLWSDLNAME) > $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_noWS.xml

#ws-sec
#	cat generated/services.xml | ../tools/add_wsdllocation.pl $(AXIS2C_SERVICES)/$(SERVICE_NAME)/$(GLWSDLNAME) | ../tools/add_service_policy.pl ../tools/service-policy-template.xml | sed "s:CLIENT-CERT:cloud-cert.pem:g" | sed "s:CLIENT-KEY:cloud-pk.pem:g" | sed "s:SERVER-CERT:cluster-cert.pem:g" | sed "s:SERVER-KEY:cluster-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_WS.xml
#	cat ../tools/client-policy-template.xml | sed "s:EUCALYPTUS_HOME:$(EUCALYPTUS):g" | sed "s:AXIS2C_HOME:$(AXIS2C_HOME):g" | sed "s:CLIENT-CERT:cloud-cert.pem:g" | sed "s:SERVER-CERT:cluster-cert.pem:g" | sed "s:SERVER-KEY:cluster-pk.pem:g" | sed "s:CLIENT-KEY:cloud-pk.pem:g" | sed "s:CLIENT-USERNAME:eucalyptus:g" > cc-client-policy.xml
#	cp ../tools/*.pem $(EUCALYPTUS)/var/lib/eucalyptus/keys/
#	cp cc-client-policy.xml $(EUCALYPTUS)/var/lib/eucalyptus/keys/
#	chmod -R 0600 $(EUCALYPTUS)/var/lib/eucalyptus/keys/*
# ws
#	cp $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_WS.xml $(AXIS2C_SERVICES)/$(SERVICE_NAME)/services.xml
# no ws
	$(INSTALL) $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services_noWS.xml $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)/services.xml

clean:
	rm -f $(SERVICE_SO) *.o $(CLIENT) *~* *#*  

distclean:
	rm -rf generated $(SERVICE_SO) *.o $(CLIENT) *~* *#*

install:

uninstall:
	@$(RM) -rf $(DESTDIR)$(AXIS2C_SERVICES)/$(SERVICE_NAME)
