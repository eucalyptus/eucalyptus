eucalyptus-ui
=============

The Eucalyptus User App allows cloud users to do in a web browser what otherwise is done over the Eucalyptus API or command-line interface. 
The design goal is to build a tool that is elegant and easy to use. 
The tool provides graphical access to the underlying API and CLI, but does not provide additional functionality of its own

This project consists of server-side implementation using Python Tornado and the rich web-client based on JQuery.
The server exposes various REST interfaces, which are roughly equivalent to EC2/S3 apis. 

You'll need to download and install tornado based on their instructions: http://www.tornadoweb.org/
To configure this to work with your cloud/acct, edit server/console.ini and change the endpoint. You can also test the client functionality using mock data by setting usemock=True.
This user console will not work with anything but the latest Eucalyptus code which will become version 3.2. That is due to an authentication path that was added to support using the same login credentials for both the admin UI and this user console.

To run the server, run "launcher.sh" and point your browser to 'http://localhost:8888'
This has been tested with Python 2.6 and 2.7. If it breaks, please specify the version of python and stack trace and let us know!

Disclaimer: This code is a work in progress and we'd appreciate any feedback. There are a lot of things that work, but plenty that don't. As we get towards a release later this year, there should be less things that are broken. You can get help on freenode IRC #eucalyptus-ui

