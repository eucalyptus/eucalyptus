package com.eucalyptus.bootstrap;

import java.util.List;

public interface Bootstrapper {
  public String getVersion();
  public boolean check();
  public boolean destroy();
  public boolean stop();
  public boolean start();
  public boolean load();
  public List<String> getDependencies();
}