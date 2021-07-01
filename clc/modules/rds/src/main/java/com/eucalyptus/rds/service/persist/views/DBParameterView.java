/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import com.eucalyptus.util.CompatPredicate;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 *
 */
@Value.Immutable
public interface DBParameterView {

  enum ApplyMethod {
    immediate,
    pending_reboot,
    ;

    @SuppressWarnings("unused")
    public static ApplyMethod fromString(final String value) {
      return DBParameterView.ApplyMethod.valueOf( value.replace('-', '_') );
    }

    public String toString() {
      return name().replace('_', '-');
    }
  }

  enum Source implements CompatPredicate<DBParameterView> {
    /**
     * User specified (customized)
     */
    user,

    /**
     * RDS system defaults
     */
    system,

    /**
     * Database engine default value
     */
    engine_default,
    ;

    @SuppressWarnings("unused")
    public static Source fromString(final String value) {
      return DBParameterView.Source.valueOf( value.replace('-', '_') );
    }

    public String toString() {
      return name().replace('_', '-');
    }

    @Override
    public boolean apply(@Nullable DBParameterView parameterView) {
      return parameterView != null && this.equals(parameterView.getSource());
    }
  }


  String getAllowedValues();

  ApplyMethod getApplyMethod();

  String getApplyType();

  String getDataType();

  String getDescription();

  Boolean isModifiable();

  String getMinimumEngineVersion();

  String getParameterName();

  String getParameterValue();

  Source getSource();

}
