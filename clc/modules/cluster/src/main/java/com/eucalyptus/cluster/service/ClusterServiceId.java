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
package com.eucalyptus.cluster.service;

import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentApi;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.techpreview.TechPreview;

/**
 *
 */
@Partition( Eucalyptus.class )
@ComponentApi( ClusterController.class )
@TechPreview( enableByDefaultProperty = "com.eucalyptus.cluster.service.enable" )
@Description( "Cluster controller service" )
public class ClusterServiceId extends ComponentId {
  private static final long serialVersionUID = 1L;

  public ClusterServiceId( ) {
    super( "ClusterService" );
  }

  @Override
  public String getInternalServicePath( final String... pathParts ) {
    return "/internal/Cluster";
  }

  @Override
  public String getServicePath( final String... pathParts ) {
    return "/services/Cluster";
  }
}
