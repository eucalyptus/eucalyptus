# Makefile for eucalyptus/util
#
# Copyright 2008 Regents of the University of California
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
# POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
# THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
# COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
# AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
# IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
# SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
# WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
# REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
# IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
# NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

include ../../Makedefs

TEST_OBJS=../config.o ../ipc.o ../misc.o ../wc.o ../log.o ../euca_string.o ../euca_file.o ../../storage/diskutil.o
STATS_OBJS=../config.o ../ipc.o ../misc.o ../wc.o ../log.o ../euca_string.o ../euca_file.o ../../storage/diskutil.o
STATS_LIBS = -ljson-c -lm
EFENCE=-lefence
#DEBUGS = -DDEBUG # -DDEBUG1
all: sensor_common.o stats.o message_stats.o message_sensor.o fs_emitter.o service_sensor.o 

buildall: build

build: all

test_fs_emitter: fs_emitter.c sensor_common.o $(TEST_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_fs_emitter fs_emitter.c $(TEST_OBJS) sensor_common.o $(STATS_LIBS) $(LIBS) $(LDFLAGS) $(EFENCE)

test_stats: stats.c fs_emitter.o message_stats.o message_sensor.o service_sensor.o sensor_common.o $(TEST_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_stats stats.c fs_emitter.o message_stats.o message_sensor.o service_sensor.o sensor_common.o $(TEST_OBJS) $(STATS_LIBS) $(LIBS) $(LDFLAGS) $(EFENCE)

test_sensor_common: sensor_common.c $(TEST_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_sensor_common sensor_common.c $(TEST_OBJS) $(STATS_LIBS) $(LIBS) $(LDFLAGS) $(EFENCE)

test_message_stats: message_stats.c $(TEST_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_message_stats message_stats.c $(TEST_OBJS) $(STATS_LIBS) $(LIBS) $(LDFLAGS) $(EFENCE)

test_message_sensor: message_sensor.c sensor_common.o message_stats.o $(TEST_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_message_sensor message_sensor.c sensor_common.o message_stats.o $(TEST_OBJS) $(STATS_LIBS) $(LIBS) $(LDFLAGS) $(EFENCE)

test_service_sensor: service_sensor.c sensor_common.o $(TEST_OBJS)
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_service_sensor service_sensor.c sensor_common.o $(TEST_OBJS) $(STATS_LIBS) $(LIBS) $(LDFLAGS) $(EFENCE)

test: all test_fs_emitter test_stats test_sensor_common test_message_stats test_message_sensor test_service_sensor

%.o: %.c %.h
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUGS) -trigraphs `xslt-config --cflags` $<

utils:
	make -C ../

../%.o: utils

../../storage/diskutil.o:
	make -C ../../storage/
indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

clean:
	rm -rf *~ *.o test_fs_emitter test_message_stats test_sensor_common test_stats test_message_sensor test_service_sensor

install: all
	$(INSTALL) -m 0644 internal_sensor.conf $(DESTDIR)$(etcdir)/eucalyptus/

deploy:

uninstall:
	$(RM) -f $(DESTDIR)$(etcdir)/eucalyptus/internal_sensor.conf

