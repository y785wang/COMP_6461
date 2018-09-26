import java.io.EOFException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Map;
import java.util.ArrayList;

public class httpc {

    private final String USER_AGENT = "Concordia-HTTP/1.0";
    
    private static boolean checkURLFormat(String url) {
        int length = url.length();
        return length > 6 && url.substring(0, 7).equals("http://");
    }

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
        } else { // 1 < args.length
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
                case "get": {
                    int length = args.length;
                    if (!checkURLFormat(args[length-1])) {
                        System.out.println("'" + args[1] + "'" + ": invalid URL");
                        break;
                    }
                    httpc http = new httpc();
                    switch (length) {
                        case 2:
                            http.sendGet(args[1], "");
                            break;
                        case 3:
                            switch (args[1]) {
                                case "-v":
                                    http.sendGet(args[2], "v");
                                    break;
                                case "-h":
                                    http.sendGet(args[2], "h");
                                    break;
                                default:
                                    System.out.println(args[1] + ": command nor found");
                                    break;
                            }
                            break;
                        case 4:
                            if (args[1].equals("-v") && args[2].equals("-h") ||
                                args[1].equals("-h") && args[2].equals("-v")) {
                                http.sendGet(args[2], "vh");
                            } else if (args[1].equals(args[2]) && (args[1].equals("-v") || args[1].equals("-h"))) {
                                System.out.println(args[1] + ": duplicated command");
                            } else if (args[1].equals("-v") || args[1].equals("-h")) {
                                System.out.println(args[2] + ": command nor found");
                            } else if (args[2].equals("-v") || args[2].equals("-h")) {
                                System.out.println(args[1] + ": command nor found");
                            }
                            break;
                        default :
                            System.out.println("too many options");
                            break;
                    }
                    break;
                }
                case "post": {
                    httpc http = new httpc();
                    http.sendPost(args);
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
     
     Parameters:
     
     Returns:

     
     **************************************************/
    private void sendGet(String url, String commandOptions) throws Exception {

        BufferedWriter bw = null;
        BufferedReader br = null;
        String contents = "";
        String detailedResponse = "";
        String associatedHeaders = "";
        
        try {
            int port = 80;
            int separateIndex = url.indexOf("/", 7);
            String host = "";
            String path = "";
            if (-1 == separateIndex) {
                host = url.substring(7, url.length());
            } else {
                host = url.substring(7, separateIndex);
                path = url.substring(separateIndex);
            }
            
            Socket socket = new Socket(InetAddress.getByName(host), 80);
            
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            bw.write("GET " + path + " HTTP/1.0\r\n");
            bw.write("Host: " + "httpbin.org\r\n");
            bw.write("\r\n\r\n");
            bw.flush();
            
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            boolean nextPart = false;
            String line;
            
            while ((line = br.readLine()) != null) {
                if (!nextPart && line.equals("")) {
                    nextPart = true;
                }
                if (!nextPart) {
                    detailedResponse += line + "\n";
                } else {
                    contents += line + "\n";
                }
            }
            
        } catch (Exception e) {
            System.out.println("ERROR of GET: " + e);
            e.printStackTrace();
        }

        finally {
            try {
                if (null != bw) {
                    bw.close();
                }
                if (null != br) {
                    br.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        
        switch (commandOptions) {
            case "v":
                System.out.println(detailedResponse);
                System.out.println(contents);
                break;
            case "h":
                System.out.println(contents);
                break;
                
            case "vh":
                
                break;
            default:
                System.out.println(contents);
        }
    }

    
    
    
    /**************************************************
     
     Description:
     
     Parameters:
     
     Returns:
     
     
     **************************************************/
    private void sendPost(String[] commandLine) throws Exception {

        BufferedWriter bw = null;
        BufferedReader br = null;
        String content = "";
        String detailedResponse = "";
        boolean showDetail = false;
        
        try {
            int port = 80;
            String host = "";
            String path = "";
            String url = "";
            ArrayList<String> headers = new ArrayList<String>();
            String data = "";
            String fileName = "";
            int numOfToken = commandLine.length;
            boolean validURL = false;
            
            // Parsing command line arguments
            for (int i = 1; i < numOfToken; ++i) {
                String option = commandLine[i];
                switch (option) {
                    case "-v":
                        showDetail = true;
                        break;
                    case "-h":
                        if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                            headers.add(commandLine[i]);
                        } else {
                            System.out.println("-h: missing key:value pair(s)");
                            return;
                        }
                        break;
                    case "-d": case "--d":
                        if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                            data = commandLine[i];
                        } else {
                            System.out.println("-d: missing an inline data");
                            return;
                        }
                        break;
                    case "-f":
                        if (++i < numOfToken && !checkURLFormat(commandLine[i])) {
                            fileName = commandLine[i];
                            //TODO data <- file content
                        } else {
                            System.out.println("-f: missing a file name");
                            return;
                        }
                        break;
                    default:
                        if (checkURLFormat(option)) {
                            url = option;
                            validURL = true;
                        } else {
                            System.out.println("post: invalid URL");
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
            
            Socket socket = new Socket(InetAddress.getByName(host), 80);
            
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            bw.write("POST " + path + " HTTP/1.0\r\n");
            bw.write("Host: " + "httpbin.org\r\n");
            bw.write("User-Agent: Concordia-HTTP/1.0\r\n");
            
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
            
        } catch (Exception e) {
            System.out.println("ERROR of GET: " + e);
            e.printStackTrace();
        }
        
        finally {
            try {
                if (null != bw) {
                    bw.close();
                }
                if (null != br) {
                    br.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        
        // Output response
        if (showDetail) {
            System.out.println(detailedResponse);
        }
        System.out.println(content);
    }
}
