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
                alarms: app.data.alarm,
                policies: new Backbone.Collection()
            });
            $(this.el).html(template);
            this.rview = rivets.bind(this.$el, scope);

            app.data.alarm.on('sync', function() { self.render(); });
            app.data.alarm.fetch();
          },

          render: function() {
            this.rview.sync();
          }
        });
});
