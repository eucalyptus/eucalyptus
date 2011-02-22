package com.eucalyptus.auth.policy.condition;

import org.apache.log4j.Logger;

@PolicyCondition( { Conditions.NUMERICGREATERTHANEQUALS, Conditions.NUMERICGREATERTHANEQUALS_S } )
public class NumericGreaterThanEquals implements NumericConditionOp {
  
  private static final Logger LOG = Logger.getLogger( NumericEquals.class );

  @Override
  public boolean check( String key, String value ) {
    try {
      return Integer.valueOf( key ).compareTo( Integer.valueOf( value ) ) >= 0;
    } catch ( NumberFormatException e ) {
      try {
        return Double.valueOf( key ).compareTo( Double.valueOf( value ) ) >= 0;
      } catch ( NumberFormatException e1 ) {
        // It does not make sense to check the equality of two floats.
        LOG.error( "Invalid number format", e1 );        
      }
    }
    return false;
  }
  
}
