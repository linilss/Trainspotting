import TSim.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Train extends Thread {
	TSimInterface tsi;
	int id;
	int speed;
	boolean up;

	static Semaphore sharedLower = new Semaphore(1, true);
	static Semaphore sharedDual = new Semaphore(1, true);
	static Semaphore lowerMainTrack = new Semaphore(0, true);
	static Semaphore sharedUpper = new Semaphore(1,true);
	static Semaphore upperMainTrack = new Semaphore(0, true);
	static Semaphore crossing = new Semaphore(1,true);

	public Train(int id, int speed, boolean up) {
		this.id = id;
		this.speed = speed;
		this.up = up;
		tsi = TSimInterface.getInstance();

		try {
				tsi.setSpeed(id, speed);
			}catch(CommandException e) {
				e.printStackTrace();	
			}
	}

	public void boardStation() {
		up = !up;
		speed = -speed;
		try {
			tsi.setSpeed(id, 0);
			sleep(1000 + (20 * Math.abs(speed)));
			tsi.setSpeed(id, speed);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}catch(CommandException e) {
			e.printStackTrace();
		}
	}

	public void makeSwitch(boolean toLeft, int switchX, int switchY) {
		try {
			if(toLeft)
				tsi.setSwitch(switchX, switchY, tsi.SWITCH_LEFT);
			else
				tsi.setSwitch(switchX, switchY, tsi.SWITCH_RIGHT);

		}catch(CommandException e) {
			e.printStackTrace();
		}

	}
	public void makeSwitch(int switchX, int switchY, boolean toLeft, 
					Semaphore toRelease, boolean leftOrRight) {
		try {
				if(toLeft) {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_LEFT);

					if(leftOrRight)
						toRelease.release();
				}
				else {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_RIGHT);	
					if(!leftOrRight)
						toRelease.release();
				}
		}catch(CommandException e) {
			e.printStackTrace();
		}
	}

	public void wait(Semaphore s) {
		try {
			tsi.setSpeed(id, 0);
			s.acquire();
			tsi.setSpeed(id, speed);

		}catch(InterruptedException e) {
		e.printStackTrace();
		}catch(CommandException e) {
			e.printStackTrace();
		}
	}
	public void waitAndSwitch(Semaphore s, int switchX, int switchY, 
				boolean toLeft, Semaphore toRelease, boolean leftOrRight) {
		try {
			tsi.setSpeed(id, 0);
			s.acquire();
			tsi.setSpeed(id, speed);

				if(toLeft) {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_LEFT);
					
					if(leftOrRight) 
						toRelease.release();
				}
				else {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_RIGHT);	

					if(!leftOrRight)
						toRelease.release();	
				}
		}catch(InterruptedException e) {
			e.printStackTrace();
		}catch(CommandException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		tsi = TSimInterface.getInstance();
		int x;
		int y;
		while(true) {
			try{

				SensorEvent e = tsi.getSensor(id);
				boolean sensorActive = e.getStatus() == 1;
				x = e.getXpos();
				y = e.getYpos();
				if(sensorActive) {
					switch(x){
						case 1:
							if(up){
								makeSwitch(sharedDual.tryAcquire(), 4,9);
							}
							else { 
								makeSwitch(lowerMainTrack.tryAcquire(), 3,11);
							}
							break;

						case 5:
							if(up){
								if(!sharedLower.tryAcquire()) {
									wait(sharedLower);
								}
								tsi.setSwitch(3,11, tsi.SWITCH_RIGHT);
							}
							else {
								sharedLower.release();
							}
							break;

						case 7:
							if(up) {
								sharedLower.release();
							}

							else {
								if(sharedLower.tryAcquire()) {
									makeSwitch(4, 9, (y==9), sharedDual, true);
								}

								else{
									waitAndSwitch(sharedLower, 4, 9, y==9, sharedDual, true);
								}
							}
							break;

						case 6:
							if(y == 5) {
								if(up) {
									crossing.release();
								}

								else {
									if(!crossing.tryAcquire()) {
										wait(crossing);
									}
								}
							}
							else {
								if(up){
									if(sharedLower.tryAcquire()) {
										makeSwitch(3,11,(y==11), lowerMainTrack, true);
									}
									else {
										waitAndSwitch(sharedLower, 3, 11, y==11, lowerMainTrack, true);
									}
								}
								else 
									sharedLower.release();
							}	 
							break;

						case 10:
							if(up) {
								crossing.release();
							}
							else {
								if(!crossing.tryAcquire()) {
									wait(crossing);
								}
							}
							break;

						case 12:
							if(up) {
								if(sharedUpper.tryAcquire()){
									makeSwitch(15,9,(y==10), sharedDual, false);
								}
								else {
									waitAndSwitch(sharedUpper, 15, 9, y==10, sharedDual, false);
								}
							}
							else {
								sharedUpper.release();
							}
						break;


						case 13: 
							if(up) {
								sharedUpper.release();
								if(!crossing.tryAcquire()) {
									wait(crossing);
								}
							}
							else {
									crossing.release();
									waitAndSwitch(sharedUpper, 17,7, y==8, upperMainTrack, false);
							}
							break;


						case 14:
							if(!up && ((y == 13) || (y == 11))) {
								boardStation();
							}
							else if(up &&((y == 3) || (y == 5))) {
								boardStation();
							}
							break;


						case 19:
							if(up) {
								makeSwitch(!upperMainTrack.tryAcquire(), 17,7);							
							}
							else {
								makeSwitch(!sharedDual.tryAcquire(), 15,9);
							}
							break;
					}
				}
			}catch(CommandException e) {
				e.printStackTrace();
			}catch(InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}	

				/*
				if(sharedUpper.availablePermits() > 1 ||
				 sharedDual.availablePermits() > 1 ||
				  sharedLower.availablePermits() > 1 || 
				  lowerMainTrack.availablePermits() > 1 || 
				  upperMainTrack.availablePermits() > 1 || 
				  crossing.availablePermits() > 1){
					System.out.println("sharedupper: " + sharedUpper.availablePermits());
				System.out.println("sharedLower: " + sharedLower.availablePermits());
				System.out.println("sharedDual: " + sharedDual.availablePermits());
				System.out.println("lowerMainTrack: " + lowerMainTrack.availablePermits());
				System.out.println("upperMainTrack: " + upperMainTrack.availablePermits());
				System.out.println("crossing: " + crossing.availablePermits());
					System.exit(1);
				}
				*/