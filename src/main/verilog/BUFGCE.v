///////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 1995/2016 Xilinx, Inc.
//  All Right Reserved.
///////////////////////////////////////////////////////////////////////////////
//   ____  ____
//  /   /\/   /
// /___/  \  /     Vendor      : Xilinx
// \   \   \/      Version     : 2017.1
//  \   \          Description : Xilinx Unified Simulation Library Component
//  /   /                        General Clock Buffer with Clock Enable
// /___/   /\      Filename    : BUFGCE.v
// \   \  /  \
//  \___\/\___\
//
///////////////////////////////////////////////////////////////////////////////
//  Revision:
//    05/15/12 - Initial version.
//    10/22/14 - 808642 - Added #1 to $finish
//  End Revision:
///////////////////////////////////////////////////////////////////////////////

`timescale 1 ps / 1 ps


module BUFGCE (
  output O,

  input CE,
  input I
);

  assign CE_in = CE; // rv 1
  assign I_in = I;

  reg enable_clk = 1'b1;

  always @(I_in or CE_in) begin
    if (~I_in)
      enable_clk = CE_in;
  end

  assign O = enable_clk & I_in;

endmodule
