/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

/*
 * Function: fnGetVisiableTrNodes
 * Purpose:  Get all of the visiable TR nodes (i.e. the ones which are on display)
 * Returns:  array or rows
 * Inputs:   object:oSettings - DataTables settings object
 */
$.fn.dataTableExt.oApi.fnGetVisibleTrNodes = function ( oSettings )
{
  return anDisplay = $('tbody tr', oSettings.nTable);
}

/*
  http://datatables.net/plug-ins/api
  Author:	Allan Jardine
*/
$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource, fnCallback, bStandingRedraw )
{
  if ( typeof sNewSource != 'undefined' && sNewSource != null ) {
        oSettings.sAjaxSource = sNewSource;
    }
 
    if (!oSettings){
      return;
    }   
    // Server-side processing should just call fnDraw
    if ( oSettings.oFeatures.bServerSide ) {
        this.fnDraw();
        return;
    }
 
    this.oApi._fnProcessingDisplay( oSettings, true );
    var that = this;
    var iStart = oSettings._iDisplayStart;
    var aData = [];
  
    this.oApi._fnServerParams( oSettings, aData );
    oSettings.fnServerData.call( oSettings.oInstance, oSettings.sAjaxSource, aData, function(json) {
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
          
        if ( typeof bStandingRedraw != 'undefined' && bStandingRedraw === true )
        {
            oSettings._iDisplayStart = iStart;
            oSettings._bPreserveDisplayStart = true;
            that.fnDraw(true); 
        }
        else
        {
            that.fnDraw(true);
        }
          
        that.oApi._fnProcessingDisplay( oSettings, false );
          
        /* Callback user function - for event handlers etc */
        if ( typeof fnCallback == 'function' && fnCallback != null )
        {
            fnCallback( oSettings );
        }
    }, oSettings );
}

/*
  http://datatables.net/plug-ins/api
  Author:	Allan Jardine
*/
$.fn.dataTableExt.oApi.fnGetTds  = function ( oSettings, mTr )
{
    var anTds = [];
    var anVisibleTds = [];
    var iCorrector = 0;
    var nTd, iColumn, iColumns;
      
    /* Take either a TR node or aoData index as the mTr property */
    var iRow = (typeof mTr == 'object') ?
        oSettings.oApi._fnNodeToDataIndex(oSettings, mTr) : mTr;
    if (!oSettings.aoData[iRow])
      return [];

    var nTr = oSettings.aoData[iRow].nTr;
      
    /* Get an array of the visible TD elements */
    for ( iColumn=0, iColumns=nTr.childNodes.length ; iColumn<iColumns ; iColumn++ )
    {
        nTd = nTr.childNodes[iColumn];
        if ( nTd.nodeName.toUpperCase() == "TD" )
        {
            anVisibleTds.push( nTd );
        }
    }
      
    /* Construct and array of the combined elements */
    for ( iColumn=0, iColumns=oSettings.aoColumns.length ; iColumn<iColumns ; iColumn++ )
    {
        if ( oSettings.aoColumns[iColumn].bVisible )
        {
            anTds.push( anVisibleTds[iColumn-iCorrector] );
        }
        else
        {
            anTds.push( oSettings.aoData[iRow]._anHidden[iColumn] );
            iCorrector++;
        }
    }
      
    return anTds;
};
