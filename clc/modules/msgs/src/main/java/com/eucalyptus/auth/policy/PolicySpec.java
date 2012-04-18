package com.eucalyptus.auth.policy;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.system.Ats;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class PolicySpec {
  
  public static final String VERSION = "Version";
  
  public static final String STATEMENT = "Statement";
  public static final String SID = "Sid";
  public static final String EFFECT = "Effect";
  public static final String ACTION = "Action";
  public static final String NOTACTION = "NotAction";
  public static final String RESOURCE = "Resource";
  public static final String NOTRESOURCE = "NotResource";
  public static final String CONDITION = "Condition";
    
  // Effect
  public static final Set<String> EFFECTS = Sets.newHashSet( );
  
  static {
    for ( EffectType effect : EffectType.values( ) ) {
      EFFECTS.add( effect.name( ) );
    }
  }
  
  // Vendor (AWS products)
  public static final String VENDOR_IAM = "iam";
  public static final String VENDOR_EC2 = "ec2";
  public static final String VENDOR_S3 = "s3";
  
  public static final Set<String> VENDORS = Sets.newHashSet( );
  
  static {
    VENDORS.add( VENDOR_IAM );
    VENDORS.add( VENDOR_EC2 );
    VENDORS.add( VENDOR_S3 );
  }
  
  public static final String ALL_ACTION = "*";
  
  // IAM actions, based on API version 2010-05-08
  public static final String IAM_ADDUSERTOGROUP = "addusertogroup";
  public static final String IAM_CREATEACCESSKEY = "createaccesskey";
  public static final String IAM_CREATEACCOUNTALIAS = "createaccountalias";
  public static final String IAM_CREATEGROUP = "creategroup";
  public static final String IAM_CREATELOGINPROFILE = "createloginprofile";
  public static final String IAM_CREATEUSER = "createuser";
  public static final String IAM_DEACTIVATEMFADEVICE = "deactivatemfadevice";
  public static final String IAM_DELETEACCESSKEY = "deleteaccesskey";
  public static final String IAM_DELETEACCOUNTALIAS = "deleteaccountalias";
  public static final String IAM_DELETEGROUP = "deletegroup";
  public static final String IAM_DELETEGROUPPOLICY = "deletegrouppolicy";
  public static final String IAM_DELETELOGINPROFILE = "deleteloginprofile";
  public static final String IAM_DELETESERVERCERTIFICATE = "deleteservercertificate";
  public static final String IAM_DELETESIGNINGCERTIFICATE = "deletesigningcertificate";
  public static final String IAM_DELETEUSER = "deleteuser";
  public static final String IAM_DELETEUSERPOLICY = "deleteuserpolicy";
  public static final String IAM_ENABLEMFADEVICE = "enablemfadevice";
  public static final String IAM_GETACCOUNTSUMMARY = "getaccountsummary";
  public static final String IAM_GETGROUP = "getgroup";
  public static final String IAM_GETGROUPPOLICY = "getgrouppolicy";
  public static final String IAM_GETLOGINPROFILE = "getloginprofile";
  public static final String IAM_GETSERVERCERTIFICATE = "getservercertificate";
  public static final String IAM_GETUSER = "getuser";
  public static final String IAM_GETUSERPOLICY = "getuserpolicy";
  public static final String IAM_LISTACCESSKEYS = "listaccesskeys";
  public static final String IAM_LISTACCOUNTALIASES = "listaccountaliases";
  public static final String IAM_LISTGROUPPOLICIES = "listgrouppolicies";
  public static final String IAM_LISTGROUPS = "listgroups";
  public static final String IAM_LISTGROUPSFORUSER = "listgroupsforuser";
  public static final String IAM_LISTMFADEVICES = "listmfadevices";
  public static final String IAM_LISTSERVERCERTIFICATES = "listservercertificates";
  public static final String IAM_LISTSIGNINGCERTIFICATES = "listsigningcertificates";
  public static final String IAM_LISTUSERPOLICIES = "listuserpolicies";
  public static final String IAM_LISTUSERS = "listusers";
  public static final String IAM_PUTGROUPPOLICY = "putgrouppolicy";
  public static final String IAM_PUTUSERPOLICY = "putuserpolicy";
  public static final String IAM_REMOVEUSERFROMGROUP = "removeuserfromgroup";
  public static final String IAM_RESYNCMFADEVICE = "resyncmfadevice";
  public static final String IAM_UPDATEACCESSKEY = "updateaccesskey";
  public static final String IAM_UPDATEGROUP = "updategroup";
  public static final String IAM_UPDATELOGINPROFILE = "updateloginprofile";
  public static final String IAM_UPDATESERVERCERTIFICATE = "updateservercertificate";
  public static final String IAM_UPDATESIGNINGCERTIFICATE = "updatesigningcertificate";
  public static final String IAM_UPDATEUSER = "updateuser";
  public static final String IAM_UPLOADSERVERCERTIFICATE = "uploadservercertificate";
  public static final String IAM_UPLOADSIGNINGCERTIFICATE = "uploadsigningcertificate";

  public static final Set<String> IAM_ACTIONS = Sets.newHashSet( );
  
  static {
    IAM_ACTIONS.add( IAM_ADDUSERTOGROUP );
    IAM_ACTIONS.add( IAM_CREATEACCESSKEY );
    IAM_ACTIONS.add( IAM_CREATEGROUP );
    IAM_ACTIONS.add( IAM_CREATELOGINPROFILE );
    IAM_ACTIONS.add( IAM_CREATEUSER );
    IAM_ACTIONS.add( IAM_DEACTIVATEMFADEVICE );
    IAM_ACTIONS.add( IAM_DELETEACCESSKEY );
    IAM_ACTIONS.add( IAM_DELETEGROUP );
    IAM_ACTIONS.add( IAM_DELETEGROUPPOLICY );
    IAM_ACTIONS.add( IAM_DELETELOGINPROFILE );
    IAM_ACTIONS.add( IAM_DELETESERVERCERTIFICATE );
    IAM_ACTIONS.add( IAM_DELETESIGNINGCERTIFICATE );
    IAM_ACTIONS.add( IAM_DELETEUSER );
    IAM_ACTIONS.add( IAM_DELETEUSERPOLICY );
    IAM_ACTIONS.add( IAM_ENABLEMFADEVICE );
    IAM_ACTIONS.add( IAM_GETGROUP );
    IAM_ACTIONS.add( IAM_GETGROUPPOLICY );
    IAM_ACTIONS.add( IAM_GETLOGINPROFILE );
    IAM_ACTIONS.add( IAM_GETSERVERCERTIFICATE );
    IAM_ACTIONS.add( IAM_GETUSER );
    IAM_ACTIONS.add( IAM_GETUSERPOLICY );
    IAM_ACTIONS.add( IAM_LISTACCESSKEYS );
    IAM_ACTIONS.add( IAM_LISTGROUPPOLICIES );
    IAM_ACTIONS.add( IAM_LISTGROUPS );
    IAM_ACTIONS.add( IAM_LISTGROUPSFORUSER );
    IAM_ACTIONS.add( IAM_LISTMFADEVICES );
    IAM_ACTIONS.add( IAM_LISTSERVERCERTIFICATES );
    IAM_ACTIONS.add( IAM_LISTSIGNINGCERTIFICATES );
    IAM_ACTIONS.add( IAM_LISTUSERPOLICIES );
    IAM_ACTIONS.add( IAM_LISTUSERS );
    IAM_ACTIONS.add( IAM_PUTGROUPPOLICY );
    IAM_ACTIONS.add( IAM_PUTUSERPOLICY );
    IAM_ACTIONS.add( IAM_REMOVEUSERFROMGROUP );
    IAM_ACTIONS.add( IAM_RESYNCMFADEVICE );
    IAM_ACTIONS.add( IAM_UPDATEACCESSKEY );
    IAM_ACTIONS.add( IAM_UPDATEGROUP );
    IAM_ACTIONS.add( IAM_UPDATELOGINPROFILE );
    IAM_ACTIONS.add( IAM_UPDATESERVERCERTIFICATE );
    IAM_ACTIONS.add( IAM_UPDATESIGNINGCERTIFICATE );
    IAM_ACTIONS.add( IAM_UPDATEUSER );
    IAM_ACTIONS.add( IAM_UPLOADSERVERCERTIFICATE );
    IAM_ACTIONS.add( IAM_UPLOADSIGNINGCERTIFICATE );
  }
  
  // EC2 actions, based on API version 2010-08-31
  public static final String EC2_ALLOCATEADDRESS = "allocateaddress";
  public static final String EC2_ASSOCIATEADDRESS = "associateaddress";
  public static final String EC2_ATTACHVOLUME = "attachvolume";
  public static final String EC2_AUTHORIZESECURITYGROUPINGRESS = "authorizesecuritygroupingress";
  public static final String EC2_BUNDLEINSTANCE = "bundleinstance";
  public static final String EC2_CANCELBUNDLETASK = "cancelbundletask";
  public static final String EC2_CANCELSPOTINSTANCEREQUESTS = "cancelspotinstancerequests";
  public static final String EC2_CONFIRMPRODUCTINSTANCE = "confirmproductinstance";
  public static final String EC2_CREATEIMAGE = "createimage";
  public static final String EC2_CREATEKEYPAIR = "createkeypair";
  public static final String EC2_CREATEPLACEMENTGROUP = "createplacementgroup";
  public static final String EC2_CREATESECURITYGROUP = "createsecuritygroup";
  public static final String EC2_CREATESNAPSHOT = "createsnapshot";
  public static final String EC2_CREATESPOTDATAFEEDSUBSCRIPTION = "createspotdatafeedsubscription";
  public static final String EC2_CREATETAGS = "createtags";
  public static final String EC2_CREATEVOLUME = "createvolume";
  public static final String EC2_DELETEKEYPAIR = "deletekeypair";
  public static final String EC2_DELETEPLACEMENTGROUP = "deleteplacementgroup";
  public static final String EC2_DELETESECURITYGROUP = "deletesecuritygroup";
  public static final String EC2_DELETESNAPSHOT = "deletesnapshot";
  public static final String EC2_DELETESPOTDATAFEEDSUBSCRIPTION = "deletespotdatafeedsubscription";
  public static final String EC2_DELETETAGS = "deletetags";
  public static final String EC2_DELETEVOLUME = "deletevolume";
  public static final String EC2_DEREGISTERIMAGE = "deregisterimage";
  public static final String EC2_DESCRIBEADDRESSES = "describeaddresses";
  public static final String EC2_DESCRIBEAVAILABILITYZONES = "describeavailabilityzones";
  public static final String EC2_DESCRIBEBUNDLETASKS = "describebundletasks";
  public static final String EC2_DESCRIBEIMAGEATTRIBUTE = "describeimageattribute";
  public static final String EC2_DESCRIBEIMAGES = "describeimages";
  public static final String EC2_DESCRIBEINSTANCEATTRIBUTE = "describeinstanceattribute";
  public static final String EC2_DESCRIBEINSTANCES = "describeinstances";
  public static final String EC2_DESCRIBEKEYPAIRS = "describekeypairs";
  public static final String EC2_DESCRIBEPLACEMENTGROUPS = "describeplacementgroups";
  public static final String EC2_DESCRIBEREGIONS = "describeregions";
  public static final String EC2_DESCRIBERESERVEDINSTANCES = "describereservedinstances";
  public static final String EC2_DESCRIBERESERVEDINSTANCESOFFERINGS = "describereservedinstancesofferings";
  public static final String EC2_DESCRIBESECURITYGROUPS = "describesecuritygroups";
  public static final String EC2_DESCRIBESNAPSHOTATTRIBUTE = "describesnapshotattribute";
  public static final String EC2_DESCRIBESNAPSHOTS = "describesnapshots";
  public static final String EC2_DESCRIBESPOTDATAFEEDSUBSCRIPTION = "describespotdatafeedsubscription";
  public static final String EC2_DESCRIBESPOTINSTANCEREQUESTS = "describespotinstancerequests";
  public static final String EC2_DESCRIBESPOTPRICEHISTORY = "describespotpricehistory";
  public static final String EC2_DESCRIBETAGS = "describetags";
  public static final String EC2_DESCRIBEVOLUMES = "describevolumes";
  public static final String EC2_DETACHVOLUME = "detachvolume";
  public static final String EC2_DISASSOCIATEADDRESS = "disassociateaddress";
  public static final String EC2_GETCONSOLEOUTPUT = "getconsoleoutput";
  public static final String EC2_GETPASSWORDDATA = "getpassworddata";
  public static final String EC2_IMPORTKEYPAIR = "importkeypair";
  public static final String EC2_MODIFYIMAGEATTRIBUTE = "modifyimageattribute";
  public static final String EC2_MODIFYINSTANCEATTRIBUTE = "modifyinstanceattribute";
  public static final String EC2_MODIFYSNAPSHOTATTRIBUTE = "modifysnapshotattribute";
  public static final String EC2_MONITORINSTANCES = "monitorinstances";
  public static final String EC2_PURCHASERESERVEDINSTANCESOFFERING = "purchasereservedinstancesoffering";
  public static final String EC2_REBOOTINSTANCES = "rebootinstances";
  public static final String EC2_REGISTERIMAGE = "registerimage";
  public static final String EC2_RELEASEADDRESS = "releaseaddress";
  public static final String EC2_REQUESTSPOTINSTANCES = "requestspotinstances";
  public static final String EC2_RESETIMAGEATTRIBUTE = "resetimageattribute";
  public static final String EC2_RESETINSTANCEATTRIBUTE = "resetinstanceattribute";
  public static final String EC2_RESETSNAPSHOTATTRIBUTE = "resetsnapshotattribute";
  public static final String EC2_REVOKESECURITYGROUPINGRESS = "revokesecuritygroupingress";
  public static final String EC2_RUNINSTANCES = "runinstances";
  public static final String EC2_STARTINSTANCES = "startinstances";
  public static final String EC2_STOPINSTANCES = "stopinstances";
  public static final String EC2_TERMINATEINSTANCES = "terminateinstances";
  public static final String EC2_UNMONITORINSTANCES = "unmonitorinstances";

  public static final Set<String> EC2_ACTIONS = Sets.newHashSet( );
  
  static {
    EC2_ACTIONS.add( EC2_ALLOCATEADDRESS );
    EC2_ACTIONS.add( EC2_ASSOCIATEADDRESS );
    EC2_ACTIONS.add( EC2_ATTACHVOLUME );
    EC2_ACTIONS.add( EC2_AUTHORIZESECURITYGROUPINGRESS );
    EC2_ACTIONS.add( EC2_BUNDLEINSTANCE );
    EC2_ACTIONS.add( EC2_CANCELBUNDLETASK );
    EC2_ACTIONS.add( EC2_CANCELSPOTINSTANCEREQUESTS );
    EC2_ACTIONS.add( EC2_CONFIRMPRODUCTINSTANCE );
    EC2_ACTIONS.add( EC2_CREATEIMAGE );
    EC2_ACTIONS.add( EC2_CREATEKEYPAIR );
    EC2_ACTIONS.add( EC2_CREATEPLACEMENTGROUP );
    EC2_ACTIONS.add( EC2_CREATESECURITYGROUP );
    EC2_ACTIONS.add( EC2_CREATESNAPSHOT );
    EC2_ACTIONS.add( EC2_CREATESPOTDATAFEEDSUBSCRIPTION );
    EC2_ACTIONS.add( EC2_CREATETAGS );
    EC2_ACTIONS.add( EC2_CREATEVOLUME );
    EC2_ACTIONS.add( EC2_DELETEKEYPAIR );
    EC2_ACTIONS.add( EC2_DELETEPLACEMENTGROUP );
    EC2_ACTIONS.add( EC2_DELETESECURITYGROUP );
    EC2_ACTIONS.add( EC2_DELETESNAPSHOT );
    EC2_ACTIONS.add( EC2_DELETESPOTDATAFEEDSUBSCRIPTION );
    EC2_ACTIONS.add( EC2_DELETETAGS );
    EC2_ACTIONS.add( EC2_DELETEVOLUME );
    EC2_ACTIONS.add( EC2_DEREGISTERIMAGE );
    EC2_ACTIONS.add( EC2_DESCRIBEADDRESSES );
    EC2_ACTIONS.add( EC2_DESCRIBEAVAILABILITYZONES );
    EC2_ACTIONS.add( EC2_DESCRIBEBUNDLETASKS );
    EC2_ACTIONS.add( EC2_DESCRIBEIMAGEATTRIBUTE );
    EC2_ACTIONS.add( EC2_DESCRIBEIMAGES );
    EC2_ACTIONS.add( EC2_DESCRIBEINSTANCEATTRIBUTE );
    EC2_ACTIONS.add( EC2_DESCRIBEINSTANCES );
    EC2_ACTIONS.add( EC2_DESCRIBEKEYPAIRS );
    EC2_ACTIONS.add( EC2_DESCRIBEPLACEMENTGROUPS );
    EC2_ACTIONS.add( EC2_DESCRIBEREGIONS );
    EC2_ACTIONS.add( EC2_DESCRIBERESERVEDINSTANCES );
    EC2_ACTIONS.add( EC2_DESCRIBERESERVEDINSTANCESOFFERINGS );
    EC2_ACTIONS.add( EC2_DESCRIBESECURITYGROUPS );
    EC2_ACTIONS.add( EC2_DESCRIBESNAPSHOTATTRIBUTE );
    EC2_ACTIONS.add( EC2_DESCRIBESNAPSHOTS );
    EC2_ACTIONS.add( EC2_DESCRIBESPOTDATAFEEDSUBSCRIPTION );
    EC2_ACTIONS.add( EC2_DESCRIBESPOTINSTANCEREQUESTS );
    EC2_ACTIONS.add( EC2_DESCRIBESPOTPRICEHISTORY );
    EC2_ACTIONS.add( EC2_DESCRIBETAGS );
    EC2_ACTIONS.add( EC2_DESCRIBEVOLUMES );
    EC2_ACTIONS.add( EC2_DETACHVOLUME );
    EC2_ACTIONS.add( EC2_DISASSOCIATEADDRESS );
    EC2_ACTIONS.add( EC2_GETCONSOLEOUTPUT );
    EC2_ACTIONS.add( EC2_GETPASSWORDDATA );
    EC2_ACTIONS.add( EC2_IMPORTKEYPAIR );
    EC2_ACTIONS.add( EC2_MODIFYIMAGEATTRIBUTE );
    EC2_ACTIONS.add( EC2_MODIFYINSTANCEATTRIBUTE );
    EC2_ACTIONS.add( EC2_MODIFYSNAPSHOTATTRIBUTE );
    EC2_ACTIONS.add( EC2_MONITORINSTANCES );
    EC2_ACTIONS.add( EC2_PURCHASERESERVEDINSTANCESOFFERING );
    EC2_ACTIONS.add( EC2_REBOOTINSTANCES );
    EC2_ACTIONS.add( EC2_REGISTERIMAGE );
    EC2_ACTIONS.add( EC2_RELEASEADDRESS );
    EC2_ACTIONS.add( EC2_REQUESTSPOTINSTANCES );
    EC2_ACTIONS.add( EC2_RESETIMAGEATTRIBUTE );
    EC2_ACTIONS.add( EC2_RESETINSTANCEATTRIBUTE );
    EC2_ACTIONS.add( EC2_RESETSNAPSHOTATTRIBUTE );
    EC2_ACTIONS.add( EC2_REVOKESECURITYGROUPINGRESS );
    EC2_ACTIONS.add( EC2_RUNINSTANCES );
    EC2_ACTIONS.add( EC2_STARTINSTANCES );
    EC2_ACTIONS.add( EC2_STOPINSTANCES );
    EC2_ACTIONS.add( EC2_TERMINATEINSTANCES );
    EC2_ACTIONS.add( EC2_UNMONITORINSTANCES );
  }
  
  // S3 actions, based on IAM User Guide version 2010-05-08
  public static final String S3_GETOBJECT = "getobject";
  public static final String S3_GETOBJECTVERSION = "getobjectversion";
  public static final String S3_PUTOBJECT = "putobject";
  public static final String S3_GETOBJECTACL = "getobjectacl";
  public static final String S3_GETOBJECTVERSIONACL = "getobjectversionacl";
  public static final String S3_PUTOBJECTACL = "putobjectacl";
  public static final String S3_PUTOBJECTACLVERSION = "putobjectaclversion";
  public static final String S3_DELETEOBJECT = "deleteobject";
  public static final String S3_DELETEOBJECTVERSION = "deleteobjectversion";
  public static final String S3_CREATEBUCKET = "createbucket";
  public static final String S3_DELETEBUCKET = "deletebucket";
  public static final String S3_LISTBUCKET = "listbucket";
  public static final String S3_LISTBUCKETVERSIONS = "listbucketversions";
  public static final String S3_LISTALLMYBUCKETS = "listallmybuckets";
  public static final String S3_GETBUCKETACL = "getbucketacl";
  public static final String S3_PUTBUCKETACL = "putbucketacl";
  public static final String S3_GETBUCKETVERSIONING = "getbucketversioning";
  public static final String S3_PUTBUCKETVERSIONING = "putbucketversioning";
  public static final String S3_GETBUCKETREQUESTPAYMENT = "getbucketrequestpayment";
  public static final String S3_PUTBUCKETREQUESTPAYMENT = "putbucketrequestpayment";
  public static final String S3_GETBUCKETLOCATION = "getbucketlocation";

  public static final Set<String> S3_ACTIONS = Sets.newHashSet( );
  
  static {
    S3_ACTIONS.add( S3_GETOBJECT );
    S3_ACTIONS.add( S3_GETOBJECTVERSION );
    S3_ACTIONS.add( S3_PUTOBJECT );
    S3_ACTIONS.add( S3_GETOBJECTACL );
    S3_ACTIONS.add( S3_GETOBJECTVERSIONACL );
    S3_ACTIONS.add( S3_PUTOBJECTACL );
    S3_ACTIONS.add( S3_PUTOBJECTACLVERSION );
    S3_ACTIONS.add( S3_DELETEOBJECT );
    S3_ACTIONS.add( S3_DELETEOBJECTVERSION );
    S3_ACTIONS.add( S3_CREATEBUCKET );
    S3_ACTIONS.add( S3_DELETEBUCKET );
    S3_ACTIONS.add( S3_LISTBUCKET );
    S3_ACTIONS.add( S3_LISTBUCKETVERSIONS );
    S3_ACTIONS.add( S3_LISTALLMYBUCKETS );
    S3_ACTIONS.add( S3_GETBUCKETACL );
    S3_ACTIONS.add( S3_PUTBUCKETACL );
    S3_ACTIONS.add( S3_GETBUCKETVERSIONING );
    S3_ACTIONS.add( S3_PUTBUCKETVERSIONING );
    S3_ACTIONS.add( S3_GETBUCKETREQUESTPAYMENT );
    S3_ACTIONS.add( S3_PUTBUCKETREQUESTPAYMENT );
    S3_ACTIONS.add( S3_GETBUCKETLOCATION );
  }
  
  // Map vendor to actions
  public static final Map<String, Set<String>> VENDOR_ACTIONS = Maps.newHashMap( );
  
  static {
    VENDOR_ACTIONS.put( VENDOR_IAM, IAM_ACTIONS );
    VENDOR_ACTIONS.put( VENDOR_EC2, EC2_ACTIONS );
    VENDOR_ACTIONS.put( VENDOR_S3, S3_ACTIONS );
  }
  
  // Action syntax
  public static final Pattern ACTION_PATTERN = Pattern.compile( "\\*|(?:(" + VENDOR_IAM + "|" + VENDOR_EC2 + "|" + VENDOR_S3 + "):(\\S+))" );
  
  // Wildcard
  public static final String ALL_RESOURCE = "*";
  
  // IAM resource types
  public static final String IAM_RESOURCE_GROUP = "group";
  public static final String IAM_RESOURCE_USER = "user";
  
  // EC2 resource types, extension to AWS IAM
  public static final String EC2_RESOURCE_IMAGE = "image";
  public static final String EC2_RESOURCE_SECURITYGROUP = "securitygroup";
  public static final String EC2_RESOURCE_ADDRESS = "address";
  public static final String EC2_RESOURCE_AVAILABILITYZONE = "availabilityzone";
  public static final String EC2_RESOURCE_INSTANCE = "instance";
  public static final String EC2_RESOURCE_KEYPAIR = "keypair";
  public static final String EC2_RESOURCE_VOLUME = "volume";
  public static final String EC2_RESOURCE_SNAPSHOT = "snapshot";
  public static final String EC2_RESOURCE_VMTYPE = "vmtype";
  
  public static final Set<String> EC2_RESOURCES = Sets.newHashSet( );
  
  static {
    EC2_RESOURCES.add( EC2_RESOURCE_IMAGE );
    EC2_RESOURCES.add( EC2_RESOURCE_SECURITYGROUP );
    EC2_RESOURCES.add( EC2_RESOURCE_ADDRESS );
    EC2_RESOURCES.add( EC2_RESOURCE_AVAILABILITYZONE );
    EC2_RESOURCES.add( EC2_RESOURCE_INSTANCE );
    EC2_RESOURCES.add( EC2_RESOURCE_KEYPAIR );
    EC2_RESOURCES.add( EC2_RESOURCE_VOLUME );
    EC2_RESOURCES.add( EC2_RESOURCE_SNAPSHOT );
    EC2_RESOURCES.add( EC2_RESOURCE_VMTYPE );
  }

  public static final Pattern IPV4_ADDRESS_RANGE_PATTERN = Pattern.compile( "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:-(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))?" );
  
  // S3 resource types
  public static final String S3_RESOURCE_BUCKET = "bucket";
  public static final String S3_RESOURCE_OBJECT = "object";
  
  public static String qualifiedName( String vendor, String name ) {
    return vendor + ":" + name;
  }
  
  /**
   * Map request to policy language's action string.
   * 
   * @param request The request message
   * @return The IAM ARN action string.
   */
  public static String requestToAction( BaseMessage request ) {
    if ( request != null ) {
      PolicyAction action = Ats.from( request ).get( PolicyAction.class );
      if ( action != null ) {
        return action.action( );
      }
    }
    return null;
  }
  
  public static String objectFullName( String bucketName, String objectKey ) {
    if ( objectKey.startsWith( "/" ) ) {
      return bucketName + objectKey;
    } else {
      return bucketName + "/" + objectKey;
    }
  }
  
}
