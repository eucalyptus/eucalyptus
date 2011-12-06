package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.NumericLessThanEquals;
import com.eucalyptus.auth.principal.Account;

public abstract class QuotaKey implements Key {

  public static final String NOT_SUPPORTED = "Not supported";
  
  public static enum Scope {
    ACCOUNT,
    GROUP,
    USER,
  }
  
  public abstract String value( Scope scope, String id, String resource, Long quantity ) throws AuthException;
  
  public static final Long MB = 1024 * 1024L;
  
  @Override
  public final String value( ) throws AuthException {
    throw new RuntimeException( "QuotaKey should not call the default value interface." );
  }
  
  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( conditionClass != NumericLessThanEquals.class ) {
      throw new JSONException( "A quota key is not allowed in condition " + conditionClass.getName( ) + ". NumericLessThanEquals is required." );
    }
  }
  
  public static Long toMb( Long sizeInBytes ) {
    return sizeInBytes / MB;
  }
}
