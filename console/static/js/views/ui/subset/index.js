define([
   'underscore',
   'text!./template.html!strip',
   'backbone',
   'app'
], function(_, template, Backbone, app) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.template = template;
            var model = args.model;

            var available = model.get('available');
            var selected = model.get('selected');
            var getId = model.get('getId')
            var getValue = model.get('getValue')

            var scope = new Backbone.Model({
                available: available,
                selected: selected,
                error: model.get('error'),
                toAdd: null,

                getId: function() {
                    return getId(this.item);
                },
                getValue: function() {
                    return getValue(this.item);
                },

                add: function(element, scope) {
                    var toAdd = scope.get('toAdd');
                    if (selected.filter(function(it) {
                            return getId(it) == toAdd;
                        }).length == 0) {
                        selected.add(available.filter(function(it) {
                            return getId(it) == toAdd;
                        }));
                        self.render();
                    }
                    console.log('add - selected:', selected);
                },

                delete: function(element, scope) {
                    selected.remove(getId(this.item));
                    self.render();
                    console.log('delete - selected:', selected);
                },
            }); // end of scope

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, scope);

            scope.get('available').on('sync', function() {
                console.log('SYNC');
                self.render();
            });
        },
        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
