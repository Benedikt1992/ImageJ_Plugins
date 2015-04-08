import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;

import ij.plugin.*;
import ij.plugin.frame.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;


public class AlphaBlending_Plugin_ implements PlugInFilter {

    private ImagePlus fgIm;
    private int nFrames = 3;

    public void run(ImageProcessor bgIp) {
    	int w=1,h=1;
        if (runDialog()) {
                 w = bgIp.getWidth();
                 h = bgIp.getHeight();
		}
            ImageProcessor fgIp = fgIm.getProcessor().convertToByte(false);
            ImageProcessor fgTmpIp = bgIp.duplicate();
            ImagePlus movie = NewImage.createByteImage("Movie", w, h, nFrames, 0);
            ImageStack stack = movie.getStack();
            for (int i = 0; i < nFrames; i++) {
                double iAlpha = 1.0 - (double) i / (nFrames - 1);
                ImageProcessor iFrame = stack.getProcessor(i + 1);
                iFrame.insert(bgIp, 0, 0);
                iFrame.multiply(iAlpha);
                fgTmpIp.insert(fgIp, 0, 0);
                fgTmpIp.multiply(1 - iAlpha);
                ByteBlitter blitter = new ByteBlitter((ByteProcessor) iFrame);
                blitter.copyBits(fgTmpIp, 0, 0, Blitter.ADD);
            }
            movie.show();
        }
    

    boolean runDialog() {
        int[] windowList = WindowManager.getIDList();
        if (windowList == null) {
            IJ.noImage();
            return false;
        }
        String[] windowTitles = new String[windowList.length];
        for (int i = 0; i < windowList.length; i++) {
            ImagePlus imp = WindowManager.getImage(windowList[i]);
            if (imp != null)
                windowTitles[i] = imp.getShortTitle();
            else
                windowTitles[i] = "untitled";
        }
        GenericDialog gd = new GenericDialog("Alpha Blending");
        gd.addChoice("Foreground image:",
                windowTitles, windowTitles[0]);
        gd.addNumericField("Frames:", nFrames, 0);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        else {
            int img2Index = gd.getNextChoiceIndex();
            fgIm = WindowManager.getImage(windowList[img2Index]);
            nFrames = (int) gd.getNextNumber();
            if (nFrames < 3)
                nFrames = 3;
            return true;
        }
    }

	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		return 0;
	}
}