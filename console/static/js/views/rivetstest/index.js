define([
    'app',
	'dataholder',
	'text!./template.html!strip',
    'rivets',
    'views/dialogs/testdialog',
    'views/dialogs/quickscaledialog',
    'models/scalinggrp',
    'views/searches/volume',
    'models/launchconfig',
    'models/tag'
 ], function( app, dh, template, rivets, TestDialog, QuickScaleDialog, ScalingGroup, Search, LaunchConfig, Tag) {
	return Backbone.View.extend({
		initialize : function() {
			var self = this;
			this.view = this;

            var test = new Backbone.Model({
                    value: 'foobarbaz',
                    toggle: true
                });
                
                console.log("VOLUMES: " + JSON.stringify(app.data.scalingGroups))
                
                for (var key in app.data) {
                  console.log('KEY ' + key + ': ' + JSON.stringify(app.data[key]));
                }
                

            var explicitFacets = {
              architecture : [{name : 'i386', label: 'i386 32-bit'}, {name : 'x86_64' , label : 'AMD64 64-bit'}]
            };
            var facetNames = ['architecture', 'description', 'name', 'owner', 'platform', 'root_device'];
            
            var customSearches = {
              description : function(facet, search, images) {
                return ["Custom custom!"]
              }
            }

            var sg = new ScalingGroup({name: 'a test group'});
            var tag = new Tag({resource_type: 'instance'});
            var lc = new LaunchConfig({name: 'my launch configuration'});


            var scope = {
                errors: new Backbone.Model({}),
                lc_errors: new Backbone.Model({}),
                test: test,
                sg: sg,
                tag: tag,
                lc: lc,
                search : new Search(app.data.images, 
                              facetNames, 
                              {architecture : 'System Type'}, 
                              explicitFacets, 
                              customSearches),
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
                        app.dialog('quickscaledialog', scope);
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
                        var model = new ScalingGroup({name:'atestgroup3', launch_config:'atestlc', min_size:'2', max_size:'2', default_cooldown:'555', zones:'cluster01'});
                        model.save();
                      });
                    }
                },
                setDesiredCapacity: {
                    click: function() { 
                      require(['models/scalinggrps'], function(collection) {
                        var model = new collection();
                        model.fetch({ success: function(model, response){
                                        console.log(model.models.length+" groups returned");
                                        for (var idx in model.models) {
                                          var grp = model.models[idx];
                                          console.log(JSON.stringify(grp));
                                          if (grp.get('name') == 'atestgroup') {
                                            grp.setDesiredCapacity(3);
                                          }
                                        }
                                      }
                                    });
                      });
                    }
                },
                setInstanceHealth: {
                    click: function() { 
                      require(['models/scalinginsts'], function(collection) {
                        var model = new collection();
                        model.fetch({ success: function(model, response){
                                        console.log(model.models.length+" instances returned");
                                        for (var idx in model.models) {
                                          var grp = model.models[idx];
                                          console.log(JSON.stringify(grp));
                                          if (grp.get('instance_id') == 'i-712F3FD7') {
                                            grp.setInstanceHealth("Unhealthy");
                                          }
                                        }
                                      }
                                    });
                      });
                    }
                },
                terminateInstance: {
                    click: function() { 
                      require(['models/scalinginsts'], function(collection) {
                        var model = new collection();
                        model.fetch({ success: function(model, response){
                                        console.log(model.models.length+" instances returned");
                                        for (var idx in model.models) {
                                          var grp = model.models[idx];
                                          console.log(JSON.stringify(grp));
                                          if (grp.get('instance_id') == 'i-22AA42DC') {
                                            grp.terminateInstance();
                                          }
                                        }
                                      }
                                    });
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

            tag.on('change', function() {
               scope.errors.clear();
               scope.errors.set(tag.validate());
            });
 
            lc.on('change', function() {
               scope.lc_errors.clear();
               scope.lc_errors.set(lc.validate());
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
