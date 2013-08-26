define([
  'app',
  'views/searches/generic',
  'views/searches/tagsearch'
], function(app, Search, TagSearch) {
  
    var config = {
      field: 'volume',
      facets : ['all_text', 'status'],
      propertyForFacet : {
        attach_data : 'status'
      },
      localize : function(what) {
        switch (what) {
          case 'status' : 
            return app.msg('search_facet_volumes_attachment'); //'Attachment';
          case 'all_text':
            return app.msg('search_facet_alltext');
          case 'available':
            return app.msg('search_facet_volumes_available');
          case 'in-use':
            return app.msg('search_facet_volumes_in_use');
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
