import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.PrintWriter;
import java.lang.System;
import java.io.IOException;
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.PatternSyntaxException;

//
// This is an implementation of a simplified version of a command
// line ftp client. The program always takes two arguments
//


public class CSftp{

    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;

    public static void main(String [] args){
        byte cmdString[] = new byte[MAX_LEN];


        String secondHalf = "";
        Boolean get = false;
        String errorHost = "";
        int errorPort = 0;
        Boolean control = false;

        // Get command line arguments and connected to FTP
        // If the arguments are invalid or there aren't enough of them
        // then exit.

        if (args.length > ARG_CNT) {
            System.out.print("Usage: cmd ServerAddress ServerPort\n");
            return;
        }


        String host = args[0];
        int portNo;
        if (args.length == 1){
            portNo = 21;
        } else {
            portNo = Integer.parseInt(args[1]);
        }

        // some booleans for error cases

        try {
            Socket csSocket = new Socket();
            errorHost = host;
            errorPort = portNo;
            csSocket.connect(new InetSocketAddress(host, portNo), 20000);

            PrintWriter out = new PrintWriter(csSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(csSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            String serverRes;
            String userIn;
            int numOfParam;

            String pasvRes;
            String pasvHost;
            int    pasvPort;
            Socket pasvSocket;

            Boolean terminate = false;

            // Read first Line
            serverRes = in.readLine();
            System.out.println("<-- " + serverRes);

            while (!terminate) {

                // reset some bools
                get = false;

                // asks for user input
                System.out.print("csftp> ");
                userIn = stdIn.readLine();
                secondHalf = userIn.substring(userIn.indexOf(" ")+1, userIn.length());
                if (secondHalf.trim().isEmpty()){
                    secondHalf = null;
                }

                // dispatches to whatever the case is
                switch (casesToNum(userIn)) {

                    // User
                    case 1:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam == 1) {
                            System.out.println("--> " + userIn);
                            userIn = userIn.replace("user", "USER");
                            out.println(userIn);
                            // prints the server response
                            serverRes = in.readLine();
                            System.out.println("<-- " + serverRes);
                        } else {
                            print002();
                        }
                        break;

                    // pw
                    case 2:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam == 1) {
                            System.out.println("--> " + userIn);
                            userIn = userIn.replace("pw", "PASS");
                            out.println(userIn);
                            // prints the server response
                            serverRes = in.readLine();
                            System.out.println("<-- " + serverRes);
                        } else {
                            print002();
                        }
                        break;

                    // quit
                    case 3:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam < 1) {
                            System.out.println("--> " + userIn);
                            userIn = userIn.replace("quit", "QUIT");
                            out.println(userIn);
                            // prints the server response
                            serverRes = in.readLine();
                            System.out.println("<-- " + serverRes);
                            // Breaks if server says Goodbye
                            if (serverRes.contains("Bye.") || serverRes.contains("Goodbye.")) {
                                terminate = true;
                            }
                        } else {
                            print002();
                        }
                        break;

                    // get
                    case 4:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam == 1) {
                            get = true;

                            System.out.println("--> " + userIn);
                            out.println("PASV");
                            System.out.println("--> " + "PASV");

                            pasvRes = in.readLine();
                            pasvHost = getHostPASV(pasvRes);
                            pasvPort = getPortPASV(pasvRes);


                            pasvSocket = new Socket();
                            errorHost = pasvHost;
                            errorPort = pasvPort;
                            pasvSocket.connect(new InetSocketAddress(pasvHost, pasvPort), 10000);
                            System.out.println("<-- " + pasvRes);

                            if (pasvSocket != null) {

                                BufferedReader pasvIn = new BufferedReader(
                                        new InputStreamReader(pasvSocket.getInputStream()));

                                out.println("RETR " + secondHalf);
                                System.out.println("--> " + "RETR " + secondHalf);

                                serverRes = in.readLine();

                                if (serverRes.startsWith("150")) {
                                    OutputStream writer = new FileOutputStream(secondHalf);
                                    InputStream inputStream = pasvSocket.getInputStream();

                                    byte[] buffer = new byte[1024 * 1024 * 1024];
                                    int i;
                                    while ((i = inputStream.read(buffer)) > 0) {
                                        writer.write(buffer, 0, i);
                                    }

                                    writer.close();
                                    inputStream.close();

                                }
                                serverRes = in.readLine();
                                System.out.println("<-- " + serverRes);
                                pasvIn.close();
                            }
                            pasvSocket.close();

                        } else {
                            print002();
                        }

                        break;

                    // features
                    case 5:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam < 1) {
                            System.out.println("--> " + userIn);
                            userIn = userIn.replace("features", "FEAT");
                            out.println(userIn);

                            serverRes = in.readLine();
                            System.out.println("<-- " + serverRes);

                            // Read and print the list of features
                            Boolean bool = true;
                            while (bool) {
                                serverRes = in.readLine();
                                if (serverRes.startsWith(" ")) {
                                    System.out.println("<-- " + serverRes);
                                } else {
                                    bool = false;
                                    break;
                                }
                            }
                            System.out.println("<-- " + serverRes);
                        } else {
                            print002();
                        }
                        break;

                    // cd
                    case 6:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam == 1) {
                            System.out.println("--> " + userIn);
                            out.println("CWD " + secondHalf);
                        }
                        else {
                            print002();
                        }
                        // prints the server response
                        serverRes = in.readLine();
                        System.out.println("<-- " + serverRes);
                        break;

                    // dir
                    case 7:
                        numOfParam = getNumberOfParam(userIn);
                        if (numOfParam < 1) {
                            System.out.println("--> " + userIn);
                            out.println("PASV");
                            System.out.println("--> " + "PASV");

                            pasvRes = in.readLine();
                            pasvHost = getHostPASV(pasvRes);
                            pasvPort = getPortPASV(pasvRes);

                            pasvSocket = new Socket();
                            errorHost = pasvHost;
                            errorPort = pasvPort;
                            pasvSocket.connect(new InetSocketAddress(pasvHost, pasvPort), 10000);


                            System.out.println("<-- " + pasvRes);

                            if (pasvSocket != null) {

                                BufferedReader pasvIn = new BufferedReader(
                                        new InputStreamReader(pasvSocket.getInputStream()));

                                out.println("LIST");
                                System.out.println("--> " + "LIST");

                                serverRes = in.readLine();
                                System.out.println("<-- " + serverRes);

                                while ((pasvRes = pasvIn.readLine()) != null) {
                                    System.out.println("<-- " + pasvRes);
                                }

                                pasvIn.close();
                                pasvSocket.close();
                                serverRes = in.readLine();
                                System.out.println("<-- " + serverRes);
                                break;
                            }
                        } else{
                            print002();
                        }
                        break;
                        // Blank input
                        case 8:
                          break;

                        case 9:
                          break;

                        default:
                            System.out.println("--> " + userIn);
                            out.println(userIn);
                            serverRes = in.readLine();
                            System.out.println("<-- " + serverRes);
                            print001();
                            break;
                      }
            }

            in.close();
            csSocket.close();
            out.close();
            stdIn.close();

        } catch (SocketTimeoutException exception){
          if (control){
            printFFFC(errorHost, errorPort);
          } else {
            print3A2(errorHost, errorPort);
          }
        } catch (SocketException exception){
          if (control){
            printFFFD();
          } else {
            print3A7();
          }
        } catch (IOException exception) {
          if (get){
            print38E(secondHalf);
          }
          else{
            System.err.println("998 Input error while reading commands, terminating.");
          }
        } catch (PatternSyntaxException exception){   //exception for using split
            printFFFE();
        }  catch (IndexOutOfBoundsException exception){   //exception for using substring
            printFFFE();
        } catch (Exception e){
            printFFFF("Reason unknown");
        }
    }

    public static int casesToNum(String userIn){
      if (userIn.startsWith("user")) return 1;
      if (userIn.startsWith("pw")) return 2;
      if (userIn.startsWith("quit")) return 3;
      if (userIn.startsWith("get")) return 4;
      if (userIn.startsWith("features")) return 5;
      if (userIn.startsWith("cd")) return 6;
      if (userIn.startsWith("dir")) return 7;
      if (userIn.startsWith("#")) return 8;
      if (userIn.trim().isEmpty()) return 9;
      return 0;
    }

    public static String getHostPASV(String res){
        String[] nums = parsePASV(res);
        String ip = nums[1]+ "." + nums[2]+"."+ nums[3] + "." + nums[4];
        return ip;
    }

    public static int getPortPASV(String res){
        String[] nums = parsePASV(res);
        int port = Integer.parseInt(nums[5]) * 256 + Integer.parseInt(nums[6]);
        return port;
    }

    public static String[] parsePASV(String res){
        Socket socket = null;
        String[] nums = res.replaceAll("[^0-9]+", " ").split("\\s+");

        return nums;
    }

    public static int getNumberOfParam(String input){
        int count = 0;
        int indexOfFirstSpace = input.indexOf(" ");
        int indexOfSpace = indexOfFirstSpace;
        while(indexOfFirstSpace != -1 && indexOfSpace != -1){
            count++;
            indexOfSpace = input.indexOf(" ", indexOfSpace + 1);
        }

        return count;
    }

    public static void print001() {
        System.err.println("0x001 Invalid command.");
    }

    public static void print002() {
        System.err.println("0x002 Incorrect number of arguments.");
    }

    public static void print38E(String name) {
        System.err.println("0x38E Access to local file" + name + " denied.");
    }

    public static void printFFFC(String host, int port) {
        System.err.println("0xFFFC Control connection to " + host +" on port " +
                port + " failed to open.");
    }

    public static void printFFFD() {
        System.err.println("OxFFFD Control connection I/O error, closing control connection.");
    }

    public static void print3A2(String host, int port) {
        System.err.println("0x3A2 Data transfer connection to "+ host +
                " on port " + port + " failed to open.");
    }

    public static void print3A7() {
        System.err.println("0x3A7 Data transfer connection I/O error, closing data connection.");
    }

    public static void printFFFE() {
        System.err.println("0xFFFE Input error while reading commands, terminating.");
    }

    public static void printFFFF(String text) {
        System.err.println("0xFFFF Processing error." + text);
    }

    public static void debug(String text){
        System.out.println(text);
    }

}

