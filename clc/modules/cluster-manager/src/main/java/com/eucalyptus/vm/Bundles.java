/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.vm;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.AsyncRequests;
import edu.ucsb.eucalyptus.msgs.CreateBucketType;
import edu.ucsb.eucalyptus.msgs.DeleteBucketType;

public class Bundles {
  public static BundleTask create( VmInstance v, String bucket, String prefix, String policy ) {
    verifyPolicy( policy, bucket );
    verifyBucket( bucket );
    verifyPrefix( prefix );
    return new BundleTask( v.getInstanceId( ).replaceFirst( "i-", "bun-" ), v.getInstanceId( ), bucket, prefix );
  }
  
  private static void verifyPolicy( String policy, String bucketName ) {
    /**
     * GRZE:NOTE: why is there S3 specific stuff here? this is the ec2 implementation. policy check
     * must happen in walrus not here.
     **/
    // check if the policy is not user-generated one
    // "expiration": "2011-07-01T16:52:13","conditions": [{"bucket": "windowsbundle" },{"acl": "ec2-bundle-read" },["starts-with", "$key", "prefix"
    int idxOpenBracket = policy.indexOf( "{" );
    int idxClosingBracket = policy.lastIndexOf( "}" );
    if ( idxOpenBracket < 0 || idxClosingBracket < 0 || idxOpenBracket >= idxClosingBracket )
      throw new RuntimeException( "Custom policy is not acceptable for bundle instance" );
    
    String bucketAndAcl = policy.substring( idxOpenBracket, idxClosingBracket - idxOpenBracket );
    if ( !bucketAndAcl.contains( bucketName ) )
      throw new RuntimeException( "Custom policy is not acceptable for bundle instance" );
    if ( !bucketAndAcl.contains( "ec2-bundle-read" ) )
      throw new RuntimeException( "Custom policy is not acceptable for bundle instance" );
    
  }
  
  private static void verifyPrefix( String prefix ) {
    // check if the prefix name starts with "windows"
    if ( !prefix.startsWith( "windows" ) )
      /**
       * GRZE:NOTE: bundling is /not/ restricted to windows
       * only in general.
       * what is it doing here? should be set in the manifest by the NC.
       **/
      throw new RuntimeException( "Prefix name should start with 'windows'" );
  }
  
  /**
   * @param bucket
   */
  private static void verifyBucket( final String bucketName ) {
    final Context ctx = Contexts.lookup( );
    CreateBucketType createBucket = new CreateBucketType( ) {
      {
        setAccessKeyID( ctx.getUserFullName( ).getUserId( ) );
        setBucket( bucketName );
      }
    }.regardingUserRequest( ctx.getRequest( ) );
    DeleteBucketType deleteBucket = new DeleteBucketType( ) {
      {
        setAccessKeyID( ctx.getUserFullName( ).getUserId( ) );
        setBucket( bucketName );
      }
    }.regardingUserRequest( ctx.getRequest( ) );
    ServiceConfiguration walrusConfig = ServiceConfigurations.enabledServices( Walrus.class ).iterator( ).next( );
    if ( walrusConfig != null ) {
      try {
        AsyncRequests.sendSync( walrusConfig, createBucket );
        AsyncRequests.sendSync( walrusConfig, deleteBucket );
      } catch ( Exception ex ) {
        Logs.extreme( ).error( ex );
      }
    } else {
      throw new RuntimeException( "Failed to lookup active service configuration for walrus." );
    }
    
  }
}
