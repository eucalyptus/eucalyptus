package com.eucalyptus.auth.policy.condition;

import java.text.ParseException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.key.Iso8601DateParser;

@PolicyCondition( { Conditions.DATEEQUALS, Conditions.DATEEQUALS_S } )
public class DateEquals implements DateConditionOp {
  
  private static final Logger LOG = Logger.getLogger( DateEquals.class );
  
  @Override
  public boolean check( String key, String value ) {
    try {
      return Iso8601DateParser.parse( key ).equals( Iso8601DateParser.parse( value ) );
    } catch ( ParseException e ) {
      LOG.error( "Invalid input date input", e );
      return false;
    }
  }
  
}
