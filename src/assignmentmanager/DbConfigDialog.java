package assignmentmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern modal dialog for configuring MySQL database parameters.
 */
public class DbConfigDialog extends JDialog {

    private final JTextField txtHost;
    private final JTextField txtPort;
    private final JTextField txtDb;
    private final JTextField txtUser;
    private final JPasswordField txtPass;
    private final JLabel lblStatus;

    private boolean saved = false;

    public DbConfigDialog(Frame owner) {
        super(owner, "Database Connection Settings", true);
        setSize(480, 420);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        DatabaseManager db = DatabaseManager.getInstance();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(new Color(33, 41, 54));
        root.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("MySQL Database Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);

        JLabel subtitle = new JLabel("Configure your local or remote MySQL connection.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(170, 185, 200));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(15));

        // Form Fields
        root.add(createLabel("Host / Server Address"));
        txtHost = createField(db.getHost());
        root.add(txtHost);
        root.add(Box.createVerticalStrut(10));

        root.add(createLabel("Port (Default: 3306)"));
        txtPort = createField(String.valueOf(db.getPort()));
        root.add(txtPort);
        root.add(Box.createVerticalStrut(10));

        root.add(createLabel("Database Name"));
        txtDb = createField(db.getDbName());
        root.add(txtDb);
        root.add(Box.createVerticalStrut(10));

        root.add(createLabel("Username"));
        txtUser = createField(db.getUsername());
        root.add(txtUser);
        root.add(Box.createVerticalStrut(10));

        root.add(createLabel("Password"));
        txtPass = new JPasswordField(db.getPassword());
        styleField(txtPass);
        root.add(txtPass);
        root.add(Box.createVerticalStrut(12));

        lblStatus = new JLabel(db.isConnected() ? "Status: Connected to MySQL" : "Status: Not Connected");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(db.isConnected() ? new Color(46, 204, 113) : new Color(241, 196, 15));
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lblStatus);

        add(root, BorderLayout.CENTER);

        // Buttons Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnBar.setBackground(new Color(25, 32, 43));

        JButton btnTest = createButton("Test Connection", new Color(52, 152, 219), Color.WHITE);
        JButton btnSave = createButton("Save & Connect", new Color(46, 204, 113), Color.WHITE);
        JButton btnCancel = createButton("Close", new Color(100, 110, 125), Color.WHITE);

        btnTest.addActionListener(e -> {
            lblStatus.setText("Testing connection...");
            lblStatus.setForeground(Color.WHITE);
            SwingUtilities.invokeLater(() -> {
                try {
                    int port = Integer.parseInt(txtPort.getText().trim());
                    String result = db.testConnectionDetails(
                            txtHost.getText().trim(),
                            port,
                            txtDb.getText().trim(),
                            txtUser.getText().trim(),
                            new String(txtPass.getPassword())
                    );
                    if (result.startsWith("SUCCESS")) {
                        lblStatus.setText(result);
                        lblStatus.setForeground(new Color(46, 204, 113));
                        JOptionPane.showMessageDialog(this, result, "Connection Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        lblStatus.setText("Connection failed.");
                        lblStatus.setForeground(new Color(231, 76, 60));
                        JOptionPane.showMessageDialog(this, result, "Connection Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Port must be a valid number (e.g. 3306).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                }
            });
        });

        btnSave.addActionListener(e -> {
            try {
                int port = Integer.parseInt(txtPort.getText().trim());
                String host = txtHost.getText().trim();
                String dbName = txtDb.getText().trim();
                String user = txtUser.getText().trim();
                String pass = new String(txtPass.getPassword());

                db.saveConfig(host, port, dbName, user, pass);
                boolean success = db.connect();
                saved = true;

                if (success) {
                    JOptionPane.showMessageDialog(this, "Successfully connected to MySQL database: " + dbName, "Connected", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "Saved configuration, but could not connect:\n" + db.getLastError()
                            + "\n\nDo you want to close anyway and use In-Memory mode?",
                            "Connection Notice",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        dispose();
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Port must be a number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dispose());

        btnBar.add(btnTest);
        btnBar.add(btnSave);
        btnBar.add(btnCancel);
        add(btnBar, BorderLayout.SOUTH);
    }

    public boolean isSaved() {
        return saved;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(200, 210, 225));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField createField(String text) {
        JTextField tf = new JTextField(text);
        styleField(tf);
        return tf;
    }

    private void styleField(JTextField tf) {
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tf.setPreferredSize(new Dimension(200, 30));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(80, 95, 115), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
        tf.setBackground(new Color(45, 55, 70));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
