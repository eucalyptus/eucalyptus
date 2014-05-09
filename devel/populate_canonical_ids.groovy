#!/usr/bin/groovy

import com.eucalyptus.auth.entities.AccountEntity;

def runUpgrade() {
  return AccountEntity.AccountEntityUpgrade.INSTANCE.apply(AccountEntity.class);
}

return runUpgrade();
