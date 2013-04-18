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

            var tags = args.model.get('tags');
            tags.each(function(t) {
                t.set({_clean: true, _deleted: false, _edited: false, _edit: false, _new: false});
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
            } // end of scope

            args.model.on('submit', function() {
                self.scope.create();
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
