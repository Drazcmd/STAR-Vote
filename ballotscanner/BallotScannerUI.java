package ballotscanner;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: mrdouglass95
 * Date: 6/17/13
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class BallotScannerUI extends JFrame {

    private JPanel electionInfoPanel;
    private JPanel userInfoPanel;

    private ImageIcon logo;
    private ImageIcon acceptedIcon;
    private ImageIcon rejectedIcon;

    private JLabel logoLabel;
    private JLabel scanResultLabel;
    private JLabel dateLabel;

    private DateFormat dateFormat = new SimpleDateFormat("MMMM d, y");
    private Date date = new Date();

    public BallotScannerUI(){
        super("STAR-Vote Ballot Scanner");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600,600);
        setResizable(false);
        setLocationRelativeTo(null);
        setLocation((int)Math.round(getLocation().getX()) - getWidth()/2,
                    (int)Math.round(getLocation().getY()) - getHeight()/2);

        try{
            logo = new ImageIcon(ImageIO.read(new File("images/logo.png")));
            logoLabel = new JLabel(logo);
        }catch(IOException ioe){
            System.out.println("BallotScannerUI: Could not locate logo Icon");
            logoLabel =  new JLabel("Votebox");
        }

        try{
            acceptedIcon = new ImageIcon(ImageIO.read(new File("images/logo.png")));

        }catch(IOException ioe){
            System.out.println("BallotScannerUI: Could not locate logo Icon");
        }

        try{rejectedIcon = new ImageIcon(ImageIO.read(new File("images/logo.png")));}
        catch(IOException ioe){System.out.println("BallotScannerUI: Could not locate logo Icon");}

        userInfoPanel = new JPanel();
        userInfoPanel.add(new JLabel("Welcome to the Ballot Scanning Console"));
        electionInfoPanel.add(new JLabel("To cast your vote, place your barcode under the scanner"));
    }

    public void dislayInitial

    /*try{
        logo = new ImageIcon(ImageIO.read(new File("images/logo.png")));
    } catch(IOException e) {
        logo = null;
        System.out.println("BallotScannerUI: Logo Icon could not be loaded!");
        new RuntimeException(e);
    }



    JPanel panel = new JPanel();
    panel.setPreferredSize(new Dimension(600, 600));
    JLabel image = new JLabel(logo);
    panel.add(image);
    panel.add(new JLabel("Please scan your ballot"));
    panel.add(new JLabel(dateFormat.format(date)


    ));
    frame.add(panel);
    frame.pack();
    frame.setVisible(true);*/


}
