Command line test cases

Help
    java httpc
    java httpc help
    java httpc help get
    java httpc help post

Get
    java httpc get 'http://httpbin.org/get'
    java httpc get 'http://httpbin.org/get?course=networking&assignment=1'
    java httpc get 'http://httpbin.org/get?course=networking&assignment=1' -v
    java httpc get 'http://httpbin.org/get?course=networking&assignment=1' -v -h 123:123
    
Post
    java httpc post 'http://httpbin.org/post'
    java httpc post 'http://httpbin.org/post' -v
    java httpc post 'http://httpbin.org/post' -v -h 123:123
    java httpc post 'http://httpbin.org/post' -v -h 123:123 -d 456
    java httpc post 'http://httpbin.org/post' -v -h 123:123 -f input
    java httpc post 'http://httpbin.org/post' -v -h 123:123 -d 456 -f input
    java httpc post 'http://httpbin.org/post' -v -h 123:123 -f input -o output

Redirect code302:
    java httpc get  http://httpbin.org/redirect/n -v
    java httpc get  http://httpbin.org/absolute-redirect/n -v
    java httpc get  http://httpbin.org/redirect-to?url=http://httpbin.org/get -v

    java httpc post http://httpbin.org/redirect-to?url=http://httpbin.org/post -v

Note:
    1) n is any positive integer
    2) Set variable 'seeRedirectDetail = true' at the top of the sendRequest class (line 140 of http.java) to see the redirect detail