define([
   'underscore',
   'text!./template.html!strip',
   'backbone',
   'models/tag',
   'app'
], function(_, template, Backbone, Tag, app) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.template = template;
            var model = args.model;
            var tags = new Backbone.Collection();

            var prepareTag = function(t) {
              if (!/^euca:/.test(t.get('name'))) {
                  var nt = new Tag(t.pick('id','name','value','res_id'));
                  nt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: false});
                  return nt;
              }
            };

            var loadTags = function() {
                model.get('tags').each(function(t) {
                  tags.add(prepareTag(t));
                });
            }

            loadTags();

            model.on('reload', function() {
              tags.reset();
              loadTags();
              self.render();
            });

            model.on('addTag', function(tag, unique_keys) {
              var name = tag.get('name');
              if(unique_keys) {
                var duplicates = tags.where({name: name});
                tags.remove(duplicates, {silent: true});
              }
              tags.add(prepareTag(tag));
              self.render();  
            });

            model.on('confirm', function(defer) {
                self.scope.create();
                _.chain(tags.models).clone().each(function(t) {
                   var backup = t.get('_firstbackup');		// _firstbackup: the original tag to begin edit with
                   console.log('TAG',t);
                   if (t.get('_deleted')) {
                       // If the tag is new and then deleted, do nothing.
                       if (!t.get('_new')) {
                           // If this was edited, we really want to destroy the original
                           if (backup != null) {
                               console.log('delete', backup);
                               backup.destroy();
                           } else {
                               console.log('delete', t);
                               t.destroy();
                           }
                       } else {
                         // remove _new _delete tags
                         tags.remove(t);
                       }
                   } else if (t.get('_edited')) {
                       // If the tag is new then it should only be saved, even if it was edited.
                       if (t.get('_new')) {
                         if(!defer){
			   t.save();
			 }
                       } else if( (backup != null) && (backup.get('name') !== t.get('name')) ){
                         // CASE OF KEY CHANGE
                         console.log("EDITED TAG TO: " + t.get('name') + ":" + t.get('value'));
                         backup.destroy();
                         if(!defer){
			   t.save();
			 }
                       }else{
                         // CASE OF VALUE CHANGE
                        if(!defer){
			  t.save();
			}
                       } 
                   } else if (t.get('_new')) {
                       if(!defer){
		         t.save();
		       }
                   }
                });
                model.get('tags').set(tags.models);
            });

            // ADDED TO ALLOW DIALOGS TO ADD NAME TAG  --- Kyo 042113
            model.on('add_tag', function(this_tag) {
              self.scope.tags.add(this_tag);
            });


            this.scope = {
                newtag: new Tag(),
                tags: tags,
                isTagValid: true,
                error: new Backbone.Model({}),
                status: '',

                // Abort other edit-in-progress
                deactivateEdits: function() {
                    self.scope.tags.each(function(t) {
                        if (t.get('_edit')) {
                            t.set(t.get('_backup').pick('name','value'));
                            t.set({_clean: true, _deleted: false, _edit: false});
                    	}
		    });
                },

                // Disable all buttons while editing a tag
                disableButtons: function() {
                    self.scope.tags.each(function(t) {
			if( !t.get('_deleted') ){
                    	    t.set({_clean: false, _displayonly: true});
                    	}
		    });
                },

                // Restore the buttons to be clickable
                enableButtons: function() {
                    self.scope.tags.each(function(t) {
			if( !t.get('_deleted') ){
                    	   t.set({_clean: true, _displayonly: false});
                    	}
		    });
                },

		// Entering the Tag-Edit mode
		enterTagEditMode: function() {
		    self.scope.deactivateEdits();
		    self.scope.disableButtons();	
		},

		// Entering the Clean mode
		enterCleanMode: function() {
		    self.scope.enableButtons();	
		},

                create: function() {

                    // NO-OP IF NOT VALID
                    if( !self.scope.isTagValid ){
                      return;
                    }

                    // only allow ten tags
                    if( self.scope.tags.length >= 10 ) {
                      var limit = self.scope.tags.length;
                      // length limit, but have any been deleted?
                      self.scope.tags.each( function(t, idx) {
                        if (t.get('_deleted')) {
                          limit--;
                        }
                      });
                      if (limit >=  10) {
                        self.scope.error.set('name', app.msg('tag_limit_error_name'));
                        self.scope.error.set('value', app.msg('tag_limit_error_value'));
                        return;
                      }
                    }

                    var newt = new Tag(self.scope.newtag.toJSON());
                    newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: true});
                    newt.set('res_id', model.get('id'));
                    if (newt.get('name') && newt.get('value') && newt.get('name') !== '' && newt.get('value') !== '') {
                        console.log('create', newt);
                        self.scope.tags.add(newt);
                        self.scope.newtag.clear();
                        self.scope.isTagValid = true;
                        self.scope.error.clear();
                        self.render();
                    }
                },
                
                edit: function(element, scope) {
                    console.log('edit');

		    self.scope.enterTagEditMode();
                    
                    // RETREIVE THE ID OF THE TAG
                    var tagID = scope.tag.get('id');
                    console.log("TAG ID: " + tagID);

                    // STORE THE ORIGINAL KEY-VALUE WHEN FIRST EDITED: _FIRSTBACKUP
                    if( scope.tag.get('_firstbackup') == undefined ){
                      scope.tag.set('_firstbackup', scope.tag.clone());
                    }
		    // KEEP THE PREVIOUS TAG AS _BACKUP 
                    scope.tag.set('_backup', scope.tag.clone());

                    // MARK THE STATE AS _EDIT
                    scope.tag.set({_clean: false, _deleted: false, _edited: false, _edit: true, _displayonly: false});

                    // SET UP VALIDATE ON THIS TAG
                    scope.tag.on('change', function(thisTag) {
                      scope.error.clear();
                      scope.error.set(scope.tag.validate());
                      thisTag.set('tag_is_invalid', !scope.tag.isValid());
                    });

                    // VERIFY THE VALIDATION STATUS
                    scope.tag.on('validated', function(model) {
                      scope.isTagValid = scope.tag.isValid();
                    });
                },

                confirm: function(element, scope) {
                   
                    // NO-OP IF NOT VALID 
                    if( !scope.isTagValid ){
                      return;
                    }

                    if (scope.tag.get('name') != scope.tag.get('_backup').get('name')) {
                        scope.tag.set('id', undefined);
                    } else {
                        scope.tag.set('_backup', undefined);
                    }
                    scope.tag.set({_clean: true, _deleted: false, _edited: true, _edit: false});
		    self.scope.enterCleanMode();
                    self.render();
                },

                restore: function(element, scope) {
                    if ( scope.tag.get('_backup') != null ) {
                        scope.tag.set( scope.tag.get('_backup').toJSON() );
                    } else {
                        scope.tag.set({_clean: true, _deleted: false, _edit: false});
                    }

                    scope.error.clear();
		    self.scope.enterCleanMode();
                    self.render();
                },

                delete: function(element, scope) {
                    console.log('delete');
		    // ALWAYS BACKUP BEFORE DELETE
                    scope.tag.set( '_backup', scope.tag.clone() );
                    scope.tag.set({_clean: false, _deleted: true, _edit: false});
                },
            } // end of scope

            self.scope.newtag.validation.res_id.required = false;

            self.scope.newtag.on('change', function() {
              self.scope.error.clear();
              self.scope.error.set(self.scope.newtag.validate());
              self.scope.newtag.set('tag_is_invalid', !self.scope.newtag.isValid());
            });

            self.scope.newtag.on('validated', function() {
              self.scope.isTagValid = self.scope.newtag.isValid();
              console.log("isTagValid: " + self.scope.newtag.isValid());
            });

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, this.scope);
            this.render(this.scope);
        },
        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
