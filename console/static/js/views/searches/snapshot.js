define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(snapshots) {

    var config = {
      facets: ['all_text', 'progress']

      , match: {
        progress: function(search, item, add) {
          add('in-progress')
          add('completed')
          return true;
        }
      }
      , localize: {
        all_text: all_text_facet,
        'in-progress': in_progress_facet,
        'completed': completed_facet,
        'progress': progress_facet
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
