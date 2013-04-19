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
      this.model.set('instance_type', 'm1.small'); // preload first choice

      var scope = {
        typeModel: self.model,
        zones: self.model.get('zones'),

        isZoneSelected: function(obj) { 
          if (self.model.get('zone') == obj.zone.get('name')) {
            return true;
          } 
          return false;
        },


        setField: function(e, el) {
          var target = e.target;
          //self.model.set('type_show', 'true');
          switch(target.id) {
            case 'launch-instance-names':
              var names = target.value.split(',');
              self.model.set('type_names', names);
              //self.model.set('type_hasNames', 'true');
              //self.model.set('instance_type', $("#launch-instance-type-size").val());
              break;
            case 'launch-instance-type-num-instance':
              //self.model.set('type_number', target.value);
              break;
            case 'launch-instance-type-size':
              //self.model.set('instance_type', target.value);
              break;
            case 'launch-instance-type-az':
              //self.model.set('zone', target.value);
              break;
            default:
          }
        },

        iconClass: function() {
          return self.model.get('image_iconclass'); 
        },

        tags: {
            list: self.model.get('tags'),
            sharedModel: self.model,
            keyLabel: "Key",
            valLabel: "Value",
            add: function(e, me) {
                e.preventDefault();
              var key = $('.keyfield').val();
              var val = $('.valuefield').val();
              if(me.list.where({key: key}).length == 0) {
                me.list.add({key: key, value: val});
                me.sharedModel.set('type_hasTags', 'true');
                $('.keyfield').val('');
                $('.valuefield').val('');
              } else {
                $('.keyfield').addClass('ui-keyval-error');
              }
            },
            remove: function(e, me) {
              me.list.remove(me.row);
            },
        },

        formatType: function(obj) {
          var buf = obj.type.get('name') + ": ";
          buf += obj.type.get('cpu') + " " + app.msg('launch_wizard_type_description_cpus') + ", ";
          buf += obj.type.get('ram') + " " + app.msg('launch_wizard_type_description_memory') + ", ";
          buf += obj.type.get('disk') + " " + app.msg('launch_wizard_type_description_disk') + ", ";
          return buf;
        },
    
        launchConfigErrors: {
          type_number: '',
          instance_type: ''
        }
    };

    scope.tags.list.on('add remove change reset', function() {
        self.render();
    });

    self.model.on('validated:invalid', function(model, errors) {
      scope.launchConfigErrors.type_number = errors.type_number;
      scope.launchConfigErrors.instance_type = errors.instance_type;
      self.render();
    });

    self.model.on('validated:valid', function(model, errors) {
      scope.launchConfigErrors.type_number = null;
      scope.launchConfigErrors.instance_type = null;
      self.render();
    });
   
    self.model.on('change', function() {
      self.model.set('type_show', true);
    });



    $(this.el).html(this.tpl);
     this.rView = rivets.bind(this.$el, scope);
     this.render();
    },

    render: function() {
      this.rView.sync();
    },

    isValid: function() {
      this.model.validate(_.pick(this.model.toJSON(),'type_number'));
      if (!this.model.isValid())
        return false;

      this.model.validate(_.pick(this.model.toJSON(),'instance_type'));
      if (!this.model.isValid())
        return false;

      return true;
    }

  });
});
