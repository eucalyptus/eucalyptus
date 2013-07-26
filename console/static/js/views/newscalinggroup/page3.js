define([
  'app',
  'backbone',
  'rivets',
  'text!./page3.html',
], function(app, Backbone, rivets, template) {
        return Backbone.View.extend({
          title: 'Policies', 

          initialize: function() {
            var self = this;

            var scope = new Backbone.Model({
                scalingGroup: this.model.get('scalingGroup'),
                alarms: this.model.get('alarms'),
                policies: new Backbone.Model({
                    error: new Backbone.Model(),
                    available: new Backbone.Collection(),
                    selected: self.model.get('policies'),
                    getId: function(item) {
                        return item;
                    },
                    getValue: function(item) {
                        return item;
                    }
                })
            });
            $(this.el).html(template);
            this.rview = rivets.bind(this.$el, scope);
           
          this.listenTo(this.model.get('scalingGroup'), 'change:name', function(model, value) {
            scope.get('policies').set('as_name', value);
          });
            
          },


          render: function() {
            this.rview.sync();
          },

       });
});
