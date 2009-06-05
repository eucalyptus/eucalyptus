# Makefile wrapper around ant
#
#
include ../Makedefs

all: build

deps:
	$(ANT) deps

build:
	@# we need JAVA_HOME
	@if test -z "${JAVA_HOME}" ; then \
		echo "You need to define JAVA_HOME!"; exit 1; fi
# this causes problems for pbuilder, should be handled by configure and build.xml already 
#	@if test ! -e deps/cloud-lib.tar.gz; then \
#		echo "Cannot find java deps, please run 'make deps' in the 'clc/' subdirectory."; exit 1; fi
	ANT_OPTS="-Xmx512m" $(ANT) build
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

