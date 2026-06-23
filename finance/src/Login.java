import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import javax.swing.*;

public class Login {
    private static final String LOGIN_FILE = "login.dat";

    public static void main(String[] args) {
        // Initialize database first
        DBHelper.initializeDatabase();
        
        SwingUtilities.invokeLater(() -> {
            if(isLoginValid()) {
                String savedEmail = getSavedEmail();
                Main.launchMainApp(savedEmail);
            } else {
                createLoginGUI();
            }
        });
    }

    private static boolean isLoginValid() {
        File file = new File(LOGIN_FILE);
        if(!file.exists()) return false;

        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String[] parts = br.readLine().split(",");
            LocalDate lastLogin = LocalDate.parse(parts[1]);
            LocalDate now = LocalDate.now();

            // Check 1 month validity
            return lastLogin.plusMonths(1).isAfter(now);
        } catch(Exception e){
            return false;
        }
    }

    private static String getSavedEmail() {
        try(BufferedReader br = new BufferedReader(new FileReader(LOGIN_FILE))){
            return br.readLine().split(",")[0];
        } catch(Exception e){ 
            return "guest@example.com"; 
        }
    }

    private static void saveLogin(String email) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(LOGIN_FILE))){
            bw.write(email + "," + LocalDate.now());
        } catch(Exception e){ 
            e.printStackTrace(); 
        }
    }

    private static void createLoginGUI() {
        JFrame frame = new JFrame("💰 Personal Finance Tracker - Login");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(6, 2, 10, 10));
        frame.setLocationRelativeTo(null);

        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JButton guestBtn = new JButton("Continue as Guest");
        JLabel infoLabel = new JLabel("");
        infoLabel.setForeground(Color.RED);

        frame.add(emailLabel); frame.add(emailField);
        frame.add(passwordLabel); frame.add(passwordField);
        frame.add(new JLabel("")); frame.add(new JLabel(""));
        frame.add(loginBtn); frame.add(registerBtn);
        frame.add(guestBtn); frame.add(new JLabel(""));
        frame.add(infoLabel);

        loginBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if(email.isEmpty() || password.isEmpty()) { 
                infoLabel.setText("Enter both email and password!"); 
                return; 
            }

            if(DBHelper.loginUser(email, password)){
                saveLogin(email);
                frame.dispose();
                Main.launchMainApp(email);
            } else {
                infoLabel.setText("❌ Invalid email or password!");
            }
        });

        registerBtn.addActionListener(e -> openRegisterWindow(frame));
        guestBtn.addActionListener(e -> {
            frame.dispose();
            Main.launchMainApp("guest@example.com");
        });

        frame.setVisible(true);
    }

    private static void openRegisterWindow(JFrame parent){
        JFrame registerFrame = new JFrame("Register");
        registerFrame.setSize(400, 300);
        registerFrame.setLayout(new GridLayout(6, 2, 10, 10));
        registerFrame.setLocationRelativeTo(parent);

        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JButton registerBtn = new JButton("Register");
        JLabel infoLabel = new JLabel("");
        infoLabel.setForeground(Color.RED);

        registerFrame.add(new JLabel("Name:")); registerFrame.add(nameField);
        registerFrame.add(new JLabel("Email:")); registerFrame.add(emailField);
        registerFrame.add(new JLabel("Phone:")); registerFrame.add(phoneField);
        registerFrame.add(new JLabel("Password:")); registerFrame.add(passwordField);
        registerFrame.add(new JLabel("Confirm Password:")); registerFrame.add(confirmPasswordField);
        registerFrame.add(registerBtn); registerFrame.add(infoLabel);

        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if(name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                infoLabel.setText("Please fill all required fields!");
                return;
            }

            if(!password.equals(confirmPassword)) {
                infoLabel.setText("Passwords don't match!");
                return;
            }

            if(DBHelper.registerUser(email, name, phone, password)) {
                JOptionPane.showMessageDialog(registerFrame, "✅ Registration successful!");
                registerFrame.dispose();
            }
        });

        registerFrame.setVisible(true);
    }
}