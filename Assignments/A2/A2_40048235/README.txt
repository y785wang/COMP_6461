In terminal, use 'javac *.java' to compile all .java files

Run server:
    java httpfs
    java httpfs -v
    java httpfs -v -d /files


Run client:

  GET:
    java httpc get localhost
    java httpc get localhost -v
    java httpc get localhost/file_1 -v
    java httpc get localhost/files/file_inside -v

  POST:
    java httpc post localhost
    java httpc post localhost -v

  Security:
    java httpc get localhost/../ -v
    java httpc get localhost/../file_outside -v
    java httpc get localhost/src -v

    java httpc post localhost/../ -v