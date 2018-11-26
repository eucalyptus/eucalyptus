# Makefile for eucalyptus/net
#
# Copyright 2009-2012 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

include ../Makedefs

# Standard Libraries, Dependencies and Includes
STDLIBS      := -lpthread -lm -lssl -lxml2 -lcurl -lcrypto -ljson-c
STDDEPS      := ../util/sequence_executor.o ../util/atomic_file.o ../util/log.o ../util/ipc.o ../util/misc.o  
STDDEPS      += ../util/euca_string.o ../util/euca_file.o ../util/hash.o ../util/fault.o ../util/wc.o ../util/utf8.o  
STDDEPS      += ../util/euca_auth.o ../storage/diskutil.o ../storage/http.o ../util/config.o ../util/euca_network.o
STDINC       +=
 
# The Eucalyptus Network Library
LIBNET       := euca_gni ipt_handler ips_handler ebt_handler dev_handler eucanetd_util
LIBNETOBJS   := $(LIBNET:=.o)
LIBNETDEPS   := $(LIBNETOBJS) $(STDDEPS)
LIBNETNAME   := libeucanet.a

# The EUCANETD Cloud Component
EUCANETD     := eucanetd eucanetd_edge  
EUCANETD     += eucanetd_vpc midonet-api euca-to-mido
EUCANETDOBJS := $(EUCANETD:=.o)
EUCANETDDEPS := $(EUCANETDOBJS) $(LIBNETNAME) $(STDDEPS)
EUCANETDNAME := eucanetd

# The EUCA_ARP Cloud Component
EUCAARP      := euca_arp
EUCAARPOBJS  := $(EUCAARP:=.o)
EUCAARPDEPS  := $(EUCAARPOBJS) $(STDDEPS) 
EUCAARPNAME  := euca_arp

# Various global Makefile parameters
TARGETS      := $(LIBNETNAME) $(EUCANETDNAME) $(EUCAARPNAME)  
MAJOR        := 0
AR           := /usr/bin/ar
ARFLAGS      := -cq
CFLAGS       += `xslt-config --cflags`

all: $(TARGETS)

build: all

$(LIBNETNAME): $(LIBNETDEPS)
	$(AR) $(ARFLAGS) $@ $(LIBNETOBJS)

$(EUCANETDNAME): $(EUCANETDDEPS)
	$(CC) -o $@ $(EUCANETDDEPS) $(LIBNETNAME) $(STDLIBS)

$(EUCAARPNAME): $(EUCAARPDEPS)
	$(CC) -o $@ $(EUCAARPDEPS) $(STDLIBS)

.c.o:
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $<

clean:
	@rm -rf *~ *.o *.a $(LIBNETNAME) $(EUCANETDNAME) $(EUCAARPNAME)

distclean: clean

install:
	@$(INSTALL) -d $(DESTDIR)$(sbindir)
	@$(INSTALL) -d $(DESTDIR)$(libexecdir)/eucalyptus
	@$(INSTALL) -m 755 eucanetd $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 700 euca_arp $(DESTDIR)$(libexecdir)/eucalyptus/announce-arp

deploy:
	@$(INSTALL) -d $(DESTDIR)$(sbindir)
	@$(INSTALL) -d $(DESTDIR)$(libexecdir)/eucalyptus
	@$(INSTALL) -m 755 eucanetd $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 700 euca_arp $(DESTDIR)$(libexecdir)/eucalyptus/announce-arp

uninstall:
	@$(RM) -f $(DESTDIR)$(sbindir)/eucanetd
	@$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/announce-arp

indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

