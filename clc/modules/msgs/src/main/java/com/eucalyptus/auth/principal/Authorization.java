package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;

/**
 * @author decker
 *
 */
public interface Authorization extends Serializable {

  public static enum EffectType {
    Deny,
    Allow,
    Limit, // extension to IAM for quota
  }
  
  public EffectType getEffect( );
  
  public String getActionPattern( );
  
  public String getResourceType( );
  
  public String getResourcePattern( );
  
  public Boolean isNegative( );
  
  public List<? extends Condition> getConditions( );
  
}
