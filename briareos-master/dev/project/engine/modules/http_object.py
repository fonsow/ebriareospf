class HttpObject:
    def __init__(self, method, status_code, url, headers, body):
        self.method = method
        self.url = url
        self.headers = headers
        self.body = body

        if status_code is not None:
            self.statusCode = status_code
            self.type = "Response"
        else:
            self.type = "Request"

    def __str__(self):
        return "%s, %s, %s, %s" % (self.method, self.url, self.headers, self.body)
