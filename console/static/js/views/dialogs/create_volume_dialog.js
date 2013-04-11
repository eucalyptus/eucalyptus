define([
   './eucadialogview',
   'text!./create_volume_dialog.html!strip',
   'models/volume',
   'app',
   'backbone',
], function(EucaDialogView, template, Volume, App, Backbone) {
    return EucaDialogView.extend({


         setupSelectOptionsForSnapshotBoxDisabled: function(args){
             var self = this;
   
             var $snapshotSelector = this.$el.find("#volume-add-snapshot-selector");

             var selected_snapshot_id = App.data.snapshot.get(args.snapshot_id).get('id');
             var selected_snapshot_size = App.data.snapshot.get(selected_snapshot_id).get('volume_size');
             console.log("Selected Snapshot ID: " + selected_snapshot_id);
             $snapshotSelector.append($('<option>', {
                 value: selected_snapshot_id,
                 text : selected_snapshot_id
             }));
             self.scope.volume.set({snapshot_id: selected_snapshot_id});
             self.scope.volume.set({size: selected_snapshot_size});
             $snapshotSelector.attr('disabled', 'disabled');
         },

         setupSelectOptionsForSnapshotBox: function(args){
             var self = this;
   
             // CASE: CALLED FROM THE SNAPSHOT LANDING PAGE
             if( args.snapshot_id != undefined ){
               this.setupSelectOptionsForSnapshotBoxDisabled(args);
               return;
             }

             var $snapshotSelector = this.$el.find("#volume-add-snapshot-selector");
               
             $snapshotSelector.append($('<option>', { 
                 value: undefined,
                 text : "None" 
             }));
             App.data.snapshot.each(function (model, index) {
               console.log("Snapshot: " + model.get('id') + " :" + index);
               $snapshotSelector.append($('<option>', { 
                 value: model.get('id'),
                 text : model.get('id') 
               }));
             });

             $snapshotSelector.change( function(){
               snapshotId = $snapshotSelector.val();
               if(snapshotId) {
                 var snapshot_size = App.data.snapshot.get(snapshotId).get('volume_size');
                 self.scope.volume.set({snapshot_id: snapshotId}); 
                 self.scope.volume.set({size: snapshot_size});
               }
             });
        },

        setupSelectOptionsForAzoneBox: function(args){
            var self = this;

            var $azSelector = this.$el.find("#volume-add-az-selector");

            if( _.size(App.data.zone) == 0 ){    // NOT TESTED --- Kyo 041013
              $azSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_dialog_zone_select']));
            };

            App.data.zone.each(function(model, index){
              console.log("Avail. Zone: " + JSON.stringify(model));
              console.log("Avail. Zone Index: " + index);
              var aZoneName = model.get('name');
              if( index == 0 ){
                self.scope.volume.set({availablity_zone: aZoneName});   // Set the first avail. zone as default
              }
              $azSelector.append($('<option>', {
                 value: aZoneName,
                 text : aZoneName
              }));
            });

            $azSelector.change( function(){
              azone = $azSelector.val();
              if(azone) {
                self.scope.volume.set({availablity_zone: azone});
                 console.log("Selected Avail. Zone: " + azone);
              }
            });
        },

        setupSelectOptions: function(args){
           var self = this;
           this.template = template;

           // SETUP THE SNAPSHOT SELECT OPTIONS
           this.setupSelectOptionsForSnapshotBox(args);

           // SETUP THE AVAILABILITY ZONE SELECT OPTIONS
           this.setupSelectOptionsForAzoneBox(args);
        },

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                volume: new Volume({snapshot_id: args.snapshot_id, size: args.size, availablity_zone: args.zone}),

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                createButton: {
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
                    var snapshotId = self.scope.volume.get('snapshot_id');          
                    var size = self.scope.volume.get('size');                      
                    var zone = self.scope.volume.get('availablity_zone');         
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
