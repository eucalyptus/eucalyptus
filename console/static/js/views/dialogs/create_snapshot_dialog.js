define([
   './eucadialogview',
   'text!./create_snapshot_dialog.html!strip',
   'models/snapshot',
   'models/tag',
   'app',
   'backbone',
], function(EucaDialogView, template, Snapshot, Tag, App, Backbone) {
    return EucaDialogView.extend({

        disableVolumeInputBox: function(){
          var $volumeSelector = this.$el.find('#snapshot-create-volume-id');
          $volumeSelector.attr('disabled', 'disabled');
        },

        // SET UP AUTOCOMPLETE FOR THE VOLUME INPUT BOX
        setupAutoComplete: function(args){
            var self = this;
            
            if( args.volume_id == undefined ){
              // CASE: CALLED FROM THE SNAPSHOT LANDING PAGE
              var vol_ids = [];
              App.data.volume.each(function(v){
                console.log("Volume ID: " + v.get('id') + "  Status:" + v.get('status'));
                var nameTag = self.findNameTag(v);
                var autocomplete_string = String(self.createIdNameTagString(v.get('id'), addEllipsis(nameTag, 15)));
                vol_ids.push(autocomplete_string);
              });

              var sorted = sortArray(vol_ids);
              console.log("Autocomplete Volume List: " + sorted);

              var $volumeSelector = this.$el.find('#snapshot-create-volume-id');
              $volumeSelector.autocomplete({
                source: sorted,
                select: function(event, ui) {
                  var selected_volume_id = ui.item.value.split(' ')[0];
                  self.scope.snapshot.set('volume_id', selected_volume_id);
                }
              });

              // ALLOWS THE VOLUME ID TO BE SCANNED AS TYPING
              $volumeSelector.keyup(function(e){
		var volumeID = $.trim($volumeSelector.val());
		self.scope.snapshot.set('volume_id', volumeID);
	      });

            }else{
              // CASE: CALLED FROM THE VOLUME LANDING PAGE
              // DISABLE THE VOLUME INPUT BOX
              this.disableVolumeInputBox();
              // DISPLAY ITS NAME TAG FOR VOLUME ID
              var foundNameTag = self.findNameTag(App.data.volume.get(args.volume_id));
              self.scope.snapshot.set({volume_id: String(self.createIdNameTagString(args.volume_id, addEllipsis(foundNameTag, 15)))});
            }

        },

        // CONSTRUCT A STRING THAT DISPLAYS BOTH RESOURCE ID AND ITS NAME TAG
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
                snapshot: new Snapshot({volume_id: args.volume_id, description: '', validvols: App.data.volumes.pluck('id')}),
                error: new Backbone.Model({}),
                help: {title: null, content: help_snapshot.dialog_create_content, url: help_snapshot.dialog_create_content_url, pop_height: 600},

                activateButton: function(e) {
                  $(e.target).change();
                  self.scope.snapshot.validate();
                },

                cancelButton: {
                  id: 'button-dialog-createsnapshot-cancel',
                  click: function() {
                    self.close();
                    self.cleanup();
                  }
                },

                createButton: new Backbone.Model({
                  id: 'button-dialog-createsnapshot-save',
                  disabled: true,
                  click: function() {
                    console.log("disabled: " + self.scope.createButton.get('disabled'));
                    if( self.scope.createButton.get('disabled') === true ){
                      return;
                    }
	            // GET THE INPUT FROM HTML VIEW
	            var volumeId = self.scope.snapshot.get('volume_id');
	            var description = self.scope.snapshot.get('description');
                    var name = self.scope.snapshot.get('name');
		    console.log("Selected Volume ID: " + volumeId);
		    console.log("Volume Description: " + description);
		    console.log("Name: " + name);

                    // EXTRACT THE RESOURCE ID IF THE NAME TAG WAS FOLLOWED
                    if( volumeId.match(/^\w+-\w+\s+/) ){
                      volumeId = volumeId.split(" ")[0];
		      console.log("Volume ID: " + volumeId);
                      self.scope.snapshot.set({volume_id: volumeId});
                    }

                    // ADD NAME TAG
                    if( name != null ){
                      var nametag = new Tag();
                      nametag.set({res_id: self.scope.snapshot.get('id'), name: 'Name', value: name, _clean: true, _new: true, _deleted: false});
                      self.scope.snapshot.trigger('add_tag', nametag);
                    }

	            // CONSTRUCT AJAX CALL RESPONSE OPTIONS
	            var createAjaxCallResponse = {
	              success: function(model, response, options){   // AJAX CALL SUCCESS OPTION
		        console.log("Returned Model ID: " + model.get('id') + " for " + volumeId);
			if(model != null){
			  snapId = model.get('id');
			  notifySuccess(null, $.i18n.prop('snapshot_create_success', snapId, volumeId));    // XSS risk  -- Kyo 040713
			}else{
			  notifyError($.i18n.prop('snapshot_create_error', volumeId, undefined_error));     // XSS risk
			}
	              },
		      error: function(model, jqXHR, options){  // AJAX CALL ERROR OPTION
		        console.log("Errored for " + volumeId + " error: " + getErrorMessage(jqXHR));
			notifyError($.i18n.prop('snapshot_create_error', volumeId), getErrorMessage(jqXHR));                     // XSS risk
		      }
	            };

	            // PERFORM CREATE CALL OM THE MODEL
                    self.scope.snapshot.trigger('confirm');
                    self.scope.snapshot.save({}, createAjaxCallResponse); 

	            // CLOSE THE DIALOG
	            self.close();
                    self.cleanup();
                  }
                })
            };


            // override validation requirements in this model instance
            // to validate required form fields in the dialog
            // DISABLED THE BACKBONE VALIDATION FOR A QUICK HACK -- KYO 072613
            // NEED TO RE-EXAMINE ONCE VOLUME ID CAN BE VALIDATED INSIDE BACKBONE MODEL
     //       this.scope.snapshot.validation.volume_id.required = true;

            this.scope.snapshot.on('change', function(model) {
                self.scope.snapshot.validate(model.changed);
            });

            this.scope.snapshot.on('validated', function(valid, model, errors) {
                self.scope.createButton.set('disabled', true);
                // SCAN THE VALIDATION ERROR
                _.each(_.keys(model.changed), function(key) { 
                     self.scope.error.set(key, errors[key]); 
                });

                // CHECK TO MAKE SURE THAT VOLUME ID IS VALID -- EXCLUSIVE VALIDATION OUTSIDE OF BACKBONE MODEL VALIDATION -- KYO 072613
                var volumeID = self.scope.snapshot.get('volume_id');
	        if( volumeID ){
                  if( volumeID.match(/^\w+-\w+\s+/) ){
                    volumeID = volumeID.split(" ")[0];
                  }
                  if( App.data.volume.findWhere({id: volumeID}) === undefined ){
                    // IF THE VOLUMD ID IS INVALID, DISPLAY THE ERROR
                    self.scope.error.set("volume_id", "Invalid volume id");
                  }else{
                    // ELSE ENABLE THE CREATE BUTTON IF OTHER SNAPSHOT VARIABLES ARE VALID
                    // NOTE: THIS STEP IS NEEDED SINCE THE VOLUME ID NEEDS TO BE VALIDATED SEPARATELY
                    self.scope.createButton.set('disabled', !self.scope.snapshot.isValid());
                  }
                }
            });

            this._do_init();

            this.setupAutoComplete(args);

            // sometimes the volume_id is preloaded, and is the only required field
            if( this.scope.snapshot.get('volume_id') !== undefined ){
              this.scope.snapshot.validate(); 
            }
        },

        cleanup: function() {
          // undo the validation override performed in initialize()
          // it seems to be leaking into other dialogs somehow
          this.scope.snapshot.validation.volume_id.required = false;
        },
    });
});
