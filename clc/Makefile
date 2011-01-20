# Makefile wrapper around ant
#
#
include ../Makedefs

theme_dir	= ${TOP}/clc/modules/www/src/main/java/edu/ucsb/eucalyptus/admin/public/themes
web_prop_dir	= ${TOP}/clc/modules/www/conf

.PHONY: all build update_properties update_theme deps extras install

all: deps build

deps:
	@$(ANT) deps

update_properties: ${web_prop_dir}/eucalyptus-web.properties

${web_prop_dir}/eucalyptus-web.properties: ${web_prop_dir}/eucalyptus-web-default.properties ${TOP}/Makedefs
	@sed "s/\(^extensions =\).*/\1 ${extensions}/" < ${web_prop_dir}/eucalyptus-web-default.properties > ${web_prop_dir}/eucalyptus-web.properties
	@echo "Copied defaults to ${web_prop_dir}/eucalyptus-web.properties"

update_theme:
	@if test -d ${theme_dir}/${euca_theme} ; then \
		if [ "`readlink ${theme_dir}/active`" != "${euca_theme}" ] ; then \
			echo "Making '${euca_theme}' the active theme" ; \
			rm -f ${theme_dir}/active ; \
			ln -sf ${euca_theme} ${theme_dir}/active ; \
		fi \
	else \
		echo "Theme ${theme_dir}/${euca_theme} is not found!"; exit 1; \
	fi \

extras: update_properties
	@if test -n "${euca_theme}" ; then \
		$(MAKE) update_theme; \
	else \
		if [ "`readlink ${theme_dir}/active`" != "eucalyptus" ] ; then \
			echo "Making 'eucalyptus' the active theme" ; \
			rm -f ${theme_dir}/active ; \
			ln -sf eucalyptus ${theme_dir}/active ; \
		fi \
	fi

build: deps extras
	@# we need JAVA_HOME
	@if test -z "${JAVA_HOME}" ; then \
		echo "You need to define JAVA_HOME!"; exit 1; fi
# this causes problems for pbuilder, should be handled by configure and build.xml already 
#	@if test ! -e deps/cloud-lib.tar.gz; then \
#		echo "Cannot find java deps, please run 'make deps' in the 'clc/' subdirectory."; exit 1; fi
	@ANT_OPTS="-Xmx512m" $(ANT) build
	@make -C modules/storage-controller/native
	@make -C modules/bootstrap

clean:
	$(ANT) clean
	make -C modules/bootstrap clean
	make -C modules/storage-controller/native clean

distclean: clean
	$(ANT) distclean
	make -C modules/bootstrap distclean
	make -C modules/storage-controller/native distclean

install: build
	@$(ANT) install
	@make -C modules/storage-controller/native install
	@make -C modules/bootstrap install

deploy: install

uninstall:
	make -C modules/storage-controller/native uninstall
	make -C modules/bootstrap uninstall
	$(RM) -rf $(DESTDIR)$(etcdir)/eucalyptus/cloud.d
	$(RM) -rf $(DESTDIR)$(datarootdir)/eucalyptus/*jar
	$(RM) -rf $(DESTDIR)$(vardir)/eucalyptus/webapps
