# Makefile for eucalyptus/storage
#
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#

include ../Makedefs

STORAGE_LIBS = ${LIBS} -lcurl

all: storage.o walrus.o Wclient

build: all

Wclient: Makefile Wclient.c ../util/euca_auth.o ../util/misc.o walrus.o
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient ../util/euca_auth.o ../util/misc.o walrus.o $(STORAGE_LIBS)

walrus.o: walrus.c walrus.h
	$(CC) $(CFLAGS) $(INCLUDES) -c walrus.c 

.c.o: 
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/euca_auth.o: ../util/euca_auth.c ../util/euca_auth.h
	make -C ../util

clean:
	rm -rf *~ *.o Wclient

distclean:

install:

deploy:

