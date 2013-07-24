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

            var scope;
            scope = new Backbone.Model({
                alarms: app.data.alarm,
                available: available,
                selected: selected,
                error: model.get('error'),
                toAdd: new Backbone.Model(),

                getId: function() {
                    return getId(this.item);
                },

                getValue: function() {
                    return getValue(this.item);
                },

                add: function(element, scope) {
                    var toAdd = scope.get('toAdd');
                    toAdd.set('alarm_model', scope.get('alarms').findWhere({name: toAdd.get('alarm')}));
                    selected.push(toAdd);
                    scope.set('toAdd', new Backbone.Model());
                    self.render();
                    console.log('add - selected:', selected);
                },

                delete: function(element, scope) {
                    selected.remove(this.item);
                    self.render();
                    console.log('delete - selected:', selected);
                },

                createAlarm: function(element, scope) {
                    app.dialog('create_alarm', {});
                }
            }); // end of scope

            this.$el.html(template);
            this.rview = rivets.bind(this.$el, scope);

            scope.get('available').on('sync', function() {
                console.log('SYNC');
                self.render();
            });

            app.data.alarm.on('sync', function() { self.render(); });
            app.data.alarm.fetch();
        },
        render : function() {
          this.rview.sync();
          return this;
        }
    });
});
