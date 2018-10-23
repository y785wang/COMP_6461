In terminal, use 'javac *.java' to compile all .java files

Run server:

Run client:
    java httpc get localhost
    java httpc get localhost -v
    java httpc get localhost/file_1 -v
    java httpc get localhost/src -v




Security:
  1) Server ignore any path followed by the "localhost" key word if request is GET