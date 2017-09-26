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
package com.eucalyptus.objectstorage.msgs;

public interface ObjectStorageCommonResponseType {

  public abstract String getOrigin( );

  public abstract void setOrigin( String origin );

  public abstract String getHttpMethod( );

  public abstract void setHttpMethod( String httpMethod );

  public abstract String getBucketName( );

  public abstract void setBucketName( String bucketName );

  public abstract String getBucketUuid( );

  public abstract void setBucketUuid( String bucketUuid );

  public abstract String getAllowedOrigin( );

  public abstract void setAllowedOrigin( String allowedOrigin );

  public abstract String getAllowedMethods( );

  public abstract void setAllowedMethods( String allowedMethods );

  public abstract String getExposeHeaders( );

  public abstract void setExposeHeaders( String exposeHeaders );

  public abstract String getMaxAgeSeconds( );

  public abstract void setMaxAgeSeconds( String maxAgeSeconds );

  public abstract String getAllowCredentials( );

  public abstract void setAllowCredentials( String allowCredentials );

  public abstract String getVary( );

  public abstract void setVary( String vary );
}
