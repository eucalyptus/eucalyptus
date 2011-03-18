openssl pkcs12 -in ${EUCALYPTUS}/var/lib/eucalyptus/keys/euca.p12 \
-name eucalyptus -name "eucalyptus" \
-password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus \
-nodes | \
grep -A30 "friendlyName: eucalyptus" | \
grep -A26 "BEGIN RSA" >  ${EUCALYPTUS}/var/lib/eucalyptus/keys/cloud-pk.pem


echo -n eucalyptus | \
openssl dgst -sha256 \
-sign ${EUCALYPTUS}/var/lib/eucalyptus/keys/cloud-pk.pem \
-hex


