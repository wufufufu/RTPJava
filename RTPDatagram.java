import java.io.*;
import java.util.zip.CRC32;


public class RTPDatagram {
	// In practice, the PSH, URG, and the urgent data pointer are not used.

	// REMOVED FROM TCP
	// public static final int URG = 0b100000;

	public static final int ACK = 8;

	// REMOVED FROM TCP
	// public static final int PSH = 0b001000;
	public static final int RST = 4;
	public static final int SYN = 2;
	public static final int FIN = 1;

	public int sourcePort;
	public int destPort;

	public long sequenceNumber;
	public long ackNumber;

	public int headerLength;
	public int flags;

	public int receiveWindowSize;
	public byte[] receiveWindow;

	// CRC32
	public long checksum;

	// REMOVED FROM TCP
	// public int urgentDataPointer;


	byte[] data;

	public RTPDatagram(int src, int dest, int flags, byte[] receiveWindow, byte[] data){
		this.sourcePort = src;
		this.destPort = dest;
		this.flags = flags;

		this.receiveWindow = receiveWindow;
		receiveWindowSize = (int) (Math.log((double) receiveWindow.length/Math.log(2.0)) + 1);
		RTPUtil.debug("receiveWindowSize:" + receiveWindowSize);
		this.data = data;
	}

	public RTPDatagram(byte[] rawData){
		int rawDataPointer = 0;

		this.sourcePort = ((((int)rawData[rawDataPointer]) << 8)  + ((int)rawData[rawDataPointer + 1])) & 0x0000FFFF;
		rawDataPointer += 2;

		this.destPort = ((((int)rawData[rawDataPointer]) << 8)  + ((int)rawData[rawDataPointer + 1])) & 0x0000FFFF;
		rawDataPointer += 2;

		this.sequenceNumber = (((long)rawData[rawDataPointer]) << 24) + 
		(((long)rawData[rawDataPointer + 1]) << 16) + 
		(((long)rawData[rawDataPointer + 2]) << 8) + 
		(((long)rawData[rawDataPointer + 3])) & 0x00000000FFFFFFFFL;
		rawDataPointer += 4;

		this.ackNumber = (((long)rawData[rawDataPointer]) << 24) + 
		(((long)rawData[rawDataPointer + 1]) << 16) + 
		(((long)rawData[rawDataPointer + 2]) << 8) + 
		(((long)rawData[rawDataPointer + 3])) & 0x00000000FFFFFFFFL;
		rawDataPointer += 4;

		this.headerLength = rawData[rawDataPointer] & 0xF;
		this.flags = (rawData[rawDataPointer] >> 4) & 0xF;
		rawDataPointer += 1;

		this.receiveWindowSize = (rawData[rawDataPointer] << 8) + ((int)rawData[rawDataPointer + 1]) & 0x0000FFFF;

		this.receiveWindow = new byte[this.receiveWindowSize];
		for (int i = 0; i < this.receiveWindowSize; i++){
			this.receiveWindow[i] = rawData[rawDataPointer + i];
		}
		rawDataPointer += this.receiveWindowSize;

		this.checksum = (((long)rawData[rawDataPointer]) << 24) + 
		(((long)rawData[rawDataPointer + 1]) << 16) + 
		(((long)rawData[rawDataPointer + 2]) << 8) + 
		(((long)rawData[rawDataPointer + 3])) & 0x00000000FFFFFFFFL;
	}

	public ByteArrayOutputStream getHeaderByteArrayNoChecksum(){
		ByteArrayOutputStream bb = new ByteArrayOutputStream();

		RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toBytes(sourcePort), bb);
		RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toBytes(destPort), bb);
    	
    	RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toIntBytes(sequenceNumber), bb);
    	RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toIntBytes(ackNumber), bb);

    	int headerLengthAndFlagsByte = (headerLength & 0xF) | ((flags & 0xF) << 4);
    	RTPUtil.debug(Integer.toBinaryString(headerLengthAndFlagsByte));

    	bb.write((headerLengthAndFlagsByte & 0xFF));
    	bb.write((receiveWindowSize & 0xFF));

    	RTPUtil.writeByteArrayToByteArrayOutputStream(receiveWindow, bb);

    	return bb;
	}

	public boolean checkChecksum(){
		ByteArrayOutputStream bb = getHeaderByteArrayNoChecksum();

    	CRC32 crc = new CRC32();
    	crc.update(bb.toByteArray());
    	crc.update(data);
    	return (this.checksum == crc.getValue());
	}

    public byte[] getByteArray(){
    	ByteArrayOutputStream bb = getHeaderByteArrayNoChecksum();

    	CRC32 crc = new CRC32();
    	crc.update(bb.toByteArray());
    	crc.update(data);

    	this.checksum = crc.getValue();

    	RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toIntBytes(this.checksum), bb);
    	RTPUtil.writeByteArrayToByteArrayOutputStream(data, bb);

    	return bb.toByteArray();
    }
}