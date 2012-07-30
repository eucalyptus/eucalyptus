/*
 * Function: fnGetVisiableTrNodes
 * Purpose:  Get all of the visiable TR nodes (i.e. the ones which are on display)
 * Returns:  array or rows
 * Inputs:   object:oSettings - DataTables settings object
 */
$.fn.dataTableExt.oApi.fnGetVisiableTrNodes = function ( oSettings )
{
  return anDisplay = $('tbody tr', oSettings.nTable);
}

/*
 * Function: fnReloadAjax
 * Purpose:  Reloads ajax based datatable
 * Inputs:   object:oSettings - DataTables settings object
 */
$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings )
{
    this.oApi._fnProcessingDisplay( oSettings, true );
    var that = this;
    var iStart = oSettings._iDisplayStart;
    var aData = [];

    this.oApi._fnServerParams( oSettings, aData );

    oSettings.fnServerData( oSettings.sAjaxSource, aData, function(json) {
        /* Clear the old information from the table */
        that.oApi._fnClearTable( oSettings );

        /* Got the data - add it to the table */
        var aData =  (oSettings.sAjaxDataProp !== "") ?
            that.oApi._fnGetObjectDataFn( oSettings.sAjaxDataProp )( json ) : json;

        for ( var i=0 ; i<aData.length ; i++ )
        {
            that.oApi._fnAddData( oSettings, aData[i] );
        }

        oSettings.aiDisplay = oSettings.aiDisplayMaster.slice();
        that.fnDraw();

        that.oApi._fnProcessingDisplay( oSettings, false );
    }, oSettings );
}
