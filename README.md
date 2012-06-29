eucalyptus-ui
=============

Eucalyptus User App

This prototype runs inside tornado. You'll need to download and install tornado based on their instructions: http://www.tornadoweb.org/

The two main files are eui-server.py which is the server side and the eui.html which is the client side. The rest are support files, mostly on the client.

To configure this to work with your cloud/acct, edit eui-server.py and change the endpoint, access_id and secret_key in the get() method of the EC2Handler class.

To run the server, run "python eui-server.py" and connect to http://localhost:8888/static/eui.html

This prototype shows tabs for the basic resource types. Those are loaded via REST calls to the server, but all load when the UI first loads, not as tabs are clicked. More work needs to be done for on-demand data loading and autmatic refresh.

This has been tested with Python 2.7. If it breaks, please specify the version of python and stack trace and let us know!

Disclaimer: This is not configured for production! This is strictly a prototype to test out various aspects of the design. Some parts of the code maybe copied in whole or in part to the final implentation. 
