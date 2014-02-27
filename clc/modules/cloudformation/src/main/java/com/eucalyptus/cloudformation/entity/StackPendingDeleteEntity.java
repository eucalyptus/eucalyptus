package com.eucalyptus.cloudformation.entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 2/26/14.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "template" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackPendingDeleteEntity {

  @Column(name = "account_id", nullable = false)
  String accountId;

  @Column(name = "stack_id", nullable = false )
  String stackId;

  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

}
