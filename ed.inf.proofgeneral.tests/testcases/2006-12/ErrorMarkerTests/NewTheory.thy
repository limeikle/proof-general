(* This file has two kinds of errors in it: a parse
   error at "donex" and a processing error with the
   duplicated line.
   
   This is to experiment with good error 
   marker addition/removal code.
*)

theory NewTheory imports Main
begin

lemma foo: "P-->P"
apply (rule impI)  
apply (rule impI)
apply assumption    
donex
done

end
