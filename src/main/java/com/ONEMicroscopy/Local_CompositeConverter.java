package com.ONEMicroscopy;

/**
 * -------------------------------------------------------------------------------------------
 * This class is taken from ImageJ library and adapted locally to implement
 * the Make Composite command for 5D hyperstack. 
 * 
 * @author Abed Chouaib
 * @version 1.0.0
 * -------------------------------------------------------------------------------------------
 */
import java.awt.Point;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.macro.Interpreter;
import ij.process.ColorProcessor;

public class Local_CompositeConverter
{

	public void StartConverting(ImagePlus imp)
	{
		String[] modes = { "Composite", "Color", "Grayscale" };
		if (imp.isComposite())
		{
			CompositeImage ci = (CompositeImage) imp;
			if (ci.getMode() != IJ.COMPOSITE)
			{
				ci.setMode(IJ.COMPOSITE);
				ci.updateAndDraw();
			}
			return;
		}
		String mode = modes[0];
		int z = imp.getStackSize();
		int c = imp.getNChannels();
		if (c == 1)
		{
			c = z;
			imp.setDimensions(c, 1, 1);
			if (c > 7)
				mode = modes[2];
		}
		if (imp.getBitDepth() == 24)
		{
			ImageWindow win = imp.getWindow();
			Point loc = win != null ? win.getLocation() : null;
			ImagePlus imp2 = makeComposite(imp);
			if (loc != null)
				ImageWindow.setNextLocation(loc);
			imp2.show();
			imp.changes = false;
			if (z == 1)
			{
				imp.hide();
				WindowManager.setCurrentWindow(imp2.getWindow());
			}

			if (IJ.isMacro() && !Interpreter.isBatchMode())
				IJ.wait(500);
		} else if (c >= 2 || (IJ.macroRunning() && c >= 1))
		{
			GenericDialog gd = new GenericDialog("Make Composite");
			gd.addChoice("Display Mode:", modes, mode);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			int index = gd.getNextChoiceIndex();
			CompositeImage ci = new CompositeImage(imp, index + 1);
			if (imp.getBitDepth() != 8)
			{
				ci.reset();
				ci.resetDisplayRanges();
			}
			ImageWindow win = imp.getWindow();
			Point location = win != null ? win.getLocation() : null;
			imp.hide();
			if (location != null)
				ImageWindow.setNextLocation(location);
			if (IJ.isMacro())
				IJ.wait(250);
			ci.show();
		} else
			IJ.error("To create a composite, the current image must be\n a stack with at least 2 channels or be in RGB format.");
	}

	public static ImagePlus makeComposite(ImagePlus imp)
	{
		if (imp.getBitDepth() == 24)
			return convertRGBToComposite(imp);
		else
			return null;
	}

	private static ImagePlus convertRGBToComposite(ImagePlus imp)
	{
		if (imp.getBitDepth() != 24)
			throw new IllegalArgumentException("RGB image or stack required");
		if (imp.getStackSize() == 1)
			return new CompositeImage(imp, IJ.COMPOSITE);
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageStack stack1 = imp.getStack();
		int n = stack1.getSize();
		ImageStack stack2 = new ImageStack(width, height);
		for (int i = 0; i < n; i++)
		{
			ColorProcessor ip = (ColorProcessor) stack1.getProcessor(1);
			stack1.deleteSlice(1);
			byte[] R = new byte[width * height];
			byte[] G = new byte[width * height];
			byte[] B = new byte[width * height];
			ip.getRGB(R, G, B);
			stack2.addSlice(null, R);
			stack2.addSlice(null, G);
			stack2.addSlice(null, B);
		}
		n *= 3;
		ImagePlus imp2 = new ImagePlus(imp.getTitle(), stack2);
		imp2.setDimensions(3, n / 3, 1);
		imp2 = new CompositeImage(imp2, IJ.COMPOSITE);
		return imp2;
	}

}
