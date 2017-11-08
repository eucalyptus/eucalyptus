/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.pipeline.stages.ObjectStorageFormPOSTAggregatorStage;
import com.eucalyptus.objectstorage.pipeline.stages.ObjectStorageFormPOSTBindingStage;
import com.eucalyptus.objectstorage.pipeline.stages.ObjectStorageFormPOSTOutboundStage;
import com.eucalyptus.objectstorage.pipeline.stages.ObjectStorageFormPOSTUserAuthenticationStage;
import com.eucalyptus.objectstorage.pipeline.stages.ObjectStorageRESTExceptionStage;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.ws.stages.UnrollableStage;

@ComponentPart(ObjectStorage.class)
public class ObjectStorageFormPOSTPipeline extends ObjectStorageRESTPipeline {
  private static Logger LOG = Logger.getLogger(ObjectStorageFormPOSTPipeline.class);
  private final UnrollableStage auth = new ObjectStorageFormPOSTUserAuthenticationStage();
  private final UnrollableStage bind = new ObjectStorageFormPOSTBindingStage();
  private final UnrollableStage aggr = new ObjectStorageFormPOSTAggregatorStage();
  private final UnrollableStage out = new ObjectStorageFormPOSTOutboundStage();
  private final UnrollableStage exHandler = new ObjectStorageRESTExceptionStage();

  @Override
  public boolean checkAccepts(HttpRequest message) {
    // Accept form POST requests only
    if (super.checkAccepts(message) && OSGUtil.isFormPOSTRequest(message)) {
      return true;
    }

    return false;
  }

  @Override
  public String getName() {
    return "objectstorage-post-multipart-form";
  }

  @Override
  public ChannelPipeline addHandlers(ChannelPipeline pipeline) {
    auth.unrollStage(pipeline);
    bind.unrollStage(pipeline);
    aggr.unrollStage(pipeline);
    out.unrollStage(pipeline);
    exHandler.unrollStage(pipeline);
    return pipeline;
  }

}
