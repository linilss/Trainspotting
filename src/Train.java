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
	static Semaphore crossing = new Semaphore(1,true);
	static int count;


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
		up = !up;
		speed = -speed;
		try {
			tsi.setSpeed(id, 0);
			sleep(2000);
			tsi.setSpeed(id, speed);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}catch(CommandException e) {
			e.printStackTrace();
		}
	}

	/**
		contains the big switch which it enters only when a sensor is activated. 

		**************
		FIX THIS:
		fix sharedDual semaphore, releases too many times
		make it more dynamic (for different velocities)

		**************
		known bugs:
		while running at speeds 10 23, sharedDual was acquired twice (twice as many
		times as it should).
	*/

	public void tryGo() {

	}
	public void makeSwitch(int switchX, int switchY, int trainY, int leftY, 
					Semaphore toRelease, boolean leftOrRight) {
		try {
			if(trainY == leftY) {
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
				int trainY, int leftY, Semaphore toRelease, boolean leftOrRight) {
		try {
			tsi.setSpeed(id, 0);
			s.acquire();
			tsi.setSpeed(id, speed);

				if(trainY == leftY) {
					tsi.setSwitch(switchX, switchY, tsi.SWITCH_LEFT);
					if(leftOrRight) {
						toRelease.release();
					}
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
							if(up) {
								
								if(sharedDual.tryAcquire())
									tsi.setSwitch(4,9, tsi.SWITCH_LEFT);
								else
									tsi.setSwitch(4,9,tsi.SWITCH_RIGHT);
							}
							else {
								if(lowerMainTrack.tryAcquire())
									tsi.setSwitch(3,11,tsi.SWITCH_LEFT);
								else
									tsi.setSwitch(3,11,tsi.SWITCH_RIGHT);
							}
							break;

						case 6:
							if(up) {
								crossing.release();
							}
							else {
								if(!crossing.tryAcquire()) {
									wait(crossing);
								}
							} 
							break;

						case 5:
							if(up){
								if(sharedLower.tryAcquire()) {
									makeSwitch(3,11,y,11, lowerMainTrack, true);
								}




								else {
									waitAndSwitch(sharedLower, 3, 11, y, 11, lowerMainTrack, true);
								}
							}
							else if((y == 11) || (y == 13)) {
								sharedLower.release();
							}
							break;


							case 7:

								if(up) {
									sharedLower.release();
								}
								else {
									if(sharedLower.tryAcquire()) {
										makeSwitch(4, 9, y, 9, sharedDual, true);
									}
									else {
										waitAndSwitch(sharedLower, 4, 9, y, 9, sharedDual, true);
									}
								}

							break;

							case 12:
								if(up) {
									if(sharedUpper.tryAcquire()) {
										makeSwitch(15,9,y, 10, sharedDual, false);
									}
									else 
										waitAndSwitch(sharedUpper, 15, 9, y, 10, sharedDual, false);
								}
								else {
									sharedUpper.release();
								}


							break;

						/*case 9: 
							if(up) {
								sharedLower.release();
								if(sharedUpper.tryAcquire()) {
									if(y == 10)
										tsi.setSwitch(15,9,tsi.SWITCH_LEFT);
									else
										tsi.setSwitch(15,9,tsi.SWITCH_RIGHT);
								}




								else {
									tsi.setSpeed(this.id, 0);
									sharedUpper.acquire();
									if(y == 10)
										tsi.setSwitch(15,9,tsi.SWITCH_LEFT);
									else
										tsi.setSwitch(15,9,tsi.SWITCH_RIGHT);
									tsi.setSpeed(this.id, speed);
								}





							}
							else {
								sharedUpper.release();
								if(sharedLower.tryAcquire()) {
									if(y == 9)
										tsi.setSwitch(4, 9, tsi.SWITCH_LEFT);
									else
										tsi.setSwitch(4, 9, tsi.SWITCH_RIGHT);
								}
								else {
									System.out.println(y);
										waitAndSwitch(sharedLower, 4, 9, 9 , y, null, false);
								}
							}




							break;
							*/

						case 10:
							if(up) {
								System.out.println("japp");
								crossing.release();
							}
							else {
								if(!crossing.tryAcquire()) {
									wait(crossing);
								}
							}
							break;

						case 13: 
							if(up) {
								sharedUpper.release();

								if(!crossing.tryAcquire()) {
									tsi.setSpeed(this.id, 0);
									crossing.acquire();
									tsi.setSpeed(this.id, this.speed);
								}
							}
							else {
								crossing.release();
								if(sharedUpper.tryAcquire()) {
									if(y == 8) {
										tsi.setSwitch(17,7,tsi.SWITCH_LEFT);
									}
									else {
										tsi.setSwitch(17,7,tsi.SWITCH_RIGHT);
										upperMainTrack.release();
										count++;
										System.out.println(count);
									
									}
								}

								else {
									crossing.release();
									tsi.setSpeed(this.id, 0);
									sharedUpper.acquire();
									tsi.setSpeed(this.id, this.speed);
									if(y == 8) {
										tsi.setSwitch(17,7,tsi.SWITCH_LEFT);
									}
									else {
										tsi.setSwitch(17,7,tsi.SWITCH_RIGHT);
										upperMainTrack.release();

									}
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
								if(upperMainTrack.tryAcquire()) {
									count--;
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