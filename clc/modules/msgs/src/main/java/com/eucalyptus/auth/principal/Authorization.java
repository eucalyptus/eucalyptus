package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import com.eucalyptus.auth.AuthException;

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
  
  public Set<String> getActions( ) throws AuthException;
  
  public Boolean isNotResource( );
  
  public Set<String> getResources( ) throws AuthException;
  
  public List<Condition> getConditions( ) throws AuthException;
  
  public Group getGroup( ) throws AuthException;
  
}
