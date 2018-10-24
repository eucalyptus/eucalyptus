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
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpEmbedded;

public class ModifyNetworkInterfaceAttributeType extends VpcMessage {

  private String networkInterfaceId;
  private NullableAttributeValueType description;
  private AttributeBooleanValueType sourceDestCheck;
  @HttpEmbedded
  private SecurityGroupIdSetType groupSet;
  private ModifyNetworkInterfaceAttachmentType attachment;

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public NullableAttributeValueType getDescription( ) {
    return description;
  }

  public void setDescription( NullableAttributeValueType description ) {
    this.description = description;
  }

  public AttributeBooleanValueType getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( AttributeBooleanValueType sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public SecurityGroupIdSetType getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( SecurityGroupIdSetType groupSet ) {
    this.groupSet = groupSet;
  }

  public ModifyNetworkInterfaceAttachmentType getAttachment( ) {
    return attachment;
  }

  public void setAttachment( ModifyNetworkInterfaceAttachmentType attachment ) {
    this.attachment = attachment;
  }
}
