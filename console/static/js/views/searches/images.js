define(['app'], function(app) {
    var self = this;
    return function(images) {
        var self = this;

        var sortKeyList = function(list, keyName) {
            return _.chain(list)
            .pluck(keyName)
            .sort()
            .uniq()
            .value()
        }

        var siftKeyList = function(list, search) {
            console.log(list, search);
            return _.chain(list).filter(function(item) { 
                return new RegExp('.*' + search + '.*').test(item);
            })
            .map(function(item) {
                return item === '' ? { value: item, label: 'None' } : item
            })
            .value();
        }

        var jsonImages = images.toJSON();
        var keyLists = {
            name: sortKeyList(jsonImages, 'name'),
            platform: sortKeyList(jsonImages, 'platform'),
            description: sortKeyList(jsonImages, 'description'),
        }
        this.images = images;
        this.filtered = images.clone();
        this.query = '';
        this.search = function(search, facets) {
            console.log("SEARCH", arguments);
            var jfacets = facets.toJSON();
            var results = self.images.filter(function(model) {
                return _.every(jfacets, function(item) {
                    var test = new RegExp('.*' + item.value + '.*').test(model.get(item.category));
                    return test;
                });
            }).map(function(model) { return model.toJSON(); });
            console.log(results);
            self.filtered.reset(results);
        }
        this.facetMatches = function(callback) {
            callback([
                  { value: 'name', label: 'Name' },
                  { value: 'description', label: 'Description' },
                  { value: 'owner', label: 'Owner' },
                  { value: 'platform', label: 'Platform' },
                  { value: 'architecture', label: 'Architecture' },
                  { value: 'root_device', label: 'Root Device' },
            ]);
        }
        this.valueMatches = function(facet, searchTerm, callback) {
            switch (facet) {
            case 'architecture':
                callback([
                  { value: 'i386', label: 'i386 32-bit' },
                  { value: 'amd64', label: 'AMD64 64-bit' },
                ]);
                break;
            case 'platform':
            case 'name':
            case 'description':
                callback(siftKeyList(keyLists[facet], searchTerm));
                break;
            }
        }
    }
});
