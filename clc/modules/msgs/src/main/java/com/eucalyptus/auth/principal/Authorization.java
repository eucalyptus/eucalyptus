package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

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
 
  public String getType( );
  
  public Boolean isNotAction( );
  
  public Set<String> getActions( );
  
  public Boolean isNotResource( );
  
  public Set<String> getResources( );
  
  public List<? extends Condition> getConditions( );
  
  public Group getGroup( );
  
}
