package com.eucalyptus.webui.client.view;

import java.util.ArrayList;

public interface ValueSaveHandler {

  void saveValue( ArrayList<String> keys, ArrayList<HasValueWidget> values );
  
}
