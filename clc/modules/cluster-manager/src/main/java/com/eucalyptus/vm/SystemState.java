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
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.NetworkAlreadyExistsException;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.component.Components;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@ConfigurableClass( root = "vmstate", description = "Parameters controlling the lifecycle of virtual machines." )
public class SystemState {
  
  public static Logger  LOG            = Logger.getLogger( SystemState.class );
  @ConfigurableField( description = "Amount of time (in milliseconds) that a terminated VM will continue to be reported.", initial = "" + 60 * 60 * 1000 )
  public static Integer BURY_TIME      = -1;
  @ConfigurableField( description = "Amount of time (in milliseconds) before a VM which is not reported by a cluster will be marked as terminated.", initial = "" + 10 * 60 * 1000 )
  public static Integer SHUT_DOWN_TIME = -1;
  
  public enum Reason {
    NORMAL( "" ),
    EXPIRED( "Instance expired after not being reported for %s ms.", SystemState.SHUT_DOWN_TIME ),
    FAILED( "The instance failed to start on the NC." ),
    USER_TERMINATED( "User initiated terminate." ),
    USER_STOPPED( "User initiated stop." ),
    BURIED( "Instance buried after timeout of %s ms.", SystemState.BURY_TIME ),
    APPEND( "" );
    private String   message;
    private Object[] args;
    
    Reason( String message, Object... args ) {
      this.message = message;
      this.args = args;
    }
    
    @Override
    public String toString( ) {
      return String.format( this.message.toString( ), this.args );
    }
    
  }
  
  public static void handle( VmDescribeResponseType request ) {
    VmInstances.flushBuried( );
    String originCluster = request.getOriginCluster( );
    for ( VmInfo runVm : request.getVms( ) ) {
      SystemState.updateVmInstance( originCluster, runVm );
    }
    final Set<String> unreportedVms = VmInstances.getInstance( ).getKeys( );
    final List<String> runningVmIds = Lists.transform( request.getVms( ), new Function<VmInfo, String>( ) {
      @Override
      public String apply( VmInfo arg0 ) {
        String vmId = arg0.getImageId( );
        unreportedVms.remove( vmId );
        return vmId;
      }
    } );
    for ( String vmId : unreportedVms ) {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( vmId );
        if ( vm.getSplitTime( ) > SHUT_DOWN_TIME && !VmState.STOPPED.equals( vm.getState( ) ) && !VmState.STOPPING.equals( vm.getState( ) ) ) {
          vm.setState( VmState.TERMINATED, Reason.EXPIRED );
        }
      } catch ( NoSuchElementException e ) {}
    }
  }
  
  private static void updateVmInstance( final String originCluster, final VmInfo runVm ) {
    VmState state = VmState.Mapper.get( runVm.getStateName( ) );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( runVm.getInstanceId( ) );
    } catch ( NoSuchElementException e ) {
      try {
        vm = VmInstances.getInstance( ).lookupDisabled( runVm.getInstanceId( ) );
        if ( !VmState.BURIED.equals( vm.getState( ) ) && vm.getSplitTime( ) > BURY_TIME ) {
          vm.setState( VmState.BURIED, Reason.BURIED );
        }
        return;
      } catch ( NoSuchElementException e1 ) {
        if ( ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) ) ) {
          SystemState.restoreInstance( originCluster, runVm );
        }
        return;
      }
    }
    long splitTime = vm.getSplitTime( );
    VmState oldState = vm.getState( );
    vm.setServiceTag( runVm.getServiceTag( ) );
    vm.setPlatform( runVm.getPlatform( ) );
    vm.setBundleTaskState( runVm.getBundleTaskStateName( ) );
    
    if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) && splitTime > SHUT_DOWN_TIME ) {
      vm.setState( VmState.TERMINATED, Reason.EXPIRED );
    } else if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) && VmState.SHUTTING_DOWN.equals( VmState.Mapper.get( runVm.getStateName( ) ) ) ) {
      vm.setState( VmState.TERMINATED, Reason.APPEND, "DONE" );
    } else if ( ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) )
                && ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) ) {
      if ( !VmInstance.DEFAULT_IP.equals( runVm.getNetParams( ).getIpAddress( ) ) ) {
        vm.updateAddresses( runVm.getNetParams( ).getIpAddress( ), runVm.getNetParams( ).getIgnoredPublicIp( ) );
      }
      vm.setState( VmState.Mapper.get( runVm.getStateName( ) ), Reason.APPEND, "UPDATE" );
      vm.updateNetworkIndex( runVm.getNetParams( ).getNetworkIndex( ) );
      vm.updateVolumeAttachments( runVm.getVolumes( ) );
      try {
        Network network = Networks.getInstance( ).lookup( runVm.getOwnerId( ) + "-" + runVm.getGroupNames( ).get( 0 ) );
        network.extantNetworkIndex( vm.getClusterName( ), vm.getNetworkIndex( ) );
      } catch ( Exception e ) {}
    }
  }
  
  public static ArrayList<String> getAncestors( BaseMessage parentMsg, String manifestPath ) {
    ArrayList<String> ancestorIds = Lists.newArrayList( );
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      String bucketName = imagePathParts[0];
      String objectName = imagePathParts[1];
      GetObjectResponseType reply = null;
      try {
        GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true ).regardingUserRequest( parentMsg );
        
        reply = ( GetObjectResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( "walrus" ) ).send( msg );
      } catch ( Exception e ) {
        throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
      }
      Document inputSource = null;
      try {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( );
        inputSource = builder.parse( new ByteArrayInputStream( Hashes.base64decode( reply.getBase64Data( ) ).getBytes( ) ) );
      } catch ( Exception e ) {
        throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
      }
      
      XPath xpath = XPathFactory.newInstance( ).newXPath( );
      NodeList ancestors = null;
      try {
        ancestors = ( NodeList ) xpath.evaluate( "/manifest/image/ancestry/ancestor_ami_id/text()", inputSource, XPathConstants.NODESET );
        if ( ancestors == null ) return ancestorIds;
        for ( int i = 0; i < ancestors.getLength( ); i++ ) {
          for ( String ancestorId : ancestors.item( i ).getNodeValue( ).split( "," ) ) {
            ancestorIds.add( ancestorId );
          }
        }
      } catch ( XPathExpressionException e ) {
        LOG.error( e, e );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
    } catch ( DOMException e ) {
      LOG.error( e, e );
    }
    return ancestorIds;
  }
  
  private static void restoreInstance( final String cluster, final VmInfo runVm ) {
    try {
      String instanceUuid = runVm.getUuid( );
      String instanceId = runVm.getInstanceId( );
      String reservationId = runVm.getReservationId( );
      UserFullName ownerId = UserFullName.getInstance( runVm.getOwnerId( ) );
      String placement = cluster;
      byte[] userData = new byte[0];
      if ( runVm.getUserData( ) != null && runVm.getUserData( ).length( ) > 1 ) {
        userData = Base64.decode( runVm.getUserData( ) );
      }
      Integer launchIndex = 0;
      try {
        launchIndex = Integer.parseInt( runVm.getLaunchIndex( ) );
      } catch ( NumberFormatException e ) {}
      //ASAP: FIXME: GRZE: HANDLING OF PRODUCT CODES AND ANCESTOR IDs
      ImageInfo img = Transactions.one( Images.exampleMachineWithImageId( runVm.getInstanceType( ).lookupRoot( ).getId( ) ), new Callback<ImageInfo>( ) {
        
        @Override
        public void fire( ImageInfo t ) {}
      } );
      SshKeyPair key = null;
      if ( runVm.getKeyValue( ) != null || !"".equals( runVm.getKeyValue( ) ) ) {
        try {
          SshKeyPair searchKey = EntityWrapper.get( SshKeyPair.class ).lookupAndClose( new SshKeyPair( ownerId ) {
            {
              setPublicKey( runVm.getKeyValue( ) );
            }
          } );
        } catch ( Exception e ) {
          key = KeyPairs.noKey( );
        }
      } else {
        key = KeyPairs.noKey( );
      }
      VmType vmType = VmTypes.getVmType( runVm.getInstanceType( ).getName( ) );
      List<Network> networks = new ArrayList<Network>( );
      
      for ( String netName : runVm.getGroupNames( ) ) {
        Network notwork = null;
        try {
          notwork = Networks.getInstance( ).lookup( runVm.getOwnerId( ) + "-" + netName );
          networks.add( notwork );
          try {
            NetworkToken netToken = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getState( ).extantAllocation( runVm.getOwnerId( ), netName,
                                                                                                                          notwork.getUuid( ),
                                                                                                                          runVm.getNetParams( ).getVlan( ) );
            notwork.addTokenIfAbsent( netToken );
          } catch ( NetworkAlreadyExistsException e ) {
            LOG.trace( e );
          }
          notwork.extantNetworkIndex( runVm.getPlacement( ), runVm.getNetParams( ).getNetworkIndex( ) );
        } catch ( NoSuchElementException e1 ) {
          try {
            //TODO:GRZE:RESTORE
            try {
              notwork = NetworkGroupUtil.getUserNetworkRulesGroup( UserFullName.getInstance( runVm.getOwnerId( ) ), netName ).getVmNetwork( );
            } catch ( Exception e ) {
              notwork = NetworkGroupUtil.getUserNetworkRulesGroup( UserFullName.getInstance( runVm.getOwnerId( ) ), "default" ).getVmNetwork( );
            }
            networks.add( notwork );
            NetworkToken netToken = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getState( ).extantAllocation( runVm.getOwnerId( ), netName,
                                                                                                                          notwork.getUuid( ),
                                                                                                                          runVm.getNetParams( ).getVlan( ) );
            notwork.addTokenIfAbsent( netToken );
            Networks.getInstance( ).registerIfAbsent( notwork, Networks.State.ACTIVE );
//          } catch ( EucalyptusCloudException e ) {
//            LOG.error( e );
//            ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
//            Callbacks.newRequest( new TerminateCallback( runVm.getInstanceId( ) ) ).dispatch( runVm.getPlacement( ) );
          } catch ( NetworkAlreadyExistsException e ) {
            LOG.trace( e );
          }
        }
      }
      VmInstance vm = new VmInstance( ownerId, instanceId, instanceUuid, reservationId, launchIndex, placement, userData, runVm.getInstanceType( ), key, vmType,
                                      img.getPlatform( ).toString( ),
                                      networks,
                                      Integer.toString( runVm.getNetParams( ).getNetworkIndex( ) ) );
      vm.clearPending( );
      vm.updatePublicAddress( VmInstance.DEFAULT_IP );
      VmInstances.getInstance( ).register( vm );
    } catch ( NoSuchElementException e ) {
      ClusterConfiguration config = Clusters.getInstance( ).lookup( runVm.getPlacement( ) ).getConfiguration( );
      AsyncRequests.newRequest( new TerminateCallback( runVm.getInstanceId( ) ) ).dispatch( runVm.getPlacement( ) );
    } catch ( Throwable t ) {
      LOG.error( t, t );
    }
  }
  
  public static ArrayList<ReservationInfoType> handle( DescribeInstancesType request ) throws Exception {
    Context ctx = Contexts.lookup( );
    boolean isAdmin = ctx.hasAdministrativePrivileges( );
    ArrayList<String> instancesSet = request.getInstancesSet( );
    String action = PolicySpec.requestToAction( request );
    User requestUser = ctx.getUser( );
    Map<String, ReservationInfoType> rsvMap = new HashMap<String, ReservationInfoType>( );
    for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
      Account instanceAccount = null;
      try {
        instanceAccount = Accounts.lookupUserById( v.getOwner( ).getUniqueId( ) ).getAccount( );
      } catch ( AuthException e ) {
        throw new EucalyptusCloudException( e );
      }
      if ( ( !isAdmin &&
             !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, v.getInstanceId( ), instanceAccount, action, requestUser ) )
           || ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) ) {
        continue;
      }
      if ( rsvMap.get( v.getReservationId( ) ) == null ) {
        ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwner( ).getNamespace( ), v.getNetworkNames( ) );
        rsvMap.put( reservation.getReservationId( ), reservation );
      }
      rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( ) );
    }
    if ( isAdmin ) {
      for ( VmInstance v : VmInstances.getInstance( ).listDisabledValues( ) ) {
        if ( VmState.BURIED.equals( v.getState( ) ) ) continue;
        if ( !instancesSet.isEmpty( ) && !instancesSet.contains( v.getInstanceId( ) ) ) continue;
        if ( rsvMap.get( v.getReservationId( ) ) == null ) {
          ReservationInfoType reservation = new ReservationInfoType( v.getReservationId( ), v.getOwner( ), v.getNetworkNames( ) );
          rsvMap.put( reservation.getReservationId( ), reservation );
        }
        rsvMap.get( v.getReservationId( ) ).getInstancesSet( ).add( v.getAsRunningInstanceItemType( ) );
      }
    }
    return new ArrayList<ReservationInfoType>( rsvMap.values( ) );
  }
  
  public static Long countByAccount( String accountId ) throws AuthException {
    long vmNum = 0;
    for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
      if ( Accounts.lookupUserById( v.getOwner( ).getUniqueId( ) ).getAccount( ).getAccountNumber( ).equals( accountId ) ) {
        vmNum++;
      }
    }
    return vmNum;
  }
  
  public static Long countByUser( String userId ) throws AuthException {
    long vmNum = 0;
    for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
      if ( v.getOwner( ).getUniqueId( ).equals( userId ) ) {
        vmNum++;
      }
    }
    return vmNum;
  }
  
}
