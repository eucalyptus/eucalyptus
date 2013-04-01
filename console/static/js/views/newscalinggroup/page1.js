define([
  'rivets',
  'dataholder',
  'text!./page1.html',
], function(rivets, dh, template) {
    return Backbone.View.extend({
      title: 'General', 

      initialize: function(args) {
        var self = this;
        var scope = this.model;

        scope.launchConfigs = {
            name: 'launchConfig',
            collection: dh.launchConfigs,
            itrLabel: function() {
              return this.itr.get('name');
            } 
        }

        scope.scalingGroupErrors = new Backbone.Model();

        scope.scalingGroup.on('change', function(model) {
            console.log('CHANGE', arguments);
            scope.scalingGroup.validate(model.changed);
        });

        scope.scalingGroup.on('validated', function(valid, model, errors) {
            _.each(_.keys(model.changed), function(key) { 
                scope.scalingGroupErrors.set(key, errors[key]); 
            });
        });

        $(this.el).html(template);
        this.rView = rivets.bind(this.$el, scope);
        this.render();
      },

      render: function() {
        this.rView.sync();
      }
    });
});
