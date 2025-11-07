package ui;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CandidateDashboard extends JFrame implements ActionListener {
    private JTable jobTable, appsTable;
    private JButton applyButton, refreshButton, backButton, withdrawButton;
    private DefaultTableModel model, appsModel;
    private String username;
    private JLabel selectionLabel;
    private JCheckBox acceptedOnly;

    public CandidateDashboard(String username) {
        this.username = username;

        setTitle("Candidate Dashboard");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(238, 242, 255));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // ----- JOB LIST TAB -----
        JPanel jobPanel = new JPanel(new BorderLayout(10, 10));
        model = new DefaultTableModel(new Object[]{"ID", "Title", "Description"}, 0);
        jobTable = new JTable(model);
        jobTable.setRowHeight(28);
        jobPanel.add(new JScrollPane(jobTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        applyButton = new JButton("Apply to Selected");
        refreshButton = new JButton("Refresh");
        bottom.add(applyButton);
        bottom.add(refreshButton);
        jobPanel.add(bottom, BorderLayout.SOUTH);
        tabs.addTab("Available Jobs", jobPanel);

        // ----- MY APPLICATIONS TAB -----
        JPanel appsPanel = new JPanel(new BorderLayout(10, 10));
        appsModel = new DefaultTableModel(new Object[]{"App ID", "Job Title", "Status"}, 0);
        appsTable = new JTable(appsModel);
        appsTable.setRowHeight(28);
        // Colorize status column
        appsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setForeground(Color.BLACK);
                if (!isSelected && column == 2) {
                    String status = String.valueOf(table.getValueAt(row, 2));
                    if ("Accepted".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(209, 250, 229)); // green tint
                    } else if ("Rejected".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(254, 226, 226)); // red tint
                    } else if ("Under Review".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(254, 249, 195)); // yellow tint
                    } else if ("Withdrawn".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(243, 244, 246)); // gray tint
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });
        appsPanel.add(new JScrollPane(appsTable), BorderLayout.CENTER);
        JPanel appsBottom = new JPanel(new BorderLayout());
        JPanel appsBottomLeft = new JPanel();
        withdrawButton = new JButton("Withdraw Selected");
        acceptedOnly = new JCheckBox("Accepted Only");
        appsBottomLeft.add(withdrawButton);
        appsBottomLeft.add(acceptedOnly);
        appsBottom.add(appsBottomLeft, BorderLayout.WEST);
        selectionLabel = new JLabel("Selection: (no row selected)");
        selectionLabel.setOpaque(true);
        selectionLabel.setBackground(new Color(236, 253, 245));
        selectionLabel.setForeground(new Color(22, 101, 52));
        selectionLabel.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        appsBottom.add(selectionLabel, BorderLayout.EAST);
        appsPanel.add(appsBottom, BorderLayout.SOUTH);
        tabs.addTab("My Applications", appsPanel);

        // ----- HEADER & BACK -----
        JPanel top = new JPanel(new BorderLayout());
        JLabel header = new JLabel("Welcome, " + username, SwingConstants.CENTER);
        header.setOpaque(true);
        header.setBackground(new Color(59, 130, 246));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        backButton = new JButton("← Logout");
        top.add(backButton, BorderLayout.WEST);
        top.add(header, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        applyButton.addActionListener(this);
        refreshButton.addActionListener(this);
        withdrawButton.addActionListener(this);
        acceptedOnly.addActionListener(this);
        backButton.addActionListener(e -> { dispose(); new LoginPage(); });

        // Update selection label on row change
        appsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) updateSelectionLabel();
            }
        });

        loadJobs();
        loadApplications();
        setVisible(true);
    }

    private void loadJobs() {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM jobs")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("title"), rs.getString("description")});
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadApplications() {
        appsModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String base = "SELECT a.id, j.title, a.status FROM applications a JOIN jobs j ON a.job_id=j.id WHERE a.candidate=?";
            String sql = acceptedOnly != null && acceptedOnly.isSelected() ? base + " AND a.status='Accepted'" : base;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                appsModel.addRow(new Object[]{rs.getInt("id"), rs.getString("title"), rs.getString("status")});
            }
        } catch (Exception e) { e.printStackTrace(); }
        updateSelectionLabel();
    }

    private void updateSelectionLabel() {
        int row = appsTable.getSelectedRow();
        if (row == -1) {
            selectionLabel.setText("Selection: (no row selected)");
            selectionLabel.setBackground(new Color(236, 253, 245));
            selectionLabel.setForeground(new Color(22, 101, 52));
            return;
        }
        String title = String.valueOf(appsModel.getValueAt(row, 1));
        String status = String.valueOf(appsModel.getValueAt(row, 2));
        selectionLabel.setText("Selection: " + title + " — " + status);
        if ("Accepted".equalsIgnoreCase(status)) {
            selectionLabel.setBackground(new Color(209, 250, 229));
            selectionLabel.setForeground(new Color(22, 101, 52));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            selectionLabel.setBackground(new Color(254, 226, 226));
            selectionLabel.setForeground(new Color(153, 27, 27));
        } else if ("Under Review".equalsIgnoreCase(status)) {
            selectionLabel.setBackground(new Color(254, 249, 195));
            selectionLabel.setForeground(new Color(120, 53, 15));
        } else if ("Withdrawn".equalsIgnoreCase(status)) {
            selectionLabel.setBackground(new Color(243, 244, 246));
            selectionLabel.setForeground(new Color(75, 85, 99));
        } else {
            selectionLabel.setBackground(new Color(219, 234, 254));
            selectionLabel.setForeground(new Color(30, 64, 175));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == applyButton) {
            int row = jobTable.getSelectedRow();
            if (row == -1) return;
            int jobId = (int) model.getValueAt(row, 0);
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO applications (job_id, candidate, status) VALUES (?, ?, 'Applied')";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, jobId);
                ps.setString(2, username);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Application Submitted!");
                loadApplications();
            } catch (Exception ex) { ex.printStackTrace(); }
        } else if (e.getSource() == refreshButton) {
            loadJobs();
            loadApplications();
        } else if (e.getSource() == acceptedOnly) {
            loadApplications();
        } else if (e.getSource() == withdrawButton) {
            int row = appsTable.getSelectedRow();
            if (row == -1) return;
            int appId = (int) appsModel.getValueAt(row, 0);
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE applications SET status='Withdrawn' WHERE id=? AND candidate=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, appId);
                ps.setString(2, username);
                ps.executeUpdate();
                loadApplications();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}