package com.eucalyptus.ws.server;

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.client.NioMessageReceiver;
import com.eucalyptus.ws.stages.BindingStage;
import com.eucalyptus.ws.stages.SoapInternalAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;

public class InternalSoapPipeline extends FilteredPipeline {

  private String servicePath;
  private String serviceName;
  
  public InternalSoapPipeline( NioMessageReceiver msgReceiver, String serviceName, String servicePath ) {
    super( msgReceiver );
    this.servicePath = servicePath;
    this.serviceName = serviceName;
  }

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new SoapInternalAuthenticationStage( ) );
    stages.add( new BindingStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    return message.getUri( ).endsWith( servicePath ) && message.getHeaderNames().contains( "SOAPAction" );
  }

  @Override
  public String getPipelineName( ) {
    return "internal-pipeline-" + this.serviceName;
  }

}
