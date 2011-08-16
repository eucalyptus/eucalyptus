openssl pkcs12 -in ${EUCALYPTUS}/var/lib/eucalyptus/keys/euca.p12 \
-name eucalyptus -name "eucalyptus" \
-password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus \
-nodes 2>&1 | egrep -v '^MAC' | \
grep -A30 "friendlyName: eucalyptus" | \
egrep -A27 "BEGIN (RSA|PRIVATE)" | grep -v 'Bag Attributes' > ${EUCALYPTUS}/var/lib/eucalyptus/keys/cloud-pk.pem


echo -n eucalyptus | \
openssl dgst -sha256 \
-sign ${EUCALYPTUS}/var/lib/eucalyptus/keys/cloud-pk.pem \
-hex | sed 's/.*= //' 

