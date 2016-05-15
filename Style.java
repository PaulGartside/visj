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
  NORMAL      ( (char)1 ),
  STATUS      ( (char)2 ),
  BORDER      ( (char)3 ),
  BORDER_HI   ( (char)4 ),
  BANNER      ( (char)5 ),
  STAR        ( (char)6 ),
  COMMENT     ( (char)7 ),
  DEFINE      ( (char)8 ),
  CONST       ( (char)9 ),
  CONTROL     ( (char)10 ),
  VARTYPE     ( (char)11 ),
  VISUAL      ( (char)12 ),
  NONASCII    ( (char)13 ),
  RV_NORMAL   ( (char)14 ),
  RV_STATUS   ( (char)15 ),
  RV_BORDER   ( (char)16 ),
  RV_BORDER_HI( (char)17 ),
  RV_BANNER   ( (char)18 ),
  RV_STAR     ( (char)19 ),
  RV_COMMENT  ( (char)20 ),
  RV_DEFINE   ( (char)21 ),
  RV_CONST    ( (char)22 ),
  RV_CONTROL  ( (char)23 ),
  RV_VARTYPE  ( (char)24 ),
  RV_VISUAL   ( (char)25 ),
  RV_NONASCII ( (char)26 ),
  EMPTY       ( (char)27 ),
  EOF         ( (char)28 ),
  DIFF_DEL    ( (char)29 ),
  DIFF_NORMAL ( (char)30 ),
  DIFF_STAR   ( (char)31 ),
  DIFF_COMMENT( (char)32 ),
  DIFF_DEFINE ( (char)33 ),
  DIFF_CONST  ( (char)34 ),
  DIFF_CONTROL( (char)35 ),
  DIFF_VARTYPE( (char)36 ),
  DIFF_VISUAL ( (char)37 ),
  CURSOR      ( (char)38 ),
  CURSOR_EMPTY( (char)39 ),
  UNKNOWN     ( (char)40 );

  final char val;

  Style( char type )
  {
    val = type;
  }
}

