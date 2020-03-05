public class httpfs {

    public static void main(String[] args) throws Exception {

        HttpFileServer httpfs = new HttpFileServer();
        int argsLength = args.length;

        for (int i = 0; i < argsLength; ++i) {
            String option = args[i];
            switch(option) {
                case "help":
                    StringBuilder httpfsHelpInfo = new StringBuilder("\nhttpfs is a simple file server.\n\n");
                    httpfsHelpInfo.append("usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n\n");
                    httpfsHelpInfo.append("    -v Prints debugging messages.\n");
                    httpfsHelpInfo.append("    -p Specifies the port number that the server will listen and serve at.\n");
                    httpfsHelpInfo.append("       Default is 8080.\n");
                    httpfsHelpInfo.append("    -d Specifies the directory that the server will use to read/write\n");
                    httpfsHelpInfo.append("       requested files. Default is the current directory when launching the\n");
                    httpfsHelpInfo.append("       application.\n\n");
                    System.out.println(httpfsHelpInfo.toString());
                    break;
                case "-v":
                    httpfs.printDebugMessage();
                    break;
                case "-p":
                    if (++i < argsLength) {
                        String tempPortNumber = args[i];
                        try {
                            httpfs.setPortNumber(Integer.parseInt(tempPortNumber));
                        } catch (Exception exception) {
                            System.out.println(tempPortNumber + ": port number has to be an integer");
                        }
                    } else {
                        System.out.println("-p: missing port number");
                    }
                    break;
                case "-d":
                    if (++i < argsLength) {
                        // TODO: check valid path here i.e. httpfs -d -p 8080 ->> "8080: unknown command"
                        httpfs.setDirectoryPath(args[i]);
                    } else {
                        System.out.println("-d: missing directory path");
                    }
                    break;
                default:
                    System.out.println(option + ": unknown command");
                    break;
            }
        }

        httpfs.run();
    }
}
