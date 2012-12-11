================================================================
ARTERIOJ README
================================================================

ArterioJ is an open-source collateral vessel analysis software tool
developed for vascular biology research.  Its use assumes at least a
basic working knowledge of ImageJ.  A copy of ImageJ is bundled with
the distribution; another copy of ImageJ can exist on your system
without interaction.  Also included in the bundled ImageJ is another
plugin MosaicJ and its dependent modules.  The system was developed
under Java 1.5 but earlier versions may work as well.

================================================================
INSTALLATION
================================================================

1. Extract the distribution to the location of your choice. 

2. Ant (Java build tool) is pretty much required for recompilation.
Any software development platform with Java should also have ant but
you may need to download this as well.

3. A database is not required to use ArterioJ, but it was developed
with the use-case in mind that a database will be used.  The software
was developed with MySQL but other databases should work as well.

4. Generation of plots is dependent upon R.  This is a high-quality
statistical toolset that generates really great looking plots.
Installation of R is not detailed here.

5. Toggling a couple of settings within ImageJ is useful for usage:

* Turn off 'Auto-Measure' in Edit->Options->Point Tool...
* Turn on 'Require Command Key for Shortcuts' in Edit->Options->Misc...
* Set 'background color' to black

6. Set the pixelwidth.  ArterioJ needs to know how big each pixel is,
you have to tell it what that is.  To do this, take a picture of a
stage micrometerusing the same objective as your specimen, feed it
through the same postprocessing steps you would otherwise do to your
sample, and then use this to calculate.  To do this, open the image of
the micrometer, select Analyze->Set Scale..., measure the appropriate
distance, and enter this into the dialog box.  Use google to get more
information on how to do this if you do not already know.  You can
then use the pixelwidth and send this into ArterioJ as a system
parameter (see the build.xml file).

      <sysproperty key="arterioj.calibration.pixelwidth" value="0.8"/>
      <sysproperty key="arterioj.calibration.units" value="microns"/>

================================================================
USING ARTERIOJ
================================================================

First, get a sense of the available ant targets:

$ ant -projecthelp
 compile
 db.recreate
 db.reset-root-password
 db.setup
 ij
 imagej
 init
 install.plugin
 mosaicj
 stats


Launch ImageJ, without launching ArterioJ

$ ant imagej

Launch MosaicJ, without launching ArterioJ

$ ant imagej

Launch ArterioJ to open the file specified in the build.xml file (look
at the code for the 'ij' task).

$ ant ij

================================================================
MODES
================================================================

While editing an ArterioJ object model, the system is in one of the
following modes:

	public void keyTyped(KeyEvent e) {
	    switch (e.getKeyChar()) {
	    case 'b': select(bifs.getName()); break;
	    case 'v': select(vessels.getName()); break;
	    case 'd': select(diameters.getName()); break;
	    case 'h': Toolbar.getInstance().setTool("hand"); break;
	    case 's': 
		commands.execute("save");
		commands.execute("sum");
		break;
	    case '?': commands.usage(); break;
	    }
	}

================================================================
KEYBOARD ACTIONS
================================================================

ArterioJ is used by using the mouse and keyboard in combination.  You
can see the various key actions here or search for them in the code.

	public void keyTyped(KeyEvent e) {
	    //debug("ObjManager.keyTyped:"+e);
	    switch (e.getKeyChar()) {
	    case 'r':
		remove(choice.getSelectedItem());
		rois.redraw();
		break;
	    case 'u':
		if (ops.isEmpty()) {
		    message("Nothing to undo.");
		    return;
		}
		((UndoOperation)ops.pop()).undo();
		rois.redraw();
		break;
	    case 'l':
		String label = labels.current();
		if (label != null) {
		    Obj b = current();
		    if (b != null) {
			if (b.getLabel() != null) {
			    b.setLabel(null); // toggle on-off
			} else {
			    b.setLabel(label);
			    info("Labeled "+b+" as '"+label+"'");
			}
			rois.redraw();
		    }
		}
		break;
	    case 'z':
		previous();
		rois.redraw();
		break;
	    case 'x':
		next();
		rois.redraw();
		break;
	    }
	    super.keyTyped(e);
	}

================================================================
COMMANDS
================================================================

Various other functions can be entered into the command entry
box. These are detailed in the code but you can see a previous here.

	public CommandManager() {
	    this.commands = new HashMap();
	    put(new LimitCommand());
	    put(new HelpCommand());
	    put(new SaveCommand());
	    put(new QuitCommand());
	    put(new LinkCommand());
	    put(new DissectCommand());
	    put(new SnapshotCommand());
	    put(new CleanDBCommand());
	    put(new SwapCollateralDirectionCommand());
	    put(new SummarizeCommand());
	}

================================================================
LABELS
================================================================

You can assign labels to an object such as least collateral
segments. The dictionary of labels can be specified as an xml file and
passed to ArterioJ.  See labels.xml in src/etc

      <sysproperty key="arterioj.labels.xml" value="${basedir}/src/etc/labels.xml"/> 

================================================================
STEP 1: SPECIFY BIFURCATION POINTS
================================================================

For each image, you want to specify the bifurcation points that
deconstruct the vessel path of interest into segments.  Change the
'Mode' to Bifurcation.  Then single click on the bifurcation points.
As you do this, a new segment is drawn from the current active
bifurcation. To change the active bifurcation, either select it from
the drop down list or scroll through them using the 'z' and 'x'
keys. Here you get a sense of what the keys are in most modes:


================================================================
STEP 2: SPECIFY COLLATERAL SEGMENTS
================================================================

Once you have defined that segments, you'll want to connect them up by
specifying the least collateral segments.  To do this, write down the
numbers of the corresponding bofurcation points (each one has a unique
name/number). Then in the command box (whch can be auto-selected by
pressing the 'c' key), type:

> link 12.34

This will create a collateral segment between points 12 and 34.  The
order of the numbers does matter, in general you want to put the
number of the tributary bifurcation first.

You will also want to provide labels to these least collateral
segments.  

================================================================
STEP 3: SPECIFY DIAMETER MEASUREMENTS
================================================================

Now go into diameter mode ('d').  The active segment will be
highlighted but you can zoom on this with the previous 'z' and next
'x' keys.  Press and hold the mouse button down as you drag a line
perpendicularly across the segment.  Repeat as necessary until the
course of the vessel is reasonably approximated.  The threshold for
finding the edge of the vessel is set at an arbitrary value that will
probably need to be changed for your experiment.  You can pass this is
as a system property (see the build.xml file) or specify as a command:

> limit 70

To disable the threshold function completely, set the limit to 0.
This is often necessary when there is alot of overlapping vessels that
the computer cannot really distinguish where the edge of the vessel
really is.

================================================================
STEP 4: Save
================================================================

Save the object model.  It will be written to an xml file having the
same filename as the original image albeit with .arterioj.xml tacked
on.

================================================================
STEP 5: Summarize, generate dissection plots and timeseries plots.
================================================================

I've you've gotten this far, you're clearly a dedicated user.  Please
contact me to go over how to set this up for your experiment.  I can
be reached at:

pcj127@gmail.com

