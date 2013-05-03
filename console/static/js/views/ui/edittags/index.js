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
                tags.each(function(t) {
                   if (t.get('_new') && !t.get('_deleted')) { 
                       // CANNOT CALL .SAVE() WHEN THERE EXISTS A TEMP ID   --- KYO 043013
                       t.save();
                       //t.sync('create', t);
                   } else if (t.get('_deleted')) {
                       // STRANGE CASE: CANNOT USE .DESTORY(); IT SKIPS THE NEXT TAG IN THE LOOP   --- KYO 042613
                       if (t.get('_backup')) {
                           t.get('_backup').destroy();
                       } else {
                           t.destroy();
                       }
                   } else if (t.get('_edited')) {
                       if( t.get('_backup').get('name') != t.get('name') ){
                         // CASE OF KEY CHANGE
                         t.get('_backup').destroy();
                         t.save();
                         console.log("Deleted a Tag: " + t.get('name') + ":" + t.get('value'));
                       }else{
                         // CASE OF VALUE CHANGE
                         t.save();
                       } 
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
                    console.log('restore');
                    scope.tag.set({_clean: true, _deleted: false});
                    self.render();
                },

                delete: function(element, scope) {
                    console.log('delete');
                    scope.tag.set({_clean: false, _deleted: true});
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
