/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cluster.common.msgs;

import java.net.URI;
import java.util.Date;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Faults;

public class NodeInfo implements Comparable<NodeInfo> {

  private String iqn;
  private String serviceTag;
  private String name;
  private String partition;
  private Hypervisor hypervisor = Hypervisor.Unknown;
  private Boolean hasClusterCert = false;
  private Boolean hasNodeCert = false;
  private Component.State lastState;
  private Faults.CheckException lastException;
  private String lastMessage;
  private Date lastSeen;
  private NodeCertInfo certs = new NodeCertInfo( );
  private NodeLogInfo logs = new NodeLogInfo( );

  public NodeInfo( ) {
  }

  public NodeInfo( final String serviceTag ) {
    this.serviceTag = serviceTag;
    this.name = URI.create( this.serviceTag ).getHost( );
    this.lastSeen = new Date( );
    this.certs.setServiceTag( this.serviceTag );
    this.logs.setServiceTag( this.serviceTag );
  }

  public NodeInfo( final String partition, final NodeType nodeType ) {
    this.partition = partition;
    this.serviceTag = nodeType.getServiceTag( );
    this.iqn = nodeType.getIqn( );
    this.hypervisor = Hypervisor.fromString( nodeType.getHypervisor( ) );
    this.name = URI.create( this.serviceTag ).getHost( );
    this.lastSeen = new Date( );
    this.certs.setServiceTag( this.serviceTag );
    this.logs.setServiceTag( this.serviceTag );
  }

  public NodeInfo( NodeInfo orig, final NodeCertInfo certs ) {
    this( orig.serviceTag );
    this.logs = orig.logs;
    this.certs = certs;
  }

  public NodeInfo( NodeInfo orig, final NodeLogInfo logs ) {
    this( orig.serviceTag );
    this.certs = orig.certs;
    this.logs = logs;
  }

  public NodeInfo( final NodeCertInfo certs ) {
    this( certs.getServiceTag( ) );
    this.certs = certs;
  }

  public NodeInfo( final NodeLogInfo logs ) {
    this( logs.getServiceTag( ) );
    this.logs = logs;
  }

  public void touch( Component.State lastState, String lastMessage, Faults.CheckException lastEx ) {
    this.lastSeen = new Date( );
    this.lastException = lastEx;
    this.lastState = lastState;
    this.lastMessage = lastMessage;
  }

  public int compareTo( NodeInfo o ) {
    return this.serviceTag.compareTo( o.serviceTag );
  }

  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof NodeInfo ) ) return false;
    NodeInfo nodeInfo = (NodeInfo) o;
    if ( !serviceTag.equals( nodeInfo.serviceTag ) ) return false;
    return true;
  }

  public int hashCode( ) {
    return serviceTag.hashCode( );
  }

  @Override
  public String toString( ) {
    return "NodeInfo name=" + name + " lastSeen=" + String.valueOf( lastSeen ) + " serviceTag=" + serviceTag + " iqn=" + iqn + " hypervisor=" + String.valueOf( hypervisor );
  }

  public String getIqn( ) {
    return iqn;
  }

  public void setIqn( String iqn ) {
    this.iqn = iqn;
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getPartition( ) {
    return partition;
  }

  public void setPartition( String partition ) {
    this.partition = partition;
  }

  public Hypervisor getHypervisor( ) {
    return hypervisor;
  }

  public void setHypervisor( Hypervisor hypervisor ) {
    this.hypervisor = hypervisor;
  }

  public Boolean getHasClusterCert( ) {
    return hasClusterCert;
  }

  public void setHasClusterCert( Boolean hasClusterCert ) {
    this.hasClusterCert = hasClusterCert;
  }

  public Boolean getHasNodeCert( ) {
    return hasNodeCert;
  }

  public void setHasNodeCert( Boolean hasNodeCert ) {
    this.hasNodeCert = hasNodeCert;
  }

  public Component.State getLastState( ) {
    return lastState;
  }

  public void setLastState( Component.State lastState ) {
    this.lastState = lastState;
  }

  public Faults.CheckException getLastException( ) {
    return lastException;
  }

  public void setLastException( Faults.CheckException lastException ) {
    this.lastException = lastException;
  }

  public String getLastMessage( ) {
    return lastMessage;
  }

  public void setLastMessage( String lastMessage ) {
    this.lastMessage = lastMessage;
  }

  public Date getLastSeen( ) {
    return lastSeen;
  }

  public void setLastSeen( Date lastSeen ) {
    this.lastSeen = lastSeen;
  }

  public NodeCertInfo getCerts( ) {
    return certs;
  }

  public void setCerts( NodeCertInfo certs ) {
    this.certs = certs;
  }

  public NodeLogInfo getLogs( ) {
    return logs;
  }

  public void setLogs( NodeLogInfo logs ) {
    this.logs = logs;
  }

  public enum Hypervisor {
    KVM( true ), ESXI( false ), Unknown( false );

    private final boolean supportsEkiEri;

    Hypervisor( boolean supportsEkiEri ) {
      this.supportsEkiEri = supportsEkiEri;
    }

    public static Hypervisor fromString( String hypervisor ) {
      try {
        return Hypervisor.valueOf( hypervisor );
      } catch ( IllegalArgumentException ex ) {
        return Unknown;
      }

    }

    public boolean supportEkiEri( ) {
      return supportsEkiEri;
    }
  }
}
