#!/bin/bash

# Generates keys for TLS migration of instances between NC nodes.
#
# Run on CC.

# Bail on error
set -e

KEYDIR=$EUCALYPTUS/var/lib/eucalyptus/keys
NCKEY=node-pk.pem
NCCERT=node-cert.pem

if [ $# -gt 0 ] ; then
	NCADDR=$1
	shift
else
	echo "Usage $0 NC-address" >&2
	exit 1
fi

if [ ! -r $KEYDIR/$NCKEY -o ! -r $KEYDIR/$NCCERT ] ; then
	echo "Cannot find keys ($NCKEY, $NCCERT) in $KEYDIR" >&2
	exit 1
fi

WORKD=`mktemp -d -t tmp-$NCADDR-XXXXXXXXXX`

echo "Determining Organization from NC cert ..."

NCORG=`certtool -i --infile $KEYDIR/$NCCERT | grep 'Subject: .*CN=' | perl -naF',' -e 'foreach (@F) { /CN=(.*)/ && print ($1) && break}'`

echo "... $NCORG"

echo "Generating keys under $WORKD ..."

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
cat > $WORKD/client.info <<EOT
country = US
state = CA
locality = Goleta
organization = $NCORG
cn = $NCADDR
tls_www_client
encryption_key
signing_key
EOT

certtool --generate-certificate --load-privkey $WORKD/clientkey.pem --load-ca-certificate $KEYDIR/$NCCERT --load-ca-privkey $KEYDIR/$NCKEY --template $WORKD/client.info --outfile $WORKD/clientcert.pem 2> $WORKD/clientcert.txt

echo "Key generation complete."
echo "Installing keys ..."

ssh $NCADDR mkdir -p /etc/pki/libvirt/private
ssh $NCADDR chown root.root /etc/pki/libvirt/private
scp $KEYDIR/$NCCERT $NCADDR:/etc/pki/CA/cacert.pem
scp $WORKD/clientcert.pem $WORKD/servercert.pem $NCADDR:/etc/pki/libvirt/
scp $WORKD/clientkey.pem $WORKD/serverkey.pem $NCADDR:/etc/pki/libvirt/private/
ssh $NCADDR chmod 600 /etc/pki/libvirt/private/serverkey.pem

echo "Key installation complete."
