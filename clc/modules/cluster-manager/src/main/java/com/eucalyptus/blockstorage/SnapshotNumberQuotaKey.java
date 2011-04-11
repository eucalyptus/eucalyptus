package com.eucalyptus.blockstorage;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;

@PolicyKey( Keys.EC2_QUOTA_SNAPSHOT_NUMBER )
public class SnapshotNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_SNAPSHOT_NUMBER;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_CREATESNAPSHOT ).equals( action ) &&
         PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SNAPSHOT ).equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( StorageUtil.countSnapshotByAccount( id ) + 1 );
      case GROUP:
        throw new AuthException( "Group level quota not supported" );
      case USER:
        return Long.toString( StorageUtil.countSnapshotByUser( id ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
