package com.eucalyptus.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;


@MappedSuperclass
public class AbstractPersistent implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Version
  @Column(name = "version")
  Integer version = 0;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_timestamp")
  Date lastUpdate;
}

@MappedSuperclass
public abstract class UserMetadata extends AbstractPersistent implements Serializable {
  @Column( name = "metadata_user_name" )
  String userName;
  @Column( name = "metadata_display_name" )
  String displayName;
}

@Entity
@Table( name = "metadata_keypair" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SshKeyPair extends UserMetadata implements Serializable {
  @Lob
  @Column( name = "metadata_keypair_public_key" )
  private String publicKey;
  @Column( name = "metadata_keypair_finger_print" )
  private String fingerPrint;
  
}
