# HTTP Client
HTTP client with Java similar to Insomnia and Postman
## Features
- Get, Post, Delete, Put methods
- Adding headers
- Form data body
- Receiving response status, headers and body
- Previewing images
- Save requests
- Review previous requests
- Save response body

## How it works
HTTP requests can be sent through console or GUI
Console commands:
```bash
Usage: postpost <url> [options...]
-M, --method <method>           Set request method
-h, --headers <header/@file>	pass custom header(s) to server
-d, --data <name=content>      specify multipart form data
-O, --output <file>          		   write to file instead of stdout
-S, --save                  				   save request with its options
-i                           					   print response headers
-h, --help                  				   print help
If you want to upload a file use -d "file=path"; 'path' is your file path.
```