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
 * Created by ethomas on 9/17/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_simplequeue" )
@Table( name = "messages", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "queue_name", "message_id"},
  name = "queues_composite_key") )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class MessageEntity extends AbstractPersistent {
  @Column(name = "account_id", nullable = false)
  String accountId;
  @Column(name = "queue_name", nullable = false)
  String queueName;
  @Column(name = "message_id", nullable = false)
  String messageId;
  @Column(name = "sent_timestamp", nullable = false)
  Long sentTimestamp = 0L;
  @Column(name = "visible_timestamp", nullable = false)
  Long visibleTimestamp = 0L; // when the message will be visible
  @Column(name = "expired_timestamp", nullable = false)
  Long expiredTimestamp = 0L; // when the message will expire
  @Column(name = "receive_count", nullable = false)
  Integer receiveCount = 0;
  @Column(name = "local_receive_count", nullable = false)
  Integer localReceiveCount = 0; //

/*
    ApproximateFirstReceiveTimestamp,
    ApproximateReceiveCount,
    SenderId,
    SentTimestamp

 */
  // In this case receipt handle = message_id + #times received (might not be best choice)

  @Column(name = "message_json", nullable = false)
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String messageJson;

  public MessageEntity() {
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getMessageJson() {
    return messageJson;
  }

  public void setMessageJson(String messageJson) {
    this.messageJson = messageJson;
  }


  public Long getSentTimestamp() {
    return sentTimestamp;
  }

  public void setSentTimestamp(Long sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
  }

  public Long getVisibleTimestamp() {
    return visibleTimestamp;
  }

  public void setVisibleTimestamp(Long visibleTimestamp) {
    this.visibleTimestamp = visibleTimestamp;
  }

  public Long getExpiredTimestamp() {
    return expiredTimestamp;
  }

  public void setExpiredTimestamp(Long expiredTimestamp) {
    this.expiredTimestamp = expiredTimestamp;
  }

  public Integer getReceiveCount() {
    return receiveCount;
  }

  public void setReceiveCount(Integer receiveCount) {
    this.receiveCount = receiveCount;
  }

  public Integer getLocalReceiveCount() {
    return localReceiveCount;
  }

  public void setLocalReceiveCount(Integer localReceiveCount) {
    this.localReceiveCount = localReceiveCount;
  }
}
