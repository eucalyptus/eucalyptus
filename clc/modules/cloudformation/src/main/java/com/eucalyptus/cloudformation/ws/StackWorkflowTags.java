package com.eucalyptus.cloudformation.ws;

import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter;
import com.eucalyptus.util.Beans;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowTags;

/**
 * Created by ethomas on 10/10/14.
 */
public class StackWorkflowTags extends WorkflowTags {

  private String stackId;
  private String stackName;
  private String accountId;
  private String accountName;


  public StackWorkflowTags( String stackId, String stackName, String accountId, String accountName ) {
    this.stackId = stackId;
    this.stackName = stackName;
    this.accountId = accountId;
    this.accountName = accountName;
  }

  public String getStackId( ) {
    return stackId;
  }

  public void setStackId( String stackId ) {
    this.stackId = stackId;
  }

  public String getStackName( ) {
    return stackName;
  }

  public void setStackName( String stackName ) {
    this.stackName = stackName;
  }

  public String getAccountId( ) {
    return accountId;
  }

  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }

  public String getAccountName( ) {
    return accountName;
  }

  public void setAccountName( String accountName ) {
    this.accountName = accountName;
  }

  /**
   * @return tags based on the properties of this class that can be used in an SWF workflow
   */
  @Override
  public List<String> constructTags( ) {
    List<String> retVal = Lists.newArrayList( );
    retVal.add( truncate( "StackId:" + stackId, 255 ) );
    retVal.add( truncate( "StackName:" + stackName, 255 ) );
    retVal.add( truncate( "AccountId:" + accountId, 255 ) );
    retVal.add( truncate( "AccountName:" + accountName, 255 ) );
    return retVal;
  }
  
  private String truncate( String s, int i ) {
    if ( s == null ) return null;
    if ( s.length( ) > i ) return s.substring( 0, i );
    return s;
  }

  @Override
  @SuppressWarnings( "CatchException" )
  protected void populatePropertyFromJson( String json, final String key ) {
    JsonDataConverter dataConverter = new JsonDataConverter( );
    String valueString = null;
    try {
      valueString = new ObjectMapper( ).readTree( MoreObjects.firstNonNull( json, "\"\"" ) ).get( key ).asText( );
    } catch ( Exception ignore ) {
      // This is not the property we are looking for, no reason to fail
    }

    if ( !Strings.isNullOrEmpty( valueString ) ) {
      Class<?> type = null;
      try {
        type = Beans.getObjectPropertyType( this, key );
      } catch ( Exception ignore ) {
        // Attempt string type
      }
      try {
        Object value = valueString;
        if ( !String.class.equals( type ) ) {
          value = dataConverter.fromData( valueString, type );
        }
        Beans.setObjectProperty( this, key, value );
      } catch ( Exception ignore ) {
        // Could not convert data so the property will not be populated
      }
    }
  }
}
