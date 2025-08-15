import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;

public class EmployeeDB extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private String loggedInUser;
    private String loggedInRole;

    public EmployeeDB() {
        setTitle("Login");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        Font font = new Font("Arial", Font.PLAIN, 18);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(font);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(usernameLabel, gbc);

        usernameField = new JTextField(20);
        usernameField.setFont(font);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(font);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(passwordField, gbc);

        loginButton = new JButton("Login");
        loginButton.setFont(font);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(loginButton, gbc);

        add(panel);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (authenticate(username, password)) {
                dispose();
                if ("ADMIN".equalsIgnoreCase(loggedInRole)) {
                    showAdminPanel();
                } else {
                    showEmployeePanel();
                }
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password");
            }
        });
    }

    private boolean authenticate(String username, String password) {
        boolean isAuthenticated = false;
        String url = "jdbc:postgresql://localhost:5432/employeedb";
        String dbUser = "nova";
        String dbPassword = "password";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            String sql = "SELECT username, role FROM employees WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    isAuthenticated = true;
                    loggedInUser = rs.getString("username");
                    loggedInRole = rs.getString("role");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return isAuthenticated;
    }

    private void showAdminPanel() {
        JFrame frame = new JFrame("Admin Dashboard");
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] columns = {"ID", "Name", "Position", "Salary", "Username", "Password", "Role"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        loadEmployeeData(model);

        JButton exportBtn = new JButton("Export to CSV");
        exportBtn.addActionListener(e -> exportToCSV(model));

        JButton saveBtn = new JButton("Save Changes");
        saveBtn.addActionListener(e -> saveChangesToDB(model));

        JPanel panel = new JPanel();
        panel.add(exportBtn);
        panel.add(saveBtn);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void showEmployeePanel() {
        JFrame frame = new JFrame("Employee Dashboard");
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] columns = {"Name", "Position", "Salary"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only
            }
        };
        loadEmployeeData(model);

        JTable table = new JTable(model);

        JButton exportBtn = new JButton("Export to CSV");
        exportBtn.addActionListener(e -> exportToCSV(model));

        JPanel panel = new JPanel();
        panel.add(exportBtn);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadEmployeeData(DefaultTableModel model) {
        String url = "jdbc:postgresql://localhost:5432/employeedb";
        String dbUser = "nova";
        String dbPassword = "password";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, position, salary, username, password, role FROM employees")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("position"),
                        rs.getDouble("salary"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSingleEmployeeData(DefaultTableModel model) {
        String url = "jdbc:postgresql://localhost:5432/employeedb";
        String dbUser = "nova";
        String dbPassword = "password";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement("SELECT name, position, salary FROM employees WHERE username = ?")) {
            stmt.setString(1, loggedInUser);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("position"),
                        rs.getDouble("salary")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveChangesToDB(DefaultTableModel model) {
        String url = "jdbc:postgresql://localhost:5432/employeedb";
        String dbUser = "nova";
        String dbPassword = "password";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            for (int i = 0; i < model.getRowCount(); i++) {
                int id = (int) model.getValueAt(i, 0);
                String name = (String) model.getValueAt(i, 1);
                String position = (String) model.getValueAt(i, 2);
                double salary = Double.parseDouble(model.getValueAt(i, 3).toString());
                String username = (String) model.getValueAt(i, 4);
                String password = (String) model.getValueAt(i, 5);
                String role = (String) model.getValueAt(i, 6);

                String sql = "UPDATE employees SET name=?, position=?, salary=?, username=?, password=?, role=? WHERE id=?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, position);
                    stmt.setDouble(3, salary);
                    stmt.setString(4, username);
                    stmt.setString(5, password);
                    stmt.setString(6, role);
                    stmt.setInt(7, id);
                    stmt.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(null, "Changes saved successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void exportToCSV(DefaultTableModel model) {
        try (FileWriter writer = new FileWriter("employees.csv")) {
            for (int i = 0; i < model.getColumnCount(); i++) {
                writer.write(model.getColumnName(i) + (i < model.getColumnCount() - 1 ? "," : ""));
            }
            writer.write("\n");

            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    writer.write(model.getValueAt(i, j).toString() + (j < model.getColumnCount() - 1 ? "," : ""));
                }
                writer.write("\n");
            }
            JOptionPane.showMessageDialog(null, "Exported to employees.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmployeeDB frame = new EmployeeDB();
            frame.setVisible(true);
        });
    }
}
