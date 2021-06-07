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
package com.eucalyptus.loadbalancing.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class PolicyAttributeTypeDescription extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  private String attributeName;
  private String attributeType;
  private String description;
  private String defaultValue;
  private String cardinality;

  public PolicyAttributeTypeDescription( ) {
  }

  public String getAttributeName( ) {
    return attributeName;
  }

  public void setAttributeName( String attributeName ) {
    this.attributeName = attributeName;
  }

  public String getAttributeType( ) {
    return attributeType;
  }

  public void setAttributeType( String attributeType ) {
    this.attributeType = attributeType;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getDefaultValue( ) {
    return defaultValue;
  }

  public void setDefaultValue( String defaultValue ) {
    this.defaultValue = defaultValue;
  }

  public String getCardinality( ) {
    return cardinality;
  }

  public void setCardinality( String cardinality ) {
    this.cardinality = cardinality;
  }
}
