/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.common.Route53Metadata.ChangeMetadata;
import com.eucalyptus.route53.service.persist.entities.ChangeInfo.Status;
import com.eucalyptus.route53.service.persist.views.ChangeInfoView;

/**
 *
 */
@Entity
@PersistenceContext(name = "eucalyptus_route53")
@Table(name = "r53_change")
public class ChangeInfo extends UserMetadata<Status> implements ChangeMetadata, ChangeInfoView {

  private static final long serialVersionUID = 1L;

  public enum Status {
    PENDING,
    INSYNC,
  }

  @Column( name = "r53_change_comment", length = 256 )
  private String comment;

  protected ChangeInfo() {
  }

  protected ChangeInfo(final OwnerFullName owner, final String displayName) {
    super(owner, displayName);
  }

  public static ChangeInfo create(
      final OwnerFullName owner,
      final String displayName,
      final String comment
  ) {
    final ChangeInfo changeInfo = new ChangeInfo(owner, displayName);
    changeInfo.setState(Status.PENDING);
    changeInfo.setComment(comment);
    return changeInfo;
  }

  public static ChangeInfo exampleWithOwner(final OwnerFullName owner) {
    return new ChangeInfo(owner, null);
  }

  public static ChangeInfo exampleWithName(final OwnerFullName owner, final String name) {
    return new ChangeInfo(owner, name);
  }

  public static ChangeInfo exampleWithState(final ChangeInfo.Status state) {
    final ChangeInfo example = new ChangeInfo( );
    example.setState( state );
    example.setLastState( null );
    example.setStateChangeStack( null );
    return example;
  }

  @Override
  public String getComment( ) {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  @Override
  public String getPartition() {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor("euca")
        .region(ComponentIds.lookup(Route53.class).name())
        .namespace(this.getOwnerAccountNumber())
        .relativeId("change", getDisplayName());
  }
}
