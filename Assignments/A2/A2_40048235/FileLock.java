class FileLock {

    private String filename;
    private int numOfReading;
    private int numOfWriting;

    public FileLock(String filename, String readOrWrite) {
        this.filename = filename;
        switch (readOrWrite) {
            case "READ":
                numOfReading = 1;
                numOfWriting = 0;
                break;
            case "WRITE":
                numOfReading = 0;
                numOfWriting = 1;
                break;
            default:
                System.out.println("FileLock.ctor(): Invalid lock " + readOrWrite);
                break;
        }
    }

    public String getFilename() { return filename; }

    public int getNumOfReading() { return numOfReading; }

    public int getNumOfWriting() { return numOfWriting; }

    public void addNumOfReading() { ++numOfReading; }

    public void reduceNumOfReading() { --numOfReading; }
}