import java.io.EOFException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Map;
import java.util.ArrayList;





public class httpc {

    private final String USER_AGENT = "Concordia-HTTP/1.0";
    private final int PORT = 80;
    
    
    
    
    /**************************************************
     
     Description:
     Determine if the given url is start with "http://"
     
     Parameters:
     A string that represents an url
     
     Returns:
     Return true if the given url string satisfy the
     codition, false otherwise
     
     **************************************************/
    private static boolean checkURLFormat(String url) {
        int length = url.length();
        return length > 6 && url.substring(0, 7).equals("http://");
    }

    
    
    
    /**************************************************
     
     Description:
     Main function, deal with the command line input,
     and basic help information about how should a user
     issues the command to achieve the get and post
     requests
     
     Parameters:
     A string array of command line arguments
     
     Returns:
     None
     
     **************************************************/
    public static void main(String[] args) throws Exception {
        
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


    
    
    /**************************************************
     
     Description:
     Deal with either get or post request with options,
     get the result from the server, then output the
     response
     
     Parameters:
     A string array of command line arguments
     
     Returns:
     None
     
     **************************************************/
    private void sendRequest(String[] commandLine) throws Exception {
        
        String content = "";
        String detailedResponse = "";
        BufferedWriter bw = null;
        BufferedReader br = null;
        BufferedWriter bwF = null;
        BufferedReader brF = null;
        boolean showDetail = false;
        
        try {
            String method = commandLine[0].equals("get") ? "GET" : "POST";
            String host = "";
            String path = "";
            String url = "";
            ArrayList<String> headers = new ArrayList<String>();
            String data = "";
            int numOfToken = commandLine.length;
            boolean validURL = false;
            boolean _dOptionDone = false;
            boolean _fOptionDone = false;
            boolean writeInFile = false;
            
            // Parsing command line arguments
            for (int i = 1; i < numOfToken; ++i) {
                String option = commandLine[i];
                switch (option) {
                    case "-v":
                        // TODO check -v?
                        showDetail = true;
                        break;
                    case "-h":
                        // TODO check -h?
                        if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                            headers.add(commandLine[i]);
                        } else {
                            System.out.println("-h: missing key:value pair(s)");
                            return;
                        }
                        break;
                    case "-d": case "--d":
                        if (method.equals("GET")) {
                            System.out.println(option + ": invalid command");
                            return;
                        } else {
                            // TODO check _dOptionDone?
                            if (_fOptionDone) {
                                System.out.println(option + " and -f can not be used together");
                                return;
                            } else if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                                data = commandLine[i];
                                _dOptionDone = true;
                            } else {
                                System.out.println("-d: missing an inline data");
                                return;
                            }
                        }
                        break;
                    case "-f":
                        if (method.equals("GET")) {
                            System.out.println(option + ": invalid command");
                            return;
                        } else {
                            // TODO check _fOptionDone?
                            if (_dOptionDone) {
                                System.out.println("-d and -f can not be used together");
                                return;
                            } else if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                                String iFilename = commandLine[i];
                                File iFile = new File(iFilename);
                                if (iFile.exists()) {
                                    brF = new BufferedReader(new FileReader(iFile));
                                    String line;
                                    while ((line = brF.readLine()) != null) {
                                        data += line + "\n";
                                    }
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
                        if (url.isEmpty() && checkURLFormat(option)) {
                            url = option;
                            validURL = true;
                        } else {
                            System.out.println(option + ": invalid command");
                            return;
                        }
                        break;
                }
            }
            
            // Set host and path
            if (url.isEmpty()) {
                System.out.println("post: missing URL");
                return;
            } else {
                int separateIndex = url.indexOf("/", 7);
                if (-1 == separateIndex) {
                    host = url.substring(7, url.length());
                } else {
                    host = url.substring(7, separateIndex);
                    path = url.substring(separateIndex);
                }
            }
            
            Socket socket = new Socket(InetAddress.getByName(host), PORT);
            
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            bw.write(method + " " + path + " HTTP/1.0\r\n");
            bw.write("Host: " + host + "\r\n");
            bw.write("User-Agent: " + USER_AGENT + "\r\n");
            
            // Associates headers to HTTP Request
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    bw.write(header + "\r\n");
                }
            }
            
            // Associates an inline data to the body HTTP POST request
            if (!data.isEmpty()) {
                bw.write("Content-Length:" + data.length() + "\r\n");
            }
            
            bw.write("\r\n");
            bw.write(data);
            bw.flush();
            
            // Get response
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            boolean contentPart = false;
            while ((line = br.readLine()) != null) {
                if (!contentPart && line.equals("")) {
                    contentPart = true;
                }
                if (!contentPart) {
                    detailedResponse += line + "\n";
                } else {
                    content += line + "\n";
                }
            }
            
            // Generate output
            String output = "";
            if (showDetail) {
                output += detailedResponse;
            }
            output += content;
            
            // Determine where to output
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
