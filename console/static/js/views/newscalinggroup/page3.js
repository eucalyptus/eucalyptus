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
                policies: new Backbone.Model({
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
          },

          render: function() {
            this.rview.sync();
          }
        });
});
