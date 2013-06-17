package at.andiwand.particle;

public class Particle {

    public final float positionX;
    public final float positionY;
    public final float positionZ;

    public final float size;

    public final float velocityX;
    public final float velocityY;
    public final float velocityZ;

    public final float lifeTime;

    public Particle(float positionX, float positionY, float positionZ,
	    float size, float velocityX, float velocityY, float velocityZ,
	    float lifeTime) {
	this.positionX = positionX;
	this.positionY = positionY;
	this.positionZ = positionZ;

	this.size = size;

	this.velocityX = velocityX;
	this.velocityY = velocityY;
	this.velocityZ = velocityZ;

	this.lifeTime = lifeTime;
    }

}