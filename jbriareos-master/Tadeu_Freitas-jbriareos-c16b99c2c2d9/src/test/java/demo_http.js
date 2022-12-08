let http = require('http');

//create a server object:
http.createServer(function (req, res) {
    if (req.method === 'POST' && req.url === '/echo') {
        let body = [];
        req.on('data', (chunk) => {
            body.push(chunk);
        }).on('end', () => {
            body = Buffer.concat(body).toString();
            res.write(body + '\n');
            res.end();
        });
    } else if (req.method === 'GET' && req.url === '/hello-world') {
        res.write('Hello World!\n'); //write a response to the client
        res.end(); //end the response
    }
    else {
        res.write("Request not found\n");
        res.end();
    }
}).listen(4000); //the server object listens on port 4000