package com.eucalyptus.records;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

@Entity
@PersistenceContext( name = "eucalyptus_records" )
@Table( name = "records" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
@DiscriminatorColumn( name = "record_class", discriminatorType = DiscriminatorType.STRING )
public class BaseRecord<T> implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "record_id" )
  String id;
  @Column( name = "record_timestamp" )
  private Long timestamp;
  @Column( name = "record_type" )
  @Enumerated(EnumType.STRING)
  private EventType type;
  @Column( name = "record_class" )
  @Enumerated(EnumType.STRING)
  private EventClass clazz;
  @Column( name = "record_creator" )
  private String creator;
  @Column( name = "record_code_location" )
  private String codeLocation;
  @Column( name = "record_user_id" )
  private String userId;
  @Column( name = "record_correlation_id" )
  private String correlationId;  
}
