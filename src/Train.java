import TSim.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Train extends Thread {
	TSimInterface tsi;
	int id;
	int speed;
	boolean up;

	// Semaphores
	static Semaphore sharedLower = new Semaphore(1, true);
	static Semaphore sharedDual = new Semaphore(1, true);
	static Semaphore lowerMainTrack = new Semaphore(0, true);
	static Semaphore sharedUpper = new Semaphore(1,true);
	static Semaphore upperMainTrack = new Semaphore(0, true);



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

	public void boardStation(int id) {
		System.out.println("Boarding");
		up = !up;
		speed = -speed;
		try {
			tsi.setSpeed(id, 0);
			sleep(2000);
			tsi.setSpeed(id, speed);
			System.out.println(speed);
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
							if(up) {
								
								if(sharedDual.tryAcquire())
									tsi.setSwitch(4,9, tsi.SWITCH_LEFT);
								else
									tsi.setSwitch(4,9,tsi.SWITCH_RIGHT);
							}
							else {
								sharedDual.release();

								if(lowerMainTrack.tryAcquire())
									tsi.setSwitch(3,11,tsi.SWITCH_LEFT);
								else
									tsi.setSwitch(3,11,tsi.SWITCH_RIGHT);
							}
							break;

						case 6:
							if(up){
								if(sharedLower.tryAcquire()) {
									if(y == 11){
										tsi.setSwitch(3,11,tsi.SWITCH_LEFT);
										lowerMainTrack.release();
									}
									else
										tsi.setSwitch(3,11,tsi.SWITCH_RIGHT);
								}
								else {
									tsi.setSpeed(this.id, 0);
									sharedLower.acquire();
									tsi.setSpeed(this.id, this.speed);
									if(y == 11) {
										tsi.setSwitch(3,11,tsi.SWITCH_LEFT);
										lowerMainTrack.release();
									}
									else
										tsi.setSwitch(3,11,tsi.SWITCH_RIGHT);
								}
							}
							else if(!up && ((y == 11) || (y == 13))) {
								sharedLower.release();
							}
							break;

						case 9: 
							if(up) {
								sharedLower.release();
								if(sharedUpper.tryAcquire()) {
									if(y == 9)
										tsi.setSwitch(15,9,tsi.SWITCH_RIGHT);
									else
										tsi.setSwitch(15,9,tsi.SWITCH_LEFT);
								}
								else {
									tsi.setSpeed(this.id, 0);
									sharedUpper.acquire();
									if(y == 9)
										tsi.setSwitch(15,9,tsi.SWITCH_RIGHT);
									else
										tsi.setSwitch(15,9,tsi.SWITCH_LEFT);
									tsi.setSpeed(this.id, speed);
								}
							}
							else {
								sharedUpper.release();
								System.out.println("Released!");
								if(sharedLower.tryAcquire()) {
									if(y == 9)
										tsi.setSwitch(4, 9, tsi.SWITCH_LEFT);
									else
										tsi.setSwitch(4, 9, tsi.SWITCH_RIGHT);
								}
								else {
									System.out.println("nope");
									tsi.setSpeed(this.id, 0);
									sharedLower.acquire();
									tsi.setSpeed(this.id, this.speed);
									if(y == 9)
										tsi.setSwitch(4, 9, tsi.SWITCH_LEFT);
									else
										tsi.setSwitch(4, 9, tsi.SWITCH_RIGHT);
								}
							}
							break;

						case 13: 
							if(up) {
								sharedUpper.release();
								System.out.println("Released!");
							}
							else {
								if(sharedUpper.tryAcquire()) {
									if(y == 7) {
										tsi.setSwitch(17,7,tsi.SWITCH_RIGHT);
										upperMainTrack.release();
									}
									else
										tsi.setSwitch(17,7,tsi.SWITCH_LEFT);
								}
								else {
									tsi.setSpeed(this.id, 0);
									sharedUpper.acquire();
									tsi.setSpeed(this.id, this.speed);
									if(y == 7) {
										tsi.setSwitch(17,7,tsi.SWITCH_RIGHT);
										upperMainTrack.release();
									}
									else
										tsi.setSwitch(17,7,tsi.SWITCH_LEFT);
								}
							}
							break;

						case 14: // 3b 5f 11b 13f
							if(!up && ((y == 13) || (y == 11))) {
								boardStation(this.id);
							}
							else if(up &&((y == 3) || (y == 5))) {
								boardStation(this.id);
							}
							break;

						case 19:
							if(up){
								sharedDual.release();								
								if(upperMainTrack.tryAcquire()) {
									tsi.setSwitch(17,7,tsi.SWITCH_RIGHT);
								}
								
								else {
									tsi.setSwitch(17,7,tsi.SWITCH_LEFT);
								}
							}
							else {
								if(sharedDual.tryAcquire())
									tsi.setSwitch(15,9,tsi.SWITCH_RIGHT);
								
								else 
									tsi.setSwitch(15,9,tsi.SWITCH_LEFT);

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