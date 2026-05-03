package client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.ws.Service;
import javax.xml.namespace.QName;
import java.net.URL;

public class ClientGUI extends JFrame {

    private JTextField nameField = new JTextField();
    private JTextField deviceField = new JTextField();
    private JTextArea descArea = new JTextArea(4, 20);
    private JTextArea output = new JTextArea();

    private DefaultListModel<String> imageListModel = new DefaultListModel<>();
    private List<String> imagesBase64 = new ArrayList<>();

    private JList<String> imageList = new JList<>(imageListModel);

    private RepairService service;

    public ClientGUI() {
        setTitle("🔧 Repair Service Client");
        setSize(650, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildOutputPanel(), BorderLayout.CENTER);

        initSOAP();
    }

    // ---------- UI ----------
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        addRow(panel, c, y++, "Client name:", nameField);
        addRow(panel, c, y++, "Device:", deviceField);

        // Description
        c.gridx = 0; c.gridy = y;
        panel.add(new JLabel("Description:"), c);

        c.gridx = 1;
        panel.add(new JScrollPane(descArea), c);
        y++;

        // Images list
        c.gridx = 0; c.gridy = y;
        panel.add(new JLabel("Images:"), c);

        JPanel imagePanel = new JPanel(new BorderLayout());

        imagePanel.add(new JScrollPane(imageList), BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(1,2));

        JButton uploadBtn = new JButton("Add images");
        uploadBtn.addActionListener(e -> uploadImages());

        JButton removeBtn = new JButton("Remove selected");
        removeBtn.addActionListener(e -> removeImages());

        btns.add(uploadBtn);
        btns.add(removeBtn);

        imagePanel.add(btns, BorderLayout.SOUTH);

        c.gridx = 1;
        panel.add(imagePanel, c);
        y++;

        // Send button
        JButton sendBtn = new JButton("Send repair request 🚀");
        sendBtn.setBackground(new Color(30, 144, 255));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);

        sendBtn.addActionListener(e -> sendRequest());

        c.gridx = 1; c.gridy = y;
        panel.add(sendBtn, c);

        return panel;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        output.setEditable(false);
        output.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(new JScrollPane(output), BorderLayout.CENTER);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridx = 0; c.gridy = y;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        panel.add(field, c);
    }

    // ---------- SOAP ----------
    private void initSOAP() {
        try {
            URL url = new URL("http://192.168.1.109:8080/repair?wsdl");
            QName qname = new QName("http://server/", "RepairServiceImplService");

            Service s = Service.create(url, qname);
            service = s.getPort(RepairService.class);

        } catch (Exception e) {
            output.setText("SOAP ERROR: " + e.getMessage());
        }
    }

    // ---------- IMAGE UPLOAD ----------
    private void uploadImages() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            File[] files = chooser.getSelectedFiles();

            for (File file : files) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String base64 = Base64.getEncoder().encodeToString(bytes);

                    imagesBase64.add(base64);
                    imageListModel.addElement(file.getName());

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error reading file: " + file.getName());
                }
            }
        }
    }

    // -------- REMOVE ---------
    private void removeImages() {
        List<String> selected = imageList.getSelectedValuesList();

        for (String name : selected) {
            int index = imageListModel.indexOf(name);

            if (index != -1) {
                imageListModel.remove(index);

                if (index < imagesBase64.size()) {
                    imagesBase64.remove(index);
                }
            }
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

            InvoiceResponse res = service.sendRepairRequest(r);

            output.setText(
                    "=== INVOICE ===\n" +
                            "ID: " + res.getInvoiceId() + "\n" +
                            "Price: " + res.getPrice() + "\n" +
                            "Status: " + res.getStatus()
            );

        } catch (Exception e) {
            output.setText("ERROR: " + e.getMessage());
        }
    }

    // ---------- MAIN ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientGUI().setVisible(true);
        });
    }
}