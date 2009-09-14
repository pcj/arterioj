import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;
import ij.io.FileInfo;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ColorBlitter;
import ij.process.Blitter;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.measure.Calibration;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.TextField;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Good website: http://chortle.ccsu.edu/VectorLessons/vectorIndex.html

// TODO: output of stats to incorporate ?Calibration units/scale
// TODO: bezier curves
// TODO: stats
// TODO: does super.init() need to be called?
//
public class ArterioJ_ extends PlugInFrame implements ImageListener {

    // --------------------------------------------------------------------------------
    // Constants
    // --------------------------------------------------------------------------------

    public static final String NEWLINE = System.getProperty("line.separator");
    public static final double SMALL_ERROR = 0.0000001;
    public static final double DEFAULT_LIMIT = 50.0;
    public static final DecimalFormat df3 = new DecimalFormat("#.###");
    public static final boolean DoExtendedDissection = false;

    // --------------------------------------------------------------------------------
    // Exceptions
    // --------------------------------------------------------------------------------

    public static class ThresholdException extends RuntimeException {
	public ThresholdException(String msg) { super (msg); }
    }

    // --------------------------------------------------------------------------------
    // Instance fields
    // --------------------------------------------------------------------------------

    Frame imagej;
    ImagePlus image;
    ImageCanvas canvas;
    ImageWindow window;
    FileInfo info;
    Calibration cal;
    
    List managers;
    DatabaseManager db;
    RoiManager rois;
    ModeManager modes;
    LabelManager labels;
    BifurcationManager bifs;
    VesselManager vessels;
    DiameterManager diameters;
    CommandManager commands;

    Integer image_id;

    // Properties
    double maxZoom;
    double pixelWidth;
    String units;
    boolean mysqlRead;
    boolean mysqlWrite;
    boolean xmlRead;
    boolean xmlWrite;
    String jdbc_url;

    // --------------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------------

    public ArterioJ_() {
	super("ArterioJ");
	this.imagej = IJ.getInstance();
	this.image = IJ.getImage();
	this.canvas = image.getCanvas();
	this.window = image.getWindow();
	this.info = image.getOriginalFileInfo();
	this.cal = image.getCalibration();

	WindowManager.addWindow(this);
	setVisible(true);

	// Place imagej frame in top corner
	if (!isBatchMode()) {

	    imagej.setLocation(0,0);
	    
	    // Put us beneath it
	    this.setTitle("ArterioJ (editing data for "+info.directory + info.fileName+")");
	    this.setLocation(0, imagej.getY() + imagej.getHeight());
	    this.setSize(1400,60);
	    
	    // and then the imagewindow below that
	    window.setLocation(0, this.getY() + this.getHeight());
	    window.toFront();
	    
	    // and then the log window
	    Frame log = WindowManager.getFrame("Log");
	    if (log != null) {
		log.setLocation(0, window.getY() + window.getHeight());
		log.setSize(window.getWidth(), 150);
	    }
	}

	info("Calibration: one pixel corresponds to "+getPixelWidth()+" "+getUnits());
	info("Ready.");

	load();

	info("ImageID: "+getImageID());

	if (isBatchMode()) {
	    commands.execute("save");
	    commands.execute("sum");
	}
    }
 
    // --------------------------------------------------------------------------------
    // Frame methods
    // --------------------------------------------------------------------------------
   
    /** 
     * Overrides Component.addNotify(). init() must be called after
     * addNotify().
     */
    public void addNotify() {
        super.addNotify();
	init();
    }

    public void init() {
	info("Initializing...");
	// Initilize system properties
	pixelWidth = getPropertyAsDouble("arterioj.calibration.pixelwidth", -1.0);
	units = getPropertyAsString("arterioj.calibration.units", null);
	mysqlRead = getPropertyAsBoolean("arterioj.mysql.read", true);
	mysqlWrite = getPropertyAsBoolean("arterioj.mysql.write", true);
	xmlRead = getPropertyAsBoolean("arterioj.xml.read", true);
	xmlWrite = getPropertyAsBoolean("arterioj.xml.write", true);
	maxZoom = getPropertyAsDouble("arterioj.diameter.maxZoom", 16.0);
	image.addImageListener(this);
	
	setLayout(new FlowLayout());
	setForeground(Color.darkGray);
	setResizable(true);
	setFont(new Font("Helvetica", Font.PLAIN, 12));
	
	managers = new LinkedList();
	if (mysqlRead || mysqlWrite) {
	    db = new DatabaseManager(); managers.add(db);
	} else {
	    info("Skipping initialization of DatabaseManager (user says no)");
	}
	rois = new RoiManager(); managers.add(rois);
	modes = new ModeManager(); managers.add(modes);
	bifs = new BifurcationManager(); managers.add(bifs);
	vessels = new VesselManager(); managers.add(vessels);
	diameters = new DiameterManager(); managers.add(diameters);
	commands = new CommandManager(); managers.add(commands);
	labels = new LabelManager(); managers.add(labels);

	Iterator i = managers.iterator();
	while (i.hasNext()) 
	    ((Manager)i.next()).init();
    }
    
    public void close() {
	image.removeImageListener(this);

	save();

	Iterator i = managers.iterator();
	while (i.hasNext()) 
	    ((Manager)i.next()).destroy();

	super.close();
    }

    // --------------------------------------------------------------------------------
    // ImageListener methods
    // --------------------------------------------------------------------------------

    public void imageClosed(ImagePlus imp) { close(); }
    public void imageUpdated(ImagePlus imp) {}
    public void imageOpened(ImagePlus imp) {}

    // --------------------------------------------------------------------------------
    // Logging methods
    // --------------------------------------------------------------------------------

    public static void trace(String msg) {
	System.out.println("[ArterioJ][trace] " + msg);
    }
    
    public static void debug(String msg) {
	write("[ArterioJ][debug] " + msg);
    }
    
    public static void info(String msg) {
	write("[ArterioJ][info] " + msg);
    }
    
    public static void warn(String msg) {
	write("[ArterioJ][warning] " + msg);
    }
    
    public static void message(String msg) {
	write("[ArterioJ] "+msg);
    }

    public static void write(String msg) {
	if (isBatchMode())
	    System.out.println(msg);
	else {
	    System.out.println(msg);
	    //IJ.log(msg);
	}
    }

    public static boolean isBatchMode() {
	return IJ.getInstance() == null;
    }

    // --------------------------------------------------------------------------------
    // Calibration methods
    // --------------------------------------------------------------------------------

    public double calibrate(double val) {
	return val * getPixelWidth();
    }

    public double getPixelWidth() {
	return pixelWidth > 0 ? pixelWidth : cal.pixelWidth;
    }

    public String getUnits() {
	return units != null ? units : cal.getUnits();
    }

    // --------------------------------------------------------------------------------
    // Property methods
    // --------------------------------------------------------------------------------

    public static String getPropertyAsString(String key, String def) {
	String r = System.getProperty(key, def);
	info("Property '"+key+"' set to '"+r+"'");
	return r;
    }

    public static boolean getPropertyAsBoolean(String key, boolean def) {
	String s = System.getProperty(key);
	Boolean r = null;
	if (s != null) {
	    r = Boolean.valueOf(s);
	} else {
	    r = new Boolean(def);
	}
	info("Property '"+key+"' set to '"+r+"'");
	return r.booleanValue();
    }

    public static double getPropertyAsDouble(String key, double def) {
	String val = System.getProperty(key);
	Double r = new Double(def);
	if (val != null) {
	    try { 
		r = Double.valueOf(val);
	    } catch (Exception ex) {
		warn("Attempted to grab double value '"+key+"' from the system environment but this happened: "+ex.getMessage());
	    }
	} 
	info("Property '"+key+"' set to '"+r+"'");
	return r.doubleValue();
    }

    // ---------------------------------------------------
    // Save and Load Methods
    // ---------------------------------------------------

    public String getImageFileName() {
	return info.directory + info.fileName;
    }

    public String getXMLFileName() {
	String dir = info.directory;
	String fileName = info.fileName;

	if (dir == null || fileName == null || fileName.equals("Untitled")) {
	    return null;
	}
	
	String xmlFileName = dir + fileName + ".arterioj.xml";
	return xmlFileName;
    }

    protected void load() {
	if (mysqlRead) {
	    fromSQL();
	}
	if (xmlRead) {
	    fromXML(getXMLFileName());
	}
    }

    protected void fromSQL() {
	Iterator i = managers.iterator();
	while (i.hasNext()) {
	    ((Manager)i.next()).fromSQL();
	    rois.redraw();
	}
    }

    protected void fromXML(String filename) {
	
	File file = new File(filename);
	if (!file.exists()) {
	    return;
	}

	try {
	    
	    Element d = DocumentBuilderFactory.newInstance()
		.newDocumentBuilder()
		.parse(file)
		.getDocumentElement();

	    String imageFileName = d.getAttribute("image");
	    if (!imageFileName.equals(getImageFileName())) {
		warn("XML data file and target image filename do not match! Are you sure you want to do this?");
	    }

	    Iterator i = managers.iterator();
	    while (i.hasNext()) {
		Manager m = (Manager)i.next();
		m.fromXML(d);
		rois.redraw();
		snapshot(m.getClass().getName());
	    }

	    i = managers.iterator();
	    while (i.hasNext()) {
		Manager m = (Manager)i.next();
		m.clearChanged();
	    }

	    info("Loaded data from "+filename);

	} catch (Exception e) {
	    warn("Error while trying to parse "+filename);
	    e.printStackTrace();
	} 
	rois.redraw();
    }

    public void save() {
	save(getXMLFileName());
    }

    public void save(String filename) {
	boolean hasChanged = false;

	Iterator i = managers.iterator();
	while (i.hasNext()) {
	    Manager m = (Manager)i.next();
	    hasChanged |= m.hasChanged();
	    //debug("Manager "+m.getClass()+".hasChanged(): "+m.hasChanged());
	}

	//if (hasChanged) {
	if (xmlWrite) {
	    toXML(filename);
	}
	if (mysqlWrite) {
	    toSQL();
	}
	//} else {
	//  info("Save aborted, file is up to date.");
	//}
    }

    protected void toXML(String filename) {
	PrintWriter out = null;
	try {
	    out = new PrintWriter(new FileWriter(filename));
	    out.println("<?xml version=\"1.0\"?>");
	    out.println("<Arterioj image=\""+getImageFileName()+"\">");

	    Iterator i = managers.iterator();
	    while (i.hasNext()) 
		((Manager)i.next()).toXML(out);

	    out.println("</Arterioj>");
	    info("Saved data to "+filename);

	    i = managers.iterator();
	    while (i.hasNext()) {
		Manager m = (Manager)i.next();
		m.clearChanged();
	    }

	} catch (IOException e) {
	    warn(e.getMessage());
	} finally {
	    if (out != null) {
		out.close();
	    }
	}
    }

    protected void toSQL() {
	Iterator i = managers.iterator();
	while (i.hasNext()) 
	    ((Manager)i.next()).toSQL();
	
	info("Saved data to "+jdbc_url);
	
	i = managers.iterator();
	while (i.hasNext()) {
	    Manager m = (Manager)i.next();
	    m.clearChanged();
	}
    }

    protected Integer getImageID() {
	if (!mysqlRead)
	    image_id = new Integer(1);

	// First see if its already loaded.
	if (image_id == null) {
	    // try to load it
	    String filename = info.fileName;
	    image_id = db.rowid("SELECT id FROM image WHERE name='"+filename+"'");
	}

	// Ok, need to make a new entry in the image table
	if (image_id == null) {
	    // try to load it
	    image_id = db.insert("INSERT INTO image (name, path) VALUES ('"+info.fileName+"', '"+info.directory+"')");
	}
	return image_id;
    }

    public static Integer decodeInteger(String key) {
	try {
	    return Integer.decode(key);
	} catch (NumberFormatException e) {
	    throw new IllegalArgumentException("Attempt to convert '"+key+"' to an Integer failed");
	}
    }
	
    public void snapshot(String s) throws Exception {
	snapshot(new Robot(), s);
    }
    
    public void snapshot(Robot robot,  String s) throws Exception {
	String filename = info.directory + info.fileName + ".snapshot-"+s+".png";
	debug("Capturing snapshot to "+filename);
	//IJ.save(filename);
	//Rectangle rect = new Rectangle(canvas.getX(), canvas.getY(), canvas.getWidth(), canvas.getHeight());
	Rectangle rect = new Rectangle(window.getX(), window.getY(), window.getWidth(), window.getHeight());
	debug("Snapshot rectangle: "+rect);
	//Rectangle rect = new Rectangle(image.getX(), image.getY(), image.getWidth(), image.getHeight());
	java.awt.image.BufferedImage buff = robot.createScreenCapture(rect);
	javax.imageio.ImageIO.write(buff, "PNG", new File(filename));
    }
    

    // ********************************************************************************
    //
    // Section II: Inner classes
    //
    // ********************************************************************************

    // ================================================================================
    // Manager Classes
    // ================================================================================

    /**
     * Root class for Collection Manager objects that (usually)
     * interact with the GUI.
     */
    public abstract class Manager implements ItemListener, KeyListener, MouseListener, MouseMotionListener {
	boolean hasChanged = false;

	public boolean hasChanged() {
	    return hasChanged;
	}

	public void clearChanged() {
	    hasChanged = false;
	}

	public void itemStateChanged(ItemEvent e) {}
	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseDragged(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {} 
	public void mouseExited(MouseEvent e) {} 
	public void init() {}
	public void destroy() {}
	public void toXML(PrintWriter out) {}
	public void fromXML(Element d) {}
	public void toSQL() {}
	public void fromSQL() {}
    }

    /**
     * Manages the list of Roi objects. A vector is required with the
     * setDisplayList method.
     */
    public class RoiManager extends Manager {
	java.util.Vector visible; 
	
	public RoiManager() {
	    visible = new java.util.Vector();
	}

	public void init() {
	    if (canvas != null) 
		canvas.setDisplayList(visible);
	}

	public void destroy() {
	    if (canvas != null)
		canvas.setDisplayList(null);
	}

	public void redraw() {
	    //debug("redraw: start");
	    if (canvas != null) {
		canvas.setDisplayList(visible);
	    }
	    if (window != null)
		window.toFront();
	    //debug("redraw: end");
	}
	
	public void put(Roi roi) {
	    if (!visible.contains(roi)) {
		visible.add(roi);
	    }
	}
	
	public void remove(Roi roi) {
	    visible.remove(roi);
	}
	
	public void clearActive() {
	    Roi nil = null;
	    image.setRoi(nil);
	}
    }

    public class DatabaseManager extends Manager {
	Connection conn;

	public DatabaseManager() {
	}

	public void init() {
	    try {
		jdbc_url = getPropertyAsString("arterioj.jdbc.connection", "jdbc:mysql//localhost/gracilis");
		String driver = getPropertyAsString("arterioj.jdbc.driver", "com.mysql.jdbc.Driver");
		String username = getPropertyAsString("arterioj.jdbc.username", "arterioj");
		String password = getPropertyAsString("arterioj.jdbc.password", "arterioj");

		info("Attached to database "+jdbc_url);

		// Load driver
		Class.forName(driver);
		
		// Setup connection
		conn = DriverManager.getConnection(jdbc_url, username, password);

	    } catch (Exception ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public void destroy() {
	    try {
		conn.close();
	    } catch (Exception e) {
	    }
	}

	public Integer rowid(String q) {
	    Statement s = null;
	    ResultSet r = null;
	    try {
		s = conn.createStatement();
		r = s.executeQuery(q);
		while (r.next()) {
		    int rowid = r.getInt(1);
		    return new Integer(rowid);
		}
	    } catch (SQLException e) {
		e.printStackTrace();
		throw new RuntimeException(e.getMessage());
	    } finally {
		try {
		    if (r != null) r.close();
		    if (s != null) s.close();
		} catch (Exception ex) {
		}
	    }
	    return null;
	}

	public void first(String q, Callback f) {
	    Statement s = null;
	    ResultSet r = null;
	    try {
		s = conn.createStatement();
		r = s.executeQuery(q);
		Results data = new Results(r);
		while (data.next()) {
		    f.result(data);
		    break;
		}
	    } catch (SQLException e) {
		e.printStackTrace();
		throw new RuntimeException(e.getMessage());
	    } finally {
		try {
		    if (r != null) r.close();
		    if (s != null) s.close();
		} catch (Exception ex) {
		}
	    }
	}

	public void query(String q, Callback f) {
	    Statement s = null;
	    ResultSet r = null;
	    try {
		s = conn.createStatement();
		r = s.executeQuery(q);
		f.result(new Results(r));
	    } catch (SQLException e) {
		e.printStackTrace();
		throw new RuntimeException(e.getMessage());
	    } finally {
		try {
		    if (r != null) r.close();
		    if (s != null) s.close();
		} catch (Exception ex) {
		}
	    }
	}

	public Integer insert(String query) {
	    Statement s = null;
	    ResultSet r = null;
	    Integer key = null;
	    try {
		s = conn.createStatement();
		int n = s.executeUpdate(query);
		if (n > 0) {
		    r = s.executeQuery("SELECT LAST_INSERT_ID()");
		    while (r.next()) {
			key = new Integer(r.getInt(1));
			break;
		    }
		} 
	    } catch (SQLException e) {
		e.printStackTrace();
		throw new RuntimeException(e.getMessage());
	    } finally {
		try {
		    if (r != null) r.close();
		    if (s != null) s.close();
		} catch (Exception ex) {
		}
	    }
	    return key;
	}

	public boolean execute(String query) {
	    Statement s = null;
	    boolean success = false;
	    try {
		s = conn.createStatement();
		success = (s.executeUpdate(query) > 0);
	    } catch (SQLException e) {
		e.printStackTrace();
		throw new RuntimeException(e.getMessage());
	    } finally {
		try {
		    if (s != null) s.close();
		} catch (Exception ex) {
		}
	    }
	    return success;
	}
    }

    public interface Callback {
	void result(Results r);
    }

    public class Results {
	ResultSet rs;
	public Results(ResultSet rs) {
	    this.rs = rs;
	}

	public String getString(String k) {
	    try {
		return rs.getString(k);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public int getInt(String k) {
	    try {
		return rs.getInt(k);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public Integer getInteger(String k) {
	    return new Integer(getInt(k));
	}

	public double getDouble(String k) {
	    try {
		return rs.getDouble(k);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public boolean getBoolean(String k) {
	    try {
		return rs.getBoolean(k);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public String getString(int i) {
	    try {
		return rs.getString(i);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public Integer getInteger(int i) {
	    try {
		return new Integer(rs.getInt(i));
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public int getInt(int i) {
	    try {
		return rs.getInt(i);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public double getDouble(int i) {
	    try {
		return rs.getDouble(i);
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}

	public boolean next() {
	    try {
		return rs.next();
	    } catch (SQLException ex) {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	    }
	}
    }

    /**
     * Reads an optional xml configuration file that is used as a
     * dictionary for labeling objects.
     */
    public class LabelManager extends Manager {
	Choice choice;

	public LabelManager() {
	}

	public void init() {
	    add(new Label("Label:"));
	    choice = new Choice();
	    choice.addItemListener(this);

	    String filename = System.getProperty("arterioj.labels.xml");
	    if (filename != null) {
		loadXML(filename);
	    } else {
		if (mysqlRead) {
		    loadSQL();
		}
	    }
	    add(choice); // to frame
	}

	protected void loadSQL() {
	    String q = "SELECT id, name FROM label";
	    db.query(q, new Callback() {
		    public void result(Results r) {
			while (r.next()) {
			    int id = r.getInt(1);
			    String name = r.getString(2);
			    choice.add(name);
			}
		    }
		});
	}

	protected void loadXML(String filename) {
	    File file = new File(filename);
	    if (!file.exists()) {
		warn("Error while trying to load labels from "+filename+": file does not exist");
		return;
	    }
	    
	    try {
		
		Element d = DocumentBuilderFactory.newInstance()
		    .newDocumentBuilder()
		    .parse(file)
		    .getDocumentElement();
		
		NodeList l = d.getElementsByTagName("label");
		for (int i = 0; i < l.getLength(); i++) {
		    Element e = (Element)l.item(i);
		    String name = e.getAttribute("key");
		    String desc = e.getAttribute("description");
		    choice.add(name);
		    debug("Loaded label '"+name+"': "+desc);
		}
		
	    } catch (Exception e) {
		warn("Error while trying to parse "+filename);
		e.printStackTrace();
	    } 
	}

	public void destroy() {
	}

	public void itemStateChanged(ItemEvent e) {
	    window.toFront();
	}

	public String current() {
	    return choice.getSelectedItem();
	}
    }

    /**
     * Manages a set of ObjManager classes, each of which represents a
     * different 'mode', or different type of object that can be
     * edited.
     */
    public class ModeManager extends Manager {
	ObjManager current;
	Choice choice;
	Map modes;

	ModeManager() {
	    modes = new HashMap();
	}

	public void init() {
	    if (window != null) window.addKeyListener(this);
	    if (canvas != null) {
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	    }

	    add(new Label("Mode:"));
	    choice = new Choice();
	    choice.addItemListener(this);
	    add(choice); // to frame
	    put(bifs);
	    put(vessels);
	    put(diameters);
	    select(bifs.getName());
	}

	public void destroy() {
	    window.removeKeyListener(this);
	    canvas.removeKeyListener(this);
	    canvas.removeMouseListener(this);
	    canvas.removeMouseMotionListener(this);
	}

	protected void put(ObjManager mode) {
	    modes.put(mode.getName(), mode);
	    choice.add(mode.getName());
	}

	public void itemStateChanged(ItemEvent e) {
	    select(e.getItem().toString());
	    window.toFront();
	}

	public void select(String name) {
	    ObjManager mgr = (ObjManager)modes.get(name);
	    choice.select(name);
	    //debug("Selecting manager: "+name);
	    mgr.select();
	    current = mgr;
	}

	public void keyTyped(KeyEvent e) {
	    //debug("ModeManager.keyTyped: "+e);
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
	    if (current != null)
		current.keyTyped(e);
	}

	public ObjManager current() {
	    return current;
	}

	public void keyReleased(KeyEvent e) { if (current != null) current.keyReleased(e); }
	public void keyPressed (KeyEvent e) { if (current != null) current.keyPressed(e); }

	public void mouseClicked(MouseEvent e)  { if (current != null) current.mouseClicked(e); }
	public void mousePressed(MouseEvent e)  { if (current != null) current.mousePressed(e); }
	public void mouseReleased(MouseEvent e) { if (current != null) current.mouseReleased(e); }
	public void mouseDragged(MouseEvent e)  { if (current != null) current.mouseDragged(e); }
	public void mouseMoved  (MouseEvent e)  { if (current != null) current.mouseMoved(e); }
	public void mouseEntered(MouseEvent e)  { if (current != null) current.mouseEntered(e); } 
	public void mouseExited (MouseEvent e)  { if (current != null) current.mouseExited(e); } 

    }
    
    /**
     * Base class for all the Manager classes that handle Obj
     * objects.
     */
    public abstract class ObjManager extends Manager {
	Map objects;
	Stack ops;
	Choice choice;
	Obj current;
	String name, table;

	protected ObjManager(String name, String table) {
	    this.name = name.intern();
	    this.table = table.intern();
	    this.objects = new HashMap();
	    this.ops = new Stack();
	}

	public Collection values() {
	    return objects.values();
	}

	public String getName() {
	    return name;
	}

	public Obj get(String key) {
	    if (key == null || key.length() == 0) {
		return null;
	    } else {
		return get(decodeInteger(key));
	    }
	}

	public Obj get(int id) {
	    return get(new Integer(id));
	}

	public Obj get(Integer key) {
	    Obj obj = (Obj)objects.get(key);
	    return obj;
	}

	public void fromSQL() {
	    String q = "SELECT * FROM "+table+" WHERE image_id="+getImageID();
	    db.query(q, new Callback() {
		    public void result(Results r) {
			while (r.next()) {
			    put(load(r));
			}
		    }
		});
	}

	public Set keys() {
	    return objects.keySet();
	}

	public void init() {
	    this.choice = new Choice();
	    this.choice.add("None");
	    this.choice.addItemListener(this);
	    add(new Label(name+":")); // to Frame
	    add(choice); // to Frame
	}

	public int size() {
	    return objects.size();
	}

	public void itemStateChanged(ItemEvent e) {
	    select(e.getItem().toString());
	    rois.redraw();
	}

	protected Integer nextKey() {
	    int i = 1;
	    while (true) {
		Integer key = new Integer(i);
		if (!objects.containsKey(key)) {
		    return key;
		}
		i++;
	    }
	}

	protected void put(Obj obj) {
	    if (objects.containsKey(obj.getKey())) {
		Obj other = (Obj)objects.get(obj.getKey());
		if (!other.getLocation().equals(obj.getLocation())) {
		    throw new IllegalStateException("Key mismatch between "+obj+" and "+other);
		}
	    } else {
		objects.put(obj.getKey(), obj);
		hasChanged = true;
		choice.add(obj.getKeyAsString());
		ops.push(new UndoAddOperation(obj));
		obj.add();
		select(obj);
	    }
	}

	public void remove(String key) {
	    if (!"None".equals(key)) {
		remove(get(decodeInteger(key)));
	    } 
	}

	protected void remove(Obj obj) {
	    if (objects.containsKey(obj.getKey())) {
		objects.remove(obj.getKey());
		obj.remove();
		hasChanged = true;
		// TODO: fix this to check if current is selected
		int idx = choice.getSelectedIndex();
		choice.remove(obj.getKeyAsString());	
		if (idx > 0) {
		    select(choice.getItem(idx - 1));
		} else {
		    int k = choice.getItemCount();
		    select(choice.getItem(k - 1));
		}
	    }
	}

	public void select(String key) {
	    if (!"None".equals(key)) {
		select(get(decodeInteger(key)));
	    } else {
		if (current != null) {
		    current.deselect();
		    current = null;
		}
	    }
	}

	public void select(Obj obj) {
	    if (current != obj) {
		Obj prev = current;
		current = obj;
		obj.select();
		choice.select(obj.getKeyAsString());
		if (prev != null) {
		    prev.deselect();
		}
	    }
	}

	// TODO: selection and deselection of objects may not keep the
	// choice currrent.  This about this.
	public void deselect(Obj obj) {
	    if (current == obj) {
		current = null;
		obj.deselect();
	    }
	}

	public Obj current() {
	    return current;
	}

	public void previous() {
	    int i = choice.getSelectedIndex();
	    int k = choice.getItemCount();
	    //debug("previous(): selected = '"+i+"', n = '"+k+"'");
	    if (k > 1) { // if there are more choices than just "None"
		int x = i - 1; // go to the previous item by default
		if (x <= 0) { // if this puts us at the beginning
		    x = k - 1; // then go to the end
		}
		select(choice.getItem(x));
	    } 
	}
	
	public void next() {
	    //debug("ObjManager.next(): begin");
	    int i = choice.getSelectedIndex();
	    int k = choice.getItemCount();
	    if (k > 1) { // if there are more choices than just "None"
		int x = i + 1; // go to the next item by default
		if (x >= k) { // if this puts us at the end 
		    x = 1; // then go to the beginning
		}
		select(choice.getItem(x));
	    } 
	    //debug("ObjManager.next(): end");
	}

	/**
	 * Code lifted from ij.plugin.Zoom.
	 */
	protected void center(Obj obj) {
	    //debug("ObjManager.center("+obj+"): begin");
	    canvas.unzoom();
	    //debug("ObjManager.center(): canvas unzoomed"); 
	    Rectangle w = window.getBounds();
	    //debug("ObjManager.center(): window bounds:"+w); 
	    Rectangle r = obj.getBounds();
	    //debug("ObjManager.center(): object bounds:"+r); 
	    //debug("ObjManager.center(): requesting magnification"); 
	    double mag = canvas.getMagnification();
	    //debug("ObjManager.center(): magnification = "+mag); 
	    int marginw = (int)((w.width - mag * image.getWidth()));
	    int marginh = (int)((w.height - mag * image.getHeight()));
	    int x = r.x+r.width/2;
	    int y = r.y+r.height/2;
	    //debug("ObjManager.center(): requesting higher zoom level: "+mag);
	    mag = canvas.getHigherZoomLevel(mag);
	    //debug("ObjManager.center(): got higher zoom level: "+mag);
	    while(r.width*mag<w.width - marginw && r.height*mag<w.height - marginh) {
		canvas.zoomIn(canvas.screenX(x), canvas.screenY(y));
		double cmag = canvas.getMagnification();
		if (cmag==maxZoom) break;
		mag = canvas.getHigherZoomLevel(cmag);
		//debug("ObjManager.center(): zoom level: "+cmag);
		w = image.getWindow().getBounds();
	    }
	}

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
		//debug("next: begin");
		next();
		rois.redraw();
		break;
	    }
	    super.keyTyped(e);
	}

	public void select() {
	}

	public void toSQL() {
	    Iterator i = objects.values().iterator();
	    while (i.hasNext()) {
		Obj o = (Obj)i.next();
		o.toSQL();
	    }
	}

	public void toXML(PrintWriter out) {
	    out.println(" <!-- "+name+" Section -->");
	    Object[] obj = objects.values().toArray();
	    Arrays.sort(obj, new Comparator() {
		    public int compare(Object o1, Object o2) {
			Obj g1 = (Obj)o1; 
			Obj g2 = (Obj)o2;
			int id1 = g1.key(); 
			int id2 = g2.key();
			if (id1 == id2) 
			    return 0;
			return id1 < id2 ? -1 : 1;
		    }
		});
	    for (int i = 0; i < obj.length; i++) {
		Obj o = (Obj)obj[i];
		o.toXML(out);
	    }
	}

	public void fromXML(Element d) {
	    try {
		//Robot robot = new Robot();
		NodeList l = d.getElementsByTagName(getName());
		double step = 1.0/l.getLength();
		double time = 0.0;
		for (int i = 0; i < l.getLength(); i++) {
		    IJ.showProgress(time);
		    Element e = (Element)l.item(i);
		    Obj obj = load(e);
		    debug("Loaded "+obj);
		    put(obj);
		    time += step;
		    //rois.redraw(); // enable for eye candy
		}
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}

	public String getTableName() {
	    return table;
	}

	protected abstract Obj load(Element e);
	protected abstract Obj load(Results r);

	protected abstract class UndoOperation {
	    Obj obj;
	    
	    public UndoOperation(Obj obj) {
		this.obj = obj;
	    }
	    
	    public abstract void undo();
	}
	
	public class UndoAddOperation extends UndoOperation {
	    public UndoAddOperation(Obj obj) {
		super(obj);
	    }
	    
	    public void undo() {
		remove(obj);
	    }
	    
	    public String toString() {
		return "Undo ADD "+obj.getClass()+" "+obj.getKey();
	    }
	}
    }

    /**
     * Manages the set of Bifurcation objects.
     */
    public class BifurcationManager extends ObjManager {
	Bifurcation mark;
	boolean polarity = true;

	public BifurcationManager() {
	    super("Bifurcation", "bif");
	}
	
	public Bifurcation create(double x, double y) {
	    Bifurcation obj = new Bifurcation(this, new Point(x, y));
	    put(obj);
	    return obj;
	}

	protected Obj load(Element e) {
	    return new Bifurcation(this, e);
	}

	protected Obj load(Results r) {
	    return new Bifurcation(this, r);
	}

	public void select() {
	    Toolbar tb = Toolbar.getInstance();
	    if (tb != null) {
		tb.setTool("point");
	    }
	}

	public void mouseClicked(MouseEvent e) {
	    double _x = canvas.offScreenX(e.getX());
	    double _y = canvas.offScreenY(e.getY());
	    if (bifs.current() == null) {
		bifs.create(_x, _y);
		return;
	    } else {
		Bifurcation src = (Bifurcation)bifs.current();
		if (polarity) {
		    if (src.isAvailable()) {
			Bifurcation dst = bifs.create(_x, _y);
			vessels.create(src, dst);
		    } else {
			info("No action taken, this Bifurcation already has two outgoing vessels.");
		    }
		} else {
		    if (src.tributary() == null) {
			Bifurcation dst = bifs.create(_x, _y);
			vessels.create(dst, src);
		    } else {
			info("No action taken, this Bifurcation already has a tributary vessel ("+src.tributary().getKey()+")");
		    }
		}
	    }
	    rois.clearActive();
	    rois.redraw();
	}

	public void keyTyped(KeyEvent e) {
	    //debug("BifurcationManager.keyTyped:"+e);
	    switch (e.getKeyChar()) {
	    case 'm':
		mark = (Bifurcation)current();
		mark.setColor(Color.ORANGE);
		info("Marked bifurcation "+mark.getKey());
		rois.redraw();
		break;
	    case 'y':
		if (polarity == true) {
		    info("Setting vessel creation polarity 'negative'");
		    polarity = false;
		} else {
		    info("Setting vessel creation polarity 'positive'");
		    polarity = true;
		}
		break;
	    case 'k':
		if (mark == null) {
		    warn("Cannot make collateral: no marked bifurcation");
		} else {
		    Bifurcation src = mark;
		    Bifurcation dst = (Bifurcation)current();
		    vessels.create(src, dst);
		    mark.setColor(Color.GRAY);
		    mark = null;
		    rois.redraw();
		}
		break;
	    }
	    super.keyTyped(e);
	}

	public void previous() {
	    super.previous();
	    Bifurcation b = (Bifurcation)current();
	    if (!b.isAvailable()) {
		previous(); // recursion until find an incomplete node
	    }
	}

	public void next() {
	    //debug("Bifurcationmanager.next(): begin");
	    super.next();
	    Bifurcation b = (Bifurcation)current();
	    if (!b.isAvailable()) {
		next(); // recursion until find an incomplete node
	    }
	}
    }

    /**
     * Manages the set of Vessel objects.
     */
    public class VesselManager extends ObjManager {

	public VesselManager() {
	    super("Vessel", "vessel");
	}

	protected VesselManager(String name, String table) {
	    super(name, table);
	}

	public Collateral collateralize(Bifurcation src, Bifurcation dst) {
	    return (Collateral)create(src, dst, true);
	}

	public Set getCollaterals() {
	    Set cols = new HashSet(); 
	    Iterator it = this.values().iterator();
	    while (it.hasNext()) {
		Vessel v = (Vessel)it.next();
		if (v instanceof Collateral) {
		    cols.add(v);
		}
	    }
	    return cols;
	}

	public Vessel create(Bifurcation src, Bifurcation dst) {
	    return create(src, dst, false);
	}

	protected Vessel create(Bifurcation src, Bifurcation dst, boolean collateral) {
	    return check(src, dst) ? insert(src, dst, collateral): null;
	}

	protected Vessel insert(Bifurcation src, Bifurcation dst, boolean collateral) {
	    Vessel obj = collateral ? new Collateral(this, src, dst) : new Vessel(this, src, dst);
	    put(obj);
	    return obj;
	}

	protected boolean check(Bifurcation src, Bifurcation dst) {
	    if (src == null || dst == null) {
		warn("Cannot create vessel: src or dst missing");
		return false;
	    } else if (src == dst) {
		warn("Cannot make collateral: src and dst are identical");
		return false;
	    } else if (!(src.isAvailable() & dst.isAvailable())) {
		warn("Refusing to create vessel between "+src+" and "+dst+": no available terminals");
		return false;
	    } else {
		return true;
	    }
	}

	protected Obj load(Element e) {
	    boolean collateral = Boolean.parseBoolean(e.getAttribute("collateral"));
	    return collateral ? new Collateral(this, e) : new Vessel(this, e);
	}

	protected Obj load(Results r) {
	    boolean collateral = r.getBoolean("collateral");
	    return collateral ? new Collateral(this, r) : new Vessel(this, r);
	}

	public void fromXML(Element d) {
	    try {
		//Robot robot = new Robot();
		NodeList l = d.getElementsByTagName(getName());
		double step = 1.0/l.getLength();
		double time = 0.0;
		for (int i = 0; i < l.getLength(); i++) {
		    IJ.showProgress(time);
		    Element e = (Element)l.item(i);
		    Obj obj = load(e);
		    debug("Loaded "+obj);
		    put(obj);
		    time += step;
		}
		rois.redraw();
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}

    }

    /**
     * Manages the set of Diameter objects.
     */
    public class DiameterManager extends ObjManager {
	Point mark;
	double limit;
	boolean limitEnabled;

	public DiameterManager() {
	    super("Diameter", "diameter");
	    limit = DEFAULT_LIMIT;
	    limitEnabled = true;
	}

	public double getLimit() {
	    return limitEnabled ? limit : 0.0;
	}

	public void setLimit(double limit) {
	    this.limit = limit;
	    limitEnabled = true;
	}

	public Diameter create(Vessel vessel, Point src, Point dst) {
	    if (getLimit() > 0) {
		MaximumSeed line = new MaximumSeed(src, dst, getLimit());
		src = line.src();
		dst = line.dst();	       
	    }

	    Diameter obj = new Diameter(this, vessel, src, dst);
	    put(obj);
	    return obj;
	}

	protected Obj load(Element e) {
	    return new Diameter(this, e);
	}

	protected Obj load(Results r) {
	    return new Diameter(this, r);
	}

	public void select() {
	    Toolbar.getInstance().setTool("line");
	}

	public void mousePressed(MouseEvent e) { 
	    Roi roi = image.getRoi();
	    if (roi == null || roi.getType() != Roi.LINE) {
		return;
	    }
	    if (vessels.current() != null) {
		mark = new Point(canvas.offScreenX(e.getX()), canvas.offScreenY(e.getY()));
	    } else {
		rois.clearActive();
	    }
	}
    
	public void mouseReleased(MouseEvent e) {
	    Roi roi = image.getRoi();
	    if (roi == null || roi.getType() != Roi.LINE || mark == null) {
		return;
	    }
	    try {
		Obj obj = vessels.current(); 
		if (obj == null) {
		    warn("Cannot create diameter here: no current vessel defined");
		} else {
		    Point src = mark;
		    Point dst = new Point(canvas.offScreenX(e.getX()), canvas.offScreenY(e.getY()));
		    diameters.create((Vessel)obj, src, dst); 
		}
	    } catch (ThresholdException ex) {
		info("Threshold failed for diameter centered at "+mark+": "+ex.getMessage());
	    } 

	    mark = null;
	    rois.clearActive();
	    rois.redraw();
	}

	public void previous() {
	    vessels.previous();
	    center(vessels.current());
	}

	public void next() {
	    //debug("DiameterManager.next(): begin");
	    vessels.next();
	    center(vessels.current());
	}

	public void keyTyped(KeyEvent e) {
	    //debug("DiameterManager.keyTyped:"+e);
	    switch (e.getKeyChar()) {
	    case 'l':
		if (limitEnabled) {
		    limitEnabled = false;
		    info("Pixel value limit threshold disabled");
		} else {
		    limitEnabled = true;
		    info("Pixel value limit threshold enabled; current value is '"+limit+"'");
		}
		break;
	    }
	    super.keyTyped(e);
	}
    }

    /**
     * A central resource for loading and execiting commands.
     */
    public class CommandManager extends Manager implements ActionListener {
	TextField field;
	Map commands;
	Pattern names;

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

	public void init() {
	    if (window != null) window.addKeyListener(this);
	    if (canvas != null) canvas.addKeyListener(this);
	    add(new Label("Command:"));
	    field = new TextField("", 20);
	    field.addActionListener(this);
	    add(field);
	}

	public void destroy() {
	    window.removeKeyListener(this);
	    canvas.removeKeyListener(this);
	}

	public void usage() {
	    Iterator i = commands.values().iterator();
	    while (i.hasNext()) {
		info(((Command)i.next()).usage());
	    }
	}

	public Command getCommand(String name) {
	    return (Command)commands.get(name);
	}

	public void put(Command c) {
	    commands.put(c.getName(), c);
	}

	public void actionPerformed(ActionEvent e) { 
	    try {
		execute(e.getActionCommand());
		window.toFront();
	    } catch (Exception ex) {
		ex.printStackTrace();
		warn("Command failed: "+ex.getMessage());
	    }
	}

	public void execute(String command) {
	    int space = command.indexOf(' ');
	    Command cmd = null;
	    String input = null;
	    if (space < 0) {
		cmd = (Command)commands.get(command);
		input = "";
	    } else {
		cmd = (Command)commands.get(command.substring(0, space));
		input = command.substring(space).trim();
	    }

	    if (cmd == null) {
		warn("No command matched '"+command+"'");
		field.setText("");
		return;
	    }

	    String regex = cmd.regexp();
	    if (regex == null || regex.length() == 0) {
		cmd.execute(null);
	    } else {
		Matcher m = Pattern.compile(cmd.regexp(), Pattern.COMMENTS).matcher(input);
		if (m.matches()) {
		    int n = m.groupCount();
		    //debug("Regexp '"+m.pattern()+"' on '"+input+"' matched "+n+" items: complete match = '"+m.group()+"'");
		    String args[] = new String[n];
		    for (int i = 0; i < n; i++) {
			args[i] = m.group(i+1);
			//debug("--> Group "+i+": '"+args[i]+"'");
		    }
		    cmd.execute(args);
		} else {
		    warn(cmd.usage());
		}
	    }
	}

	public void keyTyped(KeyEvent e) {
	    //debug("CommandManager.keyTyped:"+e);
	    switch (e.getKeyChar()) {
	    case 'c':
		field.requestFocus();
		break;
	    }
	}

    }

    // ================================================================================
    // Command Classes
    // ================================================================================

    public interface Command {
	public abstract String getName();
	public abstract String regexp();
	public abstract String usage();
	public abstract void execute(String[] args);
    }

    public class LinkCommand implements Command {
	public String getName() { return "link"; }
	public String regexp() { return "(\\d+).(\\d+)"; }
	public String usage() { return "link <src>.<dst> where src and dst identify the source and destination for the collateral"; }
	public void execute(String[] args) {
	    Bifurcation src = (Bifurcation)bifs.get(args[0]);
	    Bifurcation dst = (Bifurcation)bifs.get(args[1]);
	    if (src == null || dst == null) {
		info("Cannot continue: bifurcation "+args[0]+" or "+args[1]+" not found");
	    } else {
		vessels.collateralize(src, dst);
		rois.redraw();
	    }
	}
    }

    public class HelpCommand implements Command {
	public String getName() { return "help"; }
	public String regexp() { return "(\\d+)?"; }
	public String usage() { return "help (command)?: provides usage information"; }
	public void execute(String[] args) {
	    if (args == null || args.length == 0) {
	    } else {
		Command c = (Command)commands.getCommand(args[0]);
		info(c.usage());
	    }
	}
    }

    protected static class Tile {
	ImageProcessor image;
	int xoff, yoff;

	protected Tile(ImageProcessor image) {
	    this.image = image;
	}

	protected Tile(ImageProcessor image, int xoff, int yoff) {
	    this.image = image;
	    this.xoff = xoff;
	    this.yoff = yoff;
	}
    }

    public class DissectCommand implements Command {
	public String getName() { return "f"; }
	public String regexp() { return ""; }
	public String usage() { return "dissect: creates dissected collateral image files"; }

	public void execute(String[] args) {

	    Set cols = vessels.getCollaterals();
	    if (cols.size() == 0) {
		info("No collaterals to dissect: aborting");
		return;
	    }

	    info("Dissect: number of collaterals:"+cols.size());

	    Iterator it = cols.iterator();
	    while (it.hasNext()) {
		Collateral c = (Collateral)it.next();
		Roi l = c.getLabel();
		if (l != null && "agix".equals(l.toString())) {
		    //if (l != null)
		    dissectCollateral(c);
		}
	    }
	}

	protected void dissectCollateral(Collateral collateral) {
	    info("dissect("+collateral+"): BEGIN");

	    // Generate list of tiles the represent each dissected
	    // segment. There must always be at least one segment for
	    // any collateral
	    List segments = collateral.segment();
	    VesselSegment first = (VesselSegment)segments.get(0);

	    ImagePlus dst = extract(collateral.getLabel().toString(), first);

	    // And finish
	    dst.updateAndDraw();
	    dst.show();
	}
    }

    public class SaveCommand implements Command {
	public String getName() { return "save"; }
	public String regexp() { return "(.+)?"; }
	public String usage() { return "save <filename> where filename is the (optional) name to emit"; }

	public void execute(String[] args) {
	    String filename = null;
	    if (args != null && args.length > 0 && args[0] != null && args[0].length() > 0) {
		filename = args[0];
	    } else {
		filename = getXMLFileName();
	    }
	    save(filename);
	}
    }

    public class SnapshotCommand implements Command {
	public String getName() { return "snap"; }
	public String regexp() { return "(.+)?"; }
	public String usage() { return "snap <name> where name of snapshot title"; }

	public void execute(String[] args) {
	    String filename = null;
	    if (args != null && args.length > 0 && args[0] != null && args[0].length() > 0) {
		filename = args[0];
	    } else {
		filename = getXMLFileName();
	    }
	    try {
		snapshot(filename);
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
    }

    public class CleanDBCommand implements Command {
	public String getName() { return "cleandb"; }
	public String regexp() { return ""; }
	public String usage() { return "cleandb: removes all entries from the database for this image"; }

	public void execute(String[] args) {
	    int id = getImageID();
	    db.execute("DELETE FROM bif WHERE image_id="+id);
	    db.execute("DELETE FROM vessel WHERE image_id="+id);
	    db.execute("DELETE FROM diameter WHERE image_id="+id);
	    db.execute("DELETE pathentry.* FROM pathentry, path, vessel WHERE pathentry.path_id=path.id AND path.vessel_id=vessel.id AND vessel.image_id="+id);
	    db.execute("DELETE path.* FROM path, vessel WHERE path.vessel_id=vessel.id and vessel.image_id="+id);
	    //db.execute("DELETE FROM image WHERE id="+id);
	}
    }

    public class QuitCommand implements Command {
	public String getName() { return "quit"; }
	public String regexp() { return ""; }
	public String usage() { return "quit (no arguments)"; }

	public void execute(String[] args) {
	    close();
	}
    }

    public class LimitCommand implements Command {
	public String getName() { return "limit"; }
	public String regexp() { return "(\\d+(?:.\\d+)?+)"; }
	public String usage() { return "limit <number> where number is the double value of a stop pixel"; }

	public void execute(String[] args) {
	    if (args.length > 0) {
		double limit = Double.parseDouble(args[0]);
		diameters.setLimit(limit);
		info("Limit set to "+limit);
	    } else {
		info("Limit is currently set to "+diameters.getLimit());
	    }
	}
    }


    public class SwapCollateralDirectionCommand implements Command {

	public String getName() { return "swap"; }
	public String regexp() { return "(\\d+)"; }
	public String usage() { return "swap <id> reverse the direction of a vessel"; }

	public void execute(String[] args) {
	    if (args.length > 0) {
		Integer key = Integer.valueOf(args[0]);
		Collateral v = (Collateral)vessels.get(key);
		v.swap();
	    }
	}
    }

    public class SummarizeCommand implements Command {

	public String getName() { return "sum"; }
	public String regexp() { return "(\\d+)?"; }
	public String usage() { return "sum"; }

	public void execute(String[] args) {
	    info("Summarize...");
	    Set cols = vessels.getCollaterals();
	    info("Detected "+cols.size()+" collaterals");

	    Iterator it = cols.iterator();
	    while (it.hasNext()) {

		Collateral collateral = (Collateral)it.next();
		List segments = collateral.segment();

		PathTable path = new PathTable(collateral);
		String pathname = "";

		double sumDistance = 0.0;
		double sumLength = 0.0;
		double sumLDR = 0.0;
		double sumDiameter = 0.0;
		double sumTortuosity = 0.0;
		double sumCurvature = 0.0;

		VesselSegment last = null;
		VesselSegment s = null;

		int n = segments.size();
		for (int i = 0; i < n; i++) {
		    s = (VesselSegment)segments.get(i);

		    if (last != null) {
			Vector _u = new Vector(last.vessel.src().getLocation(), last.vessel.dst().getLocation());
			Vector _v = new Vector(s.vessel.src().getLocation(), s.vessel.dst().getLocation());
			sumCurvature += Vector.degrees(_u, _v);
		    }

		    sumDistance += s.vessel.getDistance();
		    sumLength += s.vessel.getLength();
		    sumLDR += s.vessel.getLDR();
		    sumDiameter += s.vessel.getDiameter();
		    sumTortuosity += s.vessel.getTortuosity();

		    PathTable.Entry e = path.add();
		    e.vessel_id = s.vessel.id();
		    e.x = calibrate(s.x);
		    e.diameter = calibrate(s.vessel.getDiameter());
		    e.length = calibrate(s.vessel.getLength());
		    e.distance = calibrate(s.vessel.getDistance());
		    e.ldr = s.vessel.getLDR();
		    e.tortuosity = s.vessel.getTortuosity();
		    if (s.vessel.src() != null && s.vessel.src().getLabel() != null) {
			if (pathname.length() > 0) 
			    pathname += '-';
			pathname += s.vessel.src().getLabel().toString();
			e.label = s.vessel.src().getLabel().toString();
		    }
		    last = s;
		}

		path.distance = sumDistance / n;
		path.length = sumLength / n;
		path.ldr = path.distance / path.length;
		path.diameter = sumDiameter / n;
		path.tortuosity = (sumTortuosity / n) + (sumCurvature / path.length);
		path.name = collateral.getLabel().toString();

		if (mysqlWrite)
		    path.toSQL();

	    }
	}
    }

    // ================================================================================
    // SQL Classes
    // ================================================================================

    public class PathTable {
	Vessel collateral;
	List list;
	Integer rowid;

	public double distance;
	public double length;
	public double ldr;
	public double diameter;
	public double tortuosity;
	public String name;

	public PathTable(Vessel collateral) {
	    this.collateral = collateral;
	    this.list = new LinkedList();
	}

	public Entry add() {
	    Entry e = new Entry(list.size() + 1);
	    list.add(e);
	    return e;
	}

	public void toSQL() {
	    get_rowid();
	    Iterator i = list.iterator();
	    while (i.hasNext()) {
		Entry e = (Entry)i.next();
		e.toSQL();
	    }
	}

	void format(StringBuffer b, double d) {
	    b.append(',').append(Double.isNaN(d) ? "NULL" : d);
	}
	
	void format(StringBuffer b, int d) {
	    b.append(',').append(d);
	}
	
	void format(StringBuffer b, String s) {
	    b.append(',').append(s == null ? "NULL" : "'" + s + "'");
	}
	
	void get_rowid() {
	    rowid = db.rowid("SELECT id FROM path WHERE vessel_id="+collateral.id());
	    if (rowid != null) {
		db.execute("DELETE FROM pathentry WHERE path_id="+rowid);
		db.execute("DELETE FROM path WHERE id="+rowid);
	    } 
	    StringBuffer b = new StringBuffer("INSERT INTO path (vessel_id, len, distance, ldr, diameter, tortuosity, name) VALUES (");
	    b.append(collateral.id());
	    format(b, length);
	    format(b, distance);
	    format(b, ldr);
	    format(b, diameter);
	    format(b, tortuosity);
	    format(b, name);
	    b.append(')');
	    rowid = db.insert(b.toString());
	}

	class Entry {
	    public int seq;
	    public int vessel_id;
	    public double x;
	    public double distance;
	    public double diameter;
	    public double ldr;
	    public double length;
	    public double tortuosity;
	    public double curvature;
	    public String label;
	    String units;

	    Entry(int seq) {
		this.seq = seq;
		this.units = getUnits();
	    }

	    void toSQL() {
		StringBuffer b = new StringBuffer();
		b.append("INSERT INTO pathentry (path_id, vessel_id, seq, x, distance, diameter, length, ldr, tortuosity, curvature, label, units) VALUES ("+rowid);
		format(b, vessel_id);
		format(b, seq);
		format(b, x);
		format(b, distance);
		format(b, diameter);
		format(b, length);
		format(b, ldr);
		format(b, tortuosity);
		format(b, curvature);
		format(b, label);
		format(b, units);
		b.append(")");
		db.insert(b.toString());
	    }
	}
    }

    public class Row {
	HashMap map;
	String table;
	int pk;

	public Row(String table, int pk) {
	    this.map = new HashMap();
	    this.table = table;
	    this.pk = pk;
	}

	public void put(String key, boolean val) {
	    map.put(key, val ? "TRUE" : "FALSE");
	}

	public void put(String key, double val) {
	    put(key, Double.isNaN(val) ? "NULL" : new Double(val));
	}

	public void put(String key, int val) {
	    put(key, new Integer(val));
	}

	public void put(String key, Object val) {
	    map.put(key, val != null ? val : "NULL");
	}

	public void put(String key, String val) {
	    map.put(key, val != null ? ("'"+val+"'") : "NULL");
	}

	public String toString() {
 	    String q = "UPDATE "+table+" SET ";
	    Iterator i = map.entrySet().iterator();
	    int n = 0;
	    while (i.hasNext()) {
		Map.Entry e = (Map.Entry)i.next();
		if (n++ > 0) 
		    q += ",";
		q+= e.getKey()+"="+e.getValue();
	    }

	    q += " WHERE id="+pk;
	    //info("Row SQL: "+q);
	    return q;
	}

	public boolean update() {
	    return db.execute(toString());
	}
    }

    // ================================================================================
    // Model Classes
    // ================================================================================

    /**
     * Base data model class 
     */
    public abstract class Obj {
	ObjManager manager;
	Integer key;
	Roi roi;
	Point location;
	StringRoi label;
	int id = -1;

	protected Obj(ObjManager manager, Integer key, Point location) {
	    this.manager = manager;
	    this.key = key;
	    this.location = location;
	}

	protected Obj(ObjManager manager, Point location) {
	    this(manager, manager.nextKey(), location);
	}

	protected Obj(ObjManager manager, Element e) {
	    this(manager, 
		 decodeInteger(e.getAttribute("key")), 
		 new Point(Double.parseDouble(e.getAttribute("x")), 
			   Double.parseDouble(e.getAttribute("y"))));
	    String label = e.getAttribute("label");
	    if (label != null && label.length() > 0) {
		setLabel(label);
	    }
	}

	protected Obj(ObjManager manager, Results r) {
	    this(manager, 
		 r.getInteger("item"), // key 
		 new Point(r.getDouble("x"), r.getDouble("y")));
	    this.id = r.getInteger("id");
	    String label = r.getString("label");
	    if (label != null && label.length() > 0) {
		setLabel(label);
	    }
	}

	public ObjManager getManager() {
	    return this.manager;
	}

	public void setLabel(String l) {
	    if (label != null) {
		rois.remove(label);
		label = null;
	    }
	    if (l != null) {
		this.label = new StringRoi((int)this.x(), (int)(this.y() - 30), l);
		//debug("Set label of bifurcation "+key()+" to "+label);
		rois.put(label);
	    }
	}

	public Roi getLabel() {
	    return label;
	}

	public Point getLocation() {
	    return location;
	}

	public Integer getKey() {
	    return key;
	}

	public String getKeyAsString() {
	    return key.toString();
	}

	public int key() {
	    return key.intValue();
	}

	public int id() {
	    return id;
	}

	public Integer getID() {
	    return new Integer(id);
	}

	public String toString() {
	    return "<"+this.getClass()+">-"+key;
	}    

	protected void setColor(Color c) {
	    getRoi().setInstanceColor(c);
	}

	public double x() {
	    return location.x;
	}

	public double y() {
	    return location.y;
	}

	public void remove() {
	    if (label != null) 
		rois.remove(label);
	    rois.remove(getRoi());
	    manager.remove(this);
	    if (mysqlWrite) {
		db.execute("DELETE FROM "+manager.getTableName()+" WHERE id="+id());
	    }
	}

	public void add() {
	    if (label != null) 
		rois.put(label);
	    rois.put(getRoi());

	    if (label != null) {
	    }

	    if (mysqlWrite && id < 0) {
		this.id = db.insert("INSERT INTO "+manager.getTableName()+" (item, x, y, image_id) VALUES ("
				    +key+", "+location.x+","+location.y+", "+getImageID()+")");
	    }
	}

	public void select() {
	    setColor(getSelectedColor());
	    manager.select(this);
	}

	public void deselect() {
	    setColor(getDeselectedColor());
	    manager.deselect(this);
	}

	public Color getSelectedColor() {
	    return Color.YELLOW;
	}

	public Color getDeselectedColor() {
	    return Color.GRAY;
	}

	public void toSQL() {
	    Row row = new Row(manager.getTableName(), id());
	    update(row);
	    row.update();
	}

	public void update(Row u) {
	    if (label != null) 
		u.put("label", label.toString());
	}

	public void toXML(PrintWriter out) {
	}

	public void xmlBegin(PrintWriter out) {
	    out.print(" <" + manager.getName());
	    xmlAttr(out, "key", getKey());
	    xmlAttr(out, "x", x());
	    xmlAttr(out, "y", y());
	}

	public void xmlAttr(PrintWriter out, String key, double value) {
	    xmlAttr(out, key, new Double(value));
	}

	public void xmlAttr(PrintWriter out, String key, Object value) {
	    if (value != null) {
		out.print(" " + key + "=\"" + value.toString() + "\"");
	    }
	}

	public void xmlAttrKey(PrintWriter out, Obj obj) {
	    out.print(" " + obj.getManager().getName() + "=\"" + obj.getKey() + "\"");
	}

	public void xmlEnd(PrintWriter out) {
	    out.println("/>");
	}

	protected String getKey(Obj obj) {
	    return obj != null ? obj.getKeyAsString() : "";
	}

	protected Integer getItem(Obj obj) {
	    return (obj != null) ? obj.getKey() : null;
	}

	public Rectangle getBounds() {
	    //debug("Obj.getBounds: BEGIN");
	    Rectangle result = getRoi().getBounds();
	    //debug("Obj.getBounds: END");
	    return result;
	    //Point p = src.getLocation();
	    //Point q = dst.getLocation();
	    // if (q.x < p.x) {
	    //	q = p;
	    //	p = src.getLocation();
	    //}
	    //return new Rectangle((int)p.x, (int)p.y, (int)(q.x - p.x), (int)(q.y - p.y));
	}

	public abstract Roi getRoi();
    }

    /**
     * Bifurcation Class
     */
    public class Bifurcation extends Obj {
	static final int SIZE = 20;

	Vessel tributary;
	Vessel trunk;
	Vessel branch;

	public Bifurcation(ObjManager manager, Point location) {
	    super(manager, location);
	}

	public Bifurcation(ObjManager manager, Results r) {
	    super(manager, r);
	}

	public Bifurcation(ObjManager manager, Element e) {
	    super(manager, e);
	}

	public void setLabel(String l) {
	}

	public Bifurcation root() {
	    debug("--> root(): checking "+key()+" ("+getLabel()+")");
	    if (tributary == null) {
		debug("---> root is "+key()+" ("+getLabel()+")");
		return this;
	    }
	    return tributary.src().root();
	}

	public void update(Row u) {
	    super.update(u);
	    u.put("trib_item", getItem(tributary));
	    u.put("trunk_item", getItem(trunk));
	    u.put("branch_item", getItem(branch));
	}

	public void toXML(PrintWriter out) {
	    xmlBegin(out);
	    if (label != null) 
		xmlAttr(out, "label", getLabel().toString());
	    xmlAttr(out, "tributary", getKey(tributary));
	    xmlAttr(out, "trunk", getKey(trunk));
	    xmlAttr(out, "branch", getKey(branch));
	    xmlEnd(out);
	}

	public Vessel tributary() {
	    return tributary;
	}

	public Vessel trunk() {
	    return trunk;
	}

	public Vessel branch() {
	    return branch;
	}

	public boolean isAvailable() {
	    return tributary == null || trunk == null || branch == null;
	}

	public void tributary(Vessel tributary) {
	    this.tributary = tributary;
	}

	public void trunk(Vessel trunk) {
	    this.trunk = trunk;
	}

	public void branch(Vessel branch) {
	    this.branch = branch;
	}

	public void addVessel(Vessel v) {
	    if (trunk == null) {
		trunk = v;
	    } else if (branch == null) {
		branch = v;
	    } else {
		throw new IllegalArgumentException
		    ("Cannot add more than two outgoing edges from a binary tree");
	    }
	    
	}

	public void removeVessel(Vessel v) {
	    if (trunk == v) {
		trunk = null;
	    } else if (branch == v) {
		branch = null;
	    } 
	}

	public void flip() {
	    Vessel temp = trunk;
	    trunk = branch;
	    branch = temp;
	}

	public Roi getRoi() {
	    if (roi == null) {
		GeneralPath p = new GeneralPath();
		p.moveTo((float)(x() - SIZE), (float)y()); p.lineTo((float)(x() + SIZE), (float)y());
		p.moveTo((float)x(), (float)(y() - SIZE)); p.lineTo((float)x(), (float)(y() + SIZE));
		roi = new ShapeRoi(p);
	    }
	    return roi;
	}

	public Color getSelectedColor() {
	    return Color.YELLOW;
	}

	public Color getDeselectedColor() {
	    return isAvailable() ? Color.GREEN : Color.CYAN;
	}

	public void remove() {
	    if (trunk != null) trunk.remove();
	    if (branch != null) branch.remove();
	    if (tributary != null) tributary.remove();
	    super.remove();
	}
    }

    /**
     * Vessel Class
     */
    public class Vessel extends Obj implements Dissectable {
	Bifurcation src, dst;
	double length;
	double distance;
	double diameter;
	double curvature;
	Diameter first;
	double boundingWidth, boundingHeight = -1;

	public Vessel(ObjManager manager, Bifurcation src, Bifurcation dst) {
	    super(manager, Point.midpoint(src.getLocation(), dst.getLocation()));
	    this.src = src;
	    this.dst = dst;
	}

	public Vessel(ObjManager manager, Results r) {
	    super(manager, r);
	    int src_item = r.getInteger("src_item");
	    int dst_item = r.getInteger("dst_item");
	    this.src = (Bifurcation)bifs.get(src_item);
	    this.dst = (Bifurcation)bifs.get(dst_item);
	    if (src == null) throw new IllegalStateException("Unable to locate bifurcation "+src_item);
	    if (dst == null) throw new IllegalStateException("Unable to locate bifurcation "+dst_item);
	}

	public Vessel(ObjManager manager, Element e) {
	    super(manager, e);
	    this.src = (Bifurcation)bifs.get(e.getAttribute("src"));
	    this.dst = (Bifurcation)bifs.get(e.getAttribute("dst"));
	    if (src == null) throw new IllegalStateException("Unable to locate bifurcation "+e.getAttribute("src"));
	    if (dst == null) throw new IllegalStateException("Unable to locate bifurcation "+e.getAttribute("dst"));
	}

	public void add() {
	    link();
	    super.add();
	}

	protected void link() {
	    src.addVessel(this);
	    dst.tributary(this);
	}

	public void remove() {
	    unlink();
	    Diameter current = first;
	    while (current != null) {
		current.remove();
		current = current.next;
	    }
	    first = null;
	    super.remove();
	}

	protected void unlink() {
	    src.removeVessel(this);
	    dst.tributary(null);
	}

	public void update(Row u) {
	    super.update(u);
	    u.put("src_item", getItem(src));
	    u.put("dst_item", getItem(dst));
	    u.put("len", getLength());
	    u.put("distance", getDistance());
	    u.put("ldr", getLDR());
	    u.put("diameter", getDiameter());
	    u.put("tortuosity", getTortuosity());
	}

	public void toXML(PrintWriter out) {
	    xmlBegin(out);
	    if (label != null) 
		xmlAttr(out, "label", getLabel().toString());
	    xmlAttr(out, "src", src.getKey());
	    xmlAttr(out, "dst", dst.getKey());
	    xmlEnd(out);
	}

	public Bifurcation src() {
	    return src;
	}

	public Bifurcation dst() {
	    return dst;
	}

	public double getLength() {
	    return length;
	}

	public double getDistance() {
	    if (distance == 0) {
		this.distance = Point.distance(dst.getLocation(), src.getLocation());
	    }
	    return distance;
	}

	public double getLDR() {
	    return length/distance;
	}

	public Color getSelectedColor() {
	    return Color.BLUE;
	}

	public Roi getRoi() {
	    if (roi == null) {
		GeneralPath p = new GeneralPath();
		p.moveTo((float)src.x(), (float)src.y()); 
		p.lineTo((float)dst.x(), (float)dst.y());
		roi = new ShapeRoi(p);
	    }
	    return roi;
	}

	public Rectangle getBounds() {
	    //debug("Vessel.getBounds(): BEGIN");
	    Rectangle box = super.getBounds();
	    Diameter current = first;
	    while (current != null) {
		//debug("INTERSECTION LOOP: box = "+box);
		box = box.intersection(current.getBounds());
		current = current.next;
	    }
	    return box;
	}

	public double getBoundingWidth() {
	    if (boundingWidth < 0)
		throw new NullPointerException("Must call getBoundingBox before this method");
	    return boundingWidth;
	}

	public double getBoundingHeight() {
	    if (boundingHeight < 0)
		throw new NullPointerException("Must call getBoundingBox before this method");
	    return boundingHeight;
	}

	//public Roi getBoundingRoi() {return new ShapeRoi(getBoundingShape());}

	public Shape getBoundingShape() {

	    // Need to define 4 points of the rectangle.  We know the
	    // overall orentation of the box from the vector created
	    // from src->dst.  Then, taking a perpendicular line to
	    // this at both the src and dst origins, we can use this
	    // to project where the bounds will be.
	    Vector centerline = new Vector(src.getLocation(), dst.getLocation()).normalize();
	    Vector perp = centerline.perp();
	    Ray psrc = new Ray(src.getLocation(), perp);
	    Ray pdst = new Ray(dst.getLocation(), perp);

	    double maxdiam = 100.0;

	    Diameter current = first;
	    while(current != null) {
		if (current.getDiameter() > maxdiam)
		    maxdiam = current.getDiameter();
		current = current.next;
	    }

	    Point a = psrc.project(maxdiam);
	    Point b = pdst.project(maxdiam);
	    Point c = pdst.project(-maxdiam);
	    Point d = psrc.project(-maxdiam);

	    boundingWidth = getDistance();
	    boundingHeight = maxdiam;

	    GeneralPath path = new GeneralPath();
	    path.moveTo((float)a.x, (float)a.y);
	    path.lineTo((float)b.x, (float)b.y);
	    path.lineTo((float)c.x, (float)c.y);
	    path.lineTo((float)d.x, (float)d.y);
	    path.lineTo((float)a.x, (float)a.y);

	    return path;
	    //Roi box = new ShapeRoi(path);
	    //rois.put(box);
	    //rois.redraw();
	    //return new ShapeRoi(new Rectangle2D.Double(10, 10, 100, 100));
	    //return new ShapeRoi(path);
	}

	public void compile() {
	    GeneralPath path = new GeneralPath();
	    length = 0; // instance field
	    curvature = 0; // instance field
	    Point u = src.getLocation();
	    Point v = null;
	    Diameter current = first;
	    while (current != null) {
		v = current.getLocation();
		length += line(path, u, v);
		curvature += curvature(u, v, current.next);
		u = v;
		current = current.next;
	    }

	    v = dst.getLocation();
	    length += line(path, u, v);

	    rois.remove(getRoi());
	    roi = new ShapeRoi(path);
	    rois.put(roi);
	}

	protected double curvature(Point p, Point q, Diameter d) {
	    Point r = (d == null) ? dst.getLocation() : d.getLocation();
	    Vector u = new Vector(p, q).normalize();
	    Vector v = new Vector(q, r).normalize();
	    double result = Vector.degrees(u, v);
	    //trace("curvature NaN: u = "+u+", v="+v);
	    return Double.isNaN(result) ? 0 : result;
	}

	protected double line(GeneralPath path, Point u, Point v) {
	    path.moveTo((float)u.x, (float)u.y);
	    path.lineTo((float)v.x, (float)v.y);
	    return Point.distance(u, v);
	}

	public double getTortuosity() {
	    //return (curvature*curvature)/length;
	    //debug("getCurvature("+id()+"): curvature = "+curvature+", length = "+length+", tortuosity = "+(curvature/length));
	    return (curvature)/length;
	}

	public double getDiameter() {
	    if (diameter == 0) {
		int n = 0;
		double total = 0;
		Diameter current = first;
		while (current != null) {
		    n++;
		    total += current.getDiameter();
		    current = current.next;
		}
		diameter = total/n;
	    }
	    return diameter;
	}
	
	public void addDiameter(Diameter diameter) {
	    insert(diameter);
	    compile();
	}

	private void insert(Diameter diameter) {
	    // Special case if this is the first one to be seen by
	    // this vessel.
	    if (first == null) {
		first = diameter;
		//debug("insert(Diameter): first one");
		return;
	    }

	    double eg = Point.distance(this.src().getLocation(), diameter.getLocation());
	    double ef = Point.distance(this.src().getLocation(), first.getLocation());

	    // Another special case is checking whether this diameter is
	    // between the source location of the vessel and first.
	    if (eg < ef) {
		diameter.next = first;
		first = diameter;
		//debug("insert(Diameter): argument appears closest to the source node");
		return;
	    } 

	    // Model the situation such that the diameter object we
	    // are inserting into this linked list (y) is between the
	    // current (x) and current.next (z)
	    Diameter x = first;
	    Diameter y = diameter;
	    Diameter z = first.next;

	    while (true) {

		// If we've reached this point having already tested
		// the first case, we can assume that the absence of a
		// next value of x means that the current one (y)
		// should become so.
		if (z == null) {
		    x.next = y;
		    //debug("insert(Diameter): adding to the end of the list");
		    return;
		}

		double xz = Point.distance(x.getLocation(), z.getLocation());
		double yx = Point.distance(y.getLocation(), x.getLocation());

		// Check if the current Diameter being added (y) is between 
		if (yx < xz) {
		    x.next = y;
		    y.next = z;
		    //debug("insert(Diameter): adding "+y.getLocation()+" between "+x.getLocation()+" and "+z.getLocation());
		    return;
		} else {
		    x = z;
		    z = x.next;
		}
	    }
	}
	
	public void removeDiameter(Diameter diameter) {
	    if (first == diameter) {
		first = first.next;
	    } else {
		Diameter current = first;
		Diameter previous = null;
		while (current != null) {
		    if (current == diameter) {
			previous.next = current.next;
			break;
		    }
		    previous = current;
		    current = current.next;
		}
	    } 

	    compile();
	}

	public List getDiameters() {
	    List l = new LinkedList();
	    Diameter current = first;
	    while (current != null) {
		l.add(current);
		current = current.next;
	    }
	    return l;
	}

	public int getDiameterCount() {
	    int n = 0;
	    Diameter current = first;
	    while (current != null) {
		n++;
		current = current.next;
	    }
	    return n;
	}

	public Point getSrcLocation() {return src().getLocation();}
	public Point getDstLocation() {return dst().getLocation();}

	public ImageProcessor dissect() {
	    if (DoExtendedDissection) {
		if (first == null) {
		    return doDissection(this);
		} else {
		    Segmentable s = new DiameterSegment(this, 
							first, 
							src().getLocation(), 
							first.getLocation()); 
		    ImagePlus p = extract("diameter-" + key(), s);
		    return p.getProcessor();
		}
	    } else {
		return doDissection(this);
	    }
	}
    }

    public class Collateral extends Vessel {

	public Collateral(ObjManager manager, Bifurcation src, Bifurcation dst) {
	    super(manager, src, dst);
	}

	protected Collateral(ObjManager manager, Element e) {
	    super(manager, e);
	}

	protected Collateral(ObjManager manager, Results r) {
	    super(manager, r);
	}

	public void toXML(PrintWriter out) {
	    xmlBegin(out);
	    if (label != null) 
		xmlAttr(out, "label", getLabel().toString());
	    xmlAttr(out, "src", src.getKey());
	    xmlAttr(out, "dst", dst.getKey());
	    xmlAttr(out, "collateral", "true");
	    xmlEnd(out);
	}

	public void update(Row u) {
	    super.update(u);
	    u.put("collateral", true);
	}

	protected void link() {
	    src.addVessel(this);
	    dst.addVessel(this);
	}

	protected void unlink() {
	    src.removeVessel(this);
	    dst.removeVessel(this);
	}

	public void swap() {
	    Bifurcation tmp = dst;
	    dst = src;
	    src = tmp;
	}

	public List segment() {
	    // Setup
	    List segments = new LinkedList();
	    double x = 0.0;
	    Bifurcation b = null;
	    Vessel v = null;
	    
	    if (getLabel() == null) 
		throw new IllegalStateException("Collateral vessels are required to be labeled for statistical analysis.");
	    
	    VesselSegment lcs = new VesselSegment(this, this, x);
	    segments.add(lcs);
	    
	    VesselSegment current = null;
	
	    // Iterate forwards, recording the total distance
	    // traveled .
	    current = lcs;
	    b = dst();
	    while (b.tributary() != null) {
		v = b.tributary();
		x += v.getDistance();
		current.next = new VesselSegment(this, v, x);
		current = current.next;
		segments.add(current);
		b = v.src();
	    }
	    
	    // Iterate backwards, as a negative distance relative
	    // to the collateral being processed. Place each one
	    // in the front of the list in order.
	    current = lcs;
	    x = 0.0;
	    b = src();
	    VesselSegment prev = null;
	    while (b.tributary() != null) {
		v = b.tributary();
		x -= v.getDistance();
		prev = new VesselSegment(this, v, x);
		prev.next = current;
		current = prev;
		segments.add(0, prev);
		b = v.src();
	    }
	    
	    return segments;
	}
    }

    /**
     * Diameter Class
     */
    public class Diameter extends Obj {
	Point src, dst;
	Diameter next;
	Vessel vessel;

	protected Diameter(ObjManager manager, Vessel vessel, Point src, Point dst) {
	    super(manager, Point.midpoint(src, dst));
	    this.vessel = vessel;
	    this.src = src;
	    this.dst = dst;
	}

	public Diameter(ObjManager manager, Results r) {
	    super(manager, r);
	    Integer vessel_item = r.getInteger("vessel_item");
	    this.vessel = (Vessel)vessels.get(vessel_item);
	    if (vessel == null) 
		throw new IllegalStateException("Unable to find vessel "+vessel_item);
	    this.src = new Point(r.getDouble("x1"), r.getDouble("y1"));
	    this.dst = new Point(r.getDouble("x2"), r.getDouble("y2"));
	}

	protected Diameter(ObjManager manager, Element e) {
	    super(manager, e);
	    this.vessel = (Vessel)vessels.get(e.getAttribute(vessels.getName()));
	    this.src = new Point(Double.parseDouble(e.getAttribute("x1")), Double.parseDouble(e.getAttribute("y1")));
	    this.dst = new Point(Double.parseDouble(e.getAttribute("x2")), Double.parseDouble(e.getAttribute("y2")));
	    String l = e.getAttribute("limit"); // backwards compatibility
	    if (l != null && l.length() > 0) {
		try {
		    MaximumSeed line = new MaximumSeed(src, dst, Double.parseDouble(l));
		    this.src = line.src();
		    this.dst = line.dst();
		} catch (ThresholdException ex) {
		    info(ex.getMessage() + " for diameter in vessel "+vessel.getKey());
		}
	    }
	}

	public Vector unit() {
	    return new Vector(dst, src).normalize();
	}

	public Color getSelectedColor() {
	    return Color.GREEN;
	}

	public void add() {
	    vessel.addDiameter(this);
	    super.add();
	}

	public void remove() {
	    vessel.removeDiameter(this);
	    super.remove();
	}

	public void update(Row u) {
	    super.update(u);
	    u.put("x1", x1());
	    u.put("y1", y1());
	    u.put("x2", x2());
	    u.put("y2", y2());
	    u.put("vessel_item", getItem(vessel));
	}

	public void toXML(PrintWriter out) {
	    xmlBegin(out);
	    xmlAttrKey(out, vessel);
	    if (label != null) 
		xmlAttr(out, "label", getLabel().toString());
	    xmlAttr(out, "x1", x1());
	    xmlAttr(out, "y1", y1());
	    xmlAttr(out, "x2", x2());
	    xmlAttr(out, "y2", y2());
	    xmlEnd(out);
	}

	public double x1() { return src.x; }
	public double y1() { return src.y; }
	public double x2() { return dst.x; }
	public double y2() { return dst.y; }

	public Vessel getVessel() {
	    return vessel;
	}

	public Roi getRoi() {
	    if (roi == null) {
		GeneralPath path = new GeneralPath();
		path.moveTo((float)src.x, (float)src.y); 
		path.lineTo((float)dst.x, (float)dst.y);
		roi = new ShapeRoi(path);
	    } 
	    return roi;
	}

	public double getDiameter() {
	    return Point.distance(src.x, src.y, dst.x, dst.y);
	}

    }

    /**
     * Helper class that represents a vessel relative to the modpoint of a collateral.
     */
    public class VesselSegment implements Segmentable {
	public Collateral parent;
	public Vessel vessel;
	public double x;
	public VesselSegment next;

	public VesselSegment(Collateral parent, Vessel vessel, double x) {
	    this.parent = parent;
	    this.vessel = vessel;
	    this.x = x;
	}

	public Segmentable getNextSegment() {
	    return next;
	}

	public ImageProcessor dissect() {
	    return vessel.dissect();
	}

	public Integer getKey() {
	    return vessel.getKey();
	}
    }

    public class DiameterSegment implements Dissectable, Segmentable {

	protected Vessel parent;
	protected Diameter diameter;
	protected Point src, dst;
	protected double boundingWidth, boundingHeight, distance;

	public DiameterSegment(Vessel parent, Diameter diameter, Point src, Point dst) {
	    this.parent = parent;
	    this.diameter = diameter;
	    this.src = src;
	    this.dst = dst;
	}

	public Segmentable getNextSegment() {
	    // If the diameter object is null this object is the last
	    // segment (ie the dst Point object already is the
	    // parent.dst()), just return null.
	    if (diameter == null) 
		return null;

	    // If there are no more remaining diameter objects, make
	    // one more span from our current position to the
	    // parent.dst.
	    if (diameter.next == null) {
		Point nextSrc = dst;
		Point nextDst = parent.dst().getLocation();
		return new DiameterSegment(parent, null, nextSrc, nextDst);
	    } else {
		Point nextSrc = dst;
		Point nextDst = diameter.next.getLocation();
		return new DiameterSegment(parent, diameter.next, nextSrc, nextDst);
	    }
	}

	public ImageProcessor dissect() {
	    return doDissection(this);
	}

	public Point getSrcLocation() {return src;}
	public Point getDstLocation() {return dst;}

	public double getDistance() {
	    if (distance == 0) {
		this.distance = Point.distance(dst, src);
	    }
	    return distance;
	}

	public double getBoundingWidth() {
	    if (boundingWidth < 0)
		throw new NullPointerException("Must call getBoundingBox before this method");
	    return boundingWidth;
	}

	public double getBoundingHeight() {
	    if (boundingHeight < 0)
		throw new NullPointerException("Must call getBoundingBox before this method");
	    return boundingHeight;
	}

	public Shape getBoundingShape() {

	    // Need to define 4 points of the rectangle.  We know the
	    // overall orentation of the box from the vector created
	    // from src->dst.  Then, taking a perpendicular line to
	    // this at both the src and dst origins, we can use this
	    // to project where the bounds will be.
	    Vector centerline = new Vector(src, dst).normalize();
	    Vector perp = centerline.perp();
	    Ray psrc = new Ray(src, perp);
	    Ray pdst = new Ray(dst, perp);

	    double diam = 100.0;
	    //if (diameter != null) diam = diameter.getDiameter(); else diam = 100.0;

	    Point a = psrc.project(diam);
	    Point b = pdst.project(diam);
	    Point c = pdst.project(-diam);
	    Point d = psrc.project(-diam);

	    boundingWidth = getDistance();
	    boundingHeight = diam;

	    GeneralPath path = new GeneralPath();
	    path.moveTo((float)a.x, (float)a.y);
	    path.lineTo((float)b.x, (float)b.y);
	    path.lineTo((float)c.x, (float)c.y);
	    path.lineTo((float)d.x, (float)d.y);
	    path.lineTo((float)a.x, (float)a.y);

	    return path;
	}

	public Integer getKey() {
	    if (diameter != null)
		return diameter.getKey();
	    else 
		return parent.getKey();
	}
    }

    public interface Dissectable {
	Point getSrcLocation();
	Point getDstLocation();
	Shape getBoundingShape();
	double getBoundingHeight();
	double getBoundingWidth();
	Integer getKey();
    }

    public ImageProcessor doDissection(Dissectable d) {

	// Crop the selected roi
	Roi bound = new ShapeRoi(d.getBoundingShape());
	bound.setColor(Color.GREEN);
	image.setRoi(bound);
	rois.put(bound); // show bounding box for each segment
	ImageProcessor crop = image.getProcessor().crop();
	
	// Figure out the required rotation angle in degrees
	// to make the vessel horizintal.
	Point ptsrc = d.getSrcLocation();
	Point ptdst = d.getDstLocation();

	Point pttmp = null;

	boolean showBound = false;

	// If we are heading backwards, swap these
	if (ptsrc.x < ptdst.x) {
	    pttmp = ptsrc;
	    ptsrc = ptdst;
	    ptdst = pttmp;
	}

	double theta = Vector.degrees(new Vector(ptdst, ptsrc), Vector.X_AXIS);
	
	debug("uncorrctd rotation theta for "+d.getKey()+": "+theta);
	
	if (pttmp == null) {
	    // If the destination point x is less than src.x, the line is
	    // sloping down, and we need to correct the rotation angle
	    // accordingly.
	    if (ptdst.y >= ptsrc.y) { // y-slope-down
		if (ptdst.x >= ptsrc.x) { // x-slope-forward
		    theta = 360 - theta; // QUADRANT IV
		    
		} else { // x-slope-back
		    //theta = 180 + theta;  // QUADRANT III
		}
	    } else { 
		if (ptdst.x >= ptsrc.x) { // case 2a
		    // no correction for quadrant I
		} else { // case 2b
		    //theta = 180 - theta; // QUADRANT II (original code)
		    theta = 360 - theta; // QUADRANT II
		    //showBound = true;
		}
	    }
	} else {
	    // we are swapped here

	    // If the destination point x is less than src.x, the line is
	    // sloping down, and we need to correct the rotation angle
	    // accordingly.
	    if (ptdst.y >= ptsrc.y) { // y-slope-down
		if (ptdst.x >= ptsrc.x) { // x-slope-forward
		    //showBound = true;
		    theta = 360 - theta; // QUADRANT IV
		} else { // x-slope-back
		    //showBound = true;
		    //theta = 180 + theta;  // QUADRANT III (original code)
		}
	    } else { 
		if (ptdst.x >= ptsrc.x) { // case 2a
		    // no correction for quadrant I
		} else { // case 2b
		    theta = 360 - theta; // QUADRANT II
		    //showBound = true;
		}
	    }
	}
	
	if (showBound) {
	    debug(this+": ptsrc = '"+ptsrc+"', ptdst = '"+ptdst+"'");
	    debug("corrected rotation theta for "+d.getKey()+": "+theta);
	}
	
	crop.rotate(theta);

	// Rotate the cropped selection, enlargening the pixel array
	ImagePlus tmp = new ImagePlus("vessel-"+d.getKey(), crop);
	
	// Make the new bounding Rectangle centered at the
	// midpoint of (x, y).
	int xlen = (int)d.getBoundingWidth();
	int ylen = (int)d.getBoundingHeight();	    
	int dx = tmp.getWidth();
	int dy = tmp.getHeight();
	int ox = dx/2;
	int oy = dy/2;
	int diffx = dx - xlen;
	int diffy = dy - ylen;
	
	Rectangle r = new Rectangle(diffx/2, diffy/2, xlen, ylen);
	
	// And crop this portion out
	crop.setRoi(r);
	crop = crop.crop(); // what a bunch of crop!
	
	if (pttmp != null) {
	    debug("Flipping horiz");
	    //crop.flipHorizontal();
	}

	// Debug show the roi
	//if (theta == 90.0 || theta == 180.0 || theta == -90.0 || theta == -180.0) {
	if (showBound) {
	    rois.put(bound);
	    tmp.setRoi(new ShapeRoi(r));
	    tmp.show();
	    crop.invert();
	}

	/*
	if (d instanceof DiameterSegment) {
	    DiameterSegment seg = (DiameterSegment)d;
	    if (seg.parent instanceof Collateral) {
		if (d.getKey().equals(new Integer(137))) {
		    rois.put(bound);
		    tmp.setRoi(new ShapeRoi(r));
		    tmp.show();
		    crop.invert();
		}
	    }
	}
	*/

	return crop;
    }

    public interface Segmentable {
	ImageProcessor dissect();
	Segmentable getNextSegment();
    }

    public ImagePlus extract(String label, Segmentable s) {
	List tiles = new LinkedList();
	ImageProcessor tile = null;
	Segmentable current = s;
	while (current != null) {
	    tile = current.dissect();
	    tiles.add(tile);
	    current = current.getNextSegment();
	}

	// Figure out how big the final image needs to be
	int ht = 0;
	int wd = 0;
	
	Iterator i = tiles.iterator();
	int h, w = 0;
	while (i.hasNext()) {
	    tile = (ImageProcessor)i.next();
	    
	    h = tile.getHeight();
	    w = tile.getWidth();
	    
	    ht = h > ht ? h : ht;
	    wd += w;
	}
	
	// Setup the new image that will be created
	ImagePlus dst = NewImage.createRGBImage("Dissection of '"+label+"'", 
						wd, ht, 1, NewImage.FILL_BLACK);
	
	// And a blitter object that transfers pixels
	ColorBlitter blit = new ColorBlitter((ColorProcessor)dst.getProcessor());
	blit.setTransparentColor(Color.BLACK);
	
	// Keep track of the x,y coordinates where the copied
	// selection will be originated at.
	int xoff = 0, yoff = 0;
	
	// Finally copy tiles to the new image
	i = tiles.iterator();
	while (i.hasNext()) {
	    tile = (ImageProcessor)i.next();
	    blit.copyBits(tile, xoff, yoff, Blitter.COPY);
	    xoff = xoff + tile.getWidth();
	}
	
	return dst;
    }

    // ================================================================================
    // Geometry Classes
    // ================================================================================

    public static class Point extends Point2D.Double {

	public Point(double x, double y) {
	    super(x,y);
	}

	public String toString() {
	    return "("+df3.format(x)+","+df3.format(y)+")";
	}

	public boolean equals(Object other) {
	    if (this == other)
		return true;
	    if (!(this instanceof Point)) 
		return false;
	    Point that = (Point)other;
	    if (this.x - that.x > SMALL_ERROR)
		return false;
	    if (this.y - that.y > SMALL_ERROR)
		return false;
	    return true;
	}

	public static double distance(Point p, Point q) {
	    double dx = q.x - p.x, dy = q.y - p.y;
	    return Math.sqrt(dx*dx + dy*dy);
	}

	public static double distance(double x1, double y1, double x2, double y2) {
	    double dx = x2 - x1; 
	    double dy = y2 - y1;
	    return Math.sqrt(dx*dx + dy*dy);
	}

	public static Point midpoint(Point p, Point q) {
	    double dx = q.x - p.x;
	    double dy = q.y - p.y;
	    return new Point(p.x + dx/2, p.y + dy/2);
	}
    }

    public static class Pixel extends Point {
	public double value;

	public Pixel(double x, double y) {
	    this(x, y, 0.0);
	}

	public Pixel(double x, double y, double value) {
	    super(x, y);
	    this.value = value;
	}

	public String toString() {
	    return "("+df3.format(x)+","+df3.format(y)+",="+value+")";
	}
    }

    public class MaximumSeed {
	Point src, dst;
	double limit;

	public MaximumSeed(Point src, Point dst, double limit) {
	    this.src = src;
	    this.dst = dst;
	    this.limit = limit;
	    init();
	}

	private void init() throws ThresholdException {
	    ImageProcessor ip = image.getProcessor();

	    double dx = dst.x - src.x; 
	    double dy = dst.y - src.y;
	    int n = (int)Math.round(Math.sqrt(dx*dx + dy*dy));
	    double xinc = (double)dx/n;
	    double yinc = (double)dy/n;

	    n++;

	    Pixel[] pixels = new Pixel[n];
	    double rx = src.x;
	    double ry = src.y;
	    double max_v = -1;
	    int max_n = -1;
            for (int i = 0; i < n; i++) {
		pixels[i] = new Pixel(rx + 0.5, ry + 0.5);
                pixels[i].value = ip.getPixelValue((int)pixels[i].x, (int)pixels[i].y);
		if (pixels[i].value > max_v) {
		    max_v = pixels[i].value;
		    max_n = i;
		}
                rx += xinc;
                ry += yinc;
            }

	    if (max_v < limit) 
		throw new ThresholdException("No pixel above "+limit+" detected");

	    int n_lo = -1, n_hi = -1;

	    n_hi = seekForwardBelowLimit(pixels, max_n);
	    n_lo = seekReverseBelowLimit(pixels, max_n);
		
	    if (n_lo < 0)
		throw new ThresholdException("Boundary above "+limit+" not reached");
	    if (n_hi < 0) 
		throw new ThresholdException("Boundary above "+limit+" not reached");

	    dst = pixels[n_hi];
	    src = pixels[n_lo];
	}

	public Point src() {
	    return src;
	}

	public Point dst() {
	    return dst;
	}

	protected int seekForwardBelowLimit(Pixel[] pixels, int start) {
	    int n = pixels.length;
	    for (int i = start; i < n; i++) {
		if (pixels[i].value < limit) {
		    return i;
		}
	    }
	    return -1; // not found
	}

	protected int seekReverseBelowLimit(Pixel[] pixels, int start) {
	    int n = pixels.length;
	    for (int i = start; i >= 0; i--) {
		if (pixels[i].value < limit) {
		    return i;
		}
	    }
	    return -1; // not found
	}

    }

    public static class Ray {
	public Point p;
	public Vector v;

	public Ray(Point p, Point q) {
	    this(p, new Vector(p, q));
	}

	public Ray(Point p, Vector v) {
	    this.p = p;
	    this.v = v;
	}

	public Point project(double scale) {
	    double x = v.dx * scale + p.x;
	    double y = v.dy * scale + p.y;
	    return new Point(x, y);
	}
	
	public String toString() {
	    return "["+p+", "+v+"]";
	}

	/**
	 * From http://softsurfer.com/Archive/algorithm_0104/algorithm_0104B.htm.
	 */
	public static Point intersection(Ray r, Ray s) {
	    // Obtain the vectors for these rays
	    Vector u = r.v; 
	    Vector v = s.v; 

	    // First check if they are poining in an identical
	    // direction. If so, these rays are parallel.
	    if (u.normalize().equals(v.normalize())) {
		return null;
	    }

	    // Obtain perpendicular vectors and their negated counterparts
	    Vector up = u.perp();
	    Vector vp = v.perp();
	    //Vector un = up.negate(); // this one is never used in this algo 
	    Vector vn = vp.negate();

	    // Make a new vector from the base of s to base of r
	    Vector w = new Vector(s.p, r.p); 

	    // Calculate the scalars along r and s where they meet
	    double ri = Vector.dot(vn, w) / Vector.dot(vp, u);
	    double si = Vector.dot(up, w) / Vector.dot(up, v);

	    //debug("To intersect, one must travel "+ri+" units along "+r+" and "+si+" units along "+s);

	    // obtain the point along r at ri
	    Point rp = r.project(ri);
	    Point sp = s.project(si);

	    // Check our work
	    if (!rp.equals(sp)) {
		debug("Intersection point along r = "+rp);
		debug("Intersection point along s = "+sp);
		throw new IllegalStateException("Unexpected failure of intersection algorithm");
	    }

	    return rp;
	}
    }

    public static class Vector {
	public static Vector X_AXIS = new Vector(1, 0);
	public static Vector Y_AXIS = new Vector(0, 1);

	public double dx, dy;

	public Vector(Vector that) {
	    this(that.dx, that.dy);
	}

	public Vector(Point p, Point q) {
	    this(q.x - p.x, q.y - p.y);
	}

	public Vector(double dx, double dy) {
	    this.dx = dx;
	    this.dy = dy;
	}

	public Vector normalize() {
	    double len = length();
	    if (len == 1.0) 
		return this;
	    double ux = dx/len;
	    double uy = dy/len;
	    return new Vector(ux, uy);
	}

	public void scale(double scale) {
	    this.dx *= scale;
	    this.dy *= scale;
	}
	
	public Vector perp() {
	    Vector v = new Vector(this);
	    perp(v);
	    return v;
	}

	public Vector negate() {
	    return new Vector(-dx, -dy);
	}

	public double length() {
	    return Math.sqrt(dx*dx + dy*dy);
	}

	public String toString() {
	    return "("+df3.format(dx)+","+df3.format(dy)+")T";
	}

	public boolean equals(Object other) {
	    if (this == other) 
		return true;
	    if (!(other instanceof Vector))
		return false;
	    Vector that = (Vector)other;
	    if (this.dx - that.dx > SMALL_ERROR) 
		return false;
	    if (this.dy - that.dy > SMALL_ERROR) 
		return false;
	    return true;
	}

	public int hashCode() {
	    return (int)(this.dx * this.dy * 17);
	}

	public static double dot(Vector r, Vector s) {
	    return r.dx*s.dx + r.dy*s.dy;
	}

	public static Vector cross(Vector r, Vector s) {
	    // I left this in to remind myself of this idea.
	    throw new IllegalStateException("Cross product implies that the result is perpendicular "
					    +"to both input vector arguments. In order to do that, you need "
					    +"a 3rd dimension and therefore the cross product in 2D does not exist."); 
	}

	/**
	 * Modifies the vector argument to be perpendicular to itself.
	 * There is symmetry here to the notion of 'cross-product' in
	 * 3D.
	 */
	public static void perp(Vector v) {
	    double v_dx = v.dx; // temp
	    v.dx = -v.dy;
	    v.dy =  v_dx;
	}

	/**
	 * Reports the angle between two vectors in radians.
	 */
	public static double radians(Vector u, Vector v) {
	    return Math.acos(dot(u.normalize(), v.normalize()));
	}

	/**
	 * Reports the angle between two vectors in degrees.
	 */
	public static double degrees(Vector u, Vector v) {
	    return Math.toDegrees(radians(u, v));
	}

	/**
	 * Returns a constant denoting which of 4 Quadrants this
	 * vector is within.
	 */
	public static int coordinate(Vector v) {
	    double xdeg = degrees(v, X_AXIS);
	    double ydeg = degrees(v, Y_AXIS);
	    
	    if (xdeg < 90) {
		if (ydeg < 90) {
		    return Quadrant.I;
		} else {
		    return Quadrant.IV;
		}		
	    } else {
		if (ydeg < 90) {
		    return Quadrant.II;
		} else {
		    return Quadrant.III;
		}		
	    }
	}	    
	
    }

    public static final class Quadrant {
	public static final int I = 1, II = 2, III = 3, IV = 4;

	public static String toString(int quadrant) {
	    switch (quadrant) {
	    case I:   return "I";
	    case II:  return "II";
	    case III: return "III";
	    case IV:  return "IV";
	    }
	    throw new IllegalArgumentException("Undefined quadrant: "+quadrant);
	}
    }

    // --------------------------------------------------------------------------------
    // Support ImageJ classes
    // --------------------------------------------------------------------------------

    /**
     * Stripped down from http://www.astro.physik.uni-goettingen.de/~hessman/ImageJ/Astronomy/index.html#StringRoi.
     */ 
    public class StringRoi extends Roi {
	protected int px;
	protected int py;
	protected String text;
	
	public StringRoi(int px, int py, String text) {
	    super(px,py,image);
	    this.px = px;
	    this.py = py;
	    this.text = text;
	    this.type = Roi.COMPOSITE; // not sure why this seems necessary
	}
	
	public void draw(Graphics g) {
	    g.setColor(Color.WHITE);
	    g.drawString(text, ic.screenX(px), ic.screenY(py));
	}

	public String toString() {
	    return text;
	}
    }

    // --------------------------------------------------------------------------------
    // End of ArterioJ
    // --------------------------------------------------------------------------------
}

/*
    public class Collateral extends Vessel {
	public Collateral(Bifurcation src, Bifurcation dst) {
	    super(src, dst);
	}

	public Color getSelectedColor() {
	    return Color.ORANGE;
	}
    }

    public class CollateralManager extends VesselManager {

	public CollateralManager() {
	    super("Collateral");
	}

	protected Vessel _create(Bifurcation src, Bifurcation dst) {
	    Collateral obj = new Collateral(src, dst);
	    Integer key = nextKey();
	    put(obj, key, src.getLocation());
	    return obj;
	}

	protected Obj create(Element e) {
	    Bifurcation src = (Bifurcation)bifs.get(e.getAttribute("src"));
	    Bifurcation dst = (Bifurcation)bifs.get(e.getAttribute("dst"));
	    return new Collateral(src, dst);
	}

	public void previous() {
	    bifs.previous();
	}

	public void next() {
	    bifs.next();
	}
   }


	    if (tributary == null)
		return this;

	    Bifurcation current = tributary.src();
	    while (current.tributary != null) {
		info("Searching for root: "+current.id());
		if (current == this) {
		    // Cycle detected
		    return null;
		}
		current = tributary.src();
	    }
	    return current;

*/