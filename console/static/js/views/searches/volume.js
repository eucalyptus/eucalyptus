define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  
    var config = {
      facets : ['all_text', 'attach_data'],
      propertyForFacet : {
        attach_data : 'status'
      },
      localize : function(what) {
        switch (what) {
          case 'attach_data' : 
            return attach_data_facet;
          case 'all_text' :
            return all_text_facet;
        }
        // 'Attached' is actually an object
        // of some sort
        if (typeof what === 'object') {
          return what.toString()
        }
      }
    };
  
    return function(volumes) {
      return new Search(volumes, new TagSearch(config, volumes));
    }
});
