package com.eucalyptus.auth.policy.condition;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.key.Cidr;
import com.eucalyptus.auth.policy.key.CidrParseException;

@PolicyCondition( { Conditions.IPADDRESS } )
public class IpAddress implements AddressConditionOp {
  
  private static final Logger LOG = Logger.getLogger( IpAddress.class );
                                                     
  @Override
  public boolean check( String key, String value ) {
    try {
      return Cidr.valueOf( value ).matchIp( key );
    } catch ( CidrParseException e ) {
      LOG.error( "Invalid IP address and CIDR: " + key + ", " + value, e );
      return false;
    }
  }
  
}
