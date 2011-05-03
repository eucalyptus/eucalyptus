package com.eucalyptus.webui.client.view;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.EucalyptusWebInterface;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class SampleTable extends Composite {

  private static Logger LOG = Logger.getLogger( "SampleTable" );
  
  private static class Contact {
    private final String address;
    private final Date birthday;
    private final String name;

    public Contact(String name, Date birthday, String address) {
      this.name = name;
      this.birthday = birthday;
      this.address = address;
    }
  }

  /**
   * The list of data to display.
   */
  private static final List<Contact> CONTACTS = Arrays.asList(
      new Contact("John", new Date(80, 4, 12), "123 Fourth Avenue"),
      new Contact("Joe", new Date(85, 2, 22), "22 Lance Ln"),
      new Contact("George", new Date(46, 6, 6), "1600 Pennsylvania Avenue"));
  
  public SampleTable ( ) {
    // Create a CellTable.
    CellTable<Contact> table = new CellTable<Contact>();
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);

    // Add a text column to show the name.
    TextColumn<Contact> nameColumn = new TextColumn<Contact>() {
      @Override
      public String getValue(Contact object) {
        return object.name;
      }
    };
    table.addColumn(nameColumn, "Name");

    // Add a date column to show the birthday.
    DateCell dateCell = new DateCell();
    Column<Contact, Date> dateColumn = new Column<Contact, Date>(dateCell) {
      @Override
      public Date getValue(Contact object) {
        return object.birthday;
      }
    };
    table.addColumn(dateColumn, "Birthday");

    // Add a text column to show the address.
    TextColumn<Contact> addressColumn = new TextColumn<Contact>() {
      @Override
      public String getValue(Contact object) {
        return object.address;
      }
    };
    table.addColumn(addressColumn, "Address");

    // Add a selection model to handle user selection.
    final SingleSelectionModel<Contact> selectionModel = new SingleSelectionModel<Contact>();
    table.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        Contact selected = selectionModel.getSelectedObject();
        if (selected != null) {
          EucalyptusWebInterface.shell.openDetail( );
          LOG.log(Level.INFO, "You selected: " + selected.name);
        }
      }
    });

    // Set the total row count. This isn't strictly necessary, but it affects
    // paging calculations, so its good habit to keep the row count up to date.
    table.setRowCount(CONTACTS.size(), true);

    // Push the data into the widget.
    table.setRowData(0, CONTACTS);
    
    initWidget( table );
  }
  
}
