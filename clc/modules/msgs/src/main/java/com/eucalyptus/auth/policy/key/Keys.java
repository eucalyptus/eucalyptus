/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy.key;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * IAM condition key constants.
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
  public static final String IAM_QUOTA_ROLE_NUMBER = "iam:quota-rolenumber";
  public static final String IAM_QUOTA_INSTANCE_PROFILE_NUMBER = "iam:quota-instanceprofilenumber";

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
