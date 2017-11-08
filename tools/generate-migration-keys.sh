#!/bin/bash
#
# Script to generate keys for TLS migration of instances between NC nodes.
#
# Copyright 2013 Ent. Services Development Corporation LP
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
#
# Run on NC with 2-4 arguments:
#
# 0. -v (for verbose mode) [optional]
# 1. IP address of this host
# 2. Unique token for migration(s)
# 3. 'restart' (if post-generation restart of libvirtd is desired) [optional]

# Bail on error
set -e

LIBVIRTPID=/var/run/libvirtd.pid

EUCA_CONF=$EUCALYPTUS/etc/eucalyptus/eucalyptus.conf
LIBVIRTD_SYSCONFIG=/etc/sysconfig/libvirtd

KEYDIR=$EUCALYPTUS/var/lib/eucalyptus/keys
NCKEY=node-pk.pem
NCCERT=node-cert.pem

if [ "$1" == "-v" ] ; then
    VERBOSE=true
    shift
fi

if [ $# -eq 2 ] ; then
    NCADDR=$1
    shift
    MIGTOK=$1
    shift
elif [ $# -eq 3 ] ; then
    NCADDR=$1
    shift
    MIGTOK=$1
    shift
    if [ $1 = "restart" ] ; then
        RESTART=yes
    else
        echo "Usage $0 [-v] NC-address migration-token [restart]" >&2
        exit 1
    fi
else
    echo "Usage $0 [-v] NC-address migration-token [restart]" >&2
    exit 1
fi

if [ ! -r $KEYDIR/$NCKEY -o ! -r $KEYDIR/$NCCERT ] ; then
    echo "Cannot find keys ($NCKEY, $NCCERT) in $KEYDIR" >&2
    exit 1
fi

# Set working directory and save all output to a log there.
WORKD=`mktemp -d -t tmp-$NCADDR-XXXXXXXXXX`

# From here on, any stdout to screen must be explicitly forced using >&3, stderr using >&4
exec 3>&1 4>&2 > $WORKD/generate-migration-keys.log 2>&1

echo "Determining Organization from NC cert ..."

NCORG=`certtool -i --infile $KEYDIR/$NCCERT | grep 'Subject: .*CN=' | perl -naF',' -e 'foreach (@F) { /CN=(.*)/ && print ($1) && break}'`

echo "... $NCORG"

# When run with -v, this should be the only message to the console other than the DN at the end,
# and they can be teased apart by running this script with 2> directed elsewhere, such as to /dev/null.
if [ -n "$VERBOSE" ] ; then
    echo "Generating keys under $WORKD ..." >&4
else
    echo "Generating keys under $WORKD ..." >&2
fi


# Generate the server key.
certtool --generate-privkey > $WORKD/serverkey.pem

# Create the server template.
cat > $WORKD/server.info <<EOT
organization = $NCORG
cn = $NCADDR
tls_www_server
encryption_key
signing_key
EOT

# Generate the server cert.
certtool --generate-certificate --load-privkey $WORKD/serverkey.pem --load-ca-certificate $KEYDIR/$NCCERT --load-ca-privkey $KEYDIR/$NCKEY --template $WORKD/server.info --outfile $WORKD/servercert.pem 2> $WORKD/servercert.txt

# Generate the client key.
certtool --generate-privkey > $WORKD/clientkey.pem

# Create the client template.
# I'm using the 'locality' field to hold the migration-specific
# credential/token for now. This may change.
cat > $WORKD/client.info <<EOT
country = US
locality = $MIGTOK
organization = $NCORG
cn = $NCADDR
tls_www_client
encryption_key
signing_key
EOT

certtool --generate-certificate --load-privkey $WORKD/clientkey.pem --load-ca-certificate $KEYDIR/$NCCERT --load-ca-privkey $KEYDIR/$NCKEY --template $WORKD/client.info --outfile $WORKD/clientcert.pem 2> $WORKD/clientcert.txt

echo "Key generation complete."
echo "Installing keys ..."

mkdir -p /etc/pki/libvirt/private
cp -f $KEYDIR/$NCCERT /etc/pki/CA/cacert.pem
cp -f $WORKD/clientcert.pem $WORKD/servercert.pem /etc/pki/libvirt/
cp -f $WORKD/clientkey.pem $WORKD/serverkey.pem /etc/pki/libvirt/private/

# Do this in a subshell due to the sourcing of the eucalyptus.conf file.
(
if [ -r "$EUCA_CONF" ] ; then
    . $EUCA_CONF
fi
if [ -z "$EUCA_USER" ] ; then
    EUCA_USER=root
fi
chmod 700 /etc/pki/libvirt/private
chmod 600 /etc/pki/libvirt/private/clientkey.pem  /etc/pki/libvirt/private/serverkey.pem
chmod 644 /etc/pki/CA/cacert.pem /etc/pki/libvirt/clientcert.pem /etc/pki/libvirt/servercert.pem
chown -R $EUCA_USER /etc/pki/libvirt/private/ || true
)

ensure_listen ()
{
    # Do this in a subshell due to the sourcing of the libvirtd sysconfig file.
    (
    if [ -r "$LIBVIRTD_SYSCONFIG" ] ; then
        . $LIBVIRTD_SYSCONFIG
    fi

    # Disable errexit in case following grep exits nonzero.
    set +e

    LV=`set|grep ^LIBVIRTD_ARGS=`

    if [ $? -ne 0 ] ; then
        # Variable not present
        NEWVAR=1
    fi

    # Re-enable errexit
    set -e

    eval $LV

    declare -A LVS

    for i in `echo $LIBVIRTD_ARGS` ; do
        LVS[$i]=1
    done

    if [ -n "${LVS['--listen']}" ] ; then
        return
    fi

    if [ "${#LVS[@]}" -eq 0 -a -n "$NEWVAR" ] ; then
        LVS['--listen']=1
        echo >> $LIBVIRTD_SYSCONFIG
        echo "#" >> $LIBVIRTD_SYSCONFIG
        echo "# Added by Eucalyptus to enable automatic migration between NC nodes" >> $LIBVIRTD_SYSCONFIG
        echo "#" >> $LIBVIRTD_SYSCONFIG
        echo LIBVIRTD_ARGS="\"${!LVS[@]}\"" >> $LIBVIRTD_SYSCONFIG
    else
        LVS['--listen']=1
        ARGLIST="\"${!LVS[@]}\""
        sed -i.euca "s/^\s*LIBVIRTD_ARGS=.*/LIBVIRTD_ARGS=$ARGLIST/" $LIBVIRTD_SYSCONFIG
    fi
    )
}

# Despite what the documentation says, sending a SIGHUP to libvirtd does
# not appear to update its access list of DNs. So we're doing a full restart.
# Ugh.

#kill -HUP `cat $LIBVIRTPID`

if [ "$RESTART" == "yes" ] ; then
    echo "Key installation complete, restarting libvirtd."

    # Ensure libvirtd is run with --listen arg.
    ensure_listen

    /sbin/service libvirtd restart
else
    echo "Key installation complete, libvirtd will require manual restart."
fi

if [ -n "$VERBOSE" ] ; then
    certtool -i --infile $WORKD/clientcert.pem | grep "Subject: .*CN=" | sed 's/^.*Subject: //' >&3
else
    certtool -i --infile $WORKD/clientcert.pem | grep "Subject: .*CN=" | sed 's/^.*Subject: //'
fi

echo "Done."
