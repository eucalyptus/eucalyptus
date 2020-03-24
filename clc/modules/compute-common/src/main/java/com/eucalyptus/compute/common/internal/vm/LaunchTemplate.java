/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.vm;

import static com.eucalyptus.util.Json.JsonOption.OmitNullValues;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.compute.common.CloudMetadata.LaunchTemplateMetadata;
import com.eucalyptus.compute.common.RequestLaunchTemplateData;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_launch_templates", indexes = {
    @Index( name = "metadata_launch_templates_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_launch_templates_display_name_idx", columnList = "metadata_display_name" ),
} )
public class LaunchTemplate extends AbstractOwnedPersistent implements LaunchTemplateMetadata {

  private static final long serialVersionUID = 1L;

  public static final String ID_PREFIX = "lt";

  private static final ObjectMapper mapper = Json
      .mapper( EnumSet.of( OmitNullValues ) )
      .disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );

  @Column( name = "metadata_client_token", updatable = false )
  private String clientToken;

  @Column( name = "metadata_client_token_unique", unique = true, updatable = false )
  private String  uniqueClientToken;

  @Column( name = "metadata_name", updatable = false, nullable = false)
  private String name;

  @Column( name = "metadata_qualified_name", updatable = false, nullable = false, unique = true)
  private String qualifiedName;

  @Column( name = "metadata_version_description", updatable = false )
  private String versionDescription;

  @Column( name = "metadata_template_data", length = 16_000, updatable = false )
  private String templateData;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "launchTemplate" )
  private Collection<LaunchTemplateTag> tags;

  protected LaunchTemplate( ) {
  }

  protected LaunchTemplate( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }


  public static LaunchTemplate create( final OwnerFullName owner,
                                       final String id,
                                       final String clientToken,
                                       final String name,
                                       final String versionDescription ) {
    final LaunchTemplate launchTemplate = new LaunchTemplate( owner, id );
    launchTemplate.setName( name );
    launchTemplate.setQualifiedName(owner.getAccountNumber() + ":" + name);
    launchTemplate.setVersionDescription( versionDescription );
    launchTemplate.setClientToken(clientToken);
    if ( clientToken != null ) {
      launchTemplate.setUniqueClientToken(owner.getAccountNumber() + ":" + clientToken);
    }
    return launchTemplate;
  }

  public static LaunchTemplate exampleWithOwner(final OwnerFullName owner ) {
    return new LaunchTemplate( owner, null );
  }

  public static LaunchTemplate exampleWithName( final OwnerFullName owner, final String name ) {
    return new LaunchTemplate( owner, name );
  }

  public static LaunchTemplate exampleWithClientToken( final OwnerFullName owner, final String clientToken ) {
    final LaunchTemplate launchTemplate = exampleWithOwner( owner );
    launchTemplate.setClientToken(clientToken);
    if ( clientToken != null ) {
      launchTemplate.setUniqueClientToken(owner.getAccountNumber() + ":" + clientToken);
    }
    return launchTemplate;
  }

  public String getArn( ) {
    return String.format(
        "arn:aws:ec2::%1s:launch-template/%2s",
        getOwnerAccountNumber(),
        getDisplayName() );
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getUniqueClientToken() {
    return uniqueClientToken;
  }

  public void setUniqueClientToken(final String uniqueClientToken) {
    this.uniqueClientToken = uniqueClientToken;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public void setQualifiedName(final String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  public String getVersionDescription( ) {
    return versionDescription;
  }

  public void setVersionDescription( final String versionDescription ) {
    this.versionDescription = versionDescription;
  }

  public String getTemplateData( ) {
    return templateData;
  }

  public void setTemplateData( final String templateData ) {
    this.templateData = templateData;
  }

  @Override
  protected String createUniqueName() {
    return getDisplayName( );
  }

  public void writeTemplateData(final RequestLaunchTemplateData launchTemplateData) throws IOException {
    setTemplateData(mapper.writeValueAsString(launchTemplateData));
  }

  public RequestLaunchTemplateData readTemplateData() throws IOException {
    return mapper.readValue(getTemplateData(), RequestLaunchTemplateData.class);
  }
}
