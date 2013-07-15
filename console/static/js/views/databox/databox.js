
define(['app', 'backbone'], function(app, Backbone) {
    return function(records){

      var databox = {

        columnMap: [
                  {name:'instance', column:[{id:'2', value:'display_id'}, {id:'12', value:'state'}, {id:'4', value:'image_id'}, {id:'5', value:'placement'}, {id:'6', value:'public_dns_name'}, {id:'7', value:'private_dns_name'}, {id:'8', value:'key_name'}, {id:'9', value:'group_name'}, {id:'13', value:'launch_time'}]},
                  {name:'image', column:[{id:'1', value:'display_id'}, {id:'2', value:'name'}, {id:'3', value:'id'}, {id:'4', value:'architecture'}, {id:'5', value:'description'}, {id:'6', value:'root_device_type'}]},
                  {name:'volume', column:[{id:'1', value:'display_id'}, {id:'8', value:'status'}, {id:'3', value:'size'}, {id:'4', value:'display_instance_id'}, {id:'5', value:'display_snapshot_id'}, {id:'6', value:'zone'}, {id:'9', value:'create_time'}]},
                  {name:'snapshot', column:[{id:'1', value:'display_id'}, {id:'7', value:'status'}, {id:'3', value:'volume_size'}, {id:'4', value:'display_volume_id'}, {id:'9', value:'description'}, {id:'8', value:'start_time'}]},
                  {name:'eip', column:[{id:'4', value:'public_ip'}, {id:'2', value:'instance_id'}]},
                  {name:'keypair', column:[{id:'3', value:'name'}]},
                  {name:'sgroup', column: [{id:'6', value:'description'}, {id:'7', value:'name'}]},
        ],

        sortData: function(){
          records.sort();
        },

        sortDataReverse: function(){
          records = new Backbone.Collection(records.sort().toJSON().reverse());
        },

        setComparator: function(comparator){
          records.comparator = comparator;
        },

	sortDataForDataTable: function(page, column_id, order){
	  var self = this;
	  var sortValue = "";

	  console.log("Sort Page: " + page);
	  $.each(this.columnMap, function(idx, map){
            if(map.name === page){
              $.each(map.column, function(index, col){
	        if(col.id == column_id){
		  sortValue = col.value;       
		}
	      });
            }
          });
 
	  console.log("SortValue: " + sortValue);
	  this.setComparator(function(item) {
            return item.get(sortValue);
          });

	  console.log("Sorting Order: " + order);
	  if( order === "asc" ){
	    this.sortData();
	  }else{
	    this.sortDataReverse();
	  }
	},

        getArray: function(){
          return records.toJSON();
        },

        getArrayBySlice: function(start, length){
          return records.toJSON().slice(start, length);
        },

	getTotalLength: function(){
	  return records.toJSON().length;
	},

      };

      return databox;

    };
});

