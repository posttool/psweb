package com.pagesociety.util;

public class HexUtil 
{
	// table to convert a nibble to a hex char.
	static char[] HEX_CHAR = {'0' , '1' , '2' , '3' ,'4' , '5' , '6' , '7' ,'8' , '9', 
							'a' , 'b' , 'c' , 'd' , 'e' , 'f' };
	
	// Fast convert a byte array to a hex string
	// with possible leading zero.
	
	public static String toHexString( byte[] b ) 
	{
		StringBuilder sb = new StringBuilder( b.length * 2 );
		for ( int i=0 ; i<b.length ; i++ ) 
		{
			// look up high nibble char
			sb.append( HEX_CHAR [ ( b[i] & 0xf0 ) >>> 4 ] );
			//look up low nibble char
			sb.append( HEX_CHAR [ b[i] & 0x0f ] );
	}
		return sb.toString() ;
	}
	
	public static byte[] fromHexString( String s ) 
	{
		int stringLength = s.length() ;
		if ( (stringLength & 0x1) != 0 ) 
			throw new IllegalArgumentException( "fromHexString requires an even number of hex characters" );
	
		byte[] b = new byte[ stringLength / 2 ];
	
		for ( int i=0 ,j= 0; i< stringLength; i+= 2,j ++ ) 
		{
			int high= char_to_nibble(s.charAt( i ));
			int low = char_to_nibble( s.charAt( i+1 ) );
			b[ j ] = (byte ) ( ( high << 4 ) | low );
		}
		return b;
	}
	
	private static int char_to_nibble( char c ) 
	{
		if ( '0' <= c && c <= '9' ) 
			return c - '0' ;
		else if ( 'a' <= c && c <= 'f' ) 
			return c - 'a' + 0xa ;
		else if ( 'A' <= c && c <= 'F' ) 
			return c - 'A' + 0xa ;
		else 
			throw new IllegalArgumentException( "Invalid hex character: " + c ) ;
	}

}
