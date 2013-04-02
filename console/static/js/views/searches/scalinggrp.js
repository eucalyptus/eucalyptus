define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    var config = {
      facets: ['launch_config_name', 'health_check_type', 'availability_zones'],
      localize: {
        launch_config_name: 'Launch Config'
      }
    }
    return new Search(images, config);
  }
});
