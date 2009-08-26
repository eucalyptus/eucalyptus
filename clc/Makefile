# Makefile wrapper around ant
#
#
include ../Makedefs

theme_dir	= ${TOP}/clc/modules/www/src/main/java/edu/ucsb/eucalyptus/admin/public/themes/

all: build

deps:
	$(ANT) deps

update_properties:
	@sed -i "s/\(^extensions =\).*/\1 ${extensions}/" ${TOP}/clc/modules/www/conf/eucalyptus-web.properties

themes:
	@if test -n ${euca_theme} ; then \
		if test -d ${theme_dir}/${euca_theme} ; then \
			rm -f ${theme_dir}/active; \
			ln -sf ${euca_theme} ${theme_dir}/active;\
		else \
			echo "Theme ${euca_theme} does not exists!"; exit 1;\
		fi; fi
	@
	@if test "`grep '^extensions' ${TOP}/clc/modules/www/conf/eucalyptus-web.properties | sed 's/extensions =[[:blank:]]*//'` != '${extensions}'" > /dev/null ; then $(MAKE) update_properties; fi

build: themes
	@# we need JAVA_HOME
	@if test -z "${JAVA_HOME}" ; then \
		echo "You need to define JAVA_HOME!"; exit 1; fi
# this causes problems for pbuilder, should be handled by configure and build.xml already 
#	@if test ! -e deps/cloud-lib.tar.gz; then \
#		echo "Cannot find java deps, please run 'make deps' in the 'clc/' subdirectory."; exit 1; fi
	ANT_OPTS="-Xmx512m" $(ANT) build
	make -C modules/storage-controller/native
	make -C modules/bootstrap

run: install 
	$(ANT) run

clean:
	$(ANT) clean

distclean: clean
	$(ANT) distclean

install: build
	$(ANT) install
#	@$(INSTALL) -m 644 cloud.xml $(etcdir)/eucalyptus
	make -C modules/storage-controller/native install
	make -C modules/bootstrap install

deploy: install

