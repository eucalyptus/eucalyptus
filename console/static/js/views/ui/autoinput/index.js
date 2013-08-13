define([
   'underscore',
   'text!./template.html!strip',
   'backbone'
], function(_, template, Backbone) {
    return Backbone.View.extend({
        initialize : function(args) {
            var self = this;

            this.template = template;
            var model = args.model;

            if (model.getValue == undefined) {
                model.getValue = function(map) {
                    return map.id;
                }
            }

            var available = model.get('available');

            var scope = new Backbone.Model({
                inputId: model.get('inputId'),
                available: available,
                error: model.get('error'),
            }); // end of scope

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, scope);

            $('input', this.$el).autocomplete({
                source: available.map(function(it) { return model.getValue(it); }),
                select: function(event, ui) {
                    model.set('value', ui.item.value);
                }
            });

            scope.get('available').on('sync', function() {
                $('input', self.$el).autocomplete(
                    'option', 
                    'source', 
                    available.map(function(it) { return model.getValue(it); })
                );
                self.render();
            });
        },
        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
