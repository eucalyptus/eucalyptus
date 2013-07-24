define([
  'app',
  'rivets',
  'text!./page1.html',
], function(app, rivets, template) {
    return Backbone.View.extend({
      title: 'General', 

      initialize: function(args) {
        var self = this;
        var scope = this.model;

        scope.set('launchConfigs', app.data.launchConfigs);
        scope.set('scalingGroupErrors', new Backbone.Model());

        scope.get('scalingGroup').on('change', function(model) {
            scope.get('scalingGroup').validate(model.changed);
        });

        scope.get('scalingGroup').on('validated', function(valid, model, errors) {
            _.each(_.keys(model.changed), function(key) { 
                scope.get('scalingGroupErrors').set(key, errors[key]); 
            });
        });

        scope.get('launchConfigs').on('add remove sync reset change', function(model) {
            self.render();
        });

        $(this.el).html(template);
        this.rView = rivets.bind(this.$el, scope);
        this.render();
      },

      render: function() {
        this.rView.sync();
      },

      isValid: function() {
        // assert that this step is valid before "next" button works
        var sg = this.model.get('scalingGroup');
        var errors = new Backbone.Model(sg.validate());
        var valid = sg.isValid(['name', 'launch_config_name', 'min_size', 'max_size', 'desired_capacity']); 
        if(!valid)
            this.model.get('scalingGroupErrors').set(errors.changed);//.pick('name', 'launch_config_name', 'min_size', 'max_size', 'desired_capacity'));
        return valid;
      }
    });
});
