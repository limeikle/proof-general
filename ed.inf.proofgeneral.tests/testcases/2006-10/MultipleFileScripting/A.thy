(* Comment at start. Distinguishes fileopen/theoryopen *)

theory A imports Main begin

consts foo :: 'a
consts bar :: 'a

lemma foo: "P-->P"
apply (rule impI)
apply assumption
done

end
