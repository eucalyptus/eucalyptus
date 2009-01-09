# $Id: Makefile,v 1.2 2008-05-11 23:07:34 graziano Exp $
#
#


SRCDIRS = `find . -type d -maxdepth 1 -not -name CVS -not -name .`

default:	rpm

copyright rpm clean::
	for i in $(SRCDIRS) ; do \
		(cd $$i; make $@) ; \
	done
