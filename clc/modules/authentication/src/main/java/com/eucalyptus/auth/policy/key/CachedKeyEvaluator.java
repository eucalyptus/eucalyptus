package com.eucalyptus.auth.policy.key;

import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Authorization;
import com.google.common.collect.Maps;

public class CachedKeyEvaluator {
  
  private Map<Class<? extends Key>, String> cache = Maps.newHashMap( );
  
  public CachedKeyEvaluator( ) {
  }
  
  public String getValue( Key key ) throws AuthException {
    String value = cache.get( key );
    if ( value == null ) {
      value = key.value( );
      cache.put( key.getClass( ), value );
    }
    return value;
  }
}
