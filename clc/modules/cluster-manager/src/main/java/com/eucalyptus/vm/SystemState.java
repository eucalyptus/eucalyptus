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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.vm;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.cloud.util.Resource.SetReference;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;

public class SystemState {
  
  public static Logger LOG = Logger.getLogger( SystemState.class );
  
  public static void handle( final VmDescribeResponseType request ) {
    VmInstances.flushBuried( );
    final String originCluster = request.getOriginCluster( );
    for ( final VmInfo runVm : request.getVms( ) ) {
      SystemState.updateVmInstance( originCluster, runVm );
    }
    final List<String> unreportedVms = Lists.transform( VmInstances.listValues( ), new Function<VmInstance, String>( ) {
      
      @Override
      public String apply( final VmInstance input ) {
        return input.getInstanceId( );
      }
    } );
    final List<String> runningVmIds = Lists.transform( request.getVms( ), new Function<VmInfo, String>( ) {
      @Override
      public String apply( final VmInfo arg0 ) {
        final String vmId = arg0.getImageId( );
        unreportedVms.remove( vmId );
        return vmId;
      }
    } );
    for ( final String vmId : unreportedVms ) {
      try {
        final VmInstance vm = VmInstances.lookup( vmId );
        if ( vm.getSplitTime( ) > VmInstances.SHUT_DOWN_TIME ) {
          vm.setState( VmState.TERMINATED, Reason.EXPIRED );
        }
      } catch ( final NoSuchElementException e ) {}
    }
  }
  
  private static void updateVmInstance( final String originCluster, final VmInfo runVm ) {
    final VmState state = VmState.Mapper.get( runVm.getStateName( ) );
    
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      try {
        final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, runVm.getInstanceId( ) ) );
        if ( !VmState.BURIED.equals( vm.getRuntimeState( ) ) && ( vm.getSplitTime( ) > VmInstances.BURY_TIME ) ) {
          vm.setState( VmState.BURIED, Reason.BURIED );
        }
        vm.doUpdate( ).apply( runVm );
      } catch ( final Exception ex ) {
        if ( ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) ) ) {
          SystemState.restoreInstance( originCluster, runVm );
        }
      }
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
  }
  
  public static ArrayList<String> getAncestors( final BaseMessage parentMsg, final String manifestPath ) {
    final ArrayList<String> ancestorIds = Lists.newArrayList( );
    try {
      final String[] imagePathParts = manifestPath.split( "/" );
      final String bucketName = imagePathParts[0];
      final String objectName = imagePathParts[1];
      GetObjectResponseType reply = null;
      try {
        final GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true ).regardingUserRequest( parentMsg );
        
        reply = ( GetObjectResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( "walrus" ) ).send( msg );
      } catch ( final Exception e ) {
        throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
      }
      Document inputSource = null;
      try {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( );
        inputSource = builder.parse( new ByteArrayInputStream( Hashes.base64decode( reply.getBase64Data( ) ).getBytes( ) ) );
      } catch ( final Exception e ) {
        throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
      }
      
      final XPath xpath = XPathFactory.newInstance( ).newXPath( );
      NodeList ancestors = null;
      try {
        ancestors = ( NodeList ) xpath.evaluate( "/manifest/image/ancestry/ancestor_ami_id/text()", inputSource, XPathConstants.NODESET );
        if ( ancestors == null ) return ancestorIds;
        for ( int i = 0; i < ancestors.getLength( ); i++ ) {
          for ( final String ancestorId : ancestors.item( i ).getNodeValue( ).split( "," ) ) {
            ancestorIds.add( ancestorId );
          }
        }
      } catch ( final XPathExpressionException e ) {
        LOG.error( e, e );
      }
    } catch ( final EucalyptusCloudException e ) {
      LOG.error( e, e );
    } catch ( final DOMException e ) {
      LOG.error( e, e );
    }
    return ancestorIds;
  }
  
  private static void restoreInstance( final String cluster, final VmInfo runVm ) {

  }
  
  public static ArrayList<ReservationInfoType> handle( final DescribeInstancesType request ) throws Exception {
    final Context ctx = Contexts.lookup( );
    final boolean isAdmin = ctx.hasAdministrativePrivileges( );
    final ArrayList<String> instancesSet = request.getInstancesSet( );
    final String action = PolicySpec.requestToAction( request );
    final User requestUser = ctx.getUser( );
    final Map<String, ReservationInfoType> rsvMap = new HashMap<String, ReservationInfoType>( );
    final EntityTransaction db = Entities.get( VmInstance.class );
    for ( final VmInstance v : VmInstances.listValues( ) ) {
      if ( !VmState.STOPPED.equals( v.getState( ) ) && ( v.getState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) ) {
        final long time = ( System.currentTimeMillis( ) - v.getLastUpdateTimestamp( ).getTime( ) );
        if ( time > VmInstances.SHUT_DOWN_TIME ) {
          v.setState( VmState.TERMINATED, Reason.EXPIRED );
          continue;
        } else if ( time > VmInstances.SHUT_DOWN_TIME ) {
          v.setState( VmState.BURIED, Reason.BURIED );
        }
      }
      Account instanceAccount = null;
      try {
        instanceAccount = Accounts.lookupUserById( v.getOwner( ).getUniqueId( ) ).getAccount( );
      } catch ( final AuthException e ) {
        throw new EucalyptusCloudException( e );
      }
      if ( ( !isAdmin &&
           !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, v.getInstanceId( ), instanceAccount, action, requestUser ) )
           || ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) ) {
        continue;
      }
      if ( rsvMap.get( v.getReservationId( ) ) == null ) {
        final ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwner( ).getNamespace( ), v.getNetworkNames( ) );
        rsvMap.put( reservation.getReservationId( ), reservation );
      }
      rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( VmInstance.Transform.INSTANCE.apply( v ) );
    }
    if ( isAdmin ) {
      for ( final VmInstance v : VmInstances.listDisabledValues( ) ) {
        if ( VmState.BURIED.equals( v.getState( ) ) ) continue;
        if ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) continue;
        if ( rsvMap.get( v.getReservationId( ) ) == null ) {
          final ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwner( ), v.getNetworkNames( ) );
          rsvMap.put( reservation.getReservationId( ), reservation );
        }
        rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( VmInstance.Transform.INSTANCE.apply( v ) );
      }
    }
    return new ArrayList<ReservationInfoType>( rsvMap.values( ) );
  }
  
}
