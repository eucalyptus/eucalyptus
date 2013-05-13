define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  
    var config = {
      facets : ['all_text', 'attach_data'],
      propertyForFacet : {
        attach_data : 'status'
      },
      localize : function(what) {
        switch (what) {
          case 'attach_data' : 
            return app.msg('search_facet_volumes_attachment'); //'Attachment';
          case 'all_text':
            return app.msg('search_facet_alltext');
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
