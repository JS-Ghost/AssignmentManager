package assignmentmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

/**
 * Student Assignment Manager
 * Features:
 * - Persistent MySQL connectivity with auto-fallback to In-Memory mode
 * - Full CRUD: Add, Update/Edit, Delete, Search, Undo (Pop), Sort
 * - Real-time table search & filter (by ID, Course, Title, Status)
 * - Interactive row selection with autofill for quick editing
 * - Double-click / Right-click context menu to toggle status (Pending/Completed)
 * - Statistics dashboard (Total, Pending, Completed, DB Status pill)
 * - CSV Export functionality
 * - Fixes all previous index mapping, sorting, and encoding glitches
 */
public class AssignmentManager extends JFrame {

    // --- Data Storage ---
    private final List<Assignment> assignmentList = new ArrayList<>();
    private final Stack<Assignment> undoStack = new Stack<>();
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // --- Table Model & Sorter ---
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> rowSorter;

    // --- UI Components ---
    private JTextField txtId, txtCode, txtTitle, txtDueDate, txtSearch;
    private JComboBox<String> cmbStatus;
    private JButton btnAdd, btnUpdate, btnCancelEdit;
    private JLabel lblStatus, lblStatTotal, lblStatPending, lblStatCompleted, lblDbPill;

    // --- State ---
    private Integer editingAssignmentId = null;

    // --- Colors & Fonts ---
    private static final Color SIDEBAR_BG = new Color(30, 38, 50);
    private static final Color MAIN_BG = new Color(243, 245, 249);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    public AssignmentManager() {
        setTitle("Assignment Manager");
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Apply Look and Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 1. Top Dashboard & Connection Status Bar
        JPanel topBar = createTopDashboard();
        add(topBar, BorderLayout.NORTH);

        // 2. Sidebar Panel (Inputs & Action Controls)
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // 3. Main Center Content (Search bar + Table)
        String[] columnNames = {"Assignment ID", "Course Code", "Title", "Status", "Due Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        styleTable(table);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(MAIN_BG);
        mainPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Search Bar Panel
        JPanel searchBarPanel = createSearchBar();
        mainPanel.add(searchBarPanel, BorderLayout.NORTH);

        // ScrollPane with Table
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 225)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(MAIN_BG);
        bottomBar.setBorder(new EmptyBorder(5, 5, 5, 5));

        lblStatus = new JLabel("Initializing system...");
        lblStatus.setFont(new Font("Consolas", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(90, 100, 115));
        bottomBar.add(lblStatus, BorderLayout.WEST);

        JLabel lblHint = new JLabel("\uD83D\uDCA1 Tip: Double-click a row to toggle status (Pending/Completed)");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(new Color(130, 140, 155));
        bottomBar.add(lblHint, BorderLayout.EAST);

        mainPanel.add(bottomBar, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        // 4. Attach Event Listeners
        setupEventListeners();

        // 5. Connect to Database & Load Initial Data
        initDatabaseConnection();
    }

    // =========================================================================
    // UI Creation Helpers
    // =========================================================================

    private JPanel createTopDashboard() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(22, 28, 38));
        bar.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Left: Branding
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("\uD83D\uDCDD ASSIGNMENT MANAGER");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        left.add(title);

        // Center Stats
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        statsPanel.setOpaque(false);

        lblStatTotal = createStatLabel("Total: 0", new Color(175, 195, 220));
        lblStatPending = createStatLabel("Pending: 0", new Color(241, 196, 15));
        lblStatCompleted = createStatLabel("Completed: 0", new Color(46, 204, 113));

        statsPanel.add(lblStatTotal);
        statsPanel.add(lblStatPending);
        statsPanel.add(lblStatCompleted);

        // Right: DB Status Pill & Settings
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblDbPill = new JLabel("Checking DB...");
        lblDbPill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDbPill.setForeground(Color.WHITE);
        lblDbPill.setBorder(new EmptyBorder(4, 10, 4, 10));
        lblDbPill.setOpaque(true);
        lblDbPill.setBackground(new Color(60, 70, 85));

        JButton btnDbSettings = new JButton("\u2699\uFE0F DB Settings");
        btnDbSettings.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDbSettings.setForeground(Color.WHITE);
        btnDbSettings.setBackground(new Color(41, 128, 185));
        btnDbSettings.setFocusPainted(false);
        btnDbSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDbSettings.addActionListener(e -> openDbConfigDialog());

        right.add(lblDbPill);
        right.add(btnDbSettings);

        bar.add(left, BorderLayout.WEST);
        bar.add(statsPanel, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JLabel createStatLabel(String text, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADER);
        lbl.setForeground(fg);
        return lbl;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(330, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(15, 18, 15, 18));

        JLabel sectionTitle = new JLabel("ASSIGNMENT DETAILS");
        sectionTitle.setFont(FONT_HEADER);
        sectionTitle.setForeground(new Color(220, 225, 235));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sectionTitle);
        sidebar.add(Box.createVerticalStrut(15));

        // Input Fields
        sidebar.add(createInputLabel("Assignment ID (Positive Number)"));
        txtId = createModernTextField();
        sidebar.add(txtId);
        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(createInputLabel("Course Code (e.g., ICT3202)"));
        txtCode = createModernTextField();
        sidebar.add(txtCode);
        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(createInputLabel("Assignment Title"));
        txtTitle = createModernTextField();
        sidebar.add(txtTitle);
        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(createInputLabel("Status"));
        cmbStatus = new JComboBox<>(new String[]{"Pending", "Completed"});
        cmbStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cmbStatus.setFont(FONT_REGULAR);
        cmbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(cmbStatus);
        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(createInputLabel("Due Date (Optional, e.g., 2026-12-05)"));
        txtDueDate = createModernTextField();
        sidebar.add(txtDueDate);
        sidebar.add(Box.createVerticalStrut(18));

        // Primary Form Buttons (Add & Update)
        btnAdd = createActionButton("\u2795  Add Assignment", new Color(46, 204, 113), Color.WHITE);
        btnUpdate = createActionButton("\u270F\uFE0F  Update Selected", new Color(52, 152, 219), Color.WHITE);
        btnUpdate.setEnabled(false);

        btnCancelEdit = createActionButton("\u274E  Cancel / Clear Form", new Color(108, 122, 137), Color.WHITE);

        sidebar.add(btnAdd);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnUpdate);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnCancelEdit);
        sidebar.add(Box.createVerticalStrut(18));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(60, 75, 95));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(15));

        // Management Actions
        JButton btnDelete = createActionButton("\u274C  Delete Selected", new Color(231, 76, 60), Color.WHITE);
        JButton btnPop = createActionButton("\u21A9\uFE0F  Undo Last (Pop)", new Color(230, 126, 34), Color.WHITE);
        JButton btnSort = createActionButton("\u26A1  Sort by ID (1 at Top)", new Color(155, 89, 182), Color.WHITE);
        JButton btnRefresh = createActionButton("\uD83D\uDD04  Reload from DB", new Color(41, 128, 185), Color.WHITE);
        JButton btnExport = createActionButton("\uD83D\uDCE4  Export to CSV", new Color(22, 160, 133), Color.WHITE);
        JButton btnClear = createActionButton("\uD83D\uDDD1\uFE0F  Clear All", new Color(192, 57, 43), Color.WHITE);

        sidebar.add(btnDelete);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnPop);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnSort);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnRefresh);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnExport);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnClear);

        // Sidebar Action Listeners
        btnAdd.addActionListener(e -> addAssignment());
        btnUpdate.addActionListener(e -> updateSelectedAssignment());
        btnCancelEdit.addActionListener(e -> resetForm());
        btnDelete.addActionListener(e -> deleteSelectedAssignment());
        btnPop.addActionListener(e -> undoLastAssignment());
        btnSort.addActionListener(e -> sortAssignmentsById());
        btnRefresh.addActionListener(e -> refreshDataFromDb());
        btnExport.addActionListener(e -> exportToCsv());
        btnClear.addActionListener(e -> clearAllAssignments());

        return sidebar;
    }

    private JPanel createSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        JLabel lblSearch = new JLabel("\uD83D\uDD0D Real-Time Search:");
        lblSearch.setFont(FONT_BOLD);
        lblSearch.setForeground(new Color(60, 70, 85));

        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(250, 32));
        txtSearch.setFont(FONT_REGULAR);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 190, 205), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        // Real-time filter listener
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JButton btnClearSearch = new JButton("Clear Filter");
        btnClearSearch.setFont(FONT_REGULAR);
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.addActionListener(e -> txtSearch.setText(""));

        JPanel searchControls = new JPanel(new BorderLayout(5, 0));
        searchControls.setOpaque(false);
        searchControls.add(txtSearch, BorderLayout.CENTER);
        searchControls.add(btnClearSearch, BorderLayout.EAST);

        panel.add(lblSearch, BorderLayout.WEST);
        panel.add(searchControls, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // Core Logic & Database Integration
    // =========================================================================

    private void initDatabaseConnection() {
        boolean connected = dbManager.connect();
        updateDbStatusPill(connected);

        if (connected) {
            refreshDataFromDb();
            lblStatus.setText("Connected to MySQL. " + assignmentList.size() + " assignments loaded.");
        } else {
            lblStatus.setText("MySQL offline. Running in Local In-Memory Mode.");
            updateStats();
        }
    }

    private void updateDbStatusPill(boolean connected) {
        if (connected) {
            lblDbPill.setText("● MySQL: " + dbManager.getDbName());
            lblDbPill.setBackground(new Color(39, 174, 96));
            lblDbPill.setToolTipText("Connected to MySQL on port " + dbManager.getPort());
        } else {
            lblDbPill.setText("○ In-Memory Mode");
            lblDbPill.setBackground(new Color(211, 84, 0));
            lblDbPill.setToolTipText("MySQL not detected. Operating in local memory.");
        }
    }

    private void refreshDataFromDb() {
        if (dbManager.isConnected()) {
            List<Assignment> dbAssignments = dbManager.getAllAssignments();
            assignmentList.clear();
            assignmentList.addAll(dbAssignments);
            updateTable();
            updateStats();
            lblStatus.setText("Reloaded " + assignmentList.size() + " assignments from MySQL.");
        } else {
            updateTable();
            updateStats();
            lblStatus.setText("In-Memory Mode: Table refreshed.");
        }
    }

    private void addAssignment() {
        String idStr = txtId.getText().trim();
        String code = txtCode.getText().trim().toUpperCase();
        String title = txtTitle.getText().trim();
        String status = (String) cmbStatus.getSelectedItem();
        String dueDate = txtDueDate.getText().trim();

        if (idStr.isEmpty() || code.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID, Course Code, and Title are required fields!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
            if (id <= 0) {
                JOptionPane.showMessageDialog(this, "Assignment ID must be a positive number!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Assignment ID must be a valid integer!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Duplicate Check (Local + DB)
        for (Assignment a : assignmentList) {
            if (a.getId() == id) {
                JOptionPane.showMessageDialog(this, "Assignment ID #" + id + " already exists!", "Duplicate Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Assignment newAssign = new Assignment(id, code, title, status, dueDate);

        // Save to MySQL if connected
        if (dbManager.isConnected()) {
            try {
                dbManager.addAssignment(newAssign);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to insert into MySQL:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        assignmentList.add(newAssign);
        undoStack.push(newAssign);

        updateTable();
        updateStats();
        resetForm();
        lblStatus.setText("Successfully added Assignment #" + id + " (" + code + ")");
    }

    private void updateSelectedAssignment() {
        if (editingAssignmentId == null) {
            JOptionPane.showMessageDialog(this, "No assignment selected for update.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String code = txtCode.getText().trim().toUpperCase();
        String title = txtTitle.getText().trim();
        String status = (String) cmbStatus.getSelectedItem();
        String dueDate = txtDueDate.getText().trim();

        if (code.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Course Code and Title cannot be empty!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Assignment target = null;
        for (Assignment a : assignmentList) {
            if (a.getId() == editingAssignmentId) {
                target = a;
                break;
            }
        }

        if (target == null) {
            JOptionPane.showMessageDialog(this, "Assignment #" + editingAssignmentId + " not found in memory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        target.setCourseCode(code);
        target.setTitle(title);
        target.setStatus(status);
        target.setDueDate(dueDate);

        if (dbManager.isConnected()) {
            try {
                dbManager.updateAssignment(target);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to update in MySQL:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        updateTable();
        updateStats();
        resetForm();
        lblStatus.setText("Updated Assignment #" + editingAssignmentId);
    }

    private void deleteSelectedAssignment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an assignment from the table to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Fix glitch: read ID directly from table model, completely immune to sorting or filtering
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int targetId = (int) tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Assignment #" + targetId + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (dbManager.isConnected()) {
                try {
                    dbManager.deleteAssignment(targetId);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Failed to delete from MySQL:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            assignmentList.removeIf(a -> a.getId() == targetId);
            undoStack.removeIf(a -> a.getId() == targetId);

            updateTable();
            updateStats();
            if (editingAssignmentId != null && editingAssignmentId == targetId) {
                resetForm();
            }
            lblStatus.setText("Deleted Assignment #" + targetId);
        }
    }

    private void undoLastAssignment() {
        if (undoStack.isEmpty()) {
            lblStatus.setText("Undo stack is empty! No recent additions to undo.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        Assignment last = undoStack.pop();
        int targetId = last.getId();

        if (dbManager.isConnected()) {
            try {
                dbManager.deleteAssignment(targetId);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Undo failed on MySQL:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        assignmentList.removeIf(a -> a.getId() == targetId);
        updateTable();
        updateStats();
        lblStatus.setText("Undo successful: Removed Assignment #" + targetId + " (" + last.getTitle() + ")");
    }

    private void sortAssignmentsById() {
        if (assignmentList.isEmpty()) {
            lblStatus.setText("List is empty. Nothing to sort.");
            return;
        }

        // Sort ascending: smallest ID at top
        assignmentList.sort(Comparator.comparingInt(Assignment::getId));
        updateTable();
        lblStatus.setText("Assignments sorted by ID (Ascending).");
    }

    private void clearAllAssignments() {
        if (assignmentList.isEmpty()) {
            lblStatus.setText("List is already empty.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Warning: This will delete ALL assignments from both the table and MySQL.\nAre you sure?",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (dbManager.isConnected()) {
                try {
                    dbManager.clearAllAssignments();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Failed to clear MySQL table:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            assignmentList.clear();
            undoStack.clear();
            updateTable();
            updateStats();
            resetForm();
            lblStatus.setText("All assignments cleared.");
        }
    }

    private void exportToCsv() {
        if (assignmentList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No assignments to export.", "Empty Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("assignments_export.csv"));
        int choice = fileChooser.showSaveDialog(this);

        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Assignment ID,Course Code,Title,Status,Due Date\n");
                for (Assignment a : assignmentList) {
                    writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            a.getId(),
                            a.getCourseCode().replace("\"", "\"\""),
                            a.getTitle().replace("\"", "\"\""),
                            a.getStatus(),
                            a.getDueDate()));
                }
                JOptionPane.showMessageDialog(this, "Exported " + assignmentList.size() + " assignments to:\n" + file.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
                lblStatus.setText("Exported to " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openDbConfigDialog() {
        DbConfigDialog dialog = new DbConfigDialog(this);
        dialog.setVisible(true);

        // When dialog closes, refresh connection status
        updateDbStatusPill(dbManager.isConnected());
        if (dialog.isSaved() && dbManager.isConnected()) {
            refreshDataFromDb();
        }
    }

    // =========================================================================
    // Table, Form & Stats Management
    // =========================================================================

    private void updateTable() {
        tableModel.setRowCount(0);
        for (Assignment a : assignmentList) {
            tableModel.addRow(new Object[]{
                    a.getId(),
                    a.getCourseCode(),
                    a.getTitle(),
                    a.getStatus(),
                    a.getDueDate()
            });
        }
    }

    private void updateStats() {
        int total = assignmentList.size();
        int pending = 0;
        int completed = 0;

        for (Assignment a : assignmentList) {
            if (a.isCompleted()) completed++;
            else pending++;
        }

        lblStatTotal.setText("Total: " + total);
        lblStatPending.setText("Pending: " + pending);
        lblStatCompleted.setText("Completed: " + completed);
    }

    private void resetForm() {
        editingAssignmentId = null;
        txtId.setText("");
        txtId.setEnabled(true);
        txtCode.setText("");
        txtTitle.setText("");
        cmbStatus.setSelectedIndex(0);
        txtDueDate.setText("");

        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        txtId.requestFocus();
    }

    private void populateFormForEdit(int id) {
        for (Assignment a : assignmentList) {
            if (a.getId() == id) {
                editingAssignmentId = a.getId();
                txtId.setText(String.valueOf(a.getId()));
                txtId.setEnabled(false); // ID is primary key
                txtCode.setText(a.getCourseCode());
                txtTitle.setText(a.getTitle());
                cmbStatus.setSelectedItem(a.getStatus());
                txtDueDate.setText(a.getDueDate());

                btnAdd.setEnabled(false);
                btnUpdate.setEnabled(true);
                lblStatus.setText("Editing Assignment #" + a.getId());
                break;
            }
        }
    }

    private void applyFilter() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            // Case-insensitive regex across ID, Course Code, Title, Status
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        }
    }

    private void toggleStatusAtRow(int viewRow) {
        if (viewRow == -1) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);

        for (Assignment a : assignmentList) {
            if (a.getId() == id) {
                String newStatus = a.isCompleted() ? "Pending" : "Completed";
                a.setStatus(newStatus);

                if (dbManager.isConnected()) {
                    try {
                        dbManager.updateAssignment(a);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Failed to update status in DB: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                updateTable();
                updateStats();
                lblStatus.setText("Assignment #" + id + " marked as " + newStatus);
                break;
            }
        }
    }

    // =========================================================================
    // Event Listeners & Styling
    // =========================================================================

    private void setupEventListeners() {
        // Table row selection -> Populate sidebar form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    int id = (int) tableModel.getValueAt(modelRow, 0);
                    populateFormForEdit(id);
                }
            }
        });

        // Double click to toggle status
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        toggleStatusAtRow(row);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Right click context menu
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        table.setRowSelectionInterval(row, row);
                        showContextMenu(e.getX(), e.getY(), row);
                    }
                }
            }
        });
    }

    private void showContextMenu(int x, int y, int viewRow) {
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);

        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemToggle = new JMenuItem("Toggle Status (Pending / Completed)");
        itemToggle.addActionListener(e -> toggleStatusAtRow(viewRow));

        JMenuItem itemEdit = new JMenuItem("Edit in Form");
        itemEdit.addActionListener(e -> populateFormForEdit(id));

        JMenuItem itemDelete = new JMenuItem("Delete Assignment");
        itemDelete.addActionListener(e -> deleteSelectedAssignment());

        menu.add(itemToggle);
        menu.add(itemEdit);
        menu.addSeparator();
        menu.add(itemDelete);

        menu.show(table, x, y);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(FONT_REGULAR);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(235, 238, 245));
        header.setForeground(new Color(40, 50, 70));
        header.setFont(FONT_HEADER);
        header.setPreferredSize(new Dimension(0, 42));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(360);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);

        // Custom Cell Renderer with Status Badges
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                    c.setForeground(new Color(30, 40, 50));
                } else {
                    c.setBackground(new Color(215, 230, 250));
                    c.setForeground(Color.BLACK);
                }

                if (col == 3) { // Status column badge coloring
                    String status = String.valueOf(value);
                    if ("Completed".equalsIgnoreCase(status)) {
                        c.setForeground(new Color(39, 174, 96));
                        c.setFont(FONT_BOLD);
                    } else {
                        c.setForeground(new Color(211, 84, 0));
                        c.setFont(FONT_BOLD);
                    }
                    setHorizontalAlignment(JLabel.CENTER);
                } else if (col == 2) {
                    setHorizontalAlignment(JLabel.LEFT);
                    setBorder(new EmptyBorder(0, 10, 0, 0));
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                }

                return c;
            }
        });
    }

    private JLabel createInputLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(190, 200, 215));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField createModernTextField() {
        JTextField tf = new JTextField();
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        tf.setPreferredSize(new Dimension(200, 32));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(75, 90, 110), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        tf.setBackground(new Color(45, 55, 70));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(FONT_REGULAR);
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tf;
    }

    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (btn.isEnabled()) btn.setBackground(bg.brighter());
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // =========================================================================
    // Entry Point
    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AssignmentManager app = new AssignmentManager();
            app.setVisible(true);
        });
    }
}
