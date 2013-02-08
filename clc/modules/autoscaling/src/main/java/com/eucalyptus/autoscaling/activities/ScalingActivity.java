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

import java.util.Date;
import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_scaling_activities" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ScalingActivity extends AbstractOwnedPersistent {

  private static final long serialVersionUID = 1L;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_group_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private AutoScalingGroup group;

  @Column( name = "metadata_auto_scaling_group_name", nullable = false, updatable = false )
  private String autoScalingGroupName;

  @Column( name = "metadata_cause", nullable = false )
  private String cause;

  @Column( name = "metadata_description" )
  private String description;

  @Column( name = "metadata_details" )
  private String details;

  @Column( name = "metadata_end_time" )
  private Date endTime;

  @Column( name = "metadata_progress" )
  private int progress;

  @Column( name = "metadata_status_code", nullable = false )
  @Enumerated( EnumType.STRING )
  private ActivityStatusCode activityStatusCode;

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

  public String getCause() {
    return cause;
  }

  public void setCause( final String cause ) {
    this.cause = cause;
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

  public ActivityStatusCode getActivityStatusCode() {
    return activityStatusCode;
  }

  public void setActivityStatusCode( final ActivityStatusCode activityStatusCode ) {
    this.activityStatusCode = activityStatusCode;
  }

  public static ScalingActivity create( @Nonnull final AutoScalingGroup group,
                                        @Nonnull final String cause ) {
    final ScalingActivity activity = new ScalingActivity( group.getOwner() );
    activity.setGroup( group );
    activity.setCause( cause );
    activity.setActivityStatusCode( ActivityStatusCode.InProgress );    
    return activity;
  }
  
  /**
   * Create an example ScalingActivity for the given owner. 
   *
   * @param ownerFullName The owner
   * @return The example
   */
  public static ScalingActivity withOwner( final OwnerFullName ownerFullName ) {
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
