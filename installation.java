package backend;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class handlers {
    static String url = "jdbc:mysql://localhost:3306/";
    public static void runCommand(List<String> commands) {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    public static void registerBase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, "root", "")) {
            Statement stmt = conn.createStatement();

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS UFTSBase");
            stmt.executeUpdate("USE UFTSBase");

            String createTable = "CREATE TABLE IF NOT EXISTS logontable (" +
                    "username VARCHAR(20), " +
                    "password VARCHAR(40))";
            stmt.executeUpdate(createTable);

            System.out.println("Database and table created successfully.");
        }
    }

    public static void registerUser() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, "root", "")) {
            String query = "INSERT INTO logontable (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, installation.username);
                pstmt.setString(2, installation.hashedPassword);
                int result = pstmt.executeUpdate();
                if (result > 0) {
                    Runtime.getRuntime().exec("adduser " + installation.username);
                    Runtime.getRuntime().exec("mkdir " + installation.username);
                    Runtime.getRuntime().exec("echo " + installation.username + " | sudo tee -a /etc/vsftpd.userlist");
                    System.out.println("User added successfully.");
                } else {
                    System.out.println("Failed to add user.");
                }
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

public class installation {
    static String username, hashedPassword;
    public static void installator() throws InterruptedException, SQLException {
        System.out.println("WARNING: BetaRelease only supports clean install!");
        System.out.println("Reinstalling is not supported!");

        Scanner sc = new Scanner(System.in);
        System.out.println("Setup will install next packages: MySQL, Apache, PHP, VSFTPD and configure it");

        Thread.sleep(2000);


        // Resolving dependencies and configuring firewall
        List<List<String>> bootstrap = Arrays.asList(
                List.of("clear"),
                Arrays.asList("apt", "install", "-y", "mysql-server", "apache2", "php", "libapache2-mod-php", "php-mysql", "vsftpd"),
                Arrays.asList("ufw", "allow", "\"Apache Full\""),
                Arrays.asList("ufw", "enable"),
                Arrays.asList("systemctl", "enable", "mysql", "apache2", "vsftpd"),
                Arrays.asList("cp", "/etc/vsftpd.conf", "/etc/vsftpd.conf.orig"),
                Arrays.asList("touch", "/etc/vsftpd.userlist"),
                Arrays.asList("cp", "vsftpd.conf", "/etc/vsftpd.conf")
        );

        for (List<String> command : bootstrap) {
            handlers.runCommand(command);
        }

        handlers.registerBase();

        // Configuring MySQL
        System.out.println("\nCAUTION: BETA VERSION CAN'T USE PASSWORD PROTECTED MYSQL! USE UFTS BETA ONLY FOR PERSONAL USE!");
        Thread.sleep(2000);

        System.out.println("Enter preffered UFTS username:");
        username = sc.nextLine();
        System.out.println("Enter preffered UTFS password:");
        String password = sc.nextLine();
        hashedPassword = handlers.hashPassword(password);

        handlers.registerUser();
    }

    public static void main(String[] args) throws InterruptedException, SQLException, IOException {
        Scanner sc = new Scanner(System.in);

        System.out.println("UFTS Maintenance terminal");
        System.out.println("Developer Release Rev.D");

        Thread.sleep(2000);

        boolean terminated = false;
        while (!terminated) {
            System.out.print("maintenance :> ");
            String input = sc.nextLine().toLowerCase();
            switch (input) {
                case "help":
                    System.out.println("Help - show all available commands");
                    System.out.println("Install - start installation progress");
                    System.out.println("Exit - close maintenance console");
                case "install":
                    installator();
                    Runtime.getRuntime().exec("clear");
                    System.out.println("All set!");
                case "exit":
                    terminated = true;
                default:
                    System.out.println("Unknown command. Type 'help' for list of commands!");
            }
        }
        System.out.println("Bye");
        System.exit(0);

    }
}
