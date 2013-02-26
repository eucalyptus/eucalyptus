define([
], function() {
    var Volume = Backbone.Model.extend({
	id: '',
	size: 0,
	status: '',
	zone: ''
    });
    return Volume;
});
