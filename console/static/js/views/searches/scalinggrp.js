define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  return function(sgroups) {
    var config = {
      facets: ['all_text', 'launch_config_name', 'health_check_type', 'availability_zones'],
      localize: {
        launch_config_name: 'Launch Config',
        EC2 : app.msg('search_facet_scaliinggroup_ec2') //'Compute'
      }
    }
    return new Search(sgroups, new TagSearch(config, sgroups));
  }
});
