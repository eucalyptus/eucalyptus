/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
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
