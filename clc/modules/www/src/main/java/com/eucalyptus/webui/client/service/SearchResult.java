package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import java.util.ArrayList;

public class SearchResult implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private Integer idColumn;
  private ArrayList<DataFieldDesc> descs = new ArrayList<DataFieldDesc>( );
  private ArrayList<DataRow> rows = new ArrayList<DataRow>( );
  
  public SearchResult( ) {
    this.idColumn = 0;
  }

  public void setDescs( ArrayList<DataFieldDesc> descs ) {
    this.descs = descs;
  }

  public ArrayList<DataFieldDesc> getDescs( ) {
    return descs;
  }

  public void setRows( ArrayList<DataRow> rows ) {
    this.rows = rows;
  }

  public ArrayList<DataRow> getRows( ) {
    return rows;
  }

  public void setIdColumn( Integer idColumn ) {
    this.idColumn = idColumn;
  }

  public Integer getIdColumn( ) {
    return idColumn;
  }
  
  
  
}
