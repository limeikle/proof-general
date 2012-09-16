<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<!-- 
	This file is used to convert PGIP output into HTML for display.
	It is referenced by a preference setting. To change the display,
	create a new .xsl file and change the preference setting.
	Please let us know if you create a style-sheet you want to share.
-->

<xsl:template match="/">
     <div>
          <xsl:apply-templates />
     </div>
</xsl:template>        
<xsl:template match="br">     
	<br/>
</xsl:template>
<xsl:preserve-space elements="statedisplay" />
        
	<xsl:template match="atom">	
		<xsl:choose>
			<xsl:when test="string(@kind)='free'" >
		        <font color="blue" title="free variable" ><xsl:value-of select="."/></font>			
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
