/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.service.config;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;

/**
 *
 */
@Entity
@PersistenceContext( name="eucalyptus_config" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ComponentPart( CloudWatch.class )
public class CloudWatchConfiguration extends ComponentConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SERVICE_PATH= "/services/CloudWatch";

  public CloudWatchConfiguration() { }

  public CloudWatchConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, SERVICE_PATH );
  }
}
