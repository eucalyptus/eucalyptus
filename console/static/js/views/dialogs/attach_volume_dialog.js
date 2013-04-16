define([
   './eucadialogview',
   'text!./attach_volume_dialog.html!strip',
   'models/volume',
   'app',
   'backbone',
], function(EucaDialogView, template, Volume, App, Backbone) {
    return EucaDialogView.extend({

        // GENERATE HASHMAP WITH POSSIBLE DEVICE NAMES FOR ATTACH VOLUME OPERATION
        _generateRecommendedDeviceNames : function(count) {
            possibleNames = {};
            for(i=0; i<11 && i<=count; i++){    // Generate char 'f' to 'p'
              possibleNames['/dev/sd'+String.fromCharCode(102+i)] = 1;
            }
            return possibleNames;
        },

        // LEGACY CODE FOR PROVIDING SUGGESTED DEVICE NAME FOR ATTACH VOLUME OPERATION
        _suggestNextDeviceName : function(instanceId) {
            var instance = App.data.instance.get(instanceId);   // ISSUE: Fails to quickly obtain up-to-date device information due to delay -- Kyo 040813
            if(instance == undefined){
              return 'error';
            }
            var instanceObj = instance.get('block_device_mapping');
            var count = _.size(instanceObj) + 1;
            console.log("Object device_mapping: " + JSON.stringify(instanceObj));
            console.log("Next device_mapping Index: " + count);
            possibleNames = this._generateRecommendedDeviceNames(count);
            for(device in instanceObj){
              possibleNames[device] = 0;     // Zero out the hashmap for existing device names
            }
            for(n in possibleNames){
              if(possibleNames[n] == 1){    // Pick the first string with the open hash item
                return n;
              }
            }
            return 'error';
        },

        // SET UP THE NEXT DEVICE NAME GIVEN THE INSTANCE ID
        setupNextDeviceName: function(args){
          var self = this;

          var deviceName = self._suggestNextDeviceName(args.instance_id);
          self.scope.volume.set({device: deviceName});

        },

        // SET UP AUTOCOMPLETE FOR THE VOLUME INPUT BOX
        setupAutoCompleteForVolumeInputBox: function(args){
            var self = this;

            var vol_ids = [];
            App.data.volume.each(function(item){
              console.log("Volume ID: " + item.get('id') + ":" + item.get('status'));
              if( item.get('status') === 'available' ){
                vol_ids.push(item.toJSON().id);
              }
            });
            var sorted = sortArray(vol_ids);
            console.log("Autocomplete Volume List: " + sorted);
            var $volumeSelector = this.$el.find('#volume-attach-volume-id');
            $volumeSelector.autocomplete({
              source: sorted
            });
        },

        // SET UP AUTOCOMPLETE FOR THE INSTANCE INPUT BOX
        setupAutoCompleteForInstanceInputBox: function(args){
            var self = this;

            var inst_ids = [];
            App.data.instance.each(function(item){
              console.log("Instance ID: " + item.toJSON().id +":" + item.toJSON()._state.name + ":" + item.toJSON().placement);
              if( item.toJSON()._state.name === 'running' ){
                inst_ids.push(item.toJSON().id);
              }
            });

            var sorted = sortArray(inst_ids);
            console.log("Autocomplete Instance List: " + sorted);

            var $instanceSelector = this.$el.find('#volume-attach-instance-id');
            $instanceSelector.autocomplete({
              source: sorted,
              select: function(event, ui){
                var deviceName = self._suggestNextDeviceName(ui.item.value);
                self.scope.volume.set({device: deviceName});
              }
            });
        },

        disableVolumeInputBox: function(){
          var $volumeSelector = this.$el.find('#volume-attach-volume-id');
          $volumeSelector.attr('disabled', 'disabled');
        },

        disableInstanceInputBox: function(){
          var $instanceSelector = this.$el.find('#volume-attach-instance-id');
          $instanceSelector.attr('disabled', 'disabled');
        },

        // SET UP AUTOCOMPLETE FOR INPUT BOXES
        setupAutoComplete: function(args){
            var self = this;

            // CASE: WHEN CALLED FROM THE INSTANCE PAGE
            if( args.volume_id == undefined ){
              this.setupAutoCompleteForVolumeInputBox(args);
              this.setupNextDeviceName(args);
              this.disableInstanceInputBox();
            };

            // CASE: WHEN CALLED FROM THE VOLUME PAGE
            if( args.instance_id == undefined ){
              this.setupAutoCompleteForInstanceInputBox(args);
              this.disableVolumeInputBox();
            }; 
        },

        // INITIALIZE THE VIEW
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                volume: new Volume({volume_id: args.volume_id, instance_id: args.instance_id, device: args.device}),

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                attachButton: {
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
                    var volumeId = self.scope.volume.get('volume_id');
                    var instanceId = self.scope.volume.get('instance_id');
                    var device = self.scope.volume.get('device');
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
		     console.log("Volume After Attach: " + item.toJSON().id +"   Status:"+ item.toJSON().status);
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
