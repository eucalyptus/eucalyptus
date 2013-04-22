#!/bin/bash

# Generates keys for TLS migration of instances between NC nodes.
#
# Run on NC with three arguments:
#
# 0. "source", "destination", or "both" to indicate role in migration(s).
# 1. IP address of this host.
# 2. Unique token for migration(s).

# Bail on error
set -e

LIBVIRTPID=/var/run/libvirtd.pid

KEYDIR=$EUCALYPTUS/var/lib/eucalyptus/keys
NCKEY=node-pk.pem
NCCERT=node-cert.pem

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
        echo "Usage $0 NC-address migration-token [restart]" >&2
        exit 1
    fi
else
    echo "Usage $0 NC-address migration-token [restart]" >&2
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

# This should be the only message to the console other than the DN at the end,
# and they can be teased apart by running this script with 2> directed elsewhere,
# such as to /dev/null
echo "Generating keys under $WORKD ..." >&4

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
# credential/token for now. This will probably change.
#
cat > $WORKD/client.info <<EOT
country = US
locality = $MIGTOK
organization = $NCORG
cn = $NCADDR
tls_www_client
encryption_key
signing_key
EOT
# state = CA

certtool --generate-certificate --load-privkey $WORKD/clientkey.pem --load-ca-certificate $KEYDIR/$NCCERT --load-ca-privkey $KEYDIR/$NCKEY --template $WORKD/client.info --outfile $WORKD/clientcert.pem 2> $WORKD/clientcert.txt

echo "Key generation complete."
echo "Installing keys ..."

mkdir -p /etc/pki/libvirt/private
chown root.root /etc/pki/libvirt/private
cp -f $KEYDIR/$NCCERT /etc/pki/CA/cacert.pem
cp -f $WORKD/clientcert.pem $WORKD/servercert.pem /etc/pki/libvirt/
cp -f $WORKD/clientkey.pem $WORKD/serverkey.pem /etc/pki/libvirt/private/
chmod 600 /etc/pki/libvirt/private/serverkey.pem

# Despite what the documentation says, sending a SIGHUP to libvirtd does
# not appear to update its access list of DNs. So we're doing a full restart.
# Ugh.
#kill -HUP `cat $LIBVIRTPID`
if [ "$RESTART" == "yes" ] ; then
    echo "Key installation complete, reloading libvirtd."
    service libvirtd restart
else 
    echo "Key installation complete, libvirtd will require manual restart."
fi

certtool -i --infile $WORKD/clientcert.pem | grep "Subject: .*CN=" | sed 's/^.*Subject: //' >&3

echo "Done."
