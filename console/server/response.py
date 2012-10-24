# a wrapper object for json responses to the browser

class Response(object):
    results = None

    def __init__(self, results):
        self.results = results

class ClcError(object):
    status = None
    summary = None
    message = None

    def __init__(self, status, summary, message):
        self.status = status
        self.summary = summary
        # trim up message so we don't overload the browser, trim starting at "Caused by"
        idx = -1;
        if message:
            idx = message.find("Caused by")
        self.message = (message if (idx == -1) else message[:idx-1])
