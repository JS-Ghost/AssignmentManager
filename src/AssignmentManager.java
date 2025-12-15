package assignmentmanager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 * Student Assignment File Manager (v3.1)
 * Update: Sorting fixed to arrange by Assignment ID (1 at Top).
 */
public class AssignmentManager extends JFrame {

    // --- Data Model ---
    class Assignment {
        int id;
        String courseCode; 
        String title;

        public Assignment(int id, String courseCode, String title) {
            this.id = id;
            this.courseCode = courseCode;
            this.title = title;
        }
    }

    // --- Global Variables ---
    private final Stack<Assignment> assignmentStack = new Stack<>();
    private final DefaultTableModel tableModel;
    private final JTable table;
    
    // --- UI Components ---
    private JTextField txtId, txtCode, txtTitle;
    private JLabel lblStatus;

    // --- Colors & Fonts ---
    private final Color SIDEBAR_BG = new Color(33, 41, 54); 
    private final Color MAIN_BG = new Color(240, 242, 245); 
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);

    public AssignmentManager() {
        // 1. Frame Setup
        setTitle("Assignment Manager Pro");
        setSize(1000, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set Look and Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 2. Sidebar (Inputs & Controls)
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(320, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Sidebar Title
        JLabel appTitle = new JLabel("TASK MANAGER");
        appTitle.setForeground(new Color(200, 200, 200));
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(appTitle);
        sidebar.add(Box.createVerticalStrut(25));

        // Input Fields
        sidebar.add(createDarkLabel("Assignment ID (No)"));
        txtId = createModernTextField();
        sidebar.add(txtId);
        sidebar.add(Box.createVerticalStrut(15));

        sidebar.add(createDarkLabel("Course Code (e.g. CSE110)"));
        txtCode = createModernTextField();
        sidebar.add(txtCode);
        sidebar.add(Box.createVerticalStrut(15));

        sidebar.add(createDarkLabel("Assignment Title"));
        txtTitle = createModernTextField();
        sidebar.add(txtTitle);
        sidebar.add(Box.createVerticalStrut(25));

        // Buttons
        JButton btnAdd = createModernButton("➕  Add Assignment", new Color(46, 204, 113), Color.WHITE);
        JButton btnProcess = createModernButton("⚡  Process & Sort", new Color(52, 152, 219), Color.WHITE);
        JButton btnSearch = createModernButton("🔍  Search ID", new Color(241, 196, 15), Color.BLACK);
        JButton btnDelete = createModernButton("❌  Delete Selected", new Color(231, 76, 60), Color.WHITE); 
        JButton btnPop = createModernButton("↩️  Undo Last (Pop)", new Color(230, 126, 34), Color.WHITE);
        JButton btnClear = createModernButton("🗑️  Clear All", new Color(192, 57, 43), Color.WHITE);

        sidebar.add(btnAdd);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnDelete); 
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnProcess);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnSearch);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnPop);
        sidebar.add(Box.createVerticalStrut(30)); 
        sidebar.add(btnClear);

        add(sidebar, BorderLayout.WEST);

        // 3. Main Content (Table)
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(MAIN_BG);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Table Header Label
        JLabel tableLabel = new JLabel("Current Assignment Stack");
        tableLabel.setFont(HEADER_FONT);
        tableLabel.setForeground(Color.DARK_GRAY);
        tableLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainPanel.add(tableLabel, BorderLayout.NORTH);

        // Table Setup
        String[] columnNames = {"Assignment No", "Course Code", "Title"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Status Bar
        lblStatus = new JLabel("System Ready...");
        lblStatus.setFont(new Font("Consolas", Font.ITALIC, 12));
        lblStatus.setForeground(Color.GRAY);
        lblStatus.setBorder(new EmptyBorder(10, 0, 0, 0));
        mainPanel.add(lblStatus, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // --- Action Listeners ---

        btnAdd.addActionListener(e -> addAssignment());
        btnDelete.addActionListener(e -> deleteSelectedAssignment());
        btnProcess.addActionListener(e -> processAndSort());
        btnSearch.addActionListener(e -> searchAssignment());
        btnPop.addActionListener(e -> popAssignment());
        btnClear.addActionListener(e -> clearMemory());
    }

    // --- Logic Functions ---

    private void addAssignment() {
        try {
            if (txtId.getText().trim().isEmpty() || txtCode.getText().trim().isEmpty() || txtTitle.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int id = Integer.parseInt(txtId.getText().trim());
            String code = txtCode.getText().trim().toUpperCase();
            String title = txtTitle.getText().trim();

            // Duplicate Check
            for (Assignment a : assignmentStack) {
                if (a.id == id) {
                    JOptionPane.showMessageDialog(this, "Assignment ID " + id + " already exists!", "Duplicate Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            Assignment newAssign = new Assignment(id, code, title);
            assignmentStack.push(newAssign);
            
            updateTable();
            lblStatus.setText("Successfully added Assignment #" + id);
            
            txtId.setText("");
            txtCode.setText("");
            txtTitle.setText("");
            txtId.requestFocus();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Assignment ID must be a number!", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAssignment() {
        int selectedRow = table.getSelectedRow();
        
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an assignment from the table to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Calculate actual index in Stack (Table is Top-to-Bottom, Stack is Bottom-to-Top)
        int stackIndex = assignmentStack.size() - 1 - selectedRow;

        Assignment toDelete = assignmentStack.get(stackIndex);
        
        int confirm = JOptionPane.showConfirmDialog(this, "Delete Assignment #" + toDelete.id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            assignmentStack.remove(stackIndex);
            updateTable();
            lblStatus.setText("Deleted Assignment #" + toDelete.id);
        }
    }

    // --- UPDATED SORTING LOGIC ---
    private void processAndSort() {
        if (assignmentStack.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No assignments to process.", "Empty Stack", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        /* SORT LOGIC:
           We want ID 1 to be at the TOP of the visual list.
           The visual list displays the stack from Last-In (Top) to First-In (Bottom).
           To get ID 1 at the Visual Top, ID 1 must be the Last item in the Stack memory.
           Therefore, we sort the Stack in DESCENDING order (Largest ID at bottom, Smallest ID at top).
        */
        assignmentStack.sort((a, b) -> Integer.compare(b.id, a.id));

        updateTable();
        lblStatus.setText("Assignments sorted by ID (1 at Top).");
        JOptionPane.showMessageDialog(this, "Assignments processed & sorted by ID!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void searchAssignment() {
        if (assignmentStack.isEmpty()) return;

        String input = JOptionPane.showInputDialog(this, "Enter Assignment ID to search:");
        if (input != null && !input.isEmpty()) {
            try {
                int searchId = Integer.parseInt(input);
                boolean found = false;
                
                for (int i = 0; i < assignmentStack.size(); i++) {
                    if (assignmentStack.get(i).id == searchId) {
                        int rowViewIndex = assignmentStack.size() - 1 - i;
                        table.setRowSelectionInterval(rowViewIndex, rowViewIndex);
                        table.scrollRectToVisible(table.getCellRect(rowViewIndex, 0, true));
                        
                        lblStatus.setText("Found: " + assignmentStack.get(i).title);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                     JOptionPane.showMessageDialog(this, "ID " + searchId + " not found.", "Search Result", JOptionPane.WARNING_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid ID format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void popAssignment() {
        if (assignmentStack.isEmpty()) {
            lblStatus.setText("Stack Underflow! Nothing to remove.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Assignment removed = assignmentStack.pop();
        updateTable();
        lblStatus.setText("Removed (Popped): " + removed.title);
    }

    private void clearMemory() {
        if (assignmentStack.isEmpty()) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, "Delete all assignments?", "Clear Memory", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            assignmentStack.clear();
            updateTable();
            lblStatus.setText("Memory cleared.");
        }
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        // Visual Stack: Last In, First (Top) Out
        for (int i = assignmentStack.size() - 1; i >= 0; i--) {
            Assignment a = assignmentStack.get(i);
            tableModel.addRow(new Object[]{a.id, a.courseCode, a.title});
        }
    }

    // --- Styling Helpers ---

    private JLabel createDarkLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(180, 180, 180));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField createModernTextField() {
        JTextField tf = new JTextField();
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        tf.setPreferredSize(new Dimension(200, 35));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(100, 100, 100), 1), 
            new EmptyBorder(5, 10, 5, 10)
        ));
        tf.setBackground(new Color(45, 55, 70));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tf;
    }

    private JButton createModernButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.brighter());
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(230, 230, 230));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 40));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 255));
                } else {
                    c.setBackground(new Color(220, 230, 241)); // Light blue selection
                    c.setForeground(Color.BLACK);
                }
                ((JLabel) c).setHorizontalAlignment(column == 2 ? JLabel.LEFT : JLabel.CENTER); 
                if(column == 2) setBorder(new EmptyBorder(0,10,0,0)); 
                return c;
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AssignmentManager().setVisible(true));
    }
}