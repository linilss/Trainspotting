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
		System.out.println(crossing.availablePermits());
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
	public void makeSwitch(boolean toLeft, int switchX, int switchY,
					Semaphore toRelease, boolean releaseFromLeft) {
		try {
				if(toLeft) {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_LEFT);

					if(releaseFromLeft){
						toRelease.release();
					}
				}
				else {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_RIGHT);	
					if(!releaseFromLeft) {
						System.out.println("japp");
						toRelease.release();
					}

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
					
					if(leftOrRight) {
						toRelease.release();
					}
				}
				else {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_RIGHT);	

					if(!leftOrRight) {
						toRelease.release();	
					}
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
		final boolean fromRight = false;
		final boolean fromLeft = true;

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

						case 4:
							if(up && (y == 13)){
								if(!sharedLower.tryAcquire()) {
									wait(sharedLower);
								}
								tsi.setSwitch(3,11, tsi.SWITCH_RIGHT);
							}
							break;
						case 6:
							if(y == 6) {
								if(!up) {
									if(!crossing.tryAcquire()) {
										wait(crossing);
									}
								}
							}
							else {
								if(up){
									if(sharedLower.tryAcquire()) {
										makeSwitch((y==11), 3, 11, lowerMainTrack, fromLeft);
									}
									else {
										waitAndSwitch(sharedLower, 3, 11, y==11, lowerMainTrack, fromLeft);
									}
								}
							}	 
							break;

						case 7:
							if(!up && (y >7)) {
								if(sharedLower.tryAcquire()) {
									makeSwitch((y==9), 4, 9, sharedDual, fromLeft);
								}
								else{
									waitAndSwitch(sharedLower, 4, 9, y==9, sharedDual, fromLeft);
								}
							}
							break;

						case 9:
							if(!up && (y == 5)) {
								if(!crossing.tryAcquire()) {
									wait(crossing);
								}
							}
							break;

						case 10:
							System.out.println(crossing.availablePermits());
							if(up && !crossing.tryAcquire()) {
								wait(crossing);
							}
							break;
						case 11:
							System.out.println(crossing.availablePermits());
							if(up && !crossing.tryAcquire()) {
								wait(crossing);
							}
							break;

						case 12:

							if(up) {
								if(sharedUpper.tryAcquire()){
									makeSwitch((y==10), 15, 9, sharedDual, fromRight);
								}
								else {
									waitAndSwitch(sharedUpper, 15, 9, y==10, sharedDual, fromRight);
								}
							}
						break;

						case 14:
							if(y != 9) {
								if(!up) {
									if(sharedUpper.tryAcquire()) {
										System.out.println("woo1");
										makeSwitch(y==8, 17, 7, upperMainTrack, fromRight);
									}
									else{
										waitAndSwitch(sharedUpper, 17,7, y==8, upperMainTrack, fromRight);
									}
								}
							}
							break;

						case 15: 
							if(up && ((y == 3) || (y == 5))) {
								boardStation();
							}
							else if(!up && ((y == 11) || (y == 13))) {
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
				else if(!sensorActive) {
					switch(x) {
						case 3:
							if(!up &&(y == 12)) {
								sharedLower.release();
							}
						break;
						case 4: 
							if(up && (y == 10)) {
								sharedLower.release();
							}
							else if(!up && (y == 11)) {
								sharedLower.release();
							}
						break;
						case 5:
						 	if(up && (y == 9)) {
						 		sharedLower.release();
						 	}
						 break;
						 case 7:
						 	if(up && (y == 7)) {
						 		crossing.release();
						 	}
						 break;
						 case 8:
						 	if(up && (y == 6)) {
						 		crossing.release();
						 	}
						 	else if(!up && (y == 8)) {
						 		crossing.release();
						 	}
						 break;
						 case 9:
						 	if(!up && (y == 7)) {
						 		crossing.release();
						 		System.out.println("sss" + crossing.availablePermits());
						 	}
						 break;
						 case 14:
						 	if(!up && (y == 9)) {
						 		sharedUpper.release();
						 	}
						 break;
						 case 15:
 						 	if(!up && (y == 10)) {
 						 		sharedUpper.release();
 						 	}
						 break;
						 case 16:
						 	if(up && (y == 7)) {
						 		sharedUpper.release();
						 	}
						 break;
						 case 17:
						 	if(up && (y == 8)) {
						 		sharedUpper.release();
						 	}
						 break;
					}
				}
					System.out.println(crossing.availablePermits());

			}catch(CommandException e) {
				e.printStackTrace();
			}catch(InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}	