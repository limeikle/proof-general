theory NewTheory imports Main
begin

lemma foo: "P-->P"
apply (rule impI)  
(* process to here, change next line to "done".
   Try to process it.  Gives error.  Remove done,
   try to process again. *)
apply assumption  
done

end
