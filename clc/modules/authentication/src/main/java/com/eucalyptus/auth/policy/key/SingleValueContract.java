package com.eucalyptus.auth.policy.key;

import java.util.List;
import com.eucalyptus.auth.Contract;

public class SingleValueContract implements Contract {
  
  private String name;
  private String value;
  
  public SingleValueContract( String name, String value ) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String getName( ) {
    return this.name;
  }

  @Override
  public String getValue( ) {
    return this.value;
  }

  @Override
  public List<String> getValues( ) {
    throw new RuntimeException( "SingleValueContract does not have multiple values" );
  }
  
}
