//http://coweb.cc.gatech.edu/mediaComp-plan/uploads/55/JavaSound.java

package ch.lowres.wavegraph;

/*
http://web.archive.org/web/20090107141325/http://www.nuvoton-usa.com/en/content/view/283/520/
http://www.digitalpreservation.gov/formats/fdd/fdd000038.shtml

"µ-law and A-law are audio compression schemes defined by ITU-T G.711 that compress 
16 bit linear data down to 8 bits of logarithmic data. The encoding process 
(referred to logarithmic companding) breaks the linear data into segments with 
each progressively higher segment doubling in size. This ensures that the lower 
amplitude signals (where most of the information in speech takes place) get 
the highest bit resolution while still allowing enough dynamic range to encode 
high amplitude signals. Though this method does not provide a very high compression 
ratio (roughly 2:1), it does not require much processing power to decode.

"Mu-law (also written µ-Law) is the encoding scheme used in North America and Japan 
for voice traffic. A-Law (or a-Law) is used in Europe and throughout the rest of 
the world. The two schemes are very similar. Both break the total dynamic range 
into eight positive and eight negative segments. Bit 1 (MSB) identifies the polarity, 
bits 2,3,4 identify the segment, and the last four bits quantize the value within 
the segment. The differences are in the actual coding levels and the bit inversion. 
Nevertheless, both systems offer 2:1 bit compression, thus doubling the capacity 
of a digital transmission circuit while maintaining 'toll quality' voice reproduction."
*/

//=======================================================
class LawEncoding
{
	/* ITU G.711 u-law to linear conversion table */ 
	private static short [] u2l =
	{
		-32124, -31100, -30076, -29052, -28028, -27004, -25980, -24956,
		-23932, -22908, -21884, -20860, -19836, -18812, -17788, -16764,
		-15996, -15484, -14972, -14460, -13948, -13436, -12924, -12412,
		-11900, -11388, -10876, -10364, -9852, -9340, -8828, -8316,
		-7932, -7676, -7420, -7164, -6908, -6652, -6396, -6140,
		-5884, -5628, -5372, -5116, -4860, -4604, -4348, -4092,
		-3900, -3772, -3644, -3516, -3388, -3260, -3132, -3004,
		-2876, -2748, -2620, -2492, -2364, -2236, -2108, -1980,
		-1884, -1820, -1756, -1692, -1628, -1564, -1500, -1436,
		-1372, -1308, -1244, -1180, -1116, -1052, -988, -924,
		-876, -844, -812, -780, -748, -716, -684, -652,
		-620, -588, -556, -524, -492, -460, -428, -396,
		-372, -356, -340, -324, -308, -292, -276, -260,
		-244, -228, -212, -196, -180, -164, -148, -132,
		-120, -112, -104, -96, -88, -80, -72, -64,
		-56, -48, -40, -32, -24, -16, -8, 0,
		32124, 31100, 30076, 29052, 28028, 27004, 25980, 24956,
		23932, 22908, 21884, 20860, 19836, 18812, 17788, 16764,
		15996, 15484, 14972, 14460, 13948, 13436, 12924, 12412,
		11900, 11388, 10876, 10364, 9852, 9340, 8828, 8316,
		7932, 7676, 7420, 7164, 6908, 6652, 6396, 6140,
		5884, 5628, 5372, 5116, 4860, 4604, 4348, 4092,
		3900, 3772, 3644, 3516, 3388, 3260, 3132, 3004,
		2876, 2748, 2620, 2492, 2364, 2236, 2108, 1980,
		1884, 1820, 1756, 1692, 1628, 1564, 1500, 1436,
		1372, 1308, 1244, 1180, 1116, 1052, 988, 924,
		876, 844, 812, 780, 748, 716, 684, 652,
		620, 588, 556, 524, 492, 460, 428, 396,
		372, 356, 340, 324, 308, 292, 276, 260,
		244, 228, 212, 196, 180, 164, 148, 132,
		120, 112, 104, 96, 88, 80, 72, 64,
		56, 48, 40, 32, 24, 16, 8, 0
	};
 
	public static short ulaw2linear( byte ulawbyte) 
	{ 
		return u2l[ulawbyte & 0xFF];
	}
	
    /*
     * This source code is a product of Sun Microsystems, Inc. and is provided
     * for unrestricted use.  Users may copy or modify this source code without
     * charge.
     *
     * linear2alaw() - Convert a 16-bit linear PCM value to 8-bit A-law
     *
     * linear2alaw() accepts an 16-bit integer and encodes it as A-law data.
     *
     *              Linear Input Code       Compressed Code
     *      ------------------------        ---------------
     *      0000000wxyza                    000wxyz
     *      0000001wxyza                    001wxyz
     *      000001wxyzab                    010wxyz
     *      00001wxyzabc                    011wxyz
     *      0001wxyzabcd                    100wxyz
     *      001wxyzabcde                    101wxyz
     *      01wxyzabcdef                    110wxyz
     *      1wxyzabcdefg                    111wxyz
     *
     * For further information see John C. Bellamy's Digital Telephony, 1982,
     * John Wiley & Sons, pps 98-111 and 472-476.
     */ 
    
    /*
     * ITU G.711 a-law
     *
     * conversion table alaw to linear
     */

   private static short [] a2l = 
	{
	-5504, -5248, -6016, -5760, -4480, -4224, -4992, -4736,
	-7552, -7296, -8064, -7808, -6528, -6272, -7040, -6784,
	-2752, -2624, -3008, -2880, -2240, -2112, -2496, -2368,
	-3776, -3648, -4032, -3904, -3264, -3136, -3520, -3392,
	-22016, -20992, -24064, -23040, -17920, -16896, -19968, -18944,
	-30208, -29184, -32256, -31232, -26112, -25088, -28160, -27136,
	-11008, -10496, -12032, -11520, -8960, -8448, -9984, -9472,
	-15104, -14592, -16128, -15616, -13056, -12544, -14080, -13568,
	-344, -328, -376, -360, -280, -264, -312, -296,
	-472, -456, -504, -488, -408, -392, -440, -424,
	-88, -72, -120, -104, -24, -8, -56, -40,
	-216, -200, -248, -232, -152, -136, -184, -168,
	-1376, -1312, -1504, -1440, -1120, -1056, -1248, -1184,
	-1888, -1824, -2016, -1952, -1632, -1568, -1760, -1696,
	-688, -656, -752, -720, -560, -528, -624, -592,
	-944, -912, -1008, -976, -816, -784, -880, -848,
	5504, 5248, 6016, 5760, 4480, 4224, 4992, 4736,
	7552, 7296, 8064, 7808, 6528, 6272, 7040, 6784,
	2752, 2624, 3008, 2880, 2240, 2112, 2496, 2368,
	3776, 3648, 4032, 3904, 3264, 3136, 3520, 3392,
	22016, 20992, 24064, 23040, 17920, 16896, 19968, 18944,
	30208, 29184, 32256, 31232, 26112, 25088, 28160, 27136,
	11008, 10496, 12032, 11520, 8960, 8448, 9984, 9472,
	15104, 14592, 16128, 15616, 13056, 12544, 14080, 13568,
	344, 328, 376, 360, 280, 264, 312, 296,
	472, 456, 504, 488, 408, 392, 440, 424,
	88, 72, 120, 104, 24, 8, 56, 40,
	216, 200, 248, 232, 152, 136, 184, 168,
	1376, 1312, 1504, 1440, 1120, 1056, 1248, 1184,
	1888, 1824, 2016, 1952, 1632, 1568, 1760, 1696,
	688, 656, 752, 720, 560, 528, 624, 592,
	944, 912, 1008, 976, 816, 784, 880, 848
	}; 
    
	public static short alaw2linear( byte ulawbyte) 
	{ 
		return a2l[ulawbyte & 0xFF];
	}
}//end class LawEncoding
