define([
	'dataholder',
	//'text!views/templates/template.html!strip',
	], function( dataholder, template ) {
	return Backbone.View.extend({
		initialize : function() {
			var view = this;

			this.collection = dataholder[this.name];
			this.collection.on('reset', function() { view.render() });
			this.collection.on('change', function() { view.render() });
			this.collection.on('sort', function() { view.render() });
			this.collection.fetch();
		},
        name: "base",
        template: "template",
		render : function() {
            var tmp = {};
            tmp[this.name] = this.collection.toJSON();
			this.$el.html(_.template(this.template)(_.extend({}, tmp)));
            this.delegateEvents();
            return this;
		},
		
        sortKeys:  [],

        events: function() {
            var tmp = {};
            var view = this;
            $("#sortheader th").each( function(index) { 
                tmp["click #sortheader th:eq(" + index + ")"] =  function() { 
                                view.collection.columnSort(view.sortKeys[index]);
                };
            });
            tmp["click #" + this.name + "-check-all"] = function(e) {
                e.stopPropagation();
                if ( $("#" + this.name + "-check-all").prop('checked')) {
                    $("input[type='checkbox']").prop('checked', true);
                } else {
                    $("input[type='checkbox']").prop('checked', false);
                }
            };
            return _.extend({}, tmp);
        },
        
	});
});
