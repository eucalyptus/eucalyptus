package edu.ucsb.eucalyptus.cloud.exceptions;

/*
 * as documented at:
 * http://docs.amazonwebservices.com/AWSEC2/2008-08-08/DeveloperGuide/index.html?api-error-codes.html
 */
public class ExceptionList {
  public static String ERR_ADDRESS_LIMIT_EXCEEDED = "AddressLimitExceeded";
  public static String ERR_ATTACHMENT_LIMIT_EXCEEDED = "AttachmentLimitExceeded";
  public static String ERR_AUTH_FAILURE = "AuthFailure";
  public static String ERR_INCORRECT_STATE = "IncorrectState";
  public static String ERR_INSTANCE_LIMIT_EXCEEDED = "InstanceLimitExceeded";
  public static String ERR_INVALID_AMI_ATTRIBUTE_ITEM_VALUE = "InvalidAMIAttributeItemValue";
  public static String ERR_INVALID_AMIID_MALFORMED = "InvalidAMIID.Malformed";
  public static String ERR_INVALID_AMIID_NOTFOUND = "InvalidAMIID.NotFound";
  public static String ERR_INVALID_AMIID_UNAVAILABLE = "InvalidAMIID.Unavailable";
  public static String ERR_INVALID_ATTACHMENT_NOTFOUND = "InvalidAttachment.NotFound";
  public static String ERR_INVALID_DEVICE_INUSE = "InvalidDevice.InUse";
  public static String ERR_INVALID_INSTANCEID_MALFORMED = "InvalidInstanceID.Malformed";
  public static String ERR_INVALID_INSTANCEID_NOTFOUND = "InvalidInstanceID.NotFound";
  public static String ERR_INVALID_KEYPAIR_NOTFOUND = "InvalidKeyPair.NotFound";
  public static String ERR_INVALID_KEYPAIR_DUPLICATE = "InvalidKeyPair.Duplicate";
  public static String ERR_INVALID_GROUP_NOTFOUND = "InvalidGroup.NotFound";
  public static String ERR_INVALID_GROUP_DUPLICATE = "InvalidGroup.Duplicate";
  public static String ERR_INVALID_GROUP_INUSE = "InvalidGroup.InUse";
  public static String ERR_INVALID_GROUP_RESERVED = "InvalidGroup.Reserved";
  public static String ERR_INVALID_MANIFEST = "InvalidManifest";
  public static String ERR_INVALID_PARAMETER_VALUE = "InvalidParameterValue";
  public static String ERR_INVALID_PERMISSION_DUPLICATE = "InvalidPermission.Duplicate";
  public static String ERR_INVALID_PERMISSION_MALFORMED = "InvalidPermission.Malformed";
  public static String ERR_INVALID_RESERVATIONID_MALFORMED = "InvalidReservationID.Malformed";
  public static String ERR_INVALID_RESERVATIONID_NOTFOUND = "InvalidReservationID.NotFound";
  public static String ERR_INVALID_PARAMETERCOMBINATION = "InvalidParameterCombination";
  public static String ERR_INVALID_SNAPSHOTID_MALFORMED = "InvalidSnapshotID.Malformed";
  public static String ERR_INVALID_SNAPSHOTID_NOTFOUND = "InvalidSnapshotID.NotFound";
  public static String ERR_INVALID_USERID_MALFORMED = "InvalidUserID.Malformed";
  public static String ERR_INVALID_VOLUMEID_MALFORMED = "InvalidVolumeID.Malformed";
  public static String ERR_INVALID_VOLUMEID_NOT_FOUND = "InvalidVolumeID.NotFound";
  public static String ERR_INVALID_VOLUMEID_DUPLICATE = "InvalidVolumeID.Duplicate";
  public static String ERR_INVALID_VOLUMEID_ZONE_MISMATCH = "InvalidVolumeID.ZoneMismatch";
  public static String ERR_INVALID_ZONE_NOT_FOUND = "InvalidZone.NotFound";
  public static String ERR_NON_EBS_INSTANCE = "NonEBSInstance";
  public static String ERR_PENDING_SNAPSHOT_LIMIT_EXCEEDED = "PendingSnapshotLimitExceeded";
  public static String ERR_SNAPSHOT_LIMIT_EXCEEDED = "SnapshotLimitExceeded";
  public static String ERR_UNKNOWN_PARAMETER = "UnknownParameter";
  public static String ERR_VOLUME_LIMIT_EXCEEDED = "VolumeLimitExceeded";
  public static String ERR_SYS_INTERNAL_ERROR = "InternalError";
  public static String ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY = "InsufficientAddressCapacity";
  public static String ERR_SYS_INSUFFICIENT_INSTANCE_CAPACITY = "InsufficientInstanceCapacity";
  public static String ERR_SYS_UNAVAILABLE = "Unavailable";
}
