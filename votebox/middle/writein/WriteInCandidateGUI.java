package votebox.middle.writein;

import printer.PrintImageUtils;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Author: Mircea C. Berechet
 * Added to project: 07/18/2013
 */

/**
 * A graphical user interface that allows the voter to type in a candidate's name.
 * Based on the name that the voter enters, it renders an image that display the candidate's name
 * and adds it to the ballot files.
 */
public class WriteInCandidateGUI extends JFrame {

    /* The width of the drawable/viewable space on the screen. */
    private static final int GUI_WIDTH = 800;
    /* The height of the drawable/viewable space on the screen. */
    private static final int GUI_HEIGHT = 320;
    /* The path to the directory that contains the images. */
    public static final String SLASH = System.getProperty("file.separator");
    public static final String pathToImages = "images" + SLASH + "writein" + SLASH;
    /* The standard size of the character images. */
    public static final int IMAGE_STANDARD_WIDTH = 14;
    public static final int IMAGE_STANDARD_HEIGHT = 14;
    /* The size of the canvas. */
    public static final int CANVAS_WIDTH = 700;
    public static final int CANVAS_HEIGHT = 210;
    /* The location of the upper left corner of the next image to be drawn. */
    public static int nextUpperLeftX = 0;
    public static int nextUpperLeftY = 0;
    /* STAR-Vote colors. */
    private static final Color STAR_VOTE_BLUE = new Color (48, 149, 242);
    private static final Color STAR_VOTE_PINK = Color.PINK;

    /* The UID of the write-in candidate whose name will be entered in this GUI prompt. */
    private String CANDIDATE_UID;
    /* The type of the write-in candidate. */
    private String CANDIDATE_TYPE;

    private JTextField candidateNameTextField;
    private JPanel candidateNamePanel;

    /**
     * Start displaying the GUI.
     */
    public void start ()
    {
        setVisible(true);
    }

    /**
     * Stop displaying the GUI.
     */
    public void stop ()
    {
        setVisible(false);
        dispose();
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    WriteInCandidateGUI frame = new WriteInCandidateGUI(680, 384, "Z22", "Regular");
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the GUI and build its GUI Elements.
     * @param cX the x-coordinate of the center of the GUI
     * @param cY the y-coordinate of the center of the GUI
     */
    public WriteInCandidateGUI(int cX, int cY, String uid, String guiType) {
        // Set Frame properties.
        setTitle("Type in Candidate Name");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setBounds(cX - GUI_WIDTH / 2, cY - GUI_HEIGHT / 2, GUI_WIDTH, GUI_HEIGHT);

        // Set the UID.
        CANDIDATE_UID = uid;
        // Set the TYPE.
        CANDIDATE_TYPE = guiType;

        // Build GUI Elements.
        buildGUIElements();
    }

    /**
     * Builds all GUI Elements of the Write-In-Candidate GUI.
     */
    private void buildGUIElements ()
    {
        /*
		 * CONTENT PANE
		 */
        JPanel contentPane = new JPanel();
        contentPane.setBackground(STAR_VOTE_BLUE);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(STAR_VOTE_BLUE);
        contentPane.add(mainPanel, BorderLayout.CENTER);

        candidateNameTextField = new JTextField();

        JLabel enterNameLabel = new JLabel("Enter your preferred " + CANDIDATE_TYPE + " candidate's name (" + CANDIDATE_UID + "):");
        mainPanel.add(enterNameLabel);
        mainPanel.add(candidateNameTextField);
        candidateNameTextField.setColumns(10);

        candidateNamePanel = new JPanel();
        candidateNamePanel.setBackground(Color.WHITE);
        candidateNamePanel.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
        mainPanel.add(candidateNamePanel);
        candidateNamePanel.setLayout(null);

        JButton submitAndStopButton = new JButton("Submit name and close editor");
        mainPanel.add(submitAndStopButton);

        /*
         * Listeners for events.
         */
        /*
           Listens for an Enter key being pressed. It builds and displays a candidate name based
           on the text that was entered in the text field.
         */
        candidateNameTextField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent arg0) {
                if(arg0.getKeyCode() == KeyEvent.VK_ENTER)
                {
					/* Clear the canvas panel. */
                    Graphics g = candidateNamePanel.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                    nextUpperLeftX = 0;
                    nextUpperLeftY = 0;

                    /* Render the candidate's name. */
                    BufferedImage canvas = renderCandidateName(candidateNameTextField.getText());

					/* Draw the canvas into the JPanel. */
                    g.drawImage(canvas, 0, 0, null);

                    /* Trim the image. */
                    canvas = PrintImageUtils.trimImageVertically(canvas, false, Integer.MAX_VALUE); // Above
                    canvas = PrintImageUtils.trimImageVertically(canvas, true, Integer.MAX_VALUE);  // Below
                    canvas = PrintImageUtils.trimImageHorizontally(canvas, false, Integer.MAX_VALUE); // Left
                    canvas = PrintImageUtils.trimImageHorizontally(canvas, true, Integer.MAX_VALUE); // Right

					/* Save the image to a file. */
                    File file = new File(pathToImages, "result.png");
                    try
                    {
                        ImageIO.write(canvas, "png", file);
                    }
                    catch (IOException e)
                    {
                        System.out.println("Canvas image creation failed!");
                        e.printStackTrace();
                    }
                }
            }
        });

        /*
           Listens for the submit and stop button being pressed. Once it is pressed, this should render
           the appropriate images and dispose of the frame.
         */
        submitAndStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO render the images for the candidate's current name
                stop();
            }
        });
    }

    /**
     * Creates a standardized image of the candidate's name.
     *
     * @param candidateName name of candidate
     * @return a BufferedImage representing the candidate's name
     */
    public BufferedImage renderCandidateName (String candidateName)
    {
		/* Create the canvas on which the images will be drawn. */
        BufferedImage canvas = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
		/* Add each image to the canvas. */
        for (String character: candidateName.split(""))
        {
            if (!character.equals(""))
            {
                String imageName = pathToImages + "W_" + character.toUpperCase() + ".png";
				/* Create a file, which will be used to read in the image. */
                File file = new File(imageName);
                try
                {
					/* Read the image. */
                    BufferedImage currentImage = ImageIO.read(file);
					/* Draw the image on the canvas. */
                    g.drawImage(currentImage, nextUpperLeftX, nextUpperLeftY, null);
					/* Update the coordinates of the location where the next image is to be drawn. */
                    nextUpperLeftX += IMAGE_STANDARD_WIDTH;
					/* Check if the end of the row has been reached. */
                    if (CANVAS_WIDTH - nextUpperLeftX < IMAGE_STANDARD_WIDTH)
                    {
						/* Go to the next row. */
                        nextUpperLeftY += IMAGE_STANDARD_HEIGHT;
						/* Reset the horizontal offset in the row. */
                        nextUpperLeftX = 0;
                    }
                }
                catch (IOException e) {
                    System.out.println("Character entered: " + character);
                    System.out.println("Trying to load file: " + imageName);
                    //e.printStackTrace();
                }
            }
        }
		/* Return the canvas. */
        return canvas;
    }
}
