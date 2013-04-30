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

            //var tags = new Backbone.Collection();

            // Clone the collection and add a status field;
            /*
            var origtags = args.model.get('tags');
            origtags.each(function(t) { 
                var newt = new Tag(t.toJSON());
                newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: false});
                tags.push(newt);
            });
            */

            var model = args.model;
            var tags = new Backbone.Collection();

            var loadTags = function() {
                tags.set(args.model.get('tags').filter(function(t) { return !/^euca:/.test(t.get('name')); }));
                tags.each(function(t) {
                    t.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: false});
                });
            }

            loadTags();
            model.get('tags').on('reset', function() {
                loadTags();
            });

            model.on('confirm', function() {
                self.scope.create();
                tags.each(function(t) {
                   if (t.get('_new') && !t.get('_deleted')) { 
                       t.save();
                   }
                   if (t.get('_deleted') || t.get('_replaced')) {
                       // STRANGE CASE: CANNOT USE .DESTORY(); IT SKIPS THE NEXT TAG IN THE LOOP   --- KYO 042613
//                       t.destroy();
                       t.sync('delete', t);
                   }
                   if (t.get('_edited')) {
                       t.sync('create', t);
                   }
                });
                // THE OPERATION BELOW MIGHT NOT WORK WHEN .SYNC() CALLS ARE USED   --- KYO 042913
                model.get('tags').set(tags.models);
            });

            // ADDED TO ALLOW DIALOGS TO ADD NAME TAG  --- Kyo 042113
            model.on('add_tag', function(this_tag) {
              self.scope.tags.add(this_tag);
            });

            // BACKUP COLLECTION TO STORE ORIGINAL TAGS IN CASE OF EDIT AND DELETE
            var backup = new Backbone.Collection();

            this.scope = {
                newtag: new Tag(),

                tags: tags,

                status: '',

                create: function() {
                    console.log('create');
                    var newt = new Tag(self.scope.newtag.toJSON());
                    newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: true});
                    newt.set('res_id', model.get('id'));
                    if (newt.get('name') && newt.get('value') && newt.get('name') !== '' && newt.get('value') !== '') {
                        self.scope.tags.add(newt);
                        self.scope.newtag.clear();
                        self.render();
                    }
                },
                
                edit: function(element, scope) {
                    console.log('edit');

                    // RETREIVE THE ID OF THE TAG
                    var tagID = scope.tag.get('id');
                    console.log("TAG ID: " + tagID);

                    // IF NOT DEFINED, DUE TO THE PREVIOUS EDIT, CREATE A FAKE ID 
                    if( tagID == undefined ){
                      tagID = scope.tag.get('res_id') + "-" + scope.tag.get('name');
                      scope.tag.set('id', tagID);
                      console.log("TAG ID: " + tagID);
                    }

                    // STORE THE ORIGINAL TAG IN THE BACKUP COLLECTION
                    backup.remove({id: tagID});
                    backup.add(scope.tag.toJSON());

                    // MARK THE STATE AS _EDIT
                    scope.tag.set('_state','edit');
                    scope.tag.set({_clean: false, _deleted: false, _edited: false, _edit: true});
                    
                    // ID HAS TO BE ALTERED IN ORDER TO PREVENT FORCED REFRESH   --- KYO 042813
                    scope.tag.set('original_id', tagID);   // PRESERVE THE ORIGINAL ID
                    scope.tag.set('id', 'in_edit');        // ALTER THE ID
                },

                confirm: function(element, scope) {
                    // ADDED TO ALLOW KEY MODIFICATION   --- KYO 042613
                    var origTag = backup.get(scope.tag.get('original_id'));

                    if( scope.tag.get('name') != origTag.get('name') ){
                      // ===================
                      // CASE OF NAME CHANGE
                      // ===================

                      // CREATE A NEW TAG WITH THE NEW KEY
                      var newName = scope.tag.get('name');
                      var newValue = scope.tag.get('value');
                      var newTag = new Tag({res_id: model.get('id'), name: newName, value: newValue});
                      newTag.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: true});
                      tags.add(newTag);

                      // DELETE THE ORIGINAL TAG
                      var origName = origTag.get('name');
                      var origValue = origTag.get('value');
                      scope.tag.set({name: origName, value: origValue});   // REVERT TO THE ORIGINAL NAME AND VALUE
                      scope.tag.set({_clean: false, _deleted: false, _edited: false, _edit: false, _replaced: true});
                    }else{
                      // ====================
                      // CASE OF VALUE CHANGE
                      // ====================
                      scope.tag.set({_clean: true, _deleted: false, _edited: true, _edit: false});
                    }
                    self.render();
                },

                restore: function(element, scope) {
                    console.log('restore');
                    scope.tag.set('value', backup.where({id: scope.tag.get('id')})[0].get('value'));
                    scope.tag.set({_clean: true, _deleted: false, _edited: false, _edit: false});
                },

                delete: function(element, scope) {
                    console.log('delete');
                    backup.remove({id: scope.tag.get('id')});
                    backup.add(scope.tag.toJSON());
                    scope.tag.set({_clean: false, _deleted: true, _edited: false, _edit: false});
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
