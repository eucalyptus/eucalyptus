package com.eucalyptus.auth.policy.key;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * IAM condition key constants.
 * 
 * @author wenye
 *
 */
public class Keys {

  public static final String AWS_CURRENTTIME = "aws:currenttime";
  
  public static final String S3_MAX_KEYS = "s3:max-keys";
  
  public static final String EC2_KEEPALIVE = "ec2:keepalive";
  public static final String EC2_EXPIRATIONTIME = "ec2:expirationtime";
  
  public static final String EC2_VMNUMBER = "ec2:vmnumber";
  
  public static final Map<String, Class<? extends Key>> KEY_MAP = Maps.newHashMap( );
  
  static {
    KEY_MAP.put( AWS_CURRENTTIME, CurrentTime.class );
    
    KEY_MAP.put( S3_MAX_KEYS, MaxKeys.class );
    
    KEY_MAP.put( EC2_KEEPALIVE, KeeyAlive.class );
    KEY_MAP.put( EC2_EXPIRATIONTIME, ExpirationTime.class );
    
    KEY_MAP.put( EC2_VMNUMBER, VmNumber.class );
  }
  
  public static Key getKeyInstance( Class<? extends Key> keyClass ) {
    try {
      Key key = keyClass.newInstance( );
      return key;
    } catch ( IllegalAccessException e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    } catch ( InstantiationException e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    } catch ( ExceptionInInitializerError e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    } catch ( SecurityException e ) {
      throw new RuntimeException( "Can not find key class " + keyClass.getName( ), e );
    }
  }
  
  /*
   * For testing.
   */
  public static boolean registerKey( String keyName, Class<? extends Key> keyClass ) {
    if ( KEY_MAP.containsKey( keyName ) ) {
      return false;
    }
    KEY_MAP.put( keyName, keyClass );
    return true;
  }
  
}
