function selectCheckboxChanged(context, tableName) {
  dataTable = allTablesRef[tableName];
  switch(context.checked) {
    case true:
      var rows = dataTable.fnGetVisiableTrNodes();
      for (i = 0; i<rows.length; i++) {
        cb = rows[i].firstChild.firstChild;
        if (cb != null) cb.checked = true;
      }
      break;
    case false:
      var rows = dataTable.fnGetVisiableTrNodes();
      for (i = 0; i<rows.length; i++) {
        cb = rows[i].firstChild.firstChild;
        if (cb != null) cb.checked = false;
      }
      break;
  }
}

function deleteAction(tableName) {
  // hide menu
  $menuUl = $("div.table_" + tableName + "_top div.euca-table-action ul");
  $menuUl.removeClass('activemenu');

  // find all selected rows
  dataTable = allTablesRef[tableName];
  var rows = dataTable.fnGetVisiableTrNodes();
  var rowsToDelete = [];
  for (i = 0; i<rows.length; i++) {
    cb = rows[i].firstChild.firstChild;
    if (cb != null && cb.checked == true) {
      if (rows[i].childNodes[1] != null)
        rowsToDelete.push(rows[i].childNodes[1].firstChild.nodeValue);
    }
  }

  if (rowsToDelete.length == 0) {
    // nothing to do
    // should we show a warning?
  } else {
    // show delete dialog box
    $deleteNames = $("#" + tableName + "-delete-names");
    $deleteNames.html('');
    for (i = 0; i<rowsToDelete.length; i++) {
      t = $('<div/>').text(rowsToDelete[i]).html();
      $deleteNames.append(t).append("<br/>");
    }
    $("#" + tableName + "-delete-dialog").dialog('open');
  }
}
