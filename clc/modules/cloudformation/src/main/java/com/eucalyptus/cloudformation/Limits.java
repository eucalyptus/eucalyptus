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
package com.eucalyptus.cloudformation;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.ws.WebServices;

/**
 * Created by ethomas on 10/20/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")

public class Limits {

  public final static long STACK_NAME_MAX_LENGTH_CHARS = 255;
  @ConfigurableField(initial = "100", description = "The maximum number of mappings allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_MAPPINGS_PER_TEMPLATE = 100;
  @ConfigurableField(initial = "30", description = "The maximum number of attributes allowed in a mapping in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_ATTRIBUTES_PER_MAPPING = 30;
  public final static long MAPPING_NAME_MAX_LENGTH_CHARS = 255;
  @ConfigurableField(initial = "60", description = "The maximum number of outputs allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_OUTPUTS_PER_TEMPLATE = 60;
  public final static long OUTPUT_NAME_MAX_LENGTH_CHARS = 255;
  @ConfigurableField(initial = "60", description = "The maximum number of outputs allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_PARAMETERS_PER_TEMPLATE = 60;
  public final static long PARAMETER_NAME_MAX_LENGTH_CHARS = 255;
  public final static long PARAMETER_VALUE_MAX_LENGTH_BYTES = 4096;
  @ConfigurableField(initial = "200", description = "The maximum number of resources allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_RESOURCES_PER_TEMPLATE = 200;
  public final static long RESOURCE_NAME_MAX_LENGTH_CHARS = 255;
  @ConfigurableField(initial = "51200", description = "The maximum number of bytes in a request-embedded template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long REQUEST_TEMPLATE_BODY_MAX_LENGTH_BYTES = 51200;
  @ConfigurableField(initial = "460800", description = "The maximum number of bytes in a template referenced via a URL", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES = 460800;
  @ConfigurableField(initial = "16384", description = "The maximum number of bytes in a stack policyL", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long REQUEST_STACK_POLICY_MAX_CONTENT_LENGTH_BYTES = 16384;

  public final static long TEMPLATE_DESCRIPTION_MAX_LENGTH_BYTES = 1024;

  public final static long DEFAULT_MAX_LENGTH_WAIT_CONDITION_SIGNAL = 102400;

}
