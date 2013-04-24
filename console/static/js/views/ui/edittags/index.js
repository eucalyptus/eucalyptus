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
                tags.set(args.model.get('tags').models);
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
                   if (t.get('_deleted')) {
                       t.destroy();
                   }
                   if (t.get('_edited')) {
                       t.save();
                   }
                });
                model.get('tags').set(tags.models);
            });

            // ADDED TO ALLOW DIALOGS TO ADD NAME TAG  --- Kyo 042113
            model.on('add_tag', function(this_tag) {
              self.scope.tags.add(this_tag);
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
                    newt.set('res_id', model.get('id'));
                    if (newt.get('name') && newt.get('value') && newt.get('name') !== '' && newt.get('value') !== '') {
                        self.scope.tags.add(newt);
                        self.scope.newtag.clear();
                        self.render();
                    }
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
