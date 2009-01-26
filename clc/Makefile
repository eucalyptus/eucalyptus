# Makefile wrapper around ant
#
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#
include ../Makedefs

all: build

deps:
	$(ANT) deps

build:
	@if test ! -e deps/cloud-lib.tar.gz; then \
		echo "Did you forget to run ant deps?"; exit 1; fi
	ANT_OPTS="-Xmx512m" $(ANT) build

run: install 
	$(ANT) run

clean:
	$(ANT) clean

distclean: clean
	$(ANT) distclean

install: build
	$(ANT) install
	@$(INSTALL) -m 644 cloud.xml $(etcdir)/eucalyptus

deploy:

