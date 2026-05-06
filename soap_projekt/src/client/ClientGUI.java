package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Collections;
import java.net.URL;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.*;

public class ClientGUI extends JFrame {

    private JTextField nameField = new JTextField();
    private JTextField deviceField = new JTextField();
    private JTextArea descArea = new JTextArea(4, 20);

    private DefaultListModel<String> imageListModel = new DefaultListModel<>();
    private List<String> imagesBase64 = new ArrayList<>();
    private JList<String> imageList = new JList<>(imageListModel);

    private RepairService service;
    private List<RepairRequest> cachedRequests = new ArrayList<>();

    private JTable requestTable;
    private DefaultTableModel requestModel;

    private static final String CLIENT_FILE = "client_requests.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public ClientGUI() {
        setTitle("🔧 Repair Service Client");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        initSOAP();
        loadRequestsLocal();
    }

    // ---- panels ----
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

        JPanel imgPanel = new JPanel(new BorderLayout());
        imgPanel.add(new JScrollPane(imageList), BorderLayout.CENTER);
        JPanel btns = new JPanel(new GridLayout(1, 2));
        JButton add = new JButton("Add");
        add.addActionListener(e -> uploadImages());
        JButton rem = new JButton("Remove");
        rem.addActionListener(e -> removeImages());
        btns.add(add);
        btns.add(rem);
        imgPanel.add(btns, BorderLayout.SOUTH);

        c.gridx = 1;
        panel.add(imgPanel, c); y++;
        JButton send = new JButton("Send request 🚀");
        send.addActionListener(e -> sendRequest());
        c.gridx = 1; c.gridy = y;
        panel.add(send, c);
        return panel;
    }

    private JPanel buildTablePanel() {
        requestModel = new DefaultTableModel(new String[]{"ID", "Device", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        requestTable = new JTable(requestModel);
        requestTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) showDetails();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        // ---- przyciski na dole tabeli ----
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshServer = new JButton("🔄 Sync z serwerem");
        refreshServer.addActionListener(e -> syncWithServer());

        JButton refreshLocal = new JButton("Reload local");
        refreshLocal.addActionListener(e -> loadRequestsLocal());

        bottomBar.add(refreshServer);
        bottomBar.add(refreshLocal);

        panel.add(bottomBar, BorderLayout.SOUTH);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridx = 0; c.gridy = y; panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1.0; panel.add(field, c);
    }

    // ---- SOAP ----
    private void initSOAP() {
        try {
            URL url = new URL("http://100.64.218.17:8080/repair?wsdl");
            QName qname = new QName("http://server/", "RepairServiceImplService");
            Service s = Service.create(url, qname);
            service = s.getPort(RepairService.class);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "SOAP ERROR: " + e.getMessage());
        }
    }

    // ---- SYNC Z SERWEREM ----
    /**
     * Pobiera z serwera wszystkie zgłoszenia pasujące do lokalnych ID,
     * aktualizuje status i fakturę, zapisuje lokalnie.
     */
    private void syncWithServer() {
        if (service == null) {
            JOptionPane.showMessageDialog(this, "Brak połączenia z serwerem.");
            return;
        }
        try {
            // Pobieramy pełną listę z serwera
            List<RepairRequest> serverList = service.getAllRequestsLight();

            // Budujemy mapę id -> RepairRequest po stronie serwera
            java.util.Map<Integer, RepairRequest> serverMap = new java.util.HashMap<>();
            for (RepairRequest sr : serverList) {
                serverMap.put(sr.getId(), sr);
            }

            boolean anyChange = false;

            for (RepairRequest local : cachedRequests) {
                RepairRequest server = serverMap.get(local.getId());
                if (server == null) continue;

                // Aktualizuj status
                if (!server.getStatus().equals(local.getStatus())) {
                    local.setStatus(server.getStatus());
                    anyChange = true;
                }

                // Aktualizuj fakturę (jeśli admin ją wystawił)
                if (server.getInvoice() != null && local.getInvoice() == null) {
                    local.setInvoice(server.getInvoice());
                    anyChange = true;
                }
            }

            if (anyChange) {
                saveLocalRequests();
                JOptionPane.showMessageDialog(this, "✅ Dane zaktualizowane.");
            } else {
                JOptionPane.showMessageDialog(this, "Brak zmian.");
            }

            loadRequestsLocal();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Błąd synchronizacji: " + e.getMessage());
        }
    }

    // ---- LOCAL LOAD ----
    private void loadRequestsLocal() {
        try {
            File f = new File(CLIENT_FILE);
            if (f.exists()) {
                cachedRequests = mapper.readValue(f, new TypeReference<List<RepairRequest>>() {});
            } else {
                cachedRequests = new ArrayList<>();
            }

            requestModel.setRowCount(0);
            for (RepairRequest r : cachedRequests) {
                // Pokaż "INVOICED" jeśli faktura istnieje
                String displayStatus = (r.getInvoice() != null) ? "INVOICED 🧾" : r.getStatus();
                requestModel.addRow(new Object[]{r.getId(), r.getDevice(), displayStatus});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Local load error: " + e.getMessage());
        }
    }

    // ---- LOCAL SAVE ----
    private void saveLocalRequests() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(CLIENT_FILE), cachedRequests);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- SEND ----
    private void sendRequest() {
        try {
            RepairRequest req = new RepairRequest();
            req.setClientName(nameField.getText());
            req.setDevice(deviceField.getText());
            req.setDescription(descArea.getText());
            req.setImagesBase64(new ArrayList<>(imagesBase64));

            RepairRequest created = service.sendRepairRequest(req);
            cachedRequests.add(created);
            saveLocalRequests();
            JOptionPane.showMessageDialog(this, "Request sent (ID=" + created.getId() + ")");
            clearForm();
            loadRequestsLocal();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ---- DETAILS (podwójne kliknięcie) ----
    private void showDetails() {
        int row = requestTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) requestTable.getValueAt(row, 0);
        RepairRequest r = cachedRequests.stream()
                .filter(x -> x.getId() == id)
                .findFirst().orElse(null);
        if (r == null) return;

        // Jeśli jest faktura — pokaż jej szczegóły
        if (r.getInvoice() != null) {
            showInvoiceDetails(r);
            return;
        }

        // Zwykłe szczegóły zgłoszenia
        JDialog d = new JDialog(this, "Request details #" + id, true);
        d.setSize(600, 400);
        d.setLocationRelativeTo(this);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setText(
                "Device:      " + r.getDevice() + "\n" +
                        "Status:      " + r.getStatus() + "\n" +
                        "Description: " + r.getDescription() + "\n"
        );

        d.add(new JScrollPane(area));
        d.setVisible(true);
    }

    // ---- INVOICE DETAILS ----
    private void showInvoiceDetails(RepairRequest r) {
        InvoiceResponse inv = r.getInvoice();

        JDialog dialog = new JDialog(this, "Faktura – " + r.getDevice(), true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setText(
                "=== FAKTURA ===\n\n" +
                        "Klient:      " + r.getClientName() + "\n" +
                        "Urządzenie:  " + r.getDevice() + "\n\n" +
                        "Czynności:\n" + inv.getActions() + "\n\n" +
                        "Robocizna:   " + inv.getLaborCost() + "\n" +
                        "Części:      " + inv.getPartsCost() + "\n" +
                        "RAZEM:       " + inv.getPrice() + "\n"
        );

        JButton pdfBtn = new JButton("📄 Generuj PDF");
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
                java.awt.Desktop.getDesktop().open(pdf);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Nie udało się wygenerować PDF:\n" + ex.getMessage(),
                        "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(pdfBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ---- image ops ----
    private void uploadImages() {
        JFileChooser ch = new JFileChooser();
        ch.setMultiSelectionEnabled(true);
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : ch.getSelectedFiles()) {
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    imagesBase64.add(Base64.getEncoder().encodeToString(bytes));
                    imageListModel.addElement(f.getName());
                } catch (Exception ignored) {}
            }
        }
    }

    private void removeImages() {
        List<String> sel = imageList.getSelectedValuesList();
        List<Integer> idx = new ArrayList<>();
        for (String s : sel) idx.add(imageListModel.indexOf(s));
        idx.sort(Collections.reverseOrder());
        for (int i : idx) { imageListModel.remove(i); imagesBase64.remove(i); }
    }

    private void clearForm() {
        nameField.setText("");
        deviceField.setText("");
        descArea.setText("");
        imageListModel.clear();
        imagesBase64.clear();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
