

(function($, eucalyptus) {

  $.widget( "eucalyptus.euca_resource_tag", {
 
    // These options will be used as defaults
    options: { 
      resource: null,
      resource_id: null,
    },
 
    // Set up the widget
    _create: function() {
      var thisObj = this;
      var mainDiv = $('<div>').addClass('resource_tag__main_div_class').attr('id', 'resource_tag_main_div_id')
      mainDiv.text('RESOURCE TAG PLACE HOLDER ::: RESOURCE: ' + this.options.resource + " RESOURCE_ID: " + this.options.resource_id);
      thisObj.element.append(mainDiv);
      thisObj._getAllResourceTags();
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

    _getAllResourceTags: function(){
      var thisObj = this;
      $.ajax({
          type:"POST",
          url:"/ec2?Action=DescribeInstances",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if(data.results){
              var message = "";
              var exists_instance_id = 0;
              $.each(data.results, function(idx, instance){
                message += instance.id + " ";
                if( thisObj.options.resource_id == instance.id)
                  exists_instance_id = 1;
              });
              if(exists_instance_id == 1){
                notifySuccess(null, message);
//                thisObj.tableWrapper.eucatable('refreshTable');
                thisObj._setOption('resource_id', thisObj.options.resource_id + '-MOD');
              }else{
                notifyError('failed to find the instance id');
              }
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

}( jQuery ) );

