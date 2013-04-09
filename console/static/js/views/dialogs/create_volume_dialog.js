define([
   './eucadialogview',
   'text!./create_volume_dialog.html!strip',
   'models/volume',
   'app',
   'backbone',
], function(EucaDialogView, template, Volume, App, Backbone) {
    return EucaDialogView.extend({

        setupSelectOptions: function(args){
           var self = this;
           this.template = template;

           // SETUP THE SNAPSHOT SELECT OPTIONS
           var $snapshotSelector = this.$el.find("#volume-add-snapshot-selector");
           $snapshotSelector.append($('<option>', { 
               value: undefined,
               text : "NONE" 
             }));
           App.data.snapshot.each(function (model) {
             console.log("Snapshot: " + model.get('id'));
             $snapshotSelector.append($('<option>', { 
               value: model.get('id'),
               text : model.get('id') 
             }));
           });

           $snapshotSelector.change( function(){
             snapshotId = $snapshotSelector.val();
             if(snapshotId) {
               var snapshot = describe('snapshot', snapshotId);
             //  $volSize.val(snapshot['volume_size']);
               self.scope.item.snapshot_id = snapshotId;           // super hackish ... --- Kyo 040813
               self.scope.item.size = snapshot['volume_size'];     // sper hackish .. --- Kyo 040813
             }
           });

           // SETUP THE AVAILABILITY ZONE SELECT OPTIONS
           var $azSelector = this.$el.find("#volume-add-az-selector");
           var results = describe('zone');
           if (results && results.length > 1)
             $azSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_dialog_zone_select']));
           var azArr = []; 
           for( res in results) {
             var azName = results[res].name;
             azArr.push(azName);
           }
           var sortedAz = sortArray(azArr);
           $azSelector.append($('<option>').attr('value', "Default").text("Default"));  // QUICK TEMP HACK TO FORCE USER TO CHOOSE A.ZONE  --- Kyo 040913
           $.each(sortedAz,function(idx, azName){
             $azSelector.append($('<option>').attr('value', azName).text(azName));
           });

           $azSelector.change( function(){
             azone = $azSelector.val();
             if(azone) {
               self.scope.item.zone = azone;           // super hackish ... --- Kyo 040813
               console.log("AZone: " + azone);
             }
           });

        },

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                volume: new Volume(),
                item: {snapshot_id: args.snapshot_id, size: args.size, zone: args.zone},

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                createButton: {
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
		    var snapshotId = self.scope.item.snapshot_id;
		    var size = self.scope.item.size;
		    var zone = self.scope.item.zone;
		    console.log("Selected Snapshot ID: " + snapshotId);
		    console.log("Size: " + size);
		    console.log("Zone: " + zone);

                    // CONSTRUCT AJAX CALL RESPONSE OPTIONS
                    var createAjaxCallResponse = {
		      success: function(data, response, jqXHR){   // AJAX CALL SUCCESS OPTION
		        console.log("Callback " + response);
                        if(data.results){
                          var volId = data.results.id;
                          notifySuccess(null, $.i18n.prop('volume_create_success', volId));   // XSS RISK --- Kyo 040813
                        }else{
                          notifyError($.i18n.prop('volume_create_error'), undefined_error);
                        }
		      },
		      error: function(jqXHR, textStatus, errorThrown){  // AJAX CALL ERROR OPTION
		        console.log("Callback " + textStatus  + " error: " + getErrorMessage(jqXHR));
                        notifyError($.i18n.prop('volume_create_error'), getErrorMessage(jqXHR));
		      }
                    };

		    // PERFORM CREATE CALL OM THE MODEL
                    self.scope.volume = new Volume({snapshot_id: snapshotId, size: size, availablity_zone: zone});
                    self.scope.volume.sync('create', self.scope.volume, createAjaxCallResponse);

	           // DISPLAY THE VOLUME'S STATUS -- FOR DEBUG
		   App.data.volume.each(function(item){
		     console.log("Volume After create: " + item.toJSON().id +":"+ item.toJSON().size);
	           });

	          // CLOSE THE DIALOG
	          self.close();
                }
              }

            }

            this._do_init();

            this.setupSelectOptions(args);
        },
	});
});
