<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<!--
	This file is used to convert PGIP output in PGML into HTML for display.
	It is referenced by a preference setting. To change the display,
	create a new .xsl file and change the preference setting.
	Please let us know if you create a style-sheet you want to share.
-->

<xsl:template match="/">
     <div>
          <xsl:apply-templates />
     </div>
</xsl:template>

<xsl:template match="pgmltext">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="br">
	<br/>
</xsl:template>

<xsl:template match="normalresponse">
  <!-- normal response may be uncoloured: this light blue grey
      just helps to highlight response regions. -->
  <div style="background-color: #E0E0F8;">
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="errorresponse">
  <div style="background-color: #FA8072;">
    <xsl:apply-templates/>
  </div>
</xsl:template>


<!--
      Proof state display
-->
<xsl:preserve-space elements="statedisplay pgmltext">
    <xsl:apply-templates/>
</xsl:preserve-space>

<xsl:template match="pgmltext">
  <div style="background-color: #E4F8EA;
              font-family: Mono,Lucida Sans Typewriter;
              font-size: 80%;">
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="statedisplay">
  <div style="background-color: #F0F0F0;
              font-family: Mono,Lucida Sans Typewriter;
              font-size: 80%;">
    <xsl:apply-templates/>
  </div>
</xsl:template>

<!-- Values display -->

<xsl:template match="idvalue">
    <xsl:apply-templates/>
</xsl:template>


<!--  PGML symbols.
      We ought to process these using the Eclipse symbol table (e.g.,
      using a style sheet prefix).  
      For now we just strip them out and rely on replacement of ASCII text
      before getting here using the existing regexp based replacement.
 -->

<xsl:template match="sym">
	<xsl:apply-templates/>
</xsl:template>

<!-- Isabelle-specific superscripts and subscripts -->

<!-- todo: transform symbols named \<^sup>  \<^bsup> ...\<^esup> -->

<!--
      PGML atoms: kinds in Isabelle with same colours as in PG Emacs (rgb.txt names).
-->
<xsl:template match="atom">
<xsl:choose>
  <xsl:when test="string(@kind)='class'" >
    <font color="red" title="free variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:when test="string(@kind)='tfree'" >
    <!-- purple -->
    <font color="#A020F0" title="free type variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:when test="string(@kind)='tvar'" >
    <!-- purple -->
    <font color="#A020F0" title="scheme type variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:when test="string(@kind)='free'" >
    <!-- blue -->
    <font color="blue" title="free variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:when test="string(@kind)='bound'" >
    <!-- green4 -->
    <font color="#008B00" title="bound variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:when test="string(@kind)='var'" >
    <!-- midnight blue -->
    <font color="#191970" title="scheme variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:when test="string(@kind)='skolem'" >
    <!-- chocolate -->
    <font color="D2691E" title="skolem variable" ><xsl:value-of select="."/></font>
  </xsl:when>
  <xsl:otherwise>
    <font color="green" title="bound variable" ><xsl:value-of select="."/></font>
  </xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:template match="opengoal">
  <b>opengoal</b>
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="helpdoc">
	<xsl:value-of select="@name"/>:   <xsl:value-of select="@url"/>
</xsl:template>



</xsl:stylesheet>
