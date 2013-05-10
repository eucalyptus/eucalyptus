define([
   './eucadialogview',
   'text!./create_volume_dialog.html!strip',
   'models/volume',
   'models/tag',
   'app',
   'backbone',
], function(EucaDialogView, template, Volume, Tag, App, Backbone) {
    return EucaDialogView.extend({


         setupSelectOptionsForSnapshotBoxDisabled: function(args){
             var self = this;
   
             var $snapshotSelector = this.$el.find("#volume-add-snapshot-selector");

             var snapshot_model = App.data.snapshot.get(args.snapshot_id);
             var selected_snapshot_id = snapshot_model.get('id');
             var selected_snapshot_size = snapshot_model.get('volume_size');
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
               var nameTag = self.findNameTag(model);
               var snapshot_name_string = self.createIdNameTagString(model.get('id'), nameTag);
               $snapshotSelector.append($('<option>', { 
                 value: model.get('id'),
                 text : snapshot_name_string 
               }));
             });

             $snapshotSelector.change( function(){
               snapshotId = $snapshotSelector.val();
               if(snapshotId && snapshotId != "None") {
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

           // SETUP INPUT VALIDATOR
           self.scope.volume.on('change', function() {
             self.scope.error.clear();
             self.scope.error.set(self.scope.volume.validate());
             console.log("Validation Error: " + JSON.stringify(self.scope.error));
           });
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
                error: new Backbone.Model({}),
                help: {title: null, content: help_instance.dialog_launchmore_content, url: help_instance.dialog_launchmore_content_url, pop_height: 600},

                cancelButton: {
                  click: function() {
                    self.close();
                  }
                },

                createButton: new Backbone.Model({
                  disabled: true,
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
                    if (!self.scope.volume.isValid()) return;

                    var snapshotId = self.scope.volume.get('snapshot_id');          
                    var size = self.scope.volume.get('size');                      
                    var availablity_zone = self.scope.volume.get('availablity_zone');         
                    var name = self.scope.volume.get('name');

                    // CREATE A NAME TAG
                    if( name != null ){
                      var nametag = new Tag();
                      nametag.set({res_id: self.scope.volume.get('id'), name: 'Name', value: name, _clean: true, _new: true, _deleted: false});
                      self.scope.volume.trigger('add_tag', nametag);
                    }

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
                        console.log("Error: " + getErrorMessage(jqXHR));
                        notifyError($.i18n.prop('volume_create_error'), getErrorMessage(jqXHR));
                      }
                  });

	          // CLOSE THE DIALOG
	          self.close();
                }
              })
            }


            this.scope.volume.on('validated', function() {
                self.scope.createButton.set('disabled', !self.scope.volume.isValid());
               // self.render();
            });

            this._do_init();

            this.setupSelectOptions(args);
            this.scope.volume.validate();
        },
	});
});
