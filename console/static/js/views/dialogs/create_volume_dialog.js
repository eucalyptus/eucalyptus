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

             var snapshot_model = App.data.snapshot.get(args.snapshot_id);
             var selected_snapshot_id = snapshot_model.get('id');
             var selected_snapshot_size = snapshot_model.get('volume_size');
             console.log("Selected Snapshot ID: " + selected_snapshot_id);
             var nameTag = self.findNameTag(snapshot_model);
             var snapshot_name_string = self.createIdNameTagString(selected_snapshot_id, nameTag);
             $snapshotSelector.append($('<option>', {
                 value: selected_snapshot_id,
                 text : snapshot_name_string
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
               var nameTag = self.findNameTag(model);
               var snapshot_name_string = self.createIdNameTagString(model.get('id'), nameTag);
               $snapshotSelector.append($('<option>', { 
                 value: model.get('id'),
                 text : snapshot_name_string 
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

        // CONSTRUCT A STRING THAT DISPLAY BOTH RESOURCE ID AND ITS NAME TAG
        createIdNameTagString: function(resource_id, name_tag){
          var this_string = resource_id;
          if( name_tag != null ){
            this_string += " (" + name_tag + ")";
          }
          return this_string;
        },

        // UTILITY FUNCTION TO DISCOVER THE NAME TAG OF CLOUD RESOURCE MODEL
        findNameTag: function(model){
          var nameTag = null;
          model.get('tags').each(function(tag){
            if( tag.get('name').toLowerCase() == 'name' ){
              nameTag = tag.get('value');
            };
          });
          return nameTag;
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


		    // PERFORM CREATE CALL OM THE MODEL
            self.scope.volume.trigger('confirm');
            self.scope.volume.save({}, {
                success: function(model, response, options){   // AJAX CALL SUCCESS OPTION
                    if(model != null){
                        var volId = model.get('id');
                        notifySuccess(null, $.i18n.prop('volume_create_success', volId));   // XSS RISK --- Kyo 040813
                    }else{
                        notifyError($.i18n.prop('volume_create_error'), undefined_error);
                    }
                },
                error: function(model, jqXHR, options){  // AJAX CALL ERROR OPTION
                    console.log("Callback " + getErrorMessage(jqXHR));
                    notifyError($.i18n.prop('volume_create_error'), getErrorMessage(jqXHR));
                }
            });

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
