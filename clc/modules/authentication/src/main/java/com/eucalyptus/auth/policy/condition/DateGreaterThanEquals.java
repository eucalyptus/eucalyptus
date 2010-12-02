package com.eucalyptus.auth.policy.condition;

import java.text.ParseException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.key.Iso8601DateParser;

@PolicyCondition( { Conditions.DATEGREATERTHANEQUALS, Conditions.DATEGREATERTHANEQUALS_S } )
public class DateGreaterThanEquals implements DateConditionOp {
  
  private static final Logger LOG = Logger.getLogger( DateEquals.class );
  
  @Override
  public boolean check( String key, String value ) {
    try {
      return Iso8601DateParser.parse( key ).compareTo( Iso8601DateParser.parse( value ) ) >= 0;
    } catch ( ParseException e ) {
      LOG.error( "Invalid input date input", e );
      return false;
    }
  }
  
}
