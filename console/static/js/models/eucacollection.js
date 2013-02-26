define([
], function() {
    var EucaCollection = Backbone.Collection.extend({
	sync: function(method, model, options) {
	    if (method == 'read') {
		$.ajax({
		    type:"POST",
		    url: this.url,
		    data:"_xsrf="+$.cookie('_xsrf'),
		    dataType:"json",
		    async:"false",
		    success: function(data, textStatus, jqXHR){
			if (data.results) {
			    options.success && options.success(model, data.results, options);
			} else {
			    ;//TODO: how to notify errors?
			}
		    },
		    error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
			console.log('regrettably, its an error');
		    }
		});
	    }
	}
    });
    return EucaCollection;
});
