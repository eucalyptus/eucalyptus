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
