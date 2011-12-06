###############################################################################
#  Copyright (c) 2009  Eucalyptus Systems, Inc.
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, only version 3 of the License.
#
#
#  This file is distributed in the hope that it will be useful, but WITHOUT
#  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
#  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
#  for more details.
#
#  You should have received a copy of the GNU General Public License along
#  with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#  Please contact Eucalyptus Systems, Inc., 130 Castilian
#  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
#  if you need additional information or have any questions.
#
###############################################################################
include ../../Makedefs

ifndef DESTDIR
	DESTDIR := /
endif

all: euca_admin
build: euca_admin

euca_admin:
	@sed -e 's://*:/:g' setup.cfg.template > setup.cfg
	$(PYTHON) setup.py build

install: euca_admin
	@sed -e 's://*:/:g' setup.cfg.template > setup.cfg
	$(PYTHON) setup.py install --root=$(DESTDIR)

tags:
	find bin eucadmin -regex ".*\.\(h\|c\|py\|in\)" | xargs etags
	find bin eucadmin -regex ".*\.\(h\|c\|py\|in\)" | xargs ctags

clean:
	@$(RM) -fr ./dist ./build tags TAGS

distclean: clean
	@$(RM) -f setup.cfg setup.cfg.template bin/euca_conf

uninstall:
	@echo uninstall
