# Makefile wrapper around ant
#
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#

include ../Makedefs

all: build

deps:
	$(ANT) deps

build: deps
	ANT_OPTS="-Xmx512m" $(ANT) build

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

