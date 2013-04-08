define([
	'text!./template.html!strip',
    'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.$el.html(template);
            console.log('iToggle');
			this.rview = rivets.bind(this.$el, {model: this.model});
            $('input', this.$el).iToggle({
                click: function(){
                    console.log("click");
                },
                onClickOn: function(){
                    console.log("activated");
                },
                onClickOff: function(){
                    console.log("deactivated");
                },
                onSlideOn: function(){
                    //Function here
                    console.log("activated");
                },
                onSlideOff: function(){
                    //Function here
                    console.log("deactivated");
                },
            });
			this.render();
		},
		render : function() {
            this.rview.sync();
			return this;
		}
	});
});
