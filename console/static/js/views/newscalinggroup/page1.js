define([
  'rivets',
  'dataholder',
  'text!./page1.html',
], function(rivets, dh, template) {
    return Backbone.View.extend({
      title: 'General', 

      initialize: function(args) {
        var self = this;
        var scope =  args.scope;
        if (scope) {
            myScope = scope;
            scope.launchConfigs = {
                name: 'launchConfig',
                collection: dh.launchConfigs,
                itrLabel: function() {
                  return this.itr.get('name');
                } 
            }
            scope.scalingGroup.on('change', function() {
                self.render();
            });
        }


        $(this.el).html(template)
        this.rView = rivets.bind(this.$el, scope);
        this.render();
      },

      render: function() {
        this.rView.sync();
      }
    });
});
