/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
