/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.entities;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.Ats;
import com.eucalyptus.auth.principal.FullName;

/**
 * A convenient parent class for entity types which are cloud-global in scope, owned by a user, and
 * have associated state. NOTE: this currently exploits two kinds of brain-damaged assumptions:
 * 1. the @PolicyVendor annotation of the given ComponentId is used to determine what service name should be used when constructing the FullName/ARN.
 * 2. the @PolicyResourceType annotation of the subclass is used to determine what resource type name should be used when constructing the FullName/ARN.
 * THIS SHOULD BE RECONSIDERED. See link below.
 * 
 * @see http
 *      ://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-policies-for-amazon-ec2.html#
 *      EC2_ARN_Format
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public abstract class GlobalUserMetadata<STATE extends Enum<STATE>> extends UserMetadata<STATE> {
  private static Logger LOG = Logger.getLogger( GlobalUserMetadata.class );
  private final Class<? extends ComponentId> serviceComponentClass;
  private final String                       serviceName;
  private final String                       regionName;
  private final String                       resourceTypeName;
  
  public GlobalUserMetadata( Class<? extends ComponentId> serviceComponentClass ) {
    this.serviceComponentClass = serviceComponentClass;
    ComponentId serviceComponent = ComponentIds.lookup( serviceComponentClass );
    /**
     * this lies! @PolicyVendor is confused about ARN service names. See above comments.
     */
    this.serviceName = serviceComponent.getVendorName( );
    this.regionName = serviceComponent.name( );
    /**
     * this lies! @PolicyResourceType is confused about ARN resource type names. See above comments.
     */
    Ats ats = Ats.from( this );
    if ( ats.has( PolicyResourceType.class ) ) {
      this.resourceTypeName = ats.get( PolicyResourceType.class ).value( );
    } else {
      LOG.debug( "HACK:  type is missing the resource type information in a @PolicyResourceType: " + this.getClass( ) );
      this.resourceTypeName = this.getClass( ).getSimpleName( ).toLowerCase( );
    }
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( this.serviceName )
                          .region( this.getPartition( ) )
                          .accountId( this.getOwnerAccountNumber( ) )
                          .relativeId( this.resourceTypeName, this.getDisplayName( ) );
  }
}
