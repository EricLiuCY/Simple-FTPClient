import java.net.InetAddress;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.System;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class test {

    public static void main(String[] args) {
        // Prints "Hello, World" to the terminal window.
        String s = "227 Entering Passive Mode (142,103,6,49,97,232).";
        System.out.println(s);
        int numbers = s.replaceAll("[^0-9]+", " ").split("\\s+").length;

        System.out.println(numbers);



    }

}
