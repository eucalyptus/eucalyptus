define([
    'views/searches/generic',
    'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(keypairs) {
    var config = {
      facets : ['all_text'],
      localize: {
        all_text: all_text_facet
      }
    }
    return new Search(keypairs, new TagSearch(config, keypairs));
  }
});
