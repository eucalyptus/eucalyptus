define([
	'dataholder',
	'text!./template.html!strip',
    'rivets',
    'views/newscalinggroup/editdialog',
    'views/dialogs/testdialog',
    'views/dialogs/quickscaledialog',
], function( dh, template, rivets, EditScalingGroupDialog, TestDialog, QuickScaleDialog) {
	return Backbone.View.extend({
		initialize : function() {
			var self = this;
			this.view = this;
			this.test = new Backbone.Model({
				value: 'foobarbaz'
			});
			this.buttonScope = {
				test: this.test,
				click: function() { 
					console.log('Click occurred');
					self.test.set('value', 'button click'); 
                    var dialog = new TestDialog();
				}
			}

			this.quickScaleButton = {
				click: function() { 
                    var dialog = new QuickScaleDialog();
				}
			}

			this.scalingDialog = {
				click: function() { 
                    var dialog = new EditScalingGroupDialog();
				}
			}

			this.sGroups = dh.scalingGroups;
			this.$el.html(template);
			this.rivetsView = rivets.bind(this.$el, this);
			$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			this.render();
		},
		doit : function(e, context) {
			console.log('DOIT', arguments);
			this.test.set({value: context.sg.get('name')});
		},
		render : function() {
            this.rivetsView.sync();
			return this;
		}
	});
});
