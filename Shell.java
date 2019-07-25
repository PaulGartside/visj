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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;

// Vis helper class to run shell commands:
//
class Shell
{
  Shell( VisIF vis, ConsoleIF console, StringBuilder sb )
  {
    m_vis     = vis;
    m_console = console;
    m_sh_fb   = m_vis.CV().m_fb;
    m_sb      = sb;
  }

  void Run()
  {
    m_sh_view = m_vis.CV();

    boolean ok = Get_Shell_Cmd();

    if( ok )
    {
      Run_Shell_Cmd();
    }
  }

  // Returns shell command or null
  boolean Get_Shell_Cmd()
  {
    int LAST_LINE = m_sh_fb.NumLines() - 1;

    if( 0 <= LAST_LINE )
    {
      // Something in COMMAND_BUFFER so continue:
      final int FIRST_LINE = Get_Shell_Cmd_First_Line();
 
      if( FIRST_LINE <= LAST_LINE )
      {
        return Get_Shell_Cmd_String( FIRST_LINE, LAST_LINE );
      }
    }
    return false;
  }

  int Get_Shell_Cmd_First_Line()
  {
    // Find first line of command to run.
    // First line is line after first line in which the first non-space character is #
    int first_line = 0;
    int LAST_LINE = m_sh_fb.NumLines() - 1;

    boolean found_first_line = false;
    for( int l=LAST_LINE; !found_first_line && 0<=l; l-- )
    {
      final int LL = m_sh_fb.LineLen( l );
 
      for( int p=0; !found_first_line && p<LL; p++ )
      {
        final char C = m_sh_fb.Get( l, p );
        if( Utils.IsSpace( C ) ) ;
        else if( '#' == C )
        {
          found_first_line = true;
          first_line = l+1;
        }
        else {
          // Non-white space, non-#, continue on previous line
          break;
        }
      }
    }
    return first_line;
  }

  boolean Get_Shell_Cmd_String( final int FIRST_LINE
                              , final int  LAST_LINE )
  {
    // At least one command line, so continue:
    m_sb.setLength( 0 );
    // Concatenate all command lines into String cmd:
    for( int k=FIRST_LINE; k<=LAST_LINE; k++ )
    {
      final int LL = m_sh_fb.LineLen( k );
      for( int p=0; p<LL; p++ )
      {
        final char C = m_sh_fb.Get( k, p );
        if( C == '#' ) break; //< Ignore # to end of line
        m_sb.append( C );
      }
      // Add a space between concatenated lines:
      if( 0<LL && k<LAST_LINE ) m_sb.append(' ');
    }
    Utils.Trim( m_sb ); //< Remove leading and ending spaces

    if( 0 < m_sb.length() )
    {
      m_sh_cmd = m_sb.toString();
      return true;
    }
    return false;
  }

  // Returns true if m_sh_cmd was run:
  void Run_Shell_Cmd()
  {
    String divider = "########################################";
    m_sh_fb.PushLine( divider );

    m_sh_cmd_list = Get_Shell_Cmd_List( m_sh_cmd );

    m_vis.set_run_mode( true );

    // Move cursor to bottom of file
    final int NUM_LINES = m_sh_fb.NumLines();
    m_sh_view.GoToCrsPos_NoWrite( NUM_LINES-1, 0 );

    m_sh_fb.Update();

    // Start m_sh_cmd running:
    m_vis.get_states().addFirst( m_run_sh_st );
  }

  ArrayList<String> Get_Shell_Cmd_List( String cmd )
  {
    ArrayList<String> cmd_list = new ArrayList<>();
    StringBuilder     cmd_tok  = new StringBuilder();
    boolean           in_single_quote = false;

    for( int k=0; k<cmd.length(); k++ )
    {
      final char c = cmd.charAt( k );

      if( !in_single_quote )
      {
        if( '\'' == c )
        {
          if( 0 < cmd_tok.length() )
          {
            cmd_list.add( cmd_tok.toString() );
            cmd_tok.setLength( 0 );
          }
          in_single_quote = true;
        }
        else if( Utils.IsSpace( c ) )
        {
          if( 0 < cmd_tok.length() )
          {
            cmd_list.add( cmd_tok.toString() );
            cmd_tok.setLength( 0 );
          }
        }
        else {
          cmd_tok.append( c );
        }
      }
      else { // In single quote. Keep going until ending single quote is reached.
        if( '\'' == c )
        {
          if( 0 < cmd_tok.length() )
          {
            cmd_list.add( cmd_tok.toString() );
            cmd_tok.setLength( 0 );
          }
          in_single_quote = false;
        }
        else {
          cmd_tok.append( c );
        }
      }
    }
    // If anything is left over, add it to cmd_list:
    if( 0 < cmd_tok.length() )
    {
      cmd_list.add( cmd_tok.toString() );
      cmd_tok.setLength( 0 );
    }
    return cmd_list;
  }

  void run_sh_st()
  {
    m_vis.get_states().removeFirst(); //< Pop m_run_sh_st() off state stack

    ProcessBuilder pb = new ProcessBuilder( m_sh_cmd_list );
    pb.redirectErrorStream( true ); //< Redirect process stderr to stdout
    pb.directory( new File( m_vis.get_cwd() ) ); //< Set Process directory to m_cwd
    try {
      m_sh_proc = pb.start();
    }
    catch( Exception e )
    {
      m_vis.CmdLineMessage("Could not run: "+ m_sh_cmd );
      m_vis.set_run_mode( false );
      m_sh_view.PrintCursor();
      return;
    }
    InputStream is = m_sh_proc.getInputStream();
    if( null == is ) {
      m_vis.CmdLineMessage("Could not get input stream for: "+ m_sh_cmd );
      if( m_sh_proc.isAlive() ) m_sh_proc.destroy();
      m_vis.set_run_mode( false );
      m_sh_view.PrintCursor();
    }
    else { // Success, keep going:
      m_sh_is = new BufferedInputStream( is, 512 );
      m_vis.get_states().addFirst( m_run_sh_wait ); //< Go into wait state
      m_sh_fb.PushLine();
      m_sh_T1 = System.currentTimeMillis();
    }
  }

  void run_sh_wait()
  {
    boolean done = false;
    try {
      while( !done && 0<m_sh_is.available() )
      {
        final int C = m_sh_is.read();
        if     (  -1  == C ) done = true;
        else if( '\n' != C ) m_sh_fb.PushChar( (char)C );
        else { //'\n' == C
          m_sh_fb.PushLine(); 
          long T2 = System.currentTimeMillis();
          if( 500 < (T2-m_sh_T1) ) {
            m_sh_T1 = T2;
            // Move cursor to bottom of file
            final int NUM_LINES = m_sh_fb.NumLines();
            m_sh_view.GoToCrsPos_NoWrite( NUM_LINES-1, 0 );
            m_sh_fb.Update();
          }
        }
      }
      if( ! m_sh_proc.isAlive() )
      {
        done = true;
      }
      if( !done && 0 < m_console.KeysIn() )
      {
        final int ca = m_console.GetKey();
        if( ca == m_console.CTRL_C )
        {
          m_sh_proc.destroy();
          done = true;
          m_sh_fb.PushLine("^C");
        }
      }
    }
    catch( IOException e ) { done = true; }

    if( done ) {
      m_vis.get_states().removeFirst(); //< Pop m_run_sh_wait() off state stack
      m_vis.get_states().addFirst( m_run_sh_done ); //< Go into done state
    }
  }

  void run_sh_done()
  {
    // Get m_sh_cmd exit status:
    if( ! m_sh_proc.isAlive() )
    {
      m_vis.get_states().removeFirst(); //< Pop m_run_sh_done() off state stack

      final int exit_val = m_sh_proc.exitValue();

      // Append exit_msg to Shell buffer:
      final int LLL = m_sh_fb.LineLen( m_sh_fb.NumLines()-1 );
      String exit_str = "Exit_Value="+ exit_val;
      if( 0<LLL ) m_sh_fb.PushLine( exit_str );
      else {
        m_sh_fb.AppendLineToLine( m_sh_fb.NumLines()-1, exit_str );
      }
      m_vis.set_run_mode( false );

      String divider = "########################################";
      m_sh_fb.PushLine( divider );
      m_sh_fb.PushLine();

      // Move cursor to bottom of file
      final int NUM_LINES = m_sh_fb.NumLines();

      m_sh_view.GoToCrsPos_NoWrite( NUM_LINES-1, 0 );
      m_sh_fb.Update();
    }
  }
  VisIF             m_vis;
  ConsoleIF         m_console;
  View              m_sh_view;
  FileBuf           m_sh_fb;
  StringBuilder     m_sb;
  String            m_sh_cmd;
  ArrayList<String> m_sh_cmd_list;
  Process           m_sh_proc;
  InputStream       m_sh_is;
  Long              m_sh_T1;
  Thread            m_run_sh_st  = new Thread() { public void run() { run_sh_st  (); m_vis.Give(); } };
  Thread            m_run_sh_wait= new Thread() { public void run() { run_sh_wait(); m_vis.Give(); } };
  Thread            m_run_sh_done= new Thread() { public void run() { run_sh_done(); m_vis.Give(); } };
}

