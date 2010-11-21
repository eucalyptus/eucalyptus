package com.eucalyptus.auth.policy.key;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;

public abstract class ContractKey implements Key {

  public abstract Contract getContract( String[] values );
  
  @Override
  public String value( ) throws AuthException {
    throw new RuntimeException( "ContractKey has no value." );
  }
  
}
