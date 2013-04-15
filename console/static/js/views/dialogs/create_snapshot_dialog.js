define([
   './eucadialogview',
   'text!./create_snapshot_dialog.html!strip',
   'models/snapshot',
   'app',
   'backbone',
], function(EucaDialogView, template, Snapshot, App, Backbone) {
    return EucaDialogView.extend({


        // SET UP AUTOCOMPLETE FOR THE VOLUME INPUT BOX
        setupAutoComplete: function(args){
            var self = this;
            
            if( args.volume_id == undefined ){
              var vol_ids = [];
              App.data.volume.each(function(v){
                console.log("Volume ID: " + v.get('id') + "  Status:" + v.get('status'));
                vol_ids.push(v.get('id'));
              });

              var sorted = sortArray(vol_ids);
              console.log("Autocomplete Volume List: " + sorted);

              var $volumeSelector = this.$el.find('#snapshot-create-volume-id');
              $volumeSelector.autocomplete({
                source: sorted
              });
            }; 
        },

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                snapshot: new Snapshot(),
                item: {volume_id: args.volume_id, description: ''},

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                createButton: {
                  click: function() {
	            // GET THE INPUT FROM HTML VIEW
	            var volumeId = self.scope.item.volume_id;
	            var description = self.scope.item.description;
		    console.log("Selected Volume ID: " + volumeId);
		    console.log("Volume Description: " + description);

	            // CONSTRUCT AJAX CALL RESPONSE OPTIONS
	            var createAjaxCallResponse = {
	              success: function(data, response, jqXHR){   // AJAX CALL SUCCESS OPTION
		        console.log("Callback " + response + " for " + volumeId);
			if(data.results){
			  snapId = data.results.id;
			  notifySuccess(null, $.i18n.prop('snapshot_create_success', snapId, volumeId));    // XSS risk  -- Kyo 040713
			}else{
			  notifyError($.i18n.prop('snapshot_create_error', volumeId, undefined_error));     // XSS risk
			}
	              },
		      error: function(jqXHR, textStatus, errorThrown){  // AJAX CALL ERROR OPTION
		        console.log("Callback " + textStatus  + " for " + volumeId + " error: " + getErrorMessage(jqXHR));
			notifyError($.i18n.prop('snapshot_create_error', volumeId), getErrorMessage(jqXHR));                     // XSS risk
		      }
	            };

	            // PERFORM CREATE CALL OM THE MODEL
	            self.scope.snapshot = new Snapshot({volume_id: volumeId, description: description}); 
	            self.scope.snapshot.sync('create', self.scope.snapshot, createAjaxCallResponse);

	            // DISPLAY THE MODEL LIST FOR VOLUME AFTER THE DESTROY OPERATION
	            App.data.snapshot.each(function(item){
	              console.log("Snapshot After Create: " + item.toJSON().id);
	            });

	            // CLOSE THE DIALOG
	            self.close();
                  }
                }
            };

            this._do_init();

            this.setupAutoComplete(args);
        },
    });
});
