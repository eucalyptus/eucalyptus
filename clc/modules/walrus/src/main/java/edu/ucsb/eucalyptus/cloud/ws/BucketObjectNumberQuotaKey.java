package edu.ucsb.eucalyptus.cloud.ws;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.policy.key.QuotaKey.Scope;

@PolicyKey( Keys.S3_QUOTA_BUCKET_OBJECT_NUMBER )
public class BucketObjectNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.S3_QUOTA_BUCKET_OBJECT_NUMBER;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_S3, PolicySpec.S3_PUTOBJECT ).equals( action ) &&
         PolicySpec.qualifiedName( PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT ).equals( resourceType ) ) {
    return true;
  }
  return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( WalrusUtil.countBucketObjectNumber( resource ) + 1 );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return Long.toString( WalrusUtil.countBucketObjectNumber( resource ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
