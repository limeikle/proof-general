(*  Title:      HOL/ex/BinEx.thy
  
  Note on parsing this:
  
   - Sometimes goes very quickly
   - Sometimes goes quite slowly, can watch the percentage indicator
     in the outline view count up to 100.
     
  What causes the difference in this?
  Could be marker generation maybe: there's a lot of it in this file.
  Second time through markers are created quickly for some reason?
  
  Maybe a good case for profiling.
*)

header {* Binary arithmetic examples *}

theory BinEx imports Main begin

subsection {* Regression Testing for Cancellation Simprocs *}

lemma "l + 2 + 2 + 2 + (l + 2) + (oo + 2) = (uu::int)"
apply simp  oops

lemma "2*u = (u::int)"
apply simp  oops

lemma "(i + j + 12 + (k::int)) - 15 = y"
apply simp  oops

lemma "(i + j + 12 + (k::int)) - 5 = y"
apply simp  oops

lemma "y - b < (b::int)"
apply simp  oops

lemma "y - (3*b + c) < (b::int) - 2*c"
apply simp  oops

lemma "(2*x - (u*v) + y) - v*3*u = (w::int)"
apply simp  oops

lemma "(2*x*u*v + (u*v)*4 + y) - v*u*4 = (w::int)"
apply simp  oops

lemma "(2*x*u*v + (u*v)*4 + y) - v*u = (w::int)"
apply simp  oops

lemma "u*v - (x*u*v + (u*v)*4 + y) = (w::int)"
apply simp  oops

lemma "(i + j + 12 + (k::int)) = u + 15 + y"
apply simp  oops

lemma "(i + j*2 + 12 + (k::int)) = j + 5 + y"
apply simp  oops

lemma "2*y + 3*z + 6*w + 2*y + 3*z + 2*u = 2*y' + 3*z' + 6*w' + 2*y' + 3*z' + u + (vv::int)"
apply simp  oops

lemma "a + -(b+c) + b = (d::int)"
apply simp  oops

lemma "a + -(b+c) - b = (d::int)"
apply simp  oops

(*negative numerals*)
lemma "(i + j + -2 + (k::int)) - (u + 5 + y) = zz"
apply simp  oops

lemma "(i + j + -3 + (k::int)) < u + 5 + y"
apply simp  oops

lemma "(i + j + 3 + (k::int)) < u + -6 + y"
apply simp  oops

lemma "(i + j + -12 + (k::int)) - 15 = y"
apply simp  oops

lemma "(i + j + 12 + (k::int)) - -15 = y"
apply simp  oops

lemma "(i + j + -12 + (k::int)) - -15 = y"
apply simp  oops

lemma "- (2*i) + 3  + (2*i + 4) = (0::int)"
apply simp  oops



subsection {* Arithmetic Method Tests *}


lemma "!!a::int. [| a <= b; c <= d; x+y<z |] ==> a+c <= b+d"
by arith

lemma "!!a::int. [| a < b; c < d |] ==> a-d+ 2 <= b+(-c)"
by arith

lemma "!!a::int. [| a < b; c < d |] ==> a+c+ 1 < b+d"
by arith

lemma "!!a::int. [| a <= b; b+b <= c |] ==> a+a <= c"
by arith

lemma "!!a::int. [| a+b <= i+j; a<=b; i<=j |] ==> a+a <= j+j"
by arith

lemma "!!a::int. [| a+b < i+j; a<b; i<j |] ==> a+a - - -1 < j+j - 3"
by arith

lemma "!!a::int. a+b+c <= i+j+k & a<=b & b<=c & i<=j & j<=k --> a+a+a <= k+k+k"
by arith

lemma "!!a::int. [| a+b+c+d <= i+j+k+l; a<=b; b<=c; c<=d; i<=j; j<=k; k<=l |]
      ==> a <= l"
by arith

lemma "!!a::int. [| a+b+c+d <= i+j+k+l; a<=b; b<=c; c<=d; i<=j; j<=k; k<=l |]
      ==> a+a+a+a <= l+l+l+l"
by arith

lemma "!!a::int. [| a+b+c+d <= i+j+k+l; a<=b; b<=c; c<=d; i<=j; j<=k; k<=l |]
      ==> a+a+a+a+a <= l+l+l+l+i"
by arith

lemma "!!a::int. [| a+b+c+d <= i+j+k+l; a<=b; b<=c; c<=d; i<=j; j<=k; k<=l |]
      ==> a+a+a+a+a+a <= l+l+l+l+i+l"
by arith

lemma "!!a::int. [| a+b+c+d <= i+j+k+l; a<=b; b<=c; c<=d; i<=j; j<=k; k<=l |]
      ==> 6*a <= 5*l+i"
by arith



subsection {* The Integers *}

text {* Addition *}

lemma "(13::int) + 19 = 32"
  by simp

lemma "(1234::int) + 5678 = 6912"
  by simp

lemma "(1359::int) + -2468 = -1109"
  by simp

lemma "(93746::int) + -46375 = 47371"
  by simp


text {* \medskip Negation *}

lemma "- (65745::int) = -65745"
  by simp

lemma "- (-54321::int) = 54321"
  by simp


text {* \medskip Multiplication *}

lemma "(13::int) * 19 = 247"
  by simp

lemma "(-84::int) * 51 = -4284"
  by simp

lemma "(255::int) * 255 = 65025"
  by simp

lemma "(1359::int) * -2468 = -3354012"
  by simp

lemma "(89::int) * 10 ≠ 889"
  by simp

lemma "(13::int) < 18 - 4"
  by simp

lemma "(-345::int) < -242 + -100"
  by simp

lemma "(13557456::int) < 18678654"
  by simp

lemma "(999999::int) ≤ (1000001 + 1) - 2"
  by simp

lemma "(1234567::int) ≤ 1234567"
  by simp

text{*No integer overflow!*}
lemma "1234567 * (1234567::int) < 1234567*1234567*1234567"
  by simp


text {* \medskip Quotient and Remainder *}

lemma "(10::int) div 3 = 3"
  by simp

lemma "(10::int) mod 3 = 1"
  by simp

text {* A negative divisor *}

lemma "(10::int) div -3 = -4"
  by simp

lemma "(10::int) mod -3 = -2"
  by simp

text {*
  A negative dividend\footnote{The definition agrees with mathematical
  convention and with ML, but not with the hardware of most computers}
*}

lemma "(-10::int) div 3 = -4"
  by simp

lemma "(-10::int) mod 3 = 2"
  by simp

text {* A negative dividend \emph{and} divisor *}

lemma "(-10::int) div -3 = 3"
  by simp

lemma "(-10::int) mod -3 = -1"
  by simp

text {* A few bigger examples *}

lemma "(8452::int) mod 3 = 1"
  by simp

lemma "(59485::int) div 434 = 137"
  by simp

lemma "(1000006::int) mod 10 = 6"
  by simp


text {* \medskip Division by shifting *}

lemma "10000000 div 2 = (5000000::int)"
  by simp

lemma "10000001 mod 2 = (1::int)"
  by simp

lemma "10000055 div 32 = (312501::int)"
  by simp

lemma "10000055 mod 32 = (23::int)"
  by simp

lemma "100094 div 144 = (695::int)"
  by simp

lemma "100094 mod 144 = (14::int)"
  by simp


text {* \medskip Powers *}

lemma "2 ^ 10 = (1024::int)"
  by simp

lemma "-3 ^ 7 = (-2187::int)"
  by simp

lemma "13 ^ 7 = (62748517::int)"
  by simp

lemma "3 ^ 15 = (14348907::int)"
  by simp

lemma "-5 ^ 11 = (-48828125::int)"
  by simp


subsection {* The Natural Numbers *}

text {* Successor *}

lemma "Suc 99999 = 100000"
  by (simp add: Suc_nat_number_of)
    -- {* not a default rewrite since sometimes we want to have @{text "Suc nnn"} *}


text {* \medskip Addition *}

lemma "(13::nat) + 19 = 32"
  by simp

lemma "(1234::nat) + 5678 = 6912"
  by simp

lemma "(973646::nat) + 6475 = 980121"
  by simp


text {* \medskip Subtraction *}

lemma "(32::nat) - 14 = 18"
  by simp

lemma "(14::nat) - 15 = 0"
  by simp

lemma "(14::nat) - 1576644 = 0"
  by simp

lemma "(48273776::nat) - 3873737 = 44400039"
  by simp


text {* \medskip Multiplication *}

lemma "(12::nat) * 11 = 132"
  by simp

lemma "(647::nat) * 3643 = 2357021"
  by simp


text {* \medskip Quotient and Remainder *}

lemma "(10::nat) div 3 = 3"
  by simp

lemma "(10::nat) mod 3 = 1"
  by simp

lemma "(10000::nat) div 9 = 1111"
  by simp

lemma "(10000::nat) mod 9 = 1"
  by simp

lemma "(10000::nat) div 16 = 625"
  by simp

lemma "(10000::nat) mod 16 = 0"
  by simp


text {* \medskip Powers *}

lemma "2 ^ 12 = (4096::nat)"
  by simp

lemma "3 ^ 10 = (59049::nat)"
  by simp

lemma "12 ^ 7 = (35831808::nat)"
  by simp

lemma "3 ^ 14 = (4782969::nat)"
  by simp

lemma "5 ^ 11 = (48828125::nat)"
  by simp


text {* \medskip Testing the cancellation of complementary terms *}

lemma "y + (x + -x) = (0::int) + y"
  by simp

lemma "y + (-x + (- y + x)) = (0::int)"
  by simp

lemma "-x + (y + (- y + x)) = (0::int)"
  by simp

lemma "x + (x + (- x + (- x + (- y + - z)))) = (0::int) - y - z"
  by simp

lemma "x + x - x - x - y - z = (0::int) - y - z"
  by simp

lemma "x + y + z - (x + z) = y - (0::int)"
  by simp

lemma "x + (y + (y + (y + (-x + -x)))) = (0::int) + y - x + y + y"
  by simp

lemma "x + (y + (y + (y + (-y + -x)))) = y + (0::int) + y"
  by simp

lemma "x + y - x + z - x - y - z + x < (1::int)"
  by simp

end
