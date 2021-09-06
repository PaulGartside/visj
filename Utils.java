////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 07 Sep 2015 Paul J. Gartside                                 //
////////////////////////////////////////////////////////////////////////////////
// Permission is hereby granted, free of charge, to any person obtaining a    //
// copy of this software and associated documentation files (the "Software"), //
// to deal in the Software without restriction, including without  limitation //
// the rights to use, copy, modify, merge, publish, distribute, sublicense,   //
// and/or sell copies of the Software, and to permit persons to whom the      //
// Software is furnished to do so, subject to the following conditions:       //
//                                                                            //
// The above copyright notice and this permission notice shall be included in //
// all copies or substantial portions of the Software.                        //
//                                                                            //
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR //
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   //
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    //
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER //
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    //
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        //
// DEALINGS IN THE SOFTWARE.                                                  //
////////////////////////////////////////////////////////////////////////////////

import java.lang.AssertionError;
import java.io.IOException;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Utils
{
  public static
  void Log( String msg )
  {
    System.out.println( msg );
  }
  public static
  int LLM1( final int LL )
  {
    return 0 < LL ? LL-1 : 0;
  }
  public static
  OS_Type Get_OS_Type()
  {
    if( !m_determined_os )
    {
      String os = System.getProperty("os.name");

      if( null != os )
      {
        if( os.equals    ("Mac OS X") ) m_os_type = OS_Type.OSX;
        if( os.startsWith("Windows" ) ) m_os_type = OS_Type.Windows;
      }
      m_determined_os = true;
    }
    return m_os_type;
  }
  public static
  boolean line_start_or_prev_C_non_ident( final Line line
                                        , final int p )
  {
    if( 0==p ) return true; // p is on line start

    // At this point 0 < p
    final char C = line.charAt( p-1 );
    if( !Character.isLetterOrDigit( C ) && C!='_' )
    {
      // Previous C is not an identifier
      return true;
    }
    // On identifier
    return false;
  }

  public static
  boolean line_end_or_non_ident( final Line line
                               , final int LL
                               , final int p )
  {
    if( p == LL-1 ) return true; // p is on line end

    if( p < LL-1 )
    {
      // At this point p should always be less than LL-1,
      // but put the check in above just to be safe.
      // The check above could also be implemented as an ASSERT.
      final char C = line.charAt( p+1 );
      if( !Character.isLetterOrDigit( C ) && C != '_' )
      {
        // C is not an identifier
        return true;
      }
    }
    // C is an identifier
    return false;
  }
  public static
  boolean IsUpper( final char C )
  {
    return Character.isUpperCase( C );
  }
  public static
  boolean IsLower( final char C )
  {
    return Character.isLowerCase( C );
  }
  public static
  char ToUpper( final char C )
  {
    return Character.toUpperCase( C );
  }
  public static
  char ToLower( final char C )
  {
    return Character.toLowerCase( C );
  }
  public static
  boolean IsIdent( final char C )
  {
    return Character.isLetterOrDigit( C ) || C == '_';
  }
  public static
  boolean IsWord_Ident( final char C )
  {
    return Character.isLetterOrDigit( C ) || C == '_';
  }
  public static
  boolean IsWord_NonIdent( final char C )
  {
    return !IsSpace( C ) && !IsWord_Ident( C );
  }
  public static
  boolean IsHexDigit( final char C )
  {
    return Character.isDigit( C )
        || C == 'A' || C == 'a'
        || C == 'B' || C == 'b'
        || C == 'C' || C == 'c'
        || C == 'D' || C == 'd'
        || C == 'E' || C == 'e'
        || C == 'F' || C == 'f';
  }
  public static
  boolean IsXML_Ident( final char C )
  {
    return Character.isLetterOrDigit( C )
        || C == '_'
        || C == '-'
        || C == '.'
        || C == ':';
  }
  public static
  boolean IsJavaRegexMetaChar( final char C )
  {
    return C == '<' || C == '(' || C == '[' || C == '{'
        || C == '\\'|| C == '^' || C == '-' || C == '='
        || C == '$' || C == '!' || C == '|' || C == ']'
        || C == '}' || C == ')' || C == '?' || C == '*'
        || C == '+' || C == '.' || C == '>';
  }
  public static
  boolean IsEndOfLineDelim( final int C )
  {
    if( C == '\n' ) return true;
    if( C == '\r' ) return true;
    return false;
  }
  public static
  char MS_Hex_Digit( final char Ci )
  {
    final int I = (Ci >> 4) & 0xF;

    char Co = '*';

    switch( I )
    {
    case  0: Co = '0'; break;
    case  1: Co = '1'; break;
    case  2: Co = '2'; break;
    case  3: Co = '3'; break;
    case  4: Co = '4'; break;
    case  5: Co = '5'; break;
    case  6: Co = '6'; break;
    case  7: Co = '7'; break;
    case  8: Co = '8'; break;
    case  9: Co = '9'; break;
    case 10: Co = 'A'; break;
    case 11: Co = 'B'; break;
    case 12: Co = 'C'; break;
    case 13: Co = 'D'; break;
    case 14: Co = 'E'; break;
    case 15: Co = 'F'; break;
    }
    return Co;
  }
  public static
  char LS_Hex_Digit( final char Ci )
  {
    final int I = Ci & 0xF;

    char Co = '*';

    switch( I )
    {
    case  0: Co = '0'; break;
    case  1: Co = '1'; break;
    case  2: Co = '2'; break;
    case  3: Co = '3'; break;
    case  4: Co = '4'; break;
    case  5: Co = '5'; break;
    case  6: Co = '6'; break;
    case  7: Co = '7'; break;
    case  8: Co = '8'; break;
    case  9: Co = '9'; break;
    case 10: Co = 'A'; break;
    case 11: Co = 'B'; break;
    case 12: Co = 'C'; break;
    case 13: Co = 'D'; break;
    case 14: Co = 'E'; break;
    case 15: Co = 'F'; break;
    }
    return Co;
  }
  public static
  int Hex_Char_2_Int_Val( final char C )
  {
    int I = 0;

    switch( C )
    {
    case '0': I = 0; break;
    case '1': I = 1; break;
    case '2': I = 2; break;
    case '3': I = 3; break;
    case '4': I = 4; break;
    case '5': I = 5; break;
    case '6': I = 6; break;
    case '7': I = 7; break;
    case '8': I = 8; break;
    case '9': I = 9; break;
    case 'A':
    case 'a': I = 10; break;
    case 'B':
    case 'b': I = 11; break;
    case 'C':
    case 'c': I = 12; break;
    case 'D':
    case 'd': I = 13; break;
    case 'E':
    case 'e': I = 14; break;
    case 'F':
    case 'f': I = 15; break;
    }
    return I;
  }
  public static
  char Hex_Chars_2_Byte( final char C1, final char C2 )
  {
    return (char)( Hex_Char_2_Int_Val( C1 )*16
                 + Hex_Char_2_Int_Val( C2 ) );
  }
  public static
  void Sleep( final int ms )
  {
    try { Thread.sleep( ms ); } catch( Exception e ) {}
  }
  public static
  void RemoveSpaces( StringBuilder sb )
  {
    for( int k=0; k<sb.length(); k++ )
    {
      if( IsSpace( sb.charAt( k ) ) )
      {
        sb.deleteCharAt( k );
        k--; // Since we just shifted down over current char,
      }      // re-check current char
    }
  }
  // Remove leading and trailing white space
  public static
  void Trim( StringBuilder sb )
  {
    Trim_Beg( sb );
    Trim_End( sb );
  }
  // Remove leading white space
  public static
  void Trim_Beg( StringBuilder sb )
  {
    boolean done = false;
    for( int k=0; !done && k<sb.length(); k++ )
    {
      if( IsSpace( sb.charAt( k ) ) )
      {
        sb.deleteCharAt( k );
        k--; // Since we just shifted down over current char,
      }      // re-check current char
      else done = true;
    }
  }
  // Remove trailing white space
  public static
  void Trim_End( StringBuilder sb )
  {
    final int LEN = sb.length();
    if( 0 < LEN )
    {
      boolean done = false;
      for( int k=LEN-1; !done && -1<k; k-- )
      {
        if( IsSpace( sb.charAt( k ) ) )
        {
          sb.deleteCharAt( k );
        }
        else done = true;
      }
    }
  }
  public static
  boolean IsSpace( final char c )
  {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
  }
  public static
  boolean NotSpace( final char c )
  {
    return !IsSpace( c );
  }
  public static
  boolean IsFileNameChar( final int c )
  {
    return      '$' == c
        ||      '+' == c
        ||      '-' == c
        ||      '.' == c
        || DIR_DELIM== c
        ||    ( '0' <= c && c <= '9' )
        ||    ( 'A' <= c && c <= 'Z' )
        ||      '_' == c
        ||    ( 'a' <= c && c <= 'z' )
        ||      '~' == c
        ||      ' ' == c // Allow spaces in file names
        ;
  }

  public static
  char Byte_2_Char( final byte B )
  {
    final int  I = B < 0 ? B + 256 : B;
    final char C = (char)I;
    return C;
  }

  public static
  String GetCWD()
  {
    FileSystem fs = FileSystems.getDefault();

    String cwd = fs.getPath(".").toAbsolutePath().normalize().toString();

    return Append_Dir_Delim( cwd );
  }

  public static
  Path GetPath( String fname )
  {
    Path path = null;
    try {
      path = FileSystems.getDefault().getPath( fname );
    }
    catch( Exception e ) {
    }
    return path;
  }

  public static
  boolean IsDirectory( String fname )
  {
    boolean is_dir = false;

    Path path = GetPath( fname );

    if( null != path )
    {
      is_dir = Files.isDirectory( path );
    }
    return is_dir;
  }

  public static
  boolean IsRegularFile( String fname )
  {
    boolean is_reg = false;

    Path path = GetPath( fname );

    if( null != path )
    {
      is_reg = Files.isRegularFile( path );
    }
    return is_reg;
  }

  public static
  String NormalizePname( String pname )
  {
    Path path = GetPath( pname );

    String normalized_pname = pname;
    try {
      normalized_pname = path.normalize().toString();
    }
    catch( Exception e )
    {
      // Return un-normalized pname
    }
    return normalized_pname;
  }

  // Find full file name relative to directory of this java process.
  // Find full file name relative to directory vis was started in.
  public static
  String FindFullFileName_Process( String fname )
  {
    FileSystem fs    = FileSystems.getDefault();
    Path       fpath = fs.getPath( fname );
    File       file  = fpath.toAbsolutePath().normalize().toFile();

    String full_fname = file.toString();

    if( file.isDirectory() )
    {
      if( false == full_fname.endsWith( DIR_DELIM_STR ) )
      {
        full_fname += DIR_DELIM_STR;
      }
    }
    return full_fname;
  }
  public static
  String Append_Dir_Delim( String dir )
  {
    if( ! dir.endsWith( DIR_DELIM_STR ) )
    {
      dir += DIR_DELIM_STR;
    }
    return dir;
  }
//String FindFullFileName( String fname )
//{
//  return Paths.get( fname ).toAbsolutePath().normalize().toString();
//}
  // Find full file name relative to path.
  public static
  String FindFullFileName_Path( String path, String fname )
  {
    try {
      FileSystem fs    = FileSystems.getDefault();
      Path       fpath = fs.getPath( path, fname );

      return Path_2_String( fpath );
    }
    catch( Exception e )
    {
      // Path.getPath() can throw InvalidPathException
    }
    return fname;
  }

  // If fpath is a directory, makes sure slash is at the end of the String
  public static
  String Path_2_String( Path fpath )
  {
    File   file       = fpath.toFile();
    String full_fname = file.toString();

    if( Files.isDirectory( fpath ) )
    {
      full_fname = Append_Dir_Delim( full_fname );
    }
    return full_fname;
  }
  // Given path, returns fname head
  public static
  String Pname_2_Fname( String pname )
  {
    final int last_slash_idx = pname.lastIndexOf( Utils.DIR_DELIM );

    if( 0 <= last_slash_idx )
    {
      // Return everything after last slash.
      // If last slash is at the end of pname, return empty string.
      return pname.substring( last_slash_idx+1 );
    }
    // No tail, all head:
    return pname;
  }
  // Given full path, returns fname directory, including last slash
  public static
  String Pname_2_Dname( String pname )
  {
    final int last_slash_idx = pname.lastIndexOf( Utils.DIR_DELIM );

    if( 0 <= last_slash_idx )
    {
      // Return everything before last slash:
      return pname.substring( 0, last_slash_idx+1 );
    }
    // No tail, all head:
    return "";
  }

  // Return true if string was converted to int, else false
  public static
  boolean String_2_Int( String s, Ptr_Int pi )
  {
    boolean ok = true;
    try {
      // This will throw java.lang.NumberFormatException if the String
      // given to it does not contain a number:
      pi.val = Integer.valueOf( s );
    }
    catch( Exception e )
    {
      ok = false;
    }
    return ok;
  }
  public static
  StringBuilder EnvKeys2Vals( StringBuilder sb_io )
  {
    // Replace ~/ with $HOME/
    if( 1<sb_io.length() && '~' == sb_io.charAt(0) && '/' == sb_io.charAt(1) )
    {
      sb_io.replace( 0, 0, "$HOME" );
    }
    StringBuilder env_key_sb = new StringBuilder();

    for( int k=0; k<sb_io.length()-1; k++ )
    {
      if( '$' == sb_io.charAt(k) )
      {
        int k_st = k;
        env_key_sb.setLength( 0 );
        for( k++; k<sb_io.length() && IsWord_Ident( sb_io.charAt(k) ); k++ )
        {
          env_key_sb.append( sb_io.charAt(k) );
        }
        if( 0<env_key_sb.length() )
        {
          final String env_val_s = System.getenv( env_key_sb.toString() );
          if( null != env_val_s )
          {
            env_key_sb.insert( 0, '$' );

            String s = sb_io.toString();
            String s2 = s.replace( env_key_sb, env_val_s );
            sb_io = new StringBuilder( s2 );
            // In case env_val_s is shorter than env_key_sb,
            k = k_st; // backup to where we started
          }
        }
      }
    }
    return sb_io;
  }
  public static
  void Swap( Ptr_Int A, Ptr_Int B )
  {
    int T = B.val;
    B.val = A.val;
    A.val = T;
  }

  public static
  long ModificationTime( Path path )
  {
    long mod_time = 0;
    try {
      mod_time = Files.getLastModifiedTime( path ).toMillis();
    }
    catch( IOException e )
    {
    }
    return mod_time;
  }

  // Given full path names of files, return true if the two
  // files are the same
  public static
  boolean Files_Are_Same( String fname_s, String fname_l )
  {
    boolean files_are_same = false;

    Path path_s = FileSystems.getDefault().getPath( fname_s );
    Path path_l = FileSystems.getDefault().getPath( fname_l );

    if( Files.isRegularFile( path_s )
     && Files.isRegularFile( path_l ) )
    {
      File file_s = path_s.toFile();
      File file_l = path_l.toFile();

      final long len_s = file_s.length();
      final long len_l = file_l.length();

      if( len_s == len_l )
      {
        try {
          FileInputStream     fis_s = new FileInputStream( file_s );
          BufferedInputStream bis_s = new BufferedInputStream( fis_s, 512 );

          FileInputStream     fis_l = new FileInputStream( file_l );
          BufferedInputStream bis_l = new BufferedInputStream( fis_l, 512 );

          for( boolean done = false; !done; )
          {
            final int C_s = bis_s.read();
            final int C_l = bis_l.read();

            if( -1 == C_s && -1 == C_l )
            {
              done = true;
              files_are_same = true;
            }
            else if( C_s != C_l )
            {
              done = true; // Files are different
            }
            else {
              // Keep reading till end of files or difference
            }
          }
          fis_s.close();
          fis_l.close();
        }
        catch( Exception e ) {}
      }
    }
    return files_are_same;
  }

  // Given FileBuf of files, return true if the two files are the same
  public static
  boolean Files_Are_Same( FileBuf fb_s, FileBuf fb_l )
  {
    boolean files_are_same = false;

    if( fb_s.m_isRegular && fb_l.m_isRegular )
    {
      final int num_lines_s = fb_s.NumLines();
      final int num_lines_l = fb_l.NumLines();

      if( num_lines_s == num_lines_l )
      {
        files_are_same = true;

        for( int k=0; files_are_same && k<num_lines_s; k++ )
        {
          Line l_s = fb_s.GetLine( k );
          Line l_l = fb_l.GetLine( k );

          if( !l_s.equals( l_l ) )
          {
            files_are_same = false;
          }
        }
      }
    }
    return files_are_same;
  }

  public static
  boolean Have_Regex_In_File( Pattern pattern
                            , String  fname
                            , Line    line_buf )
  {
    Path path = FileSystems.getDefault().getPath( fname );

    if( Files.isRegularFile( path ) )
    {
      File file = path.toFile();

      try {
        FileInputStream     fis = new FileInputStream( file );
        BufferedInputStream bis = new BufferedInputStream( fis, 512 );

        line_buf.setLength( 0 );

        for( boolean done = false; !done; )
        {
          final int C = bis.read();
          if( -1 == C )
          {
            done = true;
          }
          else if( '\n' == C )
          {
            Matcher matcher = pattern.matcher( line_buf.toString() );

            if( matcher.find() ) return true;
            else {
              line_buf.setLength( 0 );
            }
          }
          else {
            line_buf.append_c( (char)C );
          }
        }
        fis.close();
      }
      catch( Exception e ) {}
    }
    return false;
  }

  public static
  void Assert( final boolean condition, String msg )
  {
    if( !condition ) throw new AssertionError( msg, null );
  }

  static final String DIR_DELIM_STR = File.separator;
  static final char   DIR_DELIM     = File.separatorChar;
  static private boolean m_determined_os = false;
  static private OS_Type m_os_type       = OS_Type.Unknown;
}

