/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.pipelines;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.eucalyptus.ws.stages.NodeAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;

@ComponentPart(Storage.class)
public class ExternalStorageControllerSoapPipeline extends FilteredPipeline {
  private static final String SC_EXTERNAL_SOAP_NAMESPACE = "storagecontroller_eucalyptus_ucsb_edu";

  private final UnrollableStage auth = new NodeAuthenticationStage();

  @Override
  public boolean checkAccepts(final HttpRequest message) {
    return (message.getUri().endsWith("/services/Storage") || message.getUri().endsWith("/services/Storage/"))
        && message.getHeaderNames().contains("SOAPAction") && message.getHeader("SOAPAction").trim().startsWith("\"EucalyptusSC#");
  }

  @Override
  public String getName() {
    return "storage-controller-external-soap";
  }

  @Override
  public ChannelPipeline addHandlers(ChannelPipeline pipeline) {
    pipeline.addLast("deserialize", Handlers.soapMarshalling());
    // pipeline.addLast( "ws-security", Handlers.internalWsSecHandler() );
    auth.unrollStage(pipeline);
    pipeline.addLast("ws-addressing", Handlers.newAddressingHandler("EucalyptusSC#"));
    pipeline.addLast("build-soap-envelope", Handlers.soapHandler());
    // pipeline.addLast( "binding", Handlers.bindingHandler( ) ); //
    pipeline.addLast("binding",
        new BindingHandler(BindingHandler.context(BindingManager.getBinding(SC_EXTERNAL_SOAP_NAMESPACE))));
    return pipeline;
  }

}
