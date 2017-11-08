package com.eucalyptus.cloudformation.resources;

import java.util.ArrayList;
import java.util.Collection;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by ethomas on 12/18/13.
 */
public abstract class ResourceInfo {

  private Boolean ready = false;
  private String propertiesJson;
  private String metadataJson;
  private String creationPolicyJson;
  private String updatePolicyJson;
  private String deletionPolicy = "Delete";
  private String accountId;
  private String effectiveUserId;
  private String type;
  private String logicalResourceId;
  private String physicalResourceId;
  private Boolean createdEnoughToDelete = false;
  private Boolean allowedByCondition;
  private String referenceValueJson;
  private String description;

  protected ResourceInfo( ) {
  }

  public boolean supportsTags( ) {
    return false;
  }

  public boolean supportsSnapshot( ) {
    return false;
  }

  public boolean supportsSignals( ) {
    return false;
  }

  public boolean isAttributeAllowed( String attributeName ) throws CloudFormationException {
    return ResourceAttributeResolver.resourceHasAttribute( this, attributeName );
  }

  public Collection<String> getRequiredCapabilities( JsonNode propertiesJson ) {
    return new ArrayList<>( );
  }

  public String getResourceAttributeJson( String attributeName ) throws CloudFormationException {
    return ResourceAttributeResolver.getResourceAttributeJson( this, attributeName );
  }

  public void setResourceAttributeJson( String attributeName, String attributeValueJson ) throws CloudFormationException {
    ResourceAttributeResolver.setResourceAttributeJson( this, attributeName, attributeValueJson );
  }

  public Collection<String> getAttributeNames( ) throws CloudFormationException {
    return ResourceAttributeResolver.getResourceAttributeNames( this );
  }

  public Boolean getReady( ) {
    return ready;
  }

  public void setReady( Boolean ready ) {
    this.ready = ready;
  }

  public String getPropertiesJson( ) {
    return propertiesJson;
  }

  public void setPropertiesJson( String propertiesJson ) {
    this.propertiesJson = propertiesJson;
  }

  public String getMetadataJson( ) {
    return metadataJson;
  }

  public void setMetadataJson( String metadataJson ) {
    this.metadataJson = metadataJson;
  }

  public String getCreationPolicyJson( ) {
    return creationPolicyJson;
  }

  public void setCreationPolicyJson( String creationPolicyJson ) {
    this.creationPolicyJson = creationPolicyJson;
  }

  public String getUpdatePolicyJson( ) {
    return updatePolicyJson;
  }

  public void setUpdatePolicyJson( String updatePolicyJson ) {
    this.updatePolicyJson = updatePolicyJson;
  }

  public String getDeletionPolicy( ) {
    return deletionPolicy;
  }

  public void setDeletionPolicy( String deletionPolicy ) {
    this.deletionPolicy = deletionPolicy;
  }

  public String getAccountId( ) {
    return accountId;
  }

  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }

  public String getEffectiveUserId( ) {
    return effectiveUserId;
  }

  public void setEffectiveUserId( String effectiveUserId ) {
    this.effectiveUserId = effectiveUserId;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getLogicalResourceId( ) {
    return logicalResourceId;
  }

  public void setLogicalResourceId( String logicalResourceId ) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getPhysicalResourceId( ) {
    return physicalResourceId;
  }

  public void setPhysicalResourceId( String physicalResourceId ) {
    this.physicalResourceId = physicalResourceId;
  }

  public Boolean getCreatedEnoughToDelete( ) {
    return createdEnoughToDelete;
  }

  public void setCreatedEnoughToDelete( Boolean createdEnoughToDelete ) {
    this.createdEnoughToDelete = createdEnoughToDelete;
  }

  public Boolean getAllowedByCondition( ) {
    return allowedByCondition;
  }

  public void setAllowedByCondition( Boolean allowedByCondition ) {
    this.allowedByCondition = allowedByCondition;
  }

  public String getReferenceValueJson( ) {
    return referenceValueJson;
  }

  public void setReferenceValueJson( String referenceValueJson ) {
    this.referenceValueJson = referenceValueJson;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

}
