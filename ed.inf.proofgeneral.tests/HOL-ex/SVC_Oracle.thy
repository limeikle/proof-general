(*  Title:      HOL/ex/SVC_Oracle.thy
    ID:         $Id: SVC_Oracle.thy,v 1.1 2006/12/19 15:22:46 da Exp $
    Author:     Lawrence C Paulson
    Copyright   1999  University of Cambridge

Based upon the work of S�ren T. Heilmann.
*)

header {* Installing an oracle for SVC (Stanford Validity Checker) *}

theory SVC_Oracle
imports Main
uses "svc_funcs.ML" ("svc_oracle.ML")
begin

consts
  iff_keep :: "[bool, bool] => bool"
  iff_unfold :: "[bool, bool] => bool"

hide const iff_keep iff_unfold

oracle
  svc_oracle ("term") = Svc.oracle

use "svc_oracle.ML"

end
