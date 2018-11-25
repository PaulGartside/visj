////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 11 Feb 2017 Paul J. Gartside                                 //
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

import java.util.ArrayList;
import java.util.Deque;

interface VisIF
{
  static final char ESC =  27; // Escape
  static final int BE_FILE    = 0;    // Buffer editor file
  static final int HELP_FILE  = 1;    // Help          file
  static final int MSG_FILE   = 2;    // Message       file
  static final int SHELL_FILE = 3;    // Command Shell file
  static final int COLON_FILE = 4;    // Colon command file
  static final int SLASH_FILE = 5;    // Slash command file
  static final int USER_FILE  = 6;    // First user file 

  static final String  EDIT_BUF_NAME = "BUFFER_EDITOR";
  static final String  HELP_BUF_NAME = "VIS_HELP";
  static final String  MSG__BUF_NAME = "MESSAGE_BUFFER";
  static final String SHELL_BUF_NAME = "SHELL_BUFFER";
  static final String COLON_BUF_NAME = "COLON_BUFFER";
  static final String SLASH_BUF_NAME = "SLASH_BUFFER";

  static final int MAX_WINS = 8;  // Maximum number of window panes

  View CV();

  void    UpdateViewsOfFile( final FileBuf fb );
  boolean Update_Change_Statuses();
  void    CmdLineMessage( String msg );
  boolean GoToBuffer_Fname( String fname );
  void    Window_Message( String msg );
  boolean Is_BE_FILE( final FileBuf fb );
  boolean File_Is_Displayed( String full_fname );
  void    ReleaseFileName( String full_fname );
  boolean HaveFile( String file_name, Ptr_Int file_index );
  void    Add_FileBuf_2_Lists_Create_Views( FileBuf fb, String fname );
  void    Give();

  int  Curr_FileNum();
  View GetView_Win( final int w );
//View GetView_WinPrev( final int w, final int prev );
//int  GetWinNum_Of_View( final View rV );
  boolean Diff_By_File_Indexes( View cV, int c_file_idx
                              , View oV, int o_file_idx );
  boolean NotHaveFileAddFile( String pname );

  void Handle_SemiColon();
  void Handle_Slash_GotPattern( final String  pattern
                              , final boolean MOVE_TO_FIRST_PATTERN );

  ConsoleIF       get_Console();
  FileBuf         get_FileBuf( final int file_num );
  FileBuf         get_FileBuf( String file_name );
  int             get_num_wins();
  String          get_regex();
  Deque<Thread>   get_states();
  char            get_fast_char();
  void            set_fast_char( final char C );
  ArrayList<Line> get_reg();
  Paste_Mode      get_paste_mode();
  void            set_paste_mode( final Paste_Mode paste_mode );
  boolean         get_run_mode();
  void            set_run_mode( final boolean mode );
  String          get_cwd();
  void            set_cmd( String cmd );
  boolean         get_diff_mode();
  Diff            get_diff();
  boolean         get_sort_by_time();
}

