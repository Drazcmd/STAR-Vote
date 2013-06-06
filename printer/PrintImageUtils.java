package printer;

import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import net.sourceforge.barbecue.output.OutputException;

import javax.imageio.ImageIO;
import javax.sound.midi.SysexMessage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * A class which provides image manipulation support for printing
 *
 * @author Matt Bernhard
 */
public class PrintImageUtils {

    /**
     * Trims the given image so that there is no trailing white/transparent block.
     *
     * @param image - Image to trim
     * @param trimFromEnd - If true, the image should have whitespace at the end removed instead of that at the front
     * @param maxToTrim - the maximum amount of whitespace to trim, necessary for uniform scaling later on
     * @return trimmed image
     *
     * This was taken from BallotImageHelper
     */

    public static BufferedImage trimImage(BufferedImage image, boolean trimFromEnd, int maxToTrim) {

        BufferedImage outImage;

        if(trimFromEnd){

            outImage = flipImage(image);

            outImage = trimImageHelper(outImage, maxToTrim);

            outImage = flipImage(outImage);

        }
        else{
            outImage = trimImageHelper(image, maxToTrim);
        }

        return outImage;
    }

    /**
     * A method which will invert an image with respect to its y-axis
     *
     * @param image - image to be flipped
     * @return - a flipped image
     */
    private static BufferedImage flipImage(BufferedImage image){
        BufferedImage flipped = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        AffineTransform tran = AffineTransform.getTranslateInstance(image.getWidth(), 0);
        AffineTransform flip = AffineTransform.getScaleInstance(-1d, 1d);
        tran.concatenate(flip);

        Graphics2D g = flipped.createGraphics();
        g.setTransform(tran);
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return flipped;

    }

    /**
     * A helper method which actually trims an image
     *
     * @param image - image to be trimmed
     * @param maxToTrim - the maximum whitespace that can be trimmed off this image
     * @return - a trimmed image
     */
    private static BufferedImage trimImageHelper(BufferedImage image, int maxToTrim){
        try{
            int[] pix = new int[image.getWidth() * image.getHeight()];
            PixelGrabber grab = new PixelGrabber(image, 0, 0, image.getWidth(), image.getHeight(), pix, 0, image.getWidth());
            if(!grab.grabPixels()) return image;

            int lastClearRow = 0;
            out:
            for(int x = 1; x < image.getWidth(); x++){
                for(int y = 0; y < image.getHeight(); y++){
                    int i = y*image.getWidth() + x;
                    int pixel = pix[i];

                    int alpha = (pixel >> 24) & 0xff;
                    int red   = (pixel >> 16) & 0xff;
                    int green = (pixel >>  8) & 0xff;
                    int blue  = (pixel      ) & 0xff;

                    if(alpha == 0) continue;
                    if(red == 255 && green == 255 && blue == 255) continue;

                    break out;
                }//for
                lastClearRow = x;
            }//for

            int trimmable = Math.min(lastClearRow, maxToTrim);

            return image.getSubimage(trimmable, 0, image.getWidth() - trimmable, image.getHeight());
        }catch(InterruptedException e){ return image; }
    }

    /**
     * A method that determines how much whitespace will be trimmed off an image
     *
     * @param image - image to be evaluated for trimming
     * @param flipped - whether or not the image is being trimmed from the end or beginning
     * @return the amount of whitespace that will be trimmed
     */
    public static int getImageTrim(BufferedImage image, boolean flipped){
        BufferedImage outImage;
        int whitespace = -1;

        if(flipped){

            outImage = flipImage(image);


            whitespace = getImageTrimHelper(outImage);

;
        }
        else{
            whitespace = getImageTrimHelper(image);
        }

        return whitespace;

    }

    /**
     * A method that determines how much whitespace will be trimmed off an image
     *
     * @param image - image to be trimmed
     * @return the amount of whitespace that will be trimmed
     */
    private static int getImageTrimHelper(BufferedImage image){
        try{
            int[] pix = new int[image.getWidth() * image.getHeight()];
            PixelGrabber grab = new PixelGrabber(image, 0, 0, image.getWidth(), image.getHeight(), pix, 0, image.getWidth());
            if(!grab.grabPixels()) return -1;

            int lastClearRow = 0;
            out:
            for(int x = 1; x < image.getWidth(); x++){
                for(int y = 0; y < image.getHeight(); y++){
                    int i = y*image.getWidth() + x;
                    int pixel = pix[i];

                    int alpha = (pixel >> 24) & 0xff;
                    int red   = (pixel >> 16) & 0xff;
                    int green = (pixel >>  8) & 0xff;
                    int blue  = (pixel      ) & 0xff;

                    if(alpha == 0) continue;
                    if(red == 255 && green == 255 && blue == 255) continue;

                    break out;
                }//for
                lastClearRow = x;

            }//for

            return lastClearRow;
        }catch(InterruptedException e){ return -1; }

    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     *
     * Taken from  https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
     */
    public static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * A method for generating a barcode of some text
     *
     * @param string - the string to be converted to barcode, will be a bid in this case
     * @return - an image representing the barcode to be drawn on a ballot
     */
    public static BufferedImage getBarcode(String string){
        try {
            Barcode bar = BarcodeFactory.createCode128(string);

            BufferedImage code = BarcodeImageHandler.getImage(bar);

            return code;


        } catch (BarcodeException e) {
            throw new RuntimeException(e);
        } catch (OutputException e) {
            throw new RuntimeException(e);
        }

    }
}
