define([
  './eucadialogview',
  'text!./delete_volume_dialog.html!strip',
  'app',
], function(EucaDialogView, template, App) {
  return EucaDialogView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;

      this.scope = {
        status: '',
        items: args.items, 

        cancelButton: {
          click: function() {
            self.close();
          }
        },
        deleteButton: {
          click: function() {
              // FOR EACH VOLUME IN THE LIST 'items'
              _.each(self.scope.items, function(item) {
                console.log("Volume Item: " + item);
                // FIND THE MODEL THAT MATCHES THE VOLUME ID WITH 'item'
                var match = App.data.volume.find(function(model){ return model.get('id') == item; });
                // CONSTRUCT THE AJAX RESPONSE OPTIONS
                var deleteAjaxCallResponse = {
                  success: function(data, response, jqXHR){   // AJAX CALL SUCCESS OPTION
                    console.log("Callback " + response);
                    if(data.results){
                      console.log("sync: Volume " +item+ " Deleted = " + data.results );
                      notifySuccess(null, $.i18n.prop('volume_create_success', DefaultEncoder().encodeForHTML(item)));  // incorrect prop; Need to be fixed  -- Kyo 040813
                    }else{
                      notifyError($.i18n.prop('delete_volume_error', DefaultEncoder().encodeForHTML(item)), undefined_error);  // incorrect prop
                    }
                  },
                  error: function(jqXHR, textStatus, errorThrown){   // AJAX CALL ERROR OPTION
                    notifyError($.i18n.prop('delete_volume_error', DefaultEncoder().encodeForHTML(item)), getErrorMessage(jqXHR));  // incorrect prop 
                  }
                };
                // PERFORM DELETE CALL OM THE MODEL NEED to handle multi-ajax and multi-notificaiton -- Kyo 040813
                match.sync('delete', match, deleteAjaxCallResponse);
                // DESTORY THIS MODEL
                match.destroy({wait: true});
              });
              // DISPLAY THE MODEL LIST FOR VOLUME AFTER THE DESTROY OPERATION -- FOR DEBUG
              App.data.volume.each(function(item){
                console.log("Volume After Delete: " + item.get('id'));
              });
              // CLOSE THE DIALOG
              self.close();
          }
        },
      }
      this._do_init();
    },
  });
});
