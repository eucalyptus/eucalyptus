define([
  'app',
	'dataholder',
  'text!./security.html!strip',
  'rivets',
	], function( app, dataholder, template, rivets ) {
	return Backbone.View.extend({
    title: 'Security',

		initialize : function() {

      var self = this;
      var scope = {
        configmodel: this.model,
        sgroups: dataholder.securityGroups,
        keypairs: dataholder.keypairs,

        setSecurityGroup: function(e, item) {
          var groupWhere = this.sgroups.where({name: e.target.value});
          var group = _.find(groupWhere, function(sg) {
                        return sg.get('name') == e.target.value;
          });
          console.log("GROUPSET", this.selectedGroup);
          self.model.set('security_group', group);
          self.model.set('security_group_name', group.get('name'));
          self.model.set('security_group_rules', group.get('rules'));
          console.log("GROUPSET", this.selectedGroup);
          console.log("GROUPSET", this.groupName);
          console.log("GROUPSET", this.rules);
        }

      };
     
      this.model.bind('change:security_group', function() { 
          scope.selectedGroup = this.model.get('security_group');
          scope.groupName = this.model.get('security_group').get('name');
          scope.rules = this.model.get('security_group').get('rules');
          }, this);

console.log("SGROUPS", dataholder.securityGroups);
console.log("KEYPAIRS", dataholder.keypairs);

      $(this.el).html(template)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
		},

    render: function() {
      this.rView.sync();
    }
});
});
