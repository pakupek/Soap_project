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
        );
        activeTable = new JTable(activeModel);

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
        );
        doneTable = new JTable(doneModel);

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminGUI().setVisible(true));
    }
}