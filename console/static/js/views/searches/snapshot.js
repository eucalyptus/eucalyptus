define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  return function(snapshots) {

    var config = {
      field: 'snapshot',
      defaultSearch: 'owner: me',
      facets: ['all_text', 'progress']

      , match: {
        progress: function(search, item, add) {
          add('in-progress')
          add('completed')
          return true;
        }
      }
      , localize: {
        'in-progress': app.msg('search_facet_snapshots_inprogress'), //'In-Progress',
        completed: app.msg('search_facet_snapshots_completed'), //'Completed'
        progress: app.msg('search_facet_snapshots_progress'),
        all_text: app.msg('search_facet_alltext')
      }
      , search: {
        progress: function(search, facetSearch, item, itemsFacetValue, hit) {
          switch (facetSearch) {
            case 'completed' :
              if ('100%' === itemsFacetValue) {
                hit();
              }
              break;
            case 'in-progress' :
              if ('100%' !== itemsFacetValue) {
                hit();
              }
              break;
          }
        }
      }
    }

    return new Search(snapshots, new TagSearch(config, snapshots));
  }
});
