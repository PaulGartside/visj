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
//
// --------
// | FULL |
// --------
//
// -------------------------
// |           |           |
// | LEFT_HALF | RITE_HALF |
// |           |           |
// -------------------------
//
// -------------
// | TOP__HALF |
// -------------
// | BOT__HALF |
// -------------
//
// ---------------------------------
// | TOP__LEFT_QTR | TOP__RITE_QTR |
// ---------------------------------
// | BOT__LEFT_QTR | BOT__RITE_QTR |
// ---------------------------------
//
// -------------------------------------------------------
// |          |               |               |          |
// | LEFT_QTR | LEFT_CTR__QTR | RITE_CTR__QTR | RITE_QTR |
// |          |               |               |          |
// -------------------------------------------------------
//
// -------------------------------------------------------------------------
// |               |                   |                   |               |
// | TOP__LEFT_8TH | TOP__LEFT_CTR_8TH | TOP__RITE_CTR_8TH | TOP__RITE_8TH |
// |               |                   |                   |               |
// -------------------------------------------------------------------------
// |               |                   |                   |               |
// | BOT__LEFT_8TH | BOT__LEFT_CTR_8TH | BOT__RITE_CTR_8TH | BOT__RITE_8TH |
// |               |                   |                   |               |
// -------------------------------------------------------------------------
//
enum Tile_Pos
{
  NONE,
  // 2 x 2 tiles:
  FULL,
  LEFT_HALF,
  RITE_HALF,
  TOP__HALF,
  BOT__HALF,
  TOP__LEFT_QTR,
  TOP__RITE_QTR,
  BOT__LEFT_QTR,
  BOT__RITE_QTR,
  // Extra tiles needed for 2 x 4:
  LEFT_QTR,
  RITE_QTR,
  LEFT_CTR__QTR,
  RITE_CTR__QTR,
  TOP__LEFT_8TH,
  TOP__RITE_8TH,
  TOP__LEFT_CTR_8TH,
  TOP__RITE_CTR_8TH,
  BOT__LEFT_8TH,
  BOT__RITE_8TH,
  BOT__LEFT_CTR_8TH,
  BOT__RITE_CTR_8TH,
  // 1 x 3 tiles:
  LEFT_THIRD,
  CTR__THIRD,
  RITE_THIRD,
  LEFT_TWO_THIRDS,
  RITE_TWO_THIRDS
};

