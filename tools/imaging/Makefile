# Copyright 2011-2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

include ../../Makedefs

ifndef DESTDIR
	DESTDIR := /
endif

all: build

build:
	# Paths that begin with more than one / cause --root to be ignored
	@sed -e 's://*:/:g' setup.cfg.template > setup.cfg
	$(PYTHON) setup.py build

install: build
	@sed -e 's://*:/:g' setup.cfg.template > setup.cfg
	$(PYTHON) setup.py install --root=$(DESTDIR)

tags:
	find bin -regex ".*\.\(h\|c\|py\|in\)" | xargs etags
	find bin -regex ".*\.\(h\|c\|py\|in\)" | xargs ctags

clean:
	@$(RM) -fr ./dist ./build tags TAGS
	@$(RM) -f *~

distclean: clean
	@$(RM) -f setup.cfg

uninstall:
	@echo uninstall
