define([
   './eucadialogview',
   'text!./attachvolumedialog.html!strip',
   'models/volume',
   'app',
], function(EucaDialogView, template, Volume, App) {
    return EucaDialogView.extend({

        initialize : function(args) {
            var self = this;
            this.template = template;

            // FIND THE VOLUME MODEL IF MATCHED 'args.volume_id'
	    var matched_volume;
            if( args.volume_id != undefined ){
              matched_volume = App.data.volume.find(function(model){ return model.get('id') == args.volume_id; });
              if( matched_volume != undefined ){
     	        console.log("Found the matching model Volume: " + matched_volume.get('id'));
              }
            }

            // FIND THE INSTANCE MODEL IF MATCH 'args.instance_id'
	    var matched_instance;
            if( args.instance_id != undefined ){
              matched_instance = App.data.instance.find(function(model){ return model.get('id') == args.instance_id; });
              if( matched_instance != undefined ){
     	        console.log("Found the matching model Instance: " + matched_instance.get('id'));
              }
            }

            // CONSTRUCT THE LIST OF INSTANCE IDS FOR AUTO-COMPLETE
            var inst_ids = [];
            if( matched_instance == undefined ){
              App.data.instance.each(function(item){
                console.log("Instance ID: " + item.toJSON().id +":" + item.toJSON()._state.name + ":" + item.toJSON().placement);
                if( item.toJSON()._state.name === 'running' ){
                  inst_ids.push(item.toJSON().id);
                }
              });
              var sorted = sortArray(inst_ids);
/*
              $instanceSelector.autocomplete({
                source: sorted,
                select: function(event, ui) {
                if ($.trim(asText($deviceName.val())) == ''){
                  $deviceName.val(thisObj._suggestNextDeviceName(ui.item.value));
                }
                if ($deviceName.val() != '' && $volumeSelector.val() != '')
                  thisObj.attachDialog.eucadialog('activateButton', thisObj.attachButtonId);
                }
              });
              $instanceSelector.watermark(instance_id_watermark);
*/
            }

            this.scope = {
                status: 'Ignore me for now',
                volume: new Volume({volume_id: args.volume_id, instance_id: args.instance_id, device: args.device}),

                cancelButton: function() {
                  self.close();
                },

                attachButton: function() {
                  // GET THE INPUT FROM THE HTML VIEW
		  var volumeId = self.scope.volume.get('volume_id');
		  var instanceId = self.scope.volume.get('instance_id');
		  var device = self.scope.volume.get('device');
		  console.log("Selected Volume ID: " + volumeId);
		  console.log("Instance ID: " + instanceId);
		  console.log("Attach as device: " + device);

		  // PERFORM ATTACH CALL OM THE MODEL
		  self.scope.volume.sync('attach', self.scope.volume, {
		    // CASE OF AJAX CALL'S SUCCESS
		    success: function(data, response, jqXHR){
		      console.log("Callback " + response + " for " + volumeId);
		      if(data.results){
		        notifySuccess(null, $.i18n.prop('volume_attach_success', volumeId, instanceId));    // XSS Risk  -- Kyo 040713
		      }else{
		        notifyError($.i18n.prop('volume_attach_error', volumeId, instanceId), undefined_error);   // XSS Risk
		      }
		    },
		    // CASE OF AJAX CALL'S ERROR
		    error: function(jqXHR, textStatus, errorThrown){
		      console.log("Callback " + textStatus  + " for " + volumeId + " error: " + getErrorMessage(jqXHR));
		      notifyError($.i18n.prop('volume_attach_error', volumeId, instanceId), getErrorMessage(jqXHR));   // XSS Risk
		    }
		  });

	         // DISPLAY THE VOLUME'S STATUS -- FOR DEBUG
		 App.data.volume.each(function(item){
		   console.log("Volume After Attach: " + item.toJSON().id +":"+ item.toJSON().instance_id);
	         });

	        // CLOSE THE DIALOG
	        self.close();
              }

            }

            this._do_init();
        },
	});
});
