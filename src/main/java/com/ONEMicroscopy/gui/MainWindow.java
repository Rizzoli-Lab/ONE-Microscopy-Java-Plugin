package com.ONEMicroscopy.gui;

/**
 * -------------------------------------------------------------------------------------------
 * This class is the Main GUI for ONE-Microscopy.
 * 
 * @authors Abed Chouaib and Mohamad Mahdi Alawieh
 * @version 1.0.0
 * -------------------------------------------------------------------------------------------
 */
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.ONEMicroscopy.StartONEanalysis;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.FolderOpener;
import ij.plugin.PlugIn;

public class MainWindow implements PlugIn {
	private JFrame frmOneMicroscopy;
	private JTextField txtF_MaxFrames;
	private JTextField txtF_SRRForder;
	private JLabel lblTracOrder;
	private JTextField txtF_RadMag;
	private JTextField txtF_RingAxes;
	private JTextField txtF_DistScale;
	private JTextField txtF_Chn;
	private JTextField txtF_KnownDist;
	private JLabel lblRadMag;
	private JLabel lblRingAxes;
	private JLabel DistScaleL;
	private JLabel Label1_4;
	private JLabel Label1_5;
	private JTextField SavePathF;
	private JTextField ImportF;
	private JLabel lblNoiseFilter;
	private JLabel TopStatusLabel;
	public static int StackLength = 0; // width = 0, height = 0;
	public static String ImportDirPath, SaveDirPath, BeadsDirPath;
	private boolean ImageDetected = false;
	public static boolean MultiImageDetected = false;

	public static int EndFrame;
	public static double RadMag;
	public static double RingAxes;
	public static String SRRFType;
	public static double DistScale;
	public static int ChnNum;
	public static double KnownDist;
	private boolean SaveButtonGaurd = true;
	private Color FocusColor;
	public static ArrayList<String> StackDirectories = new ArrayList<String>();
	public static ArrayList<String> SubFolderNameList = new ArrayList<String>();
	private Checkbox CheckBoxVideosIn = new Checkbox();
	public static boolean TheInputIsVideos = true;
	public static int stackLoopNum;
	public static int NumberOfStacks;
	public static boolean ImagePresent = false;
	public static boolean OneVideo = true;
	public static boolean Calibrate;
	public static int SRRForder, MemmorySRRForder;
	public static int FPS;
	public static double ExpFactor;
	public static int SpecificChannel = 0;
	public static String OutPutDirectory;
	public static boolean SaveParentFolder = true;
	public static int StartFromVideo = 1;
	public static int EndOnVideo;
	// ============ Advance Options ============
	String AdOptPref = "nanoj.srrf.java.gui.SRRFAnalysis_ExtraSettings_.";
	public static boolean integrate_temporal_correlations;
	public static boolean Radiality_RPC, Radiality_Renorm;
	public static boolean Radiality_DoGS;
	public static boolean Weighting_DoIW, Weighting_DoGW, Minimize_SRRF_patterning;
	public static double psf_fwhm;
	// ============ end ============

	// ============ Metadata ============
	private double[] Resolution = { 0, 0, 0 };
	private int nChannels = 1;
	private int zSlices;
	private int nFrames;
	private double tempResolution;
	// ============ end ============
	String[] SRRFTypes = new String[4];
	String username;
	private ImagePlus OriginalImp;

	boolean lockImportField = false;
	InputData.ImportType DataType = InputData.ImportType.MVi;
	private JTextField txtF_CAC;
	private JTextField ViewChannels;
	private JTextField ViewFrames;
	private JTextField ViewDistance;
	private JTextField txtF_fullScale;
	private JTextField txtF_ExpFactor;
	private JTextField txtF_SpecChn;
	private JPanel panel_1;
	private JTextField txtF_StartFromVideo;
	private JTextField txtF_EndOnVideo;
	private JTextField txtF_zSlices;
	private JLabel lblZaxis;



	public MainWindow() {
		initialize();
	}

	// ==================================================================================================
	// ==================================================================================================
	// <<<<<<<<<<>>>>>>>>>> GUI JFrame <<<<<<<<<<>>>>>>>>>>
	public void initialize() {
		SRRFTypes[0] = "Temporal Radiality Maximum (TRM - activate in high-magnification)";
		SRRFTypes[1] = "Temporal Radiality Average (TRA)";
		SRRFTypes[2] = "Temporal Radiality Pairwise Product Mean (TRPPM)";
		SRRFTypes[3] = "Temporal Radiality Auto-Correlations (TRAC)";
		username = System.getProperty("user.home"); // get the user.name

		frmOneMicroscopy = new JFrame();
		frmOneMicroscopy.setResizable(false);
		frmOneMicroscopy.setBackground(Color.LIGHT_GRAY);
		frmOneMicroscopy.getContentPane().setBackground(SystemColor.control);
		frmOneMicroscopy.setTitle("ONE Microscopy");
		frmOneMicroscopy.setBounds(100, 100, 633, 550);
		frmOneMicroscopy.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frmOneMicroscopy.getContentPane().setLayout(null);
		frmOneMicroscopy.setLocationRelativeTo(null);
		FocusColor = Color.decode("#ddeeff"); // #eeddff

		txtF_MaxFrames = new JTextField("0"); // ================= Mean Brightness Tolerance
		txtF_MaxFrames.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_MaxFrames.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_MaxFrames.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_MaxFrames.setBackground(Color.white);
			}
		});
		txtF_MaxFrames.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent evt) {
				char c = evt.getKeyChar();
				if (!Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
					evt.consume();
				}
			}
		});

		txtF_MaxFrames.setBounds(194, 162, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_MaxFrames);
		txtF_MaxFrames.setColumns(10);

		JLabel Label1 = new JLabel("Frames to analyze (0-auto)");
		Label1.setBounds(9, 160, 182, 24);
		frmOneMicroscopy.getContentPane().add(Label1);

		txtF_SRRForder = new JTextField("");
		txtF_SRRForder.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_SRRForder.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent evt) {
				char c = evt.getKeyChar();
				if (Character.isAlphabetic(c)) {
					evt.consume();
				}
			}
		});

		txtF_SRRForder.setColumns(10);
		txtF_SRRForder.setBounds(194, 287, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_SRRForder);
		txtF_SRRForder.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_SRRForder.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_SRRForder.setBackground(Color.white);
			}
		});

		lblTracOrder = new JLabel("TRAC order (2-4)");
		lblTracOrder.setBounds(9, 285, 175, 24);
		frmOneMicroscopy.getContentPane().add(lblTracOrder);

		txtF_RadMag = new JTextField("10");
		txtF_RadMag.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_RadMag.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent evt) {
				char c = evt.getKeyChar();
				if (Character.isAlphabetic(c)) {
					evt.consume();
				}
			}
		});
		txtF_RadMag.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_RadMag.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_RadMag.setBackground(Color.white);
			}
		});
		txtF_RadMag.setColumns(10);
		txtF_RadMag.setBounds(194, 195, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_RadMag);

		txtF_RingAxes = new JTextField("8");
		txtF_RingAxes.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_RingAxes.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_RingAxes.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_RingAxes.setBackground(Color.white);
			}
		});
		txtF_RingAxes.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent evt) {
				char c = evt.getKeyChar();
				if (!Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
					evt.consume();
				}
			}
		});
		txtF_RingAxes.setColumns(10);
		txtF_RingAxes.setBounds(194, 226, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_RingAxes);

		txtF_DistScale = new JTextField("0");
		txtF_DistScale.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_DistScale.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (!Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
					e.consume();
				}
			}
		});
		txtF_DistScale.setColumns(10);
		txtF_DistScale.setBounds(194, 320, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_DistScale);
		txtF_DistScale.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_DistScale.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_DistScale.setBackground(Color.white);
				updateFullScale();
			}
		});

		txtF_Chn = new JTextField("0");
		txtF_Chn.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_Chn.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (!Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
					e.consume();
				}
			}
		});
		txtF_Chn.setColumns(10);
		txtF_Chn.setBounds(194, 94, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_Chn);
		txtF_Chn.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_Chn.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_Chn.setBackground(Color.white);
			}
		});

		txtF_KnownDist = new JTextField("1");
		txtF_KnownDist.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_KnownDist.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (!Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE) {
					e.consume();
				}
			}
		});
		txtF_KnownDist.setColumns(10);
		txtF_KnownDist.setBounds(194, 355, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_KnownDist);
		txtF_KnownDist.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				txtF_KnownDist.setBackground(FocusColor);
			}

			@Override
			public void focusLost(FocusEvent e) {
				txtF_KnownDist.setBackground(Color.white);
			}
		});

		lblRadMag = new JLabel("Radiality magnification (1-35)");
		lblRadMag.setBounds(9, 195, 182, 24);
		frmOneMicroscopy.getContentPane().add(lblRadMag);

		lblRingAxes = new JLabel("Ring axes ( 2 - 8)");
		lblRingAxes.setBounds(9, 226, 153, 24);
		frmOneMicroscopy.getContentPane().add(lblRingAxes);

		DistScaleL = new JLabel("Distance to scale (0-auto)");
		DistScaleL.setBounds(9, 320, 175, 24);
		frmOneMicroscopy.getContentPane().add(DistScaleL);

		Label1_4 = new JLabel("Channels to process (0-auto)");
		Label1_4.setBounds(9, 94, 182, 24);
		frmOneMicroscopy.getContentPane().add(Label1_4);

		Label1_5 = new JLabel("Known distance");
		Label1_5.setBounds(9, 353, 175, 24);
		frmOneMicroscopy.getContentPane().add(Label1_5);

		SavePathF = new JTextField(); // save path
		SavePathF.setEnabled(false);
		SavePathF.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				SavePathF.setBackground(Color.WHITE);
			}
		});
		if (username != null) {
			String CurrentTime = "" + java.time.LocalDateTime.now();
			SavePathF.setText(username + File.separator + "Desktop" + File.separator + "ONE Microscopy" + File.separator
					+ "Analysis " + CurrentTime.replace(":", "-"));
		}
		SavePathF.setToolTipText("");
		SavePathF.setColumns(10);
		SavePathF.setBounds(10, 59, 313, 20);
		frmOneMicroscopy.getContentPane().add(SavePathF);
		ImportF = new JTextField();

		ImportF.setToolTipText("");
		ImportF.setColumns(10);
		ImportF.setBounds(10, 26, 313, 20);
		frmOneMicroscopy.getContentPane().add(ImportF);

		JButton ImportFolderButton = new JButton("Import ");
		ImportFolderButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (DataType == InputData.ImportType.Vi) {
					ImportDirPath = IJ.getFilePath("Select Input Video...");
				} else {
					ImportDirPath = IJ.getDirectory("Select Input Folder...");
				}
				if (ImportDirPath != null) {
					ImportF.setText(ImportDirPath);
					LoadImportedData();
				}
			}
		});
		ImportFolderButton.setBounds(343, 25, 89, 23);
		frmOneMicroscopy.getContentPane().add(ImportFolderButton);

		// Info Icon and mouse events for TRAC Description
		JLabel trac_info = new JLabel();
		ImageIcon imageIcon = new ImageIcon(new ImageIcon(getClass().getResource("/info-icon.png")).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		trac_info.setIcon(imageIcon);
		trac_info.createToolTip();
		trac_info.addMouseListener(new MouseAdapter() {
			final int toolTip_timeout = ToolTipManager.sharedInstance().getInitialDelay();
			@Override
			public void mouseEntered(MouseEvent event) {
				ToolTipManager.sharedInstance().setInitialDelay(0);
				UIManager.put("ToolTip.background",Color.white);
				UIManager.put("ToolTip.border",new LineBorder(Color.BLACK,1));
				trac_info.setToolTipText("<html>TRAC to the order of 4 or higher is advised for use <br> when the Signal-to-Noise Ratio (SNR) is equal to <br> or exceeds 20.</html>");

			}
			@Override
			public void mouseExited(MouseEvent event) {
				ToolTipManager.sharedInstance().setInitialDelay(toolTip_timeout);
			}
		});
		trac_info.setBounds(575, 257, 20, 20);
		frmOneMicroscopy.getContentPane().add(trac_info);

		Choice Choice_SRRFTypes = new Choice();
		Choice_SRRFTypes.add(SRRFTypes[0]);
		Choice_SRRFTypes.add(SRRFTypes[1]);
		Choice_SRRFTypes.add(SRRFTypes[2]);
		Choice_SRRFTypes.add(SRRFTypes[3]);
		Choice_SRRFTypes.setBounds(194, 256, 370, 20);
		frmOneMicroscopy.getContentPane().add(Choice_SRRFTypes);
		Choice_SRRFTypes.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int choice = Choice_SRRFTypes.getSelectedIndex();
				if (choice == 3) {
					SRRForder = MemmorySRRForder != 2 ? 4 : MemmorySRRForder; //
					txtF_SRRForder.setEditable(true);
					trac_info.setVisible(true);

				} else {
					txtF_SRRForder.setEditable(false);
					trac_info.setVisible(false);
					if (choice == 0)
						SRRForder = 0;
					else if (choice == 1)
						SRRForder = 1;
					else if (choice == 2)
						SRRForder = -1;
				}
				txtF_SRRForder.setText("" + SRRForder);
			}
		});
		// get selected item from prefs.

		lblNoiseFilter = new JLabel("Temporal analysis mode ");
		lblNoiseFilter.setBounds(9, 256, 175, 24);
		frmOneMicroscopy.getContentPane().add(lblNoiseFilter);

		JPanel panel = new JPanel();
		panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel.setBackground(UIManager.getColor("Button.background"));
		panel.setBounds(10, 414, 594, 101);
		frmOneMicroscopy.getContentPane().add(panel);
		panel.setLayout(null);

		Checkbox CheckBox_CAC = new Checkbox("Chromatic aberration correction");
		CheckBox_CAC.setFont(new Font("Tahoma", Font.PLAIN, 11));
		CheckBox_CAC.setBounds(10, 10, 215, 22);
		panel.add(CheckBox_CAC);

		JButton StartButton = new JButton("Start");
		StartButton.setBounds(495, 67, 89, 23);
		panel.add(StartButton);

		txtF_CAC = new JTextField();
		txtF_CAC.setEnabled(false);
		txtF_CAC.setBounds(147, 40, 313, 20);
		panel.add(txtF_CAC);
		txtF_CAC.setToolTipText("");
		txtF_CAC.setColumns(10);

		JButton Import_CAC = new JButton("Import Beads");
		Import_CAC.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				BeadsDirPath = IJ.getDirectory("Select Input Folder...");
				if (BeadsDirPath != null) {
					txtF_CAC.setText(BeadsDirPath);
				}
			}
		});
		Import_CAC.setEnabled(false);
		Import_CAC.setBounds(10, 38, 121, 23);
		panel.add(Import_CAC);

		TopStatusLabel = new JLabel("");
		TopStatusLabel.setFont(new Font("Tahoma", Font.PLAIN, 10));
		TopStatusLabel.setForeground(SystemColor.textHighlight);
		TopStatusLabel.setBounds(10, 0, 412, 24);
		frmOneMicroscopy.getContentPane().add(TopStatusLabel);

		JButton SaveButton = new JButton("Save To");
		SaveButton.setEnabled(false);
		SaveButton.setBounds(343, 58, 89, 23);
		frmOneMicroscopy.getContentPane().add(SaveButton);

		ViewChannels = new JTextField("");
		ViewChannels.setHorizontalAlignment(SwingConstants.CENTER);
		ViewChannels.setEditable(false);
		ViewChannels.setColumns(10);
		ViewChannels.setBounds(290, 94, 86, 20);
		frmOneMicroscopy.getContentPane().add(ViewChannels);

		ViewFrames = new JTextField("");
		ViewFrames.setHorizontalAlignment(SwingConstants.CENTER);
		ViewFrames.setEditable(false);
		ViewFrames.setColumns(10);
		ViewFrames.setBounds(290, 162, 86, 20);
		frmOneMicroscopy.getContentPane().add(ViewFrames);

		ViewDistance = new JTextField("");
		ViewDistance.setHorizontalAlignment(SwingConstants.CENTER);
		ViewDistance.setEditable(false);
		ViewDistance.setColumns(10);
		ViewDistance.setBounds(290, 320, 86, 20);
		frmOneMicroscopy.getContentPane().add(ViewDistance);

		txtF_fullScale = new JTextField("");
		txtF_fullScale.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_fullScale.setEditable(false);
		txtF_fullScale.setColumns(10);
		txtF_fullScale.setBounds(290, 390, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_fullScale);

		txtF_ExpFactor = new JTextField("1");
		txtF_ExpFactor.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_ExpFactor.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateFullScale();
			}
		});
		txtF_ExpFactor.setColumns(10);
		txtF_ExpFactor.setBounds(194, 390, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_ExpFactor);

		JLabel lblExpansionFactor = new JLabel("Expansion factor");
		lblExpansionFactor.setBounds(9, 388, 175, 24);
		frmOneMicroscopy.getContentPane().add(lblExpansionFactor);

		txtF_SpecChn = new JTextField("0");
		txtF_SpecChn.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_SpecChn.setColumns(10);
		txtF_SpecChn.setBounds(194, 127, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_SpecChn);

		JLabel Label1_4_1 = new JLabel("Drift channel reference (0-auto)");
		Label1_4_1.setBounds(9, 125, 182, 24);
		frmOneMicroscopy.getContentPane().add(Label1_4_1);

		panel_1 = new JPanel();
		panel_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_1.setBounds(456, 26, 161, 215);
		frmOneMicroscopy.getContentPane().add(panel_1);
		panel_1.setLayout(null);

		Checkbox CheckBoxOneVideo = new Checkbox("Video");
		CheckBoxOneVideo.setBounds(10, 10, 97, 23);
		panel_1.add(CheckBoxOneVideo);

		CheckBoxVideosIn = new Checkbox("Multi-videos");
		CheckBoxVideosIn.setBounds(10, 39, 97, 23);
		panel_1.add(CheckBoxVideosIn);
		CheckBoxVideosIn.setState(true);

		Checkbox CheckBoxImages = new Checkbox("Olympus files (.ets)");
		CheckBoxImages.setBounds(10, 68, 131, 23);
		panel_1.add(CheckBoxImages);
		CheckBoxImages.setEnabled(false);

		Checkbox CheckBox_SaveParentFolder = new Checkbox("Save in parent folder");
		CheckBox_SaveParentFolder.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (CheckBox_SaveParentFolder.getState() == false) {
					SavePathF.setEnabled(true);
					SaveButton.setEnabled(true);
				} else {
					SavePathF.setEnabled(false);
					SaveButton.setEnabled(false);
				}
			}
		});
		CheckBox_SaveParentFolder.setState(true);
		CheckBox_SaveParentFolder.setBounds(10, 97, 125, 22);
		panel_1.add(CheckBox_SaveParentFolder);
		CheckBox_SaveParentFolder.setFont(new Font("Tahoma", Font.PLAIN, 11));

		JButton btn_AdOpt = new JButton("Advanced Options");
		btn_AdOpt.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				showDialog();
			}
		});
		btn_AdOpt.setBounds(10, 185, 141, 23);
		panel_1.add(btn_AdOpt);

		txtF_StartFromVideo = new JTextField("1");
		txtF_StartFromVideo.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_StartFromVideo.setColumns(10);
		txtF_StartFromVideo.setBounds(105, 127, 46, 20);
		panel_1.add(txtF_StartFromVideo);


		txtF_EndOnVideo = new JTextField("1");
		txtF_EndOnVideo.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_EndOnVideo.setColumns(10);
		txtF_EndOnVideo.setBounds(105, 150, 46, 20);
		panel_1.add(txtF_EndOnVideo);


		JLabel Label1_4_2 = new JLabel("Start from video");
		Label1_4_2.setBounds(10, 125, 125, 24);
		panel_1.add(Label1_4_2);

		JLabel Label1_4_3 = new JLabel("End on video");
		Label1_4_3.setBounds(10, 150, 125, 24);
		panel_1.add(Label1_4_3);

		txtF_zSlices = new JTextField("");
		txtF_zSlices.setHorizontalAlignment(SwingConstants.CENTER);
		txtF_zSlices.setEditable(false);
		txtF_zSlices.setColumns(10);
		txtF_zSlices.setBounds(506, 383, 86, 20);
		frmOneMicroscopy.getContentPane().add(txtF_zSlices);

		lblZaxis = new JLabel("zONE");
		lblZaxis.setHorizontalAlignment(SwingConstants.CENTER);
		lblZaxis.setBounds(506, 365, 86, 24);
		frmOneMicroscopy.getContentPane().add(lblZaxis);
		CheckBoxImages.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CheckBoxVideosIn.setState(!CheckBoxImages.getState());
				CheckBoxOneVideo.setState(!CheckBoxImages.getState());
				TheInputIsVideos = CheckBoxVideosIn.getState();
				OneVideo = false;
				DataType = InputData.ImportType.ImgS;
				txtF_StartFromVideo.setEnabled(true);
				txtF_EndOnVideo.setEnabled(true);
			}
		});

		CheckBoxVideosIn.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CheckBoxImages.setState(!CheckBoxVideosIn.getState());
				CheckBoxOneVideo.setState(!CheckBoxVideosIn.getState());
				TheInputIsVideos = CheckBoxVideosIn.getState();
				OneVideo = false;
				DataType = InputData.ImportType.MVi;
				txtF_StartFromVideo.setEnabled(true);
				txtF_EndOnVideo.setEnabled(true);
			}
		});

		CheckBoxOneVideo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				CheckBoxImages.setState(!CheckBoxOneVideo.getState());
				CheckBoxVideosIn.setState(!CheckBoxOneVideo.getState());
				TheInputIsVideos = !CheckBoxOneVideo.getState();
				OneVideo = true;
				DataType = InputData.ImportType.Vi;
				txtF_StartFromVideo.setEnabled(false);
				txtF_EndOnVideo.setEnabled(false);
				txtF_StartFromVideo.setText("1");
				txtF_EndOnVideo.setText("1");

			}
		});

		SaveButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				SaveDirPath = IJ.getDirectory("Select OutPut Folder...");
				if (SaveDirPath != null) {
					SavePathF.setText(SaveDirPath);
					OutPutDirectory = SaveDirPath;
					SaveButtonGaurd = false;
				}
			}
		});
		CheckBox_CAC.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					txtF_CAC.setEnabled(true);
					Import_CAC.setEnabled(true);
				} else if (e.getStateChange() == ItemEvent.DESELECTED) {
					txtF_CAC.setEnabled(false);
					Import_CAC.setEnabled(false);
				}
			}
		});

		// ====================== Search for Images ======================
		LoadPrefs();
		int SRRFtypeNum = Arrays.asList(SRRFTypes).indexOf(SRRFType);
		Choice_SRRFTypes.select(SRRFtypeNum);
		// ====================== Search for Images ======================
		ImportF.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				ImportF.setBackground(Color.WHITE);
				lockImportField = true;
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (!lockImportField) {
					LoadImportedData();
				}
			}

		});
		ImportF.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
			}

			public void removeUpdate(DocumentEvent e) {
			}

			public void insertUpdate(DocumentEvent e) {
				lockImportField = false;
			}
		});
		// ====================== Search for Images ======================
		// ====================== Search for Images ======================
		frmOneMicroscopy.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				char c = evt.getKeyChar();
				if (c == KeyEvent.VK_ENTER) {
//					RefreshGUI();
				}
			}
		});
		frmOneMicroscopy.getContentPane().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
//				RefreshGUI();
			}
		});
		// ==================== Start Program
		// ====================================================================================
		StartButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				Thread StartProgram = new Thread(new Runnable() {
					@Override
					public void run() {
						if ((SavePathF.getText() != null && !SavePathF.getText().isEmpty()) || SaveParentFolder) {
							if (ImageDetected) {
								frmOneMicroscopy.dispose();
								if (SaveButtonGaurd) {
									if (!SaveParentFolder) {
										if (username != null) {
											File OutDir = new File(username + File.separator + "Desktop"
													+ File.separator + "ONE Analysis" + File.separator);
											if (!OutDir.exists()) {
												OutDir.mkdir();
											}
										}
										OutPutDirectory = SavePathF.getText();
									}
								}
								RefreshGUI();
								ChnNum = Integer.parseInt(txtF_Chn.getText());
								EndFrame = Integer.parseInt(txtF_MaxFrames.getText());
								RadMag = (double) Double.parseDouble(txtF_RadMag.getText());
								RingAxes = (double) Double.parseDouble(txtF_RingAxes.getText());
								SRRFType = Choice_SRRFTypes.getSelectedItem();
								DistScale = (double) Double.parseDouble(txtF_DistScale.getText());
								KnownDist = (double) Double.parseDouble(txtF_KnownDist.getText());
								SRRForder = Integer.parseInt(txtF_SRRForder.getText());
								String temp1 = txtF_CAC.getText();
								ExpFactor = (double) Double.parseDouble(txtF_ExpFactor.getText());
								BeadsDirPath = temp1;
								String temp2 = ImportF.getText();
								SaveParentFolder = CheckBox_SaveParentFolder.getState();

								StartFromVideo = Integer.parseInt(txtF_StartFromVideo.getText()) - 1;
								EndOnVideo = Integer.parseInt(txtF_EndOnVideo.getText());
								SpecificChannel = Integer.parseInt(txtF_SpecChn.getText());
								ImportDirPath = temp2;
								Calibrate = CheckBox_CAC.getState();
								stackLoopNum = 0;

								savePrefs();

								NumberOfStacks = StackDirectories.size(); // <----------------- Start Analysis
								StartONEanalysis cl2 = new StartONEanalysis();
								cl2.run(null);

							} else {
								ImportF.setBackground(Color.decode("#ff5555"));
							}
						} else {
							SavePathF.setBackground(Color.decode("#ff5555"));
						}
					}
				});
				StartProgram.start();
			}
		});
		// ==================== Start Program
		// ====================================================================================
	}
	// <<<<<<<<<<>>>>>>>>>> GUI JFrame <<<<<<<<<<>>>>>>>>>>
	// ==================================================================================================
	// ==================================================================================================

	// ================================ Methods ================================
	private void RefreshGUI() {
		double temp = (1 / Resolution[0]);
		temp = roundDec3(temp);
		ViewChannels.setText("" + nChannels);
		ViewFrames.setText("" + nFrames);
		ViewDistance.setText("" + temp);
		txtF_zSlices.setText("" + zSlices);
		tempResolution = temp;
		updateFullScale();
	}

	public void CreateDir(String OutPutDirectory) {
		File OutPutDir = new File(OutPutDirectory);
		if (!OutPutDir.exists()) {
			OutPutDir.mkdir();
		}
	}

	// ============================= Run ONE ===============================
	// ============================= Run ONE ===============================
	public void run(String arg) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.initialize();
					window.frmOneMicroscopy.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// ============================= Run ONE ===============================
	// ============================= Run ONE ===============================

	public int[] SplitCommaReturn(String str) {
		String[] array = str.split(",", 0);
		int[] FinalArray = new int[array.length];
		if (array.length == 4) {
			for (int i = 0; i < FinalArray.length; i++) {
				try {
					FinalArray[i] = Integer.parseInt(array[i]);
				} catch (Exception e) {
				}
			}
		}
		return FinalArray;
	}

	// ================================ Methods ================================
	public void printLine(String[] data) {
		for (int i = 0; i < data.length; i++) {
			System.out.println(data[i]);
		}
	}

	public void printLine(ArrayList<String> data) {
		for (int i = 0; i < data.size(); i++) {
			System.out.println(data.get(i));
		}
	}

	public ArrayList<String> CheckDirectory(String Path) // TODO write to InputData class
	{
		ArrayList<String> ImgDirectories = new ArrayList<String>();
		SubFolderNameList.clear();
		switch (DataType) {
		case Vi:

			try {
				InputData IData = new InputData(Path, DataType, 3);
				ImgDirectories = IData.GetListofFiles();
				ReadMetaData(IData);
				RefreshGUI();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			break;

		case MVi:
			try {
				InputData IData = new InputData(Path, DataType, 3);
				ImgDirectories = IData.GetListofFiles();
				ReadMetaData(IData);
				RefreshGUI();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (ImgDirectories != null) {
				for (int i = 0; i < ImgDirectories.size(); i++) {
					File file = new File(ImgDirectories.get(i));
					SubFolderNameList.add(file.getName());
				}
			}
			break;

		case ImgS:
			boolean MultiStack = false;
			File newDir = new File(Path);
			if (newDir.isDirectory()) {
				String[] files = newDir.list();
				for (String file : files) {
					File TempFile = new File(Path + File.separator + file);
					if (TempFile.isDirectory()) {
						OriginalImp = FolderOpener.open(TempFile.getPath(), "virtual");
						StackLength = OriginalImp.getStackSize();
						if (StackLength >= 3) {
							ImgDirectories.add(TempFile.getPath());
							SubFolderNameList.add(TempFile.getName());
							MultiStack = true;
						}
					}
				}
				if (!MultiStack) {
					OriginalImp = FolderOpener.open(newDir.getPath(), "virtual");
					StackLength = OriginalImp.getStackSize();
					if (StackLength >= 3) {
						ImgDirectories.add(newDir.getPath());
						SubFolderNameList.add(newDir.getName());
					}
				}
			}
			break;
		default:
			break;
		}
		return ImgDirectories;
	}

	void ReadMetaData(InputData IData) {
		Resolution = IData.GetResolutionsXYZ();
		nChannels = IData.GetnChannels();
		nFrames = IData.GetnFrames();
		zSlices = IData.GetzSlices();
		FPS = (int) Math.round(1 / IData.GetTime());
	}

//============================================== Mask Creation Window ==============================================
	public void PrintConfigFile(String OutDir, ArrayList<String> LogFile) throws IOException {
		FileWriter LogF = new FileWriter(OutDir + "/Log file.txt");

		for (int i = 0; i < LogFile.size(); i++) {
			LogF.write(LogFile.get(i) + "\n");
		}
		LogF.close();
	}

	void LoadPrefs() {
		String tempArg = Prefs.get("onemic.srrf.java.gui.Expansion_Factor", "1");
		ExpFactor = (double) Double.parseDouble(tempArg);

		tempArg = Prefs.get("nanoj.srrf.java.gui.SRRFAnalysis_.radialityMagnification", "10");
		RadMag = (double) Double.parseDouble(tempArg);

		String tempSRRForder = Prefs.get(AdOptPref + "SRRForder", "4");
		SRRFType = Prefs.get(AdOptPref + "SRRFType", SRRFTypes[3]);

		if (SRRFType.equals(SRRFTypes[0])) {
			SRRForder = 0;
		} else if (SRRFType.equals(SRRFTypes[1])) {
			SRRForder = 1;
		} else if (SRRFType.equals(SRRFTypes[2])) {
			SRRForder = -1;
		} else if (SRRFType.equals(SRRFTypes[3])) {
			SRRForder = Integer.parseInt(tempSRRForder);
			MemmorySRRForder = SRRForder;
			txtF_SRRForder.setEditable(true);
		}
		txtF_SRRForder.setText("" + SRRForder);
		txtF_ExpFactor.setText("" + ExpFactor);
		txtF_RadMag.setText("" + RadMag);

		integrate_temporal_correlations = Prefs.get(AdOptPref + "doIntegrateLagTimes", true);

		Radiality_RPC = Prefs.get(AdOptPref + "removeRadialityPositivityConstraint", true);
		Radiality_Renorm = Prefs.get(AdOptPref + "renormalize", false);
		Radiality_DoGS = Prefs.get(AdOptPref + "doGradSmooth", true);

		Weighting_DoIW = Prefs.get(AdOptPref + "doIntensityWeighting", true);
		Weighting_DoGW = Prefs.get(AdOptPref + "doGradWeight", true);
		// 1.35 sigma (s) of the widefield PSF (135 nm).
		psf_fwhm = Prefs.get(AdOptPref + "PSF_Width", 1.35) * 2.35; // 2*sqrt(2ln(2)) 3.17nm

		Minimize_SRRF_patterning = Prefs.get(AdOptPref + "doMinimizePatterning", true);
	}

	void savePrefs() {
		Prefs.set("nanoj.srrf.java.gui.SRRFAnalysis_.radialityMagnification", RadMag);
		Prefs.set(AdOptPref + "SRRForder", SRRForder);
		Prefs.set(AdOptPref + "SRRFType", SRRFType);
		Prefs.set("onemic.srrf.java.gui.Expansion_Factor", ExpFactor);

		Prefs.set(AdOptPref + "doIntegrateLagTimes", integrate_temporal_correlations);

		Prefs.set(AdOptPref + "removeRadialityPositivityConstraint", Radiality_RPC);
		Prefs.set(AdOptPref + "renormalize", Radiality_Renorm);
		Prefs.set(AdOptPref + "doGradSmooth", Radiality_DoGS);

		Prefs.set(AdOptPref + "doIntensityWeighting", Weighting_DoIW);
		Prefs.set(AdOptPref + "doGradWeight", Weighting_DoGW);
		Prefs.set(AdOptPref + "PSF_Width", psf_fwhm / 2.35);

		Prefs.set(AdOptPref + "doMinimizePatterning", Minimize_SRRF_patterning);
	}

	void updateFullScale() {
		DistScale = (double) Double.parseDouble(txtF_DistScale.getText());
		tempResolution = DistScale == 0 ? tempResolution : DistScale;
		double ScaleNumber = tempResolution * (double) Double.parseDouble(txtF_ExpFactor.getText())
				* (double) Double.parseDouble(txtF_RadMag.getText());
		ScaleNumber = roundDec3(ScaleNumber);
		txtF_fullScale.setText("" + ScaleNumber);
	}

	public static double roundDec3(double num) {
		num = Math.round(num * 1000);
		return num / 1000;
	}

	public void LoadImportedData() {
		StackDirectories.clear();
		Thread GetValues = new Thread(new Runnable() {
			@Override
			public void run() {
				String ImgsPath = ImportF.getText();
				File CheckPath = new File(ImgsPath);
				if (CheckPath.isDirectory()) {
					StackDirectories = CheckDirectory(ImgsPath);
					if (StackDirectories == null) {
						if (!ImageDetected) {
							TopStatusLabel.setText("");
						}
					} else {
						try {
							if (!TheInputIsVideos) {
								{
									OriginalImp = FolderOpener.open(StackDirectories.get(0), "virtual");
									StackLength = OriginalImp.getStackSize();
								}
							}
							if (StackDirectories.size() == 1) {
								ImageDetected = true;
								MultiImageDetected = false;
								TopStatusLabel.setText("video loaded successfully!");

							} else {
								ImageDetected = true;
								MultiImageDetected = true;
								TopStatusLabel.setText(StackDirectories.size() + " videos were detected");
								txtF_EndOnVideo.setText(String.valueOf(StackDirectories.size()));
							}
							updateFullScale();
						} catch (Exception e) {
							ImageDetected = false;
							MultiImageDetected = false;
							TopStatusLabel.setText("Nothing detected! please import image sequence/s or video/s.");
						}
					}
				} else if (OneVideo) {
					StackDirectories = CheckDirectory(ImgsPath);
					SubFolderNameList.add("Null");
					ImagePresent = false;
					ImageDetected = true;
					MultiImageDetected = false;
					TopStatusLabel.setText("Images stack loaded successfully!");
				}
			}
		});
		GetValues.start();
	}

	public boolean showDialog() {
		GenericDialog GD = new GenericDialog("Advanced Settings");
		GD.addCheckbox("Integrate_Temporal_Correlations (default: active)", integrate_temporal_correlations);

		GD.addMessage(" 		 Radiality 		");
		GD.addCheckbox("Remove_Positivity_Constraint (default: enabled)", Radiality_RPC);
		GD.addCheckbox("Renormalize (default: disabled, activate for 2D structures)", Radiality_Renorm);
		GD.addCheckbox("Do_Gradient_Smoothing (default: active, activate in low-density)", Radiality_DoGS);

		GD.addMessage(" 		 Weighting 		");
		GD.addCheckbox("Do_Intensity_Weighting (default: active)", Weighting_DoIW);
		GD.addCheckbox("Do_Gradient_Weighting (default: active, activate in low-SNR, unstable)", Weighting_DoGW);

		GD.addMessage(" 		 Correction 		");
		GD.addCheckbox("Minimize_SRRF_patterning (default: active, experimental)", Minimize_SRRF_patterning);

		GD.showDialog();
		if (GD.wasCanceled()) {
			return false;
		}
		integrate_temporal_correlations = GD.getNextBoolean();
		Radiality_RPC = GD.getNextBoolean();
		Radiality_Renorm = GD.getNextBoolean();
		Radiality_DoGS = GD.getNextBoolean();
		Weighting_DoIW = GD.getNextBoolean();
		Weighting_DoGW = GD.getNextBoolean();

		Minimize_SRRF_patterning = GD.getNextBoolean();
		return true;
	}


}
