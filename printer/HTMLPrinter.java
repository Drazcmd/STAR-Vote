package printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class which provides tools to write images to HTML files. Might be useful in printing higher quality images.
 * @author Mircea Berechet
 */
public class HTMLPrinter {

    /**
     * Generates a HTML file that will be used to print a voter's selections.
     * @param filename - The name of the HTML file to be written
     * @param useTwoColumns - Whether or not to use two columns of images per page
     * @param pathToBallotVVPATFolder - The path to the vvpat folder in the ballot files
     * @param imageNames - ArrayLists of file names for images. One ArrayList per column.
     */
    public static void generateHTMLFile (String filename, Boolean useTwoColumns, String pathToBallotVVPATFolder, ArrayList<String>... imageNames)
    {
        System.out.println("Attempting to create an html file at " + filename);
        File file = new File(filename);

        // If the file does not exist, then create it.
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                System.out.println("HTML File Generator Error: Unable to create file '" + filename + "'");
            }
        }

        // Create the writer.
        BufferedWriter writer = null;
        try
        {
        writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        }
        catch (IOException e)
        {
            System.out.println("HTML File Generator Error: Unable to create BufferedWriter for file '" + filename + "'");
        }


        // Actually write the HTML file.
        try
        {
            // Beginning of the file.
            writer.write("<html>\n");

            // Beginning of head.
            writer.write("<head>\n");
            // End of head.
            writer.write("</head>\n");

            // Beginning of body.
            writer.write("<body bgcolor = \"#FFFFFF\" text = \"#000000\">\n");

            // Writes the header of the page(s) to be printed.
            writer.write("<h2>Rice University Demo Election</h2>\n");


            if (!useTwoColumns) // Ballot printing uses one-column format
            {
                generatorHelperForOneColumn(writer, pathToBallotVVPATFolder, imageNames);
            }
            else // Ballot printing uses two-column format
            {
                generatorHelperForTwoColumns(writer, pathToBallotVVPATFolder, imageNames);
            }

            // End of body.
            writer.write("</body>\n");

            // End of file.
            writer.write("</html>\n");

            writer.flush();
            writer.close();
            System.out.println("It should have generated an html file.");
        }
        catch (IOException e)
        {
            System.out.println("HTML File Generator Error: Unable to write to file '" + filename + "'");
        }

    }

    /**
     * Creates a "page" that contains two columns of images.
     * @param writer - The writer to the HTML file
     * @param pathToBallotVVPATFolder - The path to the vvpat folder in the ballot files
     * @param imageNames - ArrayLists of file names for images. One ArrayList per column.
     */
    private static void generatorHelperForTwoColumns (BufferedWriter writer, String pathToBallotVVPATFolder, ArrayList<String>... imageNames)
    {
        try
        {
            // Creates the container for the columns of images.
            writer.write("<div id = \"container\" style = \"background-color:#CCFF00;width:750px;height:792px;\">\n");

            // Creates the left and right margins.
            writer.write("<div id = \"left_margin\" style = \"background-color:#000000;width:72px;float:left\"><br></div>\n");
            writer.write("<div id = \"right_margin\" style = \"background-color:#000000;width:72px;float:right\"><br></div>\n");

            // Left Column //////////////////////////////////////////////////////////////////////////////////////////////////////
            // Create the left column.
            writer.write("<div id = \"left_column\" style=\"background-color:#ABCDEF;width:303px;float:left;\">\n");

            // Put images in the left column.
            ArrayList<String> left_column = imageNames[0];
            Boolean isSelectionImage = false; // Used to leave an empty line after every selectionImage
            for (String imageName : left_column)
            {
                // Load in the image.
                writer.write("<img src = \"" + pathToBallotVVPATFolder + imageName + "\" alt = \"Image did not load properly\" width = \"250\">\n");
                // Leave an empty line after selection images.
                if (isSelectionImage)
                {
                    writer.write("<br>\n<br>\n");
                }
                // Set the flag for selection images and reset it for label/title images.
                isSelectionImage = !isSelectionImage;
            }

            // End of the left column.
            writer.write("</div>\n");
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // Right Column /////////////////////////////////////////////////////////////////////////////////////////////////////
            // Create the right column.
            writer.write("<div id = \"right_column\" style=\"background-color:#FEDCBA;width:303px;float:right;\">\n");

            // Put images in the right column.
            // It might be the case that there is an odd number of columns. Check if imageNames[1] throws an IndexOutOfBounds Exception.
            try
            {
                ArrayList<String> right_column = imageNames[1];
                isSelectionImage = false; // Used to leave an empty line after every selectionImage
                for (String imageName : right_column)
                {
                    // Load in the image.
                    writer.write("<img src = \"" + pathToBallotVVPATFolder + imageName + "\" alt = \"Image did not load properly\" width = \"303\">\n");
                    // Leave an empty line after selection images.
                    if (isSelectionImage)
                    {
                        writer.write("<br>\n<br>\n");
                    }
                    // Set the flag for selection images and reset it for label/title images.
                    isSelectionImage = !isSelectionImage;
                }
            }
            catch (IndexOutOfBoundsException e)
            {
                System.out.println("HTML File Generator Exception Caught: IndexOutOfBoundsException: Found an odd number of columns when trying to print in two-columns format.");
            }

            // End of the right column.
            writer.write("</div>\n");
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // End of the container for the columns.
            writer.write("</div>\n");
        }
        catch (IOException e)
        {
            System.out.println("HTML File Generator Error: Unable to write to file.");
        }

        // If there are more columns to be printed, call this helper again on the rest of the columns.
        if (imageNames.length > 2)
        {
            generatorHelperForTwoColumns(writer, pathToBallotVVPATFolder, Arrays.copyOfRange(imageNames, 2, imageNames.length));
        }
    }

    /**
     * Creates a "page" that contains one column of images.
     * @param writer - The writer to the HTML file
     * @param pathToBallotVVPATFolder - The path to the vvpat folder in the ballot files
     * @param imageNames - ArrayLists of file names for images. One ArrayList per column.
     */
    private static void generatorHelperForOneColumn (BufferedWriter writer, String pathToBallotVVPATFolder, ArrayList<String>... imageNames)
    {
        try
        {
            // Creates the container for the column of images.
            writer.write("<div id = \"container\" style = \"background-color:#CCFF00;width:750px;height:792px;\">\n");

            // Creates the left and right margins.
            writer.write("<div id = \"left_margin\" style = \"background-color:#000000;width:72px;float:left\"><br></div>\n");
            writer.write("<div id = \"right_margin\" style = \"background-color:#000000;width:72px;float:right\"><br></div>\n");

            // Column ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Create the column.
            writer.write("<div id = \"column\" style=\"background-color:#ABCDEF;width:606px;float:left;\">\n");

            // Put images in the column.
            ArrayList<String> column = imageNames[0];
            Boolean isSelectionImage = false; // Used to leave an empty line after every selectionImage
            for (String imageName : column)
            {
                // Load in the image.
                writer.write("<img src = \"" + pathToBallotVVPATFolder + imageName + "\" alt = \"Image did not load properly\" width = \"606\">\n");
                // Leave an empty line after selection images.
                if (isSelectionImage)
                {
                    writer.write("<br>\n<br>\n");
                }
                // Set the flag for selection images and reset it for label/title images.
                isSelectionImage = !isSelectionImage;
            }

            // End of the column.
            writer.write("</div>\n");
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // End of the container for the column.
            writer.write("</div>\n");
        }
        catch (IOException e)
        {
            System.out.println("HTML File Generator Error: Unable to write to file.");
        }

        // If there are more columns to be printed, call this helper again on the rest of the columns.
        if (imageNames.length > 1)
        {
            generatorHelperForTwoColumns(writer, pathToBallotVVPATFolder, Arrays.copyOfRange(imageNames, 1, imageNames.length));
        }
    }
}

