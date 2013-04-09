define([
   './eucadialogview',
   'text!./attach_volume_dialog.html!strip',
   'models/volume',
   'app',
   'backbone',
], function(EucaDialogView, template, Volume, App, Backbone) {
    return EucaDialogView.extend({

        setupAutoComplete: function(args){
            var self = this;
            this.template = template;

            // FIND THE VOLUME MODEL IF MATCHED 'args.volume_id'  --- FOR DEBUG. NOT NEEDED  Kyo 040813
            if( args.volume_id != undefined ){
              var matched_volume = App.data.volume.find(function(model){ return model.get('id') == args.volume_id; });
              if( matched_volume != undefined ){
     	        console.log("Found the matching model Volume: " + matched_volume.get('id'));
              }
            }else{
              // ELSE CONSTRCUT THE AUTO-COMPLETE FOR THE VOLUME INPUT
              var vol_ids = [];
              App.data.volume.each(function(item){
                console.log("Volume ID: " + item.toJSON().id + ":" + item.toJSON().status);
                if( item.toJSON().status === 'detached' ){
                  vol_ids.push(item.toJSON().id);
                }
              });
              var sorted = sortArray(vol_ids);
              console.log("selector: " + sorted);
              var $volumeSelector = this.$el.find('#volume-attach-volume-id');
              $volumeSelector.autocomplete({
                source: sorted
              });
            } 

            // FIND THE INSTANCE MODEL IF MATCH 'args.instance_id'   ---   FOR DEBUG. NOT NEEDED Kyo 040813
            if( args.instance_id != undefined ){
              var matched_instance = App.data.instance.find(function(model){ return model.get('id') == args.instance_id; });
              if( matched_instance != undefined ){
     	        console.log("Found the matching model Instance: " + matched_instance.get('id'));
              }
            }else{
              // CONSTRUCT THE LIST OF INSTANCE IDS FOR AUTO-COMPLETE
              var inst_ids = [];
              App.data.instance.each(function(item){
                console.log("Instance ID: " + item.toJSON().id +":" + item.toJSON()._state.name + ":" + item.toJSON().placement);
                if( item.toJSON()._state.name === 'running' ){
                  inst_ids.push(item.toJSON().id);
                }
              });
              var sorted = sortArray(inst_ids);
              console.log("selector: " + sorted);
              var $instanceSelector = this.$el.find('#volume-attach-instance-id');
              var $deviceName = this.$el.find('#volume-attach-device-name');
              $instanceSelector.autocomplete({
                source: sorted,
                select: function(event, ui){
                  if($deviceName.val() == ''){
                    var this_device = self._suggestNextDeviceName(ui.item.value);
                    $deviceName.val(this_device);
                    self.scope.item.device = this_device;    // Super Hackish Way --- Kyo 040813
                  }
                }
              });
            }; 
        },

        _generateRecommendedDeviceNames : function(count) {
          possibleNames = {};
          for(i=0; i<11 && i<=count; i++){ // f..p
            possibleNames['/dev/sd'+String.fromCharCode(102+i)] = 1;
          }
          return possibleNames;
        },

        _suggestNextDeviceName : function(instanceId) {
          //var instance = describe('instance', instanceId);
          var instance = App.data.instance.get(instanceId);   // Fails to quickly obtain the device information due to delay, or max is 2 ? -- Kyo 040813
          if (instance) {
            var count = 1;
            console.log("device_mapping: " + instance.get('block_device_mapping'));
            for(device in instance.get('block_device_mapping')) count++;
            console.log("device_mapping count: " + count);
            possibleNames = this._generateRecommendedDeviceNames(count);
            for(device in instance.get('block_device_mapping')){
              possibleNames[device] = 0;
            }
             for(n in possibleNames){
               if (possibleNames[n] == 1){
                 return n;
               }
             }
          }
          return '';
        },

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                item: {volume_id: args.volume_id, instance_id: args.instance_id, device: args.device},

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                attachButton: {
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
		    var volumeId = self.scope.item.volume_id;
		    var instanceId = self.scope.item.instance_id;
		    var device = self.scope.item.device;
		    console.log("Selected Volume ID: " + volumeId);
		    console.log("Instance ID: " + instanceId);
		    console.log("Attach as device: " + device);

                    // CONSTRUCT AJAX CALL RESPONSE OPTIONS
                    var attachAjaxCallResponse = {
		      success: function(data, response, jqXHR){   // AJAX CALL SUCCESS OPTION
		        console.log("Callback " + response + " for " + volumeId);
		        if(data.results){
		          notifySuccess(null, $.i18n.prop('volume_attach_success', volumeId, instanceId));    // XSS Risk  -- Kyo 040713
		        }else{
		          notifyError($.i18n.prop('volume_attach_error', volumeId, instanceId), undefined_error);   // XSS Risk
		        }
		      },
		      error: function(jqXHR, textStatus, errorThrown){  // AJAX CALL ERROR OPTION
		        console.log("Callback " + textStatus  + " for " + volumeId + " error: " + getErrorMessage(jqXHR));
		        notifyError($.i18n.prop('volume_attach_error', volumeId, instanceId), getErrorMessage(jqXHR));   // XSS Risk
		      }
                    };

		    // PERFORM ATTACH CALL OM THE MODEL
                    App.data.volume.get(volumeId).attach(instanceId, device, attachAjaxCallResponse);

	           // DISPLAY THE VOLUME'S STATUS -- FOR DEBUG
		   App.data.volume.each(function(item){
		     console.log("Volume After Attach: " + item.toJSON().id +":"+ item.toJSON().status);
	           });

	          // CLOSE THE DIALOG
	          self.close();
                }
              }

            }

            this._do_init();

            this.setupAutoComplete(args);
        },
	});
});
