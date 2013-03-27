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
                      require(['models/launchconfig'], function(config) {
                        var model = new LaunchConfig({name:'atestlc'});
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
