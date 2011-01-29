openssl pkcs12 -in ${EUCALYPTUS}/var/lib/eucalyptus/keys/euca.p12 \
-name eucalyptus -name "eucalyptus" \
-password pass:eucalyptus  -passin pass:eucalyptus -passout pass:eucalyptus \
-nodes | \
grep -A30 "friendlyName: eucalyptus" | \
grep -A26 "BEGIN RSA" >  ${EUCALYPTUS}/var/lib/eucalyptus/keys/cloud-pk.pem


PASS=$(echo -n eucalyptus | \
openssl dgst -sha256 \
-sign ${EUCALYPTUS}/var/lib/eucalyptus/keys/cloud-pk.pem \
-hex)

echo export PASS="${PASS}"
echo mysql -u eucalyptus --password=${PASS} --port=8777 --protocol=TCP


