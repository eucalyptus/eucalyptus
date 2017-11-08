/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cluster.common;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.cluster.common.ResourceState.NoSuchTokenException;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;

public abstract class ResourceToken implements Comparable<ResourceToken> {
  private static Logger       LOG    = Logger.getLogger( ResourceToken.class );

  private final TypedContext  resourceContext = TypedContext.newTypedContext( );
  private final Cluster       cluster;
  private final Date          creationTime;
  private final VmType        vmType;
  private final String        resourceId;
  private final boolean       unorderedType;

  public ResourceToken( final Cluster cluster, final VmType vmType, final boolean unorderedType, final String resourceId ) {
    this.cluster = cluster;
    this.creationTime = Calendar.getInstance( ).getTime( );
    this.vmType = vmType;
    this.unorderedType = unorderedType;
    this.resourceId = resourceId;
  }
  
  public Integer getAmount( ) {
    return 1;
  }
  
  public Date getCreationTime( ) {
    return this.creationTime;
  }

  public VmType getVmType( ) {
    return vmType;
  }

  @Override
  public int compareTo( final ResourceToken that ) {
    return this.resourceId.compareTo( that.resourceId );
  }
  
  public <T> T getAttribute( final TypedKey<T> key ) {
    return resourceContext.get( key );
  }

  public <T> T setAttribute( final TypedKey<T> key, final T value ) {
    return resourceContext.put( key, value );
  }

  public <T> T removeAttribute( final TypedKey<T> key ) {
    return resourceContext.remove( key );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof ResourceToken ) ) return false;
    final ResourceToken that = (ResourceToken) o;
    return Objects.equals( resourceId, that.resourceId );
  }

  @Override
  public int hashCode() {
    return Objects.hash( resourceId );
  }

  static Logger getLOG( ) {
    return LOG;
  }
  
  public void submit( ) throws NoSuchTokenException {
    this.cluster.getNodeState( ).submitToken( this );
  }
  
  public void redeem( ) throws NoSuchTokenException {
    this.cluster.getNodeState( ).redeemToken( this );
  }
  
  public void release( ) throws NoSuchTokenException {
    this.cluster.getNodeState( ).releaseToken( this );
  }

  public boolean isPending( ) {
    return this.cluster.getNodeState( ).isPending( this );
  }

  abstract public boolean isCommitted( );

  abstract public OwnerFullName getOwner( );

  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "ResourceToken:" );
    if ( this.resourceId != null ) {
      builder.append( this.resourceId ).append( ":" );
    }
    builder.append( "resources=" ).append( resourceContext );
    return builder.toString( );
  }
  
  public boolean isUnorderedType( ) {
    return this.unorderedType;
  }

  public Cluster getCluster( ) {
    return this.cluster;
  }
}
