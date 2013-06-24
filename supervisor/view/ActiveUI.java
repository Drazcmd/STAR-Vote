/**
  * This file is part of VoteBox.
  * 
  * VoteBox is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 3 as published by
  * the Free Software Foundation.
  * 
  * You should have received a copy of the GNU General Public License
  * along with VoteBox, found in the root of any distribution or
  * repository containing all or part of VoteBox.
  * 
  * THIS SOFTWARE IS PROVIDED BY WILLIAM MARSH RICE UNIVERSITY, HOUSTON,
  * TX AND IS PROVIDED 'AS IS' AND WITHOUT ANY EXPRESS, IMPLIED OR
  * STATUTORY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF
  * ACCURACY, COMPLETENESS, AND NONINFRINGEMENT.  THE SOFTWARE USER SHALL
  * INDEMNIFY, DEFEND AND HOLD HARMLESS RICE UNIVERSITY AND ITS FACULTY,
  * STAFF AND STUDENTS FROM ANY AND ALL CLAIMS, ACTIONS, DAMAGES, LOSSES,
  * LIABILITIES, COSTS AND EXPENSES, INCLUDING ATTORNEYS' FEES AND COURT
  * COSTS, DIRECTLY OR INDIRECTLY ARISING OUR OF OR IN CONNECTION WITH
  * ACCESS OR USE OF THE SOFTWARE.
 */

package supervisor.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;

import printer.Printer;
import supervisor.model.AMachine;
import supervisor.model.Model;
import votebox.AuditoriumParams;

/**
 * The view that is shown on a supervisor that is active - consists of
 * information about the election, and a grid of all of the machines on the
 * network.
 * @author cshaw
 */
@SuppressWarnings("serial")
public class ActiveUI extends JPanel {

    private Model model;

    private MachineViewGenerator viewGen;

    private JPanel leftPanel;

    private JLabel timeLbl;

    private JLabel pollsOpenLbl;

    private JButton leftButton;

    private JButton ballotButton;

    private JButton pinButton;

    private JButton spoilButton;

    private JPanel mainPanel;

    private JFileChooser ballotLocChooser;


    /**
     * Constructs a new ActiveUI
     * @param m the supervisor's model
     */
    public ActiveUI(Model m) {
        model = m;
        viewGen = new MachineViewGenerator();
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        initializeLeftPanel();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(leftPanel, c);

        initializeMainPanel();
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        add(mainPanel, c);

        model.registerForMachinesChanged(new Observer() {
            public void update(Observable o, Object arg) {
                updateAllMachineViews();
            }
        });
    }

    /**
     * Turns on antialiasing
     */
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g);
    }

    /**
     * Updates the view of the list of machines (adding new machines as
     * necessary), and then updates each machine's view
     */
    public void updateAllMachineViews() {
        mainPanel.removeAll();
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        int i = 0;
        for (AMachine m : model.getMachines()) {
            c.gridx = i % 4;
            c.gridy = i / 4;
            innerPanel.add(viewGen.generateView(model, this, m), c);
            ++i;
        }

        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(innerPanel, c);
        validate();
        repaint();
    }

    private void initializeLeftPanel() {
        leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JPanel leftLabelPanel = new JPanel();
        leftLabelPanel.setLayout(new GridBagLayout());
        leftLabelPanel.add(new MyJLabel(model.getParams().getElectionName()), c);
        Date d = new Date();
        c.gridy = 1;
        leftLabelPanel.add(new MyJLabel(DateFormat.getDateInstance(
                DateFormat.LONG).format(d)), c);
        c.gridy = 2;
        timeLbl = new MyJLabel(DateFormat.getTimeInstance(DateFormat.LONG)
                .format(d));
        leftLabelPanel.add(timeLbl, c);
        new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeLbl.setText(DateFormat.getTimeInstance(DateFormat.LONG)
                        .format(new Date()));
            }
        }).start();
        c.gridy = 3;
        c.weighty = .2;
        c.anchor = GridBagConstraints.PAGE_END;
        pollsOpenLbl = new MyJLabel("Polls currently closed");
        leftLabelPanel.add(pollsOpenLbl, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.ipady = 20;
        c.insets = new Insets(20, 20, 80, 20);
        leftPanel.add(leftLabelPanel, c);

        ballotLocChooser = new JFileChooser("");
        ballotLocChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String path = f.getAbsolutePath();
                return (f.isDirectory() || path.length() > 4
                        && path.substring(path.length() - 4).equals(".zip"));
            }

            @Override
            public String getDescription() {
                return "Ballot ZIP files";
            }
        });

        ballotButton = new MyJButton("Select Ballot");
        ballotButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int answer = ballotLocChooser.showOpenDialog(ActiveUI.this);
                if (answer == JFileChooser.APPROVE_OPTION) {
                    model.addBallot(ballotLocChooser.getSelectedFile());
                }
            }
        });
        c.ipady = 50;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(20, 20, 20, 20);
        leftPanel.add(ballotButton, c);

        final JPanel fthis = this;

        pinButton = new MyJButton("Generate Pin");
        pinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(model.getSelections().length>0){
                    int precinct = (Integer) JOptionPane.showInputDialog(fthis, "Please choose a precinct", " Pin Generator",
                            JOptionPane.QUESTION_MESSAGE, null, model.getSelections(), model.getInitialSelection());
                    int pin = model.generatePin(precinct);
                    Printer printer = new Printer();
                    String strPin;
                    printer.printPin(strPin = (new DecimalFormat("0000")).format(pin));
                    JOptionPane.showMessageDialog(fthis, "Your pin is: " + strPin);
                }
                else{
                    JOptionPane.showMessageDialog(fthis, "Please select at least one ballot before generating a pin");
                }
            }
        });
        c.ipady = 50;
        c.gridy = 2;
        leftPanel.add(pinButton, c);

        spoilButton = new MyJButton("Spoil Ballot");
        spoilButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Scanner scanner = new Scanner(System.in);
                boolean entered = false;

                String bid = "-1";

                Object[] options = { "Type in a BID", "Scan a BID", "Cancel"};
                int scan = JOptionPane.showOptionDialog(fthis, "Would you like to scan or enter in the BID of the ballot to be spoiled?",
                        "Spoil Ballot", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
//                JFrame f = new JFrame("Spoil a BID");
//                JPanel p = new JPanel();
//                JLabel label = new JLabel("Please scan a BID");
//                JTextArea a = new JTextArea(1, 30);
//                p.add(label);
//                p.add(a);
//                label.setEnabled(true);
//                a.setEnabled(true);
//                f.add(p);
//                p.setEnabled(true);
//                f.pack();
//                f.setVisible(true);
//
//                if(scanner.hasNextLine()){
//                    a.setText(scanner.nextLine());
//                    a.repaint();
//                    entered = true;
//                }



                if(scan == 0){
                    bid = JOptionPane.showInputDialog(fthis, "Please enter the ballot ID of the ballot you would like to spoil.", "Spoil Ballot",
                        JOptionPane.QUESTION_MESSAGE);
                    entered = true;
                }
                else if(scan == 1){
                    JFrame f = new JFrame("BID Scan");
                    JDialog d = new JDialog(f, "", Dialog.ModalityType.MODELESS);
                    JPanel p = new JPanel();
                    p.setLayout(new FlowLayout());
                    BufferedImage img = null;
                    try{
                        img = ImageIO.read(new File("images/logo.png"));
                    } catch (IOException ie){
                        throw new RuntimeException(ie);
                    }


                    JLabel l = new JLabel(new ImageIcon(img));
                    l.setEnabled(true);
                    f.add(p);
                    p.add(l);
                    f.pack();
                    f.setVisible(true);



                    if(scanner.hasNext()){
                        d.dispose();
                        bid = scanner.nextLine();
                        int choice = JOptionPane.showConfirmDialog(fthis, "You scanned ballot " + bid + ". Is this the ballot you would like to spoil?", "Spoil Confirm",
                                JOptionPane.YES_NO_OPTION);

                        entered = true;

                        if(choice == 1)
                            entered = false;


                    }

                }


                if(entered){
                    boolean spoiled = model.spoilBallot(bid);
                    if(spoiled)
                        JOptionPane.showMessageDialog(fthis, "Ballot " + bid + " has been spoiled.");
                    else
                        JOptionPane.showMessageDialog(fthis, bid + " is not a valid ballot ID. No ballot was spoiled.");
                }

            }
        });

        c.ipady = 50;
        c.gridy = 3;
        leftPanel.add(spoilButton, c);

        leftButton = new MyJButton("Open Polls Now");
        leftButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                leftButtonPressed();
            }
        });
        model.registerForPollsOpen(new Observer() {
            public void update(Observable o, Object arg) {
                if (model.isPollsOpen()) {
                    pollsOpenLbl.setText("Polls currently open");
                    leftButton.setText("Close Polls Now");
                } else {
                    pollsOpenLbl.setText("Polls currently closed");
                    leftButton.setText("Open Polls Now");
                }
                updateAllMachineViews();
            }
        });
        c.ipady = 100;
        c.gridy = 4;
        leftPanel.add(leftButton, c);


    }

    private void initializeMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0,
                Color.BLACK));
        mainPanel.setLayout(new GridBagLayout());
        updateAllMachineViews();
    }

    /**
     * Called when the left button is pressed; toggles the polls open status
     */
    private void leftButtonPressed() {
        if (model.isPollsOpen()) {
            Map<String, BigInteger> tally = model.closePolls();
            JDialog tallyDlg = new TallyDialog(this, tally, ballotLocChooser.getSelectedFile()
                    .getAbsolutePath());
            tallyDlg.setVisible(true);
        } else
            model.openPolls();
    }

}
