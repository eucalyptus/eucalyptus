#
#


SRCDIRS = `find . -type d -maxdepth 1 -not -name CVS -not -name .`

default:	rpm

copyright rpm clean::
	for i in $(SRCDIRS) ; do \
		(cd $$i; make $@) ; \
	done
