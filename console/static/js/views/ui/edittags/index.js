define([
   'underscore',
   'text!./template.html!strip',
   'backbone',
   'models/tag'
], function(_, template, Backbone, Tag) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.template = template;
            var model = args.model;
            var tags = new Backbone.Collection();

            var loadTags = function() {
                model.get('tags').each(function(t) {
                    if (!/^euca:/.test(t.get('name'))) {
                        var nt = new Tag(t.pick('id','name','value','res_id'));
                        nt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: false});
                        tags.add(nt);
                    }
                });
            }

            loadTags();

            model.on('confirm', function() {
                self.scope.create();
                _.chain(tags.models).clone().each(function(t) {
                   var backup = t.get('_backup');
                   console.log('TAG',t);
                   if (t.get('_deleted')) {
                       // If the tag is new and then deleted, do nothing.
                       if (!t.get('_new')) {
                           // If there is a backup then this was edited and we really want to destroy the original
                           if (backup != null) {
                               console.log('delete', backup);
                               backup.destroy();
                           } else {
                               console.log('delete', t);
                               t.destroy();
                           }
                       }
                   } else if (t.get('_edited')) {
                       // If the tag is new then it should only be saved, even if it was edited.
                       if (t.get('_new')) {
                         t.save();
                       } else if( (backup != null) && (backup.get('name') !== t.get('name')) ){
                         // CASE OF KEY CHANGE
                         console.log("Edited, with previous value: " + t.get('name') + ":" + t.get('value'));
                         t.get('_backup').destroy();
                         t.save();
                       }else{
                         // CASE OF VALUE CHANGE
                         t.save();
                       } 
                   } else if (t.get('_new')) {
                       t.save();
                   }
                });
                // THE OPERATION BELOW MIGHT NOT WORK WHEN .SYNC() CALLS ARE USED   --- KYO 042913
                model.get('tags').set(tags.models);
            });

            // ADDED TO ALLOW DIALOGS TO ADD NAME TAG  --- Kyo 042113
            model.on('add_tag', function(this_tag) {
              self.scope.tags.add(this_tag);
            });

            this.scope = {
                newtag: new Tag(),

                tags: tags,

                status: '',

                create: function() {
                    var newt = new Tag(self.scope.newtag.toJSON());
                    newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: true});
                    newt.set('res_id', model.get('id'));
                    if (newt.get('name') && newt.get('value') && newt.get('name') !== '' && newt.get('value') !== '') {
                        console.log('create', newt);
                        self.scope.tags.add(newt);
                        self.scope.newtag.clear();
                        self.render();
                    }
                },
                
                edit: function(element, scope) {
                    console.log('edit');

                    // Abort existing edits
                    tags.each(function(t) {
                        if (t.get('_edit')) {
                            t.set(t.get('_backup').pick('name','value'));
                            t.set({_clean: true, _deleted: false, _edit: false});
                        }
                    });

                    // RETREIVE THE ID OF THE TAG
                    var tagID = scope.tag.get('id');
                    console.log("TAG ID: " + tagID);

                    // NEW METHOD: STORE THE ORIGINAL KEY-VALUE WHEN FIRST EDITED
                    if( scope.tag.get('_backup') == undefined ){
                      scope.tag.set('_backup', scope.tag.clone());
                    } 

                    // MARK THE STATE AS _EDIT
                    scope.tag.set({_clean: false, _deleted: false, _edited: false, _edit: true});
                },

                confirm: function(element, scope) {
                    if (scope.tag.get('name') != scope.tag.get('_backup').get('name')) {
                        scope.tag.set('id', undefined);
                    } else {
                        scope.tag.set('_backup', undefined);
                    }
                    scope.tag.set({_clean: true, _deleted: false, _edited: true, _edit: false});
                    self.render();
                },

                restore: function(element, scope) {
                    scope.tag.set({_clean: true, _deleted: false, _edit: false});
                    self.render();
                },

                delete: function(element, scope) {
                    console.log('delete');
                    scope.tag.set({_clean: false, _deleted: true, _edit: false});
                },
            } // end of scope

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
