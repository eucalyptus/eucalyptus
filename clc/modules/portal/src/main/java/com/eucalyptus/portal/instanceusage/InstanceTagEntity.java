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
package com.eucalyptus.portal.instanceusage;

import com.eucalyptus.portal.workflow.InstanceTag;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Parent;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
public class InstanceTagEntity implements InstanceTag {
  private static Logger LOG     = Logger.getLogger( InstanceTagEntity.class );

  @Parent
  private InstanceLogEntity instanceLog;

  @Transient
  private static final long serialVersionUID = 1L;

  @Column( name = "tag_key", nullable = false )
  private String tagKey;

  @Column( name = "tag_value", nullable = false)
  private String tagValue;


  public InstanceTagEntity() { }

  public InstanceTagEntity(final String key, final String value) {
    this.tagKey = key;
    this.tagValue = value;
  }

  public void setKey(final String tagKey) {
    this.tagKey = tagKey;
  }
  public String getKey() {
    return this.tagKey;
  }

  public void setValue(final String tagValue) {
    this.tagValue = tagValue;
  }
  public String getValue() {
    return this.tagValue;
  }

  public void setInstanceLog(final InstanceLogEntity entity) {
    this.instanceLog = entity;
  }
  public InstanceLogEntity getInstanceLog() {
    return this.instanceLog;
  }
}
