function selectCheckboxChanged(context, dataTable) {
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
