package client;

import common.InvoiceRequest;
import common.InvoiceResponse;
import common.RepairRequest;
import common.RepairService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class AdminGUI extends JFrame {

    private JTable activeTable;
    private JTable doneTable;
    private JTable unpaidDoneTable;
    private JTable invoiceTable;

    private DefaultTableModel activeModel;
    private DefaultTableModel doneModel;
    private DefaultTableModel unpaidDoneModel;
    private DefaultTableModel invoiceModel;

    private AdminDataController controller;

    private List<RepairRequest> cache = new ArrayList<>();

    public AdminGUI() {
        setTitle("Admin Panel");
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        initModels();
        initTables();
        initTabs(tabs);

        add(createTopBar(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        initSOAP();
    }

    private void initModels() {

        activeModel = createModel(new String[]{"ID", "Client", "Device", "Status"});
        doneModel = createModel(new String[]{"ID", "Client", "Device"});
        unpaidDoneModel = createModel(new String[]{"ID", "Client", "Device"});
        invoiceModel = createModel(new String[]{"ID", "Client", "Device", "Total"});
    }

    private void initTables() {

        activeTable = createTable(activeModel);
        doneTable = createTable(doneModel);
        unpaidDoneTable = createTable(unpaidDoneModel);
        invoiceTable = createTable(invoiceModel);

        attachDoubleClick(activeTable);
        attachDoubleClick(doneTable);
        attachDoubleClick(unpaidDoneTable);
        attachInvoiceClick();
    }

    private DefaultTableModel createModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }
    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    private void initTabs(JTabbedPane tabs) {

        tabs.addTab("Aktywne", createActiveTab());
        tabs.addTab("Zakończone bez faktury", createUnpaidTab());
        tabs.addTab("Faktury", createInvoiceTab());
    }

    private JPanel createActiveTab() {

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(new JScrollPane(activeTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel();

        JButton inProgress = new JButton("IN_PROGRESS");
        inProgress.addActionListener(e -> updateStatus(activeTable, "IN_PROGRESS"));

        JButton done = new JButton("DONE");
        done.addActionListener(e -> updateStatus(activeTable, "DONE"));

        buttons.add(inProgress);
        buttons.add(done);

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createUnpaidTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(unpaidDoneTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createInvoiceTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTopBar() {

        JPanel topBar = new JPanel(new BorderLayout());

        JButton refreshAll = new JButton("Refresh");
        refreshAll.setFocusPainted(false);
        refreshAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshAll.addActionListener(e -> loadData());

        JPanel right = new JPanel();
        right.add(refreshAll);

        topBar.add(right, BorderLayout.EAST);

        return topBar;
    }

    private void attachDoubleClick(JTable table) {
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetails(table);
                }
            }
        });
    }

    private void attachInvoiceClick() {
        invoiceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showInvoiceDetails(invoiceTable);
                }
            }
        });
    }

    private void initSOAP() {
        try {
            URL url = new URL("http://192.168.1.107:8080/repair?wsdl");
            QName qname = new QName("http://server/", "RepairServiceImplService");

            Service s = Service.create(url, qname);

            RepairService svc = s.getPort(RepairService.class);
            controller = new AdminDataController(svc);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "SOAP ERROR: " + e.getMessage(),
                    "Błąd połączenia",
                    JOptionPane.ERROR_MESSAGE
            );

            // 🔥 blokada działania UI bez backendu
            controller = null;

            setEnabled(false);
        }
    }

    private void loadData() {
        try {
            if (controller == null) return;

            cache = controller.getAll();

            activeModel.setRowCount(0);
            doneModel.setRowCount(0);
            unpaidDoneModel.setRowCount(0);
            invoiceModel.setRowCount(0);

            for (RepairRequest r : cache) {

                // ACTIVE
                if (!"DONE".equals(r.getStatus()) && r.getInvoice() == null) {
                    activeModel.addRow(new Object[]{
                            r.getId(),
                            r.getClientName(),
                            r.getDevice(),
                            r.getStatus()
                    });
                }

                // DONE WITHOUT INVOICE
                else if ("DONE".equals(r.getStatus()) && r.getInvoice() == null) {
                    unpaidDoneModel.addRow(new Object[]{
                            r.getId(),
                            r.getClientName(),
                            r.getDevice()
                    });
                }

                // INVOICES
                else if (r.getInvoice() != null) {

                    InvoiceResponse inv = r.getInvoice();

                    invoiceModel.addRow(new Object[]{
                            r.getId(),
                            r.getClientName(),
                            r.getDevice(),
                            inv.getPrice()
                    });
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
        }
    }

    private void updateStatus(JTable table, String status) {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Wybierz zgłoszenie 😄");
            return;
        }

        try {
            int id = (int) table.getValueAt(row, 0);
            controller.updateStatus(id, status);
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
        }
    }

    // ----- Details page ----
    private void showDetails(JTable table) {
        int row = table.getSelectedRow();

        if (row == -1) return;

        int id = (int) table.getValueAt(row, 0);

        RepairRequest r = findById(id);

        if (r == null) return;

        // ===== OKNO =====
        JDialog dialog = new JDialog(this, "Request #" + id, true);
        dialog.setSize(1000, 750);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(true);

        // ===== TEKST (GÓRA) =====
        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setFont(new Font("Consolas", Font.PLAIN, 13));
        details.setText(
                "=== REQUEST DETAILS ===\n" +
                        "Client: " + r.getClientName() + "\n" +
                        "Device: " + r.getDevice() + "\n" +
                        "Status: " + r.getStatus() + "\n" +
                        "Description: " + r.getDescription() + "\n"
        );

        JScrollPane textScroll = new JScrollPane(details);
        textScroll.setPreferredSize(new Dimension(1000, 180));

        dialog.add(textScroll, BorderLayout.NORTH);

        // ===== ZDJĘCIA (ŚRODEK) - VIEWER =====
        JPanel imagesPanel = new JPanel(new BorderLayout());

        JLabel imageLabel = new JLabel("", JLabel.CENTER);

        List<String> images = r.getImagesBase64();
        final int[] index = {0};

        if (images == null || images.isEmpty()) {

            JLabel empty = new JLabel("No images", JLabel.CENTER);
            imagesPanel.setLayout(new BorderLayout());
            imagesPanel.add(empty, BorderLayout.CENTER);

        } else {

            Runnable showImage = () -> {
                try {
                    byte[] bytes = java.util.Base64.getDecoder().decode(images.get(index[0]));

                    ImageIcon icon = new ImageIcon(bytes);
                    Image img = icon.getImage();

                    Image scaled = img.getScaledInstance(700, 450, Image.SCALE_SMOOTH);

                    imageLabel.setIcon(new ImageIcon(scaled));
                    imageLabel.setText((index[0] + 1) + " / " + images.size());
                    imageLabel.setHorizontalTextPosition(JLabel.CENTER);
                    imageLabel.setVerticalTextPosition(JLabel.BOTTOM);

                } catch (Exception ex) {
                    imageLabel.setText("Image error");
                }
            };

            showImage.run();

            JButton prev = new JButton("<");
            prev.addActionListener(e -> {
                if (index[0] > 0) {
                    index[0]--;
                    showImage.run();
                }
            });

            JButton next = new JButton(">");
            next.addActionListener(e -> {
                if (index[0] < images.size() - 1) {
                    index[0]++;
                    showImage.run();
                }
            });

            JPanel controls = new JPanel();
            controls.add(prev);
            controls.add(next);

            imagesPanel.add(imageLabel, BorderLayout.CENTER);
            imagesPanel.add(controls, BorderLayout.SOUTH);
        }

        dialog.add(imagesPanel, BorderLayout.CENTER);

        if ("DONE".equals(r.getStatus()) && r.getInvoice() == null) {

            JButton invoiceBtn = new JButton("Wystaw fakturę");

            invoiceBtn.addActionListener(e -> {
                openInvoiceWindow(r, id);
                dialog.dispose();
            });

            dialog.add(invoiceBtn, BorderLayout.SOUTH);
        }

        // ===== SHOW =====
        dialog.setVisible(true);
    }

    private void showInvoiceDetails(JTable table) {

        int row = table.getSelectedRow();
        if (row == -1) return;

        int id = (int) table.getValueAt(row, 0);

        RepairRequest r = findById(id);
        if (r == null) return;

        InvoiceResponse inv = r.getInvoice();
        if (inv == null) return;

        JDialog dialog = new JDialog(this, "Invoice details", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setText(
                "CLIENT: " + r.getClientName() + "\n" +
                        "DEVICE: " + r.getDevice() + "\n\n" +
                        "ACTIONS:\n" + inv.getActions() + "\n\n" +
                        "LABOR: " + inv.getLaborCost() + "\n" +
                        "PARTS: " + inv.getPartsCost() + "\n" +
                        "TOTAL: " + inv.getPrice()
        );

        JButton pdfBtn = new JButton("Generuj PDF");
        pdfBtn.addActionListener(e -> {
            try {
                File pdf = PdfGenerator.generate(
                        inv.getInvoiceId(),
                        r.getClientName(),
                        r.getDevice(),
                        inv.getActions(),
                        inv.getLaborCost(),
                        inv.getPartsCost()
                );
                java.awt.Desktop.getDesktop().open(new java.io.File(pdf.getAbsolutePath()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Nie udało się wygenerować PDF:\n" + ex.getMessage(),
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(pdfBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private RepairRequest findById(int id) {
        return cache.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private void openInvoiceWindow(RepairRequest r, int requestId) {

        JDialog dialog = new JDialog(this, "Invoice Creator", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setText(
                "Client: " + r.getClientName() + "\n" +
                        "Device: " + r.getDevice() + "\n" +
                        "Description: " + r.getDescription() + "\n"
        );

        dialog.add(new JScrollPane(info), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField laborCost = new JTextField();
        JTextField partsCost = new JTextField();
        JTextArea actions = new JTextArea(5, 20);

        form.add(new JLabel("Labor cost:"));
        form.add(laborCost);

        form.add(new JLabel("Parts cost:"));
        form.add(partsCost);

        form.add(new JLabel("Actions:"));
        form.add(new JScrollPane(actions));

        dialog.add(form, BorderLayout.CENTER);

        JButton send = new JButton("Send invoice");

        send.addActionListener(e -> {
            try {
                InvoiceRequest req = new InvoiceRequest();
                req.setRequestId(requestId);
                req.setActions(actions.getText());
                req.setLaborCost(Double.parseDouble(laborCost.getText()));
                req.setPartsCost(Double.parseDouble(partsCost.getText()));

                InvoiceResponse res = controller.createInvoice(req);

                // 🔥 NAJWAŻNIEJSZE: przypięcie faktury do zgłoszenia
                r.setInvoice(res);
                r.setStatus("INVOICED");

                JOptionPane.showMessageDialog(dialog,
                        "Invoice sent!\nID: " + res.getInvoiceId() +
                                "\nTotal: " + res.getPrice()
                );


                dialog.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.add(send, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminGUI().setVisible(true));
    }
}