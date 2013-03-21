console.log('EANTEST:start');
define([
	'dataholder',
	'text!./template.html!strip',
	], function( dataholder, template ) {
	return Backbone.View.extend({
		events : {
		},
		initialize : function() {
			var view = this;

			this.collection = dataholder.scalingGroups;
			this.collection.on('reset', function() { view.render() });
			this.collection.on('change', function() { view.render() });
			this.collection.fetch();	
		},
		render : function() {
			this.$el.html(_.template(template)({ groups: this.collection.toJSON() }));
			return this;
		}
	});
});

function AutoScaling ($scope, $http) {
    http.post('/autoscaling?Action=DescribeAutoScalingGroups').success(function(data) {
        $scope.results = data.results;
    });
}

angular.bootstrap($('ng-scope'), ['AutoScaling']);
