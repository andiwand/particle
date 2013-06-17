package at.andiwand.particle.gl.cpu;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import at.andiwand.particle.Particle;
import at.andiwand.particle.ParticleSystemOptionDialog;
import at.andiwand.particle.gl.GLParticleSystem;

public class CPUParticleSystem extends GLParticleSystem {

    private static final String TITLE = "CPU Particle Test";

    private static final int PARTICLE_BUFFER_SIZE_FACTOR = 16;

    private static final float G = (float) (6.67428 * Math.pow(10.0, -11.0));

    private float ratio;

    private int positionVbo;
    private int velocityVbo;

    private int maxParticles;
    private int activeParticles;
    private ArrayList<Particle> queuedParticles;

    private long lastTime = System.nanoTime();

    private int eventId = 0;

    public CPUParticleSystem(int maxParticles, DisplayMode displayMode,
	    boolean fullscreen, boolean vsync) throws LWJGLException {
	super(TITLE, displayMode, fullscreen, vsync);

	ratio = (float) displayMode.getWidth() / displayMode.getHeight();

	this.maxParticles = maxParticles;
	queuedParticles = new ArrayList<Particle>();

	start();
    }

    @Override
    public int getActiveParticle() {
	return activeParticles;
    }

    public void addParticle(Particle particle) {
	synchronized (queuedParticles) {
	    queuedParticles.add(particle);
	}
    }

    private void initVBO() {
	IntBuffer dummy = BufferUtils.createIntBuffer(2);
	GL15.glGenBuffers(dummy);

	positionVbo = dummy.get(0);
	velocityVbo = dummy.get(1);

	int bufferSize = maxParticles * PARTICLE_BUFFER_SIZE_FACTOR;

	for (int i = 0; i < dummy.limit(); i++) {
	    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, dummy.get(i));
	    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferSize,
		    GL15.GL_DYNAMIC_DRAW);
	}

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
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
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    private void update() {
	long time = System.nanoTime();
	float deltaTime = (time - lastTime) / 1000000000f;
	lastTime = time;

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

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, bufferOffset,
			positionData);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, velocityVbo);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, bufferOffset,
			velocityData);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		queuedParticles.clear();
		activeParticles += particles;
	    }
	}

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

	float mx = (float) Mouse.getX() / Display.getDisplayMode().getWidth()
		- 0.5f;
	float my = (float) Mouse.getY() / Display.getDisplayMode().getHeight()
		- 0.5f;
	mx *= 2 * ratio;
	my *= 2;

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo);
	FloatBuffer positionData = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER,
		GL15.GL_READ_WRITE, null).asFloatBuffer();

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, velocityVbo);
	FloatBuffer velocityData = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER,
		GL15.GL_READ_WRITE, null).asFloatBuffer();

	while (positionData.position() < positionData.capacity()) {
	    int old = positionData.position();

	    float px = positionData.get();
	    float py = positionData.get();
	    float pz = positionData.get();
	    float size = positionData.get();

	    float vx = velocityData.get();
	    float vy = velocityData.get();
	    float vz = velocityData.get();
	    float lifeTime = velocityData.get();

	    px += vx * deltaTime;
	    py += vy * deltaTime;
	    pz += vz * deltaTime;

	    float mpx = mx - px;
	    float mpy = my - py;
	    float mp = (float) Math.sqrt(mpx * mpx + mpy * mpy);

	    float fx = 0;
	    float fy = 0;

	    if ((eventId & 1) != 0) {
		fx += (mpx / mp) * G * (10000000 / (mp * mp));
		fy += (mpy / mp) * G * (10000000 / (mp * mp));
	    }

	    vx += fx * deltaTime;
	    vy += fy * deltaTime;

	    int tmp = positionData.position();
	    positionData.position(old);
	    velocityData.position(old);

	    positionData.put(px);
	    positionData.put(py);
	    positionData.put(pz);
	    positionData.put(size);

	    velocityData.put(vx);
	    velocityData.put(vy);
	    velocityData.put(vz);
	    velocityData.put(lifeTime);

	    positionData.position(tmp);
	    velocityData.position(tmp);
	}

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo);
	GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, velocityVbo);
	GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void render() {
	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo);

	GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

	GL11.glVertexPointer(4, GL11.GL_FLOAT, 0, 0);
	GL11.glColor3f(0, 0, 0);
	GL11.glDrawArrays(GL11.GL_POINTS, 0, activeParticles);

	GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    protected void drawImpl() {
	update();
	render();
    }

    @Override
    protected void destroyImpl() {

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

	CPUParticleSystem particleSystem = new CPUParticleSystem(particleCount,
		displayMode, fullscreen, vSync);

	for (int i = 0; i < particleCount; i++) {
	    Particle particle = new Particle((float) (Math.random() - 0.5),
		    (float) (Math.random() - 0.5), -1, 1, 0, 0, 0,
		    Float.POSITIVE_INFINITY);

	    particleSystem.addParticle(particle);
	}
    }

}