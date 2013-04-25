define([
   './eucadialogview',
   'text!./register_snapshot_dialog.html!strip',
   'models/snapshot',
   'app',
   'backbone',
], function(EucaDialogView, template, Snapshot, App, Backbone) {
    return EucaDialogView.extend({

        setupInputValidation: function(args){
            var self = this;
            this.template = template;

            // SETUP INPUT VALIDATOR
            self.scope.snapshot.on('change', function() {
              self.scope.error.clear();
              self.scope.error.set(self.scope.snapshot.validate());
              console.log("Validation Error: " + JSON.stringify(self.scope.error));
           });
        },

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                snapshot: new Snapshot({snapshot_id: args.item}),
                error: new Backbone.Model({}),

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                registerButton: {
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
		    var snapshotId = self.scope.snapshot.get('snapshot_id');
		    var name = self.scope.snapshot.get('name');
		    var description = self.scope.snapshot.get('description');
		    var isWindows = self.scope.snapshot.get('os') ? true : false;
		    console.log("Selected Snapshot ID: " + snapshotId);
		    console.log("Name: " + name);
		    console.log("Description: " + description);
		    console.log("isWindows: " + isWindows);

                    // CONSTRUCT AJAX CALL RESPONSE OPTIONS
                    var registerAjaxCallResponse = {
		      success: function(data, response, jqXHR){   // AJAX CALL SUCCESS OPTION
		        console.log("Callback " + response + " for " + snapshotId);
                        if ( data.results ) {
                          notifySuccess(null, $.i18n.prop('snapshot_register_success', snapshotId, data.results));   // XSS Risk -- Kyo 040813
                        }else{
                          notifyError($.i18n.prop('snapshot_register_error', snapshotId), undefined_error);   // XSS Risk
                        }
		      },
		      error: function(jqXHR, textStatus, errorThrown){  // AJAX CALL ERROR OPTION
		        console.log("Callback " + textStatus  + " for " + snapshotId + " error: " + getErrorMessage(jqXHR));
                        notifyError($.i18n.prop('snapshot_register_error', snapshotId), getErrorMessage(jqXHR));   // XSS Risk
		      }
                    };

		    // PERFORM REGISTER SNAPSHOT CALL OM THE MODEL
                    self.scope.snapshot.registerSnapshot(name, description, isWindows, registerAjaxCallResponse);

	           // DISPLAY THE SNAPSHOT'S STATUS -- FOR DEBUG
		   App.data.snapshot.each(function(item){
		     console.log("Snapshot After Register: " + item.toJSON().id);
	           });

	          // CLOSE THE DIALOG
	          self.close();
                }
              }
            };
            this._do_init();

            this.setupInputValidation(args);
          },
    });
});
