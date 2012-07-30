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

function getAllSelectedRows(tableName, idIndex) {
  dataTable = allTablesRef[tableName];
  var rows = dataTable.fnGetVisiableTrNodes();
  var rowsToDelete = [];
  for (i = 0; i<rows.length; i++) {
    cb = rows[i].firstChild.firstChild;
    if (cb != null && cb.checked == true) {
      if (rows[i].childNodes[idIndex] != null)
        rowsToDelete.push(rows[i].childNodes[idIndex].firstChild.nodeValue);
    }
  }
  return rowsToDelete;
}

function escapeHTML(input) {
  return $('<div/>').text(input).html();
}

//TODO: pass in index id
function deleteAction(tableName) {
  // hide menu
  $menuUl = $("div.table_" + tableName + "_top div.euca-table-action ul");
  $menuUl.removeClass('activemenu');

  var rowsToDelete = getAllSelectedRows(tableName, 1);

  if (rowsToDelete.length == 0) {
    // nothing to do
    // should we show a warning?
  } else {
    // show delete dialog box
    $deleteNames = $("#" + tableName + "-delete-names");
    $deleteNames.html('');
    for (i = 0; i<rowsToDelete.length; i++) {
      t = escapeHTML(rowsToDelete[i]);
      $deleteNames.append(t).append("<br/>");
    }
    $("#" + tableName + "-delete-dialog").dialog('open');
  }
}

function deleteSelectedKeyPairs() {
  var rowsToDelete = getAllSelectedRows('keys', 1);
  for (i = 0; i<rowsToDelete.length; i++) {
    keyName = rowsToDelete[i];
    $.ajax({
      type:"GET",
      url:"/ec2?type=key&Action=DeleteKeyPair",
      data:"_xsrf="+$.cookie('_xsrf') + "&KeyName=" + keyName,
      dataType:"json",
      async:"true",
      success:
        function(data, textStatus, jqXHR){
          if (data.results && data.results == true) {
            successNotification("Deleted keypair " + keyName);
            //TODO: refresh table once
            allTablesRef['keys'].fnReloadAjax();
          } else {
            errorNotification("Failed to delte keypair " + keyName);
          }
        },
      error:
        function(jqXHR, textStatus, errorThrown){
          //TODO: show communication error?
          errorNotification("Failed to delete keypair " + keyName);
        }
    });
  }
}

function addKeyPair(keyName) {
  $.ajax({
    type:"GET",
    url:"/ec2?type=key&Action=CreateKeyPair",
    data:"_xsrf="+$.cookie('_xsrf') + "&KeyName=" + keyName,
    dataType:"json",
    async:"false",
    success:
      function(data, textStatus, jqXHR){
         if (data.results && data.results.material) {
           $.generateFile({
             filename    : keyName,
             content     : data.results.material,
             script      : '/support?Action=DownloadFile&_xsrf=' + $.cookie('_xsrf')
           });
           // TODO: can we wait till file is saved by user?
           successNotification("Added keypair " + keyName);
           // refresh table
           allTablesRef['keys'].fnReloadAjax();
         } else {
           errorNotification("Failed to create keypair " + keyName);
         }
      },
    error:
      function(jqXHR, textStatus, errorThrown){
        //TODO: show communication error?
        errorNotification("Failed to create keypair " + keyName);
      }
  });
}

function S4() {
   return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

function hideNotification(notification) {
  $("#"+notification).detach();
}

function successNotification(message) {
  var nId = "n-"+ S4() + "-" + S4();
  $('#euca-notification-container').append('<div class="success" id="'+ nId + '"><span>' + escapeHTML(message) + '</span><a class="hide-notification" href="#" onclick="hideNotification(\'' + nId + '\');">X</a></div>');
  // hide after 30 sec
  setTimeout(function(){ hideNotification(nId) }, 1000 * 30);
}

function errorNotification(message) {
  var nId = "n-"+ S4() + "-" + S4();
  $('#euca-notification-container').append('<div class="error" id="'+ nId + '"><span>' + escapeHTML(message) + '</span><a class="hide-notification" href="#" onclick="hideNotification(\'' + nId + '\');">X</a></div>');
}
