/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSS3BucketProperties implements ResourceProperties {

  @Property
  private String accessControl;

  @Property
  private String bucketName;

  @Property
  private S3CorsConfiguration corsConfiguration;

  @Property
  private S3LifecycleConfiguration lifecycleConfiguration;

  @Property
  private S3LoggingConfiguration loggingConfiguration;

  @Property
  private S3NotificationConfiguration notificationConfiguration;

  @Property
  private S3ReplicationConfiguration replicationConfiguration;

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  @Property
  private S3VersioningConfiguration versioningConfiguration;

  @Property
  private S3WebsiteConfiguration websiteConfiguration;

  public String getAccessControl( ) {
    return accessControl;
  }

  public void setAccessControl( String accessControl ) {
    this.accessControl = accessControl;
  }

  public String getBucketName( ) {
    return bucketName;
  }

  public void setBucketName( String bucketName ) {
    this.bucketName = bucketName;
  }

  public S3CorsConfiguration getCorsConfiguration( ) {
    return corsConfiguration;
  }

  public void setCorsConfiguration( S3CorsConfiguration corsConfiguration ) {
    this.corsConfiguration = corsConfiguration;
  }

  public S3LifecycleConfiguration getLifecycleConfiguration( ) {
    return lifecycleConfiguration;
  }

  public void setLifecycleConfiguration( S3LifecycleConfiguration lifecycleConfiguration ) {
    this.lifecycleConfiguration = lifecycleConfiguration;
  }

  public S3LoggingConfiguration getLoggingConfiguration( ) {
    return loggingConfiguration;
  }

  public void setLoggingConfiguration( S3LoggingConfiguration loggingConfiguration ) {
    this.loggingConfiguration = loggingConfiguration;
  }

  public S3NotificationConfiguration getNotificationConfiguration( ) {
    return notificationConfiguration;
  }

  public void setNotificationConfiguration( S3NotificationConfiguration notificationConfiguration ) {
    this.notificationConfiguration = notificationConfiguration;
  }

  public S3ReplicationConfiguration getReplicationConfiguration( ) {
    return replicationConfiguration;
  }

  public void setReplicationConfiguration( S3ReplicationConfiguration replicationConfiguration ) {
    this.replicationConfiguration = replicationConfiguration;
  }

  public ArrayList<CloudFormationResourceTag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<CloudFormationResourceTag> tags ) {
    this.tags = tags;
  }

  public S3VersioningConfiguration getVersioningConfiguration( ) {
    return versioningConfiguration;
  }

  public void setVersioningConfiguration( S3VersioningConfiguration versioningConfiguration ) {
    this.versioningConfiguration = versioningConfiguration;
  }

  public S3WebsiteConfiguration getWebsiteConfiguration( ) {
    return websiteConfiguration;
  }

  public void setWebsiteConfiguration( S3WebsiteConfiguration websiteConfiguration ) {
    this.websiteConfiguration = websiteConfiguration;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "accessControl", accessControl )
        .add( "bucketName", bucketName )
        .add( "corsConfiguration", corsConfiguration )
        .add( "lifecycleConfiguration", lifecycleConfiguration )
        .add( "loggingConfiguration", loggingConfiguration )
        .add( "notificationConfiguration", notificationConfiguration )
        .add( "replicationConfiguration", replicationConfiguration )
        .add( "tags", tags )
        .add( "versioningConfiguration", versioningConfiguration )
        .add( "websiteConfiguration", websiteConfiguration )
        .toString( );
  }
}
