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

  // Keys
  public static final String AWS_CURRENTTIME = "aws:currenttime";
  public static final String AWS_SOURCEIP = "aws:sourceip";
  
  public static final String S3_MAX_KEYS = "s3:max-keys";
  
  public static final String EC2_KEEPALIVE = "ec2:keepalive";
  public static final String EC2_EXPIRATIONTIME = "ec2:expirationtime";
    
  // Quota keys
  public static final String EC2_QUOTA_VM_INSTANCE_NUMBER = "ec2:quota-vminstancenumber";
  public static final String EC2_QUOTA_IMAGE_NUMBER = "ec2:quota-imagenumber";
  public static final String EC2_QUOTA_VOLUME_NUMBER = "ec2:quota-volumenumber";
  public static final String EC2_QUOTA_VOLUME_TOTAL_SIZE = "ec2:quota-volumetotalsize";
  public static final String EC2_QUOTA_SNAPSHOT_NUMBER = "ec2:quota-snapshotnumber";
  public static final String EC2_QUOTA_ADDRESS_NUMBER = "ec2:quota-addressnumber";
  
  public static final String S3_QUOTA_BUCKET_NUMBER = "s3:quota-bucketnumber";
  public static final String S3_QUOTA_BUCKET_TOTAL_SIZE = "s3:quota-buckettotalsize";
  public static final String S3_QUOTA_BUCKET_SIZE = "s3:quota-bucketsize";
  public static final String S3_QUOTA_BUCKET_OBJECT_NUMBER = "s3:quota-bucketobjectnumber";
  
  public static final String IAM_QUOTA_USER_NUMBER = "iam:quota-usernumber";
  public static final String IAM_QUOTA_GROUP_NUMBER = "iam:quota-groupnumber";

  private static final Map<String, Class<? extends Key>> KEY_MAP = Maps.newHashMap( );
    
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
  
  public static Class<? extends Key> getKeyClass( String name ) {
    return KEY_MAP.get( name );
  }
  
  public synchronized static boolean registerKey( String keyName, Class<? extends Key> keyClass ) {
    if ( KEY_MAP.containsKey( keyName ) ) {
      return false;
    }
    KEY_MAP.put( keyName, keyClass );
    return true;
  }
  
}
