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
