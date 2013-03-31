define([
	'dataholder',
  'text!./type.html!strip',
  'rivets',
	], function( dataholder, template, rivets ) {
  return Backbone.View.extend({
    title: 'Type',

    initialize : function() {

      var self = this;
      this.model.set('tags', new Backbone.Collection());
      var scope = {

        setField: function(e, el) {
          var target = e.target;
          self.model.set('type_show', 'true');
          switch(target.id) {
            case 'launch-instance-names':
              var names = target.value.split(',');
              self.model.set('type_names', names);
              self.model.set('type_hasNames', 'true');
              break;
            case 'launch-instance-type-num-instance':
              self.model.set('type_number', target.value);
              break;
            case 'launch-instance-type-size':
              self.model.set('instance_type', target.value);
              break;
            case 'launch-instance-type-az':
              self.model.set('type_zone', target.value);
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
                console.log("TAGS", arguments);
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

            }
        }
      };


    $(this.el).html(template)
     this.rView = rivets.bind(this.$el, scope);
     this.render();
     console.log("TYPEVIEW", this.model);
    },


    render: function() {
      this.rView.sync();
      
      
    }
  });
});
