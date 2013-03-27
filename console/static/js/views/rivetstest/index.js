define([
	'dataholder',
	'text!./template.html!strip',
    'rivets',
    'views/dialogs/testdialog',
    'views/dialogs/quickscaledialog',
    'models/scalinggrp',
    'models/launchconfig'
], function( dh, template, rivets, TestDialog, QuickScaleDialog, ScalingGroup, LaunchConfig) {
	return Backbone.View.extend({
		initialize : function() {
			var self = this;
			this.view = this;

            var test = new Backbone.Model({
                    value: 'foobarbaz',
                    toggle: true
                });

            var sg = new ScalingGroup({name: 'a test group'});
            var scope = {
                errors: new Backbone.Model({}),
                test: test,
                sg: sg,
                buttonScope: {
                    test: test,
                    click: function() { 
                        console.log('Click occurred');
                        test.set('value', 'button click'); 
                        var dialog = new TestDialog();
                    }
                },
                quickScaleButton: {
                    click: function() { 
                        var dialog = new QuickScaleDialog();
                    }
                },
                makeLaunchConfig: {
                    click: function() { 
                      require(['models/launchconfigs'], function(collection) {
                        var model = new collection();
                        model.fetch({ success: function(model, response){
                                        console.log(JSON.stringify(model));
                                        for (var idx in model.models) {
                                          var lc = model.models[idx];
                                          if (lc.get('name') == 'atestlc') {
                                            lc.destroy({wait:true});
                                          }
                                        }
                                      }
                                    });
                      });
                      require(['models/launchconfig'], function(LaunchConfig) {
                        var model = new LaunchConfig({name:'atestlc', image_id:'emi-F5373E2F', instance_type:'t1.micro', instance_monitoring:'true'});
                        model.save();
                      });
                    }
                },
                makeScalingGroup: {
                    click: function() { 
                      require(['models/scalinggrps'], function(collection) {
                        var model = new collection();
                        model.fetch({ success: function(model, response){
                                        console.log(model.models.length+" groups returned");
                                        for (var idx in model.models) {
                                          var grp = model.models[idx];
                                          console.log(JSON.stringify(grp));
                                          if (grp.get('name') == 'atestgroup') {
                                            ;//grp.destroy({wait:true});
                                          }
                                        }
                                      }
                                    });
                      });
                      require(['models/scalinggrp'], function(ScalingGroup) {
                        var model = new ScalingGroup({name:'atestgroup2', launch_config:'atestlc', min_size:'0', max_size:'1', default_cooldown:'444', zones:'cluster01'});
                        model.save();
                      });
                    }
                },
                scalingDialog: {
                    click: function() { 
                    }
                },
                showName: function() {
                    return this.sg.get('name');
                },
                doit : function(e, context) {
                    console.log('DOIT', arguments);
                    this.test.set({value: context.sg.get('name')});
                },
                sGroups: dh.scalingGroups
            }
            this.scope = scope;
            rScope = scope;

            sg.on('change', function() {
               scope.errors.clear();
               scope.errors.set(sg.validate()); 
            });

			this.$el.html(template);
			this.rivetsView = rivets.bind(this.$el, this.scope);
			$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			this.render();
		},
		render : function() {
            this.rivetsView.sync();
			return this;
		}
	});
});
