define([
   './eucadialogview',
   'text!./deletevolumedialog.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: 'Ignore me for now',
                items: args.items, 

                cancelButton: function() {
                  self.close();
                },

                deleteButton: function() {
                  require(['app'], function(app) {
                      // FOR EACH VOLUME IN THE LIST 'items'
                      _.each(self.scope.items, function(item) {
                        console.log("Volume Item: " + item);
                        // FIND THE MODEL THAT MATCHES THE VOLUME ID WITH 'item'
                        var match = app.data.volume.find(function(model){ return model.get('id') == item; });
//                        console.log("Volume Create Time: " + match.get('create_time'));

                        // PERFORM DELETE CALL OM THE MODEL
                        match.sync('delete', match, {
                          // IN CASE OF AJAX CALL'S SUCCESS
                          success: function(data, response, jqXHR){
                            console.log("Callback " + response + " for " + item);
                            if(data.results){
                              console.log("sync: Volume " +item+ " Deleted = " + data.results );
                              notifySuccess(null, $.i18n.prop('volume_create_success', DefaultEncoder().encodeForHTML(item)));  // incorrect prop
                            }else{
                              notifyError($.i18n.prop('delete_volume_error', DefaultEncoder().encodeForHTML(item)), undefined_error);  // incorrect prop
                            }
                          },
                          // IN CASE OD AJAX CALL'S ERROR
                          error: function(jqXHR, textStatus, errorThrown){
                            notifyError($.i18n.prop('delete_volume_error', DefaultEncoder().encodeForHTML(item)), getErrorMessage(jqXHR));  // incorrect prop 
                          }
                        });

                        // DESTORY THIS MODEL
                        match.destroy({wait: true});
                      });

                      // DISPLAY THE MODEL LIST FOR VOLUME AFTER THE DESTROY OPERATION
                      app.data.volume.each(function(item){
                        console.log("Volume After Delete: " + item.toJSON().id);
                      });
                  });

                  // CLOSE THE DIALOG
                  self.close();
                }
            }

            this._do_init();
        },
	});
});
