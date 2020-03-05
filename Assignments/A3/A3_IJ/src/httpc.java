import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;


/**
 * A simple http client
 */
public class httpc {

    private final String USER_AGENT = "Concordia-HTTP/1.0";
    private int PORT = 80;
    private int locahostPort = 8007;


    /**
     * Determine if the given url is start with "http://"
     * @param url is a string that represents an url
     * @return true if the given url string satisfy the
     *      codition, false otherwise
     */
    private static boolean checkURLFormat(String url) {
        int length = url.length();
        return length > 8 && url.substring(0, 9).equals("localhost") || length > 6 && url.substring(0, 7).equals("http://");
    }

    
    /**
     * Main function, deal with the command line input,
     * and basic help information about how should a user
     * issues the command to achieve the get and post
     * requests
     * @param args is a string array of command line arguments
     */
    public static void main(String[] args) {

        if (0 == args.length || (1 == args.length && args[0].equals("help"))) {
            String help = "\n";
            help += "httpc is a curl-like application but supports HTTP protocol only.\n";
            help += "Usage: \n    httpc command [arguments]\nThe commands are:\n";
            help += "    get     executes a HTTP GET request and prints the response.\n";
            help += "    post    executes a HTTP POST request and prints the response.\n";
            help += "    help    prints this screen.\n\n";
            help += "Use \"httpc help [command]\" for more information about a command.\n";
            System.out.println(help);
        } else if (1 == args.length) {
            if (args[0].equals("get") || args[0].equals("post")) {
                System.out.println(args[0] + ": a following URL is needed");
            } else {
                System.out.println(args[0] + ": command nor found");
            }
        } else {
            switch (args[0]) {
                case "help":
                    switch (args[1]) {
                        case "get":
                            String get = "\n";
                            get += "usage: httpc get [-v] [-h key:value] URL\n\n";
                            get += "Get executes a HTTP GET request for a given URL.\n\n";
                            get += "    -v           Prints the detail of the response such as protocol, status, and headers.\n";
                            get += "    -h key:value Associates headers to HTTP Request with the format 'key:value'.\n";
                            System.out.println(get);
                            break;
                        case "post":
                            String post = "\n";
                            post += "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n\n";
                            post += "Post executes a HTTP POST request for a given URL with inline data or from file.\n\n";
                            post += "    -v             Prints the detail of the response such as protocol, status, and headers.\n";
                            post += "    -h key:value   Associates headers to HTTP Request with the format 'key:value'.\n";
                            post += "    -d string          Associates an inline data to the body HTTP POST request.\n";
                            post += "    -f file            Associates the content of a file to the body HTTP POST request.\n\n";
                            post += "Either [-d] or [-f] can be used but not both.\n";
                            System.out.println(post);
                            break;
                        default:
                            System.out.println(args[0] + ": command nor found");
                            break;
                    }
                    break;
                case "get": case "post": {
                    httpc http = new httpc();
                    http.sendRequest(args);
                    break;
                }
                default:
                    System.out.println(args[0] + ": command nor found");
                    break;
            }
        }
    }

    
    /**
     * Deal with either get or post request with options,
     * get the result from the server, then output the
     * response
     * @param commandLine is a string array of command line arguments
     * @throws Exception
     */
    private void sendRequest(String[] commandLine) {

        // for test purpose
        boolean seeRedirectDetail = true;
        
        String responseHeader = "";
        String responseBody = "";
        BufferedWriter bw = null;
        BufferedReader br = null;
        BufferedWriter bwF = null;
        BufferedReader brF = null;
        boolean showDetail = false;
        
        try {
            String method = commandLine[0].equals("get") ? "GET" : "POST";
            String host = "";
            String path = "/";
            String url = "";
            ArrayList<String> headers = new ArrayList<String>();
            String data = "";
            String httpVersion = "HTTP/1.0";
            String crlf = "\r\n";
            String boundry = "This_is_Yishi_Wang's_boundary_:D";
            String output = "";
            int numOfToken = commandLine.length;
            CharSequence code302 = "302";
            boolean validURL = false;
            boolean _dOptionDone = false;
            boolean _fOptionDone = false;
            boolean writeInFile = false;
            boolean statusLineChecked = false;
            boolean statusCode302 = false;
            boolean redirect = false;
            
            // Parsing command line arguments
            for (int i = 1; i < numOfToken; ++i) {
                String option = commandLine[i];
                switch (option) {
                    // -v Prints the detail of the response such as protocol, status, and headers
                    case "-v":
                        showDetail = true;
                        break;
                    // -h key:value Associates headers to HTTP Request with the format
                    case "-h":
                        if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                            headers.add(commandLine[i]);
                        } else {
                            System.out.println("-h: missing key:value pair(s)");
                            return;
                        }
                        break;
                    // -d string Associates an inline data to the body HTTP POST request
                    case "-d":
                        if (method.equals("GET")) {
                            System.out.println("-d: invalid command for GET request");
                            return;
                        } else {
                            if (_dOptionDone) {
                                System.out.println("-d: duplicate option");
                                return;
                            } else if (_fOptionDone) {
                                System.out.println("-d: -d and -f can not be used together");
                                return;
                            } else if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                                data = commandLine[i] + crlf;
                                _dOptionDone = true;
                            } else {
                                System.out.println("-d: missing an inline data");
                                return;
                            }
                        }
                        break;
                    // -f file Associates the content of a file to the body HTTP POST request
                    case "-f":
                        if (method.equals("GET")) {
                            System.out.println("-f: invalid command for GET request");
                            return;
                        } else {
                            if (_fOptionDone) {
                                System.out.println("-f: duplicate option");
                                return;
                            } else if (_dOptionDone) {
                                System.out.println("-f: -d and -f can not be used together");
                                return;
                            } else if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                                String iFilename = commandLine[i];
                                File iFile = new File(iFilename);
                                String fileContent = "";
                                if (iFile.exists()) {
                                    brF = new BufferedReader(new FileReader(iFile));
                                    String line;
                                    while ((line = brF.readLine()) != null) {
                                        fileContent += line + crlf;
                                    }
                                    data += "--" + boundry + crlf;
                                    data += "Content-Disposition: form-data; name=\"file\"; filename=" + iFilename + crlf;
                                    data += "Content-Type: text/plain" + crlf;
                                    data += "Content-Length:" + fileContent.length() + crlf;
                                    data += crlf;
                                    data += fileContent + crlf;
                                    data += "--" + boundry + "--" + crlf;
                                    _fOptionDone = true;
                                } else {
                                    System.out.println(iFilename + ": input file does not exist");
                                    return;
                                }
                            } else {
                                System.out.println("-f: missing a input file name");
                                return;
                            }
                        }
                        break;
                    // -o filename Write the body of the response to the specific file
                    case "-o":
                        if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                            String oFilename = commandLine[i];
                            File oFile = new File(oFilename);
                            if (/*oFile.exists()*/ false) {
                                System.out.println(oFilename + ": output file exists");
                                return;
                            } else {
                                oFile.createNewFile();
                                bwF = new BufferedWriter(new FileWriter(oFile));
                                writeInFile = true;
                            }
                        } else {
                            System.out.println("-o: missing a output file name");
                            return;
                        }
                        break;
                    default:
                        if (url.isEmpty()) {
                            if (checkURLFormat(option)) {
                                url = option;
                                validURL = true;
                            } else {
                                System.out.println(option + ": invalid command");
                                return;
                            }
                        } else {
                            System.out.println(option + ": invalid command");
                            return;
                        }
                        break;
                }
            }
            
            while (true) {
            
                // Set host and path
                if (url.isEmpty()) {
                    System.out.println("post: missing URL");
                    return;
                } else if (url.length() >= 9 && url.substring(0, 9).equals("localhost")) {
                    int separateIndex = url.indexOf("/", 9);
                    host = url.substring(0, 9);
                    PORT = locahostPort;
                    if (-1 != separateIndex) { path = url.substring(separateIndex); }
                } else {
                    int separateIndex = url.indexOf("/", 7);
                    if (-1 == separateIndex) {
                        host = url.substring(7, url.length());
                    } else {
                        host = url.substring(7, separateIndex);
                        path = url.substring(separateIndex);
                    }
                }
                
                // Establish TCP connection through socket
//                Socket socket = new Socket(InetAddress.getByName(host), PORT);
                MyClientSocket myClientSocket = new MyClientSocket(3000, PORT);

//                // Prepare request
//                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
//                bw.write(method + " " + path + " " + httpVersion + crlf);
//                bw.write("Host: " + host + crlf);
//                bw.write("User-Agent: " + USER_AGENT + crlf);
//                if (_fOptionDone)
//                    bw.write("Content-Type: multipart/form-data; boundary=" + boundry + crlf);
//
//                // Associates headers to HTTP Request, -h
//                if (!headers.isEmpty()) {
//                    for (String header : headers) {
//                        bw.write(header + crlf);
//                    }
//                }
//
//                // Associates an inline data or a file to the body HTTP POST request, -d, -f
//                if (!data.isEmpty()) {
//                    bw.write("Content-Length: " + data.length() + crlf);
//                }
//
//                bw.write(crlf);
//                bw.write(data);
//                bw.flush();

                // Prepare request
                String request = "";
                request += method + " " + path + " " + httpVersion + crlf;
                request += "Host: " + host + crlf;
                request += "User-Agent: " + USER_AGENT + crlf;
                if (_fOptionDone) {
                    request += "Content-Type: multipart/form-data; boundary=" + boundry + crlf;
                }
                // Associates headers to HTTP Request, -h
                if (!headers.isEmpty()) {
                    for (String header : headers) {
                        request += header + crlf;
                    }
                }
                if (!data.isEmpty()) {
                    request += "Content-Length: " + data.length() + crlf;
                }
                request += crlf;
                request += data;
                myClientSocket.send(request);

                
                // Get response
                br = new BufferedReader(new StringReader(myClientSocket.receive()));
                String responseLine;
                boolean contentPart = false;
                while ((responseLine = br.readLine()) != null) {
                    // Check status code
                    if (!statusLineChecked && responseLine.contains(code302)) {
                        statusLineChecked = true;
                        redirect = true;
                    } else
                        if (!contentPart && responseLine.equals("")) {
                        contentPart = true;
                    }
                    // handle redirect location
                    if (redirect && responseLine.length() > 10 && responseLine.substring(0, 10).equals("Location: ")) {
                        String newLocation = responseLine.substring(10, responseLine.length());
                        // determine relative/absolute redirect
                        if (newLocation.length() >= 7 && newLocation.substring(0, 7).equals("http://")) {
                            url = newLocation;
                        } else {
                            url = "http://" + host + newLocation;
                        }
                        if (seeRedirectDetail) {
                            System.out.println("302: redirect to \"" + url + "\"");
                        }
                        break;
                    }
                    if (!contentPart) {
                        responseHeader += responseLine + "\n";
                    } else {
                        responseBody += responseLine + "\n";
                    }
                }
                
                // Check redirect
                if (redirect) {
                    statusLineChecked = false;
                    redirect = false;
                    responseHeader = "";
                    responseBody = "";
                    continue;
                }

                break;
            }
            
            // Generate output
            if (showDetail) {
                output += responseHeader;
            }
            output += responseBody;
            
            // Determine where to output, -o
            if (writeInFile) {
                bwF.write(output);
            } else {
                System.out.println(output);
            }
            
        } catch (Exception e) {
            System.out.println("ERROR of POST: " + e);
            e.printStackTrace();
        } finally {
            try {
                if (null != bw) {
                    bw.close();
                }
                if (null != br) {
                    br.close();
                }
                if (null != brF) {
                    brF.close();
                }
                if (null != bwF) {
                    bwF.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
