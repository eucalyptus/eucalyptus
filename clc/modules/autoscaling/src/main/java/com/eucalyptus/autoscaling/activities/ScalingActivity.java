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
package com.eucalyptus.autoscaling.activities;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingActivityMetadata;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_scaling_activities" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ScalingActivity extends AbstractOwnedPersistent implements ScalingActivityMetadata {

  private static final long serialVersionUID = 1L;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_group_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private AutoScalingGroup group;

  @Column( name = "metadata_auto_scaling_group_name", nullable = false, updatable = false )
  private String autoScalingGroupName;

  @Column( name = "metadata_description" )
  private String description;

  @Column( name = "metadata_details" )
  private String details;

  @Column( name = "metadata_end_time" )
  private Date endTime;

  @Column( name = "metadata_progress", nullable = false )
  private Integer progress;

  @Column( name = "metadata_status_code", nullable = false )
  @Enumerated( EnumType.STRING )
  private ActivityStatusCode statusCode;

  @Column( name = "metadata_status_message" )
  private String statusMessage;

  @Column( name = "metadata_client_token" )
  private String clientToken;

  @ElementCollection
  @CollectionTable( name = "metadata_scaling_activity_causes" )
  @JoinColumn( name = "metadata_scaling_activity_id" )
  @OrderColumn( name = "metadata_cause_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<ActivityCause> causes = Lists.newArrayList( );

  protected ScalingActivity() {    
  }

  protected ScalingActivity( final OwnerFullName ownerFullName ) {
    super( ownerFullName );
  }

  protected ScalingActivity( final OwnerFullName ownerFullName,
                             final String activityId ) {
    super( ownerFullName, activityId );
  }

  public String getActivityId() {
    return getNaturalId();
  }

  @Override
  public String getDisplayName() {
    return getNaturalId();
  }

  public AutoScalingGroup getGroup() {
    return group;
  }

  public void setGroup( final AutoScalingGroup group ) {
    this.group = group;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( final String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails( final String details ) {
    this.details = details;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime( final Date endTime ) {
    this.endTime = endTime;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress( final int progress ) {
    this.progress = progress;
  }

  public ActivityStatusCode getStatusCode() {
    return statusCode;
  }

  public void setStatusCode( final ActivityStatusCode statusCode ) {
    this.statusCode = statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage( final String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public String getClientToken() {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public List<ActivityCause> getCauses() {
    return causes;
  }

  public void setCauses( final List<ActivityCause> causes ) {
    this.causes = causes;
  }

  public String getCauseAsString() {
    return Joiner.on( "  " ).join( getCauses() );
  }

  public static ScalingActivity create( @Nonnull final AutoScalingGroup group,
                                        @Nullable final String clientToken,
                                        @Nonnull final Collection<ActivityCause> causes ) {
    final ScalingActivity activity = new ScalingActivity( group.getOwner() );
    activity.setGroup( group );
    activity.setClientToken( clientToken );
    activity.setCauses( Lists.newArrayList( causes ) );
    activity.setStatusCode( ActivityStatusCode.InProgress );
    return activity;
  }
  
  /**
   * Create an example ScalingActivity for the given owner. 
   *
   * @param ownerFullName The owner
   * @return The example
   */
  public static ScalingActivity withOwner( @Nullable final OwnerFullName ownerFullName ) {
    return new ScalingActivity( ownerFullName );
  }

  /**
   * Create an example ScalingActivity for the given owner and identifier. 
   *
   * @param ownerFullName The owner
   * @param activityId The scaling activity identifier
   * @return The example
   */
  public static ScalingActivity named( final OwnerFullName ownerFullName,
                                       final String activityId ) {
    final ScalingActivity example = withOwner( ownerFullName );
    example.setDisplayName( activityId );
    return example;
  }

  public static ScalingActivity withUuid( final String uuid ) {
    final ScalingActivity example = new ScalingActivity();
    example.setNaturalId( uuid );
    return example;
  }

  @Override
  protected String createUniqueName() {
    return getNaturalId();
  }

  @PrePersist
  @PreUpdate
  private void preUpdate() {
    setDisplayName( getNaturalId() );
    autoScalingGroupName = AutoScalingMetadatas.toDisplayName().apply( group );
  }
}
