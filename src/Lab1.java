import TSim.*;

public class Lab1 {

  public Lab1(Integer speed1, Integer speed2) {
    TSimInterface tsi = TSimInterface.getInstance();
    System.out.println("sss");

    Train t1 = new Train(1, speed1, false);
    Train t2 = new Train(2, speed2, true);
    t1.start();
    t2.start();
  }
}
