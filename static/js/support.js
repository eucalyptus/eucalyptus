function selectMenuChangeFunc(context, dataTable) {
  var val = $("#" + context.id + " option:selected").val();
  switch (val) {
    case "select":
      $("#" + context.id).val("none");
      var rows = dataTable.fnGetVisiableTrNodes();
      for (i = 0; i<rows.length; i++) {
        cb = rows[i].firstChild.firstChild;
        if (cb != null) cb.checked = true;
      }
      break;
    case "deselect":
      $("#selectMenu").val("none");
      var rows = dataTable.fnGetVisiableTrNodes();
      for (i = 0; i<rows.length; i++) {
        cb = rows[i].firstChild.firstChild;
        if (cb != null) cb.checked = false;
      }
      break;
  }
}
/*
function selectMenuChangeFunc() {
       var val = $("#selectMenu option:selected").val();
       switch (val) {
         case "select":
           $("#selectMenu").val("none");
           var nNodes = oDataTable.fnGetVisiableTrNodes();
           for (i = 0; i<nNodes.length; i++) {
             cb = nNodes[i].firstChild.firstChild;
             if (cb != null) cb.checked = true;
           }
           break;
         case "deselect":
           $("#selectMenu").val("none");
           var nNodes = oDataTable.fnGetVisiableTrNodes();
           for (i = 0; i<nNodes.length; i++) {
             cb = nNodes[i].firstChild.firstChild;
             if (cb != null) cb.checked = false;
           }
           break;
       }
     }
*/
