(* Some tests to exercise the Proof Objects view + ProverKnowledge *)

theory KnowledgeTest imports Main
begin

lemma foo: "x+y = y+x+z "
apply (rule IntDef.int)

end

(* NB: some things PK can't read:

 LOrder.join_aci
 
 *)