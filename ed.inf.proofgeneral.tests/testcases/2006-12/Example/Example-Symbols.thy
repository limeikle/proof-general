(*
      Example proof document for Isabelle/Isar Proof General.
   
      $Id: Example-Symbols.thy,v 1.2 2007/02/19 19:06:49 da Exp $
*)

theory Example imports Main begin

theorem and_comms: "A ∧ B -⟶ B ∧ A"
proof
  assume "A ∧ B"
  then show "B ∧ A"
  proof
    assume B and A
    then
   show ?thesis ..
 qed
qed

end
