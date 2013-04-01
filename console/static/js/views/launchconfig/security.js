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
        keymodel: this.options.keymodel,
        sgroups: dataholder.sgroup,
        keypairs: dataholder.keypair,

        setSecurityGroup: function(e, item) {
          var groupWhere = this.sgroups.where({name: e.target.value});
          var group = _.find(groupWhere, function(sg) {
                        return sg.get('name') == e.target.value;
          });
          if(groupWhere.length > 0) {
              self.model.set(group.toJSON());
              self.model.set('security_show', true);
          }
        },

        setKeyPair: function(e, item) {
          var keyWhere = this.keypairs.where({name: e.target.value});
          var key = _.find(keyWhere, function(k) {
                      return k.get('name') == e.target.value;
          });
          this.keymodel.set(key.toJSON());
          self.model.set('security_show', true);
        }

      };
     

      $(this.el).html(template)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
		},

    render: function() {
      this.rView.sync();
    }
});
});
