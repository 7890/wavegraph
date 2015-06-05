/* RiffRead32 class for IE JVM reads existing RIFF format file and determines 
	(1) if valid RIFF file
	(2) chunk structure
	(3) details of fmt chunk
	(4) details of list chunk
                           Copyright  JavaScience Consulting  01/03/2003, 10/26/2006 , 11/15/2006, 03/31/2007, 05/17/2007     */


import java.io.* ;
import java.awt.* ;
import java.util.Date ;
import java.util.Hashtable;

public class RiffRead32 {

 static final int WAVE_FORMAT_PCM = 0x0001;
 static final int WAVE_FORMAT_IEEE_FLOAT = 0x0003 ;
 static final int WAVE_FORMAT_EXTENSIBLE = 0xFFFE;

 static final String title = "RiffRead32 " ;
 static final String[] infotype = {"IARL", "IART", "ICMS", "ICMT", "ICOP", "ICRD", "ICRP", "IDIM",
	"IDPI", "IENG", "IGNR", "IKEY", "ILGT", "IMED", "INAM", "IPLT", "IPRD", "ISBJ",
	"ISFT", "ISHP", "ISRC", "ISRF", "ITCH", "ISMP", "IDIT" } ;

 static final String[] infodesc = {"Archival location", "Artist", "Commissioned", "Comments", "Copyright", 
	"Creation date","Cropped", "Dimensions", "Dots per inch", "Engineer", "Genre", "Keywords", 
	"Lightness settings", "Medium", "Name of subject", "Palette settings", "Product", "Description",
	"Software package", "Sharpness", "Source", "Source form", "Digitizing technician", 
	"SMPTE time code", "Digitization time"};


 public static void main(String args[]) throws IOException {
  StringBuffer txtbuf = new StringBuffer() ;
  String nl = System.getProperty("line.separator") ;
  String fileSeparator = System.getProperty("file.separator") ;
  String selectFileDir ;
  String selectFile="";
  FileInputStream fis = null ;
  FileOutputStream fos = null ;
  Frame myFrame = new Frame("frame") ;
  long datasize = 0;
  int bytespersec = 0;
  int byteread = 0;
  boolean isPCM = false;

  Hashtable listinfo = new Hashtable() ;
  for(int i=0; i<infotype.length; i++)  //build the hashtable of values for easy searching
	listinfo.put(infotype[i], infodesc[i]);

  if(args.length>0)
	selectFile = args[0];
  if(!(new File(selectFile)).exists()) {  //if file argument is not valid, show filedialog
   FileDialog myFD; 
   myFD= new FileDialog(myFrame, "Select a WAVE File ...") ;
   myFD.setVisible(true) ;    // this blocks main program thread until select.
   if(myFD.getFile() == null){
	myFrame.dispose();
	String info = "No valid file selected" ;
	System.out.println(info);
	System.exit(1);
   }
   selectFileDir = myFD.getDirectory() ;  // determine is file directory ends properly.
   if( selectFileDir.charAt(selectFileDir.length()-1) != fileSeparator.charAt(0)) 
         selectFileDir += fileSeparator ;
   selectFile = selectFileDir + myFD.getFile() ;
  }

  try {fis = new  FileInputStream(selectFile); }
   catch (IOException ie) {;}

 DataInputStream dis = new DataInputStream(fis) ;
 

try {  

  int riffdata=0;  // size of RIFF data chunk.
  int chunkSize=0, infochunksize=0, bytecount=0, listbytecount=0;
  String sfield="", infofield="", infodescription="", infodata="";
  String sp = "   " ;  // spacer string.
  String indent = sp + "   " ;
long filesize = (new File(selectFile)).length() ;  // get file length.

txtbuf.append(selectFile   + "    LENGTH:  " + filesize + " bytes\n\n") ; 

/*  --------  Get RIFF chunk header --------- */
  for (int i=1; i<=4; i++) 
     sfield+=(char)dis.readByte() ;
   if (!sfield.equals("RIFF")) {
	txtbuf.append(" ****  Not a valid RIFF file  ****\n") ;
	System.out.println(txtbuf.toString());
	System.exit(1) ;
    }      

  for (int i=0; i<4; i++) 
	chunkSize += dis.readUnsignedByte() *(int)Math.pow(256,i) ;
	txtbuf.append("\n" + sfield + " ----- data size: "+chunkSize+ " bytes\n") ;
   
 sfield="" ;
  for (int i=1; i<=4; i++) 
	sfield+=(char)dis.readByte() ;
	txtbuf.append("Form-type: "+ sfield + "\n\n") ;

  riffdata=chunkSize ;
/* --------------------------------------------- */
//System.out.println(txtbuf.toString());

   bytecount = 4 ;  // initialize bytecount to include RIFF form-type bytes.

  while (bytecount < riffdata )  {    // check for chunks inside RIFF data area. 
      sfield="" ;
	int firstbyte = dis.readByte() ;
	if(firstbyte == 0) {  //if previous data had odd bytecount, was padded by null so skip
	  bytecount++;
	  continue;
	}
	  sfield+=(char)firstbyte;  //if we have a new chunk
          for (int i=1; i<=3; i++) 
            sfield+=(char)dis.readByte() ;

     chunkSize=0 ;
         for (int i=0; i<4; i++) 
           chunkSize += dis.readUnsignedByte() *(int)Math.pow(256,i) ;
	bytecount += (8+chunkSize) ;
	txtbuf.append("\n" + sfield + " ----- data size: "+chunkSize+ " bytes\n") ;

    if (sfield.equals("data"))   //get data size to compute duration later.
	datasize = chunkSize;

    if (sfield.equals("fmt ")) {               // extract info from "format" chunk.
         if (chunkSize<16) {
             txtbuf.append(" ****  Not a valid fmt chunk  ****\n") ;
             System.exit(1);
           }      
       int wFormatTag = dis.readUnsignedByte() + dis.readUnsignedByte()*256;
       if (wFormatTag ==WAVE_FORMAT_PCM || wFormatTag ==WAVE_FORMAT_EXTENSIBLE || wFormatTag == WAVE_FORMAT_IEEE_FLOAT)
	isPCM = true;
            if (wFormatTag == WAVE_FORMAT_PCM) 
              txtbuf.append(indent + "wFormatTag:  WAVE_FORMAT_PCM\n") ;
            else if (wFormatTag == WAVE_FORMAT_EXTENSIBLE)
	txtbuf.append(indent + "wFormatTag:  WAVE_FORMAT_EXTENSIBLE\n") ;
	else if (wFormatTag == WAVE_FORMAT_IEEE_FLOAT)
	  txtbuf.append(indent + "wFormatTag:  WAVE_FORMAT_IEEE_FLOAT\n") ;
	else
              txtbuf.append(indent + "wFormatTag:  non-PCM format  " + wFormatTag + "\n") ;


       int nChannels = dis.readUnsignedByte() ;
           dis.skipBytes(1) ;
             txtbuf.append(indent + "nChannels:  "+nChannels+"\n") ;
       int nSamplesPerSec=0 ;
         for (int i=0; i<4; i++) 
           nSamplesPerSec += dis.readUnsignedByte() *(int)Math.pow(256,i) ;
             txtbuf.append(indent + "nSamplesPerSec:  "+nSamplesPerSec+"\n") ;
       int nAvgBytesPerSec=0 ;
         for (int i=0; i<4; i++) 
           nAvgBytesPerSec += dis.readUnsignedByte() *(int)Math.pow(256,i) ;
	   bytespersec = nAvgBytesPerSec;
             txtbuf.append(indent + "nAvgBytesPerSec:  "+nAvgBytesPerSec+"\n") ;
       int nBlockAlign=0;
         for (int i=0; i<2; i++)
           nBlockAlign += dis.readUnsignedByte() *(int)Math.pow(256,i) ;
             txtbuf.append(indent + "nBlockAlign:  "+nBlockAlign+"\n") ;
          if (isPCM) {     // if PCM or EXTENSIBLE format
             int nBitsPerSample = dis.readUnsignedByte() ;
             dis.skipBytes(1) ;
             txtbuf.append(indent + "nBitsPerSample:  "+nBitsPerSample+"\n") ;
            }
           else  dis.skipBytes(2) ;
          dis.skipBytes(chunkSize-16) ; //skip over any extra bytes in format specific field.                                  
      }
    else
    if(sfield.equals("LIST")) {
	String listtype="" ;
	for (int i=1; i<=4; i++) 
	 listtype+=(char)dis.readByte() ;
	if(!listtype.equals("INFO")) { //skip over LIST chunks which don't contain INFO subchunks
	 dis.skipBytes(chunkSize-4);
	 continue;
	}

	listbytecount = 4;
	txtbuf.append("\n------- INFO chunks -------\n");
	while(listbytecount < chunkSize) {  //iterate over all entries in LIST chunk
	 infofield="";
	 infodescription = "";
	 infodata="";

	firstbyte = dis.readByte() ;
	if(firstbyte == 0) {  //if previous data had odd bytecount, was padded by null so skip
	  listbytecount++;
	  continue;
	}
	  infofield+=(char)firstbyte;  //if we have a new chunk
          for (int i=1; i<=3; i++)  //get the remaining part of info chunk name ID
		infofield+=(char)dis.readByte() ;
	  infochunksize=0 ;
          for (int i=0; i<4; i++)  //get the info chunk data byte size
		infochunksize += dis.readUnsignedByte() *(int)Math.pow(256,i) ;
	  listbytecount += (8+infochunksize) ;

	  for(int i=0; i<infochunksize; i++) {   //get the info chunk data
		byteread=dis.readByte() ;
		if(byteread == 0)    //if null byte in string, ignore it
		 continue;
		infodata+=(char)byteread;
	 }

	infodescription = (String)listinfo.get(infofield);
	if(infodescription !=null)
	  txtbuf.append(infodescription + ": " + infodata + "\n") ;
	else
	  txtbuf.append("unknown : " + infodata + "\n") ;
	}
//------- end iteration over LIST chunk ------------
	txtbuf.append("------- end INFO chunks -------\n");
   }

    else    // if NOT the fmt or LIST chunks just skip over the data.
	dis.skipBytes(chunkSize) ;
  }  // end while.
//-----------  End of chunk iteration -------------

    if(isPCM && datasize>0){   //compute duration of PCM wave file
	long waveduration = 1000L*datasize/bytespersec; //in msec units
	long mins = waveduration/ 60000;	// integer minutes
	double secs = 0.001*(waveduration % 60000);	//double secs.
	txtbuf.append("\nwav duration:  " + mins + " mins  " + secs +  " sec\n") ;
	}

    txtbuf.append("\nFinal RIFF data bytecount: " + bytecount+"\n") ;
     if ((8+bytecount) != (int)filesize) 
        txtbuf.append("!!!!!!! Problem with file structure  !!!!!!!!! \n") ; 
      else 
        txtbuf.append("File chunk structure consistent with valid RIFF \n") ; 
   System.out.println(txtbuf.toString());

  System.exit(0);
 }  // end try.


 finally {
         myFrame.dispose() ;
         dis.close() ;                         // close all streams.
         fis.close() ;
       }
 }



}