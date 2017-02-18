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

interface ConsoleIF
{
  static final char CTRL_C =   3;
  static final char BS     =   8; // Backspace
  static final char ESC    =  27; // Escape
  static final char DEL    = 127; // Delete

  final int     FONT_SIZE = 17;
  final int MIN_FONT_SIZE = 8;

  int Num_Rows();
  int Num_Cols();

  boolean Update();
  int     KeysIn();

  void    Set_Crs_Cell( final int ROW, final int COL );
  void    Set( final int ROW, final int COL, final char C, final Style S );
  void    SetS( final int ROW, final int COL, final String str, final Style S );
  char    GetKey();
  void    copy_vis_buf_2_dot_buf();
  void    copy_paste_buf_2_system_clipboard();
  void    copy_system_clipboard_2_paste_buf();

  StringBuilder get_dot_buf();
  boolean       get_from_dot_buf();
  void          set_save_2_vis_buf( final boolean save );
}

