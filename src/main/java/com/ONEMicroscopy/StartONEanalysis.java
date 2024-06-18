package com.ONEMicroscopy;


/**
 * -------------------------------------------------------------------------------------------
 * The ONE Microscopy is a Java-written software that utilizes Fiji app,
 * developed for Ali Shaib and Silvio Rizzoli, University Medical Center
 * Göttingen, Germany, by Abed Chouaib, University of Saarland, Homburg Saar,
 * Germany. Driver compatibility and library updates by Mohamad Mahdi Alawieh,
 * University Medical Center Göttingen, Germany.
 * This software is installed in the freeware Fiji app and provided
 * without any express or implied warranty. Permission for Everyone to copy,
 * modify and distribute verbatim copies of this software for any purpose
 * without a fee is hereby granted, provided that this entire notice is included
 * in all copies of any software which is or includes a copy or a modification
 * of ONE Platform. One Microscopy is licensed under the GNU General Public
 * License v3.0.
 * 
 * @authors Abed Chouaib and Mohamad Mahdi Alawieh
 * @version 1.0.0 
 * -------------------------------------------------------------------------------------------
 */
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.ONEMicroscopy.gui.InputData;
import com.ONEMicroscopy.gui.MainWindow;
import com.ONEMicroscopy.gui.ONEsetup;
import ij.*;


import org.apache.commons.io.FilenameUtils;


import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.frame.Editor;
import ij.process.ImageProcessor;

import loci.formats.FormatException;
import loci.plugins.BF;

public class StartONEanalysis implements PlugIn {
	private int channels = MainWindow.ChnNum;
	private int SRRF_MaxImgNum = MainWindow.EndFrame;
	private double RadMagnification = MainWindow.RadMag;
	private double AxesRing = MainWindow.RingAxes;
	private int TRAC_order = MainWindow.SRRForder;
	private double Distance = MainWindow.DistScale;
	private double KnownDist = MainWindow.KnownDist;
//	private String unit = MainWindow.Unit;
	private String AnalysisType = MainWindow.SRRFType;
	private int zSlices;
	private String InputDirPath = MainWindow.ImportDirPath;
	private String BeadsDirPath = MainWindow.BeadsDirPath;
	private String OutputDir;
	private String OutputBeadsDir;
//	private String BeadsImageName = "";
	private double ExpFactor = MainWindow.ExpFactor;
	private boolean jump = false;
	private boolean Calibrate = MainWindow.Calibrate;
	private ArrayList<String> StackDirectories = MainWindow.StackDirectories;
	private double ScaleNumber;
	private int SpecChn = MainWindow.SpecificChannel;
//	public final String MACRO_CANCELED = "Macro canceled";
	private boolean SaveParentFolder = MainWindow.SaveParentFolder;
	private String DefaultSavePath = MainWindow.OutPutDirectory;
	private int FPS = MainWindow.FPS;
	private int[] ChannelFlag;
	private int StartFromVideo = MainWindow.StartFromVideo;
	private int EndOnVideo = MainWindow.EndOnVideo;
	String OutputSRRF = "-OUTPUT";
//	String OutputCAC = "-CAC_OUTPUT";
	String CAC = "-CAC";
	private String OutName = "ONE";
	private ArrayList<String> LogFile = new ArrayList<String>();
	private boolean CreateLogFileOnce = false;
	// ============ Advance Settings ============
	private boolean integrate_temporal_correlations = MainWindow.integrate_temporal_correlations;
	private boolean radiality_RPC = MainWindow.Radiality_RPC, radiality_Renorm = MainWindow.Radiality_Renorm,
			radiality_DoGS = MainWindow.Radiality_DoGS;
	private boolean weighting_DoIW = MainWindow.Weighting_DoIW, weighting_DoGW = MainWindow.Weighting_DoGW,
			Minimize_SRRF_patterning = MainWindow.Minimize_SRRF_patterning;
	private double psf_fwhm = MainWindow.psf_fwhm;

	ColorModel[] ChnColor;

	String psf;
	String SRRForder;

	String IntTempCor, Radiality_RPC, Radiality_Renorm, Radiality_DoGS;
	String Weighting_DoIW, Weighting_DoGW, minSRRFPat;

	// ============ end ============
	private int resumeFile = -1;
	private boolean terminateThreads = false;
	private boolean lock = false, lockChromatic = false;
	private String CurrentImageName = "";
	// ************************** #Region Progress Bar **************************
	boolean Cancel = false;
	JFrame frame = new JFrame();
	JProgressBar progressBar = new JProgressBar();
	JLabel progressStatusLabel = new JLabel();
	boolean OneChannel = false;
	// ========================== #End Progress Bar ==========================

	public void run(String arg) {
		AdjustOption();
		LogFile.add("Options used:");
		LogFile.add("SRRF Type = " + AnalysisType + " -- " + SRRForder + " -- " + psf);
		LogFile.add(Radiality_RPC + Radiality_Renorm + Radiality_DoGS + Weighting_DoIW + Weighting_DoGW + minSRRFPat);
		LogFile.add("----------------------------------------------------------------");
		runAnalysis();
	}

	// ========================== ==========================
	private void runAnalysis() {
		OneChannel = MainWindow.ChnNum != 0 ? true : false;
		IJ.run("Close All");
		terminateThreads = false;
		Thread cpuLoadThread = new Thread(() -> {

		});

		cpuLoadThread.start();
		String BeadsFoldeName = "";
		// ************************** #Region System IO **************************
		if (Calibrate && !lock) {
			File BeadsFolder = new File(BeadsDirPath);
			if (SaveParentFolder) {
				BeadsFoldeName = BeadsFolder.getName();
				OutputBeadsDir = BeadsFolder.getParent() + File.separator + BeadsFoldeName + OutputSRRF
						+ File.separator;
			} else {
				BeadsFoldeName = BeadsFolder.getName();
				OutputBeadsDir = DefaultSavePath + File.separator + BeadsFoldeName + OutputSRRF + File.separator;
			}
			CreateDir(OutputBeadsDir);
			File[] BeadsList = BeadsFolder.listFiles();
			AdjustParameters(BeadsList, 0); // read metadata and calibrate it with user input.
			StartAnalysis(BeadsList, 0, OutputBeadsDir, true, false, 0);
		}
		File folder = new File(InputDirPath);
		if (SaveParentFolder) {
			OutputDir = folder.getParent() + File.separator + folder.getName() + OutputSRRF + File.separator;
		} else {
			OutputDir = DefaultSavePath + File.separator + folder.getName() + OutputSRRF + File.separator;
		}
		CreateDir(OutputDir);


		File[] FileList = GetFiles(StackDirectories);

		// ------------------- #Call Progress Bar Window -------------------
		if (!lock) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						ShowProgressBarWindow();
						int progressMax = EndOnVideo == 1 ? 1 : EndOnVideo;
						SetPrograssBarMax(progressMax);
						UpdateProgressBar(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		// ------------------- #Call Progress Bar Window -------------------
		// ========================== #End System IO ==========================

		// ************************** #Region Active loop **************************
		int startFileNum = resumeFile == -1 ? StartFromVideo : resumeFile;
		UpdateProgressBar(startFileNum);
		for (int i = startFileNum; i < EndOnVideo; i++) {
			if (!Cancel) {
				resumeFile = i + 1;
				AdjustParameters(FileList, i); // read metadata and user input.
				LogFile.add("------------------------------");
				LogFile.add("Video name " + FileList[i].getName());
				LogFile.add("parameters used: Distance = " + Distance + "  --  Ragdiality Magnification = "
						+ RadMagnification + " --  Expansion Factor " + ExpFactor);
				LogFile.add("  --  channels number  = " + channels + "  --  Delta Time " + FPS);
				if (zSlices == 1) {
					ChannelFlag = new int[channels];
//					TODO: Add Try Catch Statement to catch array out of bounds exception and terminate
					StartAnalysis(FileList, i, OutputDir, false, false, 0);
				} else {
					ChannelFlag = new int[zSlices];
					StartAnalysis5D(FileList, i, OutputDir, false, false, 0);
				}
			}
			LogFile.add("------------------------------");
			UpdateProgressBar(i);
		}
		if (!Cancel) {
			try {
				PrintLogFile(OutputDir, LogFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (resumeFile == EndOnVideo) {
				frame.dispose();
				terminateThreads = true;
				IJ.showMessage("Computation complete!");
			}
		}
		terminateThreads = true;

		// ========================== #End Active loop ==========================
	}
	// ========================== ==========================

	// ************************** **************************
	// ************************** #Region Methods **************************
	/**
	 * Start analysis method.
	 *
	 *
	 * @param FileList      array of files.
	 * @param i             loop number.
	 * @param Output        output directory link.
	 * @param GenerateLog   perform chromatic aberration if true.
	 * @param SwitchChannel automatically switch drift correction to another channel
	 *                      if the current fails.
	 * @param ChnPos        is used to determine the channel position to process.
	 */
	private void StartAnalysis(File[] FileList, int i, String Output, boolean GenerateLog, boolean SwitchChannel,
			int ChnPos) {
		jump = false;
		ChnColor = new ColorModel[channels];
		String Path = FileList[i].getPath();
		InputData iData = new InputData();
		ImagePlus[] OImgArray = iData.getImageBF(Path);


		// (width[0], height[1], nChannels[2],nSlices[3], nFrames[4]).
		int[] imDim = OImgArray[0].getDimensions(); // 0 get first series.

		if (imDim[4] == 1) // correct dimension order if frames = 1.
		{
			String Options = "order=xyczt(default) channels=" + channels + " slices=" + 1; // length instead of 1
			Options = Options + " frames=" + imDim[3];
			IJ.run(OImgArray[0], "Stack to Hyperstack...", Options);
			imDim = OImgArray[0].getDimensions();
		}

		ScaleNumber = Distance * ExpFactor * RadMagnification;
		ImagePlus[] OImg = new ImagePlus[imDim[2]];
		int start = MainWindow.ChnNum == 0 ? 0 : MainWindow.ChnNum - 1; // TODO:
		SpecChn = start == 0 ? SpecChn : 0;

		int counter = 0;
		for (int k = start; k < OImg.length; k++) {
			int chn = k + 1;
			if (chn <= channels) {
				OImg[counter] = new Duplicator().run(OImgArray[0], chn, chn, 1, 1, 1, imDim[4]);
				ChnColor[counter] = OImg[counter].getStack().getColorModel();
				counter++;
			}
		}
		String ImageName = FileList[i].getName();
		CurrentImageName = ImageName;
		String FileExt = GetFileExtension(ImageName);
		if (FileExt != null) {
			ImageName = ImageName.replace(FileExt, "");
		}
		String[] ChannelNames = new String[imDim[2]];
		// ------------------------- Creating Directories -------------------------
		String OutSubFolder = Output + ImageName + File.separator;
		CreateDir(OutSubFolder); // 2nd level subfolder
		String OutSRRF = OutSubFolder + OutName + File.separator;
		CreateDir(OutSRRF); // 3rd level subfolder
		String OutResults = OutSubFolder + ImageName + File.separator;
		CreateDir(OutResults); // 3rd level subfolder
		// ------------------------- end -------------------------

		ImagePlus[] ImgSRRF = new ImagePlus[imDim[2]];
		String DriftTablePath = "";
		if (SpecChn != 0) {
			int index = MainWindow.SpecificChannel == 0 ? 0 : SpecChn;
			DriftTablePath = GetFixedDriftTablePath(OImg, ImageName, ChannelNames, OutResults, OutSRRF, index);
		}
		// createHyperStack(title, width, height, channels, slices, frames, bitdepth);
//			ImagePlus HyperImg = IJ.createHyperStack(ImageName, (int) (imDim[0] * RadMagnification), (int) (imDim[1] * RadMagnification),
//					channels, 1, 1, 32);
		ImagePlus HyperImg = IJ.createImage(ImageName, "32-bit", (int) (imDim[0] * RadMagnification),
				(int) (imDim[1] * RadMagnification), channels, 1, 1);

		counter = 0;
		for (int k = start; k < imDim[2]; k++) {
			if (!Cancel) {
				int chn = k + 1;
				int currentChn = counter;
				// Channel reference if exists
				if (chn <= channels) {
					ChannelNames[counter] = ImageName + "_" + chn;
					OImg[counter].setTitle(ChannelNames[counter]);
					String[] PathAndName = { ChannelNames[counter], OutResults, OutSRRF };
					if (SpecChn == 0) {
						if (SwitchChannel) {
							currentChn = GetCurrentChannel(ChannelFlag);
							LogFile.add("Drift reference channel was channed from " + (ChnPos + 1) + " --> to "
									+ (currentChn + 1));
						}
						DriftTablePath = GetDriftTablePath(OImg[currentChn], PathAndName);
					}
					ImgSRRF[counter] = SRRFanalysis(OImg[counter], PathAndName, 0, SRRF_MaxImgNum, DriftTablePath);
					if (jump) {
						ChannelFlag[currentChn] = 1;
						break;
					}
					ChnPos = k;
					ColorizeChannel(ImgSRRF[counter], chn); // colorize ImgSRRF
					ChannelNames[counter] = "SRRF_" + ChannelNames[counter];
					ImgSRRF[counter].setTitle(ChannelNames[counter]);
					ImgSRRF[counter].createImagePlus();
					counter++;
				}
			}
		}
		if (!Cancel) {
			if (!jump) {
				if (channels == 1) {
					HyperImg = ImgSRRF[0];
					IJ.run(HyperImg, "Enhance Contrast", "saturated=" + ONEsetup.StrechColor);
				} else { // TODO:
					counter = 0;
					for (int k = start; k < imDim[2]; k++) {
						int chn = k + 1;
						if (chn <= channels) {
							ImageProcessor ip = ImgSRRF[counter].getProcessor();
							HyperImg.getStack().setProcessor(ip, chn);
							HyperImg.setC(chn);

							IJ.run(HyperImg, "Enhance Contrast", "saturated=" + ONEsetup.StrechColor);
							counter++;
						}
					}
					ImageProcessor ip = ImgSRRF[0].getProcessor();
					HyperImg.getStack().setProcessor(ip, 1);
					HyperImg.setC(1);

					IJ.run(HyperImg, "Enhance Contrast", "saturated=" + ONEsetup.StrechColor);
					Local_CompositeConverter LocalComp = new Local_CompositeConverter();
					LocalComp.StartConverting(HyperImg);
				}
				if (channels == 1 || SpecChn == 1) {
					IJ.run(HyperImg, "NanoJ-Orange", "");
				}
				IJ.run(HyperImg, "Set Scale...", "distance=" + ScaleNumber + " known=" + KnownDist + " unit=micron");
				HyperImg.setTitle(OutName + "_");
				IJ.save(HyperImg, OutSRRF + OutName + "_" + ImageName + ".tif");
				if (GenerateLog) {
					// Generate optical flow log file and get its name.
					GenerateOpticalFlowLogFile(HyperImg, channels, 1);
				}
				if (Calibrate && !GenerateLog) {
					// Performing chromatic correction for the final image.
					ChromaticCorrection(HyperImg, channels, 1, OutSRRF, ImageName);
				}
				HyperImg.changes = false;
				HyperImg.close();
			} else {
				int AvailableChn = GetCurrentChannel(ChannelFlag);
				if (AvailableChn != -1 && SpecChn == 0) {
					IJ.run("Close All");
					StartAnalysis(FileList, i, OutputDir, false, !OneChannel, ChnPos);
				} else {
					LogFile.add("SRRF failed to analyze this video");
				}
			}
		}
		if (Calibrate) {
			GetLogFile(Output, false);
		}

	}





	/**
	 * Calculate the channel drift table and return its path.
	 * 
	 * @param Img       Stack ImagePlus used to perform analysis on.
	 * @param PathAndName Array of Strings related to the image name and its path.
	 */
	private String GetDriftTablePath(ImagePlus Img, String[] PathAndName) {

		String OutPath = PathAndName[1];
		String drift_table_path = OutPath + PathAndName[0] + "_";

        String Options = "time=100 max=150 reference=[previous frame (better for live)]";
		Options = Options + "choose=[" + drift_table_path + "]";

		IJ.run(Img, "Estimate Drift", Options);
//		ImagePlus imp = WindowManager.getImage("Average CCM");
//		IJ.save(imp, OutPath + "AvgCCM_" + PathAndName[0] + ".tif");
//		String table_path = drift_table_path + "DriftTable.njt";
//		imp.changes = false;
//		imp.close();

		return drift_table_path + "DriftTable.njt";
	}


	/**
	 * Calls SRRF plugin and start analysis, returns super-resolution image.
	 * 
	 * @param Img       Stack ImagePlus used to perform analysis on.
	 * @param PathAndName Array of Strings related to the image name and its path.
	 * @param Start       Controls the starting frame.
	 * @param End         Controls the ending frame.
	 * @param table_path  Channel drift table path.
	 */
	private ImagePlus SRRFanalysis(ImagePlus Img, String[] PathAndName, int Start, int End, String table_path) {
		ImagePlus result;
		String OutPath = PathAndName[1];
		String OptionsSRRF = "ring=0.50 radiality_magnification=" + RadMagnification;
		OptionsSRRF = OptionsSRRF + " axes=" + AxesRing + " do_drift-correction frames_per_time-point=0";
		OptionsSRRF = OptionsSRRF + " start=" + Start + " end=" + End + " max=100 preferred=0";
		OptionsSRRF = OptionsSRRF + " rbg1=[" + AnalysisType + "] " + IntTempCor + SRRForder + " ";
		OptionsSRRF = OptionsSRRF + Radiality_RPC + Radiality_Renorm + Radiality_DoGS + Weighting_DoIW + Weighting_DoGW
				+ psf + minSRRFPat;
		OptionsSRRF = OptionsSRRF + " save=.nji choose=[" + table_path + "]";

		IJ.run(Img, "SRRF Analysis", OptionsSRRF);
		try {
			ImagePlus SRRF_Img = WindowManager.getImage(PathAndName[0] + " - SRRF");

			if (SRRF_Img == null) {
				jump = true;
				return null;
			} else {

				result = SRRF_Img.duplicate();
				String SRRF_Name = OutName + "_" + PathAndName[0];
				IJ.save(SRRF_Img, OutPath + SRRF_Name + ".tif");
				SRRF_Img.close();

				jump = false;
			}
		} catch (Exception e) {
			System.out.println("Image doesn't exist");
			jump = true;
			return null;
		}
//		Img.changes = false;
//		Img.close();
		return result;
	}

	// ------------------------- Computing log file -------------------------
	/**
	 * Calls Image Stabilizer plugin and start analysis, returns image title.
	 * 
	 * @param Beads  Stack ImagePlus used to perform analysis on.
	 * @param chn    Channel number.
	 * @param length Z slice length.
	 */
	private String GenerateOpticalFlowLogFile(ImagePlus Beads, int chn, int length) {

		IJ.run(Beads, "Hyperstack to Stack", "");
//		IJ.run("Next Slice [>]");
		Beads.setSlice(1);
		String Options = "";
		Options = Options + "transformation=Translation maximum_pyramid_levels=0";
		Options = Options + " template_update_coefficient=0.90 maximum_iterations=400";
		Options = Options + " error_tolerance=0.0000001 log_transformation_coefficients";
		Beads.show();
		IJ.selectWindow(Beads.getTitle());
		IJ.run(Beads, "Image Stabilizer", Options);
		String Options2 = "";
		Options2 = Options2 + "order=xyczt(default) channels=" + chn + " slices=" + 1; // length instead of 1
		Options2 = Options2 + " frames=1 display=Composite";
		IJ.run(Beads, "Stack to Hyperstack...", Options2);
		return Beads.getTitle();
	}
	// ------------------------- end -------------------------

	// ------------------------- Calibrating -------------------------
	/**
	 * Calls Image Stabilizer plugin to perform log applier.
	 * 
	 * @param imp     Merged SRRF images.
	 * @param chn       Number of channels.
	 * @param Zax       Number of Z slices.
	 * @param Path      Output path.
	 * @param ImageName Image Name.
	 */
	private void ChromaticCorrection(ImagePlus imp, int chn, int Zax, String Path, String ImageName) {
		lockChromatic = true;
		String Options = "";
		Options = Options + "order=xyczt(default) channels=" + chn + " slices=" + Zax;
		Options = Options + " frames=1 display=Composite";
		IJ.run(imp, "Hyperstack to Stack", "");
		imp.show();
		IJ.selectWindow(imp.getTitle());
		IJ.run(imp, "Image Stabilizer Log Applier", " ");
		IJ.run(imp, "Stack to Hyperstack...", Options);
		String name = ImageName + CAC;
		imp.setTitle(name);
		IJ.save(imp, Path + name + ".tif");
		lockChromatic = false;
	}
	// ------------------------- end -------------------------

	/**
	 * If the user want to calculate the drift correction for one channel only.
	 * 
	 * @param OImg         Array of images.
	 * @param ImageName    Image Name.
	 * @param ChannelNames Array channel names.
	 * @param OutResults   Output directory sub directory.
	 * @param OutSRRF      Output directory.
	 */
	String GetFixedDriftTablePath(ImagePlus[] OImg, String ImageName, String[] ChannelNames, String OutResults,
			String OutSRRF, int chn) {
		int k = SpecChn - 1;
		ChannelNames[k] = ImageName + "_" + chn;
		OImg[k].setTitle(ChannelNames[k]);

		String[] PathAndName = { ChannelNames[k], OutResults, OutSRRF };
		String DriftTablePath = GetDriftTablePath(OImg[k], PathAndName);
		return DriftTablePath;
	}

	/**
	 * Update metadata for each image stack.
	 * 
	 * @param FileList Array of files.
	 * @param i        loop number.
	 */
	private void AdjustParameters(File[] FileList, int i) {
		String Path = FileList[i].getPath();
		InputData IData;
		try {
			IData = new InputData(Path, InputData.ImportType.Vi, 1);
			AutomateUserInput(IData); // adjust inputs to conditions.
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read and convert List of strings to array of files.
	 * 
	 * @param ListofFiles List of strings.
	 */
	private File[] GetFiles(ArrayList<String> ListofFiles) {
		int L_ = ListofFiles.size();
		String[] ArrayofFiles = ListofFiles.toArray(new String[L_]);
		File[] files = new File[L_];
		for (int i = 0; i < L_; i++) {
			File temp = new File(ArrayofFiles[i]);
			files[i] = temp;
		}
		return files;
	}

	/**
	 * get minimum value inside a double array.
	 * 
	 * @param inputArray input array
	 */
	static double getMin(double[] inputArray) {
		double minValue = inputArray[0];
		for (int i = 1; i < inputArray.length; i++) {
			if (inputArray[i] < minValue) {
				minValue = inputArray[i];
			}
		}
		return minValue;
	}

	/**
	 * Adjust input settings relative to user input and image metadata.
	 * 
	 * @param IData Input data
	 */
	void AutomateUserInput(InputData IData) { // TODO:
		double[] Resolution = IData.GetResolutionsXYZ();
		int nChannels = IData.GetnChannels();
		int nFrames = IData.GetnFrames();
		Distance = MainWindow.DistScale == 0 ? MainWindow.roundDec3(1 / Resolution[0]) : MainWindow.DistScale;

		channels = MainWindow.ChnNum == 0 ? nChannels : MainWindow.ChnNum;
		SRRF_MaxImgNum = MainWindow.EndFrame == 0 ? nFrames : MainWindow.EndFrame;
		zSlices = IData.GetzSlices();
	}

	private void ColorizeChannel(ImagePlus imp, int chn) {
		switch (chn) {
		case 1:
			IJ.run(imp, "Red", ""); // new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
			break;
		case 2:
			IJ.run(imp, "Green", "");
			break;
		case 3:
			IJ.run(imp, "Cyan", "");
			break;
		case 4:
			IJ.run(imp, "Magenta", "");
			break;
		case 5:
			IJ.run(imp, "Yellow", "");
			break;
		case 6:
			IJ.run(imp, "Blue", "");
			break;
		default:
			IJ.run(imp, "Grays", "");
			break;
		}
	}

	private String GetFileExtension(String name) {
		String ext = "." + FilenameUtils.getExtension(name);
		return ext;
	}

	private void CreateDir(String path) {
		File OutLocation = new File(path);
		if (!OutLocation.exists()) {
			OutLocation.mkdir();
		}
	}

	private int GetCurrentChannel(int[] ChnFlag) {
		int currentChn = -1;
		for (int i = 0; i < ChnFlag.length; i++) {
			if (ChnFlag[i] == 0) {
				currentChn = i;
			}
		}
		return currentChn;
	}

	// ======================= Dialog Window =======================
	void AdjustOption() {
		IntTempCor = integrate_temporal_correlations ? "integrate_temporal_correlations " : "";
		Radiality_RPC = radiality_RPC ? "remove_positivity_constraint " : "";
		Radiality_Renorm = radiality_Renorm ? "renormalize " : "";
		Radiality_DoGS = radiality_DoGS ? "do_gradient_smoothing " : "";
		Weighting_DoIW = weighting_DoIW ? "do_intensity_weighting " : "";
		Weighting_DoGW = weighting_DoGW ? "do_gradient_weighting " : "";
		minSRRFPat = Minimize_SRRF_patterning ? " minimize_srrf_patterning" : "";
		psf = "psf_fwhm=" + psf_fwhm;
		SRRForder = "trac_order=" + TRAC_order;
	}

	public void PrintLogFile(String OutDir, ArrayList<String> LogFile) throws IOException {
		FileWriter LogF = new FileWriter(OutDir + File.separator + "Log file.txt");

		for (int i = 0; i < LogFile.size(); i++) {
			LogF.write(LogFile.get(i) + "\n");
		}
		LogF.close();
	}

	// ======================= #Region Progress Bar Window =======================
	public void ShowProgressBarWindow() {
		frame.setVisible(true);
		frame.setResizable(false);
		progressStatusLabel = new JLabel();
		progressStatusLabel.setText("Analyzing data please wait...");
		frame.setBounds(100, 100, 320, 160);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Status Window");



		progressBar.setStringPainted(true);
		progressBar.setBounds(10, 50, 294, 25);
		frame.getContentPane().add(progressBar);

		JButton CancelButton = new JButton("Cancel");
		CancelButton.setBounds(110, 97, 89, 23);
		frame.getContentPane().add(CancelButton);

		progressStatusLabel.setBounds(10, 30, 251, 14);
		frame.getContentPane().add(progressStatusLabel);
		CancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				progressStatusLabel.setText("Canceling in progress...");
				Cancel = true;
				frame.dispose();
				throw new RuntimeException(Macro.MACRO_CANCELED);
			}
		});
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				progressStatusLabel.setText("Canceling in progress...");
				Cancel = true;
				frame.dispose();
				throw new RuntimeException(Macro.MACRO_CANCELED);
			}
		});
	}

	public void SetPrograssBarMax(int Max) {
		progressBar.setMaximum(Max);
	}

	public void UpdateProgressBar(int value) {
		progressBar.setValue(value);
	}

	// ======================= #Region Progress Bar Window =======================
	// ======================= =======================

	// ======================= =======================
	// ======================= #Region ONE Z Analysis =======================
	/**
	 * Start zONE analysis.
	 *
	 *
	 * @param FileList      array of files.
	 * @param i             loop number.
	 * @param Output        output directory link.
	 * @param GenerateLog   perform chromatic aberration if true.
	 * @param SwitchChannel automatically switch drift correction to another channel
	 *                      if the current fails.
	 * @param ChnPos        is used to determine the channel position to process.
	 */
	private void StartAnalysis5D(File[] FileList, int i, String Output, boolean GenerateLog, boolean SwitchChannel,
			int ChnPos) {
		ChnColor = new ColorModel[channels];
		String Path = FileList[i].getPath();
		try {
			ImagePlus[] OImgArray = BF.openImagePlus(Path);
			int[] imDim = OImgArray[0].getDimensions(); // 0 get first series.
			ScaleNumber = Distance * ExpFactor * RadMagnification;

			ImagePlus[][] OImg = new ImagePlus[imDim[2]][zSlices];
			for (int k = 0; k < OImg.length; k++) {
				int chn = k + 1;
				if (chn <= channels) {
					for (int z = 0; z < zSlices; z++) {
						int Zaxis = z + 1;
						OImg[k][z] = new Duplicator().run(OImgArray[0], chn, chn, Zaxis, Zaxis, 1, imDim[4]);
						ChnColor[k] = OImg[k][0].getStack().getColorModel(); //

					}
				}
			}
			int width = OImg[0][0].getWidth();
			int height = OImg[0][0].getHeight();


			String ImageName = FileList[i].getName();
			String FileExt = GetFileExtension(ImageName);
			if (FileExt != null) {
				ImageName = ImageName.replace(FileExt, "");
			}
			String[] ChannelNames = new String[imDim[3]];
			// ------------------------- Creating Directories -------------------------
			String OutSubFolder = Output + ImageName + File.separator;
			CreateDir(OutSubFolder); // 2nd level subfolder
			String OutSRRF = OutSubFolder + OutName + File.separator;
			CreateDir(OutSRRF); // 3rd level subfolder
			String OutResults = OutSubFolder + ImageName + File.separator;
			CreateDir(OutResults); // 3rd level subfolder
			// ------------------------- end -------------------------
			ImagePlus[] HyperImg = new ImagePlus[channels];
			for (int k = 0; k < channels; k++) {
				HyperImg[k] = IJ.createHyperStack(ImageName, (int) (width * RadMagnification),
						(int) (height * RadMagnification), 1, zSlices, 1, 32);
				for (int z = 0; z < zSlices; z++) {
					if (!Cancel) {
						zONE(OImg, HyperImg[k], k, z, ChannelNames, ImageName, 1, OutResults, OutSRRF, false);
					}
				}
			}
			if (!Cancel) {
				ImagePlus MasterStack;
				if (channels == 1 || SpecChn == 1) {
					MasterStack = IJ.createHyperStack(ImageName, (int) (width * RadMagnification),
							(int) (height * RadMagnification), channels, zSlices, 1, 32);
					MasterStack = HyperImg[0];
					MasterStack.resetDisplayRange();

				} else {
					MasterStack = IJ.createImage(ImageName, "32-bit", (int) (width * RadMagnification),
							(int) (height * RadMagnification), channels, zSlices, 1);
					String options = "";
					for (int k = 0; k < imDim[2]; k++) {
						int chn = k + 1;
						if (chn <= channels) {
							options = options + "c" + chn + "=[" + ChannelNames[k] + "] ";
							HyperImg[k].setTitle(ChannelNames[k]);
							for (int z = 1; z <= zSlices; z++) {
								int ndx5D = get5Dindex(chn, z, 1, channels, zSlices);
								ImageProcessor zIP = HyperImg[k].getStack().getProcessor(z);
								MasterStack.getStack().setProcessor(zIP, ndx5D);
							}
							MasterStack.setC(chn);
							IJ.run(MasterStack, "Enhance Contrast", "saturated=" + ONEsetup.StrechColor); // TODO: new
						}
					}
					MasterStack.getStack().setProcessor(HyperImg[0].getStack().getProcessor(1), 1); // Initialize first
																									// image
					MasterStack.setC(1);
					IJ.run(MasterStack, "Enhance Contrast", "saturated=" + ONEsetup.StrechColor);
					Local_CompositeConverter LocalComp = new Local_CompositeConverter();
					LocalComp.StartConverting(MasterStack);
				}
				IJ.run(MasterStack, "Set Scale...", "distance=" + ScaleNumber + " known=" + KnownDist + " unit=&unit");
				IJ.save(MasterStack, OutSRRF + OutName + "-" + ImageName + ".tif");
				if (Calibrate) {
					if (!CreateLogFileOnce) {
						String[] txt = GetLogFile(Output, true); // locate log file.
						CreateHyperStackLogFile(channels, zSlices, txt); // create Hyperstack compatible log file.
						CreateLogFileOnce = true;
					}
					ChromaticCorrection(MasterStack, channels, zSlices, OutSRRF, ImageName);
				}
				MasterStack.changes = false;
				MasterStack.close();
			}
			IJ.run("Close All");
		} catch (FormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Start Z analysis.
	 *
	 * @param OImg          2D array of images.
	 * @param HyperImg      Final hyper-image.
	 * @param k             Channel number -1 .
	 * @param z             Z slice number.
	 * @param ChannelNames  Array of channel's names.
	 * @param ImageName     Image title.
	 * @param chn           Channel number.
	 * @param OutResults    Output directory sub directory.
	 * @param OutSRRF       Output directory.
	 * @param SwitchChannel automatically switch drift correction to another channel
	 *                      if the current fails.
	 */
	void zONE(ImagePlus[][] OImg, ImagePlus HyperImg, int k, int z, String[] ChannelNames, String ImageName, int chn,
			String OutResults, String OutSRRF, boolean SwitchChannel) {
		int currentZ = z;
		int zi = z + 1;
		jump = false;
		String DriftTablePath = "";
		if (chn <= channels) {
			ChannelNames[z] = ImageName + "_" + chn + "_" + zi;
			String[] PathAndName = { ChannelNames[z], OutResults, OutSRRF };
			if (SpecChn == 0) {
				DriftTablePath = GetDriftTablePath(OImg[k][currentZ], PathAndName);
			}
			ImagePlus Imgz = OImg[k][z].duplicate();
			Imgz.setTitle(ChannelNames[z]);
			ImagePlus ImgSRRF = SRRFanalysis(Imgz, PathAndName, 0, SRRF_MaxImgNum, DriftTablePath);
			if (!jump) {
				HyperImg.getStack().setProcessor(ImgSRRF.getProcessor(), zi);
				ImgSRRF.changes = false;
				ImgSRRF.close();
			}
			Imgz.changes = false;
			Imgz.close();
		}
	}
	// ======================= #Region ONE Z Analysis =======================
	// ======================= =======================

	// ======================= =======================
	// ======================= #Region experimenting with 5D======================= //

	/**
	 * Get log file.
	 *
	 * @param OutPath 2D array of images.
	 * @param close   Close when finish.
	 */
	String[] GetLogFile(String OutPath, boolean close) {
		Frame[] LogWins = WindowManager.getNonImageWindows();
		String[] txt = null;
		for (int i = 0; i < LogWins.length; ++i) {
			if (LogWins[i] instanceof Editor) {
				String temp = ((Editor) LogWins[i]).getText();
				if (!temp.startsWith("Image Stabilizer Log File"))
					continue;
				txt = temp.split("\n");
				String name = LogWins[i].getTitle();
				IJ.selectWindow(name);
				IJ.saveAs("Text", OutPath + name);
				if (close) {
					IJ.selectWindow(name);
					IJ.run("Close", "");
				}
			}
		}
		return txt;
	}

	/**
	 * Modify log file to suit 5D hperstack.
	 *
	 * @param chn Number of channels.
	 * @param Zax Number of Z slices.
	 * @param txt Log file text.
	 */
	public void CreateHyperStackLogFile(int chn, int Zax, String[] txt) {
		int L_ = chn * Zax;
		int transform = 0;
		int interval = 1;
		Editor eLog = new Editor();
		String name = "HyperStack_CAC_Log";
		eLog.create(name, "Image Stabilizer Log File for " + "\"" + name + "\"\n" + transform + "\n");
		Double[][] values = new Double[chn][2];
		String[] lines = new String[L_ + 1];
		lines[0] = ""; // lines start from [1] not 0;
		for (int i = 0; i < chn; i++) {
			String[] fields = txt[i + 2].split(",");
			values[i][0] = Double.parseDouble(fields[2]);
			values[i][1] = Double.parseDouble(fields[3]);
		}
		for (int c = 1; c <= chn; c++) {
			for (int z = 1; z <= Zax; z++) {
				int ki = get5Dindex(c, z, 1, chn, Zax);
				String temp = Integer.toString(ki) + "," + Integer.toString(interval) + ","
						+ Double.toString(values[c - 1][0]) + "," + Double.toString(values[c - 1][1]);
				lines[ki] = temp;
			}
		}
		for (int i = 1; i <= L_; i++) {
			eLog.append(lines[i] + "\n");
		}
	}

	private int get5Dindex(int c, int z, int t, int chn, int zSlices) {
		return chn * zSlices * (t - 1) + chn * (z - 1) + c;
	}


	// ======================= #Region experimenting with 5D ======================= //


}
