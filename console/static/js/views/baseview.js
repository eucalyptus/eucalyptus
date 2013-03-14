define([
	'dataholder',
	'text!views/templates/base.html!strip',
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
        header: "Base View",
        tableHeader: [],
        filterOptions: {},
        searchText: "Search",
        buttonLabel: "Create new",
        itemsFound: function() { return this.collection.size() + " items found."; },
        template: template,
		render : function() {
            var tmp = {};
            tmp['collection'] = this.collection.toJSON();
            tmp['header'] = this.header;
            tmp['tableHeader'] = this.tableHeader;
            tmp['filterOptions'] = this.filterOptions;
            tmp['searchText'] = this.searchText;
            tmp['buttonLabel'] = this.buttonLabel;
            tmp['itemsFound'] = this.itemsFound();
            console.log(tmp);
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
