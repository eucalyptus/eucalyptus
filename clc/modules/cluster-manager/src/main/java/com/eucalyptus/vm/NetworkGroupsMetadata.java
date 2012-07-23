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
 ************************************************************************/

package com.eucalyptus.vm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.NetworkPeer;
import com.eucalyptus.network.NetworkRule;
import com.eucalyptus.util.ByteArray;
import com.eucalyptus.vm.VmInstance.VmState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class NetworkGroupsMetadata implements Function<MetadataRequest, ByteArray> {
  private static Logger                  LOG              = Logger.getLogger( NetworkGroupsMetadata.class );
  private static Lock                    lock             = new ReentrantLock( );
  private static Long                    lastTime         = 0l;
  private static AtomicReference<String> topoString       = new AtomicReference<String>( "" );
  private static final Supplier<String>  topoSupplier     = new Supplier<String>( ) {
                                                            
                                                            @Override
                                                            public String get( ) {
                                                              String ret = generateTopology( );
                                                              topoString.set( ret );
                                                              return ret;
                                                            }
                                                          };
  private static Supplier<String>        topoMemoSupplier = Suppliers.memoizeWithExpiration( topoSupplier, VmInstances.NETWORK_METADATA_REFRESH_TIME, TimeUnit.SECONDS );
  
  private String getNetworkTopology( ) {
    if ( Databases.isVolatile( ) ) {
      return topoString.get( );
    } else {
      return topoMemoSupplier.get( );
    }
  }
  
  private static String generateTopology( ) {
    StringBuilder buf = new StringBuilder( );
    Multimap<String, String> networks = ArrayListMultimap.create( );
    Multimap<String, String> rules = ArrayListMultimap.create( );
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      Predicate<VmInstance> filter = Predicates.and( VmState.TERMINATED.not( ), VmState.STOPPED.not( ) );
      for ( VmInstance vm : VmInstances.list( filter ) ) {
        try {
          for ( NetworkGroup ruleGroup : vm.getNetworkGroups( ) ) {
            try {
              ruleGroup = Entities.merge( ruleGroup );
              networks.put( ruleGroup.getClusterNetworkName( ), vm.getPrivateAddress( ) );
              if ( !rules.containsKey( ruleGroup.getNaturalId( ) ) ) {
                for ( NetworkRule netRule : ruleGroup.getNetworkRules( ) ) {
                  try {
                    String rule = String.format(
                      "-P %s -%s %d%s%d ",
                      netRule.getProtocol( ),
                      ( NetworkRule.Protocol.icmp.equals( netRule.getProtocol( ) )
                                                                                  ? "t"
                                                                                  : "p" ),
                      netRule.getLowPort( ),
                      ( NetworkRule.Protocol.icmp.equals( netRule.getProtocol( ) )
                                                                                  ? ":"
                                                                                  : "-" ),
                      netRule.getHighPort( ) );
                    for ( NetworkPeer peer : netRule.getNetworkPeers( ) ) {
                      Account groupAccount = Accounts.lookupAccountById( peer.getUserQueryKey( ) ); 
                      String groupId = NetworkGroups.lookup( AccountFullName.getInstance( groupAccount ), peer.getGroupName( ) ).getNaturalId( );
                      String ruleString = String.format( "%s -o %s -u %s", rule, groupId, groupAccount.getAccountNumber( ) ); 
                      if ( !rules.get( ruleGroup.getClusterNetworkName( ) ).contains( ruleString ) ) {
                        rules.put( ruleGroup.getClusterNetworkName( ), ruleString );
                      }
                    }
                    for ( String cidr : netRule.getIpRanges( ) ) {
                      String ruleString = String.format( "%s -s %s", rule, cidr );
                      if ( !rules.get( ruleGroup.getClusterNetworkName( ) ).contains( ruleString ) ) {
                        rules.put( ruleGroup.getClusterNetworkName( ), ruleString );
                      }
                    }
                  } catch ( Exception ex ) {
                    LOG.error( ex, ex );
                  }
                }
              }
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
      buf.append( rulesToString( rules ) );
      buf.append( groupsToString( networks ) );
      db.rollback( );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      db.rollback( );
    }
    return buf.toString( );
  }
  
  private static String groupsToString( Multimap<String, String> networks ) {
    StringBuilder buf = new StringBuilder( );
    for ( String networkName : networks.keySet( ) ) {
      buf.append( "GROUP " ).append( networkName );
      for ( String ip : networks.get( networkName ) ) {
        buf.append( " " ).append( ip );
      }
      buf.append( "\n" );
    }
    return buf.toString( );
  }
  
  private static String rulesToString( Multimap<String, String> rules ) {
    StringBuilder buf = new StringBuilder( );
    for ( String networkName : rules.keySet( ) ) {
      for ( String rule : rules.get( networkName ) ) {
        buf.append( "RULE " ).append( networkName ).append( " " ).append( rule ).append( "\n" );
      }
    }
    return buf.toString( );
  }
  
  @Override
  public ByteArray apply( MetadataRequest arg0 ) {
    return ByteArray.newInstance( getNetworkTopology( ) );
  }
}
