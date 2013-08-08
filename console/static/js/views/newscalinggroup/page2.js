console.log('WIZARD:start');
define([
  'rivets',
  'app',
  'text!./page2.html',
], function(rivets, app, template) {
        return Backbone.View.extend({
          title: app.msg('create_scaling_group_section_header_membership'),
          next: app.msg('create_scaling_group_btn_next_policies'), 

          initialize: function() {
            var self = this;
            $(this.el).html(template)

            var scope = new Backbone.Model({
                scalingGroup: this.model.get('scalingGroup'),
                scalingGroupErrors: this.model.get('scalingGroupErrors'),
                loadBalancers: new Backbone.Model({
                    name: 'loadBalancers',
                    default_msg: app.msg('create_scaling_group_general_loadbalancer_select'),
                    available: app.data.loadbalancer,
                    selected: self.model.get('loadBalancers'),
                    add_tip: app.msg('create_scaling_group_memb_add_lb_tip'),
                    delete_tip: app.msg('create_scaling_group_memb_del_lb_tip'),
                    select_tip: app.msg('create_scaling_group_memb_select_lb_tip'),
                    getId: function(item) {
                        return item.get('name');
                    },
                    getValue: function(item) {
                        return item.get('name');
                    }
                }),
                zoneSelect: new Backbone.Model({
                    available: app.data.availabilityzone,
                    selected: self.model.get('availabilityZones'),
                    add_tip: app.msg('create_scaling_group_memb_add_az_tip'),
                    delete_tip: app.msg('create_scaling_group_memb_del_az_tip'),
                    select_tip: app.msg('create_scaling_group_memb_select_az_tip'),
                    getId: function(item) {
                        return item.get('name');
                    },
                    getValue: function(item) {
                        return item.get('name');
                    }
                })
            });

            //default
            if(scope.get('zoneSelect').get('selected').length == 0) {
              this.listenToOnce(scope.get('zoneSelect').get('available'), 'sync', function() {
                scope.get('zoneSelect').set('default_selection', app.data.availabilityzone.at(0).get('name'));
              });
            }

            this.rview = rivets.bind(this.$el, scope);

            scope.get('zoneSelect').get('available').fetch();

            this.scope = scope;
          },

          render: function() {
            this.rview.sync();
            return this;
          },

          focus: function() {
            this.model.get('scalingGroup').set('showPage2', true);
          },

          isValid: function() {
            // make ui-subset emulate tag editor - don't require + button to add current selection
            this.scope.get('zoneSelect').trigger('force_itemadd');
            this.scope.get('loadBalancers').trigger('force_itemadd');

            // assert that this step is valid before "next" button works
            var sg = this.model.get('scalingGroup');
            var errors = new Backbone.Model(sg.validate());
            var valid = sg.isValid(['availability_zones', 'health_check_type']); 
            if(!valid)
                this.model.get('scalingGroupErrors').set(errors.pick('availability_zones', 'health_check_type'));
            return valid;
          }
        });
});
