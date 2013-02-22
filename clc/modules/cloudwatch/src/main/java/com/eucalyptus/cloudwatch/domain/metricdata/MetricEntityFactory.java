package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Collection;

import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.google.common.collect.ImmutableSet;

public class MetricEntityFactory {
  private static MetricFactoryDelegate delegate = new MetricFactoryDelegate16();
//  private static MetricFactoryDelegate delegate = new MetricFactoryDelegateSingle();
  public static MetricEntity getNewMetricEntity(String hash) {
    return delegate.getNewMetricEntity(hash);
  }
  public static Class getClassForEntitiesGet(String hash) {
    return delegate.getClassForEntitiesGet(hash);
  }

  public static Collection<Class> getAllClassesForEntitiesGet() {
    return delegate.getAllClassesForEntitiesGet();
  }
  public interface MetricFactoryDelegate {
    public MetricEntity getNewMetricEntity(String hash); 
    public Class getClassForEntitiesGet(String hash);
    public Collection<Class> getAllClassesForEntitiesGet();
  }
/*
  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntitySingle extends MetricEntity {
    public MetricEntitySingle() {
      super();
    }
  }
*/
/*
  public static class MetricFactoryDelegateSingle implements MetricFactoryDelegate {

    @Override
    public MetricEntity getNewMetricEntity(String hash) {
      return new MetricEntitySingle();
    }

    @Override
    public Class getClassForEntitiesGet(String hash) {
      return MetricEntitySingle.class;
    }

    @Override
    public Collection<Class> getAllClassesForEntitiesGet() {
      return ImmutableSet.<Class>of(MetricEntitySingle.class);
    }


  }
*/
  public static class MetricFactoryDelegate16 implements MetricFactoryDelegate {

    @Override
    public MetricEntity getNewMetricEntity(String hash) {
      if (hash == null) throw new IllegalArgumentException("Invalid hash");
      switch (hash.charAt(0)) {
      case '0':
        return new MetricEntity0();
      case '1':
        return new MetricEntity1();
      case '2':
        return new MetricEntity2();
      case '3':
        return new MetricEntity3();
      case '4':
        return new MetricEntity4();
      case '5':
        return new MetricEntity5();
      case '6':
        return new MetricEntity6();
      case '7':
        return new MetricEntity7();
      case '8':
        return new MetricEntity8();
      case '9':
        return new MetricEntity9();
      case 'A':
      case 'a':
        return new MetricEntityA();
      case 'B':
      case 'b':
        return new MetricEntityB();
      case 'C':
      case 'c':
        return new MetricEntityC();
      case 'D':
      case 'd':
        return new MetricEntityD();
      case 'E':
      case 'e':
        return new MetricEntityE();
      case 'F':
      case 'f':
        return new MetricEntityF();
      default:
        throw new IllegalArgumentException("Illegal hash " + hash);
      }
    }

    @Override
    public Class getClassForEntitiesGet(String hash) {
      if (hash == null) throw new IllegalArgumentException("Invalid hash");
      switch (hash.charAt(0)) {
      case '0':
        return MetricEntity0.class;
      case '1':
        return MetricEntity1.class;
      case '2':
        return MetricEntity2.class;
      case '3':
        return MetricEntity3.class;
      case '4':
        return MetricEntity4.class;
      case '5':
        return MetricEntity5.class;
      case '6':
        return MetricEntity6.class;
      case '7':
        return MetricEntity7.class;
      case '8':
        return MetricEntity8.class;
      case '9':
        return MetricEntity9.class;
      case 'A':
      case 'a':
        return MetricEntityA.class;
      case 'B':
      case 'b':
        return MetricEntityB.class;
      case 'C':
      case 'c':
        return MetricEntityC.class;
      case 'D':
      case 'd':
        return MetricEntityD.class;
      case 'E':
      case 'e':
        return MetricEntityE.class;
      case 'F':
      case 'f':
        return MetricEntityF.class;
      default:
        throw new IllegalArgumentException("Illegal hash " + hash);
      }
    }

    @Override
    public Collection<Class> getAllClassesForEntitiesGet() {
      // TODO Auto-generated method stub
      return ImmutableSet.<Class> of(MetricEntity0.class, MetricEntity1.class,
          MetricEntity2.class, MetricEntity3.class, MetricEntity4.class,
          MetricEntity5.class, MetricEntity6.class, MetricEntity7.class,
          MetricEntity8.class, MetricEntity9.class, MetricEntityA.class,
          MetricEntityB.class, MetricEntityC.class, MetricEntityD.class,
          MetricEntityE.class, MetricEntityF.class);
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_0")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity0 extends MetricEntity {
    public MetricEntity0() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_1")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity1 extends MetricEntity {
    public MetricEntity1() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_2")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity2 extends MetricEntity {
    public MetricEntity2() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_3")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity3 extends MetricEntity {
    public MetricEntity3() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_4")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity4 extends MetricEntity {
    public MetricEntity4() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_5")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity5 extends MetricEntity {
    public MetricEntity5() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_6")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity6 extends MetricEntity {
    public MetricEntity6() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_7")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity7 extends MetricEntity {
    public MetricEntity7() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_8")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity8 extends MetricEntity {
    public MetricEntity8() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_9")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntity9 extends MetricEntity {
    public MetricEntity9() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_a")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntityA extends MetricEntity {
    public MetricEntityA() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_b")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntityB extends MetricEntity {
    public MetricEntityB() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_c")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntityC extends MetricEntity {
    public MetricEntityC() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_d")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntityD extends MetricEntity {
    public MetricEntityD() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_e")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntityE extends MetricEntity {
    public MetricEntityE() {
      super();
    }
  }

  @Entity @javax.persistence.Entity
  @PersistenceContext(name="eucalyptus_cloudwatch")
  @Table(name="metric_data_f")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class MetricEntityF extends MetricEntity {
    public MetricEntityF() {
      super();
    }
  }


}
