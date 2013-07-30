define([
   './eucadialogview',
   'text!./create_volume_dialog.html!strip',
   'models/volume',
   'models/tag',
   'app',
   'backbone',
], function(EucaDialogView, template, Volume, Tag, App, Backbone) {
    return EucaDialogView.extend({

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
                zones: App.data.zones,
                error: new Backbone.Model({}),

                help: {title: null, content: help_volume.dialog_add_content, url: help_volume.dialog_add_content_url, pop_height: 600},

                cancelButton: {
                  id: 'button-dialog-createvolume-cancel',
                  click: function() {
                    self.close();
                  }
                },

                setZone: function(e, obj) {
                  var zone = e.target.value;
                  self.scope.volume.set('availability_zone', zone);
                },

                createButton: new Backbone.Model({
                  id: 'button-dialog-createvolume-save',
                  disabled: true,
                  click: function() {
                    // GET THE INPUT FROM THE HTML VIEW
                    if (!self.scope.volume.isValid()) return;

                    var snapshotId = self.scope.volume.get('snapshot_id');          
                    var size = self.scope.volume.get('size');                      
                    var availability_zone = self.scope.volume.get('availability_zone');         
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
                        notifyError($.i18n.prop('volume_create_error'), getErrorMessage(jqXHR));
                      }
                  });

	          // CLOSE THE DIALOG
	          self.close();
                }
              }),

              activateButton: function(e) {
                // need to activate the button when 
                // the required size field is typed in, instead of having to tab
                // out of it or click another optional field - EUCA-6106
                // button click will still validate and disallow weird input.
                setTimeout(function() { $(e.target).change(); }, 0);
                self.scope.createButton.set('disabled', !self.scope.volume.isValid());
              },

              isSnapSelected: function(val) {
                return (val.item.id == self.scope.volume.get('snapshot_id'));
              }

            }

            if (args.zone == undefined) {
              var zone = App.data.zones.at(0).get('name');
              this.scope.volume.set('availability_zone', zone);
            }

            this.scope.snapshots = [];
            App.data.snapshots.each(function(snap) {
              var nameTag = self.findNameTag(snap);
              var name_string = self.createIdNameTagString(snap.get('id'),
                                                addEllipsis(nameTag, 15));
              self.scope.snapshots.push({id:snap.get('id'), name:name_string});
            });

            this.scope.volume.on('change', function(model) {
                console.log('CHANGE', arguments);
                self.scope.volume.validate(model.changed);
            });

            this.scope.volume.on('validated', function(valid, model, errors) {
                _.each(_.keys(model.changed), function(key) { 
                    self.scope.error.set(key, errors[key]); 
                });

                self.scope.createButton.set('disabled', !self.scope.volume.isValid());
            });

            this._do_init();

            var $snapshotSelector = this.$el.find("#volume-add-snapshot-selector");
            $snapshotSelector.change( function(){
              snapshotId = $snapshotSelector.val();
              if(snapshotId && snapshotId != "None") {
                var snapshot_size = App.data.snapshot.get(snapshotId).get('volume_size');
                self.scope.volume.set({snapshot_id: snapshotId}); 
                self.scope.volume.set({size: snapshot_size});
              }
            });
            if( args.snapshot_id != undefined ){
              var snap = App.data.snapshot.get(args.snapshot_id);
              self.scope.volume.set({snapshot_id: snap.get('id')});
              self.scope.volume.set({size: snap.get('volume_size')});
              $snapshotSelector.prop('disabled', true);
            }
            this.scope.volume.validate();
        },
	});
});
