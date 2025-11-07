package ui;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class RegistrationPage extends JFrame implements ActionListener {
    JTextField usernameField;
    JPasswordField passwordField;
    JComboBox<String> roleBox;
    JButton registerButton;

    public RegistrationPage() {
        setTitle("User Registration");
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        roleBox = new JComboBox<>(new String[]{"Candidate", "Employer"});
        registerButton = new JButton("Register");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);
        panel.add(new JLabel(""));
        panel.add(registerButton);

        add(panel);
        registerButton.addActionListener(this);
        getRootPane().setDefaultButton(registerButton);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());
            String role = roleBox.getSelectedItem().toString();
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password are required.");
                return;
            }
            // Check duplicate username
            PreparedStatement check = conn.prepareStatement("SELECT 1 FROM users WHERE username=?");
            check.setString(1, user);
            ResultSet crs = check.executeQuery();
            if (crs.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists. Choose another.");
                return;
            }
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
            ps.setString(1, user);
            ps.setString(2, pass);
            ps.setString(3, role);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Registration successful!");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}