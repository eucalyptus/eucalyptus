eucalyptus management console
=============

The Eucalyptus Management Console allows cloud users to do in a web browser what otherwise is done over the Eucalyptus API or command-line interface. 
The design goal is to build a tool that is elegant and easy to use. 
The tool provides graphical access to the underlying API and CLI, but does not provide additional functionality of its own

The management console consists of a web server implementation using Python Tornado and a rich Ajax client based on JQuery.
The server exposes various REST interfaces, which are roughly equivalent to EC2/S3 apis. 

PREREQUISITE

The management console works with Eucalyptus version 3.2 and later (earlier versions do not work). 
This is due to the new authentication mechanism added in 3.2 to support the management console.
If you run the console in mock mode (see below), the Eucalyptus 3.2 is not required (Eucalyptus cloud is not required).

The management console was tested with the following browsers:
  - Firefox 15 (Recommended)
  - Google Chrome 22 (Recommended)
  - Safari 6
  - Internet Explorer 9 

INSTALLATION

You will need to download and install tornado based on their instructions: http://www.tornadoweb.org/. For RHEL or Centos 5/6, you can install it from the package repository:
  - (optional) configure the EPEL repo for your version: http://fedoraproject.org/wiki/EPEL
  - yum install python-tornado

Likewise, for UBUNTU, use:
  - apt-get install python-tornado

You also need to install python-boto and m2crypto, which are required to communicate with Eucalyptus Cloud Controller.

For RHEL or Centos 5/6, use:
  - yum install m2crypto python-boto

For UBUNTU, use:
  - apt-get install python-m2crypto python-boto


CONFIGURATION

To configure the console to work with your Eucalyptus cloud (version 3.2 and later), edit server/console.ini and change the endpoint. 
  - clchost: "HOSTNAME OR IP OF YOUR EUCALYPTUS CLC". 
Make sure that the 'clchost' is reachable from the host that runs console's web server.

You can also test the console's functionality using the supplied mock data. To enable the mock mode:
  - usemock: True

Other notable configuration options include:
  - uiport: the port number to listen for incoming HTTP request
  - sslcert/sslkey: the path to the SSL certificate to be used for HTTPS
  - session.idle.timeout: the timeout that expires users' session after idling period
  - session.abs.timeout: the absolute timeout afterwhich users' should log-in the console again  
  - language: the language code to be used for internationalization (see below)
  - support.url: the url (or mailto: admin ) of your Cloud support page

HOW TO RUN

To launch the console's web server, simply run "launcher.sh". 

USING THE CONSOLE

  - Open your web brower and point to 'http://localhost:8888' (replace localhost and 8888, with the address of console's web server and the uiport).
  - When prompted the log-in screen, use your Eucalyptus credential to log-in. For example,
    - Account name : fred
    - Username     : admin
    - Password     : foobar
    You may need to visit Eucalyptus admin console to setup your account first. The default address of the admin console is 'https:{clchost}:8443'.
  - Make sure your account/username has the corresponding access key and secret key. You can create one using the admin console.
  - If you are running the console in mock mode, you can put any string in the log-in field.

INTERNATIONALIZATION

The management console is designed to support many different languages. Currently Russian and Korean are supported, in addition to English by default.
We expect the more languages would be added by the Eucalyptus community. 
To add a language, follow these steps:
  - Go to 'static/custom/' and copy 'Messages.properties' file to a new file. The new file name should starts with 'Messages', followed by the language and country code, according to the convention defined in ISO 639 and ISO 3166. For example, for main-land Chinese use 'Messages_zh_CN.properties'. For Japanese, one can use 'Messages_ja_JP.properties'
  - Update 'language' option in server/console.ini to the new language. For example, 'language = zh_CN' for main-land Chinese.
  - After updating 'Messages_xx_YY.properties' during the translation, you can simply refresh your browser to see the change.
  - The console includes HTML help files stored under 'static/help'. You may want to translate the static HTML files as well to your language (If no such translation found, the console falls back to the English help). You should create a subdirectory named after the language&country code, under 'static/help', and store your translated help files.

DISCLAIMER

The Eucalyptus management console is in-progress and hasn't been released yet. 
It is our plan to make it feature complete with Eucalyptus 3.2 release. 
For answers to some of your questions, try here: https://github.com/eucalyptus/eucalyptus-ui/wiki/FAQ

