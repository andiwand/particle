package at.andiwand.particle;

public abstract class ParticleSystem extends Thread {

    private int frameCount;

    private boolean running;

    private Thread fpsThread = new Thread() {
	private long lastReadTime;

	public void run() {
	    lastReadTime = System.nanoTime();
	    int tmpFrameCount = 0;
	    long readTime = 0;
	    double time = 0d;
	    double fps = 0d;

	    while (true) {
		try {
		    Thread.sleep(500);
		} catch (InterruptedException e) {
		    return;
		}

		tmpFrameCount = frameCount;
		frameCount = 0;
		readTime = System.nanoTime();
		time = (readTime - lastReadTime) / 1000000000d;
		lastReadTime = readTime;
		fps = tmpFrameCount / time;

		fpsCallback(fps);
	    }
	}
    };

    public ParticleSystem() {
    }

    public boolean isRunning() {
	return running;
    }

    public void setRunning(boolean running) {
	this.running = running;
    }

    public void run() {
	init();

	while (running) {
	    draw();
	}

	destroy();
    }

    protected abstract void initImpl();

    public void init() {
	initImpl();
	running = true;
	fpsThread.start();
    }

    protected abstract void drawImpl();

    public void draw() {
	drawImpl();
	frameCount++;
    }

    protected abstract void destroyImpl();

    public void destroy() {
	destroyImpl();
	fpsThread.interrupt();
	interrupt();
    }

    protected abstract void fpsCallback(double fps);

}