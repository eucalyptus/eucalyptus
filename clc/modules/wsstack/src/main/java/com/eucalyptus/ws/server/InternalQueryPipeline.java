package com.eucalyptus.ws.server;

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.client.NioMessageReceiver;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;
import com.eucalyptus.ws.stages.HmacV2UserAuthenticationStage;
import com.eucalyptus.ws.stages.QueryBindingStage;
import com.eucalyptus.ws.stages.UnrollableStage;

public class InternalQueryPipeline extends FilteredPipeline {

  private String servicePath;
  private String serviceName;

  public InternalQueryPipeline( NioMessageReceiver msgReceiver, String serviceName, String servicePath ) {
    super( msgReceiver );
    this.servicePath = servicePath;
    this.serviceName = serviceName;
  }

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new HmacV2UserAuthenticationStage( true ) );
    stages.add( new QueryBindingStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
        if ( !httpRequest.getParameters( ).containsKey( p.toString( ) ) ) { return false; }
      }
      return true && message.getUri( ).startsWith( servicePath );
    }
    return false;
  }

  @Override
  public String getPipelineName( ) {
    return "internal-query-pipeline-" + this.serviceName;
  }

}
