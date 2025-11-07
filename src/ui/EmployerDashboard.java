package ui;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class EmployerDashboard extends JFrame implements ActionListener {
    private JTextField titleField;
    private JTextArea descArea;
    private JButton postButton, refreshButton, backButton, deleteButton, updateButton, searchButton, acceptButton, rejectButton;
    private JTextField searchField;
    private JTable jobTable, applicantsTable;
    private DefaultTableModel model, appModel;
    private String username;
    private String currentFilter = "";

    public EmployerDashboard(String username) {
        this.username = username;

        setTitle("Employer Dashboard");
        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(244, 245, 250));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel header = new JLabel("Employer Dashboard", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setOpaque(true);
        header.setBackground(new Color(42, 90, 217));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(900, 60));
        backButton = new JButton("← Logout");
        headerPanel.add(backButton, BorderLayout.WEST);
        headerPanel.add(header, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        add(tabs, BorderLayout.CENTER);

        // ============= TAB 1: Post Job =============
        JPanel postPanel = new JPanel(new BorderLayout(10, 10));
        postPanel.setBackground(new Color(244, 245, 250));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        formPanel.setBackground(new Color(244, 245, 250));

        titleField = new JTextField();
        descArea = new JTextArea(3, 20);
        postButton = new JButton("Post Job");
        refreshButton = new JButton("Refresh");
        deleteButton = new JButton("Delete Selected");
        updateButton = new JButton("Update Selected");
        searchField = new JTextField();
        searchButton = new JButton("Search");

        formPanel.add(new JLabel("Job Title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(new JScrollPane(descArea));
        formPanel.add(postButton);
        formPanel.add(refreshButton);
        postPanel.add(formPanel, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Title", "Description"}, 0);
        jobTable = new JTable(model);
        jobTable.setRowHeight(28);
        jobTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        jobTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        postPanel.add(new JScrollPane(jobTable), BorderLayout.CENTER);
        JPanel actions = new JPanel();
        searchField.setPreferredSize(new Dimension(220, 28));
        actions.add(new JLabel("Filter:"));
        actions.add(searchField);
        actions.add(searchButton);
        actions.add(updateButton);
        actions.add(deleteButton);
        postPanel.add(actions, BorderLayout.SOUTH);
        tabs.addTab("Post & Manage Jobs", postPanel);

        // ============= TAB 2: View Applicants =============
        JPanel applicantsPanel = new JPanel(new BorderLayout(10, 10));
        appModel = new DefaultTableModel(new Object[]{"App ID", "Job Title", "Candidate", "Status"}, 0);
        applicantsTable = new JTable(appModel);
        applicantsTable.setRowHeight(28);
        applicantsTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        applicantsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        // Colorize status column
        applicantsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setForeground(Color.BLACK);
                if (!isSelected && column == 3) {
                    String status = String.valueOf(table.getValueAt(row, 3));
                    if ("Accepted".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(209, 250, 229));
                    } else if ("Rejected".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(254, 226, 226));
                    } else if ("Under Review".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(254, 249, 195));
                    } else if ("Withdrawn".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(243, 244, 246));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });
        applicantsPanel.add(new JScrollPane(applicantsTable), BorderLayout.CENTER);
        JPanel appActions = new JPanel();
        acceptButton = new JButton("Accept");
        rejectButton = new JButton("Reject");
        appActions.add(acceptButton);
        appActions.add(rejectButton);
        applicantsPanel.add(appActions, BorderLayout.SOUTH);
        tabs.addTab("View Applicants", applicantsPanel);

        // Button actions
        postButton.addActionListener(this);
        refreshButton.addActionListener(this);
        deleteButton.addActionListener(this);
        updateButton.addActionListener(this);
        searchButton.addActionListener(this);
        acceptButton.addActionListener(this);
        rejectButton.addActionListener(this);
        backButton.addActionListener(e -> {
            dispose();
            new LoginPage();
        });

        // Load data
        loadJobs();
        loadApplicants();

        setVisible(true);
    }

    private void loadJobs() {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String base = "SELECT * FROM jobs WHERE employer=?";
            boolean hasFilter = currentFilter != null && !currentFilter.isEmpty();
            String sql = hasFilter ? base + " AND title LIKE ?" : base;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            if (hasFilter) {
                ps.setString(2, "%" + currentFilter + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadApplicants() {
        appModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT a.id AS app_id, j.title, a.candidate, a.status 
                FROM applications a
                JOIN jobs j ON a.job_id = j.id
                WHERE j.employer = ?
                ORDER BY j.title;
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                appModel.addRow(new Object[]{
                        rs.getInt("app_id"),
                        rs.getString("title"),
                        rs.getString("candidate"),
                        rs.getString("status")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == postButton) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Title is required.");
                    return;
                }
                String sql = "INSERT INTO jobs (title, description, employer) VALUES (?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, title);
                ps.setString(2, descArea.getText());
                ps.setString(3, username);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Job Posted Successfully!");
                titleField.setText("");
                descArea.setText("");
                loadJobs();
                loadApplicants();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == refreshButton) {
            loadJobs();
            loadApplicants();
        } else if (e.getSource() == searchButton) {
            currentFilter = searchField.getText().trim();
            loadJobs();
        } else if (e.getSource() == updateButton) {
            int row = jobTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a job to update.");
                return;
            }
            int jobId = (int) model.getValueAt(row, 0);
            String currTitle = (String) model.getValueAt(row, 1);
            String currDesc = (String) model.getValueAt(row, 2);
            String newTitle = JOptionPane.showInputDialog(this, "Edit Title", currTitle);
            if (newTitle == null) return; // cancelled
            newTitle = newTitle.trim();
            if (newTitle.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title cannot be empty.");
                return;
            }
            String newDesc = JOptionPane.showInputDialog(this, "Edit Description", currDesc);
            if (newDesc == null) newDesc = currDesc;
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE jobs SET title=?, description=? WHERE id=? AND employer=?");
                ps.setString(1, newTitle);
                ps.setString(2, newDesc);
                ps.setInt(3, jobId);
                ps.setString(4, username);
                ps.executeUpdate();
                loadJobs();
            } catch (Exception ex) { ex.printStackTrace(); }
        } else if (e.getSource() == acceptButton || e.getSource() == rejectButton) {
            int row = applicantsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select an application first.");
                return;
            }
            int appId = (int) appModel.getValueAt(row, 0);
            String newStatus = (e.getSource() == acceptButton) ? "Accepted" : "Rejected";
            int confirm = JOptionPane.showConfirmDialog(this, (newStatus.equals("Accepted") ? "Accept" : "Reject") + " selected application?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE applications a JOIN jobs j ON a.job_id=j.id SET a.status=? WHERE a.id=? AND j.employer=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, newStatus);
                ps.setInt(2, appId);
                ps.setString(3, username);
                ps.executeUpdate();
                loadApplicants();
            } catch (Exception ex) { ex.printStackTrace(); }
        } else if (e.getSource() == deleteButton) {
            int row = jobTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a job to delete.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete selected job?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            int jobId = (int) model.getValueAt(row, 0);
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM jobs WHERE id=? AND employer=?");
                ps.setInt(1, jobId);
                ps.setString(2, username);
                ps.executeUpdate();
                loadJobs();
                loadApplicants();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}