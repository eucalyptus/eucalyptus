define([
	'dataholder',
	'text!views/jp_vols/template.html!strip',
	], function( dataholder, template ) {
	return Backbone.View.extend({
		initialize : function() {
			var view = this;

			this.collection = dataholder.volumes;
			this.collection.on('reset', function() { view.render() });
			this.collection.on('change', function() { view.render() });
			this.collection.on('sort', function() { view.render() });
			this.collection.fetch();
		},
		render : function() {
			this.$el.html(_.template(template)({ volumes: this.collection.toJSON() }));
            this.delegateEvents();
            return this;
		},
		
        sortKeys:  ['', 'id', 'status', 'size', 'instance_id', 'snapshot_id', 'zone', 'create_time'],

        events: function() {
            var tmp = {};
            var view = this;
            $("#sortheader th").each( function(index) { 
                tmp["click #sortheader th:eq(" + index + ")"] =  function() { 
                                view.collection.columnSort(view.sortKeys[index]);
                };
            });
            tmp["click #volumes-check-all"] = function(e) {
                e.stopPropagation();
                if ( $("#volumes-check-all").prop('checked')) {
                    $("input[type='checkbox']").prop('checked', true);
                } else {
                    $("input[type='checkbox']").prop('checked', false);
                }
            };
            return _.extend({}, tmp);
        },
        
	});
});
