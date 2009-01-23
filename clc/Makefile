# Makefile wrapper around ant
#
# $Id: Makefile,v 1.5 2008-12-24 18:28:07 nurmi Exp $
#
include ../Makedefs

all: build

deps:
	$(ANT) deps

build: 
	ANT_OPTS="-Xmx512m" $(ANT) build
	make -C modules/storage-controller/c
	make -C modules/storage-manager/c

run: install 
	$(ANT) run

clean:
	$(ANT) clean
	rm -rf modules/storage-controller/h/ modules/storage-controller/lib/ modules/storage-manager/h/ modules/storage-manager/lib/ modules/www/webapps/ target  modules/msgs/resources/msgs-binding.xml




distclean: clean
	$(ANT) distclean

install: build
	$(ANT) install
	@$(INSTALL) -m 644 cloud.xml $(etcdir)/eucalyptus
	make -C modules/storage-controller/c install
	make -C modules/storage-manager/c install

deploy:

