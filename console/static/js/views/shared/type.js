define([
  'app',
	'dataholder',
  'text!./type.html!strip',
  'rivets',
	], function( app, dataholder, template, rivets ) {
  return Backbone.View.extend({
    tpl: template,
    title: 'Type',

    initialize : function() {

      var self = this;
      this.model.set('tags', new Backbone.Collection());
      this.model.set('zones', dataholder.zone);
      this.model.set('type_names', new Backbone.Collection());
      this.t_names = this.model.get('type_names');

      // for the instance types/sizes pulldown, sorted asc
      var typesTemp = new Backbone.Collection();
      typesTemp.comparator = function(type) {
          return (+type.get('cpu')) + (+type.get('ram'));
      };
      var typeObjects = $.eucaData.g_session['instance_type'];
      _.each(typeObjects, function(val, key) {
        typesTemp.add({name: key, cpu: val[0], ram: val[1], disk: val[2]});
      });
      this.model.set('types', typesTemp.sort());

      this.model.set('type_number', 1); // preload 1
      this.model.set('zone', 'Any'); // preload no zone preference


      var scope = {
        typeModel: self.model,

        isZoneSelected: function(obj) { 
          if (self.model.get('zone') == obj.zone.get('name')) {
            return true;
          } 
          return false;
        },


        setField: function(e, el) {
          var target = e.target;
          switch(target.id) {
            case 'launch-instance-names':
              if(target.value == '') {
                self.t_names.reset();
              } else {
                var names = target.value.split(',');
                self.t_names.reset();
                for(i=0; i<names.length; i++) {
                  var trimmed = names[i].replace(/^\s\s*/, '').replace(/\s\s*$/, '');
                  self.t_names.add({name: "Name", value: trimmed});
                }
              }
              break;
            default:
          }
        },

        iconClass: function() {
          return self.model.get('image_iconclass'); 
        },

        tags:  self.model,

        formatType: function(obj) {
          var buf = obj.type.get('name') + ": ";
          buf += obj.type.get('cpu') + " " + app.msg('launch_wizard_type_description_cpus') + ", ";
          buf += obj.type.get('ram') + " " + app.msg('launch_wizard_type_description_memory') + ", ";
          buf += obj.type.get('disk') + " " + app.msg('launch_wizard_type_description_disk') + ", ";
          return buf;
        },

        isSelected: function(objects) {
          if(objects.type.get('name') == self.model.get('instance_type')) {
            return true;
          }
        },
    
        launchConfigErrors: {
          type_number: '',
          instance_type: '',
          type_names_count: ''
        }
    };

    self.model.on('validated:invalid', function(model, errors) {
      scope.launchConfigErrors.type_number = errors.type_number;
      scope.launchConfigErrors.instance_type = errors.instance_type;
      scope.launchConfigErrors.type_names_count = errors.type_names_count;
      self.render();
    });

    self.model.on('validated:valid', function(model, errors) {
      scope.launchConfigErrors.type_number = null;
      scope.launchConfigErrors.instance_type = null;
      scope.launchConfigErrors.type_names_count = null;
      self.render();
    });
  
    // used for instance name/number congruity validation... see below
    self.t_names.on('add reset sync change remove', function() {
      self.model.set('type_names_count', self.model.get('type_names').length);
    });

    self.model.on('change:instance_type', function() {
      $.cookie('instance_type', self.model.get('instance_type'));
      self.render();
    });

    $(this.el).html(this.tpl);
     this.rView = rivets.bind(this.$el, scope);
     this.render();

     if($.cookie('instance_type')) {
       this.model.set('instance_type', $.cookie('instance_type'));
     } else {
       this.model.set('instance_type', 'm1.small'); // preload first choice if no cookie value
     }
    },

    render: function() {
      this.rView.sync();
    },

    isValid: function() {
      var json = this.model.toJSON();
      this.model.validate(_.pick(this.model.toJSON(),'type_number'));
      if (!this.model.isValid())
        return false;

      this.model.validate(_.pick(this.model.toJSON(),'instance_type'));
      if (!this.model.isValid())
        return false;

      // cannot pass a collection like type_names in here. Have to maintain
      // the count of the collection separately.
      this.model.validate(_.pick(json, 'type_names_count'));
      if (!this.model.isValid())
        return false;

      return true;
    },

    // called from wizard.js when each step is displayed.
    // there is also a matching blur() hook. 
    focus: function() {
      this.model.set('type_show', true);
    },

    blur: function() {
      this.model.trigger('confirm');
    },

  });
});
