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
                toAdd: model.get('default_selection') ? model.get('default_selection') : null,
                add_tip: model.get('add_tip') ? model.get('add_tip') : app.msg('add_label'),
                delete_tip: model.get('delete_tip') ? model.get('delete_tip') : app.msg('delete_label'),
                select_tip: model.get('select_tip') ? model.get('select_tip') : null,

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

                isSelected: function(ctx) {
                  if(this.selected.get(ctx.item)) {
                    return true;
                  }
                }, 

                default_msg: model.get('default_msg'),

                delete: function(element, scope) {
                    selected.remove(getId(this.item));
                    self.render();
                    console.log('delete - selected:', selected);
                },
            }); // end of scope

            this.model.on('force_itemadd', function() {
              scope.get('add')(null, scope);
            });

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, scope);

            scope.get('available').on('sync', function() {
                console.log('SYNC');
                self.render();
            });

            // pull any already-selected items out of the 'available' collection
            scope.get('selected').each( function(item) {
              if( scope.get('available').get(item) ) {
                scope.get('available').remove(item);
              }
            });

            this.listenTo(scope.get('selected'), 'add remove', function(model, collection, evt) {
              if(evt.add) {
                scope.get('available').remove(model);
              } else {
                scope.get('available').add(model);
              }
            });
        },
        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
