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
 ************************************************************************/
package com.eucalyptus.tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.compute.common.CloudMetadata.TagMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * Entity for EC2 tags
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.JOINED )
@DiscriminatorColumn( name="metadata_tag_resource_type", discriminatorType = DiscriminatorType.STRING, length = 32) // ignored by Hibernate (for JOINED)
@AttributeOverride(name = "displayName", column = @Column(name = "metadata_display_name", updatable = false, nullable = false, length = 128))
public class Tag<T extends Tag<T>> extends UserMetadata<Tag.State> implements TagMetadata {
  private static final long serialVersionUID = 1L;

  enum State {
    available
  }

  @SuppressWarnings( "FieldCanBeLocal" )
  @Column( name = "metadata_resource_id", nullable = false )
  private String resourceId;

  @Column( name = "metadata_tag_value", nullable = false, length = 256 )
  @Nullable

  private String value;
  @Transient
  @Nonnull

  private Function<? super T,String> resourceIdFunction = Functions.constant( null );
  @Transient
  @Nullable
  private String resourceType;

  protected Tag(  ) {
  }

  protected Tag( @Nullable final String resourceType,
                 @Nonnull final Function<? super T,String> resourceIdFunction ) {
    this.resourceType = resourceType;
    this.resourceIdFunction = resourceIdFunction;
  }

  public Tag( @Nullable final String resourceType,
              @Nonnull final Function<? super T,String> resourceIdFunction,
              @Nonnull final OwnerFullName ownerFullName,
              @Nullable final String key,
              @Nullable final String value ) {
    this( resourceType, resourceIdFunction );
    setOwner( ownerFullName );
    setDisplayName( key );
    setValue( value );
  }

  /**
   * Must be called from constructor
   */
  protected void init() {
    setResourceId( getResourceId() ); // Set for query by example use
  }

  @Override
  protected String createUniqueName() {
    return getOwnerAccountNumber() + ":" + getResourceType() + ":" + getResourceId() + ":" + getKey();
  }
  
  @Nullable
  public String getKey() {
    return getDisplayName();
  }

  public void setKey( @Nonnull final String key ) {
    setDisplayName( key );
  }

  @Nullable
  public String getValue() {
    return value;
  }

  public void setValue( @Nullable final String value ) {
    this.value = value;
  }

  @SuppressWarnings( "unchecked" )
  @Nullable
  public final String getResourceId(){ 
    return resourceId != null ?
        resourceId :
        extractResourceId( );
  }

  @Nullable
  public String getResourceType(){ 
    return resourceType; 
  }

  @Override
  public String getPartition( ) {
    return ComponentIds.lookup(Eucalyptus.class).name( );
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( this.getOwnerAccountNumber( ) )
        .relativeId( "tag", this.getDisplayName( ) );
  }

  @SuppressWarnings( "unchecked" )
  public static Tag withOwner( @Nonnull final OwnerFullName ownerFullName ) {
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new Tag( null, Functions.constant( null ), ownerFullName, null, null );
  }

  /**
   * The resource ID can be set for query by example usage.
   */
  private void setResourceId( final String resourceId ) {
    this.resourceId = resourceId;
  }

  private String extractResourceId() {
    return resourceIdFunction.apply( (T) this );
  }

  @PrePersist
  @PreUpdate
  private void generatedFieldUpdate( ) {
    setResourceId( extractResourceId() );
  }
}
