package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.awt.*;
import java.net.URL;
import java.util.List;

public class AdminGUI extends JFrame {

    private JTable activeTable;
    private JTable doneTable;

    private DefaultTableModel activeModel;
    private DefaultTableModel doneModel;

    private RepairService service;

    public AdminGUI() {
        setTitle("Admin Panel 🔧");
        setSize(900, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();

        // ===== ACTIVE TAB =====
        activeModel = new DefaultTableModel(
                new String[]{"ID", "Client", "Device", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        activeTable = new JTable(activeModel);

        activeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        activeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        activeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetails(activeTable);
                }
            }
        });

        JPanel activePanel = new JPanel(new BorderLayout());
        activePanel.add(new JScrollPane(activeTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel();

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadData());

        JButton inProgress = new JButton("IN_PROGRESS");
        inProgress.addActionListener(e -> updateStatus(activeTable, "IN_PROGRESS"));

        JButton done = new JButton("DONE");
        done.addActionListener(e -> updateStatus(activeTable, "DONE"));

        buttons.add(refresh);
        buttons.add(inProgress);
        buttons.add(done);

        activePanel.add(buttons, BorderLayout.SOUTH);

        // ===== DONE TAB =====
        doneModel = new DefaultTableModel(
                new String[]{"ID", "Client", "Device"}, 0
        ){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        doneTable = new JTable(doneModel);

        doneTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        doneTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetails(doneTable);
                }
            }
        });

        JPanel donePanel = new JPanel(new BorderLayout());
        donePanel.add(new JScrollPane(doneTable), BorderLayout.CENTER);

        JButton refreshDone = new JButton("Refresh DONE");
        refreshDone.addActionListener(e -> loadData());

        donePanel.add(refreshDone, BorderLayout.SOUTH);

        tabs.addTab("Aktywne", activePanel);
        tabs.addTab("Zakończone", donePanel);

        add(tabs);

        initSOAP();
    }

    private void initSOAP() {
        try {
            URL url = new URL("http://192.168.1.109:8080/repair?wsdl");
            QName qname = new QName("http://server/", "RepairServiceImplService");
            Service s = Service.create(url, qname);
            service = s.getPort(RepairService.class);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "SOAP ERROR: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<RepairRequest> list = service.getAllRequests();

            activeModel.setRowCount(0);
            doneModel.setRowCount(0);

            for (int i = 0; i < list.size(); i++) {
                RepairRequest r = list.get(i);

                if ("DONE".equals(r.getStatus())) {
                    doneModel.addRow(new Object[]{
                            i,
                            r.getClientName(),
                            r.getDevice()
                    });
                } else {
                    activeModel.addRow(new Object[]{
                            i,
                            r.getClientName(),
                            r.getDevice(),
                            r.getStatus()
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
            service.updateStatus(id, status);
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

        RepairRequest r = service.getAllRequests().get(id);

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

        // ===== ZDJĘCIA (ŚRODEK) =====
        JPanel imagesPanel = new JPanel();

        int cols = Math.max(1, Math.min(3, r.getImagesBase64().size()));
        imagesPanel.setLayout(new GridLayout(0, cols, 5, 5));

        for (String base64 : r.getImagesBase64()) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(base64);

                ImageIcon icon = new ImageIcon(bytes);
                Image img = icon.getImage();

                int maxSize = 720;

                int width = icon.getIconWidth();
                int height = icon.getIconHeight();

                double scale = Math.min((double) maxSize / width, (double) maxSize / height);

                int newW = (int) (width * scale);
                int newH = (int) (height * scale);

                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);

                JLabel label = new JLabel(new ImageIcon(scaled));
                label.setHorizontalAlignment(JLabel.CENTER);

                imagesPanel.add(label);

            } catch (Exception e) {
                imagesPanel.add(new JLabel("Image error"));
            }
        }

        JScrollPane imageScroll = new JScrollPane(imagesPanel);
        imageScroll.getVerticalScrollBar().setUnitIncrement(16);
        imageScroll.setBorder(null);

        dialog.add(imageScroll, BorderLayout.CENTER);
        if ("DONE".equals(r.getStatus())) {
            JButton invoiceBtn = new JButton("💰 Wystaw fakturę");

            invoiceBtn.addActionListener(e -> {
                openInvoiceWindow(r, id);

                // 🔥 po wystawieniu faktury zmień status
                r.setStatus("INVOICED");

                dialog.dispose(); // zamknij szczegóły
                showDetails(table); // odśwież widok
            });

            dialog.add(invoiceBtn, BorderLayout.SOUTH);
        }

        // ===== SHOW =====
        dialog.setVisible(true);
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

                InvoiceResponse res = service.createInvoice(req);

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