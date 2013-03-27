define([
	'text!./template.html!strip',
    'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.$el.html(template);
            $('input', this.$el).iToggle({
                easing: 'easeOutExpo',
                type: 'radio',
                keepLabel: true,
                easing: 'easeInExpo',
                speed: 300,
                onClick: function(){
                    //Function here
                },
                onClickOn: function(){
                    //Function here
                },
                onClickOff: function(){
                    //Function here
                },
                onSlide: function(){
                    //Function here
                },
                onSlideOn: function(){
                    //Function here
                },
                onSlideOff: function(){
                    //Function here
                },
            });
			this.rview = rivets.bind(this.$el, this);
			this.render();
		},
		render : function() {
            this.rview.sync();
			return this;
		}
	});
});
