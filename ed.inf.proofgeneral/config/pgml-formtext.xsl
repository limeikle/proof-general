<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<!--
	This file is used to convert PGIP output in PGML into Eclipse Forms XML for display.

	NB: Eclipse Forms XML is very limited; markup that cannot be handled will give
	an empty display.  In particular, nested <span> elements are not allowed.

	It is referenced by a preference setting. To change the display,
	create a new .xsl file and change the preference setting.
	Please let us know if you create a style-sheet you want to share.
-->

<xsl:template match="/">
	<p><xsl:apply-templates /></p>
</xsl:template>

<xsl:template match="br">
	<br/>
</xsl:template>

<xsl:template match="normalresponse">
  <!-- actually normal response is uncoloured: this light blue grey
      just helps to highlight response regions. -->
  <span color="gray">
    <xsl:apply-templates/>
  </span>
</xsl:template>

<xsl:template match="errorresponse">
  <span color="gray">
    <xsl:apply-templates/>
  </span>
</xsl:template>


<!--
      Proof state display
-->
<xsl:preserve-space elements="statedisplay pgmltext">
    <xsl:apply-templates/>
</xsl:preserve-space>

<xsl:template match="pgmltext">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="statedisplay">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="sym">
  <xsl:apply-templates/>
</xsl:template>


<!--
      PGML atoms: kinds in Isabelle with same colours as in PG Emacs (rgb.txt names).
-->
<xsl:template match="atom">
<xsl:choose>
  <xsl:when test="string(@kind)='class'" >
    <span color="red"><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:when test="string(@kind)='tfree'" >
    <!-- purple -->
    <span color="#A020F0" ><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:when test="string(@kind)='tvar'" >
    <!-- purple -->
    <span color="#A020F0" ><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:when test="string(@kind)='free'" >
    <!-- blue -->
    <span color="blue" ><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:when test="string(@kind)='bound'" >
    <!-- green4 -->
    <span color="#008B00" ><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:when test="string(@kind)='var'" >
    <!-- midnight blue -->
    <span color="#191970" ><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:when test="string(@kind)='skolem'" >
    <!-- chocolate -->
    <span color="D2691E" ><xsl:value-of select="."/></span>
  </xsl:when>
  <xsl:otherwise>
    <span color="green"><xsl:value-of select="."/></span>
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
