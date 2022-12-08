package engine_modules_helpers;

import java.util.Hashtable;

public class HttpObject {
    public String url;
    String method;
    Hashtable<String, String> headers;
    int statusCode;
    String objType;

    public HttpObject(String url, String method, Hashtable<String, String> headers, int statusCode) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.statusCode = statusCode;
        this.objType = "Response";
    }

    public HttpObject(String url, String method, Hashtable<String, String> headers) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.objType = "Request";
    }
}
