////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 02 Nov 2019 Paul J. Gartside                                 //
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

import java.lang.Math;

class Image_data
{
  int sx;
  int sy;
  int zoom = 100; // Percent zoom
  int zoom_index = 13;

  void inc_zoom()
  {
    zoom_index++;

    // Dont let zoom_index go past end of m_zoom_percents:
    zoom_index = Math.min( zoom_index, m_zoom_percents.length-1 );

    zoom = m_zoom_percents[ zoom_index ];
  }
  void dec_zoom()
  {
    zoom_index--;

    // Dont let zoom_index go below zero:
    zoom_index = Math.max( zoom_index, 0 );

    zoom = m_zoom_percents[ zoom_index ];
  }
  int[] m_zoom_percents =
  {
  //  0,   1,   2,   3,   4,   5,   6,   7,   8,   9
  //  5,  10,  15,  20,  25,  33,  40,  50,  58,  65,
  // 75,  83,  91, 100, 110, 121, 135, 150, 165, 182,
  //200, 220, 250, 275, 300, 330, 363, 400, 440, 500,
  //550, 600, 660, 725, 800, 880, 1000
      5,  10,  15,  20,  25,  33,  40,  50,  60,  70,
     80,  90, 100, 110, 125, 150, 175, 200, 225, 250,
    275, 300, 350, 400, 450, 500, 550, 600, 650, 700,
    800, 900, 1000
  };
}

