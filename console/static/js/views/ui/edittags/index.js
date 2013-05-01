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
                       // CANNOT CALL .SAVE() WHEN THERE EXISTS A TEMP ID   --- KYO 043013
                       //t.save();
                       t.sync('create', t);
                   }else if (t.get('_deleted')) {
                       // STRANGE CASE: CANNOT USE .DESTORY(); IT SKIPS THE NEXT TAG IN THE LOOP   --- KYO 042613
//                       t.destroy();
                       t.sync('delete', t);
                   }else if (t.get('_edited')) {
                       if( t.get('_prev_name') != t.get('name') ){
                         // CASE OF KEY CHANGE
                         var newTag = new Tag({res_id: t.get('res_id'), name: t.get('name'), value: t.get('value')});
                         newTag.sync('create', newTag);
                         console.log("Created a New Tag: " + newTag.get('name') + ":" + newTag.get('value'));

                         t.set('name', t.get('_prev_name'));
                         t.set('value', t.get('_prev_value'));
                         t.sync('delete', t);
                         console.log("Deleted a Tag: " + t.get('name') + ":" + t.get('value'));
                       }else{
                         // CASE OF VALUE CHANGE
                         t.sync('create', t);
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
                    console.log('create');
                    var newt = new Tag(self.scope.newtag.toJSON());
                    newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: true});
                    newt.set('res_id', model.get('id'));
                    if (newt.get('name') && newt.get('value') && newt.get('name') !== '' && newt.get('value') !== '') {
                        // TEMP ID IS NEEDED SO THAT THE NEWLY CREATED TAG CAN BE TREATED AS IF AN EXISTING TAG   --- KYO 043013
                        var tempID = model.get('id') + "-" + newt.get('name') + "-temp";
                        console.log("TEMP ID: " + tempID);
                        newt.set('id', tempID);
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

                    // STORE THE ORIGINAL TAG IN THE BACKUP COLLECTION
                    backup.remove({id: tagID});
                    backup.add(scope.tag.toJSON());

                    // NEW METHOD: STORE THE ORIGINAL KEY-VALUE WHEN FIRST EDITED
                    if( scope.tag.get('_prev_name') == undefined ){
                      scope.tag.set('_prev_name', scope.tag.get('name'));
                      scope.tag.set('_prev_value', scope.tag.get('value'));
                    } 

                    // MARK THE STATE AS _EDIT
                    scope.tag.set('_state','edit');
                    scope.tag.set({_clean: false, _deleted: false, _edited: false, _edit: true});
                    
                    // ID HAS TO BE ALTERED IN ORDER TO PREVENT FORCED REFRESH   --- KYO 042813
                    if( scope.tag.get('original_id') == undefined ){
                      scope.tag.set('original_id', tagID);       // PRESERVE THE ORIGINAL ID   ::: NOT SURE IF IT'S NEEDED ANYMORE, USED FOR STATE CHECK   --- KYO 043013
                      scope.tag.set('id', tagID + "-in_edit");   // ALTER THE ID TO AVOID REFRESH
                    }
                },

                confirm: function(element, scope) {
                    scope.tag.set({_clean: true, _deleted: false, _edited: true, _edit: false});
                    self.render();
                },

                restore: function(element, scope) {
                    console.log('restore');
                    // RETRIEVE THE BACKUP TAG
                    var backupTag = backup.get(scope.tag.get('id'));
                    scope.tag.set('name', backupTag.get('name'));
                    scope.tag.set('value', backupTag.get('value'));
                    scope.tag.set({_clean: true, _deleted: false, _edited: backupTag.get('_edited'), _edit: false});
                    self.render();
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
