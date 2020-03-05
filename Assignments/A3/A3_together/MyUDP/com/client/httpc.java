package com.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.*;

public class httpc {

    private static final String HELP_INFO = "\nclient is a curl-like application but supports HTTP protocol only.\n" +
            "Usage:\n" +
            "    client command [arguments]\n\n" +
            "The commands are:\n" +
            "    get     executes a HTTP GET request and prints the response.\n" +
            "    post    executes a HTTP POST request and prints the response.\n" +
            "    help    prints this screen.\n" +
            "Use \"client help [command]\" for more information about a command.";

    private static final String GET_INFO = "\nUsage: client get [-v] [-h key:value] URL\n\n" +
            "Get executes a HTTP GET request for a given URL.\n\n" +
            "    -v            Prints the detail of the response such as protocol, status, and headers.\n" +
            "    -h key:value  Associates headers to HTTP Request with the format 'key:value'.\n"+
            "    -o file       Write the body of the response to the specified file instead of the console.\n";

    private static final String POST_INFO="\nUsage: client post [-v] [-h key:value] [-d inline-data] [-f file] URL.\n\n"+
            "Post executes a HTTP POST request for a given URL with inline data or from file.\n\n" +
            "    -v            Prints the detail of the response such as protocol, status, and headers.\n" +
            "    -h key:value  Associates headers to HTTP Request with the format 'key:value'.\n" +
            "    -d string     Associates an inline data to the body HTTP POST request.\n" +
            "    -f file       Associates the content of a file to the body HTTP POST request.\n" +
            "    -o file       Write the body of the response to the specified file instead of the console.\n\n"+
            "Either [-d] or [-f] can be used but not both.\n";


    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println(HELP_INFO);
            return;
        }

        switch (args[0]) {
            case "help":
                helpCmd(args);
                break;
            case "get":
                getCmd(args);
                break;
            case "post":
                postCmd(args);
                break;
            default:
                System.out.println("Command not recognized");
                System.out.println(HELP_INFO);
                break;
        }


    }

    private static void helpCmd(String[] args){
        if (args.length==1)
            System.out.println(HELP_INFO);
        else if(args[1].equals("get"))
            System.out.println(GET_INFO);
        else if(args[1].equals("post"))
            System.out.println(POST_INFO);
        else{
            System.out.println("Command not recognized");
            System.out.println(HELP_INFO);
        }

    }

    private static void getCmd(String[] args) throws IOException {
        Request request = cmdParser(args);

//        System.out.println(request.isPost);
//        System.out.println(request.url);
//        System.out.println("v: "+request.verbose);
//        System.out.println(request.isOutput);
//        System.out.println(request.outputFileName);
//        System.out.println(request.errMsg);

        if (request.isValid) {
            LibHTTP aLibHTTP = new LibHTTP();
            String response = aLibHTTP.get(request.url, request.userHeader);

            if (!request.isOutput){
                if (!request.verbose)
                    System.out.println(extractBody(response));
                else
                    System.out.println(response);
            }
            else{
                if(!request.verbose)
                    System.out.println(writeToFile(request.outputFileName,extractBody(response)));
                else
                    System.out.println(writeToFile(request.outputFileName,response));
            }

        } else {
            System.out.println("ERROR: " + request.errMsg);
            System.out.println(GET_INFO);
        }
    }

    private static void postCmd(String[] args) throws IOException {
        Request request = cmdParser(args);
        if (request.isValid) {
            LibHTTP aLibHTTP = new LibHTTP();
            String response;

            if(request.postMode==request.postMode.DATA){
                response = aLibHTTP.postData(request.url, request.userHeader, request.userData);
            }else{
                response = aLibHTTP.postFile(request.url, request.userHeader, request.inputFileName);
            }

            if (!request.isOutput){
                if (!request.verbose)
                    System.out.println(extractBody(response));
                else
                    System.out.println(response);
            }
            else{
                if(!request.verbose)
                    System.out.println(writeToFile(request.outputFileName,extractBody(response)));
                else
                    System.out.println(writeToFile(request.outputFileName,response));
            }

        } else {
            System.out.println("ERROR: " + request.errMsg);
            System.out.println(POST_INFO);
        }
    }

    private static String extractBody(String response) {
        Pattern pBody = Pattern.compile("[\\s\\S]*(?:\\n\\n|\\r\\n\\r\\n)([\\s\\S]*)");
        Matcher mBody = pBody.matcher(response);
        if (mBody.matches())
            return mBody.group(1);
        return "";
    }

    private static String writeToFile(String filename, String content) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filename);
            fout.write(content.getBytes());
            return "Write to file "+filename+" successfully";
        } catch (FileNotFoundException e) {
            return "Illegal file path: " + filename;
        } catch (IOException e) {
            return "Write to file " + filename + " failed";
        } finally {
            if (fout != null)
                fout.close();
        }
    }

    private static Request cmdParser(String[] args) {

        Request request = new Request();

        //identify mode
        if (args[0].equals("get"))
            request.isPost = false;
        else
            request.isPost = true;

        for (int i = 1; i < args.length; ++i) {
            switch (args[i]) {
                case "-h":
                    if (i <= args.length - 2 && !isURL(args[i + 1]) && !isOption(args[i + 1])) {
                        if(request.userHeader.isEmpty())
                            request.userHeader += (args[++i]);
                        else
                            request.userHeader += ("\r\n" + args[++i]);
                    } else {
                        request.isValid = false;
                        request.errMsg = "No value paired with '-h' option";
                        return request;
                    }
                    break;
                case "-v":
                    request.verbose = true;
                    break;
                case "-o":
                    request.isOutput = true;
                    if (i <= args.length - 2 && !isURL(args[i + 1]) && !isOption(args[i + 1])) {
                        request.outputFileName = args[++i];
                    } else {
                        request.isValid = false;
                        request.errMsg = "No output file name associated with '-o' option";
                        return request;
                    }
                    break;
                default:
                    if (isURL(args[i])) {
                        request.url = args[i];
                    } else if (request.isPost = true) {
                        //try to match with POST cmd
                        switch (args[i]) {
                            case "-d":

                                if (request.postMode == request.postMode.FILE) {
                                    request.isValid = false;
                                    request.errMsg = "Cannot use '-d' and '-f' options at the same time";
                                    return request;
                                }

                                if (i <= args.length - 2 && !isURL(args[i + 1]) && !isOption(args[i + 1])) {
                                    request.postMode = request.postMode.DATA;
                                    request.userData = args[++i];
                                } else {
                                    request.isValid = false;
                                    request.errMsg = "No value associated with '-d' option";
                                    return request;
                                }
                                break;

                            case "-f":
                                if (request.postMode == request.postMode.DATA) {
                                    request.isValid = false;
                                    request.errMsg = "Cannot use '-d' and '-f' options at the same time";
                                    return request;
                                }

                                if (i <= args.length - 2 && !isURL(args[i + 1]) && !isOption(args[i + 1])) {
                                    request.postMode = request.postMode.FILE;
                                    request.inputFileName = args[++i];
                                } else {
                                    request.isValid = false;
                                    request.errMsg = "No value paired with '-f' option";
                                    return request;
                                }
                                break;
                            default:
                                request.isValid = false;
                                request.errMsg = "Unrecognized command: " + args[i];
                                return request;

                        }
                    } else {
                        request.isValid = false;
                        request.errMsg = "Unrecognized command: " + args[i];
                        return request;
                    }
            }
        }

        //validate if url exists
        if (request.url.equals("###N/A###")) {
            request.isValid = false;
            request.errMsg = "URL is required";
            return request;
        }

        //validate if post mode has -d or -f option
        if (request.isPost == true && request.postMode == request.postMode.UNSET) {
            request.isValid = false;
            request.errMsg = "Post method has to come with either -d or -f option";
            return request;
        }

        return request;
    }

    private static boolean isURL(String url) {
        Pattern pURL = Pattern.compile("^['\"]?https?:\\/\\/.*['\"]?$");
        Matcher mURL = pURL.matcher(url.toLowerCase());
        if (mURL.matches())
            return true;
        return false;
    }

    private static boolean isOption(String url) {
        Pattern pOption = Pattern.compile("^-[dfvoh]+$");
        Matcher mOption = pOption.matcher(url.toLowerCase());
        if (mOption.matches())
            return true;
        return false;
    }


}
