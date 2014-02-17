/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.google.common.collect.ImmutableSet;

public class MetricEntityFactory {
  private static MetricFactoryDelegate delegate = new MetricFactoryDelegate32();

  // private static MetricFactoryDelegate delegate = new
  // MetricFactoryDelegateSingle();
  public static MetricEntity getNewMetricEntity(MetricType metricType,
      String hash) {
    return delegate.getNewMetricEntity(metricType, hash);
  }

  public static Class getClassForEntitiesGet(MetricType metricType, String hash) {
    return delegate.getClassForEntitiesGet(metricType, hash);
  }

  public static Collection<Class> getAllClassesForEntitiesGet(
      MetricType metricType) {
    return delegate.getAllClassesForEntitiesGet(metricType);
  }

  public static Collection<Class> getAllClassesForEntitiesGet() {
    return delegate.getAllClassesForEntitiesGet();
  }

  public interface MetricFactoryDelegate {
    public MetricEntity getNewMetricEntity(MetricType metricType, String hash);

    public Class getClassForEntitiesGet(MetricType metricType, String hash);

    public Collection<Class> getAllClassesForEntitiesGet(MetricType metricType);

    public Collection<Class> getAllClassesForEntitiesGet();
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "metric_data")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class MetricEntitySingle extends MetricEntity {
    public MetricEntitySingle() {
      super();
    }
  }

  public static class MetricFactoryDelegateSingle implements
      MetricFactoryDelegate {

    @Override
    public MetricEntity getNewMetricEntity(MetricType metricType, String hash) {
      return new MetricEntitySingle();
    }

    @Override
    public Class getClassForEntitiesGet(MetricType metricType, String hash) {
      return MetricEntitySingle.class;
    }

    @Override
    public Collection<Class> getAllClassesForEntitiesGet(MetricType metricType) {
      return ImmutableSet.<Class> of(MetricEntitySingle.class);
    }

    @Override
    public Collection<Class> getAllClassesForEntitiesGet() {
      return ImmutableSet.<Class> of(MetricEntitySingle.class);
    }
  }

  public static class MetricFactoryDelegate32 implements MetricFactoryDelegate {

    @Override
    public MetricEntity getNewMetricEntity(MetricType metricType, String hash) {
      if (hash == null)
        throw new IllegalArgumentException("Invalid hash");
      if (metricType == null)
        throw new IllegalArgumentException("Invalid metricType");
      switch (metricType) {
        case System: {
          switch (hash.charAt(0)) {
            case '0':
              return new SystemMetricEntity0();
            case '1':
              return new SystemMetricEntity1();
            case '2':
              return new SystemMetricEntity2();
            case '3':
              return new SystemMetricEntity3();
            case '4':
              return new SystemMetricEntity4();
            case '5':
              return new SystemMetricEntity5();
            case '6':
              return new SystemMetricEntity6();
            case '7':
              return new SystemMetricEntity7();
            case '8':
              return new SystemMetricEntity8();
            case '9':
              return new SystemMetricEntity9();
            case 'A':
            case 'a':
              return new SystemMetricEntityA();
            case 'B':
            case 'b':
              return new SystemMetricEntityB();
            case 'C':
            case 'c':
              return new SystemMetricEntityC();
            case 'D':
            case 'd':
              return new SystemMetricEntityD();
            case 'E':
            case 'e':
              return new SystemMetricEntityE();
            case 'F':
            case 'f':
              return new SystemMetricEntityF();
            default:
              throw new IllegalArgumentException("Illegal hash " + hash);
          }
        }
        case Custom: {
          switch (hash.charAt(0)) {
            case '0':
              return new CustomMetricEntity0();
            case '1':
              return new CustomMetricEntity1();
            case '2':
              return new CustomMetricEntity2();
            case '3':
              return new CustomMetricEntity3();
            case '4':
              return new CustomMetricEntity4();
            case '5':
              return new CustomMetricEntity5();
            case '6':
              return new CustomMetricEntity6();
            case '7':
              return new CustomMetricEntity7();
            case '8':
              return new CustomMetricEntity8();
            case '9':
              return new CustomMetricEntity9();
            case 'A':
            case 'a':
              return new CustomMetricEntityA();
            case 'B':
            case 'b':
              return new CustomMetricEntityB();
            case 'C':
            case 'c':
              return new CustomMetricEntityC();
            case 'D':
            case 'd':
              return new CustomMetricEntityD();
            case 'E':
            case 'e':
              return new CustomMetricEntityE();
            case 'F':
            case 'f':
              return new CustomMetricEntityF();
            default:
              throw new IllegalArgumentException("Illegal hash " + hash);
          }
        }
        default: {
          throw new IllegalArgumentException("Invalid metric type");
        }
      }
    }

    @Override
    public Class getClassForEntitiesGet(MetricType metricType, String hash) {
      if (hash == null)
        throw new IllegalArgumentException("Invalid hash");
      switch (metricType) {
        case System: {
          switch (hash.charAt(0)) {
            case '0':
              return SystemMetricEntity0.class;
            case '1':
              return SystemMetricEntity1.class;
            case '2':
              return SystemMetricEntity2.class;
            case '3':
              return SystemMetricEntity3.class;
            case '4':
              return SystemMetricEntity4.class;
            case '5':
              return SystemMetricEntity5.class;
            case '6':
              return SystemMetricEntity6.class;
            case '7':
              return SystemMetricEntity7.class;
            case '8':
              return SystemMetricEntity8.class;
            case '9':
              return SystemMetricEntity9.class;
            case 'A':
            case 'a':
              return SystemMetricEntityA.class;
            case 'B':
            case 'b':
              return SystemMetricEntityB.class;
            case 'C':
            case 'c':
              return SystemMetricEntityC.class;
            case 'D':
            case 'd':
              return SystemMetricEntityD.class;
            case 'E':
            case 'e':
              return SystemMetricEntityE.class;
            case 'F':
            case 'f':
              return SystemMetricEntityF.class;
            default:
              throw new IllegalArgumentException("Illegal hash " + hash);
          }
        }
        case Custom: {
          switch (hash.charAt(0)) {
            case '0':
              return CustomMetricEntity0.class;
            case '1':
              return CustomMetricEntity1.class;
            case '2':
              return CustomMetricEntity2.class;
            case '3':
              return CustomMetricEntity3.class;
            case '4':
              return CustomMetricEntity4.class;
            case '5':
              return CustomMetricEntity5.class;
            case '6':
              return CustomMetricEntity6.class;
            case '7':
              return CustomMetricEntity7.class;
            case '8':
              return CustomMetricEntity8.class;
            case '9':
              return CustomMetricEntity9.class;
            case 'A':
            case 'a':
              return CustomMetricEntityA.class;
            case 'B':
            case 'b':
              return CustomMetricEntityB.class;
            case 'C':
            case 'c':
              return CustomMetricEntityC.class;
            case 'D':
            case 'd':
              return CustomMetricEntityD.class;
            case 'E':
            case 'e':
              return CustomMetricEntityE.class;
            case 'F':
            case 'f':
              return CustomMetricEntityF.class;
            default:
              throw new IllegalArgumentException("Illegal hash " + hash);
          }
        }
        default: {
          throw new IllegalArgumentException("Invalid metric type");
        }
      }
    }

    @Override
    public Collection<Class> getAllClassesForEntitiesGet(MetricType metricType) {
      switch (metricType) {
        case System: {
          return ImmutableSet.<Class> of(SystemMetricEntity0.class,
              SystemMetricEntity1.class, SystemMetricEntity2.class,
              SystemMetricEntity3.class, SystemMetricEntity4.class,
              SystemMetricEntity5.class, SystemMetricEntity6.class,
              SystemMetricEntity7.class, SystemMetricEntity8.class,
              SystemMetricEntity9.class, SystemMetricEntityA.class,
              SystemMetricEntityB.class, SystemMetricEntityC.class,
              SystemMetricEntityD.class, SystemMetricEntityE.class,
              SystemMetricEntityF.class);
        }
        case Custom: {
          return ImmutableSet.<Class> of(CustomMetricEntity0.class,
              CustomMetricEntity1.class, CustomMetricEntity2.class,
              CustomMetricEntity3.class, CustomMetricEntity4.class,
              CustomMetricEntity5.class, CustomMetricEntity6.class,
              CustomMetricEntity7.class, CustomMetricEntity8.class,
              CustomMetricEntity9.class, CustomMetricEntityA.class,
              CustomMetricEntityB.class, CustomMetricEntityC.class,
              CustomMetricEntityD.class, CustomMetricEntityE.class,
              CustomMetricEntityF.class);
        }
        default: {
          throw new IllegalArgumentException("Invalid metric type");
        }
      }
    }

    @Override
    public Collection<Class> getAllClassesForEntitiesGet() {
      // TODO Auto-generated method stub
      return ImmutableSet.<Class> of(SystemMetricEntity0.class,
          SystemMetricEntity1.class, SystemMetricEntity2.class,
          SystemMetricEntity3.class, SystemMetricEntity4.class,
          SystemMetricEntity5.class, SystemMetricEntity6.class,
          SystemMetricEntity7.class, SystemMetricEntity8.class,
          SystemMetricEntity9.class, SystemMetricEntityA.class,
          SystemMetricEntityB.class, SystemMetricEntityC.class,
          SystemMetricEntityD.class, SystemMetricEntityE.class,
          SystemMetricEntityF.class, CustomMetricEntity0.class,
          CustomMetricEntity1.class, CustomMetricEntity2.class,
          CustomMetricEntity3.class, CustomMetricEntity4.class,
          CustomMetricEntity5.class, CustomMetricEntity6.class,
          CustomMetricEntity7.class, CustomMetricEntity8.class,
          CustomMetricEntity9.class, CustomMetricEntityA.class,
          CustomMetricEntityB.class, CustomMetricEntityC.class,
          CustomMetricEntityD.class, CustomMetricEntityE.class,
          CustomMetricEntityF.class);
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_0")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity0 extends MetricEntity {
    public CustomMetricEntity0() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_1")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity1 extends MetricEntity {
    public CustomMetricEntity1() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_2")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity2 extends MetricEntity {
    public CustomMetricEntity2() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_3")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity3 extends MetricEntity {
    public CustomMetricEntity3() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_4")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity4 extends MetricEntity {
    public CustomMetricEntity4() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_5")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity5 extends MetricEntity {
    public CustomMetricEntity5() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_6")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity6 extends MetricEntity {
    public CustomMetricEntity6() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_7")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity7 extends MetricEntity {
    public CustomMetricEntity7() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_8")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity8 extends MetricEntity {
    public CustomMetricEntity8() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_9")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntity9 extends MetricEntity {
    public CustomMetricEntity9() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_a")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntityA extends MetricEntity {
    public CustomMetricEntityA() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_b")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntityB extends MetricEntity {
    public CustomMetricEntityB() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_c")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntityC extends MetricEntity {
    public CustomMetricEntityC() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_d")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntityD extends MetricEntity {
    public CustomMetricEntityD() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_e")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntityE extends MetricEntity {
    public CustomMetricEntityE() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "custom_metric_data_f")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class CustomMetricEntityF extends MetricEntity {
    public CustomMetricEntityF() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_0")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity0 extends MetricEntity {
    public SystemMetricEntity0() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_1")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity1 extends MetricEntity {
    public SystemMetricEntity1() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_2")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity2 extends MetricEntity {
    public SystemMetricEntity2() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_3")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity3 extends MetricEntity {
    public SystemMetricEntity3() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_4")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity4 extends MetricEntity {
    public SystemMetricEntity4() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_5")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity5 extends MetricEntity {
    public SystemMetricEntity5() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_6")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity6 extends MetricEntity {
    public SystemMetricEntity6() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_7")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity7 extends MetricEntity {
    public SystemMetricEntity7() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_8")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity8 extends MetricEntity {
    public SystemMetricEntity8() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_9")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntity9 extends MetricEntity {
    public SystemMetricEntity9() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_a")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntityA extends MetricEntity {
    public SystemMetricEntityA() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_b")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntityB extends MetricEntity {
    public SystemMetricEntityB() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_c")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntityC extends MetricEntity {
    public SystemMetricEntityC() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_d")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntityD extends MetricEntity {
    public SystemMetricEntityD() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_e")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntityE extends MetricEntity {
    public SystemMetricEntityE() {
      super();
    }
  }

  @Entity
  @PersistenceContext(name = "eucalyptus_cloudwatch")
  @Table(name = "System_metric_data_f")
  @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
  public static class SystemMetricEntityF extends MetricEntity {
    public SystemMetricEntityF() {
      super();
    }
  }

}
