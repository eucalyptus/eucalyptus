#
# $Id: Makefile,v 1.12 2008-12-09 21:24:33 graziano Exp $
# 
# An easy way to build a roll

#include ../Makedefs

URL_BASE=http://eucalyptus.cs.ucsb.edu/dependencies/1.4/CentOS/RPMS

all: get_rpm
	@(cd eucalyptus; make roll)

get_rpm:
	@if [ ! -x /opt/rocks/bin/rocks ]; then \
		echo "You are not on rocks!"; exit 1; fi
	@mkdir -p eucalyptus/RPMS/`uname -m`
	@echo "removing old rpms ..."
	@rm -f eucalyptus/RPMS/`uname -m`/*
	@echo "downloading rpms ..."
	@(cd eucalyptus/RPMS/`uname -m`; wget -np -q -nd -r -A .rpm ${URL_BASE}/`uname -m`/)

dist:
	@rm -f euca-roll.tgz
	@tar --exclude CVS -czf euca-roll.tgz *

clean:
	@rm euca-roll.tgz

distclean:

install:
