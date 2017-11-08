#!/bin/sh -e

# Copyright 2015 Ent. Services Development Corporation LP
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

export PYTHONPATH=".:$PYTHONPATH"
export TERM="dumb"  # http://www.incenp.org/notes/2012/python-term-smm-fix.html

py_version=$(python -c 'import sys; print ".".join(map(str, sys.version_info[:2]))')
script_dir="build/scripts-$py_version"
version="$(python -c 'import eucalyptus_admin; print eucalyptus_admin.__version__')"

mkdir -p man
for exe in $@; do
    echo $exe
    description="$($script_dir/$exe --help 2>&1 | python -c 'import sys; print sys.stdin.read().split("\n\n")[1]')"
    help2man -N --no-discard-stderr -S "eucalyptus $version" -n "$description" --version-string "$version" -o man/$(basename $exe).1 $script_dir/$exe
    sed -i -e 's/^.SH DESCRIPTION/.SH SYNOPSIS/' \
           -e 's/usage: *//' \
           -e '/^\.IP/{/^\.IP/d}' \
           -e '/^\.PP/{s/^\.PP.*/.SH DESCRIPTION/}' \
           man/$(basename $exe).1
done
