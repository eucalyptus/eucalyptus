package com.eucalyptus.auth.policy.condition;

import org.apache.log4j.Logger;

@PolicyCondition( { Conditions.NUMERICEQUALS, Conditions.NUMERICEQUALS_S } )
public class NumericEquals implements NumericConditionOp {
  
  private static final Logger LOG = Logger.getLogger( NumericEquals.class );

  @Override
  public boolean check( String key, String value ) {
    try {
      return Integer.valueOf( key ).equals( Integer.valueOf( value ) );
    } catch ( NumberFormatException e ) {
      // It does not make sense to check the equality of two floats.
      LOG.error( "Invalid number format", e );
    }
    return false;
  }
  
}
