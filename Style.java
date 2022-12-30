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

enum Style
{
  NORMAL        ( (char) 1 ),
  STATUS        ( (char) 2 ),
  BORDER        ( (char) 3 ),
  BORDER_HI     ( (char) 4 ),
  BANNER        ( (char) 5 ),
  STAR          ( (char) 6 ), // In search pattern
  STAR_IN_F     ( (char) 7 ), // In Search pattern in file
  COMMENT       ( (char) 8 ),
  DEFINE        ( (char) 9 ),
  CONST         ( (char)10 ),
  CONTROL       ( (char)11 ),
  VARTYPE       ( (char)12 ),
  VISUAL        ( (char)13 ),
  NONASCII      ( (char)14 ),
  RV_NORMAL     ( (char)15 ), // Reverse video
  RV_STATUS     ( (char)16 ), // Not used
  RV_BORDER     ( (char)17 ), // Not used
  RV_BORDER_HI  ( (char)18 ), // Not used
  RV_BANNER     ( (char)19 ), // Not used
  RV_STAR       ( (char)20 ),
  RV_STAR_IN_F  ( (char)21 ),
  RV_COMMENT    ( (char)22 ),
  RV_DEFINE     ( (char)23 ),
  RV_CONST      ( (char)24 ),
  RV_CONTROL    ( (char)25 ),
  RV_VARTYPE    ( (char)26 ),
  RV_VISUAL     ( (char)27 ),
  RV_NONASCII   ( (char)28 ),
  EMPTY         ( (char)29 ),
  EOF           ( (char)30 ),
  DIFF_DEL      ( (char)31 ),
  DIFF_NORMAL   ( (char)32 ),
  DIFF_STAR     ( (char)33 ),
  DIFF_STAR_IN_F( (char)34 ),
  DIFF_COMMENT  ( (char)35 ),
  DIFF_DEFINE   ( (char)36 ),
  DIFF_CONST    ( (char)37 ),
  DIFF_CONTROL  ( (char)38 ),
  DIFF_VARTYPE  ( (char)39 ),
  DIFF_VISUAL   ( (char)40 ),
  CURSOR        ( (char)41 ),
  CURSOR_EMPTY  ( (char)42 ),
  UNKNOWN       ( (char)43 );

  final char val;

  Style( char type )
  {
    val = type;
  }
}

