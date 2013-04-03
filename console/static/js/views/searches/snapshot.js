define([
  'views/searches/generic',
], function(Search) {
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
        'in-progress': 'In-Progress',
        completed: 'Completed'
      }
      , search: {
        progress: function(search, facetSearch, item, itemsFacetValue, hit) {
          console.log("Facet search " + facetSearch + " itemsValue " + itemsFacetValue);
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

    return new Search(snapshots, config);
  }
});
