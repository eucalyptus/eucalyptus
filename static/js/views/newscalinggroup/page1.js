define([
  'app',
  'rivets',
  'text!./page1.html',
], function(app, rivets, template) {
    return Backbone.View.extend({
      title: app.msg('create_scaling_group_section_header_general'), 
      next: app.msg('create_scaling_group_btn_next_membership'),

      initialize: function(args) {
        var self = this;
        var scope = this.model;

        // if editing existing model, make name immutable (aws prohibits changing it)
        if(this.model.get('scalingGroup').get('autoscaling_group_arn')) {
          this.model.set('editing', true);
        }

        scope.set('launchConfigs', app.data.launchConfigs);
        scope.set('scalingGroupErrors', new Backbone.Model());

        scope.get('scalingGroup').on('change', function(model) {
            scope.get('scalingGroup').validate(model.changed);
        });

        scope.get('scalingGroup').on('validated', function(valid, model, errors) {
            scope.get('scalingGroupErrors').clear();
            _.each(_.keys(errors), function(key) { 
                scope.get('scalingGroupErrors').set(key, errors[key]);
                if(errors[key] == undefined) {
                  scope.get('scalingGroupErrors').unset(key); 
                }
            });
            self.trigger('validationchange', scope.get('scalingGroupErrors'), 'sgerr');
        });

        scope.get('launchConfigs').on('add remove sync reset change', function(model) {
            self.render();
        });

        
        // uncomment for EUCA-7140
        //if(!scope.get('scalingGroup').get('launch_config_name')) {
        //  scope.get('scalingGroup').set('launch_config_name', scope.get('launchConfigs').at(0).get('name'));
        //}

        $(this.el).html(template);
        this.rView = rivets.bind(this.$el, scope);
        //this.render();
      },

      render: function() {
        this.rView.sync();
        return this;
      },

      isValid: function() {
        // assert that this step is valid before "next" button works
        var sg = this.model.get('scalingGroup');
        var errors = new Backbone.Model(sg.validate());
        var valid = sg.isValid(['name', 'launch_config_name', 'min_size', 'max_size', 'desired_capacity']); 
        if(!valid)
            this.model.get('scalingGroupErrors').set(errors.pick('name', 'launch_config_name', 'min_size', 'max_size', 'desired_capacity'));
        return valid;
      }
    });
});
