define([
   'underscore',
   './eucadialogview',
   'text!./edittags.html!strip',
   'backbone',
   'models/tag'
], function(_, EucaDialogView, template, Backbone, Tag) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            var tags = new Backbone.Collection();

            // Clone the collection and add a status field;
            var origtags = args.model.get('tags');
            origtags.each(function(t) { 
                var newt = new Tag(t.toJSON());
                newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: false});
                tags.push(newt);
            });

            var backup = new Backbone.Collection();

            this.scope = {
                newtag: new Tag(),
                tags: tags, 

                status: '',

                create: function() {
                    console.log('create');
                    var newt = new Tag(self.scope.newtag.toJSON());
                    newt.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: true});
                    newt.set('id', args.model.get('id') + '-' + newt.get('name'));
                    newt.set('res_id', args.model.get('id'));
                    self.scope.tags.add(newt);
                    self.scope.newtag.clear();
                    self.render();
                },
                
                edit: function(element, scope) {
                    console.log('edit');
                    backup.remove({id: scope.tag.get('id')});
                    backup.add(scope.tag.toJSON());
                    scope.tag.set('_state','edit');
                    scope.tag.set({_clean: false, _deleted: false, _edited: false, _edit: true});
                },

                confirm: function(element, scope) {
                    console.log('edit');
                    scope.tag.set({_clean: false, _deleted: false, _edited: true, _edit: false});
                },

                restore: function(element, scope) {
                    console.log('edit');
                    scope.tag.set('value', backup.where({id: scope.tag.get('id')})[0].get('value'));
                    scope.tag.set({_clean: true, _deleted: false, _edited: false, _edit: false});
                },

                delete: function(element, scope) {
                    console.log('delete');
                    backup.remove({id: scope.tag.get('id')});
                    backup.add(scope.tag.toJSON());
                    scope.tag.set({_clean: false, _deleted: true, _edited: false, _edit: false});
                },

                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },

                confirmButton: {
                  click: function() {
                       tags.each(function(t) {
                           console.log('TAG', t);
                           if (t.get('_new') && !t.get('_deleted')) { 
                               console.log('add', t); 
                               origtags.add(t);
                           }
                           if (t.get('_deleted')) {
                               _.each(origtags.where({id: t.get('id')}), function(ot) { 
                                   console.log('remove', ot); 
                                   origtags.remove(ot) 
                               });
                           }
                           if (t.get('_edited')) {
                               console.log('update', t); 
                               _.each(origtags.where({id: t.get('id')}), function(ot) { 
                                   console.log('update run', ot, t); 
                                   ot.set('value', t.get('value'));
                               });
                           }
                       });
                       self.close();
                  }
                }
            }

            this._do_init();
        },
	});
});
