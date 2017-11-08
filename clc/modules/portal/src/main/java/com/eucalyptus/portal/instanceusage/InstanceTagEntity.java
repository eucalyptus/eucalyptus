/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
