# Makefile wrapper around ant
#
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#

all: build

deps:
	$(ANT) deps

build: 
	make -C modules/storage-controller/c

run: install 
	$(ANT) run

clean:
	$(ANT) clean

distclean: clean
	$(ANT) distclean

install: build
	$(ANT) install
	@$(INSTALL) -m 640 cloud.xml $(etcdir)/eucalyptus

deploy:

