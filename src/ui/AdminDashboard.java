package ui;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class AdminDashboard extends JFrame {
    private JTable userTable;
    private DefaultTableModel model;
    private JButton toggleButton, refreshButton, backButton, deleteButton;
    private JComboBox<String> roleFilter, statusFilter;

    public AdminDashboard() {
        setTitle("Admin Panel");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(244, 245, 250));
        setLayout(new BorderLayout(10, 10));

        JLabel header = new JLabel("User Management Panel", SwingConstants.CENTER);
        header.setOpaque(true);
        header.setBackground(new Color(42, 90, 217));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(header, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Username", "Role", "Status"}, 0);
        userTable = new JTable(model);
        userTable.setRowHeight(28);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // Colorize status column
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && column == 3) {
                    String status = String.valueOf(table.getValueAt(row, 3));
                    if ("Active".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(209, 250, 229));
                        c.setForeground(new Color(22, 101, 52));
                    } else if ("Inactive".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(254, 226, 226));
                        c.setForeground(new Color(153, 27, 27));
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        toggleButton = new JButton("Toggle Status");
        refreshButton = new JButton("Refresh");
        deleteButton = new JButton("Delete User");
        backButton = new JButton("← Logout");
        roleFilter = new JComboBox<>(new String[]{"All","Admin","Employer","Candidate"});
        statusFilter = new JComboBox<>(new String[]{"All","Active","Inactive"});
        bottom.add(new JLabel("Role:"));
        bottom.add(roleFilter);
        bottom.add(new JLabel("Status:"));
        bottom.add(statusFilter);
        bottom.add(toggleButton);
        bottom.add(refreshButton);
        bottom.add(deleteButton);
        bottom.add(backButton);
        add(bottom, BorderLayout.SOUTH);

        toggleButton.addActionListener(e -> toggleStatus());
        refreshButton.addActionListener(e -> loadUsers());
        roleFilter.addActionListener(e -> loadUsers());
        statusFilter.addActionListener(e -> loadUsers());
        deleteButton.addActionListener(e -> deleteUser());
        backButton.addActionListener(e -> { dispose(); new LoginPage(); });

        loadUsers();
        setVisible(true);
    }

    private void loadUsers() {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             ) {
            StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
            boolean filterRole = roleFilter != null && roleFilter.getSelectedItem() != null && !"All".equals(roleFilter.getSelectedItem().toString());
            boolean filterStatus = statusFilter != null && statusFilter.getSelectedItem() != null && !"All".equals(statusFilter.getSelectedItem().toString());
            if (filterRole) sql.append(" AND role=?");
            if (filterStatus) sql.append(" AND status=?");
            sql.append(" ORDER BY id DESC");
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (filterRole) ps.setString(idx++, roleFilter.getSelectedItem().toString());
            if (filterStatus) ps.setString(idx++, statusFilter.getSelectedItem().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void toggleStatus() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user first!");
            return;
        }
        String username = (String) model.getValueAt(row, 1);
        String current = (String) model.getValueAt(row, 3);
        String newStatus = current.equals("Active") ? "Inactive" : "Active";
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET status=? WHERE username=?");
            ps.setString(1, newStatus);
            ps.setString(2, username);
            ps.executeUpdate();
            loadUsers();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user first!");
            return;
        }
        String username = (String) model.getValueAt(row, 1);
        String role = (String) model.getValueAt(row, 2);
        if ("Admin".equals(role)) {
            JOptionPane.showMessageDialog(this, "Cannot delete Admin users.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete user '" + username + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username=?");
            ps.setString(1, username);
            ps.executeUpdate();
            loadUsers();
        } catch (Exception e) { e.printStackTrace(); }
    }
}