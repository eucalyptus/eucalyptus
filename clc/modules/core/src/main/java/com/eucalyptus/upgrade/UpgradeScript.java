package com.eucalyptus.upgrade;

import java.io.File;

public interface UpgradeScript {
  public Boolean accepts( String from, String to );
  public void upgrade( File oldEucaHome, File newEucaHome );
}