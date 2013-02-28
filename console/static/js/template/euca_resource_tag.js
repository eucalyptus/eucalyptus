

(function($, eucalyptus) {

  $.widget( "eucalyptus.euca_resource_tag", {
 
    options: { 
      resource: null,
      resource_id: null,
      tag_data: null,
    },

    baseTable: null,
    alteredRow: null,
    removedRow: null,
    addedRow: null,
 
    // Set up the widget
    _create: function() {
      var thisObj = this;

      thisObj.alteredRow = $('div')[0];   //initialize an element to store 'altered row' data
      thisObj.removedRow = $('div')[1];   //initialize an element to store 'removed row' data
      thisObj.addedRow = $('div')[2];     //initialize an element to store 'added row' data

      var mainDiv = $('<div>').addClass('resource_tag_main_div_class').attr('id', 'resource_tag_main_div_id-' + thisObj.options.resource_id);
      var demo = $('<table>').addClass('resource_tag_datatable_class').attr('id', 'resource_tag_datatable_id-' + thisObj.options.resource_id);
      mainDiv.append(demo);
      thisObj.baseTable = demo;
      // dataTable Initialization
      thisObj.baseTable.dataTable({
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeTags",
          "fnServerData": function (sSource, aoData, fnCallback) {
                $.ajax( {
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": "_xsrf="+$.cookie('_xsrf')+"&Filter.1.Name=resource-id&Filter.1.Value.1="+thisObj.options.resource_id,
                    "success": fnCallback
                });
          },
/*          "fnServerParams": function (aoData) {
               var addedRowData = jQuery.data(thisObj.addedRow);   // Retreive the added row data
               for(thisKey in addedRowData) {
                 var thisData = addedRowData[thisKey];
                 if( thisData.key != null ){
                    aoData.push({"name": thisData.key, "value": thisData.value });
                    alert("pushed: "+thisData.key);
                 };
               };
          },
*/          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "bPaginate": false, 
          "bFilter": false ,
          "aoColumns": [
            {
              "sTitle": "Key",
              "fnRender": function(oObj) {
 /*               if( jQuery.data(thisObj.alteredRow, oObj.aData.name) != null )
                  return jQuery.data(thisObj.alteredRow, oObj.aData.name).key;
                else
*/		  return DefaultEncoder().encodeForHTML(oObj.aData.name);
	       },
              "mDataProp": "name",
            },
            {
              "sTitle": "Value",
              "fnRender": function(oObj) {
/*                if( jQuery.data(thisObj.removedRow, oObj.aData.name) != null )
                  return '<del>' + DefaultEncoder().encodeForHTML(oObj.aData.value) + '</del>';
                else if( jQuery.data(thisObj.alteredRow, oObj.aData.name) != null )
                  return jQuery.data(thisObj.alteredRow, oObj.aData.name).value;
                else
*/		  return DefaultEncoder().encodeForHTML(oObj.aData.value);
	       },
               "mDataProp": "value",
            },
          ],

          "fnDrawCallback" : function() { 

             // Create more rows in the dataTable for newly added resource tags
/*             var addedRowData = jQuery.data(thisObj.addedRow);   // Retreive the added row data
             for(thisKey in addedRowData) { 
                var thisData = addedRowData[thisKey];
                if( thisData.key != null ){   // if the stored data contains sub key-value pair,
                  var tdAddedResourceTag = $('<td>').html('<font color="blue">'+jQuery.data(thisObj.addedRow, thisKey).key+'</font>');
                  tdAddedResourceTag.after($('<td>').html('<font color="blue">'+jQuery.data(thisObj.addedRow, thisKey).value+'</font>'));
                  thisObj.baseTable.find('tr').last().after($('<tr>').addClass('resource-tag-added-row-tr').append(tdAddedResourceTag));   // Add the new row at the end of the table
                };
             };
*/
             // For each row of dataTable, add an extra column for the dual button. 
	     thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){
                 var $currentRow = $(tr);
                 var key = $currentRow.children().first().text();   // Grab the key of the resource tag, which is the first 'td' item
                 var value = $currentRow.children().eq(1).text();   // Grab the value of the resource tag, which is the second 'td' item
                 
                  // Prevent the dataTable's sorting from altering the states of the table view               
                 if( !($currentRow.children().last().hasClass('resource-tag-table-extra-td')) ){              
 
                   var keyForId = key.replace(' ', '-');
                   var buttonSpan = $('<span>').addClass('dual-tag-button-span').attr('id', 'dualButtonSpan-'+keyForId);
                   var editButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-edit-'+keyForId).text('edit').hide();
                   var removeButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-remove-'+keyForId).text('remove').hide();

                   buttonSpan.append(editButton);
                   buttonSpan.append(removeButton);

                   var tdExtraColumn = $('<td>').addClass('resource-tag-table-extra-td').attr('id', 'extra-td');   // Create the extra column object
                   tdExtraColumn.append(buttonSpan);
                   $currentRow.append(tdExtraColumn);

                   $currentRow.bind('mouseenter', function(e){
                     $currentRow.children().last().find('#dualButton-edit-'+keyForId+'.dual-tag-button').show();
                     $currentRow.children().last().find('#dualButton-remove-'+keyForId+'.dual-tag-button').show();
                   });

                   $currentRow.bind('mouseleave', function(e){
                     $currentRow.children().last().find('#dualButton-edit-'+keyForId+'.dual-tag-button').hide();
                     $currentRow.children().last().find('#dualButton-remove-'+keyForId+'.dual-tag-button').hide();
                   });

                   editButton.bind('click', function(e){

                     thisObj.baseTable.find('tr.resource-tag-input-row-tr').hide();   // Hide the add new tag display

                     thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){  // Re-display all the edit buttons
                       var $thisCurrentRow = $(tr);
                       $thisCurrentRow.children().show();
                       $thisCurrentRow.find('#extra-td.resource-tag-table-extra-td').hide();
                     });

                     var tdResourceTagEdit = $('<td>').html('<input name="tag_key" type="text" id="tag_key" size="128" value="'+key+'">');
                     tdResourceTagEdit.after($('<td>').html('<input name="tag_value" type="text" id="tag_value" size="256" value="'+value+'">'));
                     
                     var editButtonSpan = $('<span>').addClass('dual-tag-edit-button-span').attr('id', 'dualButtonSpan-'+keyForId);
                     var editdoneButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-editdone-'+keyForId).text('done').show();
                     var editcancelButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-editcancel-'+keyForId).text('cancel').show();

                     editButtonSpan.append(editdoneButton);
                     editButtonSpan.append(editcancelButton);
                     var tdExtraColumn = $('<td>').addClass('resource-tag-table-extra-td').attr('id', 'extra-td');   // Create the extra column object
                     tdExtraColumn.append(editButtonSpan);
                     tdResourceTagEdit.after(tdExtraColumn);

        //             $currentRow.text('');
                     $currentRow.children().hide();
                     $currentRow.append(tdResourceTagEdit);

                     editdoneButton.bind('click', function(e){
                       var newKey = tdResourceTagEdit.find('#tag_key').val();
                       var newValue = tdResourceTagEdit.find('#tag_value').val();
                       jQuery.data(thisObj.alteredRow, key, {key: newKey, value: newValue});
                       thisObj.baseTable.fnUpdate({"name": newKey, "value": newValue}, thisObj.baseTable.fnGetPosition($(this).closest("tr").get(0)));
                       tdResourceTagEdit.remove();
                       $currentRow.children().show();
//                       thisObj.baseTable.fnReloadAjax();

                       thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){      // Re-display all the edit buttons
                         var $thisCurrentRow = $(tr);
                         $thisCurrentRow.children().show();
                         $thisCurrentRow.find('#extra-td.resource-tag-table-extra-td').show();
                       });

                     });

                     editcancelButton.bind('click', function(e){
   /*                    var tdResourceTagEdit = $('<td>').text(key);
                       tdResourceTagEdit.after($('<td>').text(value));
                       $currentRow.text('');
                       $currentRow.append(tdResourceTagEdit);
   */                  
                       tdResourceTagEdit.remove();  
          //             $currentRow.children().show();

                       thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){      // Re-display all the edit buttons
                         var $thisCurrentRow = $(tr);
                         $thisCurrentRow.children().show();
                         $thisCurrentRow.find('#extra-td.resource-tag-table-extra-td').show();
                       }); 

//                       thisObj.baseTable.fnReloadAjax();
                       thisObj.baseTable.find('tr.resource-tag-input-row-tr').show();   // Re-display the add new tag row  
                     });

                   });

                   removeButton.bind('click', function(e){
                     jQuery.data(thisObj.removedRow, key, {key: key, value: value});
                     thisObj.baseTable.fnDeleteRow(thisObj.baseTable.fnGetPosition($(this).closest("tr").get(0)));
        //             thisObj.baseTable.fnReloadAjax();
                   });
                 };
             });

             // Construct the last row of the table; a special case for the add-only tag row.
             var addNewTagButton = $('<a>').addClass('button single-tag-button').attr('id', 'singleButton-add').text('Add new tag');
             var tdResourceTag = $('<td>').html('<input name="added_tag_key" type="text" id="added_tag_key" size="128">');
             tdResourceTag.after($('<td>').html('<input name="added_tag_value" type="text" id="added_tag_value" size="256">'));
             tdResourceTag.after($('<td>').append(addNewTagButton));
             thisObj.baseTable.find('tr').last().after($('<tr>').addClass('resource-tag-input-row-tr').append(tdResourceTag));

             addNewTagButton.bind('click', function(e){
               var addedKey = tdResourceTag.find('#added_tag_key').val();
               var addedValue = tdResourceTag.find('#added_tag_value').val();
               jQuery.data(thisObj.addedRow, addedKey, {key: addedKey, value: addedValue});
               thisObj.baseTable.fnAddData({"name": addedKey, "value": addedValue});
          //     thisObj.baseTable.fnReloadAjax();
             });

          },
     });

     // Add the initialized dataTable element into the main class element
     thisObj.element.append(thisObj.baseTable);
    },

    // Use the _setOption method to respond to changes to options
    _setOption: function( key, value ) {
      switch( key ) {
        case "resource":
          this.options.resource = value;
          break;
        case "resource_id":
          this.options.resource_id = value;
          break;
      }
      // In jQuery UI 1.8, you have to manually invoke the _setOption method from the base widget
      $.Widget.prototype._setOption.apply( this, arguments );
      // In jQuery UI 1.9 and above, you use the _super method instead
      this._super( "_setOption", key, value );
    },

    _renderResourceTags: function(){
       var thisObj = this;
       var mainResourceTagDiv = thisObj.element.find('#resource_tag_main_div_id.resource_tag_main_div_class');
       // Clean up the default text
       mainResourceTagDiv.text("");
       // Append p block for a few sentences on resource tagging
       mainResourceTagDiv.append($('<p>').text('RESOURCE: ' + this.options.resource + " RESOURCE_ID: " + this.options.resource_id));
       // Create a table
       var tableResourceTag = $('<table>').addClass('resource-tag-table');
       // Create a header
       var trHeadResourceTag = $('<tr>').addClass('resource-tag-table-tr');
       trHeadResourceTag.append($('<th>').text("INDEX")); 
       trHeadResourceTag.append($('<th>').text("KEY"));
       trHeadResourceTag.append($('<th>').text("VALUE"));
       trHeadResourceTag.append($('<th>').text("BUTTON"));
       tableResourceTag.append(trHeadResourceTag);
       var tag_count = 0;
       // if tag_data exists
       if(thisObj.options.tag_data){
              $.each(thisObj.options.tag_data, function(idx, tag){
                var trResourceTag = $('<tr>').addClass('resource-tag-table-tr');
                trResourceTag.append($('<td>').text("TAG" + idx));
                trResourceTag.append($('<td>').text(tag.name));
                trResourceTag.append($('<td>').text(tag.value));
                trResourceTag.append($('<td>').text("button"));
                tableResourceTag.append(trResourceTag);
                tag_count++;
              });
       };
	var trResourceTag = $('<tr>').addClass('resource-tag-table-tr');
	trResourceTag.append($('<td>').text("TAG" + tag_count));
	trResourceTag.append($('<td>').html('<input name="tag_key" type="text" id="tag_key" size="128">'));
	trResourceTag.append($('<td>').html('<input name="tag_value" type="text" id="tag_value" size="128">'));
        trResourceTag.append($('<td>').text("button"));
	tableResourceTag.append(trResourceTag);
        // Append the table to the main resource tag div
        mainResourceTagDiv.append(tableResourceTag);
 //      alert("_renderResourceTags: " + message);
    },

    _getAllResourceTags: function(){
      var thisObj = this;
      $.ajax({
          type:"POST",
          url:"/ec2?Action=DescribeTags",
          data:"_xsrf="+$.cookie('_xsrf')+"&Filter.1.Name=resource-id&Filter.1.Value.1="+thisObj.options.resource_id,
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if(data.results){
              var message = "";
              $.each(data.results, function(idx, tag){
		message += idx + "::";
                for(key in tag) {
                  message += key + "=" + tag[key] + "&";
                };
                message +="::";
              });
//              notifySuccess(null, message);
              thisObj._setOption('tag_data', data.results);
              thisObj._renderResourceTags();
            }else
              notifyError('no data results returned');
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError(getErrorMessage(jqXHR));
          }
        });
    },

    //
    // PUBLIC METHODS 
    //

    // Use the destroy method to clean up any modifications your widget has made to the DOM
    destroy: function() {
      // In jQuery UI 1.8, you must invoke the destroy method from the base widget
      $.Widget.prototype.destroy.call( this );
      // In jQuery UI 1.9 and above, you would define _destroy instead of destroy and not call the base method
    },

  });

})( jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {} );

