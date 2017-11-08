/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.eucalyptus.util.MessageValidation
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import javax.annotation.Nonnull

import static com.eucalyptus.util.MessageValidation.validateRecursively

class IdFormatMessage extends ComputeMessage implements MessageValidation.ValidatableMessage {
  Map<String,String> validate( ) {
    validateRecursively(
        Maps.<String,String>newTreeMap( ),
        new ComputeMessageValidation.ComputeMessageValidationAssistant( ),
        "",
        this )
  }
}

class DescribeIdentityIdFormatType extends IdFormatMessage {
  @Nonnull
  String principalArn
  String resource
}

class DescribeIdentityIdFormatResponseType extends IdFormatMessage {
  ArrayList<IdFormatItemType> statuses = Lists.newArrayList( )
}

class DescribeIdFormatType extends IdFormatMessage {
  String resource
}

class DescribeIdFormatResponseType extends IdFormatMessage {
  ArrayList<IdFormatItemType> statuses = Lists.newArrayList( )
}

class IdFormatItemType extends EucalyptusData {
  String resource
  Boolean useLongIds
  Date deadline

  IdFormatItemType( ) { }

  IdFormatItemType( final String resource, final Boolean useLongIds ) {
    this.resource = resource
    this.useLongIds = useLongIds
  }
}

class ModifyIdentityIdFormatType extends IdFormatMessage {
  @Nonnull
  String principalArn
  @Nonnull
  String resource
  @Nonnull
  Boolean useLongIds
}

class ModifyIdentityIdFormatResponseType extends IdFormatMessage {
}

class ModifyIdFormatType extends IdFormatMessage {
  @Nonnull
  String resource
  @Nonnull
  Boolean useLongIds
}

class ModifyIdFormatResponseType extends IdFormatMessage {
}
