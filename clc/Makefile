# Makefile wrapper around ant
#
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#
include ../Makedefs

all: build

deps:
	$(ANT) deps

build:
	ANT_OPTS="-Xmx1g" $(ANT) build
	make -C modules/storage-controller/native
	make -C modules/storage-manager/native

run: install 
	$(ANT) run

clean:
	$(ANT) clean

distclean: clean
	$(ANT) distclean

install: build
	$(ANT) install
	@$(INSTALL) -m 644 cloud.xml $(etcdir)/eucalyptus
	make -C modules/storage-controller/native install
	make -C modules/storage-manager/native install

deploy: install

