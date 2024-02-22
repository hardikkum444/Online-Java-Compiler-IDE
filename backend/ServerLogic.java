import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.security.*;
import java.net.SocketPermission;

public class ServerLogic {

    private static ServerSocket myServer;

    public static void main(String[] args) {


        // System.setSecurityManager(new CustomSecurityManager("java.net.SocketPermission"));


        try {
            myServer = new ServerSocket(9000);

            System.out.println("Server started. Listening on port 9000...");

            while (true) {
                Socket client = myServer.accept();
                System.out.println("Client connected: " + client.getInetAddress());

                BufferedReader reader = IOutils.createReader(client);
                PrintWriter writer = IOutils.createWriter(client);

                String person = "";
                String output = "";
                String fileName = "";
                String input = "";
                boolean isFin = false;

                StringBuilder requestBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        isFin = true;
                        break;
                    }
                    requestBuilder.append(line).append("\n");
                }

                //-------------------------------------------------------------------------------------------------------
                // checking and implementing the payload
                if (isFin) {
                    // Check if the request has a payload
                    int contentLength = 0;
                    String contentLengthHeader = "Content-Length: ";
                    for (String header : requestBuilder.toString().split("\n")) {
                        if (header.startsWith(contentLengthHeader)) {
                            contentLength = Integer.parseInt(header.substring(contentLengthHeader.length()).trim());
                            break;
                        }
                    }

                    // Read the payload
                    StringBuilder payloadBuilder = new StringBuilder();
                    int bytesRead = 0;
                    while (bytesRead < contentLength) {
                        int c = reader.read();

                        if (c == -1) {
                            break;
                        }

                        char character = (char) c;
                        payloadBuilder.append(character);

                        String payload1 = payloadBuilder.toString();

                        // this is for extracting the file name
                        if (payload1.contains("public class") && (payload1.contains(" {"))) {
                            int index1 = payload1.indexOf("public class") + "public class".length();
                            int index2 = payload1.indexOf(" {", index1);
                            fileName = payload1.substring(index1, index2).trim();
                        }

                        // this is for user input
                        if (payload1.contains("//*")) {
                            int index3 = payload1.indexOf("//*") + "//*".length();
                            String trimmedSubstring = payload1.substring(index3).trim();
                            if (trimmedSubstring.contains("//*")) {
                                trimmedSubstring = trimmedSubstring.replace("//*", "");
                            }
                            input = trimmedSubstring;
                        }

                        bytesRead++;
                    }

                    // finally getting the code stored and extracted from the payload
                    String payload = payloadBuilder.toString().trim();

                    //creating a new file 
                    File code = new File(fileName + ".java"); 
                    try (PrintWriter codeWriter = new PrintWriter(code)) {
                        codeWriter.println(payload);
                    }

                    //command to perform compiling
                    String command = "javac -d . " + fileName + ".java";
                    Process compile = Runtime.getRuntime().exec(command);

                    // BufferedReader errorReader = IOutils.createReader(compile);
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
                    StringBuilder compileError = new StringBuilder();
                    String error = "";

                    while ((error = errorReader.readLine()) != null) {
                        compileError.append(error).append("\n");
                    }

                    //printing an error if the java code given by the user consists of some
                    String finalError = compileError.toString();
                    if (!finalError.isEmpty()) {
                        output = finalError;
                    } else {
                        String command_2 = "java " + fileName; 

                        Process execute = Runtime.getRuntime().exec(command_2);

                        if (!input.isEmpty()) {
                            OutputStream outputStream = execute.getOutputStream();
                            PrintWriter writer1 = new PrintWriter(new OutputStreamWriter(outputStream));
                            writer1.println(input); // Write input to the process
                            writer1.flush();
                        }

                        BufferedReader execReader = IOutils.createReader(execute);
                        StringBuilder outputBuilder = new StringBuilder();
                        String execLine = "";
                        while ((execLine = execReader.readLine()) != null) {
                            outputBuilder.append(execLine).append("\n");
                        }

                        output = outputBuilder.toString();
                    }

                    //handles deleting the files that have been created to avoid clutter in the machine (my machine as of now)
                    //basically deleting the unwanted files
                    delete(fileName,0);
                    delete(fileName,1);

                }
                //-------------------------------------------------------------------------------------------------------
                
                String request = requestBuilder.toString();
                System.out.println("Received request:\n" + request);

                // Add CORS headers to allow requests from any origin
                writer.print("HTTP/1.1 200 OK\r\n");
                writer.print("Content-Type: text/plain\r\n");
                writer.print("Access-Control-Allow-Origin: *\r\n"); // Allow requests from any origin
                writer.print("\r\n");

                writer.print(output);

                writer.flush();

                client.close();
            }
        } catch (IOException error) {
            error.printStackTrace();
            System.out.println("Error");
        } finally {

            if(myServer!=null){
                try {
                myServer.close();
                
                //releasing the resources after usage
                System.out.println("Server has been closed on port 9000");
                }catch (IOException prob){
                    prob.printStackTrace();
                }
            }
        }
    }

    public static void delete(String filename,int op){

        if(op == 1){
            String filePath = filename + ".class";
            File fileToDelete = new File(filePath);
            fileToDelete.delete();

        }else{
            String filePath = filename + ".java";
            File fileToDelete = new File(filePath);
            fileToDelete.delete();   
        }

    }
}
    class IOutils {

    public static BufferedReader createReader(Socket socket) throws IOException{

        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static BufferedReader createReader(Process process) throws IOException{

        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }


    public static PrintWriter createWriter(Socket socket) throws IOException{

        return new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public static PrintWriter createWriter(Process process) throws IOException{

        return new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
    }    
    
}





// public class CustomSecurityManager extends SecurityManager {
//     private final String allowedDirectory;

//     public CustomSecurityManager(String allowedDirectory) {
//         this.allowedDirectory = allowedDirectory;
//     }

//     @Override
//     public void checkRead(String file) {
//         if (!file.startsWith(allowedDirectory)) {
//             throw new SecurityException("Read access denied for file: " + file);
//         }
//     }

//     @Override
//     public void checkWrite(String file) {
//         if (!file.startsWith(allowedDirectory)) {
//             throw new SecurityException("Write access denied for file: " + file);
//         }
//     }

//     @Override
//     public void checkConnect(String host, int port) {
//         throw new SecurityException("Network access denied");
//     }
// }

