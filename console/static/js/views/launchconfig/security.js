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
        },

        isKeySelected: function(obj) { 
          if (this.keymodel.get('name') == obj.keypair.get('name')) {
            return true;
          } 
          return false;
        },

        isGroupSelected: function(obj) { 
          if (this.configmodel.get('name') == obj.sgroup.get('name')) {
            return true;
          } 
          return false;
        },

        launchConfigErrors: {
          key: null,
          group: null
        },

        newKeyPair: function() {
            addKeypair( function(){ 
              var oldKeypairs = [];
              $.each($section.find('#launch-wizard-security-keypair-selector').find('option'), function(){
                oldKeypairs.push($(this).val());
              });

              var numKeypairs = oldKeypairs.length;
              refresh('keypair'); 
              thisObj._keypairCallback = runRepeat(function(){
                populateKeypair(oldKeypairs); 
                if($section.find('#launch-wizard-security-keypair-selector').find('option').length  > numKeypairs){
                  cancelRepeat(thisObj._keypairCallback);
                }
              }, 2000); 
            });
        },

        newSecGroup: function() {
          addGroup( function() {
              var oldGroups = [];
              $.each($section.find('#launch-wizard-security-sg-selector').find('option'), function(){
                oldGroups.push($(this).val());
              });
              var numGroups = oldGroups.length;
              refresh('sgroup');
              thisObj._sgCallback = runRepeat(function(){ 
                populateSGroup(oldGroups);
                if($section.find('#launch-wizard-security-sg-selector').find('option').length > numGroups){
                  cancelRepeat(thisObj._sgCallback);
                }  
             }, 2000);
            });
        }


      };
     
      self.model.on('validated:invalid', function(obj, errors) {
        scope.launchConfigErrors.group = errors.name;
        self.render();
      });

      self.model.on('validated:valid', function(obj, errors) {
        scope.launchConfigErrors.group = null;
        self.render();
      });

      this.options.keymodel.on('validated:invalid', function(obj, errors) {
        scope.launchConfigErrors.key = errors.name;
        self.render();
      });

      this.options.keymodel.on('validated:valid', function(obj, errors) {
        scope.launchConfigErrors.key = null;
        self.render();
      });

      $(this.el).html(template)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
		},

    render: function() {
      this.rView.sync();
      console.log("KEY", this.options.keymodel);
    },

    isValid: function() {
      this.options.keymodel.validate(_.pick(this.options.keymodel.toJSON(), 'name'));
      if (!this.options.keymodel.isValid()) {
          return false;
      }
      this.model.validate(_.pick(this.model.toJSON(), 'name'));
      if(!this.model.isValid()) {
        return false;
      }
      return true;
    }
});
});
