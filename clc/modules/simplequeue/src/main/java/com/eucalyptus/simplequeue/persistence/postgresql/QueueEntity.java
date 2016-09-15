package com.eucalyptus.simplequeue.persistence.postgresql;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by ethomas on 9/7/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_simplequeue" )
@Table( name = "queues", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "queue_name"},
  name = "queues_composite_key") )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class QueueEntity extends AbstractPersistent {
  @Column(name = "account_id", nullable = false)
  String accountId;
  @Column(name = "queue_name", nullable = false)
  String queueName;
  @Column(name = "attributes_json", nullable = false)
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String attributes;

  public QueueEntity() {
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAttributes() {
    return attributes;
  }

  public void setAttributes(String attributes) {
    this.attributes = attributes;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }
}
