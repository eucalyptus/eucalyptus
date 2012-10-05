import tornado.web
import json
import server

from .botojsonencoder import BotoJsonEncoder

class SupportHandler(tornado.web.RequestHandler):
    # TODO: should an authorization be checked? 
    ##
    # This is the main entry point for support calls
    ##
    def post(self):
        action = self.get_argument("Action")
        if action == 'DownloadFile':
            data = self.get_argument("FileData")
            # TODO: should we sanitize file, trim, etc name?
            file_name = self.get_argument("FileName")
            self.set_header("Content-type", "application/x-pem-file;charset=ISO-8859-1")
            self.set_header("Content-Disposition", "attachment; filename=\"" + file_name + '.pem"')
            self.write(data)
        else:
            self.write("What are you doing here?")
