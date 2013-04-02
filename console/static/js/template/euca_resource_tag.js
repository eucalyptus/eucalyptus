

(function($, eucalyptus) {

  $.widget( "eucalyptus.euca_resource_tag", {

    // =============
    // CLASS OPTIONS
    // =============
 
    options: { 
      resource: null,             // cloud resource identifier
      resource_id: null,          // euca/aws ID for the resource
      saveButtonCallback: null,   // Callback for save button
      cancelButtonCallback: null, // Callback for cancel button 
      widgetMode: null,           // Mode: edit or view-only      
    },

    // ===============
    // CLASS VARIABLES
    // ===============

    baseTable: null,  // an element to hold the dataTable object    

    displayView: null,  // a variable to store the table's interaction mode    
    isFirstData: null,  // a variable to store the table's data state, edited or unedited
    totalTagCount: null, // a variable to store the total number of the resource tags
    strEditMessage: null, // a variable to store a string that describes the edit message

    uneditedData: null,
    createdTagList: null,
    deletedTagList: null,

    alteredRow: null,   // an element to store altered row information
    removedRow: null,   // an element to store removed row information 
    addedRow: null,    // an element to store newly added row information

    // ===============================
    // CREATE EUCA_RESOURCE_TAG WIDGET
    // ===============================

    _create: function() {
      var thisObj = this;

      thisObj.displayView = "view";   //The table is in 'view' mode by default
      thisObj.isFirstData = "true";   //The table is unedited
      thisObj.totalTagCount = 0;  
      thisObj.strEditMessage = "";  
 
      thisObj.uneditedData = [];
      thisObj.createdTagList = [];
      thisObj.deletedTagList = [];

      thisObj.alteredRow = $('div')[0];   //initialize an element to store 'altered row' data
      thisObj.removedRow = $('div')[1];   //initialize an element to store 'removed row' data
      thisObj.addedRow = $('div')[2];     //initialize an element to store 'added row' data

      var mainDiv = $('<div>').addClass('resource_tag_main_div_class').attr('id', 'resource_tag_main_div_id-' + thisObj.options.resource_id);
      var demo = $('<table>').addClass('resource_tag_datatable_class').attr('id', 'resource_tag_datatable_id-' + thisObj.options.resource_id);
      mainDiv.append(demo);
      thisObj.baseTable = demo;
      
      // ====================
      // INITIALIZE DATATABLE
      // ====================
      
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
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "bPaginate": false, 
          "bFilter": false ,
          "aoColumnDefs": [
            {
              "sTitle": "Key",
              "aTargets":[0],
              "mRender": function(data){
                 return DefaultEncoder().encodeForHTML(data);
	       },
              "mData": "name",
            },
            {
              "sTitle": "Value",
              "aTargets":[1],
              "mRender": function(data){
                 return DefaultEncoder().encodeForHTML(data);
	       },
               "mData": "value",
            },
            {
               "bVisible": false,
               "aTargets":[2],
               "mData": null,
               "sDefaultContent": "edited",
            },
          ],

          "fnRowCallback": function( nRow, aData ) {

            // =====================
            // COLOR CODE EDITED ROW
            // =====================

            var $nRow = $(nRow);
            var key = aData.name.toLowerCase();
            var edited = "false";
            if( aData.edited != undefined ){
               edited = aData.edited.toLowerCase();
            }
            if(edited == "true") {
              $nRow.css({"color":"red"});       // When modified, color it red
            }else if(key == "name") {
              $nRow.css({"color":"green"});     // Special Tag 'name' will be highlighted
            };
            return nRow;
          },

          "fnDrawCallback" : function() { 

             // ===============================================
             // SKIP THE PROCESS BELOW IF IT'S 'VIEW-ONLY' MODE
             // ===============================================

             if( thisObj.options.widgetMode == "view-only" ){
               return;
             };

             // =============================================================
             // RECIEVE AND STORE THE RESOURCE TAGS PRIOR TO THE MODIFICATION
             // =============================================================

             // Store the unedited row data here
             if( thisObj.isFirstData == "true" ){
               thisObj.uneditedData = [];
               $.each( thisObj.baseTable.fnGetData(), function(i, row){
                thisObj.uneditedData.push(row);           // Store the initial raw data per row
               });
               if( thisObj.uneditedData.length > 0 ){
                 console.log(thisObj.uneditedData);
                 thisObj.isFirstData = "false";           // Flip the Flag to store the initial Data in uneditedData storage
               };
             };

             // ==============================================
             // GO THROUGH EACH ROW AND CONSTRUCT EDIT BUTTONS
             // ==============================================

             // For each row of dataTable, add an extra column for the dual button. 
	     thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){

                 var $currentRow = $(tr);

                 // ==================================
                 // RETREIVE KEY AND VALUE ON THIS ROW
                 // ==================================

                 var key = $currentRow.children().first().text();   // Grab the key of the resource tag, which is the first 'td' item
                 var value = $currentRow.children().eq(1).text();   // Grab the value of the resource tag, which is the second 'td' item
                 
                  // Prevent the dataTable's sorting from altering the states of the table view
                  // In other words, do not run below, if the buttons are already added. 
                 if( !($currentRow.children().last().hasClass('resource-tag-table-extra-td')) ){              

                   // =============================
                   // CREATE NEW COLUMN FOR BUTTONS
                   // =============================
 
                   var keyForId = $.md5(key);   // Convert the key into MD5 hash value to be used as an identifier
                   var buttonSpan = $('<span>').addClass('dual-tag-button-span').attr('id', 'dualButtonSpan-'+keyForId);
                   var editButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-edit-'+keyForId).text('edit').hide();
                   var removeButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-remove-'+keyForId).text('remove').hide();

                   buttonSpan.append(editButton);
                   buttonSpan.append(removeButton);

                   var tdExtraColumn = $('<td>').addClass('resource-tag-table-extra-td').attr('id', 'extra-td');   // Create the extra column object
                   tdExtraColumn.append(buttonSpan);
                   $currentRow.append(tdExtraColumn);

                   // BIND for 'mouseenter' in this row
                   $currentRow.bind('mouseenter', function(e){
                     $currentRow.children().last().find('#dualButton-edit-'+keyForId+'.dual-tag-button').show();
                     $currentRow.children().last().find('#dualButton-remove-'+keyForId+'.dual-tag-button').show();
                   });

                   // BIND for 'mouseleave' in this row
                   $currentRow.bind('mouseleave', function(e){
                     $currentRow.children().last().find('#dualButton-edit-'+keyForId+'.dual-tag-button').hide();
                     $currentRow.children().last().find('#dualButton-remove-'+keyForId+'.dual-tag-button').hide();
                   });

                   // ================
                   // BIND EDIT BUTTON
                   // ================

                   editButton.bind('click', function(e){

                     thisObj.displayView = "edit";   // The table is in 'edit' mode
                     thisObj.baseTable.find('tr.resource-tag-input-row-tr').hide();   // Hide the add new tag display

                     thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){  // Re-display all the edit buttons
                       var $thisCurrentRow = $(tr);
                       $thisCurrentRow.children().show();
                       $thisCurrentRow.find('#extra-td.resource-tag-table-extra-td').hide();
                     });

                     // =========================
                     // CHANGE ROW TO INPUT BOXES
                     // =========================

                     // Replace the row with input boxes
                     var tdResourceTagEdit = $('<td>').html('<input name="tag_key" type="text" id="tag_key" size="128" value="'+key+'">');
                     tdResourceTagEdit.after($('<td>').html('<input name="tag_value" type="text" id="tag_value" size="256" value="'+value+'">'));

		     // Monitor the length of the input for key
		     tdResourceTagEdit.find('input#tag_key').keypress(function() {
		       if($(this).val().length >= 128) {
			 $(this).val($(this).val().slice(0, 128));
			 alert("Warnig: the length of the key cannot be longer than 128 chars.\nKey: " + $(this).val());
		       };
		       // TODO: It doen't appear to be regulating the final input length
		     });

		     // Monitor the length of the input for value
		     tdResourceTagEdit.find('input#tag_value').keypress(function() {
		       if($(this).val().length >= 256) {
			 $(this).val($(this).val().slice(0, 256));
			 alert("Warnig: the length of the key cannot be longer than 256 chars.\nValue: " + $(this).val());
		       };
		     });

                     // ========================================
                     // CREATE EDIT-DONE AND EDIT-CANCEL BUTTONS
                     // ========================================

                     var editButtonSpan = $('<span>').addClass('dual-tag-edit-button-span').attr('id', 'dualButtonSpan-'+keyForId);
                     var editdoneButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-editdone-'+keyForId).text('done').show();
                     var editcancelButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-editcancel-'+keyForId).text('cancel').show();

                     editButtonSpan.append(editdoneButton);
                     editButtonSpan.append(editcancelButton);
                     var tdExtraColumn = $('<td>').addClass('resource-tag-table-extra-td').attr('id', 'extra-td');   // Create the extra column object
                     tdExtraColumn.append(editButtonSpan);
                     tdResourceTagEdit.after(tdExtraColumn);

                     $currentRow.children().hide();
                     $currentRow.append(tdResourceTagEdit);

                     // =====================
                     // BIND EDIT-DONE BUTTON
                     // =====================

                     editdoneButton.bind('click', function(e){

                       thisObj.displayView = "view";   // The table is in 'view' mode

                       var newKey = tdResourceTagEdit.find('#tag_key').val();
                       var newValue = tdResourceTagEdit.find('#tag_value').val();
                       jQuery.data(thisObj.alteredRow, key, {key: newKey, value: newValue, prev_key: key, prev_value: value});
                       thisObj.baseTable.fnUpdate({"name": newKey, "value": newValue, "edited": "true"}, thisObj.baseTable.fnGetPosition($(this).closest("tr").get(0)));
                       tdResourceTagEdit.remove();
                       $currentRow.children().show();

                       key = newKey;   // Update the variable 'key' with the newly edited value
                       value = newValue;   // Update the variable 'value' with newlt edited value

                       thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){      // Re-display all the edit buttons
                         var $thisCurrentRow = $(tr);
                         $thisCurrentRow.children().show();
                         $thisCurrentRow.find('#extra-td.resource-tag-table-extra-td').show();
                       });

                       thisObj._updateAdditionalMessageDiv(thisObj.strEditMessage);   // Update the additional message div
                     });

                     // =======================
                     // BIND EDIT-CANCEL BUTTON
                     // =======================

                     editcancelButton.bind('click', function(e){

                       thisObj.displayView = "view";   // The table is in 'view' mode
                       tdResourceTagEdit.remove();  
                       
                       thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){      // Re-display all the edit buttons
                         var $thisCurrentRow = $(tr);
                         $thisCurrentRow.children().show();
                         $thisCurrentRow.find('#extra-td.resource-tag-table-extra-td').show();
                       }); 

                       thisObj.baseTable.find('tr.resource-tag-input-row-tr').show();   // Re-display the add new tag row  
                       
                       thisObj._updateAdditionalMessageDiv(thisObj.strEditMessage);   // Update the additional message div
                     });

                   });   // END of BIND for EDIT button

                   // ==================
                   // BIND REMOVE BUTTON
                   // ==================

                   removeButton.bind('click', function(e){

                     thisObj.displayView = "view";   // The table is in 'view' mode

                     jQuery.data(thisObj.removedRow, key, {key: key, value: value});
                     thisObj.baseTable.fnDeleteRow(thisObj.baseTable.fnGetPosition($(this).closest("tr").get(0)));
                     if( thisObj.strEditMessage == "" ){
                       thisObj.strEditMessage = "Deleted Row:";
                     };
                     thisObj.strEditMessage += " ["+key+"="+value+"]";
                     
                     thisObj._updateAdditionalMessageDiv(thisObj.strEditMessage);   // Update the additional message div
                   });

                 };  // END of IF statement for preventing additional button creation when sorted.

             });  // END of $.each for adding buttons per row

             // ============================
             // ATTACHING ADD NEW TAG BUTTON
             // ============================
             
             // Construct the last row of the table; a special case for the add-only tag row.
             var addNewTagButton = $('<a>').addClass('button single-tag-button').attr('id', 'singleButton-add').text('Add new tag');
             var tdResourceTag = $('<td>').html('<input name="added_tag_key" type="text" id="added_tag_key" size="128">');
             tdResourceTag.after($('<td>').html('<input name="added_tag_value" type="text" id="added_tag_value" size="256">'));
             tdResourceTag.after($('<td>').append(addNewTagButton));
             thisObj.baseTable.find('tr').last().after($('<tr>').addClass('resource-tag-input-row-tr').append(tdResourceTag));

             // Monitor the length of the input for key
             tdResourceTag.find('input#added_tag_key').keypress(function() {
               if($(this).val().length >= 128) {
                 $(this).val($(this).val().slice(0, 128));
                 alert("Warnig: the length of the key cannot be longer than 128 chars.\nKey: " + $(this).val());
               };
               // TODO: It doen't appear to be regulating the final input length
             });

             // Monitor the length of the input for value
             tdResourceTag.find('input#added_tag_value').keypress(function() {
               if($(this).val().length >= 256) {
                 $(this).val($(this).val().slice(0, 256));
                 alert("Warnig: the length of the key cannot be longer than 256 chars.\nValue: " + $(this).val());
               };
             });

             // BIND add new tag button 
             addNewTagButton.bind('click', function(e){
               var addedKey = tdResourceTag.find('#added_tag_key').val();
               var addedValue = tdResourceTag.find('#added_tag_value').val();
               thisObj.isFirstData = "false"; 
               jQuery.data(thisObj.addedRow, addedKey, {key: addedKey, value: addedValue});
               thisObj.baseTable.fnAddData({"name": addedKey, "value": addedValue, "edited": "true"});
              
               thisObj._updateAdditionalMessageDiv(thisObj.strEditMessage);   // Update the additional message div
             });

             // Get the total count of the resource tags 
             thisObj.totalTagCount = thisObj.baseTable.fnGetData().length;
             console.log("totalTagCount: " + thisObj.totalTagCount);

	     // If the table is in 'edit' mode or there exists 10 tags, hide the add new tag row
             if( thisObj.displayView != "view" || thisObj.totalTagCount >= 10){
               thisObj.baseTable.find('tr.resource-tag-input-row-tr').hide();       
             };

          },
     });

     // ======================================
     // APPEND DATATABLE INTO THE MAIN ELEMENT
     // ======================================
 
     // Add the initialized dataTable element into the main class element
     thisObj.element.append(thisObj.baseTable);

     // ===============================================
     // SKIP THE PROCESS BELOW IF IT'S 'VIEW-ONLY' MODE
     // ===============================================

     if( thisObj.options.widgetMode == "view-only" ){
       return;
     };

     // ==========================
     // Display Additional Message
     // ==========================

     var additionalMessage = $('<div>').addClass('resource-tag-additional-message').attr('id', 'tag-div-additional-message').text("");
     thisObj.element.append(additionalMessage); 

     // ====================================
     // Display Apply to Auto Scale Checkbox
     // ====================================

     var autoScaleCheckbox = $('<div>').addClass('resource-tag-auto-scale-checkbox').attr('id', 'tagCheckbox').html('<input type="checkbox"/> Also apply tags to instances in the scaling group');
     thisObj.element.append(autoScaleCheckbox);

     // ===========================
     // ADD SAVE AND CANCEL BUTTONS
     // ===========================

     // Add Save and Cancel buttons
     var saveButton = $('<a>').addClass('button single-tag-button').attr('id', 'tagButton-save').text('Save changes');
     var cancelButton = $('<a>').addClass('button single-tag-button').attr('id', 'tagButton-cancel').text('Cancel');
     var saveandcancelDiv = $('<div>').addClass('resource-tag-save-cancel-buttons');
     saveandcancelDiv.append(saveButton);
     saveandcancelDiv.append(cancelButton);
     thisObj.element.append(saveandcancelDiv);

     saveButton.bind('click', function(e){
       thisObj._clickedSaveButton();
       var callback = thisObj.options.saveButtonCallback;
       if ($.isFunction(callback)){
          callback();
       };
     });

     cancelButton.bind('click', function(e){
       thisObj._clickedCancelButton();
       var callback = thisObj.options.cancelButtonCallback;
       if ($.isFunction(callback)){
          callback();
       };
     });

    },    // END OF _CREATE()

    _updateAdditionalMessageDiv: function(str){
      var thisObj = this;
      thisObj.element.find('div#tag-div-additional-message').text(str).css({"color":"red"});
    },

    _clickedSaveButton: function(){
      var thisObj = this;
      thisObj._processEditedTags();
      jQuery.removeData(thisObj.alteredRow);   //remove 'altered row' data
      jQuery.removeData(thisObj.removedRow);   //remove 'removed row' data
      jQuery.removeData(thisObj.addedRow);     //remove 'added row' data

      thisObj.isFirstData = "true";            // Reset the Flag
      thisObj.strEditMessage = "";             // Reset the additional message string
      thisObj._updateAdditionalMessageDiv("Tag Updated Saved");
      thisObj.baseTable.fnReloadAjax();        // Reload the table
    },

    _clickedCancelButton: function(){
      var thisObj = this;
      jQuery.removeData(thisObj.alteredRow);   //remove 'altered row' data
      jQuery.removeData(thisObj.removedRow);   //remove 'removed row' data
      jQuery.removeData(thisObj.addedRow);     //remove 'added row' data

      thisObj.strEditMessage = "";             //Reset the additional message string
      thisObj._updateAdditionalMessageDiv("Tag Update Canceled");
      thisObj.baseTable.fnReloadAjax();        //Reload the table
    },

    _compareBeforeAndAfter: function(before, after){
      var thisObj = this;
      thisObj.createdTagList = [];
      thisObj.deletedTagList = [];

      // Look for any changes going from unedited table to edited table
      $.each(before, function(i, v){
        var beforeKey = v.name;
        var beforeValue = v.value;
        var isModed = "false";
        var isExist = "false";
        var newAfterValue = "";
        $.each(after, function(i, v){
          var afterKey = v.name;
          var afterValue = v.value;
          if( beforeKey == afterKey ){
             isExist = "true";
             if( beforeValue != afterValue ){
                isModed = "true";
                newAfterValue = afterValue;
            };
          };
        });
        if( isExist == "false" ){
           thisObj.deletedTagList.push({key: beforeKey, value: beforeValue});
        }else{
           if( isModed == "true" ){
           thisObj.deletedTagList.push({key: beforeKey, value: beforeValue});
           thisObj.createdTagList.push({key: beforeKey, value: newAfterValue});
           };
        };
      });

      // Look for newly created keys
      $.each(after, function(i, v){
          var afterKey = v.name;
          var afterValue = v.value;
          var isExistBefore = "false";
          $.each(before, function(i, v){
             var beforeKey = v.name;
             if( beforeKey == afterKey ){
               isExistBefore = "true";
             };
          });
          if( isExistBefore == "false" ){
            thisObj.createdTagList.push({key: afterKey, value: afterValue});
          };
      });

    },

    _processEditedTags: function(){
      var thisObj = this;

      var editedData = [];
      $.each( thisObj.baseTable.fnGetData(), function(i, row){
         editedData.push(row);
      });
      console.log(editedData);

      thisObj._compareBeforeAndAfter(thisObj.uneditedData, editedData);

      $.each(thisObj.deletedTagList, function(i, v){
         console.log("Deleted Tag: " + v.key + "=" + v.value);
         thisObj._makeCall_deleteTag(thisObj.options.resource_id, v.key, v.value);
      });

      $.each(thisObj.createdTagList, function(i, v){
         console.log("Created Tag: " + v.key + "=" + v.value);
         thisObj._makeCall_createTag(thisObj.options.resource_id, v.key, v.value);
      });

/*
      // Process Added Rows
      var addedRowData = jQuery.data(thisObj.addedRow);   // Retreive the added row data
      for(thisKey in addedRowData) {
        var thisData = addedRowData[thisKey];
        if( thisData.key != null ){   // if the stored data contains sub key-value pair,
           var newKey = jQuery.data(thisObj.addedRow, thisKey).key;
           var newValue = jQuery.data(thisObj.addedRow, thisKey).value;
           thisObj._makeCall_createTag(thisObj.options.resource_id, newKey, newValue);
        };
      };

      // Process Deleted Rows
      var removedRowData = jQuery.data(thisObj.removedRow);   // Retreive the removed row data
      for(thisKey in removedRowData) {
        var thisData = removedRowData[thisKey];
        if( thisData.key != null ){   // if the stored data contains sub key-value pair,
           var deletedKey = jQuery.data(thisObj.removedRow, thisKey).key;
           var deletedValue = jQuery.data(thisObj.removedRow, thisKey).value;
           thisObj._makeCall_deleteTag(thisObj.options.resource_id, deletedKey, deletedValue);
        };
      };

      // Process Altered Rows
      var alteredRowData = jQuery.data(thisObj.alteredRow);   // Retreive the removed row data
      for(thisKey in alteredRowData) {
        var thisData = alteredRowData[thisKey];
        if( thisData.key != null ){   // if the stored data contains sub key-value pair,
           var prevKey = jQuery.data(thisObj.alteredRow, thisKey).prev_key;
           var prevValue = jQuery.data(thisObj.alteredRow, thisKey).prev_value;
           var alteredKey = jQuery.data(thisObj.alteredRow, thisKey).key;
           var alteredValue = jQuery.data(thisObj.alteredRow, thisKey).value;
           thisObj._makeCall_alteredTag(thisObj.options.resource_id, prevKey, prevValue, alteredKey, alteredValue);
        };
      };
*/
    },

    _makeCall_createTag : function (resource_id, key, value) {
      var thisObj = this;
      $.ajax({
        type:"POST",
        url:"/ec2?Action=CreateTags",
        data:"_xsrf="+$.cookie('_xsrf')+"&ResourceId.1="+resource_id+"&Tag.1.Key="+key+"&Tag.1.Value="+value,
        dataType:"json",
        cache:false,
        async: false,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('eip_associate_success', resource_id, "["+key+"="+value+"]"));
            } else {
              notifyError($.i18n.prop('eip_associate_error', resource_id, "["+key+"="+value+"]"), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('eip_associate_error', resource_id, "["+key+"="+value+"]"), getErrorMessage(jqXHR));
          }
      });
    },

    _makeCall_deleteTag : function (resource_id, key, value) {
      var thisObj = this;
      $.ajax({
        type:"POST",
        url:"/ec2?Action=DeleteTags",
        data:"_xsrf="+$.cookie('_xsrf')+"&ResourceId.1="+resource_id+"&Tag.1.Key="+key+"&Tag.1.Value="+value,
        dataType:"json",
        cache:false,
        async: false,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('eip_disassociate_success', resource_id, "["+key+"="+value+"]"));
            } else {
              notifyError($.i18n.prop('eip_disassociate_error', resource_id, "["+key+"="+value+"]"), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('eip_disassociate_error', resource_id, "["+key+"="+value+"]"), getErrorMessage(jqXHR));
          }
      });
    },

    _makeCall_alteredTag : function (resource_id, prev_key, prev_value, new_key, new_value) {
      var thisObj = this;
      $.ajax({
        type:"POST",
        url:"/ec2?Action=DeleteTags",
        data:"_xsrf="+$.cookie('_xsrf')+"&ResourceId.1="+resource_id+"&Tag.1.Key="+prev_key+"&Tag.1.Value="+prev_value,
        dataType:"json",
        cache:false,
        async: false,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              thisObj._makeCall_createTag(resource_id, new_key, new_value);   // Make Ajax Call to Create Tag
            } else {
              notifyError($.i18n.prop('eip_disassociate_error', resource_id, "["+new_key+"="+new_value+"]"), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('eip_disassociate_error', resource_id, "["+new_key+"="+new_value+"]"), getErrorMessage(jqXHR));
          }
      });
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

