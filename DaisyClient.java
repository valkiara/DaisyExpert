package Main;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;



public class DaisyClient {
	
	private byte SOH = (byte)(Integer.parseInt("01", 16));
	private byte LEN;
    private byte SEQ;
    private byte CMD;
    private byte[] DATA;
    private byte[] BCC;
    private byte POST_AMBLE = (byte)(Integer.parseInt("05", 16));
    private byte POST_AMBLE_2 = (byte)(Integer.parseInt("04", 16));
    private byte ETX = (byte)(Integer.parseInt("03", 16));

    public SerialPort xPort;
    
    public DaisyClient(String PortName)
    {
    	xPort = SerialPort.getCommPort(PortName);
    	xPort.setNumStopBits(1);
    	xPort.setBaudRate(9600);
    	xPort.setNumDataBits(8);
    	xPort.setParity(0);
    	xPort.openPort();
    	
    	xPort.addDataListener(new SerialPortDataListener() {

			@Override
			public int getListeningEvents() {
				
				return SerialPort.LISTENING_EVENT_DATA_WRITTEN;
			}

			@Override
			public void serialEvent(SerialPortEvent event) {
				if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_WRITTEN)
				{
					return;
				}
				System.out.println("daidzaxa!");
				Read();
				
			}
    		
    	});
    	
    	
    }
    

    
    private void Read()
    {
    	int SYNCount = 0;
    	try 
		{
			TimeUnit.MILLISECONDS.sleep(300);
			
		} 
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
    	
    	byte[] readByts = new byte[4096];
		xPort.readBytes(readByts, readByts.length);
		
		
		if(readByts[0] == 21)
		{
			System.out.println("NAK");
			Write(CMD, DATA);
		}
		else if(readByts[0] == 22)
		{
			while(readByts[0] != 1)
			{
				SYNCount++;
				System.out.println("SYNCount: " + SYNCount);
				try 
				{
					TimeUnit.MILLISECONDS.sleep(60);
					
				} 
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				xPort.readBytes(readByts, readByts.length);
				
			}
			
		}
		
		if(readByts[0] == 1)
		{
			List<Byte> bytes = new ArrayList<Byte>();
			
			
			for (int px = 4; px <= 200 + 4; px++)
	        {
				if (readByts[px] == 0x4)
	                break; // TODO: might not be correct. Was : Exit For
				bytes.add(readByts[px]);
	        }
			byte[] fromLst = new byte[bytes.size()];
			for(int i = 0; i < bytes.size(); i++)
			{
				fromLst[i] = bytes.get(i);
			}
			
			String answer = new String(fromLst);
			System.out.println(answer);
		}
		
		
    }
    
    
    
    

	private String SendCommand(int CommandCode, byte[] xData) {
		
		CMD = (byte)CommandCode;
		DATA = xData;
		byte[] BytesToWrite = Write(CMD, DATA);
		xPort.writeBytes(BytesToWrite, BytesToWrite.length);
		
		

//		byte[] readByts = new byte[4096];
//		
//		xPort.readBytes(readByts, readByts.length);
//		
//		 String answer = new String(readByts);
		
		return null;
	}
	
	
	private byte[] Write(int Command, byte[] Data)
	{
		byte[] packedMessage = new byte[10];
		
		if(Data == null)
		{
			LEN = (byte) ((Integer.valueOf("04", 16) + Integer.valueOf("20", 16)));
			SEQ = GetSEQ();
			
			
			packedMessage[0] = SOH;
			packedMessage[1] = LEN;
			packedMessage[2] = SEQ;
			packedMessage[3] = (byte)CMD;
			packedMessage[4] = POST_AMBLE;
			
			int V_Int = LEN + unsignedToBytes(SEQ) + unsignedToBytes(POST_AMBLE) + unsignedToBytes(CMD);
			String reString = GetBCCStrHex(V_Int);
			
			packedMessage[5] = (byte)(Integer.parseInt("3" + reString.charAt(0), 16));
			packedMessage[6] = (byte)(Integer.parseInt("3" + reString.charAt(1), 16));
			packedMessage[7] = (byte)(Integer.parseInt("3" + reString.charAt(2), 16));
			packedMessage[8] = (byte)(Integer.parseInt("3" + reString.charAt(3), 16));
            packedMessage[9] = ETX;
			
            return packedMessage;
		}
		else
		{
			LEN = (byte) (Data.length + 4 + 32);
			SEQ = GetSEQ();
			
			packedMessage[0] = SOH;
			packedMessage[1] = LEN;
			packedMessage[2] = SEQ;
			packedMessage[3] = (byte)CMD;
			
			int II = 4;
			int bCal = 0;
			
			for(int i = 0; i < Data.length; i++)
			{
				packedMessage[II] = Data[i];
				bCal += Data[i];
				II++;
			}
			
			packedMessage[II] = POST_AMBLE;
			II++;
			
			int V_Int = unsignedToBytes(LEN) + unsignedToBytes(SEQ) + unsignedToBytes(POST_AMBLE) + unsignedToBytes(CMD) + bCal;
			
			String reString = GetBCCStrHex(V_Int);
			
			packedMessage[II++] = (byte)(Integer.parseInt("3" + reString.charAt(0), 16));
			packedMessage[II++] = (byte)(Integer.parseInt("3" + reString.charAt(1), 16));
			packedMessage[II++] = (byte)(Integer.parseInt("3" + reString.charAt(2), 16));
			packedMessage[II++] = (byte)(Integer.parseInt("3" + reString.charAt(3), 16));
            packedMessage[II++] = ETX;
            
            return packedMessage;
			
		}
	}
	
	public static int unsignedToBytes(byte b) {
	    return b & 0xFF;
	}
	
	private String GetBCCStrHex(int V_Int)
    {
		String strHex = Integer.toHexString(V_Int);
		String reString = strHex;

        if (strHex.length() == 1)
        {
            reString = "000" + strHex;
        }
        if (strHex.length() == 2)
        {
            reString = "00" + strHex;
        }
        if (strHex.length() == 3)
        {
            reString = "0" + strHex;
        }

        return reString;
    }
	
	
	private byte GetSEQ() 
	{
		Random ran = new Random();
		int x = ran.nextInt(1) + 255;
		return (byte) x;
	}
	
	
	
	/// <summary>
    /// 62 (3Eh) GET DATE AND TIME INFORMATION
    ///Data field: No data.
    ///Date and time information is received from FD.
    /// </summary>
    /// <returns>String Reply: { DD–MM–YY}{Space}{HH:MM:SS}</returns>
    public String GetDateAndTimeInformation()
    {
        return SendCommand(62, null);
    }
    
    
  /// <summary>
    /// 63 (3Fh) DISPLAY DATE AND TIME
    /// Data field: No data.
    /// Reply : No data.Current date and time are displayed in format : DD-MM-YYYY HH:MM.
    /// </summary>
    public void DisplayDateAndTime()
    {
        SendCommand(63, null);
    }
    
  /// <summary>
    /// 166 (A6h) PRINT SYSTEM PARAMETERS
    /// Data field: No data
    /// Reply: Code
    /// Code One byte with the following options:
    /// "P" –when the operation is completed successfully
    /// "F" - when the operation is not completed successfully
    /// FD prints number, name and system paraters’ values.
    /// </summary>
    /// <returns></returns>
    public String PrintSystemParameters()
    {
       return SendCommand(166, null);
    }
}
