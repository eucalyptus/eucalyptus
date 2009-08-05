package com.eucalyptus.ws.server;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.stages.HmacV2UserAuthenticationStage;
import com.eucalyptus.ws.stages.QueryBindingStage;
import com.eucalyptus.ws.stages.UnrollableStage;


public class EucalyptusQueryPipeline extends FilteredPipeline {
  private static Logger LOG = Logger.getLogger( EucalyptusQueryPipeline.class );

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new HmacV2UserAuthenticationStage( ) );
    stages.add( new QueryBindingStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      if ( httpRequest.getMethod( ).equals( HttpMethod.POST ) ) {
        ChannelBuffer buffer = httpRequest.getContent( );
        buffer.markReaderIndex( );
        byte[] read = new byte[buffer.readableBytes( )];
        buffer.readBytes( read );
        String query = new String( read );
        httpRequest.setQuery( query );
        buffer.resetReaderIndex( );
      }
      for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
        if ( !httpRequest.getParameters( ).containsKey( p.toString( ) ) ) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public String getPipelineName( ) {
    return "eucalyptus-query";
  }

  public enum RequiredQueryParams {
    SignatureVersion,
    Version
  }
  
  public enum OperationParameter {

    Operation, Action;
    private static String patterh = buildPattern();

    private static String buildPattern()
    {
      StringBuilder s = new StringBuilder();
      for ( OperationParameter op : OperationParameter.values() ) s.append( "(" ).append( op.name() ).append( ")|" );
      s.deleteCharAt( s.length() - 1 );
      return s.toString();
    }

    public static String toPattern()
    {
      return patterh;
    }

    public static String getParameter( Map<String,String> map )
    {
      for( OperationParameter op : OperationParameter.values() )
        if( map.containsKey( op.toString() ) )
          return map.get( op.toString() );
      return null;
    }
  }

}
