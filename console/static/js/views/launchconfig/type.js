define([
	'dataholder',
  'text!./type.html!strip',
  'rivets',
	], function( dataholder, template, rivets ) {
	return Backbone.View.extend({
    title: 'Type',

		initialize : function() {
        
     var scope = {
       view: this,

       setField: function(e, el) {
           console.log("SETFIELD:", e);
         var target = e.target;
         this.view.model.set('type_show', 'true');
         switch(target.id) {
           case 'launch-instance-names':
             var names = target.value.split(',');
             this.view.model.set('type_names', names);
             this.view.model.set('type_hasNames', 'true');
             break;
           case 'launch-instance-type-num-instance':
             this.view.model.set('type_number', target.value);
             break;
           case 'launch-instance-type-size':
             this.view.model.set('type_size', target.value);
             break;
           case 'launch-instance-type-az':
             this.view.model.set('type_zone', target.value);
             break;

           default:
         }
       },

       iconClass: function() {
         return this.view.model.get('image_iconclass'); 
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
