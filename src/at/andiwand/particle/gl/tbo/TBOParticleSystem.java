package at.andiwand.particle.gl.tbo;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.ARBTextureBufferObject;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.NVTransformFeedback;

import at.andiwand.particle.Particle;
import at.andiwand.particle.ParticleSystemOptionDialog;
import at.andiwand.particle.gl.GLParticleSystem;

public class TBOParticleSystem extends GLParticleSystem {

    private static final String TITLE = "TBO Particle Test";

    private static final int PARTICLE_BUFFER_SIZE_FACTOR = 16;

    private float ratio;

    private int[] positionVbo;
    private int[] velocityVbo;
    private int[] velocityTbo;

    private int trigger;

    private int query;

    private int maxParticles;
    private int activeParticles;
    private ArrayList<Particle> queuedParticles;

    private int updateProgram;
    private int updateVertexShader;
    private int updateGeometryShader;

    private int renderProgram;
    private int renderFragmentShader;
    private int renderVertexShader;
    private int renderGeometryShader;

    private long lastTime;

    private int eventId;

    public TBOParticleSystem(int maxParticles, boolean calcRatio,
	    DisplayMode displayMode, boolean fullscreen, boolean vsync)
	    throws LWJGLException {
	super(TITLE, displayMode, fullscreen, vsync);

	if (calcRatio)
	    ratio = (float) displayMode.getWidth() / displayMode.getHeight();
	else
	    ratio = 1;

	this.maxParticles = maxParticles;
	queuedParticles = new ArrayList<Particle>();

	start();
    }

    @Override
    public int getActiveParticle() {
	return activeParticles;
    }

    private void initVBO() {
	IntBuffer dummy = BufferUtils.createIntBuffer(4);
	GL15.glGenBuffers(dummy);

	positionVbo = new int[] { dummy.get(0), dummy.get(1) };
	velocityVbo = new int[] { dummy.get(2), dummy.get(3) };

	int bufferSize = maxParticles * PARTICLE_BUFFER_SIZE_FACTOR;

	for (int i = 0; i < dummy.limit(); i++) {
	    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, dummy.get(i));
	    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferSize,
		    GL15.GL_DYNAMIC_DRAW);
	}

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void initTBO() {
	IntBuffer dummy = BufferUtils.createIntBuffer(2);
	GL11.glGenTextures(dummy);

	velocityTbo = new int[] { dummy.get(0), dummy.get(1) };

	for (int i = 0; i < velocityTbo.length; i++) {
	    GL11.glBindTexture(ARBTextureBufferObject.GL_TEXTURE_BUFFER_ARB,
		    velocityTbo[i]);
	    ARBTextureBufferObject.glTexBufferARB(
		    ARBTextureBufferObject.GL_TEXTURE_BUFFER_ARB,
		    ARBTextureFloat.GL_RGBA32F_ARB, velocityVbo[i]);
	}

	GL11.glBindTexture(ARBTextureBufferObject.GL_TEXTURE_BUFFER_ARB, 0);
    }

    private void initQuery() {
	query = GL15.glGenQueries();
    }

    private void initUpdateProgram() throws IOException {
	updateProgram = GL20.glCreateProgram();

	updateVertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
	updateGeometryShader = GL20
		.glCreateShader(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);

	GL20.glShaderSource(updateVertexShader,
		readShaderSource(TBOParticleSystem.class
			.getResourceAsStream("update_vertexshader.txt")));
	GL20.glShaderSource(updateGeometryShader,
		readShaderSource(TBOParticleSystem.class
			.getResourceAsStream("update_geometryshader.txt")));

	ARBGeometryShader4.glProgramParameteriARB(updateProgram,
		ARBGeometryShader4.GL_GEOMETRY_VERTICES_OUT_ARB, 1);
	ARBGeometryShader4.glProgramParameteriARB(updateProgram,
		ARBGeometryShader4.GL_GEOMETRY_INPUT_TYPE_ARB, GL11.GL_POINTS);
	ARBGeometryShader4.glProgramParameteriARB(updateProgram,
		ARBGeometryShader4.GL_GEOMETRY_OUTPUT_TYPE_ARB, GL11.GL_POINTS);

	GL20.glCompileShader(updateVertexShader);
	GL20.glCompileShader(updateGeometryShader);

	validateShader(updateVertexShader);
	validateShader(updateGeometryShader);

	GL20.glAttachShader(updateProgram, updateVertexShader);
	GL20.glAttachShader(updateProgram, updateGeometryShader);

	GL20.glLinkProgram(updateProgram);

	validateProgram(updateProgram);
    }

    private void initRenderProgram() throws IOException {
	renderProgram = GL20.glCreateProgram();

	renderFragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
	renderVertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
	renderGeometryShader = GL20
		.glCreateShader(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);

	GL20.glShaderSource(renderFragmentShader,
		readShaderSource(TBOParticleSystem.class
			.getResourceAsStream("render_fragmentshader.txt")));
	GL20.glShaderSource(renderVertexShader,
		readShaderSource(TBOParticleSystem.class
			.getResourceAsStream("render_vertexshader.txt")));
	GL20.glShaderSource(renderGeometryShader,
		readShaderSource(TBOParticleSystem.class
			.getResourceAsStream("render_geometryshader.txt")));

	ARBGeometryShader4.glProgramParameteriARB(renderProgram,
		ARBGeometryShader4.GL_GEOMETRY_VERTICES_OUT_ARB, 1);
	ARBGeometryShader4.glProgramParameteriARB(renderProgram,
		ARBGeometryShader4.GL_GEOMETRY_INPUT_TYPE_ARB, GL11.GL_POINTS);
	ARBGeometryShader4.glProgramParameteriARB(renderProgram,
		ARBGeometryShader4.GL_GEOMETRY_OUTPUT_TYPE_ARB, GL11.GL_POINTS);

	GL20.glCompileShader(renderFragmentShader);
	GL20.glCompileShader(renderVertexShader);
	GL20.glCompileShader(renderGeometryShader);

	validateShader(renderFragmentShader);
	validateShader(renderVertexShader);
	validateShader(renderGeometryShader);

	GL20.glAttachShader(renderProgram, renderFragmentShader);
	GL20.glAttachShader(renderProgram, renderVertexShader);
	GL20.glAttachShader(renderProgram, renderGeometryShader);

	GL20.glLinkProgram(renderProgram);

	validateProgram(renderProgram);
    }

    protected void initImpl() {
	try {
	    GL11.glDisable(GL11.GL_DEPTH_TEST);

	    GL11.glClearColor(1, 1, 1, 1);

	    GL11.glViewport(0, 0, displayMode.getWidth(),
		    displayMode.getHeight());

	    GL11.glMatrixMode(GL11.GL_PROJECTION);
	    GL11.glLoadIdentity();
	    GL11.glOrtho(-ratio, ratio, -1, 1, 0.1, 10);

	    initVBO();
	    initTBO();
	    initQuery();
	    initUpdateProgram();
	    initRenderProgram();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	lastTime = System.nanoTime();
    }

    private void update() {
	int input = trigger;
	int output = trigger ^= 1;

	long yetTime = System.nanoTime();
	float deltaTime = (yetTime - lastTime) / 1000000000f;
	lastTime = yetTime;

	if (!queuedParticles.isEmpty()) {
	    synchronized (queuedParticles) {
		int particles = Math.min(queuedParticles.size(), maxParticles
			- activeParticles);

		int bufferOffset = activeParticles
			* PARTICLE_BUFFER_SIZE_FACTOR;
		FloatBuffer positionData = BufferUtils
			.createFloatBuffer(particles * 4);
		FloatBuffer velocityData = BufferUtils
			.createFloatBuffer(particles * 4);

		for (int i = 0; i < particles; i++) {
		    Particle particle = queuedParticles.get(i);

		    positionData.put(particle.positionX);
		    positionData.put(particle.positionY);
		    positionData.put(particle.positionZ);
		    positionData.put(particle.size);

		    velocityData.put(particle.velocityX);
		    velocityData.put(particle.velocityY);
		    velocityData.put(particle.velocityZ);
		    velocityData.put(particle.lifeTime);
		}
		positionData.rewind();
		velocityData.rewind();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo[input]);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, bufferOffset,
			positionData);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, velocityVbo[input]);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, bufferOffset,
			velocityData);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		queuedParticles.clear();
		activeParticles += particles;
	    }
	}

	GL20.glUseProgram(updateProgram);

	programUniform1i(updateProgram, "velocityIn", 0);
	programUniform1f(updateProgram, "ratio", ratio);
	programUniform1f(updateProgram, "deltaTime", deltaTime);

	while (Mouse.next()) {
	    boolean down = Mouse.getEventButtonState();
	    int mask;

	    switch (Mouse.getEventButton()) {
	    case 0:
		mask = 1;
		break;
	    case 1:
		mask = 2;
		break;
	    default:
		continue;
	    }

	    if (down)
		eventId |= mask;
	    else
		eventId &= ~mask;
	}

	float mouseX = (float) Mouse.getX()
		/ Display.getDisplayMode().getWidth() - 0.5f;
	float mouseY = (float) Mouse.getY()
		/ Display.getDisplayMode().getHeight() - 0.5f;
	mouseX *= 2 * ratio;
	mouseY *= 2;

	FloatBuffer eventPoints = BufferUtils.createFloatBuffer(40);
	eventPoints.put(new float[] { mouseX, mouseY, -1, 10000000 });
	// eventId = 1;
	// eventPoints.put(new float[] {1, 0, -1, 10000000});
	// eventPoints.put(new float[] {-1, 0, -1, 10000000});
	int eventPointNumber = eventPoints.position() / 4;
	eventPoints.rewind();

	programUniform1i(updateProgram, "eventId", eventId);
	programUniform4(updateProgram, "eventPoints", eventPoints);
	programUniform1i(updateProgram, "eventPointNumber", eventPointNumber);

	NVTransformFeedback.glBindBufferBaseNV(
		NVTransformFeedback.GL_TRANSFORM_FEEDBACK_BUFFER_NV, 0,
		positionVbo[output]);
	NVTransformFeedback.glBindBufferBaseNV(
		NVTransformFeedback.GL_TRANSFORM_FEEDBACK_BUFFER_NV, 1,
		velocityVbo[output]);

	IntBuffer locations = BufferUtils.createIntBuffer(2);
	locations.put(NVTransformFeedback.glGetVaryingLocationNV(updateProgram,
		"gl_Position"));
	locations.put(NVTransformFeedback.glGetVaryingLocationNV(updateProgram,
		"velocityOut"));
	locations.rewind();
	NVTransformFeedback.glTransformFeedbackVaryingsNV(updateProgram,
		locations, NVTransformFeedback.GL_SEPARATE_ATTRIBS_NV);

	GL11.glEnable(NVTransformFeedback.GL_RASTERIZER_DISCARD_NV);

	GL15.glBeginQuery(
		NVTransformFeedback.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN_NV,
		query);

	NVTransformFeedback.glBeginTransformFeedbackNV(GL11.GL_POINTS);

	GL13.glActiveTexture(GL13.GL_TEXTURE0);
	GL11.glBindTexture(ARBTextureBufferObject.GL_TEXTURE_BUFFER_ARB,
		velocityTbo[input]);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo[input]);

	GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

	GL11.glVertexPointer(4, GL11.GL_FLOAT, 0, 0);
	GL11.glDrawArrays(GL11.GL_POINTS, 0, activeParticles);

	GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

	NVTransformFeedback.glEndTransformFeedbackNV();

	GL15.glEndQuery(NVTransformFeedback.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN_NV);
	activeParticles = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);

	GL11.glDisable(NVTransformFeedback.GL_RASTERIZER_DISCARD_NV);

	GL20.glUseProgram(0);
    }

    private void render() {
	int input = trigger;

	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

	GL20.glUseProgram(renderProgram);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo[input]);

	GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

	GL11.glVertexPointer(4, GL11.GL_FLOAT, 0, 0);
	GL11.glDrawArrays(GL11.GL_POINTS, 0, activeParticles);

	GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

	GL20.glUseProgram(0);
    }

    protected void drawImpl() {
	update();
	render();
    }

    protected void destroyImpl() {

    }

    public void addParticle(Particle particle) {
	synchronized (queuedParticles) {
	    queuedParticles.add(particle);
	}
    }

    public static void main(String[] args) throws LWJGLException {
	ParticleSystemOptionDialog optionDialog = new ParticleSystemOptionDialog(
		"particle count");
	optionDialog.waitUntilCommit();
	optionDialog.dispose();

	int particleCount = optionDialog.getParticleCount();
	DisplayMode displayMode = optionDialog.getDisplayMode();
	boolean fullscreen = optionDialog.isFullscreen();
	boolean vSync = optionDialog.isVSync();
	boolean calcRatio = optionDialog.isCalcRatio();

	TBOParticleSystem particleSystem = new TBOParticleSystem(particleCount,
		calcRatio, displayMode, fullscreen, vSync);

	for (int i = 0; i < particleCount; i++) {
	    Particle particle = new Particle((float) (Math.random() - 0.5),
		    (float) (Math.random() - 0.5), -1, 1, 0, 0, 0,
		    Float.POSITIVE_INFINITY);

	    particleSystem.addParticle(particle);
	}
    }

}