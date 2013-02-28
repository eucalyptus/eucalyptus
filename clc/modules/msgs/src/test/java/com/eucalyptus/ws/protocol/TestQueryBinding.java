/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.ws.protocol;

import java.util.Map;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
public class TestQueryBinding extends BaseQueryBinding<OperationParameter> {
  private final Binding binding;  
  
  public TestQueryBinding( final Binding binding ) {
    super( "urn:eucalyptus.com", "V1", OperationParameter.Action, OperationParameter.Operation );
    this.binding = binding;
  }

  @Override
  protected Binding getBindingWithElementClass( final String operationName ) throws BindingException {
    return binding.getElementClass( operationName ) != null ?
      binding :
      null;
  }

  @Override
  protected void validateBinding( final Binding currentBinding,
                                  final String operationName,
                                  final Map<String, String> params,
                                  final BaseMessage eucaMsg) {
    // Validation requires compiled bindings
  }
}
