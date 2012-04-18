package com.eucalyptus.auth.policy.ern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import net.sf.json.JSONException;
import com.eucalyptus.auth.policy.PolicySpec;

public abstract class Ern {
  
  private static final Logger LOG = Logger.getLogger( Ern.class );
  
  public static final String ARN_PREFIX = "arn:aws:";
  // Resource ARN syntax
  public static final Pattern ARN_PATTERN =
      Pattern.compile( "\\*" + 
                       "|(?:arn:aws:(?:" +
                           "(?:(" + PolicySpec.VENDOR_IAM + ")::([a-z0-9]+):(user|group)((?:/[^/\\s]+)+))" +
                           "|(?:(" + PolicySpec.VENDOR_EC2 + "):::([a-z0-9_]+)/(\\S+))" +
                           "|(?:(" + PolicySpec.VENDOR_S3 + "):::([^\\s/]+)(?:(/\\S+))?)" +
                           ")" +
                       ")" );

  // Group index of ARN fields in the ARN pattern
  public static final int ARN_PATTERNGROUP_IAM = 1;
  public static final int ARN_PATTERNGROUP_IAM_NAMESPACE = 2;
  public static final int ARN_PATTERNGROUP_IAM_USERGROUP = 3;
  public static final int ARN_PATTERNGROUP_IAM_ID = 4;
  public static final int ARN_PATTERNGROUP_EC2 = 5;
  public static final int ARN_PATTERNGROUP_EC2_TYPE = 6;
  public static final int ARN_PATTERNGROUP_EC2_ID = 7;
  public static final int ARN_PATTERNGROUP_S3 = 8;
  public static final int ARN_PATTERNGROUP_S3_BUCKET = 9;
  public static final int ARN_PATTERNGROUP_S3_OBJECT = 10;
 
  protected String vendor;
  protected String region = "";
  protected String namespace = "";

  public static Ern parse( String ern ) throws JSONException {
    Matcher matcher = ARN_PATTERN.matcher( ern );
    if ( !matcher.matches( ) ) {
      throw new JSONException( "'" + ern + "' is not a valid ARN" );
    }
    if ( matcher.group( ARN_PATTERNGROUP_IAM ) != null ) {
      String pathName = matcher.group( ARN_PATTERNGROUP_IAM_ID );
      String path;
      String name;
      int lastSlash = pathName.lastIndexOf( '/' );
      if ( lastSlash == 0 ) {
        path = "/";
        name = pathName.substring( 1 );
      } else {
        path = pathName.substring( 0, lastSlash );
        name = pathName.substring( lastSlash + 1 );
      }
      return new EuareResourceName( matcher.group( ARN_PATTERNGROUP_IAM_NAMESPACE ),
                                    matcher.group( ARN_PATTERNGROUP_IAM_USERGROUP ),
                                    path,
                                    name);
    } else if ( matcher.group( ARN_PATTERNGROUP_EC2 ) != null ) {
      String type = matcher.group( ARN_PATTERNGROUP_EC2_TYPE ).toLowerCase( );
      if ( !PolicySpec.EC2_RESOURCES.contains( type ) ) {
        throw new JSONException( "EC2 type '" + type + "' is not supported" );
      }
      String id = matcher.group( ARN_PATTERNGROUP_EC2_ID ).toLowerCase( );
      if ( PolicySpec.EC2_RESOURCE_ADDRESS.equals( type ) ) {
        AddressUtil.validateAddressRange( id );
      }
      return new Ec2ResourceName( type, id );
    } else if ( matcher.group( ARN_PATTERNGROUP_S3 ) != null ) {
      String bucket = matcher.group( ARN_PATTERNGROUP_S3_BUCKET );
      String object = matcher.group( ARN_PATTERNGROUP_S3_OBJECT );
      return new S3ResourceName( bucket, object );
    } else {
      return new WildcardResourceName( );
    }
  }
  
  public String getVendor( ) {
    return vendor;
  }

  public String getNamespace( ) {
    return namespace;
  }

  public String getRegion( ) {
    return region;
  }
  
  public abstract String getResourceType( );
  
  public abstract String getResourceName( );
  
}
