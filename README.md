eucalyptus-ui
=============

Eucalyptus User App
The Eucalyptus User App allows cloud users to do in a web browser what otherwise is done over the Eucalyptus API or command-line interface. 
The design goal is to build a tool that is elegant and easy to use. 
The tool provides graphical access to the underlying API and CLI, but does not provide additional functionality of its own

This prototype runs inside tornado. You'll need to download and install tornado based on their instructions: http://www.tornadoweb.org/

The two main files are eui-server.py which is the server side and the eui.html which is the client side. The rest are support files, mostly on the client.

To configure this to work with your cloud/acct, edit server/eui.ini and change the endpoint. Edit server/__init__.py to change the access_id and secret_key in the post() method of the LoginHandler class.

To run the server, run "launcher.sh" and connect to http://localhost:8888/static/eui.html

This prototype shows tabs for the basic resource types. Those are loaded via REST calls to the server, but all load when the UI first loads, not as tabs are clicked. More work needs to be done for on-demand data loading and autmatic refresh.

This has been tested with Python 2.7. If it breaks, please specify the version of python and stack trace and let us know!

Disclaimer: This is not configured for production! This is strictly a prototype to test out various aspects of the design. Some parts of the code maybe copied in whole or in part to the final implentation. 
