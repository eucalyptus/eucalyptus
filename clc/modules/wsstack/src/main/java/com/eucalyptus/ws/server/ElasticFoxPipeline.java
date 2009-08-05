package com.eucalyptus.ws.server;

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.stages.ElasticFoxMangleStage;
import com.eucalyptus.ws.stages.UnrollableStage;

public class ElasticFoxPipeline extends EucalyptusQueryPipeline {

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    super.addStages( stages );
    stages.add( new ElasticFoxMangleStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      String userAgent = httpRequest.getHeader( "User-Agent" );
      if( userAgent != null && userAgent.matches( ".*Elasticfox.*" )){
        httpRequest.setServicePath( httpRequest.getServicePath( ).replaceAll( "Eucalyptus/", "Eucalyptus" ) );
        return true;
      }
    }
    return false;
  }

  @Override
  public String getPipelineName( ) {
    return "elasticfox-"+super.getPipelineName( );
  }

  
  
}
