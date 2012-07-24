eucalyptus-ui
=============

The Eucalyptus User App allows cloud users to do in a web browser what otherwise is done over the Eucalyptus API or command-line interface. 
The design goal is to build a tool that is elegant and easy to use. 
The tool provides graphical access to the underlying API and CLI, but does not provide additional functionality of its own

This prototype consists of server-side implementation using Python Tornado and the rich web-client based on JQuery.
The server exposes various REST interfaces, which are roughly equivalent to EC2/S3 apis. 

You'll need to download and install tornado based on their instructions: http://www.tornadoweb.org/
To configure this to work with your cloud/acct, edit server/eui.ini and change the endpoint. Edit server/__init__.py to change the access_id and secret_key in the post() method of the LoginHandler class. You can also test the client functionality using mock data by setting usemock=True.

To run the server, run "launcher.sh" and point your browser to 'http://localhost:8888'
This has been tested with Python 2.7. If it breaks, please specify the version of python and stack trace and let us know!

Disclaimer: This is not configured for production! This is strictly a prototype to test out various aspects of the design. Some parts of the code maybe copied in whole or in part to the final implementation. 
