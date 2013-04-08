define([
   './eucadialogview',
   'text!./createsnapshotdialog.html!strip',
   'models/snapshot',
], function(EucaDialogView, template, Snapshot) {
    return EucaDialogView.extend({

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: 'Ignore me for now',
                snapshot: new Snapshot({volume_id: args.volume_id, description: 'empty'}),

                cancelButton: function() {
                  self.close();
                },

                createButton: function() {
                        var volumeId = self.scope.snapshot.get('volume_id');
                        var description = self.scope.snapshot.get('description');
                        console.log("Selected Volume ID: " + volumeId);
                        console.log("Volume Description: " + description);
                        // CREATE NEW SNAPSHOT MODEL WITH 'volume' and 'description'
//                        self.scope.snapshot.save();

                        // PERFORM CREATE CALL OM THE MODEL
                        self.scope.snapshot.sync('create', self.scope.snapshot, {
                          // IN CASE OF AJAX CALL'S SUCCESS
                          success: function(data, response, jqXHR){
                            console.log("Callback " + response + " for " + volumeId);
                            if(data.results){
                              snapId = data.results.id;
                              notifySuccess(null, $.i18n.prop('snapshot_create_success', snapId, volumeId));    // XSS risk  -- Kyo 040713
                            }else{
                              notifyError($.i18n.prop('snapshot_create_error', volumeId, undefined_error));     // XSS risk
                            }
                          },
                          // IN CASE OD AJAX CALL'S ERROR
                          error: function(jqXHR, textStatus, errorThrown){
                            console.log("Callback " + textStatus  + " for " + volumeId + " error: " + getErrorMessage(jqXHR));
                            notifyError($.i18n.prop('snapshot_create_error', volumeId), getErrorMessage(jqXHR));                     // XSS risk
                          }
                        });

                      // DISPLAY THE MODEL LIST FOR VOLUME AFTER THE DESTROY OPERATION
                      require(['app'], function(app) {
                        app.data.snapshot.each(function(item){
                          console.log("Snapshot After Create: " + item.toJSON().id);
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
