package Main;

import com.fazecast.jSerialComm.SerialPort;

public class program {

	public static void main(String[] args) {
		
		DaisyClient xClient = new DaisyClient("COM4");
		
		//System.out.println(xClient.xPort.getSystemPortName());
		
		xClient.DisplayDateAndTime();
		
		//String DateAndTime = xClient.GetDateAndTimeInformation();
		
		//xClient.PrintSystemParameters();
		
		// TODO Auto-generated method stub
		System.out.println("Hello Daisy!");
	}

}
