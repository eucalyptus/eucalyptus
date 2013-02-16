

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
/*              "fnRender": function(oObj) {
                if( oObj.aData.type === "html" )
                  return asHTML(oObj.aData.name);
                else
		  return DefaultEncoder().encodeForHTML(oObj.aData.name);
	       },
*/
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
            },
            {
               "sTitle": "Button",
               "fnRender": function(oObj) {
                 var dualButton = $('<a>').addClass('dual-tag-button').attr('id', 'dualButton-'+oObj.aData.name).text(image_launch_btn);
                 return asHTML(dualButton);
               },
            },
          ],

          "fnDrawCallback" : function() { 
             tdResourceTag = $('<td>').html('<input name="tag_key" type="text" id="tag_key" size="128">');
             tdResourceTag.append($('<td>').html('<input name="tag_value" type="text" id="tag_value" size="128">'));
             tdResourceTag.append($('<td>').html($('<a>').addClass('button table-button').attr('id', 'addTagButton').text(image_launch_btn)));
             var trResourceTag = $('<tr>').addClass('resource-tag-input-row-tr').html(tdResourceTag.html());
             var message = "";
             thisObj.baseTable.find('tr').each(function(index, tr){
                 var $currentRow = $(tr);
                 var allTds = thisObj.baseTable.fnGetTds($currentRow[0]);      
                 $(allTds).each(function(){ 
                    message += $(this).html() + " | ";
                  });
                 message += " && "; 	
              });
              thisObj.baseTable.find('tr').last().after(trResourceTag);

              thisObj.baseTable.find('tbody').delegate('.dual-tag-button', 'click', function(e){
                var iPos = thisObj.baseTable.fnGetPosition( $(this).closest("tr").get(0) );
                if(iPos!=null){
                  var aData = thisObj.baseTable.fnGetData( iPos ); //get data of the clicked row
                  var key = aData[0]; // get 'key' of the row
                  var value = aData[1]; //get 'value' of the row
      //            thisObj.baseTable.find('#dualButton'+key+'.dual-tag-button').text('hi');
                  thisObj.baseTable.fnUpdate("Item Updated", iPos, 0);
       //           thisObj.baseTable.fnDeleteRow(iPos);
       //           thisObj.baseTable.fnClearTable(this);           
                 alert('hover on ' + key);
                };
        //        alert("hi " + $(this).parent().parent().text());
              });

/*
              thisObj.baseTable.find("tbody").delegate("tr", "click", function() {
                var iPos = thisObj.baseTable.fnGetPosition( this );
                if(iPos!=null){
                  var aData = thisObj.baseTable.fnGetData( iPos ); //get data of the clicked row
                  var key = aData[0]; // get 'key' of the row
                  var value = aData[1]; //get 'value' of the row
                  //thisObj.baseTable.fnDeleteRow(iPos);//delete row
                  alert("clicked : Row "+key+"="+value+" "+$(this).text());
                };
              });
*/
       //       alert("hello " + thisObj.options.resource_id + " " + message);
          },
     });
/*     demo.fnAddData([
       {
         "name": '<input name="tag_key" type="text" id="tag_key" size="128">',
         "value": '<input name="tag_value" type="text" id="tag_value" size="256">',
         "type": "html"
      }
     ]);
*/
     thisObj.element.append(thisObj.baseTable);
//     alert("hello! ");
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

