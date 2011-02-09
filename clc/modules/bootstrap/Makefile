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
#  Author: Chris Grzegorczyk grze@eucalyptus.com
###############################################################################
include ../../../Makedefs
CC = gcc
CFLAGS = -g -O2 -DOS_LINUX -DDSO_DLFCN \
		 -I$(JAVA_HOME)/include \
		 -I$(JAVA_HOME)/include/linux \
		 -I../../../util \
		 -Wall -Wstrict-prototypes
LDFLAGS =  -ldl -lpthread
JAVACFLAGS = 
RANLIB = ranlib
LDCMD = gcc

OBJS =  eucalyptus-bootstrap.o eucalyptus-opts.o
all: eucalyptus-cloud
build: eucalyptus-cloud
.c.o:
	$(CC) $(CFLAGS) -c $< -o $@

eucalyptus-cloud: $(OBJS)
	$(LDCMD) $(OBJS) -o eucalyptus-cloud $(LDFLAGS)

ggo:
	gengetopt --input=arguments.ggo --file-name=eucalyptus-opts --func-name=arguments --arg-struct-name=eucalyptus_opts

install: eucalyptus-cloud
	$(INSTALL) -d $(DESTDIR)$(sbindir)
	$(INSTALL) -m 0755 eucalyptus-cloud $(DESTDIR)$(sbindir)/eucalyptus-cloud

clean:
	@$(RM) -f $(OBJS) eucalyptus-cloud

distclean: clean

uninstall:
	@$(RM) -f $(DESTDIR)$(sbindir)/eucalyptus-cloud
