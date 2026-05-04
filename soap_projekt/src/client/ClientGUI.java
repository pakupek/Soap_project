package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Collections;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class ClientGUI extends JFrame {

    private JTextField nameField = new JTextField();
    private JTextField deviceField = new JTextField();
    private JTextArea descArea = new JTextArea(4, 20);

    private DefaultListModel<String> imageListModel = new DefaultListModel<>();
    private List<String> imagesBase64 = new ArrayList<>();
    private JList<String> imageList = new JList<>(imageListModel);

    private RepairService service;
    private List<RepairRequest> cachedRequests = new ArrayList<>();

    // ===== LISTA ZGŁOSZEŃ =====
    private JTable requestTable;
    private DefaultTableModel requestModel;

    public ClientGUI() {
        setTitle("🔧 Repair Service Client");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        initSOAP();
        loadRequests();
    }

    // ---------- FORM ----------
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        addRow(panel, c, y++, "Client name:", nameField);
        addRow(panel, c, y++, "Device:", deviceField);

        c.gridx = 0; c.gridy = y;
        panel.add(new JLabel("Description:"), c);

        c.gridx = 1;
        panel.add(new JScrollPane(descArea), c);
        y++;

        // images
        c.gridx = 0; c.gridy = y;
        panel.add(new JLabel("Images:"), c);

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(new JScrollPane(imageList), BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(1, 2));

        JButton uploadBtn = new JButton("Add");
        uploadBtn.addActionListener(e -> uploadImages());

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> removeImages());

        btns.add(uploadBtn);
        btns.add(removeBtn);

        imagePanel.add(btns, BorderLayout.SOUTH);

        c.gridx = 1;
        panel.add(imagePanel, c);
        y++;

        JButton sendBtn = new JButton("Send request 🚀");
        sendBtn.addActionListener(e -> sendRequest());

        c.gridx = 1; c.gridy = y;
        panel.add(sendBtn, c);

        return panel;
    }

    // ---------- TABLE ----------
    private JPanel buildTablePanel() {

        requestModel = new DefaultTableModel(
                new String[]{"ID", "Device", "Status"}, 0
        ) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        requestTable = new JTable(requestModel);

        requestTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetails();
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadRequests());

        panel.add(refresh, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- LOAD REQUESTS ----------
    private void loadRequests() {
        try {
            cachedRequests = service.getAllRequests();

            requestModel.setRowCount(0);

            for (RepairRequest r : cachedRequests) {
                requestModel.addRow(new Object[]{
                        r.getId(),
                        r.getDevice(),
                        r.getStatus()
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
        }
    }

    // ---------- DETAILS (WITH INVOICE) ----------
    private void showDetails() {

        int row = requestTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) requestTable.getValueAt(row, 0);
        RepairRequest r = cachedRequests.stream()
                .filter(x -> x.getId() == id)
                .findFirst()
                .orElse(null);

        if (r == null) return;

        JDialog dialog = new JDialog(this, "Request details", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setText(
                "Device: " + r.getDevice() + "\n" +
                        "Status: " + r.getStatus() + "\n" +
                        "Description: " + r.getDescription() + "\n"
        );

        dialog.add(new JScrollPane(info), BorderLayout.NORTH);

        JTextArea invoiceArea = new JTextArea();
        invoiceArea.setEditable(false);

        if (r.getInvoice() != null) {
            InvoiceResponse inv = r.getInvoice();

            invoiceArea.setText(
                    "=== INVOICE ===\n" +
                            "ID: " + inv.getInvoiceId() + "\n" +
                            "Price: " + inv.getPrice() + "\n" +
                            "Koszt usługi: " + inv.getLaborCost() + "zł\n" +
                            "Koszt części: " + inv.getPartsCost() + "zł\n" +
                            "Opis: \n" + inv.getActions()
            );
        } else {
            invoiceArea.setText("No invoice yet");
        }

        dialog.add(new JScrollPane(invoiceArea), BorderLayout.CENTER);

        dialog.setVisible(true);
    }


    private void addRow(JPanel panel, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = y;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(field, c);
    }

    // ---------- SOAP ----------
    private void initSOAP() {
        try {
            URL url = new URL("http://192.168.0.193:8080/repair?wsdl");
            QName qname = new QName("http://server/", "RepairServiceImplService");

            Service s = Service.create(url, qname);
            service = s.getPort(RepairService.class);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "SOAP ERROR: " + e.getMessage());
        }
    }

    // ---------- SEND ----------
    private void sendRequest() {
        try {
            RepairRequest r = new RepairRequest();
            r.setClientName(nameField.getText());
            r.setDevice(deviceField.getText());
            r.setDescription(descArea.getText());
            r.setImagesBase64(new ArrayList<>(imagesBase64));

            InvoiceResponse invoice = service.sendRepairRequest(r);
            if (invoice != null) {
                r.setInvoice(invoice);
            }

            JOptionPane.showMessageDialog(this, "Request sent!");
            clearForm();
            loadRequests();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ---------- IMAGES ----------
    private void uploadImages() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    imagesBase64.add(Base64.getEncoder().encodeToString(bytes));
                    imageListModel.addElement(file.getName());
                } catch (Exception ignored) {}
            }
        }
    }

    private void removeImages() {
        List<String> selected = imageList.getSelectedValuesList();

        List<Integer> indices = new ArrayList<>();
        for (String s : selected) {
            indices.add(imageListModel.indexOf(s));
        }

        indices.sort(Collections.reverseOrder());

        for (int i : indices) {
            imageListModel.remove(i);
            imagesBase64.remove(i);
        }
    }

    // ---------- CLEAR ----------
    private void clearForm() {
        nameField.setText("");
        deviceField.setText("");
        descArea.setText("");

        imagesBase64.clear();
        imageListModel.clear();
    }

    // ---------- MAIN ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}