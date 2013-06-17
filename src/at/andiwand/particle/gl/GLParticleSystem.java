package at.andiwand.particle.gl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.security.CodeSource;
import java.text.DecimalFormat;

import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL20;

import at.andiwand.particle.ParticleSystem;

public abstract class GLParticleSystem extends ParticleSystem {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
	    "#.##");

    static {
	String platformName = LWJGLUtil.getPlatformName();
	String librarySubPath = File.separator + "lwjgl" + File.separator
		+ "native" + File.separator + platformName;

	CodeSource classCodeSource = GLParticleSystem.class
		.getProtectionDomain().getCodeSource();
	URL classLocation = classCodeSource.getLocation();
	File classLocationFile = new File(classLocation.getPath());
	String libraryPath;

	if (classLocationFile.isDirectory()) {
	    libraryPath = classLocationFile + librarySubPath;
	} else {
	    libraryPath = classLocationFile.getParent() + librarySubPath;
	}

	System.setProperty("org.lwjgl.librarypath", libraryPath);
    }

    public static ByteBuffer readShaderSource(InputStream inputStream)
	    throws IOException {
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	while (true) {
	    int b = inputStream.read();

	    if (b < 0)
		break;

	    outputStream.write(b);
	}

	byte[] array = outputStream.toByteArray();
	ByteBuffer result = ByteBuffer.allocateDirect(array.length);
	result.put(array);
	result.rewind();
	return result;
    }

    public static void validateShader(int shader) {
	int statusInt = GL20.glGetShader(shader, GL20.GL_COMPILE_STATUS);
	boolean status = statusInt != 0;

	if (!status) {
	    int length = GL20.glGetShader(shader, GL20.GL_INFO_LOG_LENGTH);

	    String infoLog = GL20.glGetShaderInfoLog(shader, length);

	    System.out.println(infoLog.trim());
	    System.exit(1);
	}
    }

    public static void validateProgram(int program) {
	int statusInt = GL20.glGetProgram(program, GL20.GL_LINK_STATUS);
	boolean status = statusInt != 0;

	if (!status) {
	    int length = GL20.glGetProgram(program, GL20.GL_INFO_LOG_LENGTH);

	    String infoLog = GL20.glGetProgramInfoLog(program, length);

	    System.out.println(infoLog.trim());
	    System.exit(1);
	}
    }

    public static void programUniform1i(int program, String name, int value) {
	int location = GL20.glGetUniformLocation(program, name);
	GL20.glUniform1i(location, value);
    }

    public static void programUniform1f(int program, String name, float value) {
	int location = GL20.glGetUniformLocation(program, name);
	GL20.glUniform1f(location, value);
    }

    public static void programUniform4(int program, String name,
	    FloatBuffer values) {
	int location = GL20.glGetUniformLocation(program, name);
	GL20.glUniform4(location, values);
    }

    private String title;
    private String titleSuffix;
    private String fullTitle;

    protected DisplayMode displayMode;
    protected boolean fullscreen;
    protected boolean vsync;
    protected boolean titleChanged;

    public GLParticleSystem(String title, DisplayMode displayMode,
	    boolean fullscreen, boolean vsync) throws LWJGLException {
	this.title = title;
	titleSuffix = "";
	setTitle(title);

	this.displayMode = displayMode;
	this.fullscreen = fullscreen;
	this.vsync = vsync;
    }

    public void setTitle(String title) {
	setFullTitle(title + titleSuffix);
    }

    public void setTitleSuffix(String titleSuffix) {
	setFullTitle(title + titleSuffix);
    }

    public void setFullTitle(String fullTitle) {
	this.fullTitle = fullTitle;
	titleChanged = true;
    }

    public abstract int getActiveParticle();

    @Override
    public void init() {
	try {
	    Display.setDisplayMode(displayMode);
	    Display.setFullscreen(fullscreen);
	    Display.setIcon(null);
	    Display.setVSyncEnabled(vsync);
	    Display.setTitle(title);
	    Display.create();

	    Mouse.create();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	super.init();
    }

    @Override
    public void draw() {
	Display.update();
	setRunning(!Display.isCloseRequested());
	if (titleChanged)
	    Display.setTitle(fullTitle);

	super.draw();
    }

    @Override
    public void destroy() {
	Display.destroy();

	Mouse.destroy();

	super.destroy();
    }

    protected void fpsCallback(double fps) {
	String suffix = " with " + getActiveParticle() + " particles @"
		+ DECIMAL_FORMAT.format(fps) + "fps";
	setTitleSuffix(suffix);
    }

}