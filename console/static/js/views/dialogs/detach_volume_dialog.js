define([
   './eucadialogview',
   'text!./detach_volume_dialog.html!strip',
   'app',
], function(EucaDialogView, template, App) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: 'Ignore me for now',
                items: function(){                   // Feels very hacky -- Kyo 040813
                  var volume_list = [];
                  _.each(args.volume_ids, function(v){
                    var match = App.data.volume.find(function(model){ return model.get('id') == v; });
                    var instance_id = match.toJSON().attach_data.instance_id;
                    volume_list.push({volume_id: v, instance_id: instance_id});
                    console.log("Volume: " + v + " Instance: " + instance_id);
                  });
                  return volume_list;
                },

                cancelButton: function() {
                  self.close();
                },

                detachButton: function() {
	          // FOR EACH VOLUME ID IN THE LIST
	          _.each(args.volume_ids, function(item) {           // Need a better way to access the list of volumes rather than direct args.
		    console.log("Volume Item: " + item);
		    // FIND THE MODEL THAT MATCHES THE VOLUME ID WITH 'item'
		    var match = App.data.volume.find(function(model){ return model.get('id') == item; });

                    // CONSTRUCT AJAX CALL RESPONSE OPTIONS
                    var detachAjaxCallResponse = {
		      success: function(data, response, jqXHR){   // AJAX CALL SUCCESS OPTION
		        console.log("Callback " + response + " for " + item);
		        if(data.results){
		          console.log("sync: Volume " +item+ " Detached = " + data.results );
		          notifySuccess(null, $.i18n.prop('volume_create_success', DefaultEncoder().encodeForHTML(item)));  // incorrect prop; Need to be fixed  -- Kyoe 040813
		        }else{
		          notifyError($.i18n.prop('delete_volume_error', DefaultEncoder().encodeForHTML(item)), undefined_error);  // incorrect prop
		        }
		      },
		      error: function(jqXHR, textStatus, errorThrown){   // AJAX CALL ERROR OPTION
		        notifyError($.i18n.prop('delete_volume_error', DefaultEncoder().encodeForHTML(item)), getErrorMessage(jqXHR));  // incorrect prop 
		      }
                    };

		    // PERFORM DETACH CALL OM THE MODEL            Need to handle multi ajax call and multi-notification -- Kyo 040813
//		    match.sync('detach', match, detachAjaxCallResponse);
                    match.detach(detachAjaxCallResponse);
	          });

	          // DISPLAY THE MODEL LIST FOR VOLUME AFTER THE DETACH OPERATION -- FOR DEBUG
	          App.data.volume.each(function(item){
		    console.log("Volume After Detach: " + item.get('id'));
	          });

                  // CLOSE THE DIALOG
                  self.close();
                }
          }
          this._do_init();
        },
	});
});
