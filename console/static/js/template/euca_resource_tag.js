

(function($, eucalyptus) {

  $.widget( "eucalyptus.euca_resource_tag", {
 
    options: { 
      resource: null,
      resource_id: null,
      tag_data: null,
    },
    baseTable: null,
 
    // Set up the widget
    _create: function() {
      var thisObj = this;
      var mainDiv = $('<div>').addClass('resource_tag_main_div_class').attr('id', 'resource_tag_main_div_id-' + thisObj.options.resource_id);
//      mainDiv.text('RESOURCE TAG PLACE HOLDER ::: RESOURCE: ' + this.options.resource + " RESOURCE_ID: " + this.options.resource_id);
//      thisObj.element.append(mainDiv);
//      thisObj._getAllResourceTags();
      var demo = $('<table>').addClass('resource_tag_datatable_class').attr('id', 'resource_tag_datatable_id-' + thisObj.options.resource_id);
      mainDiv.append(demo);
      thisObj.baseTable = demo;
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
          "aoColumns": [
            {
              "sTitle": "Key",
              "fnRender": function(oObj) {
                if( oObj.aData.type === "html" )
                  return asHTML(oObj.aData.name);
                else
		  return DefaultEncoder().encodeForHTML(oObj.aData.name);
	       },
              "mDataProp": "name",
            },
            {
              "sTitle": "Value",
              "fnRender": function(oObj) {
                if( oObj.aData.type === "html" )
                  return asHTML(oObj.aData.value);
                else
		  return DefaultEncoder().encodeForHTML(oObj.aData.value);
	       },
               "mDataProp": "value",
            },
          ],

          "fnDrawCallback" : function() { 
            
             // For each row of dataTable, add an extra column for the dual button. 
	     thisObj.baseTable.find('tbody').find('tr').each(function(index, tr){
                 var $currentRow = $(tr);
                 var key = $currentRow.children().first().text();   // Grab the key of the tag, which is the first 'tr' item
                 var $dualButtonRow = $currentRow.children().last().text();  // Grab the last column's text value
                 if( $dualButtonRow != 'edit' ){                             // If the dual button column was rendered previously, skip the step below. 
                   var dualButton = $('<span>').addClass('dual-tag-button-span').attr('id', 'dualButtonSpan-'+key);
                   var editButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-edit-'+key).text('edit');
                   editButton.bind('click', function(e){
                     alert('clicked on ' + key);
                   });
                 
                   dualButton.append(editButton);
                   dualButton.bind('mouseenter', function(e){
                     var removeButton = $('<a>').addClass('button dual-tag-button').attr('id', 'dualButton-remove-'+key).text('remove');
                     removeButton.bind('click', function(e){
                       alert('clicked to remove ' + key);
                     });
                     dualButton.append(removeButton);
                   });
                   dualButton.bind('mouseleave', function(e){
                     dualButton.find('#dualButton-remove-'+key+'.dual-tag-button').remove();
                   }); 
                   // Attach the new column that contains the dualButton for this row
                   var newTd = $('<td>').addClass('dual-tag-button-td').attr('id', 'dualButtonTd-'+key).append(dualButton);
                   $currentRow.children().last().after(newTd);
                 };
             });

             // Construct the last row of the table; a special case for the add-only tag row.
             tdResourceTag = $('<td>').html('<input name="tag_key" type="text" id="tag_key" size="128">');
             tdResourceTag.after($('<td>').html('<input name="tag_value" type="text" id="tag_value" size="256">'));
             tdResourceTag.after($('<td>').append($('<a>').addClass('button single-tag-button').attr('id', 'singleButton-add').text('add')));
             thisObj.baseTable.find('tr').last().after($('<tr>').addClass('resource-tag-input-row-tr').append(tdResourceTag));
          },
     });
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

