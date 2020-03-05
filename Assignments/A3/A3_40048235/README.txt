In terminal, use 'javac *.java' to compile all .java files

Run server:
    java httpfs
    java httpfs -v
    java httpfs -v -p 4225
    java httpfs -v -d /files

Run client:
  GET:
    java httpc get localhost -v
    java httpc get localhost/textFile.txt -v
    java httpc get localhost/files/file_inside.txt -v

  POST:
    java httpc post localhost/file_2.txt -v -f input
    java httpc post localhost/file_2.txt -v -d inlineData
    java httpc post localhost/dir_1/dir_2/file_3.txt -f input 

  Security:
    java httpc get localhost/../file_outside -v
    java httpc post localhost/../ -v

  Multi-Request Support:
    java httpc post localhost/file_2.txt -v -f input
    java httpc post localhost/file_2.txt -v -f input

    java httpc get localhost/file_2.txt -v
    java httpc post localhost/file_2.txt -v -f input

    java httpc get localhost/textFile.txt -v
    java httpc get localhost/textFile.txt -v

  Content-Type & Content-Disposition Support:
    java httpc get localhost/imageFile.jpg -v
    http://localhost:8080/imageFile.jpg
