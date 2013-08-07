define([
   'backbone',
   'rivets',
   'views/databox/databox',
], function(Backbone, rivets, DataBox) {
    return Backbone.View.extend({

        _do_init : function() {
          var self = this;
          $tmpl = $(this.template);

          this.$el.append($tmpl);
          $('#euca-main-container').children().remove();
          this.$el.appendTo($('#euca-main-container'));

          this.scope.databox = new DataBox(this.scope.collection);
          this.scope.items = this.scope.databox.getCollectionBySlice(0, 20);
          this.bind();
          this.render(); 

          // NOT SURE WHY THIS IS NOT WORKING - KYO 080613
          //this.scope.collection.on('change add remove reset', this.refresh_view());
        },
        close : function() {
          this.$el.empty();
        },
        bind: function() {
          this.rivetsView = rivets.bind(this.$el, this.scope);
        },
        render : function() {
          this.rivetsView.sync();
          return this;
        },
        get_element: function() {
          return this.$el;
        },
        refresh_view: function() {
          this.adjust_page(0, 20);
          this.render();
          console.log("-- Landing Page View Refresh --");
        },
        adjust_page: function(pageNumber, pageDisplayCount){
          this.scope.items = this.scope.databox.getCollectionBySlice(pageNumber, pageDisplayCount);
        },
        count_checked_items: function(){
          var count = 0;
          this.scope.items.each(function(model){
            if( model.get('clicked') === true ){
              count++;
            }
          });
          return count;
        },
        get_checked_items_for_datatables: function(sourceName, columnIdx){
          // TRY TO MATCH THE BEHAVIOR OF GETSELECTROW CALL  -- KYO 0080613
          // THIS NEEDS TO BE SIMPLIFIED.
          var selectedRows = [];
          var source = sourceName;
          // GET THE SOURCE OF THE LANDING PAGE
          console.log("Landing Page Source: " + source);
          // GET THE DATATABLE COLUMN MAP BASED ON THE SOURCE
          var columnMaps = this.scope.databox.columnMap;
          var thisColumnMap = [];
          $.each(columnMaps, function(index, map){
            if( map.name == source ){
              thisColumnMap = map.column;
            }
          });
          console.log("Column Map: " + JSON.stringify(thisColumnMap));
          // SET THE DEFAULT COLUMN ITEM TO "ID"
          var thisValue = "id";
          // SCAN ALL THE MODELS ON THIS LANDING PAGE
          this.scope.items.each(function(model){
            // CHECK IF THE MODEL IS CLICKED
            if( model.get('clicked') === true ){
             console.log("Clicked Row's ID: " + model.get('id'));
             // IF THIS getSelectedRows() FUNCTION IS INVOKED WITH A SPECIFIC COLUMN INDEX, 
	     if(columnIdx){
	       console.log("columnIdx: " + columnIdx);
               // SCAN THE MAP AND FIND THE MATCHING VALUE PER INDEX
               $.each(thisColumnMap, function(index, col){
                 if( col.id == columnIdx ){
                   thisValue = col.value;
                 };
               });
               selectedRows.push(model.toJSON()[thisValue]);
               console.log("Selected Row's Column Value: " + thisValue + "=" + model.toJSON()[thisValue]);
             }else{
               // NO SPECIFIC COLUMN INDEX CASE: SEND THE WHOLE MODEL ARRAY
	       selectedRows.push(model.toJSON());
	     }
            }	
          });  
          return selectedRows;
        },
   });
});

