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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

class Utils
{
  public static
  void Log( String msg )
  {
    System.out.println( msg );
  }
  public static
  OS_Type Get_OS_Type()
  {
    if( !m_determined_os )
    {
      String os = System.getenv("OS");

      if( null != os )
      {
        if( os.equals("Windows_NT") ) m_os_type = OS_Type.Windows;
      }
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
      final char c = line.charAt( p+1 );
      if( !Character.isLetterOrDigit( c ) && c != '_' )
      {
        // c is not an identifier
        return true;
      }
    }
    // c is an identifier
    return false;
  }
  public static
  boolean IsUpper( final char c )
  {
    return Character.isUpperCase( c );
  }
  public static
  boolean IsLower( final char c )
  {
    return Character.isLowerCase( c );
  }
  public static
  char ToUpper( final char c )
  {
    return Character.toUpperCase( c );
  }
  public static
  char ToLower( final char c )
  {
    return Character.toLowerCase( c );
  }
  public static
  boolean IsIdent( final char c )
  {
    return Character.isLetterOrDigit( c ) || c == '_';
  }
  public static
  boolean IsWord_Ident( final char c )
  {
    return Character.isLetterOrDigit( c ) || c == '_';
  }
  public static
  boolean IsWord_NonIdent( final char c )
  {
    return !IsSpace( c ) && !IsWord_Ident( c );
  }
  public static
  boolean IsHexDigit( final char c )
  {
    return Character.isDigit( c )
        || c == 'A' || c == 'a'
        || c == 'B' || c == 'b'
        || c == 'C' || c == 'c'
        || c == 'D' || c == 'd'
        || c == 'E' || c == 'e'
        || c == 'F' || c == 'f';
  }
  public static
  boolean IsXML_Ident( final char c )
  {
    return Character.isLetterOrDigit( c )
        || c == '_'
        || c == '-'
        || c == '.'
        || c == ':';
  }
//public static
//boolean IsGraph( final char c )
//{
//  return true;
//}

  public static
  boolean IsEndOfLineDelim( final int c )
  {
    if( c == '\n' ) return true;
    if( c == '\r' ) return true;
    return false;
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
        ;
  }

  public static
  String GetCWD()
  {
    FileSystem fs = FileSystems.getDefault();
    String cwd = fs.getPath(".").toAbsolutePath().normalize().toString();
    if( !cwd.endsWith( DIR_DELIM_STR ) ) cwd +=  DIR_DELIM_STR ;
    return cwd;
  }
//public static
//String GetCWD()
//{
//  FileSystem fs = FileSystems.getDefault();
//  return fs.getPath(".").toAbsolutePath().normalize().toString();
//}
//public static
//String GetCWD()
//{
////return Paths.get("").toAbsolutePath().toString();
//  return Paths.get(".").toAbsolutePath().normalize().toString();
//}

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
      if( false == full_fname.endsWith( Utils.DIR_DELIM_STR ) )
      {
        full_fname += Utils.DIR_DELIM_STR;
      }
    }
    return full_fname;
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
      File       file  = fpath.toFile();
    //File file = Paths.get( path, fname ).toFile();
 
      String full_fname = file.toString();
 
      if( file.isDirectory() )
      {
        if( false == full_fname.endsWith( Utils.DIR_DELIM_STR ) )
        {
          full_fname += Utils.DIR_DELIM_STR;
        }
      }
      return full_fname;
    }
    catch( Exception e )
    {
      // Path.getPath() can throw InvalidPathException
    }
    return fname;
  }

  // Given full path, returns fname head
  public static
  String FnameHead( String in_fname )
  {
    final int last_slash_idx = in_fname.lastIndexOf( Utils.DIR_DELIM );

    if( 0 <= last_slash_idx )
    {
      // Return everything after last slash:
      return in_fname.substring( last_slash_idx+1 );
    }
    // No tail, all head:
    return in_fname;
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
  void EnvKeys2Vals( Ptr_StringBuilder in_out_fname )
  {
    StringBuilder sb = in_out_fname.val;

    // Replace ~/ with $HOME/
    if( 1<sb.length() && '~' == sb.charAt(0) && '/' == sb.charAt(1) )
    {
      sb.replace( 0, 0, "$HOME" );
    }
    StringBuilder env_key_sb = new StringBuilder();

    for( int k=0; k<sb.length()-1; k++ )
    {
      if( '$' == sb.charAt(k) )
      {
        env_key_sb.setLength( 0 );
        for( k++; k<sb.length() && IsWord_Ident( sb.charAt(k) ); k++ )
        {
          env_key_sb.append( sb.charAt(k) );
        }
        if( 0<env_key_sb.length() )
        {
          final String env_val_s = System.getenv( env_key_sb.toString() );
          if( null != env_val_s )
          {
            env_key_sb.insert( 0, '$' );

            String s = sb.toString();
            String s2 = s.replace( env_key_sb, env_val_s );
            in_out_fname.val = new StringBuilder( s2 );
          }
        }
      }
    }
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
    //System.out.println( "ModificationTime: "+ e );
    }
    return mod_time;
  }

  public static
  void Assert( final boolean condition, String msg )
  {
    if( !condition ) throw new AssertionError( msg, null );
  }

//private static
//char Get_Dir_Delim()
//{
//  char dir_delim_c = '/';
//  String dir_delim_s = System.getProperty("file.separator");
//  if( null != dir_delim_s )
//  {
//    dir_delim_c = dir_delim_s.charAt( 0 );
//  }
//  return dir_delim_c;
//}
//private static
//String Get_Dir_Delim_Str()
//{
//  StringBuilder sb = new StringBuilder();
//
//  sb.append( DIR_DELIM );
//
//  return sb.toString();
//}
  private static
  String Get_Dir_Delim_Str()
  {
    String file_sep_s = System.getProperty("file.separator");

    if( null != file_sep_s )
    {
      return file_sep_s;
    }
    return "/";
  }
  private static
  char Get_Dir_Delim()
  {
    return DIR_DELIM_STR.charAt( 0 );
  }
  static final String DIR_DELIM_STR = Get_Dir_Delim_Str();
  static final char   DIR_DELIM     = Get_Dir_Delim();
  static private boolean m_determined_os = false;
  static private OS_Type m_os_type       = OS_Type.Unknown;
}

