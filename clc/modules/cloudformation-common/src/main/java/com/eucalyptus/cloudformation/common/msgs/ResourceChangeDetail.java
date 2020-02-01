/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResourceChangeDetail extends EucalyptusData {

  private String causingEntity;

  @FieldRegex(FieldRegexValue.ENUM_CHANGESOURCE)
  private String changeSource;

  @FieldRegex(FieldRegexValue.ENUM_EVALUATIONTYPE)
  private String evaluation;

  private ResourceTargetDefinition target;

  public String getCausingEntity() {
    return causingEntity;
  }

  public void setCausingEntity(final String causingEntity) {
    this.causingEntity = causingEntity;
  }

  public String getChangeSource() {
    return changeSource;
  }

  public void setChangeSource(final String changeSource) {
    this.changeSource = changeSource;
  }

  public String getEvaluation() {
    return evaluation;
  }

  public void setEvaluation(final String evaluation) {
    this.evaluation = evaluation;
  }

  public ResourceTargetDefinition getTarget() {
    return target;
  }

  public void setTarget(final ResourceTargetDefinition target) {
    this.target = target;
  }

}
