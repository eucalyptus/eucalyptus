/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
                           "(?:(" + PolicySpec.VENDOR_IAM + ")::([0-9]{12}):(user|group|role|instance-profile)((?:/[^/\\s]+)+))" +
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
