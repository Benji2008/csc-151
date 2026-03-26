// UPDATED VERSION - Multi-Project CSV Support Added
// Now saves multiple estimates and can load/display history

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;

public class ConcreteEstimatorPro extends JFrame {

    private JTextField txtProject, txtLocation;
    private JTextField txtLength, txtWidth, txtThickness;
    private JTextField txtWorkers, txtHours, txtDays, txtRate;
    private JTextField txtDiscount;

    private JLabel lblMaterialCost, lblLaborCost, lblTotalCost;
    private JTextArea txtHistory;

    private final DecimalFormat DF = new DecimalFormat("#,##0.00");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConcreteEstimatorPro().setVisible(true));
    }

    public ConcreteEstimatorPro() {
        setTitle("Concrete Pad Estimator Pro - Fayetteville NC");
        setSize(700, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(0,1));

        add(projectPanel());
        add(sizePanel());
        add(laborPanel());
        add(costPanel());
        add(buttonPanel());
        add(historyPanel());

        loadLastLabor();
        loadHistory();
    }

    private JPanel projectPanel() {
        JPanel p = new JPanel(new GridLayout(2,2));
        p.setBorder(BorderFactory.createTitledBorder("Project Info"));

        txtProject = new JTextField();
        txtLocation = new JTextField("Fayetteville, NC");

        p.add(new JLabel("Project Name:"));
        p.add(txtProject);
        p.add(new JLabel("Location:"));
        p.add(txtLocation);

        return p;
    }

    private JPanel sizePanel() {
        JPanel p = new JPanel(new GridLayout(3,2));
        p.setBorder(BorderFactory.createTitledBorder("Concrete Dimensions"));

        txtLength = new JTextField("100");
        txtWidth = new JTextField("150");
        txtThickness = new JTextField("6");

        p.add(new JLabel("Length (ft):")); p.add(txtLength);
        p.add(new JLabel("Width (ft):")); p.add(txtWidth);
        p.add(new JLabel("Thickness (in):")); p.add(txtThickness);

        return p;
    }

    private JPanel laborPanel() {
        JPanel p = new JPanel(new GridLayout(5,2));
        p.setBorder(BorderFactory.createTitledBorder("Labor"));

        txtWorkers = new JTextField();
        txtHours = new JTextField();
        txtDays = new JTextField();
        txtRate = new JTextField();
        txtDiscount = new JTextField("0");

        p.add(new JLabel("Workers:")); p.add(txtWorkers);
        p.add(new JLabel("Hours/Day:")); p.add(txtHours);
        p.add(new JLabel("Days:")); p.add(txtDays);
        p.add(new JLabel("Hourly Rate:")); p.add(txtRate);
        p.add(new JLabel("Discount %:")); p.add(txtDiscount);

        return p;
    }

    private JPanel costPanel() {
        JPanel p = new JPanel(new GridLayout(3,2));
        p.setBorder(BorderFactory.createTitledBorder("Costs"));

        lblMaterialCost = new JLabel("$");
        lblLaborCost = new JLabel("$");
        lblTotalCost = new JLabel("$");

        p.add(new JLabel("Material Cost:")); p.add(lblMaterialCost);
        p.add(new JLabel("Labor Cost:")); p.add(lblLaborCost);
        p.add(new JLabel("Total Cost:")); p.add(lblTotalCost);

        return p;
    }

    private JPanel historyPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Saved Estimates (CSV)"));

        txtHistory = new JTextArea(8,40);
        txtHistory.setEditable(false);

        p.add(new JScrollPane(txtHistory), BorderLayout.CENTER);
        return p;
    }

    private JPanel buttonPanel() {
        JPanel p = new JPanel();

        JButton calc = new JButton("Calculate & Save");
        JButton print = new JButton("Print Estimate");

        calc.addActionListener(e -> calculate());
        print.addActionListener(e -> printEstimate());

        p.add(calc);
        p.add(print);

        return p;
    }

    private void calculate() {
        try {
            double L = Double.parseDouble(txtLength.getText());
            double W = Double.parseDouble(txtWidth.getText());
            double T = Double.parseDouble(txtThickness.getText())/12;

            double volume = (L*W*T)/27 * 1.1;
            double materialCost = volume * 180;

            double workers = Double.parseDouble(txtWorkers.getText());
            double hours = Double.parseDouble(txtHours.getText());
            double days = Double.parseDouble(txtDays.getText());
            double rate = Double.parseDouble(txtRate.getText());

            double laborCost = workers * hours * days * rate;

            double discount = Double.parseDouble(txtDiscount.getText());
            double total = (materialCost + laborCost) * (1 - discount/100);

            lblMaterialCost.setText("$" + DF.format(materialCost));
            lblLaborCost.setText("$" + DF.format(laborCost));
            lblTotalCost.setText("$" + DF.format(total));

            saveProjectCSV(materialCost, laborCost, total);
            saveLastLabor();
            loadHistory();

        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input");
        }
    }

    private void saveProjectCSV(double mat, double labor, double total) {
        try(PrintWriter pw = new PrintWriter(new FileWriter("projects.csv", true))) {
            pw.println(txtProject.getText()+","+
                       txtLocation.getText()+","+
                       DF.format(mat)+","+
                       DF.format(labor)+","+
                       DF.format(total));
        } catch(Exception e) {}
    }

    private void loadHistory() {
        try(BufferedReader br = new BufferedReader(new FileReader("projects.csv"))) {
            txtHistory.setText("");
            String line;
            while((line = br.readLine()) != null) {
                txtHistory.append(line + "\n");
            }
        } catch(Exception e) {
            txtHistory.setText("No saved projects yet.");
        }
    }

    private void printEstimate() {
        String report = "PROJECT ESTIMATE\n\n" +
                "Project: " + txtProject.getText() + "\n" +
                "Location: " + txtLocation.getText() + "\n\n" +
                "Material Cost: " + lblMaterialCost.getText() + "\n" +
                "Labor Cost: " + lblLaborCost.getText() + "\n" +
                "Total Cost: " + lblTotalCost.getText() + "\n";

        try {
            PrintWriter out = new PrintWriter("estimate.txt");
            out.println(report);
            out.close();
            JOptionPane.showMessageDialog(this, "Estimate saved to estimate.txt");
        } catch(Exception e) {}
    }

    private void saveLastLabor() {
        try(PrintWriter pw = new PrintWriter(new FileWriter("labor.csv"))) {
            pw.println(txtWorkers.getText()+","+txtHours.getText()+","+txtDays.getText()+","+txtRate.getText());
        } catch(Exception e) {}
    }

    private void loadLastLabor() {
        try(BufferedReader br = new BufferedReader(new FileReader("labor.csv"))) {
            String[] data = br.readLine().split(",");
            txtWorkers.setText(data[0]);
            txtHours.setText(data[1]);
            txtDays.setText(data[2]);
            txtRate.setText(data[3]);
        } catch(Exception e) {}
    }
}
